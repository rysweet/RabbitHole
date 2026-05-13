# Validate SceneEditorFieldManager Extraction

Use this guide to verify the extraction of `SceneEditorFieldManager` and
`SceneRenderTargetListener` from `StorytellingSceneEditor` (issue #528,
continuation of PR #534).

For the full contract, see the [SceneEditorFieldManager Extraction
reference](../reference/scene-editor-field-manager-extraction.md).

For the pre-extraction baseline, see the [StorytellingSceneEditor
Characterization reference](../reference/storytelling-scene-editor-characterization.md)
and the [Inner Class Extraction reference](../reference/storytelling-scene-editor-inner-class-extraction.md).

## When to use this guide

Use this guide when:

- Reviewing changes that extract methods from `StorytellingSceneEditor`
  into `SceneEditorFieldManager` or `SceneRenderTargetListener`
- Modifying any of the extracted delegate classes
- Changing visibility of fields or methods in `StorytellingSceneEditor`
  that the delegate classes depend on
- Rewiring `SceneEditorListeners` lambdas to call through `fieldManager`
- Adding new public methods that should delegate to `fieldManager`
- Changing the render target listener registration in `initializeComponents`

Do not use this guide for runtime scene rendering, drag-and-drop visual
behavior, camera navigation interaction, or VR pipeline changes. Those
behaviors require a live render target and are outside this structural lane.

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Verify compilation

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip compile
```

All files in `org.alice.stageide.sceneeditor` must compile without errors.
This catches:
- Missing package-private visibility widenings
- Broken delegation calls (wrong method name or parameter types)
- Missing imports in the new delegate classes

## Step 2: Verify new file existence

```bash
ls core/ide/src/main/java/org/alice/stageide/sceneeditor/{SceneEditorFieldManager,SceneRenderTargetListener}.java
```

Both files must exist alongside the previously extracted files:

```bash
ls core/ide/src/main/java/org/alice/stageide/sceneeditor/{SceneEditorDropReceptor,LookingGlassPanel,SceneEditorListeners,SceneFieldCodeGenerator,SceneEditorFieldManager,SceneRenderTargetListener}.java
```

All 6 extracted/delegate files must exist.

## Step 3: Verify line count target

```bash
wc -l core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: under 700 lines. The extraction removes approximately 250 net
lines from the original 907-line file.

## Step 4: Verify SSE no longer implements RenderTargetListener

```bash
grep 'implements.*RenderTargetListener' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: no matches. The `RenderTargetListener` implementation is now on
`SceneRenderTargetListener`.

Verify the new class implements it:

```bash
grep 'implements.*RenderTargetListener' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneRenderTargetListener.java
```

Expected: one match.

## Step 5: Verify delegation pattern on SSE

```bash
grep -c 'fieldManager\.' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: at least 28 delegation calls. The 16 thin stub methods each call
`fieldManager.xxx()`, plus 8 code generation redirects, plus call-site
updates in `initializeComponents`, `setSelectedField`, `setSelectedExpression`,
`setActiveScene`, and `addField`.

Verify no stale `codeGenerator` references remain on SSE:

```bash
grep 'codeGenerator\.' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: no matches. All code generation calls now go through
`fieldManager`.

## Step 6: Verify SceneEditorListeners rewiring

```bash
grep 'fieldManager\.' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorListeners.java
```

Expected: at least 4 matches — one for each rewired lambda:
- `editor.fieldManager.handleCameraMarkerFieldSelection`
- `editor.fieldManager.handleObjectMarkerFieldSelection`
- `editor.fieldManager.setSelectedInstance`
- `editor.fieldManager.handleMainCameraViewSelection`

The snap grid listeners should still call `editor.setShowSnapGrid` and
`editor.setSnapGridSpacing` (these methods stay on SSE):

```bash
grep 'editor\.set\(ShowSnapGrid\|SnapGridSpacing\)' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorListeners.java
```

Expected: 3 matches (two for `setShowSnapGrid`, one for
`setSnapGridSpacing`).

## Step 7: Verify extracted class visibility

```bash
grep 'class SceneEditorFieldManager' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorFieldManager.java
grep 'class SceneRenderTargetListener' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneRenderTargetListener.java
```

Both must show `class` without `public`, `private`, or `protected` prefix —
confirming package-private visibility. No new public API surface.

## Step 8: Verify render target registration

```bash
grep 'addRenderTargetListener' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: one match containing `this.renderTargetListener` (not `this`).

## Step 9: Run the characterization suite

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

All tests must pass. The 37 `api_*` tests verify that every public method
signature is preserved on SSE.

## Step 10: Run the full module test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

This is the definitive validation. All existing tests must pass, confirming
that the extraction is behavior-preserving.

## Troubleshooting

### Compilation fails with "has private access"

A member of `StorytellingSceneEditor` that an extracted class accesses is
still `private`. Members that must be widened to package-private:

| Member | Type | Accessed by |
| --- | --- | --- |
| `onscreenRenderTarget` | field | `SceneEditorFieldManager`, `SceneRenderTargetListener` |
| `mainCameraNavigatorWidget` | field | `SceneEditorFieldManager` |
| `orthographicCameraImp` | field | `SceneEditorFieldManager` |
| `automaticDisplayListener` | field | `SceneEditorFieldManager` (`handleShowing`/`handleHiding`) |
| `movableSceneCameraImp` | field | `SceneEditorFieldManager` (`getTransformForNewCameraMarker`) |
| `getPropertyPanel()` | method | `SceneEditorFieldManager` (`setSelectedInstance`) |
| `lookingGlassPanel` | field | `SceneEditorFieldManager` (also needed by `SceneEditorDropReceptor` from PR #534) |
| `globalDragAdapter` | field | `SceneEditorFieldManager` (also needed by `SceneEditorDropReceptor` from PR #534) |
| `selectionIsFromInstanceSelector` | field | `SceneEditorListeners` (unchanged from PR #534) |

### Compilation fails with "cannot find symbol codeGenerator"

Stale references to `codeGenerator` remain in SSE. All code generation
calls must go through `fieldManager`:

```java
// Wrong (stale reference):
return codeGenerator.getDoStatementsForAddField(field, initialTransform);

// Correct (through fieldManager):
return fieldManager.getDoStatementsForAddField(field, initialTransform);
```

### NullPointerException in SceneEditorFieldManager

If `editor.globalDragAdapter` throws NPE at runtime, the field manager is
being used before `initializeComponents()` has set `globalDragAdapter`.
Methods that access `globalDragAdapter` must guard against null:

```java
if (editor.globalDragAdapter != null) { ... }
```

This is the same pattern used in the original SSE methods.

### RenderTargetListener callbacks not firing

If horizon line painting or other render callbacks stop working:

1. Verify the `renderTargetListener` field is instantiated on SSE.
2. Verify `initializeComponents()` registers `this.renderTargetListener`
   (not `this`) with the render target.
3. Verify `SceneRenderTargetListener` implements all 5 interface methods.

### Line count still over 700

If SSE is still over 700 lines after extraction, check for:

1. Methods that should have moved entirely but still have bodies on SSE.
2. Methods that should be thin stubs but still have multi-line bodies.
3. Stale private helper methods that are no longer called from SSE.
4. Import statements for types only used by extracted classes — these
   should be removed.

Run `grep -c 'private.*void\|private.*Statement\|private.*AffineMatrix' StorytellingSceneEditor.java`
to find private methods that may be candidates for removal.

### Characterization test RenderTargetListener assertions fail

After extraction, SSE no longer implements `RenderTargetListener`.
Update or remove these tests:

- `implementsRenderTargetListener` → update to assert NOT in interfaces,
  or add an assertion that `SceneRenderTargetListener` implements it
- `renderTargetListener_*` → remove from SSE characterization, or add
  equivalent tests for `SceneRenderTargetListener`

### Field count assertion fails

The `codeGenerator` field moves from SSE to `SceneEditorFieldManager`,
and a `fieldManager` field is added to SSE. If there was a `field_codeGenerator`
test, update it to `field_fieldManager`. The aggregate
`declaredFieldCount_atLeast20` guard may need its threshold adjusted if
enough fields were removed.

## What this guide does NOT cover

- 3D scene rendering (render targets, camera views, OpenGL pipelines)
- Drag-and-drop visual behavior (ghost objects, drop previews)
- VR integration (`isVrActive`, VR scene setup)
- Object manipulation (handle visibility, snap grid visual behavior)
- Camera navigation interaction (orbit, pan, zoom)
- Runtime animation playback (`SceneEditorProgramImp.getAnimator()`)
- Desktop UI layout, save workflows, or project IO
- The previous inner class extraction (PR #534) — see
  [validate-storytelling-scene-editor-inner-class-extraction](validate-storytelling-scene-editor-inner-class-extraction.md)
