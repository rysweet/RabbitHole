# Validate StorytellingSceneEditor Inner Class Extraction

Use this guide to verify the extraction of `SceneEditorDropReceptor`,
`LookingGlassPanel`, and `SceneEditorListeners` from
`StorytellingSceneEditor` (issue #528).

For the full contract, see the [StorytellingSceneEditor Inner Class
Extraction reference](../reference/storytelling-scene-editor-inner-class-extraction.md).

For the pre-extraction characterization baseline, see the
[StorytellingSceneEditor Characterization reference](../reference/storytelling-scene-editor-characterization.md).

## When to use this guide

Use this guide when:

- Reviewing changes that extract inner classes from
  `StorytellingSceneEditor`
- Modifying any of the 3 extracted files (`SceneEditorDropReceptor`,
  `LookingGlassPanel`, `SceneEditorListeners`)
- Changing visibility of fields or methods in `StorytellingSceneEditor`
  that the extracted classes depend on
- Adding or removing listeners in `SceneEditorListeners`
- Modifying the drop receptor's drag delegation logic

Do not use this guide for runtime scene rendering, drag-and-drop visual
behavior, camera navigation, or VR pipeline changes. Those behaviors
require a live render target and are outside this characterization lane.

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

All 4 files (`StorytellingSceneEditor.java` plus the 3 extracted class
files) must compile without errors.

## Step 2: Verify new file existence

```bash
ls core/ide/src/main/java/org/alice/stageide/sceneeditor/{SceneEditorDropReceptor,LookingGlassPanel,SceneEditorListeners}.java
```

All 3 files must exist.

## Step 3: Run the characterization suite

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

Expected: 81 test methods (80 original + 1 new `field_listeners`). All pass. 0 failures, 0 errors, 0 skipped.
BUILD SUCCESS.

This verifies:

- Inner class count is exactly 2 (`SingletonHolder`, `SceneEditorProgramImp`)
- `SceneEditorDropReceptor` exists as a top-level package-private class
- `LookingGlassPanel` exists as a top-level package-private class
- The `listeners` field exists on `StorytellingSceneEditor`
- All 37 public API methods are unchanged
- All 5 `RenderTargetListener` overrides are unchanged
- All 21 key fields still exist (minus 7 listeners, plus 1 `listeners`
  helper — net ≥ 20)
- `SceneEditorProgramImp.getAnimator()` is unchanged
- Singleton pattern is unchanged

## Step 4: Run the full module test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

This catches any compilation or linking errors that affect other tests
in `core/ide`.

## Step 5: Verify inner class count

```bash
grep -c 'private class\|private static class\|public static class' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: 2 (`SingletonHolder` and `SceneEditorProgramImp`).

## Step 6: Verify extracted class visibility

```bash
grep 'class SceneEditorDropReceptor' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorDropReceptor.java
grep 'class LookingGlassPanel' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/LookingGlassPanel.java
grep 'class SceneEditorListeners' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorListeners.java
```

All 3 must show `class` without `public`, `private`, or `protected`
prefix — confirming package-private visibility. No new public API surface.

## Step 7: Verify enclosing instance pattern

```bash
grep -n 'this.editor' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorDropReceptor.java \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorListeners.java
```

Both files should reference `this.editor` for back-references to
`StorytellingSceneEditor`.

```bash
grep -n 'this.onscreenRenderTarget' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/LookingGlassPanel.java
```

`LookingGlassPanel` should reference `this.onscreenRenderTarget` (not
`this.editor`).

## Step 8: Verify listener registration path

```bash
grep 'this.listeners\.' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: 7 matches (one per listener registration). Each should be of the
form `this.listeners.showSnapGridListener`, etc.

## Troubleshooting

### Compilation fails with "has private access"

A member of `StorytellingSceneEditor` that an extracted class accesses is
still `private`. Seven members must be widened to package-private:

| Member | Type | Accessed by |
| --- | --- | --- |
| `lookingGlassPanel` | field | `SceneEditorDropReceptor` |
| `globalDragAdapter` | field | `SceneEditorDropReceptor` |
| `selectionIsFromInstanceSelector` | field | `SceneEditorListeners` |
| `handleCameraMarkerFieldSelection` | method | `SceneEditorListeners` |
| `handleObjectMarkerFieldSelection` | method | `SceneEditorListeners` |
| `handleMainCameraViewSelection` | method | `SceneEditorListeners` |
| `setSelectedInstance` | method | `SceneEditorListeners` |

### Inner class count test fails

If `innerClassCount_exactly4` still expects 4, the test was not updated.
After extraction, the expected count is 2.

### Field count test fails

If `declaredFieldCount_atLeast20` fails, verify that the `listeners`
field was added to `StorytellingSceneEditor`. The 7 listener fields are
removed but 1 `listeners` field is added. Net field count should be ~25.

### NullPointerException in SceneEditorDropReceptor

If `editor.globalDragAdapter` throws NPE at runtime, the drop receptor
is being used before `initializeComponents()` has set `globalDragAdapter`
(line 648). The drop receptor accesses `globalDragAdapter` lazily through
the editor — this is by design because `globalDragAdapter` is `null` at
field-initialization time and remains `null` until `initializeComponents()`
runs.

### LookingGlassPanel createJPanel returns null

If `onscreenRenderTarget` is `null`, it was not passed to the
`LookingGlassPanel` constructor. Verify the instantiation site uses
`new LookingGlassPanel(onscreenRenderTarget)` and that the render target
field is initialized before `lookingGlassPanel`.

### Class not found in test reflection

If `Class.forName("org.alice.stageide.sceneeditor.SceneEditorDropReceptor")`
throws `ClassNotFoundException`, verify the file exists and compiles.
Check that the `core/ide` module compiles: `mvn -pl core/ide -am compile`.

## What this guide does NOT cover

- 3D scene rendering (render targets, camera views, OpenGL pipelines)
- Drag-and-drop visual behavior (ghost objects, drop previews)
- VR integration (`isVrActive`, VR scene setup)
- Object manipulation (handle visibility, snap grid visual behavior)
- Camera navigation (orbit, pan, zoom interaction)
- Runtime animation playback (`SceneEditorProgramImp.getAnimator()` behavior)
- Desktop UI layout, save workflows, or project IO
