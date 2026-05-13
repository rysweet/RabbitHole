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
- `dragAdapter.fireStateChange()` — to trigger manipulator evaluation
- `dragAdapter.getOnscreenRenderTarget()` — for pick-into-scene
- `dragAdapter.handleMouseEntered(e)` — polymorphic dispatch for subclasses
- `dragAdapter.handleMouseMoved(e)` — polymorphic dispatch for subclasses
- `dragAdapter.getLookingGlassComponent()` — to compare event sources

That's 6 different touchpoints across the DragAdapter API. Passing them
individually would create a 6+ parameter constructor or require a custom
interface. Since both classes are package-private in the same package,
the back-reference is simpler and equally safe.

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

This is critical. If the listener called `this.handleMouseEntered(e)`
directly, subclass overrides on `DragAdapter.handleMouseEntered` would
never fire. The indirection through `dragAdapter` ensures polymorphic
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

This is the most critical design decision. Five subclasses override
methods that were on DragAdapter:

**RuntimeDragAdapter** overrides `handleMouseEntered` as a no-op:
```java
@Override
protected void handleMouseEntered(MouseEvent e) {
    // intentionally empty — runtime doesn't need rollover picking
}
```

If `DragEventHandler` called its own local method, this override would
never fire. Because the AWT listener calls `dragAdapter.handleMouseEntered(e)`,
the JVM dispatches to `RuntimeDragAdapter.handleMouseEntered`, preserving
the no-op behavior.

**SingleViewerDragAdapter** overrides both `handleMouseEntered` and
`handleMouseMoved` with custom first-person camera logic. Same routing
pattern preserves those overrides.

**CroquetSupportingDragAdapter** overrides `handleMouseMoved` to add
network synchronization. The routing pattern ensures this fires on every
mouse move event in the scene editor.

## 5. Trace the field ownership decisions

**Why does `currentInputState` stay on DragAdapter?**

`currentInputState` is `protected final` and accessed directly by:
- `RuntimeDragAdapter` — reads mouse state for runtime event dispatch
- All `handle*` methods (now in DragEventHandler) — mutates on every event
- `handleStateChange` (stays on DragAdapter) — reads for manipulator evaluation
- `setCameraOnManipulator` (moves to DragCameraController) — reads pick camera

Moving it to either extracted class would require the other two consumers
to use a getter. Keeping it on DragAdapter with package-private access
means all three locations access it naturally.

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
