# StorytellingSceneEditor Inner Class Extraction

This reference documents the extraction of 2 inner classes and 7 anonymous
listener fields from `StorytellingSceneEditor` into 3 separate top-level
package-private files in `org.alice.stageide.sceneeditor` (issue #528).

## Contents

- [Motivation](#motivation)
- [Extracted classes](#extracted-classes)
- [File inventory](#file-inventory)
- [Enclosing instance pattern](#enclosing-instance-pattern)
- [Visibility changes](#visibility-changes)
- [Listener registration site changes](#listener-registration-site-changes)
- [Test updates](#test-updates)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Motivation

`StorytellingSceneEditor.java` is a 1259-line singleton class with 4 inner
classes and 7 anonymous listener fields. Issue #528 extracts 3 cohesive
groups into their own top-level files to reduce cognitive load and prepare
for further decomposition:

| Extraction | Lines removed | Purpose |
| --- | --- | --- |
| `SceneEditorDropReceptor` (inner class) | ~70 | Gallery drag-and-drop onto the 3D viewport. |
| `LookingGlassPanel` (inner class) | ~15 | Custom `JPanel` wrapping the OpenGL render target. |
| `SceneEditorListeners` (new helper) | ~48 | Consolidates 7 `ValueListener` fields for snap grid, markers, instance factory, and camera view. |

After extraction, `StorytellingSceneEditor.java` drops from 1259 to
approximately 1126 lines. Two inner classes remain: `SingletonHolder`
(private static, singleton pattern) and `SceneEditorProgramImp` (public
static, animation engine bridge).

## Extracted classes

### SceneEditorDropReceptor

| Property | Value |
| --- | --- |
| Original location | Lines 129–198 (private inner class) |
| New file | `SceneEditorDropReceptor.java` |
| Visibility | Package-private (no access modifier) |
| Superclass | `AbstractDropReceptor` |
| Constructor | `SceneEditorDropReceptor(StorytellingSceneEditor editor)` |
| Back-reference field | `editor` (`StorytellingSceneEditor`) |

The class handles gallery drag-and-drop onto the 3D scene viewport. It
overrides 9 methods — 7 for the drag lifecycle and 2 that return
references to the enclosing editor:

| Method | Purpose |
| --- | --- |
| `isPotentiallyAcceptingOf` | Accepts only `GalleryDragModel` instances. |
| `dragStarted` | Shows the drag proxy on the source component. |
| `dragEntered` | No-op (required by interface). |
| `dragUpdated` | Delegates to `globalDragAdapter` when cursor is over the looking glass. Tracks enter/exit state. |
| `dragDroppedPostRejectorCheck` | Creates a `SceneDropSite` and returns the drop operation. |
| `dragExited` | No-op (required by interface). |
| `dragStopped` | Delegates to `globalDragAdapter.dragExited()`. |
| `getTrackableShape` | Returns `editor` (was `StorytellingSceneEditor.this`). |
| `getViewController` | Returns `editor` (was `StorytellingSceneEditor.this`). |

It also has one private helper method (`isDropLocationOverLookingGlass`)
and one private field (`overLookingGlass`) for cursor-over-looking-glass
state tracking.

**Why the constructor takes `StorytellingSceneEditor`, not
`lookingGlassPanel` + `globalDragAdapter`:** `globalDragAdapter` is `null`
at field-initialization time — it is set later in `initializeComponents()`
at line 648 (`this.globalDragAdapter = new GlobalDragAdapter(this)`). The
drop receptor is instantiated at field-declaration time (line 210:
`private final SceneEditorDropReceptor dropReceptor = new
SceneEditorDropReceptor()`), so it must lazy-access `globalDragAdapter`
through the editor reference rather than storing it directly.

### LookingGlassPanel

| Property | Value |
| --- | --- |
| Original location | Lines 230–244 (private inner class) |
| New file | `LookingGlassPanel.java` |
| Visibility | Package-private (no access modifier) |
| Superclass | `CompassPointSpringPanel` |
| Constructor | `LookingGlassPanel(OnscreenRenderTarget onscreenRenderTarget)` |
| Back-reference field | `onscreenRenderTarget` (`OnscreenRenderTarget`) |

The class wraps the OpenGL render target as a Swing `JPanel` with spring
layout support for compass-point positioning. It overrides 2 methods:

| Method | Purpose |
| --- | --- |
| `createJPanel` | Returns `onscreenRenderTarget.getAwtComponent()` (was `StorytellingSceneEditor.this.onscreenRenderTarget.getAwtComponent()`). |
| `setNorthWestComponent` | Adds a `SOUTH` spring constraint to pin the north-west component above the bottom edge. |

**Why the constructor takes `OnscreenRenderTarget`:** Unlike
`globalDragAdapter`, the render target is initialized at field-declaration
time (line 228), before `LookingGlassPanel` is instantiated (line 298).
Passing the render target directly is safe and avoids an unnecessary
back-reference to the full editor.

### SceneEditorListeners

| Property | Value |
| --- | --- |
| Original location | Lines 246–293 (7 anonymous `ValueListener` fields) |
| New file | `SceneEditorListeners.java` |
| Visibility | Package-private (no access modifier) |
| Constructor | `SceneEditorListeners(StorytellingSceneEditor editor)` |

This is a new helper class that consolidates 7 listener fields. Each
listener delegates to a method on `StorytellingSceneEditor`:

| Field | Type | Delegates to |
| --- | --- | --- |
| `showSnapGridListener` | `ValueListener<Boolean>` | `editor.setShowSnapGrid(value)` |
| `snapEnabledListener` | `ValueListener<Boolean>` | `editor.setShowSnapGrid(value)` (guarded by `SnapState.isShowSnapGridEnabled()`) |
| `snapGridSpacingListener` | `ValueListener<Double>` | `editor.setSnapGridSpacing(value)` |
| `cameraMarkerFieldSelectionListener` | `ValueListener<UserField>` | `editor.handleCameraMarkerFieldSelection(value)` |
| `objectMarkerFieldSelectionListener` | `ValueListener<UserField>` | `editor.handleObjectMarkerFieldSelection(value)` |
| `instanceFactorySelectionListener` | `ValueListener<InstanceFactory>` | `editor.selectionIsFromInstanceSelector = true; editor.setSelectedInstance(value); editor.selectionIsFromInstanceSelector = false` |
| `mainCameraViewSelectionObserver` | `ValueListener<CameraOption>` | `editor.handleMainCameraViewSelection()` |

All listener fields are `final` and package-private (no access modifier),
accessible as `this.listeners.showSnapGridListener` at registration sites.

## File inventory

| File | Role | Approx lines |
| --- | --- | --- |
| `StorytellingSceneEditor.java` | Parent class — 2 inner classes remain (`SingletonHolder`, `SceneEditorProgramImp`). Owns field declarations, constructor, and all public API. | ~1126 |
| `SceneEditorDropReceptor.java` | Extracted drop receptor. Package-private. | ~80 |
| `LookingGlassPanel.java` | Extracted looking glass panel. Package-private. | ~25 |
| `SceneEditorListeners.java` | Extracted listener helper. Package-private. | ~55 |
| `StorytellingSceneEditorCharacterizationTest.java` | Characterization test suite (81 tests after adding `field_listeners`). Updated for extraction. | ~590 |

All source files reside in:

```text
core/ide/src/main/java/org/alice/stageide/sceneeditor/
core/ide/src/test/java/org/alice/stageide/sceneeditor/
```

## Enclosing instance pattern

`SceneEditorDropReceptor` and `SceneEditorListeners` were non-static inner
classes (or relied on `StorytellingSceneEditor.this`). After extraction,
each takes an explicit `StorytellingSceneEditor editor` parameter:

```java
// SceneEditorDropReceptor — gallery drag-and-drop
SceneEditorDropReceptor(StorytellingSceneEditor editor) {
  this.editor = editor;
}

// SceneEditorListeners — all 7 listeners
SceneEditorListeners(StorytellingSceneEditor editor) {
  // initializes all 7 final listener fields
}
```

`LookingGlassPanel` only needs the render target, not the full editor:

```java
// LookingGlassPanel — render target wrapper
LookingGlassPanel(OnscreenRenderTarget onscreenRenderTarget) {
  this.onscreenRenderTarget = onscreenRenderTarget;
}
```

Instantiation sites in `StorytellingSceneEditor` update accordingly:

| Field | Before | After |
| --- | --- | --- |
| `dropReceptor` | `new SceneEditorDropReceptor()` | `new SceneEditorDropReceptor(this)` |
| `lookingGlassPanel` | `new LookingGlassPanel()` | `new LookingGlassPanel(onscreenRenderTarget)` |
| `listeners` (new) | N/A — 7 inline fields | `new SceneEditorListeners(this)` |

## Visibility changes

Seven members of `StorytellingSceneEditor` are widened from `private` to
package-private (no access modifier) to allow cross-file access within the
same package:

### Fields (3)

| Field | Type | Accessed by |
| --- | --- | --- |
| `lookingGlassPanel` | `LookingGlassPanel` | `SceneEditorDropReceptor` (for `getAwtComponent()` hit testing) |
| `globalDragAdapter` | `GlobalDragAdapter` | `SceneEditorDropReceptor` (for drag delegation) |
| `selectionIsFromInstanceSelector` | `boolean` | `SceneEditorListeners` (set/reset around `setSelectedInstance`) |

### Methods (4)

| Method | Signature | Accessed by |
| --- | --- | --- |
| `handleCameraMarkerFieldSelection` | `void handleCameraMarkerFieldSelection(UserField)` | `SceneEditorListeners` |
| `handleObjectMarkerFieldSelection` | `void handleObjectMarkerFieldSelection(UserField)` | `SceneEditorListeners` |
| `handleMainCameraViewSelection` | `void handleMainCameraViewSelection()` | `SceneEditorListeners` |
| `setSelectedInstance` | `void setSelectedInstance(InstanceFactory)` | `SceneEditorListeners` |

No member is widened beyond package-private. All 3 extracted classes are
in the same package (`org.alice.stageide.sceneeditor`), so package-private
access is the minimum required.

## Listener registration site changes

Registration sites in `StorytellingSceneEditor.initializeComponents()`
change from direct field access to access through the `listeners` helper:

| Before | After |
| --- | --- |
| `this.showSnapGridListener` | `this.listeners.showSnapGridListener` |
| `this.snapEnabledListener` | `this.listeners.snapEnabledListener` |
| `this.snapGridSpacingListener` | `this.listeners.snapGridSpacingListener` |
| `this.instanceFactorySelectionListener` | `this.listeners.instanceFactorySelectionListener` |
| `this.cameraMarkerFieldSelectionListener` | `this.listeners.cameraMarkerFieldSelectionListener` |
| `this.objectMarkerFieldSelectionListener` | `this.listeners.objectMarkerFieldSelectionListener` |
| `this.mainCameraViewSelectionObserver` | `this.listeners.mainCameraViewSelectionObserver` |

No listener registration logic changes. Only the field access path changes.

## Test updates

The characterization test suite (`StorytellingSceneEditorCharacterizationTest`)
requires 5 test updates and 1 new test. All 81 tests pass.

### Tests that change

| Test | Before | After | Reason |
| --- | --- | --- | --- |
| `innerClassCount_exactly4` | `assertEquals(4, ...)` | `assertEquals(2, ...)` | 2 inner classes extracted to top-level. |
| `innerClass_SceneEditorDropReceptor_exists` | `findInner("SceneEditorDropReceptor")` | `Class.forName("org.alice.stageide.sceneeditor.SceneEditorDropReceptor")` | Now a top-level class. |
| `innerClass_SceneEditorDropReceptor_isPrivate` | Asserts private, non-static | Asserts package-private (not public, not private, not protected) | Top-level package-private class. |
| `innerClass_LookingGlassPanel_exists` | `findInner("LookingGlassPanel")` | `Class.forName("org.alice.stageide.sceneeditor.LookingGlassPanel")` | Now a top-level class. |
| `innerClass_LookingGlassPanel_isPrivate` | Asserts private, non-static | Asserts package-private | Top-level package-private class. |

### Tests that do NOT change

- All 37 `api_*` tests — no public API change on the outer class.
- All 5 `renderTargetListener_*` tests — no change.
- All 21 `field_*` tests — `dropReceptor`, `lookingGlassPanel` remain as
  fields. The 7 listener fields are removed but replaced by the `listeners`
  field (net effect: field count drops from ~31 to ~25, still above the
  `≥ 20` guardrail).
- `publicMethodCount_atLeast35` — public method count unchanged.
- `declaredFieldCount_atLeast20` — field count ~25, still ≥ 20.
- `SingletonHolder` and `SceneEditorProgramImp` inner class tests — unchanged.

### New test added

| Test | Assertion |
| --- | --- |
| `field_listeners` | `assertDeclaredField("listeners")` — verifies the `SceneEditorListeners` helper field exists. |

## Validation commands

### Run the full characterization suite

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dcheckstyle.skip \
  -Dtest=StorytellingSceneEditorCharacterizationTest \
  test
```

Expected outcome: 81 test methods (80 original + 1 new `field_listeners`).
5 existing tests are updated, not removed.
All pass. 0 failures, 0 errors, 0 skipped. BUILD SUCCESS.

### Run alongside all core/ide tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

The extraction does not affect any other test in `core/ide`.

### Verify new file existence

```bash
ls core/ide/src/main/java/org/alice/stageide/sceneeditor/{SceneEditorDropReceptor,LookingGlassPanel,SceneEditorListeners}.java
```

All 3 files must exist.

### Verify no inner classes remain (except 2)

```bash
grep -c 'private class\|private static class\|public static class' \
  core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Expected: 2 matches (`SingletonHolder` and `SceneEditorProgramImp`).

## Compatibility rules

1. **Do not widen visibility beyond package-private.** The 3 extracted
   classes and the 7 widened members must remain package-private. No new
   public API surface is introduced by this extraction.

2. **Do not pass `globalDragAdapter` to `SceneEditorDropReceptor`'s
   constructor.** It is `null` at field-initialization time. The drop
   receptor must lazy-access it through the `editor` reference.

3. **Do not remove the `dropReceptor` or `lookingGlassPanel` fields from
   `StorytellingSceneEditor`.** They remain as fields on the outer class,
   instantiated with the new constructors. The `getDropReceptor()` public
   method continues to return `this.dropReceptor`.

4. **Do not change the listener registration pattern.** Listeners are
   registered in `StorytellingSceneEditor.initializeComponents()` using
   `addAndInvokeNewSchoolValueListener`. The only change is the access
   path (`this.listeners.X` instead of `this.X`).

5. **The `≥ 20` field count guardrail remains safe.** Removing 7 listener
   fields and adding 1 `listeners` field drops the count from ~31 to ~25.
   The test threshold is `≥ 20`.

6. **The `≥ 35` public method count guardrail remains safe.** No public
   methods are added or removed. The 4 methods widened to package-private
   were already non-public (`private`), so the public method count is
   unchanged.

7. **`SceneEditorProgramImp.getAnimator()` is unchanged.** This inner
   class remains in `StorytellingSceneEditor` as a public static inner
   class. Its `getAnimator()` override is the animation engine entry point
   and is not affected by this extraction.

8. **The singleton pattern is unchanged.** `SingletonHolder` remains as a
   private static inner class. `getInstance()` continues to use the lazy
   initialization holder idiom.

## Examples

### Verify the extraction compiles

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip compile
```

If a private member was not widened to package-private, compilation fails
with "has private access in StorytellingSceneEditor". See the
[Visibility changes](#visibility-changes) table for the complete list.

### Add a new listener to SceneEditorListeners

1. Add the new `ValueListener` field in `SceneEditorListeners.java`.
2. Initialize it in the constructor, delegating to the editor method.
3. Update the registration site in `StorytellingSceneEditor.initializeComponents()`:
   `this.listeners.newListener`.
4. Run the characterization suite — all 81 tests pass (additive change).
5. Optionally add a `field_*` test for the new listener.

### Understand why LookingGlassPanel takes OnscreenRenderTarget, not the editor

The `LookingGlassPanel` only needs the render target's AWT component for
`createJPanel()`. Passing the full editor would create an unnecessary
coupling. The render target is available at field-initialization time
(line 228), so it can be passed directly to the constructor. This follows
the principle of depending on the narrowest interface needed.
