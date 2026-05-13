# DragAdapter Event and Camera Extraction

This reference documents the extraction of mouse/keyboard event handling and
camera management from `DragAdapter` (1039 lines) into two new composition
classes: `DragEventHandler` and `DragCameraController`. After extraction,
`DragAdapter.java` is reduced to under 500 lines.

Issue #566 decomposes DragAdapter without changing observable behavior. All
AWT event routing, pick-into-scene logic, camera registry operations, and
manipulator orchestration are preserved identically.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Extracted classes](#extracted-classes)
- [File inventory](#file-inventory)
- [Composition pattern](#composition-pattern)
- [Subclass override preservation](#subclass-override-preservation)
- [Visibility rules](#visibility-rules)
- [Field ownership after extraction](#field-ownership-after-extraction)
- [Delegation methods on DragAdapter](#delegation-methods-on-dragadapter)
- [Enums and inner classes](#enums-and-inner-classes)
- [Configuration](#configuration)
- [Validation](#validation)
- [Characterization tests](#characterization-tests)
- [Acceptance criteria](#acceptance-criteria)
- [Claim boundaries](#claim-boundaries)

## Motivation

`DragAdapter.java` contained 1039 lines mixing three distinct concerns:

1. **AWT event handling** — Mouse listeners, key listeners, mouse wheel
   state, pick-into-scene dispatch, `InputState` transitions, and
   `Component` listener registration (~350 lines).
2. **Camera management** — `CameraSet` inner class, camera view registry,
   active camera selection, camera-to-manipulator wiring, camera
   transformation listener, and display callback (~150 lines).
3. **Selection and manipulator orchestration** — Object selection, handle
   management, manipulator condition evaluation, state change firing,
   interaction groups, and public API surface (~540 lines).

The first two concerns are cohesive and self-contained. Extracting them
into composition objects makes DragAdapter easier to navigate and test,
and enables future independent evolution of event handling and camera
logic.

## Architecture

```text
DragAdapter (~485 lines, abstract)
├── DragEventHandler (composition, final field)
│   ├── AWT listener instances (mouseListener, mouseMotionListener,
│   │   keyListener, mouseWheelListener)
│   ├── addListeners / removeListeners
│   ├── pickIntoScene / pickIntoSceneSuppressingErrors
│   ├── handle* methods (7 event handlers)
│   ├── Mouse wheel timeout state
│   ├── getHandleForComponent
│   └── isComponentListener
│
├── DragCameraController (composition, final field)
│   ├── CameraSet (package-private static inner class)
│   ├── cameraMap (CameraView → CameraSet)
│   ├── addCameraView / clearCameraViews
│   ├── getActiveCamera / makeCameraActive
│   ├── getCameraForManipulator / setCameraOnManipulator
│   ├── cameraTransformationListener
│   └── handleAutomaticDisplayCompleted / update loop
│
└── Retained on DragAdapter
    ├── Enums: ObjectType, CameraView
    ├── Selection: setSelectedImplementation,
    │   setSelectedSceneObjectImplementation,
    │   setSelectedCameraMarker, setSelectedObjectMarker
    ├── Manipulators: handleStateChange, fireStateChange,
    │   addManipulatorConditionSet, setManipulatorStartState
    ├── Interaction groups: mapHandleStyleToInteractionGroup,
    │   setInteractionState, setCurrentInteractionState
    ├── Handle management: handleManager, pushHandleSet, popHandleSet
    ├── Public API: addManipulationListener, triggerManipulationEvent,
    │   setAnimator, getAnimator, setOnscreenRenderTarget,
    │   clearMouseAndKeyboardState, shouldSnap*, getGridSpacing,
    │   getRotationSnapAngle, addCameraMouseControl
    ├── Protected hooks: update(double), setSGCamera, hasSceneEditor
    └── State: currentInputState, previousInputState, selectedObject,
        isInStageChange, manipulators list
```

## Extracted classes

### DragEventHandler

| Property | Value |
| --- | --- |
| File | `DragEventHandler.java` |
| Package | `org.alice.interact` |
| Visibility | Package-private (no access modifier) |
| Constructor | `DragEventHandler(DragAdapter dragAdapter)` |
| Back-reference field | `dragAdapter` (`DragAdapter`, final) |

Owns all AWT listener instances and routes events through the
`DragAdapter` instance so that subclass overrides on `handleMouseEntered`
and `handleMouseMoved` continue to fire.

**Moved fields:**

| Field | Original visibility | New visibility |
| --- | --- | --- |
| `mouseListener` | private final | private final |
| `mouseMotionListener` | private final | private final |
| `keyListener` | private final | private final |
| `mouseWheelListener` | private final | private final |
| `mouseWheelTimeoutTime` | private | private |
| `mouseWheelStartLocation` | private | private |
| `currentRolloverComponent` | private | private |
| `MOUSE_WHEEL_TIMEOUT_TIME` | private static final | package-private static final |
| `CANCEL_MOUSE_WHEEL_DISTANCE` | private static final | package-private static final |

**Moved methods:**

| Method | Original visibility | Calls back to DragAdapter? |
| --- | --- | --- |
| `addListeners(Component)` | public | No — self-contained |
| `removeListeners(Component)` | public | No — self-contained |
| `isComponentListener(Component)` | private | No — checks own listener refs |
| `handleMouseEntered(MouseEvent)` | protected | Yes — `dragAdapter.handleMouseEntered(e)` |
| `handleMouseExited(MouseEvent)` | private | Yes — calls `dragAdapter.fireStateChange()` |
| `handleMousePressed(MouseEvent)` | private | Yes — calls `dragAdapter.fireStateChange()` |
| `handleMouseReleased(MouseEvent)` | private | Yes — calls `dragAdapter.fireStateChange()` |
| `handleMouseDragged(MouseEvent)` | private | Yes — calls `dragAdapter.fireStateChange()` |
| `handleMouseMoved(MouseEvent)` | protected | Yes — `dragAdapter.handleMouseMoved(e)` |
| `handleMouseWheelMoved(MouseWheelEvent)` | private | Yes — calls `dragAdapter.fireStateChange()` |
| `handleKeyPressed(KeyEvent)` | private | Yes — calls `dragAdapter.fireStateChange()` |
| `handleKeyReleased(KeyEvent)` | private | Yes — calls `dragAdapter.fireStateChange()` |
| `pickIntoScene(Point, PickFrontMostObserver)` | private | No — uses `dragAdapter.getOnscreenRenderTarget()` |
| `pickIntoSceneSuppressingErrors(Point, PickFrontMostObserver)` | private | No — wraps `pickIntoScene` |
| `getHandleForComponent(Component)` | private | No — recursive Component walk |
| `stopMouseWheel()` | private | No — mutates own state + `currentInputState` |
| `isMouseWheelActive()` | private | No — reads own state |
| `shouldStopMouseWheel(Point)` | private | No — reads own state |
| `updateMouseWheelTimeout(double, Runnable)` | package-private | No — new method combining wheel timeout from `update()` |

**Listener routing detail:** The `mouseMotionListener` and `mouseListener`
anonymous classes call `dragAdapter.handleMouseEntered(e)` and
`dragAdapter.handleMouseMoved(e)` rather than calling the local private
methods directly. This ensures that subclass overrides in
`RuntimeDragAdapter`, `SingleViewerDragAdapter`, and
`CroquetSupportingDragAdapter` continue to intercept these events.

### DragCameraController

| Property | Value |
| --- | --- |
| File | `DragCameraController.java` |
| Package | `org.alice.interact` |
| Visibility | Package-private (no access modifier) |
| Constructor | `DragCameraController(DragAdapter dragAdapter)` |
| Back-reference field | `dragAdapter` (`DragAdapter`, final) |

Owns the camera view registry and the display-loop callback.

**Moved fields:**

| Field | Original visibility | New visibility |
| --- | --- | --- |
| `cameraMap` | private final | private final |
| `cameraTransformationListener` | private final | private final |
| `automaticDisplayAdapter` | private final | private final |
| `timePrev` | private | private |
| `hasSetCameraTransformables` | private | private |

**Moved inner class:**

| Class | Original visibility | New visibility |
| --- | --- | --- |
| `CameraSet` | private static final | package-private static final |

`CameraSet` moves into `DragCameraController` as a package-private static
inner class. It was previously a private inner class of `DragAdapter`.
External code never referenced `CameraSet` directly.

**Moved methods:**

| Method | Original visibility | Notes |
| --- | --- | --- |
| `addCameraView(CameraView, SymmetricPerspectiveCamera)` | public | Thin delegation from DragAdapter |
| `addCameraView(CameraView, SPC, SPC, OC)` | public | Thin delegation from DragAdapter |
| `addCameraView(CameraView, CameraSet)` | private | Internal |
| `clearCameraViews()` | public | Thin delegation from DragAdapter |
| `getActiveCamera()` | public | Thin delegation from DragAdapter |
| `makeCameraActive(AbstractCamera)` | public | Thin delegation from DragAdapter |
| `getCameraForManipulator(CameraInformedManipulator)` | private | Used by `setCameraOnManipulator` |
| `setCameraOnManipulator(CameraInformedManipulator)` | public | Thin delegation from DragAdapter |
| `handleAutomaticDisplayCompleted(AutomaticDisplayEvent)` | private | Display loop callback |
| `getSGCamera()` | private | Helper for display loop |

## File inventory

| File | Lines (approx) | Change |
| --- | --- | --- |
| `DragAdapter.java` | ~485 | Modified — delegation stubs replace method bodies |
| `DragEventHandler.java` | ~290 | New |
| `DragCameraController.java` | ~175 | New |
| `DragEventHandlerTest.java` | ~80 | New characterization test |
| `DragCameraControllerTest.java` | ~60 | New characterization test |

Total line count across the three production files: ~950 (vs. 1039
original). The split improves navigability without adding net code.

## Composition pattern

DragAdapter creates both composition objects as final fields initialized
in field declarations:

```java
public abstract class DragAdapter {
    final DragEventHandler eventHandler = new DragEventHandler(this);
    final DragCameraController cameraController = new DragCameraController(this);
    // ...
}
```

Both fields are package-private (no modifier) so that test classes in the
same package can access them if needed.

**Constructor `this`-escape safety:** The composition objects store the
`DragAdapter` reference but do not call any methods on it during
construction. The listener instances inside `DragEventHandler` capture
`dragAdapter` but are not registered on any AWT component until
`addListeners()` is called later (triggered by `setOnscreenRenderTarget`).
Similarly, `DragCameraController` only registers its
`automaticDisplayAdapter` when `setOnscreenRenderTarget` is called.

## Subclass override preservation

Five known subclasses override methods on DragAdapter:

| Subclass | Overrides | Preserved how |
| --- | --- | --- |
| `RuntimeDragAdapter` | `handleMouseEntered` (no-op override), `update` | Overrides stay on DragAdapter; `handleMouseEntered` is still `protected` on DragAdapter and called by `eventHandler` via `dragAdapter.handleMouseEntered(e)` |
| `SingleViewerDragAdapter` | `handleMouseEntered`, `handleMouseMoved` | Same pattern — listener routes through DragAdapter protected methods |
| `CroquetSupportingDragAdapter` | `handleMouseMoved` | Same pattern |
| `GlobalDragAdapter` | `addCameraMouseControl`, `update` | `addCameraMouseControl` stays on DragAdapter; `update` stays as protected hook, calls `eventHandler.updateMouseWheelTimeout()` then iterates manipulators |
| `CreateAPersonDragAdapter` | `addCameraMouseControl` | Stays on DragAdapter |

**Critical invariant:** `handleMouseEntered` and `handleMouseMoved` remain
as `protected` methods on `DragAdapter`. The AWT listeners in
`DragEventHandler` call these methods on the `dragAdapter` reference,
which dispatches polymorphically to the correct subclass override.

## Visibility rules

1. Both new classes are **package-private** — they live in `org.alice.interact`
   alongside `DragAdapter` and have no `public` modifier.
2. No method that was previously `public` or `protected` on DragAdapter has
   its visibility reduced. All public API methods remain on DragAdapter as
   thin delegation stubs.
3. `DragAdapter.currentInputState` remains `protected final` — both
   composition objects access it via `dragAdapter.currentInputState`
   (same-package access).
4. `DragAdapter.fireStateChange()` remains `protected` — the event handler
   calls it via `dragAdapter.fireStateChange()` (same-package).
5. `DragAdapter.getOnscreenRenderTarget()` remains `public` — used by
   `DragEventHandler.pickIntoScene()`.
6. The `lookingGlassComponent` field needs a package-private accessor
   `Component getLookingGlassComponent()` added to DragAdapter for use by
   `DragEventHandler`.

## Field ownership after extraction

| Field | Owner after extraction |
| --- | --- |
| `currentInputState` | DragAdapter (accessed by DragEventHandler via `dragAdapter.currentInputState`) |
| `previousInputState` | DragAdapter |
| `onscreenRenderTarget` | DragAdapter |
| `lookingGlassComponent` | DragAdapter (package-private accessor added) |
| `currentRolloverComponent` | DragEventHandler |
| `mouseWheelTimeoutTime` | DragEventHandler |
| `mouseWheelStartLocation` | DragEventHandler |
| `cameraMap` | DragCameraController |
| `timePrev` | DragCameraController |
| `hasSetCameraTransformables` | DragCameraController |
| `handleManager` | DragAdapter |
| `manipulators` | DragAdapter |
| `manipulationEventManager` | DragAdapter |
| `selectedObject` | DragAdapter |
| `animator` | DragAdapter |
| `isInStageChange` | DragAdapter |
| `sgSilhouette` | DragAdapter |
| `selectionListeners` | DragAdapter |
| `mapHandleStyleToInteractionGroup` | DragAdapter |

## Delegation methods on DragAdapter

These methods remain on DragAdapter with one-line delegation bodies:

```java
// Event handler delegation
public void addListeners(Component c)    { eventHandler.addListeners(c); }
public void removeListeners(Component c) { eventHandler.removeListeners(c); }
public void clearMouseAndKeyboardState() {
    currentInputState.clearKeyState();
    currentInputState.clearMouseState();
    currentInputState.clearMouseWheelState();
    fireStateChange();
}

// Camera controller delegation
public void addCameraView(CameraView v, SymmetricPerspectiveCamera c) {
    cameraController.addCameraView(v, c);
}
public void addCameraView(CameraView v, SymmetricPerspectiveCamera main,
        SymmetricPerspectiveCamera layout, OrthographicCamera ortho) {
    cameraController.addCameraView(v, main, layout, ortho);
}
public void clearCameraViews()                        { cameraController.clearCameraViews(); }
public AbstractCamera getActiveCamera()               { return cameraController.getActiveCamera(); }
public void makeCameraActive(AbstractCamera camera)   { cameraController.makeCameraActive(camera); }
public void setCameraOnManipulator(CameraInformedManipulator m) {
    cameraController.setCameraOnManipulator(m);
}
```

## Enums and inner classes

| Type | Location after extraction | Reason |
| --- | --- | --- |
| `DragAdapter.ObjectType` | Stays on DragAdapter | Referenced by 10+ external files as `DragAdapter.ObjectType` |
| `DragAdapter.CameraView` | Stays on DragAdapter | Referenced by 25+ external files as `DragAdapter.CameraView` |
| `CameraSet` | Moves to DragCameraController | Was `private static final` — never referenced externally |

## Configuration

No configuration changes. No new dependencies. No build script changes.
Both new classes are in the same Maven module (`core/story-api`) and
same package (`org.alice.interact`).

## Validation

```bash
# Full compilation and test
mvn -pl core/story-api -am -DfailIfNoTests=false -Dcheckstyle.skip test

# Quick compilation check
mvn -pl core/story-api -am compile

# Run only the new characterization tests
mvn -pl core/story-api -Dtest="DragEventHandlerTest,DragCameraControllerTest" test
```

## Characterization tests

### DragEventHandlerTest

Located at `core/story-api/src/test/java/org/alice/interact/DragEventHandlerTest.java`.

Tests verify:

1. **Listener registration** — After `addListeners(component)`, the
   component has exactly one of each listener type. After
   `removeListeners(component)`, none remain.
2. **Idempotent registration** — Calling `addListeners` twice does not
   double-register.
3. **Mouse wheel timeout** — `updateMouseWheelTimeout` decrements the
   timeout and fires the state change callback when timeout expires.
4. **Mouse wheel stop on movement** — When `shouldStopMouseWheel` detects
   cursor movement beyond `CANCEL_MOUSE_WHEEL_DISTANCE`, the wheel state
   resets.

### DragCameraControllerTest

Located at `core/story-api/src/test/java/org/alice/interact/DragCameraControllerTest.java`.

Tests verify:

1. **Camera view registration** — After `addCameraView(MAIN, camera)`,
   `getActiveCamera()` returns the registered camera.
2. **Clear camera views** — After `clearCameraViews()`,
   `getActiveCamera()` returns `null`.
3. **Active camera selection** — `makeCameraActive(camera)` sets the
   active camera on the correct `CameraSet`.

## Acceptance criteria

1. `DragAdapter.java` is under 500 lines.
2. `mvn -pl core/story-api -am -DfailIfNoTests=false -Dcheckstyle.skip test` passes.
3. All five known subclasses compile without modification (verified by
   `-am` flag pulling in dependent modules).
4. No `public` or `protected` method signature on `DragAdapter` changes.
5. `DragAdapter.CameraView` and `DragAdapter.ObjectType` remain accessible
   at their original qualified names.
6. New characterization tests pass.

## Claim boundaries

This extraction does NOT:

- Change any manipulator logic (all `AbstractManipulator` subclasses
  are unmodified).
- Modify `InputState` or `ManipulatorConditionSet`.
- Change the `update()` hook's contract — subclasses still override
  `update(double)` on DragAdapter; the only change is that the mouse
  wheel timeout portion delegates to `eventHandler.updateMouseWheelTimeout()`.
- Touch any code outside `core/story-api`.
- Add any new public API surface beyond what DragAdapter already exposed.
- Move `fireStateChange()` — it remains on DragAdapter because four
  subclasses call it directly.
- Move `currentInputState` — it remains `protected final` on DragAdapter
  because `RuntimeDragAdapter` and other subclasses access it directly.
