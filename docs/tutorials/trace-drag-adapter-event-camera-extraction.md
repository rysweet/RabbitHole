# Tutorial: Trace the DragAdapter Event and Camera Extraction

This tutorial walks through the extraction of `DragEventHandler` and
`DragCameraController` from `DragAdapter` (issue #566). You will trace
each design decision — why event listeners route through DragAdapter
instead of handling events directly, how camera state is isolated, and
why certain fields must remain on the parent class.

For the full contract, see the [DragAdapter Event and Camera Extraction
reference](../reference/drag-adapter-event-camera-extraction.md).

For validation steps, see the [Validation how-to](../howto/validate-drag-adapter-event-camera-extraction.md).

## Contents

- [Goal](#goal)
- [1. Understand the pre-extraction structure](#1-understand-the-pre-extraction-structure)
- [2. Trace the DragEventHandler extraction](#2-trace-the-drageventhandler-extraction)
- [3. Trace the DragCameraController extraction](#3-trace-the-dragcameracontroller-extraction)
- [4. Trace the subclass override routing](#4-trace-the-subclass-override-routing)
- [5. Trace the field ownership decisions](#5-trace-the-field-ownership-decisions)
- [6. Trace the delegation stubs](#6-trace-the-delegation-stubs)
- [7. Run the characterization tests](#7-run-the-characterization-tests)
- [8. Understand the boundaries](#8-understand-the-boundaries)

## Goal

After this tutorial you will be able to explain:

- Why AWT listeners live in `DragEventHandler` but route events through
  `DragAdapter` protected methods
- Why `currentInputState` stays on DragAdapter despite being heavily used
  by event handlers
- Why `CameraSet` moves to `DragCameraController` but `CameraView` stays
  on DragAdapter
- How `fireStateChange()` acts as the bridge between event handling and
  manipulator orchestration
- Why the composition objects store a `DragAdapter` back-reference instead
  of accepting narrow dependencies

Open these source files alongside this guide:

```text
core/story-api/src/main/java/org/alice/interact/DragAdapter.java
core/story-api/src/main/java/org/alice/interact/DragEventHandler.java
core/story-api/src/main/java/org/alice/interact/DragCameraController.java
core/story-api/src/test/java/org/alice/interact/DragEventHandlerTest.java
core/story-api/src/test/java/org/alice/interact/DragCameraControllerTest.java
```

## 1. Understand the pre-extraction structure

Before extraction, `DragAdapter` has 1039 lines with three interleaved
concerns:

```text
DragAdapter (1039 lines, abstract)
  │
  ├── Lines 112-150    Constants and key mappings (DEFAULT_MOVEMENT_KEYS, etc.)
  ├── Lines 151-170    Field declarations (handleManager, selectionListeners,
  │                    cameraTransformationListener, cameraMap, selectedObject...)
  ├── Lines 172-212    Listener registration (isComponentListener, addListeners,
  │                    removeListeners) ← EVENT HANDLING
  ├── Lines 214-234    Manipulator API (add/remove ManipulationListener, etc.)
  ├── Lines 236-266    Render target and component wiring ← BRIDGE
  ├── Lines 268-301    Animator and interaction state
  ├── Lines 303-349    Camera registry ← CAMERA MANAGEMENT
  ├── Lines 351-384    Camera mouse control setup
  ├── Lines 386-425    Selection listeners and handle management
  ├── Lines 427-450    Camera view operations ← CAMERA MANAGEMENT
  ├── Lines 452-605    Selection logic and state change
  ├── Lines 607-638    Mouse wheel state and helpers ← EVENT HANDLING
  ├── Lines 640-757    Scene object selection and picking ← MIXED
  ├── Lines 761-880    All handle* methods ← EVENT HANDLING
  ├── Lines 883-916    Update loop and display callback ← CAMERA MANAGEMENT
  ├── Lines 918-971    AWT listener instances ← EVENT HANDLING
  ├── Lines 973-984    Remaining field declarations
  └── Lines 986-1039   Enums and CameraSet inner class
```

Observe how event handling code (172-212, 607-638, 761-880, 918-971) is
scattered across the file. Camera management (303-349, 427-450, 883-916)
is also dispersed. This makes it difficult to reason about either
concern in isolation.

## 2. Trace the DragEventHandler extraction

Open `DragEventHandler.java`. The class receives a `DragAdapter` in its
constructor and stores it as a final field.

**Why a back-reference to the full DragAdapter?** The event handler needs:
- `dragAdapter.currentInputState` — to mutate input state on every event
- `dragAdapter.currentRolloverComponent` — read by `handleMouseReleased`
- `dragAdapter.fireStateChange()` — to trigger manipulator evaluation
- `dragAdapter.getOnscreenRenderTarget()` — for pick-into-scene
- `dragAdapter.handleMouseEntered(e)` — polymorphic dispatch for subclasses
- `dragAdapter.handleMouseMoved(e)` — polymorphic dispatch for subclasses
- `dragAdapter.getLookingGlassComponent()` — to compare event sources

That's 7 different touchpoints across the DragAdapter API. Passing them
individually would create a 7+ parameter constructor or require a custom
interface. Since both classes are package-private in the same package,
the back-reference is simpler and equally safe.

**What moves vs. what stays:**

Only the _private_ `handle*` methods move to DragEventHandler:
`handleMouseExited`, `handleMousePressed`, `handleMouseReleased`,
`handleMouseDragged`, `handleMouseWheelMoved`, `handleKeyPressed`,
`handleKeyReleased`. These are never overridden by subclasses.

Several helper methods also move: `pickIntoScene`,
`pickIntoSceneSuppressingErrors`, `getHandleForComponent`,
`stopMouseWheel`, `shouldStopMouseWheel`, `isMouseWheelActive`, and
`isComponentListener`. The first five of these become **package-private**
(not private) on DragEventHandler because they are called by
`handleMouseEntered` and `handleMouseMoved` which stay on DragAdapter.

`handleMouseEntered` and `handleMouseMoved` stay on DragAdapter as
`protected` methods because `RuntimeDragAdapter` and
`SingleViewerDragAdapter` override them. The AWT listeners in
DragEventHandler route through `dragAdapter.handleMouseEntered(e)` and
`dragAdapter.handleMouseMoved(e)` for polymorphic dispatch.

**Bidirectional delegation:** This creates a two-way relationship:
- DragEventHandler → DragAdapter: AWT listeners call
  `dragAdapter.handleMouseEntered(e)` and `dragAdapter.handleMouseMoved(e)`
  for polymorphic dispatch; private `handle*` methods call
  `dragAdapter.fireStateChange()` and read `dragAdapter.currentInputState`.
- DragAdapter → DragEventHandler: `handleMouseEntered` and
  `handleMouseMoved` call `eventHandler.pickIntoSceneSuppressingErrors()`,
  `eventHandler.getHandleForComponent()`,
  `eventHandler.shouldStopMouseWheel()`, and `eventHandler.stopMouseWheel()`
  for event-handling helpers that moved with the mouse wheel and pick state.

**Trace the listener routing pattern:**

```java
// In DragEventHandler
private final MouseListener mouseListener = new MouseListener() {
    @Override
    public void mouseEntered(MouseEvent e) {
        // Routes through DragAdapter, not directly to handleMouseEntered
        dragAdapter.handleMouseEntered(e);
    }
    // ...
};
```

This is critical. If the listener called a local `handleMouseEntered`
method on DragEventHandler, subclass overrides on
`DragAdapter.handleMouseEntered` would never fire. The indirection
through `dragAdapter` ensures polymorphic
dispatch.

**Trace the mouse wheel timeout:**

The new `updateMouseWheelTimeout(double timeDelta, Runnable onTimeout)`
method replaces inline code that was in `DragAdapter.update()`:

```java
// In DragEventHandler
void updateMouseWheelTimeout(double timeDelta, Runnable onTimeout) {
    if (isMouseWheelActive()) {
        mouseWheelTimeoutTime -= timeDelta;
        if (!isMouseWheelActive()) {
            stopMouseWheel();
            onTimeout.run();  // calls dragAdapter.fireStateChange()
        }
    }
}
```

The `Runnable` parameter avoids a direct dependency on `fireStateChange()`,
keeping the timeout logic testable in isolation.

## 3. Trace the DragCameraController extraction

Open `DragCameraController.java`. Like `DragEventHandler`, it holds a
final `DragAdapter` reference.

**Why does `CameraSet` move here but `CameraView` stays on DragAdapter?**

`CameraView` is a public enum referenced by 25+ external files as
`DragAdapter.CameraView`. Moving it would break every import. `CameraSet`
was `private static final` — no external code ever referenced it. Moving
it to `DragCameraController` as a package-private static inner class
changes nothing for consumers.

**Trace the display loop callback:**

```java
// In DragCameraController
void handleAutomaticDisplayCompleted(AutomaticDisplayEvent e) {
    AbstractCamera sgCamera = getSGCamera();
    if (sgCamera != null) {
        if (!hasSetCameraTransformables) {
            dragAdapter.setSGCamera(sgCamera);
            hasSetCameraTransformables = true;
        }
        double timeCurr = Clock.getCurrentTime();
        if (Double.isNaN(this.timePrev)) {
            this.timePrev = Clock.getCurrentTime();
        }
        double timeDelta = timeCurr - this.timePrev;
        dragAdapter.update(timeDelta);  // polymorphic — subclasses override
        this.timePrev = timeCurr;
    }
}
```

The callback calls `dragAdapter.update(timeDelta)` and
`dragAdapter.setSGCamera(sgCamera)` — both are protected hooks that
subclasses override. The camera controller must route through DragAdapter
for the same polymorphic dispatch reason as the event handler.

**Trace the handleManager interaction:**

`makeCameraActive` and `addCameraView` call
`dragAdapter.getHandleManager().updateCameraPosition(...)`. A
package-private `HandleManager getHandleManager()` accessor is added to
DragAdapter for this purpose.

## 4. Trace the subclass override routing

This is the most critical design decision. Nine subclasses exist in the
hierarchy, and two override the protected methods that the extraction
must preserve:

**RuntimeDragAdapter** overrides both `handleMouseEntered` (no-op) and
`handleMouseMoved` (simplified — no picking):
```java
@Override
protected void handleMouseEntered(MouseEvent e) {
    // intentionally empty — runtime doesn't need rollover picking
}

@Override
protected void handleMouseMoved(MouseEvent e) {
    // simplified — no picking, just update location and fire
    this.currentInputState.setMouseLocation(e.getPoint());
    this.fireStateChange();
}
```

If `DragEventHandler` called its own local method, these overrides would
never fire. Because the AWT listener calls `dragAdapter.handleMouseEntered(e)`,
the JVM dispatches to `RuntimeDragAdapter.handleMouseEntered`, preserving
the no-op behavior.

**SingleViewerDragAdapter** overrides `handleMouseMoved` with a simplified
version that skips scene picking (there is no need for rollover events
in the skeleton viewer). Same routing pattern preserves this override.

**CroquetSupportingDragAdapter** does NOT override `handleMouseMoved` or
`handleMouseEntered`. Instead, it has its own `dragUpdated`,
`dragEntered`, and `dragExited` methods that call `fireStateChange()`
directly for drag-and-drop event handling. Since `fireStateChange()`
stays on DragAdapter, this works unchanged.

**NiceDragAdapter** (in `test.ik` package) extends
`OnscreenLookingGlassDragAdapter` and overrides only methods defined on
that intermediate class (`handleMousePress`, `handleMouseDrag`,
`handleMouseRelease`, `updateTranslation`). It does not override any
DragAdapter methods relevant to this extraction, so it compiles
unchanged.

## 5. Trace the field ownership decisions

**Why does `currentInputState` stay on DragAdapter?**

`currentInputState` is `protected final` and accessed directly by:
- `RuntimeDragAdapter` — reads mouse state for runtime event dispatch
- `handleMouseEntered`, `handleMouseMoved` (stay on DragAdapter) — mutates on every event
- Private `handle*` methods (move to DragEventHandler) — mutates via `dragAdapter.currentInputState`
- `handleStateChange` (stays on DragAdapter) — reads for manipulator evaluation
- `setCameraOnManipulator` (moves to DragCameraController) — reads pick camera

Moving it to either extracted class would require the other two consumers
to use a getter. Keeping it on DragAdapter with package-private access
means all three locations access it naturally.

**Why does `currentRolloverComponent` stay on DragAdapter?**

`currentRolloverComponent` is used by `handleMouseEntered` (which stays
on DragAdapter for subclass override reasons) and by `handleMouseReleased`
(which moves to DragEventHandler but accesses it via
`dragAdapter.currentRolloverComponent`). Since the field is mutated by
a staying method and read by a moving method, it stays on DragAdapter
as package-private.

**Why does `lookingGlassComponent` stay on DragAdapter?**

`setAWTComponent` (a private method on DragAdapter) calls both
`eventHandler.removeListeners(component)` and
`eventHandler.addListeners(component)`. The field is the canonical
reference managed by `setOnscreenRenderTarget()`. A package-private
`getLookingGlassComponent()` accessor lets `DragEventHandler` compare
event sources against it.

## 6. Trace the delegation stubs

Open the modified `DragAdapter.java` and search for `eventHandler.` and
`cameraController.`. Each delegation method is a one-liner:

```java
public void addListeners(Component c) { eventHandler.addListeners(c); }
public AbstractCamera getActiveCamera() { return cameraController.getActiveCamera(); }
```

The `update()` method is the one non-trivial delegation:

```java
@Override
protected void update(double timeDelta) {
    eventHandler.updateMouseWheelTimeout(timeDelta, this::fireStateChange);
    for (ManipulatorConditionSet mcs : this.manipulators) {
        if (mcs.getManipulator().hasStarted()
            && mcs.shouldContinue(this.currentInputState, this.previousInputState)) {
            mcs.getManipulator().timeUpdateManipulator(timeDelta, this.currentInputState);
        }
    }
}
```

The mouse wheel timeout portion delegates to the event handler. The
manipulator time-update loop stays because it reads `manipulators`,
`currentInputState`, and `previousInputState` — all owned by DragAdapter.

The `handleMouseEntered` and `handleMouseMoved` methods also call into
`eventHandler` for helper methods that moved:

```java
protected void handleMouseEntered(MouseEvent e) {
    this.currentRolloverComponent = e.getComponent();
    if (!this.currentInputState.isAnyMouseButtonDown()) {
        this.currentInputState.setMouseLocation(e.getPoint());
        if (e.getComponent() == this.lookingGlassComponent) {
            eventHandler.pickIntoSceneSuppressingErrors(
                e.getPoint(), currentInputState::setRolloverPickResult);
        } else {
            this.currentInputState.setRolloverHandle(
                eventHandler.getHandleForComponent(e.getComponent()));
        }
        this.currentInputState.setTimeCaptured();
        this.currentInputState.setInputEvent(e);
        this.fireStateChange();
    }
}
```

These five package-private methods on DragEventHandler
(`pickIntoScene`, `pickIntoSceneSuppressingErrors`,
`getHandleForComponent`, `shouldStopMouseWheel`, `stopMouseWheel`)
form the reverse direction of the bidirectional delegation: DragAdapter
calls them on `eventHandler`, while `DragEventHandler` calls back to
`dragAdapter` for polymorphic hooks and state.

## 7. Run the characterization tests

```bash
# Run only the new tests
mvn -pl core/story-api -Dtest="DragEventHandlerTest,DragCameraControllerTest" test

# Run full module test suite to verify no regressions
mvn -pl core/story-api -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

The characterization tests verify structural contracts:

- `DragEventHandlerTest` creates a mock `DragAdapter` (or a minimal
  concrete subclass) and verifies that listener registration, mouse wheel
  timeout countdown, and wheel stop-on-movement work correctly.
- `DragCameraControllerTest` creates a controller with a stub
  `DragAdapter` and verifies camera view add/get/clear and active camera
  selection.

## 8. Understand the boundaries

This extraction does NOT change:

- **Manipulator evaluation** — `handleStateChange()`, `fireStateChange()`,
  and all manipulator condition set logic stay on DragAdapter unchanged.
- **Selection logic** — `setSelectedImplementation`,
  `setSelectedSceneObjectImplementation`, silhouette management, and
  selection event firing stay on DragAdapter.
- **Subclass behavior** — No subclass requires any code change. All
  overrides fire through the same polymorphic dispatch paths.
- **External API** — No qualified name changes. `DragAdapter.CameraView`,
  `DragAdapter.ObjectType`, and all public methods remain at their
  original signatures.
- **Runtime behavior** — The same AWT events trigger the same
  `InputState` mutations, the same `fireStateChange()` calls, and the
  same manipulator evaluations, in the same order.
