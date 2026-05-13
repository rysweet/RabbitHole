# Validate DragAdapter Event and Camera Extraction

Use this guide to verify the extraction of `DragEventHandler` and
`DragCameraController` from `DragAdapter` (issue #566).

For the full contract, see the [DragAdapter Event and Camera Extraction
reference](../reference/drag-adapter-event-camera-extraction.md).

For the design walkthrough, see the
[Tutorial: Trace the DragAdapter Event and Camera Extraction](../tutorials/trace-drag-adapter-event-camera-extraction.md).

## When to use this guide

Use this guide when:

- Reviewing changes that extract event handling or camera management
  from `DragAdapter`
- Modifying `DragEventHandler` or `DragCameraController`
- Changing visibility of fields or methods in `DragAdapter` that the
  extracted classes depend on
- Adding new AWT listener types or camera view modes
- Modifying subclasses that override `handleMouseEntered`,
  `handleMouseMoved`, or `update`

Do not use this guide for manipulator logic changes, handle rendering,
selection behavior, or interaction group configuration. Those
responsibilities remain on `DragAdapter` and are not affected by this
extraction.

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Verify compilation

```bash
mvn -pl core/story-api -am compile
```

All 3 files (`DragAdapter.java`, `DragEventHandler.java`,
`DragCameraController.java`) must compile without errors.

## Step 2: Verify DragAdapter is under 500 lines

```bash
wc -l core/story-api/src/main/java/org/alice/interact/DragAdapter.java
```

Expected: under 500 lines. The extraction target is ~485 lines.

## Step 3: Verify subclass compilation

The `-am` flag in Step 1 compiles all upstream modules, but to
explicitly verify all five known subclasses compile:

```bash
mvn -pl core/story-api,core/ide,core-nonfree/ide-nonfree -am compile
```

These modules contain:
- `RuntimeDragAdapter` (core/story-api)
- `GlobalDragAdapter` (core/ide)
- `CroquetSupportingDragAdapter` (core/ide)
- `SingleViewerDragAdapter` (core/ide)
- `CreateAPersonDragAdapter` (core-nonfree/ide-nonfree)

## Step 4: Run characterization tests

```bash
mvn -pl core/story-api \
  -Dtest="DragEventHandlerTest,DragCameraControllerTest" test
```

Both test classes must pass. If either fails, check:

1. **DragEventHandlerTest failures** — likely a listener registration or
   mouse wheel timeout issue. Verify that `addListeners` and
   `removeListeners` correctly delegate and that `updateMouseWheelTimeout`
   decrements the timer.

2. **DragCameraControllerTest failures** — likely a camera registry
   issue. Verify that `addCameraView` stores cameras and
   `getActiveCamera` retrieves from the `MAIN` view.

## Step 5: Run full module test suite

```bash
mvn -pl core/story-api -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

This runs all tests in `core/story-api` plus all upstream dependencies.
No regressions should appear.

## Step 6: Verify no public API changes

Check that `DragAdapter` still exposes all original public and protected
methods. These must exist with unchanged signatures:

```bash
# Quick grep for the public delegation methods
grep -n "public void addListeners\|public void removeListeners\|public void addCameraView\|public void clearCameraViews\|public AbstractCamera getActiveCamera\|public void makeCameraActive\|public void setCameraOnManipulator\|public void clearMouseAndKeyboardState\|protected void handleMouseEntered\|protected void handleMouseMoved\|protected void update\|protected void fireStateChange" \
  core/story-api/src/main/java/org/alice/interact/DragAdapter.java
```

All listed methods must appear. `handleMouseEntered` and `handleMouseMoved`
must remain `protected` (not `public` or package-private) for subclass
overrides.

## Step 7: Verify enum accessibility

```bash
grep -rn "DragAdapter\.CameraView" core/ core-nonfree/ | head -5
grep -rn "DragAdapter\.ObjectType" core/ core-nonfree/ | head -5
```

Both enums must still be accessible as `DragAdapter.CameraView` and
`DragAdapter.ObjectType`. The extraction must not move them.

## Step 8: Verify new files are package-private

```bash
head -5 core/story-api/src/main/java/org/alice/interact/DragEventHandler.java
head -5 core/story-api/src/main/java/org/alice/interact/DragCameraController.java
```

Neither file should have `public class` — they should be `class`
(package-private) in the `org.alice.interact` package.

## Troubleshooting

### "cannot find symbol" for DragEventHandler or DragCameraController

Both files must be in `core/story-api/src/main/java/org/alice/interact/`.
Verify the package declaration matches `org.alice.interact`.

### Subclass override no longer fires

If a subclass's `handleMouseEntered` or `handleMouseMoved` override stops
being called, check that `DragEventHandler`'s AWT listener instances
route through `dragAdapter.handleMouseEntered(e)` and
`dragAdapter.handleMouseMoved(e)` rather than calling a local method.

### Mouse wheel stops working

Verify that `DragAdapter.update()` calls
`eventHandler.updateMouseWheelTimeout(timeDelta, this::fireStateChange)`.
The `Runnable` parameter must call `fireStateChange()` to propagate the
wheel-stop event to manipulators.

### Camera not found after clearCameraViews

Verify `DragAdapter.clear()` calls `cameraController.clearCameraViews()`
(not the old direct `this.clearCameraViews()` which may invoke a
deleted method body).
