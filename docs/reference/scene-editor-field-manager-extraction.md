# SceneEditorFieldManager and SceneRenderTargetListener Extraction

This reference documents the extraction of field management, scene
interaction, and render target callback methods from
`StorytellingSceneEditor` into two new delegate classes (issue #528,
continuation of PR #534).

## Contents

- [Motivation](#motivation)
- [Extracted classes](#extracted-classes)
- [File inventory](#file-inventory)
- [Delegation pattern](#delegation-pattern)
- [Ownership changes](#ownership-changes)
- [Visibility changes](#visibility-changes)
- [SceneEditorListeners rewiring](#sceneeditorlisteners-rewiring)
- [Methods that move entirely](#methods-that-move-entirely)
- [Methods that become thin stubs](#methods-that-become-thin-stubs)
- [Test updates](#test-updates)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Motivation

After PR #534 extracted 3 inner classes (`SceneEditorDropReceptor`,
`LookingGlassPanel`, `SceneEditorListeners`) and `SceneFieldCodeGenerator`,
`StorytellingSceneEditor.java` was still 907 lines — well above the 700-line
target. Two cohesive groups of methods remained as extraction candidates:

| Extraction | Lines removed | Purpose |
| --- | --- | --- |
| `SceneEditorFieldManager` | ~195 | Selection/manipulator wiring, camera switching, marker handling, right-click context menu, show/hide lifecycle, and public accessor helpers. Also takes ownership of `SceneFieldCodeGenerator`. |
| `SceneRenderTargetListener` | ~55 | `RenderTargetListener` implementation (5 callbacks + `paintHorizonLine` helper). |

After extraction, `StorytellingSceneEditor.java` drops from 907 to
approximately 655 lines — safely under the 700-line target. The public
API surface is unchanged: all 37 public methods remain on SSE as thin
delegation stubs or unchanged methods. No external caller changes.

## Extracted classes

### SceneEditorFieldManager

| Property | Value |
| --- | --- |
| New file | `SceneEditorFieldManager.java` |
| Visibility | Package-private (no access modifier) |
| Constructor | `SceneEditorFieldManager(StorytellingSceneEditor editor)` |
| Back-reference field | `editor` (`StorytellingSceneEditor`) |
| Owned delegate | `codeGenerator` (`SceneFieldCodeGenerator`) — moved from SSE |
| Approximate lines | ~210 |

This class consolidates scene interaction concerns that were scattered
across `StorytellingSceneEditor`:

**Selection and manipulator wiring:**

| Method | Visibility | Purpose |
| --- | --- | --- |
| `setSelectedFieldOnManipulator` | package-private | Sets the drag adapter's selected implementation for a given field. |
| `setSelectedExpressionOnManipulator` | package-private | Sets the drag adapter's selected implementation for an expression. |
| `handleManipulatorSelection` | package-private | Responds to 3D viewport selection events — routes to `setSelectedField`, `setSelectedCameraMarker`, or `setSelectedObjectMarker`. |
| `setSelectedInstance` | package-private | Resolves an `InstanceFactory` to a field or expression selection. Called from `SceneEditorListeners`. |

**Camera switching:**

| Method | Visibility | Purpose |
| --- | --- | --- |
| `switchToCamera` | private | Core camera switch — clears render target, adds new camera, updates snap grid and drag adapter. |
| `switchToOrthographicCamera` | package-private | Switches to the orthographic (top-down) camera and updates the navigator widget. |
| `switchToPerspectiveCamera` | package-private | Switches to a perspective camera and updates the navigator widget. |

**Marker handling:**

| Method | Visibility | Purpose |
| --- | --- | --- |
| `handleCameraMarkerFieldSelection` | package-private | Responds to camera marker list selection — updates drag adapter and move operations. |
| `handleObjectMarkerFieldSelection` | package-private | Responds to object marker list selection — updates drag adapter and move operations. |
| `setSelectedObjectMarker` | package-private | Programmatically selects an object marker in the side panel list. |
| `setSelectedCameraMarker` | package-private | Programmatically selects a camera marker in the side panel list. |
| `handleMainCameraViewSelection` | package-private | Updates the instance factory state when the camera view combo box changes. |

**Right-click context menu:**

| Method | Visibility | Purpose |
| --- | --- | --- |
| `showRightClickMenuForModel` | package-private | Shows the one-shot context menu for a right-clicked 3D model. |

**Show/hide lifecycle:**

| Method | Visibility | Purpose |
| --- | --- | --- |
| `showLookingGlassPanel` | package-private | Adds the looking glass panel to the scene editor (synchronized on AWT tree lock via editor). |
| `hideLookingGlassPanel` | package-private | Removes the looking glass panel from the scene editor (synchronized on AWT tree lock via editor). |
| `handleShowing` | package-private | Increments automatic display count and shows the looking glass panel. |
| `handleHiding` | package-private | Hides the looking glass panel and decrements automatic display count. |

**Rendering control:**

| Method | Visibility | Purpose |
| --- | --- | --- |
| `enableRendering` | package-private | Enables rendering for specific disable reasons. |
| `disableRendering` | package-private | Disables rendering for specific disable reasons. |
| `preScreenCapture` | package-private | Hides drag handles before screen capture. |
| `postScreenCapture` | package-private | Restores drag handles after screen capture. |
| `setHandleVisibilityForObject` | package-private | Shows or hides manipulation handles for a specific object. |

**Camera/marker accessor helpers:**

| Method | Visibility | Purpose |
| --- | --- | --- |
| `getTransformForNewCameraMarker` | package-private | Returns the current camera transform for creating a new camera marker. |
| `getTransformForNewObjectMarker` | package-private | Returns the selected object's transform (or identity) for creating a new object marker. |
| `getColorForNewObjectMarker` | package-private | Returns the next color for a new object marker. |
| `getColorForNewCameraMarker` | package-private | Returns the next color for a new camera marker. |
| `getGoodPointOfViewInSceneForObject` | package-private | Returns a good camera point of view for an object's bounding box (currently throws `RuntimeException("todo")`). |
| `getMarkerForField` | package-private | Returns the `MarkerImp` for a given field, or `null` if the field is not a marker. |

**Code generation delegates (forwarded to SceneFieldCodeGenerator):**

| Method | Visibility | Purpose |
| --- | --- | --- |
| `getCurrentStateCodeForField` | package-private | Delegates to `codeGenerator.getCurrentStateCodeForField()`. |
| `generateCodeForSetUp` | package-private | Delegates to `codeGenerator.generateCodeForSetUp()`. |
| `getDoStatementsForCopyField` | package-private | Delegates to `codeGenerator.getDoStatementsForCopyField()`. |
| `getDoStatementsForAddField` | package-private | Delegates to `codeGenerator.getDoStatementsForAddField()`. |
| `getUndoStatementsForAddField` | package-private | Delegates to `codeGenerator.getUndoStatementsForAddField()`. |
| `getRiders` | package-private | Delegates to `codeGenerator.getRiders()`. |
| `getDoStatementsForRemoveField` | package-private | Delegates to `codeGenerator.getDoStatementsForRemoveField()`. |
| `getUndoStatementsForRemoveField` | package-private | Delegates to `codeGenerator.getUndoStatementsForRemoveField()`. |

### SceneRenderTargetListener

| Property | Value |
| --- | --- |
| New file | `SceneRenderTargetListener.java` |
| Visibility | Package-private (no access modifier) |
| Implements | `edu.cmu.cs.dennisc.render.event.RenderTargetListener` |
| Constructor | `SceneRenderTargetListener(StorytellingSceneEditor editor)` |
| Back-reference field | `editor` (`StorytellingSceneEditor`) |
| Approximate lines | ~60 |

This class contains the five `RenderTargetListener` callbacks and the
`paintHorizonLine` helper that were previously implemented directly on
`StorytellingSceneEditor`.

| Method | Visibility | Purpose |
| --- | --- | --- |
| `initialized` | public | No-op callback. Required by `RenderTargetListener`. |
| `cleared` | public | No-op callback. Required by `RenderTargetListener`. |
| `rendered` | public | Paints the horizon line when the active camera is orthographic. |
| `resized` | public | No-op callback. Required by `RenderTargetListener`. |
| `displayChanged` | public | No-op callback. Required by `RenderTargetListener`. |
| `paintHorizonLine` | private | Draws a horizontal line at the camera's horizon position on the orthographic viewport. |

**Why a separate class rather than adding to SceneEditorFieldManager:**
The `RenderTargetListener` callbacks are a render-loop concern, not a field
management concern. Mixing them with selection, camera switching, and marker
handling would create a class with two unrelated responsibilities.

## File inventory

| File | Role | Approx lines |
| --- | --- | --- |
| `StorytellingSceneEditor.java` | Parent class — thin delegation stubs for public API. Owns `fieldManager` and `renderTargetListener`. | ~655 |
| `SceneEditorFieldManager.java` | Selection, camera switching, marker handling, lifecycle, rendering control, and code generation delegation. | ~210 |
| `SceneRenderTargetListener.java` | Render target callbacks and horizon line painting. | ~60 |
| `SceneFieldCodeGenerator.java` | Field code generation (unchanged — now owned by `SceneEditorFieldManager`). | ~305 |
| `SceneEditorDropReceptor.java` | Gallery drag-and-drop (unchanged from PR #534). | ~139 |
| `LookingGlassPanel.java` | Render target panel wrapper (unchanged from PR #534). | ~76 |
| `SceneEditorListeners.java` | Listener consolidation (rewired to call `fieldManager` for 4 lambdas). | ~86 |

All source files reside in:

```text
core/ide/src/main/java/org/alice/stageide/sceneeditor/
```

## Delegation pattern

`StorytellingSceneEditor` retains all public method signatures. Each
extracted method becomes a one-line delegation stub:

```java
// Before (multi-line body on SSE):
public void switchToOrthographicCamera() {
    switchToCamera(orthographicCameraImp.getSgCamera());
    mainCameraNavigatorWidget.setToOrthographicMode();
}

// After (thin delegation stub on SSE):
public void switchToOrthographicCamera() {
    fieldManager.switchToOrthographicCamera();
}
```

Override methods follow the same pattern:

```java
// Before:
@Override
public Statement[] getDoStatementsForCopyField(UserField fieldToCopy, UserField newField, AffineMatrix4x4 initialTransform) {
    return codeGenerator.getDoStatementsForCopyField(fieldToCopy, newField, initialTransform);
}

// After:
@Override
public Statement[] getDoStatementsForCopyField(UserField fieldToCopy, UserField newField, AffineMatrix4x4 initialTransform) {
    return fieldManager.getDoStatementsForCopyField(fieldToCopy, newField, initialTransform);
}
```

Private and package-private methods that have no external callers outside
the `sceneeditor` package move entirely — no stub is left on SSE:

```java
// Before (on SSE):
private void setSelectedFieldOnManipulator(UserField field) { ... }

// After: method body is on SceneEditorFieldManager
// SSE has no trace of this method
```

## Ownership changes

### SceneFieldCodeGenerator ownership transfer

`SceneFieldCodeGenerator` was previously instantiated and owned directly
by `StorytellingSceneEditor`:

```java
// Before (on SSE):
private final SceneFieldCodeGenerator codeGenerator = new SceneFieldCodeGenerator(this);
```

After extraction, `SceneEditorFieldManager` owns `codeGenerator`:

```java
// On SceneEditorFieldManager:
private final SceneFieldCodeGenerator codeGenerator;

SceneEditorFieldManager(StorytellingSceneEditor editor) {
    this.editor = editor;
    this.codeGenerator = new SceneFieldCodeGenerator(editor);
}
```

`StorytellingSceneEditor` no longer has a `codeGenerator` field. All code
generation requests flow through `fieldManager`.

### RenderTargetListener implementation transfer

`StorytellingSceneEditor` no longer implements `RenderTargetListener`
directly. The render target registration changes:

```java
// Before:
this.onscreenRenderTarget.addRenderTargetListener(this);

// After:
this.onscreenRenderTarget.addRenderTargetListener(this.renderTargetListener);
```

SSE's class declaration changes from:

```java
public class StorytellingSceneEditor extends AbstractSceneEditor implements RenderTargetListener
```

to:

```java
public class StorytellingSceneEditor extends AbstractSceneEditor
```

## Visibility changes

Six additional members of `StorytellingSceneEditor` are widened from
`private` to package-private to allow cross-file access. These are in
addition to the 7 members widened in PR #534.

### Fields (3 additional)

| Field | Type | Accessed by |
| --- | --- | --- |
| `onscreenRenderTarget` | `OnscreenRenderTarget` | `SceneEditorFieldManager` (for rendering control), `SceneRenderTargetListener` (for horizon line painting) |
| `mainCameraNavigatorWidget` | `CameraNavigatorWidget` | `SceneEditorFieldManager` (for camera mode switching) |
| `orthographicCameraImp` | `OrthographicCameraImp` | `SceneEditorFieldManager` (for camera switching) |

### Methods (3 additional)

| Method | Signature | Accessed by |
| --- | --- | --- |
| `getSelectedField` | `UserField getSelectedField()` | `SceneEditorFieldManager` (inherited from `AbstractSceneEditor`, already protected — no change needed) |
| `getImplementation` | `<T extends EntityImp> T getImplementation(AbstractField)` | `SceneEditorFieldManager` (inherited from `AbstractSceneEditor`, already protected — no change needed) |
| `revalidateAndRepaint` | `void revalidateAndRepaint()` | `SceneEditorFieldManager` (inherited from `AwtComponentView`, already public — no change needed) |

**Note:** Most methods that `SceneEditorFieldManager` calls on the editor
are already public or protected on `AbstractSceneEditor`. The 3 field
visibility widenings are the only new package-private exposures.

No member is widened beyond package-private. All extracted classes are in
the same package (`org.alice.stageide.sceneeditor`).

## SceneEditorListeners rewiring

Four lambdas in `SceneEditorListeners` change from calling `editor.xxx()`
to calling `editor.fieldManager.xxx()`:

| Listener field | Before | After |
| --- | --- | --- |
| `cameraMarkerFieldSelectionListener` | `editor.handleCameraMarkerFieldSelection(value)` | `editor.fieldManager.handleCameraMarkerFieldSelection(value)` |
| `objectMarkerFieldSelectionListener` | `editor.handleObjectMarkerFieldSelection(value)` | `editor.fieldManager.handleObjectMarkerFieldSelection(value)` |
| `instanceFactorySelectionListener` | `editor.setSelectedInstance(value)` | `editor.fieldManager.setSelectedInstance(value)` |
| `mainCameraViewSelectionObserver` | `editor.handleMainCameraViewSelection()` | `editor.fieldManager.handleMainCameraViewSelection()` |

The `selectionIsFromInstanceSelector` flag access changes accordingly:

```java
// Before:
editor.selectionIsFromInstanceSelector = true;
editor.setSelectedInstance(e.getNextValue());
editor.selectionIsFromInstanceSelector = false;

// After:
editor.selectionIsFromInstanceSelector = true;
editor.fieldManager.setSelectedInstance(e.getNextValue());
editor.selectionIsFromInstanceSelector = false;
```

The three snap grid listeners are unchanged — they call methods
(`setShowSnapGrid`, `setSnapGridSpacing`) that remain on SSE because the
`snapGrid` field stays on SSE (it is used in `setActiveScene` and
`initializeComponents`).

## Methods that move entirely

These 11 private/package-private methods move from SSE to
`SceneEditorFieldManager` with no stub left on SSE. Callers within the
`sceneeditor` package update to use `fieldManager.xxx()` or
`editor.fieldManager.xxx()`.

| Method | Former visibility | Lines removed from SSE |
| --- | --- | --- |
| `setSelectedFieldOnManipulator(UserField)` | private | ~13 |
| `setSelectedExpressionOnManipulator(Expression)` | private | ~13 |
| `setSelectedInstance(InstanceFactory)` | package-private | ~21 |
| `handleManipulatorSelection(SelectionEvent)` | private | ~21 |
| `showRightClickMenuForModel(InputState)` | private | ~21 |
| `switchToCamera(AbstractCamera)` | private | ~11 |
| `handleCameraMarkerFieldSelection(UserField)` | package-private | ~6 |
| `handleObjectMarkerFieldSelection(UserField)` | package-private | ~6 |
| `setSelectedCameraMarker(UserField)` | private | ~4 |
| `handleMainCameraViewSelection()` | package-private | ~8 |
| `showLookingGlassPanel()` | private | ~5 |
| `hideLookingGlassPanel()` | private | ~5 |

Total: ~134 lines removed (no stubs).

## Methods that become thin stubs

These 15 public/override methods remain on SSE as one-line delegation
stubs. The method body moves to `SceneEditorFieldManager`.

| Method | Lines saved | SSE stub body |
| --- | --- | --- |
| `switchToOrthographicCamera()` | ~3 | `fieldManager.switchToOrthographicCamera()` |
| `switchToPerspectiveCamera(AbstractCamera)` | ~3 | `fieldManager.switchToPerspectiveCamera(sgCamera)` |
| `setSelectedObjectMarker(UserField)` | ~3 | `fieldManager.setSelectedObjectMarker(field)` |
| `handleShowing()` | ~5 | `fieldManager.handleShowing()` |
| `handleHiding()` | ~5 | `fieldManager.handleHiding()` |
| `enableRendering(...)` | ~5 | `fieldManager.enableRendering(reason)` |
| `disableRendering(...)` | ~5 | `fieldManager.disableRendering(reason)` |
| `preScreenCapture()` | ~3 | `fieldManager.preScreenCapture()` |
| `postScreenCapture()` | ~3 | `fieldManager.postScreenCapture()` |
| `setHandleVisibilityForObject(...)` | ~2 | `fieldManager.setHandleVisibilityForObject(imp, b)` |
| `getTransformForNewCameraMarker()` | ~2 | `fieldManager.getTransformForNewCameraMarker()` |
| `getTransformForNewObjectMarker()` | ~9 | `fieldManager.getTransformForNewObjectMarker()` |
| `getColorForNewObjectMarker()` | ~2 | `fieldManager.getColorForNewObjectMarker()` |
| `getColorForNewCameraMarker()` | ~2 | `fieldManager.getColorForNewCameraMarker()` |
| `getGoodPointOfViewInSceneForObject(...)` | ~2 | `fieldManager.getGoodPointOfViewInSceneForObject(box)` |
| `getMarkerForField(UserField)` | ~6 | `fieldManager.getMarkerForField(field)` |

Total: ~60 lines saved via thinning.

Additionally, the `RenderTargetListener` callbacks (~19 lines) and the
`paintHorizonLine` helper (~23 lines) move to `SceneRenderTargetListener`,
and the `codeGenerator` field declaration is removed.

## Test updates

The characterization test suite (`StorytellingSceneEditorCharacterizationTest`)
requires updates to reflect the structural changes. All existing public API
tests continue to pass because the public method signatures are unchanged.

### Tests that change

| Test | Before | After | Reason |
| --- | --- | --- | --- |
| `implementsRenderTargetListener` | Asserts `RenderTargetListener` in interfaces | Asserts `RenderTargetListener` is NOT in interfaces (or test removed) | SSE no longer directly implements the interface. |
| `renderTargetListener_initialized` | Asserts method on SSE | Removed or updated | Method is on `SceneRenderTargetListener`. |
| `renderTargetListener_cleared` | Asserts method on SSE | Removed or updated | Method is on `SceneRenderTargetListener`. |
| `renderTargetListener_rendered` | Asserts method on SSE | Removed or updated | Method is on `SceneRenderTargetListener`. |
| `renderTargetListener_resized` | Asserts method on SSE | Removed or updated | Method is on `SceneRenderTargetListener`. |
| `renderTargetListener_displayChanged` | Asserts method on SSE | Removed or updated | Method is on `SceneRenderTargetListener`. |
| `declaredFieldCount_atLeast20` | Expects ≥ 20 | Threshold may lower to ≥ 15 | `codeGenerator` field removed; several fields may widen or shift. |
| `field_codeGenerator` (if present) | Asserts `codeGenerator` field exists | Removed or renamed to `fieldManager` | `codeGenerator` is now owned by `SceneEditorFieldManager`. |

### Tests that do NOT change

- All 37 `api_*` tests — all public method signatures are preserved as
  thin delegation stubs.
- `extendsAbstractSceneEditor` — superclass unchanged.
- `hasPrivateConstructor` — singleton pattern unchanged.
- `hasGetInstanceMethod` — unchanged.
- `getInstanceReturnsOwnType` — unchanged.
- `innerClassCount_exactly2` — unchanged (still `SingletonHolder` and
  `SceneEditorProgramImp`).
- `sceneEditorProgramImp_hasGetAnimator` — unchanged.
- `publicMethodCount_atLeast35` — all 37 public methods remain.

## Validation commands

### Run the full core/ide test suite

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

This is the primary validation command. All existing tests must pass.

### Run the characterization suite only

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dcheckstyle.skip \
  -Dtest=StorytellingSceneEditorCharacterizationTest \
  test
```

### Verify new file existence

```bash
ls core/ide/src/main/java/org/alice/stageide/sceneeditor/{SceneEditorFieldManager,SceneRenderTargetListener}.java
```

Both files must exist.

### Verify line count target

```bash
wc -l core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: under 700 lines.

### Verify no direct RenderTargetListener implementation

```bash
grep 'implements.*RenderTargetListener' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: no matches. SSE no longer directly implements the interface.

### Verify delegation pattern

```bash
grep 'fieldManager\.' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java | wc -l
```

Expected: at least 15 delegation calls (one per thin stub + code generation
delegates).

## Compatibility rules

1. **Do not widen visibility beyond package-private.** The 2 extracted
   classes and all widened members must remain package-private. No new
   public API surface is introduced by this extraction.

2. **Do not remove public method signatures from SSE.** All 37 public
   methods remain as thin delegation stubs. External callers (outside the
   `sceneeditor` package) continue to call `StorytellingSceneEditor`
   methods. They are unaware of the `SceneEditorFieldManager` delegate.

3. **synchronized(this.getTreeLock()) blocks must stay on SSE.** The
   `showLookingGlassPanel()` and `hideLookingGlassPanel()` methods
   synchronize on the AWT tree lock, which belongs to the `Component`
   (i.e., `StorytellingSceneEditor`). The `SceneEditorFieldManager` methods
   call back to the editor to acquire the tree lock:
   `synchronized (editor.getTreeLock()) { ... }`.

4. **Do not pass `globalDragAdapter` to `SceneEditorFieldManager`'s
   constructor.** Like `SceneEditorDropReceptor`, the field manager must
   lazy-access `globalDragAdapter` through the editor reference because
   it is `null` until `initializeComponents()` runs.

5. **`SceneFieldCodeGenerator` constructor signature is unchanged.** It
   still takes `StorytellingSceneEditor` as its constructor argument. The
   only change is who creates it — `SceneEditorFieldManager` instead of
   SSE.

6. **The `initializeComponents()` render target registration updates.**
   The call `this.onscreenRenderTarget.addRenderTargetListener(this)` changes
   to `this.onscreenRenderTarget.addRenderTargetListener(this.renderTargetListener)`.
   The registration site remains in SSE's `initializeComponents()`.

7. **The anonymous `SelectionListener` and `ManipulatorClickAdapter` in
   `initializeComponents()` update.** These anonymous inner classes call
   `handleManipulatorSelection` and `showRightClickMenuForModel`, which
   are now on `fieldManager`:

   ```java
   // Before:
   StorytellingSceneEditor.this.handleManipulatorSelection(e);
   // After:
   StorytellingSceneEditor.this.fieldManager.handleManipulatorSelection(e);
   ```

8. **The `selectionIsFromInstanceSelector` flag remains on SSE.** It is
   set by `SceneEditorListeners` and read by `setSelectedField` (which
   stays on SSE). The flag is not moved to `SceneEditorFieldManager`.

9. **`setSelectedField` override stays on SSE.** It is a public override
   of `AbstractSceneEditor.setSelectedField()` that interacts with the
   instance factory state, selection flags, and UI refresh. It calls
   `fieldManager.setSelectedFieldOnManipulator(field)` for the
   drag-adapter wiring.

10. **`setActiveScene` stays on SSE.** It is a 95-line protected override
    tightly coupled to SSE's initialization state (`sceneCameraImp`,
    `movableSceneCameraImp`, `orthographicCameraImp`, `globalDragAdapter`,
    `snapGrid`, `mainCameraViewTracker`). Extracting it would require
    exposing too many SSE internals. It calls helpers on `fieldManager`
    for marker-related operations.

11. **`addField` override stays on SSE.** It calls `super.addField()` and
    interacts with SSE state (marker display, VR flag, code state
    initialization). It remains on SSE.

12. **The singleton pattern is unchanged.** `SingletonHolder` remains a
    private static inner class. `getInstance()` continues to use the lazy
    initialization holder idiom.

## Examples

### Verify the extraction compiles

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip compile
```

If a private member was not widened to package-private, compilation fails
with "has private access in StorytellingSceneEditor". Check the
[Visibility changes](#visibility-changes) table.

### Add a new camera-related public method to SSE

1. Add the method signature on `StorytellingSceneEditor` as a thin
   delegation stub: `return fieldManager.newMethod()`.
2. Add the method body on `SceneEditorFieldManager`.
3. Run the characterization suite — all tests pass (additive change).
4. Add a new `api_*` test for the method.

### Understand why selection methods split between SSE and fieldManager

`setSelectedField` (the public override) stays on SSE because it:
- Calls `super.setSelectedField()` on `AbstractSceneEditor`
- Interacts with the `selectionIsFromInstanceSelector` flag (SSE state)
- Updates the instance factory state via `StageIDE`
- Triggers UI refresh via `SwingUtilities.invokeLater`

`setSelectedFieldOnManipulator` (private helper) moves to
`SceneEditorFieldManager` because it:
- Only interacts with `globalDragAdapter` (which fieldManager accesses
  via `editor.globalDragAdapter`)
- Has no dependency on SSE's selection flags or UI refresh

The split follows the principle: keep framework-facing overrides on the
owning class, extract implementation helpers to the delegate.

### Understand why SceneRenderTargetListener is separate from SceneEditorFieldManager

The render target callbacks fire on every frame render and deal with
pixel-level operations (horizon line painting). Field management deals with
AST operations (code generation, statement construction) and UI interaction
(selection, camera switching, marker handling). These are separate concerns
with different change frequencies. Combining them would create a god-class
with two unrelated responsibilities.

### Trace the delegation chain for getDoStatementsForCopyField

```text
External caller
  → SSE.getDoStatementsForCopyField(fieldToCopy, newField, initialTransform)
    → fieldManager.getDoStatementsForCopyField(fieldToCopy, newField, initialTransform)
      → codeGenerator.getDoStatementsForCopyField(fieldToCopy, newField, initialTransform)
```

The extra indirection adds one method call. This is acceptable because:
- These methods are called during user actions (add/remove/copy field),
  not in tight loops.
- The delegation keeps SSE's method count at 37 (unchanged public API)
  while allowing `SceneFieldCodeGenerator` to be an implementation detail
  of `SceneEditorFieldManager`.
