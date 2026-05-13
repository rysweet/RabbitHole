# Tutorial: Trace the StorytellingSceneEditor Inner Class Extraction

This tutorial walks through the extraction of `SceneEditorDropReceptor`,
`LookingGlassPanel`, and `SceneEditorListeners` from
`StorytellingSceneEditor` (issue #528). You will trace each design
decision — why some classes take the full editor while others take a
narrow dependency, how visibility is minimized, and how the
characterization tests adapt.

For the full contract, see the [StorytellingSceneEditor Inner Class
Extraction reference](../reference/storytelling-scene-editor-inner-class-extraction.md).

For validation steps, see the [Validation how-to](../howto/validate-storytelling-scene-editor-inner-class-extraction.md).

## Contents

- [Goal](#goal)
- [1. Understand the pre-extraction structure](#1-understand-the-pre-extraction-structure)
- [2. Trace the SceneEditorDropReceptor extraction](#2-trace-the-sceneeditordropreceptor-extraction)
- [3. Trace the LookingGlassPanel extraction](#3-trace-the-lookingglasspanel-extraction)
- [4. Trace the SceneEditorListeners extraction](#4-trace-the-sceneeditorlisteners-extraction)
- [5. Trace the visibility changes](#5-trace-the-visibility-changes)
- [6. Trace the import cleanup](#6-trace-the-import-cleanup)
- [7. Trace the test updates](#7-trace-the-test-updates)
- [8. Run the tests](#8-run-the-tests)
- [9. Understand the boundaries](#9-understand-the-boundaries)

## Goal

After this tutorial you will be able to explain:

- Why `SceneEditorDropReceptor` takes the full editor, not individual fields
- Why `LookingGlassPanel` takes `OnscreenRenderTarget` instead of the editor
- Why anonymous listeners are grouped into a helper class
- How field initialization order constrains constructor design
- How the characterization tests detect structural changes via reflection
- What the extraction does and does not change about runtime behavior

Open these source files alongside this guide:

```text
core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorDropReceptor.java
core/ide/src/main/java/org/alice/stageide/sceneeditor/LookingGlassPanel.java
core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorListeners.java
core/ide/src/test/java/org/alice/stageide/sceneeditor/StorytellingSceneEditorCharacterizationTest.java
```

## 1. Understand the pre-extraction structure

Before extraction, `StorytellingSceneEditor` has 4 inner classes and 7
anonymous listener fields:

```text
StorytellingSceneEditor (1259 lines)
  extends  AbstractSceneEditor
  implements  RenderTargetListener
  │
  ├── SingletonHolder          (private static)   — lazy init holder
  ├── SceneEditorDropReceptor  (private instance)  — gallery drag-and-drop
  ├── LookingGlassPanel        (private instance)  — render target JPanel
  ├── SceneEditorProgramImp    (public static)     — animation engine bridge
  │
  ├── showSnapGridListener             (anonymous ValueListener<Boolean>)
  ├── snapEnabledListener              (anonymous ValueListener<Boolean>)
  ├── snapGridSpacingListener          (anonymous ValueListener<Double>)
  ├── cameraMarkerFieldSelectionListener  (anonymous ValueListener<UserField>)
  ├── objectMarkerFieldSelectionListener  (anonymous ValueListener<UserField>)
  ├── instanceFactorySelectionListener    (anonymous ValueListener<InstanceFactory>)
  └── mainCameraViewSelectionObserver     (anonymous ValueListener<CameraOption>)
```

The 2 inner classes being extracted (`SceneEditorDropReceptor`,
`LookingGlassPanel`) access the enclosing instance via
`StorytellingSceneEditor.this`. The 7 listeners also access the enclosing
instance. These three groups — drop receptor, panel, listeners — form
natural extraction boundaries because each is a cohesive unit with
clear dependencies on the outer class.

`SingletonHolder` and `SceneEditorProgramImp` are **not** extracted.
`SingletonHolder` is an implementation detail of the singleton pattern and
belongs with the singleton. `SceneEditorProgramImp` is `public static` —
it has no enclosing instance reference and is accessed by external code
(the animation engine), so extracting it would change the public API.

## 2. Trace the SceneEditorDropReceptor extraction

Open `StorytellingSceneEditor.java` at lines 129–198. This inner class:

- Extends `AbstractDropReceptor`
- Overrides 7 methods for the drag-and-drop lifecycle
- Accesses `lookingGlassPanel` (for hit testing — is the cursor over the
  render target?)
- Accesses `globalDragAdapter` (for delegation — forward drag events to
  the 3D interaction handler)
- Returns `StorytellingSceneEditor.this` from `getTrackableShape()` and
  `getViewController()`

**The constructor design problem.** You might expect the constructor to
take `lookingGlassPanel` and `globalDragAdapter` directly. But look at
the field initialization order:

```text
Line 210: dropReceptor = new SceneEditorDropReceptor()        ← created here
Line 228: onscreenRenderTarget = GlrRenderFactory...          ← initialized after dropReceptor
Line 298: lookingGlassPanel = new LookingGlassPanel()         ← initialized after dropReceptor
Line 299: globalDragAdapter                                   ← declared but null!
Line 648: globalDragAdapter = new GlobalDragAdapter(this)     ← set in initializeComponents()
```

The `dropReceptor` field is initialized at line 210 — before
`lookingGlassPanel` (line 298) and long before `globalDragAdapter` is
set (line 648 in `initializeComponents()`). Passing either field to the
extracted constructor would capture `null`.

**Solution: pass the editor, access fields lazily.** The extracted
constructor takes the full `StorytellingSceneEditor` as `editor`. When
drag events arrive at runtime, the drop receptor accesses
`editor.globalDragAdapter`, which by then is non-null
(`initializeComponents()` has completed).

```java
// SceneEditorDropReceptor.java
class SceneEditorDropReceptor extends AbstractDropReceptor {
  private final StorytellingSceneEditor editor;

  SceneEditorDropReceptor(StorytellingSceneEditor editor) {
    this.editor = editor;
  }

  @Override
  public DropSite dragUpdated(DragStep dragStep) {
    // editor.globalDragAdapter is non-null by runtime
    editor.globalDragAdapter.dragUpdated(dragStep);
    ...
  }
}
```

The `getTrackableShape()` and `getViewController()` methods return
`editor` instead of `StorytellingSceneEditor.this`.

## 3. Trace the LookingGlassPanel extraction

Open `StorytellingSceneEditor.java` at lines 230–244. This inner class:

- Extends `CompassPointSpringPanel`
- Overrides `createJPanel()` — returns the render target's AWT component
- Overrides `setNorthWestComponent()` — adds a spring layout constraint
- Accesses only `StorytellingSceneEditor.this.onscreenRenderTarget`

**Why the constructor takes `OnscreenRenderTarget`, not the editor.**
Unlike `SceneEditorDropReceptor`, this class only needs one thing from the
enclosing instance: the render target. And the render target **is**
available at field-initialization time (line 228). Passing the full editor
would create unnecessary coupling.

```java
// LookingGlassPanel.java
class LookingGlassPanel extends CompassPointSpringPanel {
  private final OnscreenRenderTarget onscreenRenderTarget;

  LookingGlassPanel(OnscreenRenderTarget onscreenRenderTarget) {
    this.onscreenRenderTarget = onscreenRenderTarget;
  }

  @Override
  protected JPanel createJPanel() {
    return this.onscreenRenderTarget.getAwtComponent();
  }
}
```

The instantiation site changes from `new LookingGlassPanel()` to
`new LookingGlassPanel(onscreenRenderTarget)`.

**Design principle at work.** Depend on the narrowest type that satisfies
the dependency. `LookingGlassPanel` needs a render target, not a full
scene editor. This makes the class testable in isolation and clearly
communicates its actual dependency.

## 4. Trace the SceneEditorListeners extraction

Open `StorytellingSceneEditor.java` at lines 246–293. Seven anonymous
`ValueListener` fields, each a 3–6 line class that delegates to a method
on the enclosing instance:

```java
private final ValueListener<Boolean> showSnapGridListener = new ValueListener<Boolean>() {
  @Override
  public void valueChanged(ValueEvent<Boolean> e) {
    StorytellingSceneEditor.this.setShowSnapGrid(e.getNextValue());
  }
};
```

**Why a helper class, not 7 separate top-level classes?** Each listener
is 3–6 lines. Creating 7 separate files for trivial delegating classes
would be worse than the original. A single `SceneEditorListeners` helper
groups them by purpose (they are all event listeners for the scene editor)
and reduces file sprawl.

**Why fields, not factory methods?** The listeners are stored as `final`
fields on the helper, accessed as `this.listeners.showSnapGridListener`
at registration sites. This is simpler than factory methods
(`createShowSnapGridListener()`) because:

- The listeners are stateless delegates — they don't need per-call creation.
- Direct field access is idiomatic for listener wiring in the Alice codebase.
- Registration sites read clearly: `addListener(this.listeners.snapGridSpacingListener)`.

**The `instanceFactorySelectionListener` is special.** It sets a flag
on the editor (`selectionIsFromInstanceSelector = true`), calls
`setSelectedInstance()`, then resets the flag. This requires the
`selectionIsFromInstanceSelector` field to be widened to package-private:

```java
// SceneEditorListeners.java — instanceFactorySelectionListener
final ValueListener<InstanceFactory> instanceFactorySelectionListener =
    new ValueListener<InstanceFactory>() {
  @Override
  public void valueChanged(ValueEvent<InstanceFactory> e) {
    editor.selectionIsFromInstanceSelector = true;
    editor.setSelectedInstance(e.getNextValue());
    editor.selectionIsFromInstanceSelector = false;
  }
};
```

## 5. Trace the visibility changes

Seven members are widened from `private` to package-private. Trace each
one to understand why:

**Fields:**

| Field | Why widened |
| --- | --- |
| `lookingGlassPanel` | `SceneEditorDropReceptor.isDropLocationOverLookingGlass()` calls `editor.lookingGlassPanel.getAwtComponent()`. |
| `globalDragAdapter` | `SceneEditorDropReceptor.dragUpdated()` delegates to `editor.globalDragAdapter.dragEntered/Updated/Exited()`. |
| `selectionIsFromInstanceSelector` | `SceneEditorListeners.instanceFactorySelectionListener` sets/resets this flag around `setSelectedInstance()`. |

**Methods:**

| Method | Why widened |
| --- | --- |
| `handleCameraMarkerFieldSelection` | `SceneEditorListeners.cameraMarkerFieldSelectionListener` calls it. |
| `handleObjectMarkerFieldSelection` | `SceneEditorListeners.objectMarkerFieldSelectionListener` calls it. |
| `handleMainCameraViewSelection` | `SceneEditorListeners.mainCameraViewSelectionObserver` calls it. |
| `setSelectedInstance` | `SceneEditorListeners.instanceFactorySelectionListener` calls it. |

**Why not public?** Package-private is the minimum access that allows
same-package classes to use these members. Making them public would expand
the API surface unnecessarily — external callers don't need these methods.

**How to verify.** If you accidentally leave a member `private`, the
compiler reports "has private access in StorytellingSceneEditor". The fix
is to remove the `private` keyword (making it package-private, the Java
default).

## 6. Trace the import cleanup

After the extracted classes move to their own files, 8 imports in
`StorytellingSceneEditor.java` are no longer referenced:

| Import | Why orphaned |
| --- | --- |
| `GalleryDragModel` | Used only in `SceneEditorDropReceptor.isPotentiallyAcceptingOf()` |
| `SceneDropSite` | Used only in `SceneEditorDropReceptor.dragDroppedPostRejectorCheck()` |
| `ValueEvent` | Used only in listener `valueChanged()` signatures |
| `ValueListener` | Used only in listener field declarations |
| `DragStep` | Used only in `SceneEditorDropReceptor` drag methods |
| `JPanel` | Used only in `LookingGlassPanel.createJPanel()` |
| `SpringLayout` | Used only in `LookingGlassPanel.setNorthWestComponent()` |
| `Point` | Used only in `SceneEditorDropReceptor.isDropLocationOverLookingGlass()` |

Each extracted file adds the imports it needs. The parent file's import
section becomes shorter and accurately reflects its own dependencies.

Verify with:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml -pl core/ide
```

## 7. Trace the test updates

Open `StorytellingSceneEditorCharacterizationTest.java`. Five tests change:

### Inner class count: 4 → 2

```java
// Before
assertEquals(4, innerClasses().length);

// After
assertEquals(2, innerClasses().length);
```

Two inner classes extracted. Two remain (`SingletonHolder`,
`SceneEditorProgramImp`).

### SceneEditorDropReceptor existence: inner → top-level

```java
// Before — finds an inner class
assertNotNull(findInner("SceneEditorDropReceptor"));

// After — finds a top-level class
Class.forName("org.alice.stageide.sceneeditor.SceneEditorDropReceptor");
```

### SceneEditorDropReceptor visibility: private → package-private

```java
// Before — asserts private, non-static inner class
assertTrue(Modifier.isPrivate(c.getModifiers()));

// After — asserts package-private top-level class
assertFalse(Modifier.isPublic(c.getModifiers()));
assertFalse(Modifier.isPrivate(c.getModifiers()));
assertFalse(Modifier.isProtected(c.getModifiers()));
```

Same pattern for `LookingGlassPanel`.

### What does NOT change

- All 37 `api_*` tests — the public API is identical.
- All 5 `renderTargetListener_*` tests — render callbacks are untouched.
- All 21 `field_*` tests (existing) — `dropReceptor` and
  `lookingGlassPanel` remain as fields. The 7 listener fields are removed,
  but individual `field_*` tests for those fields did not exist (they were
  anonymous inner classes, not named fields in the test assertions).
- Aggregate guardrails — method count ≥ 35 (unchanged), field count ≥ 20
  (drops from ~31 to ~25, still passes).

## 8. Run the tests

From the repository root:

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

All 80 test methods pass. No display server, render target, or network
access required.

## 9. Understand the boundaries

| Question | Answer |
| --- | --- |
| Does this change any runtime behavior? | No. The extracted classes contain identical logic. Only the field access path changes (e.g., `StorytellingSceneEditor.this.globalDragAdapter` → `editor.globalDragAdapter`). |
| Does this change the public API? | No. All 37 public methods, `getDropReceptor()`, `getInstance()`, and `SceneEditorProgramImp` are unchanged. |
| Does this affect drag-and-drop behavior? | No. `SceneEditorDropReceptor` has the same logic — only its location (inner → top-level) and dependency injection mechanism (implicit `this` → explicit constructor) change. |
| Does this affect the render target? | No. `LookingGlassPanel.createJPanel()` returns the same AWT component. The only change is how it accesses the render target (stored field vs. enclosing instance reference). |
| Does this affect listener behavior? | No. Each listener delegates to the same editor method with the same arguments. The access path changes from `StorytellingSceneEditor.this.setShowSnapGrid(v)` to `editor.setShowSnapGrid(v)`. |
| Does this reduce the line count? | Yes. `StorytellingSceneEditor` drops from ~1259 to ~1116 lines (~143 lines removed, net of added constructor calls and field declarations). |
| Can I further decompose `StorytellingSceneEditor`? | Yes. The remaining ~1116 lines include method groups for camera management, field management, markers, and rendering control that are candidates for future extraction. The characterization tests will catch any structural changes. |
