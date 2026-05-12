# StorytellingSceneEditor Characterization

This reference is the build contract for the `StorytellingSceneEditor`
characterization lane: class hierarchy, singleton pattern, inner class
structure, public API surface, RenderTargetListener overrides, key field
declarations, and aggregate stability guardrails — all verified via
reflection without GUI instantiation.

## Contents

- [Scope](#scope)
- [Artifact inventory](#artifact-inventory)
- [Class contracts](#class-contracts)
- [API reference](#api-reference)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Scope

This lane characterizes the structural contract of `StorytellingSceneEditor`
(1259 lines), the 3D scene editor in the Alice IDE. The class is a singleton
that extends `AbstractSceneEditor`, implements `RenderTargetListener`, and
manages camera views, drag adapters, snap grids, object markers, and field
management for the scene graph.

All 80 tests use pure reflection (`Class.forName`, `getDeclaredMethods`,
`getDeclaredFields`, `getDeclaredClasses`). No instance of
`StorytellingSceneEditor` is created, no GUI is launched, and no display
server is required.

It covers:

| Area | Contract |
| --- | --- |
| Class hierarchy | Extends `AbstractSceneEditor`, implements `RenderTargetListener`. |
| Singleton pattern | Private no-arg constructor, public static `getInstance()` returning `StorytellingSceneEditor`. |
| Inner classes | Exactly 4: `SingletonHolder` (private static), `SceneEditorDropReceptor` (private instance), `LookingGlassPanel` (private instance), `SceneEditorProgramImp` (public static). |
| `SceneEditorProgramImp` | Has public `getAnimator()` override — the animation engine entry point. |
| Public API surface | 37 outer-class public methods asserted by name and parameter types. |
| `RenderTargetListener` overrides | 5 methods: `initialized`, `cleared`, `rendered`, `resized`, `displayChanged`. |
| Key field declarations | 21 fields including camera imps, drag adapter, snap grid, VR flag, render target, animator, and UI components. |
| Aggregate stability | ≥ 35 public methods, ≥ 20 declared fields. |

This lane does not cover:

- 3D scene rendering or camera view switching behavior
- Drag-and-drop visual effects or interaction feedback
- VR pipeline integration or VR scene setup
- Object manipulation handle visibility
- Camera navigation interaction (orbit, pan, zoom)
- Runtime animation playback
- Snap grid visual rendering
- Desktop UI layout or project IO

## Artifact inventory

| Artifact | Purpose |
| --- | --- |
| `StorytellingSceneEditor.java` | Production class (~1259 lines). 4 inner classes, 37+ public methods, 21+ fields. |
| `StorytellingSceneEditorCharacterizationTest.java` | Characterization test suite (575 lines, 80 test methods). |

Source locations:

```text
core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
core/ide/src/test/java/org/alice/stageide/sceneeditor/StorytellingSceneEditorCharacterizationTest.java
```

## Class contracts

### Class hierarchy

```text
StorytellingSceneEditor
  extends  org.alice.ide.sceneeditor.AbstractSceneEditor
  implements  edu.cmu.cs.dennisc.render.event.RenderTargetListener
```

`AbstractSceneEditor` provides the base scene editor framework including
field management, project lifecycle hooks, and screen capture support.
`RenderTargetListener` provides the five render callback methods for the
OpenGL render target.

### Singleton pattern

| Element | Contract |
| --- | --- |
| Constructor | Private, no-arg. Prevents external instantiation. |
| `getInstance()` | Public, static, returns `StorytellingSceneEditor`. Uses the lazy initialization holder idiom via `SingletonHolder`. |

### Inner classes (exactly 4)

| Class | Modifiers | Purpose |
| --- | --- | --- |
| `SingletonHolder` | `private static` | Lazy initialization holder for the singleton instance. |
| `SceneEditorDropReceptor` | `private` (instance) | Handles gallery drag-and-drop onto the 3D scene viewport. |
| `LookingGlassPanel` | `private` (instance) | Custom `JPanel` wrapping the OpenGL render target for layout. |
| `SceneEditorProgramImp` | `public static` | Program implementation providing the `Animator` to the scene editor runtime. |

### SceneEditorProgramImp

The key override on this inner class:

| Method | Contract |
| --- | --- |
| `getAnimator()` | Public. Returns the `Animator` instance used for scene editor animation playback. This is the entry point connecting the scene editor to the animation engine. |

### Public API surface (37 methods)

Grouped by responsibility:

**Drop receptor:**

| Method | Parameters |
| --- | --- |
| `getDropReceptor` | (none) |

**Camera management:**

| Method | Parameters |
| --- | --- |
| `isStartingCameraView` | (none) |
| `setStartingCameraMarkerTransformation` | `AffineMatrix4x4` |
| `switchToOrthographicCamera` | (none) |
| `switchToPerspectiveCamera` | `AbstractCamera` |
| `getSgCameraForCreatingThumbnails` | (none) |

**Field management:**

| Method | Parameters |
| --- | --- |
| `setSelectedField` | `UserType`, `UserField` |
| `setSelectedExpression` | `Expression` |
| `centerCameraOnSelectedField` | `UserActivity` |
| `addField` | `UserType`, `UserField`, `int`, `Statement[]` |
| `setFieldToState` | `UserField`, `Statement[]` |
| `getCurrentStateCodeForField` | `UserField` |
| `generateCodeForSetUp` | `StatementListProperty` |
| `getDoStatementsForCopyField` | `UserField`, `UserField`, `AffineMatrix4x4` |
| `getDoStatementsForAddField` | `UserField`, `AffineMatrix4x4` |
| `getUndoStatementsForAddField` | `UserField` |
| `getRiders` | `UserField` |
| `getDoStatementsForRemoveField` | `UserField`, `Map` |
| `getUndoStatementsForRemoveField` | `UserField`, `Map` |

**VR:**

| Method | Parameters |
| --- | --- |
| `isVrActive` | (none) |

**Markers:**

| Method | Parameters |
| --- | --- |
| `setSelectedObjectMarker` | `UserField` |
| `getTransformForNewCameraMarker` | (none) |
| `getTransformForNewObjectMarker` | (none) |
| `getColorForNewObjectMarker` | (none) |
| `getColorForNewCameraMarker` | (none) |
| `getMarkerForField` | `UserField` |

**Rendering control:**

| Method | Parameters |
| --- | --- |
| `enableRendering` | `ReasonToDisableSomeAmountOfRendering` |
| `disableRendering` | `ReasonToDisableSomeAmountOfRendering` |
| `preScreenCapture` | (none) |
| `postScreenCapture` | (none) |
| `getOnscreenRenderTarget` | (none) |

**Object manipulation:**

| Method | Parameters |
| --- | --- |
| `setHandleVisibilityForObject` | `TransformableImp`, `boolean` |
| `getGoodPointOfViewInSceneForObject` | `AxisAlignedBox` |

**Snap grid:**

| Method | Parameters |
| --- | --- |
| `setShowSnapGrid` | `boolean` |
| `setSnapGridSpacing` | `double` |

**Lifecycle:**

| Method | Parameters |
| --- | --- |
| `handleShowing` | (none) |
| `handleHiding` | (none) |

### RenderTargetListener overrides (5 methods)

| Method | Event type |
| --- | --- |
| `initialized` | `RenderTargetInitializeEvent` |
| `cleared` | `RenderTargetRenderEvent` |
| `rendered` | `RenderTargetRenderEvent` |
| `resized` | `RenderTargetResizeEvent` |
| `displayChanged` | `RenderTargetDisplayChangeEvent` |

### Key field declarations (21 fields)

| Field | Responsibility |
| --- | --- |
| `isVrScene` | VR scene flag — controls VR-specific behavior. |
| `dropReceptor` | Gallery drag-and-drop receptor for the scene viewport. |
| `automaticDisplayListener` | Listener for automatic display changes. |
| `onscreenRenderTarget` | The OpenGL render target for the scene viewport. |
| `animator` | The animation engine for the scene editor. |
| `lookingGlassPanel` | The JPanel wrapping the render target. |
| `globalDragAdapter` | The drag adapter for 3D object manipulation. |
| `movableSceneCameraImp` | Implementation for the movable scene camera. |
| `sceneCameraImp` | Implementation for the main scene camera. |
| `mainCameraNavigatorWidget` | Camera navigation widget (orbit/pan/zoom). |
| `expandButton` | UI button for expanding the scene editor. |
| `contractButton` | UI button for contracting the scene editor. |
| `runButton` | UI button for running the project. |
| `orthographicCameraImp` | Implementation for the orthographic (top-down) camera. |
| `layoutCameraImp` | Implementation for the layout camera. |
| `snapGrid` | The snap grid for object positioning. |
| `isInitialized` | Initialization flag. |
| `selectionIsFromInstanceSelector` | Tracks whether the current selection originated from the instance selector. |
| `selectionIsFromMain` | Tracks whether the current selection originated from the main code editor. |
| `mainCameraMarkerList` | List of camera markers for the main camera. |
| `savedSceneEditorViewSelection` | Saved view selection state for scene editor view persistence. |

### Aggregate stability guardrails

| Guardrail | Bound | Purpose |
| --- | --- | --- |
| `publicMethodCount_atLeast35` | ≥ 35 | Catches bulk removal of public API. Additive changes do not trigger failure. |
| `declaredFieldCount_atLeast20` | ≥ 20 | Catches bulk removal of internal state. Additive changes do not trigger failure. |

## API reference

The test suite uses pure reflection throughout. No compile-time dependency on
`StorytellingSceneEditor` exists in the test class.

| Access pattern | Usage |
| --- | --- |
| `Class.forName(FQCN)` | Loads `StorytellingSceneEditor` by fully qualified class name. |
| `clazz.getDeclaredMethod(name, paramTypes)` | Asserts method existence, parameter types, and modifiers. |
| `clazz.getDeclaredField(name)` | Asserts field existence. |
| `clazz.getDeclaredClasses()` | Enumerates inner classes for count and modifier assertions. |
| `clazz.getDeclaredConstructors()` | Verifies the private no-arg constructor. |
| `clazz.getSuperclass()` | Verifies the superclass is `AbstractSceneEditor`. |
| `clazz.getInterfaces()` | Verifies `RenderTargetListener` is implemented. |
| `resolve(fqcn)` helper | Resolves parameter types by fully qualified name (e.g., `org.lgna.project.ast.UserField`). |

No `setAccessible(true)` is used. All assertions operate on structural
metadata (names, modifiers, parameter types) rather than field values or
method invocations.

## Validation commands

### Run the full characterization suite

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dcheckstyle.skip \
  -Dtest=StorytellingSceneEditorCharacterizationTest \
  test
```

Expected outcome: 80 test methods. All pass. 0 failures, 0 errors, 0
skipped. BUILD SUCCESS.

### Run alongside all core/ide tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

The characterization suite runs alongside existing `core/ide` tests without
interference.

## Compatibility rules

1. **Do not instantiate `StorytellingSceneEditor` in tests.** The singleton
   triggers the full IDE bootstrap, OpenGL render target creation, and Swing
   event dispatch. Tests must use reflection only.

2. **Do not change inner class count without updating the test.** The
   `innerClassCount_exactly4` assertion is a hard contract. Adding or
   extracting an inner class requires updating both the count and adding
   new name/modifier assertions.

3. **Do not remove public methods without updating the test.** Each public
   method has a named test (`api_*`). Removing a method fails the test.
   The aggregate `publicMethodCount_atLeast35` guard catches bulk removals
   that might bypass individual test updates.

4. **Do not change method parameter types without updating the test.** The
   `resolve()` helper loads parameter types by fully qualified name. Changing
   a parameter type causes `NoSuchMethodException`.

5. **Do not rename fields without updating the test.** Each key field has a
   named test (`field_*`). The aggregate `declaredFieldCount_atLeast20` guard
   catches bulk removals.

6. **The `≥` bounds are intentional.** Aggregate guardrails use `≥` (not `==`)
   to allow additive changes without test churn. Only removals trigger
   failures.

7. **`SceneEditorProgramImp.getAnimator()` is load-bearing.** This is the
   method that provides the animation engine to the scene editor runtime.
   Removing it breaks scene animation playback in the IDE.

8. **The singleton pattern is load-bearing.** `getInstance()` is called
   throughout the IDE to access the single scene editor instance. Breaking
   the singleton pattern (e.g., making the constructor public) could cause
   multiple scene editors to coexist, leading to state corruption.

## Examples

### Verify a method rename is safe

Before renaming `setShowSnapGrid` to `setSnapGridVisible`:

1. Run the characterization suite (see [Validation commands](#validation-commands)).
2. The `api_setShowSnapGrid` test fails with `NoSuchMethodException`.
3. Update the test to use the new method name.
4. Search the codebase for all callers of the old name and update them.
5. Run the suite again — all 80 tests pass.

### Add a new public method

After adding a new public method `resetCamera()`:

1. Run the characterization suite. All 80 tests pass (additive changes don't
   break `≥` bounds).
2. Add a new test method:
   ```java
   @Test
   public void api_resetCamera() {
     assertPublicMethod("resetCamera");
   }
   ```
3. Run the suite again — 81 tests pass.

### Extract an inner class to top-level

When extracting `SceneEditorDropReceptor` to a top-level file:

1. Run the characterization suite. The `innerClassCount_exactly4` test fails.
2. Update the expected count to 3.
3. Remove or update the `SceneEditorDropReceptor` inner class tests.
4. Add new tests for the extracted top-level class if it has a public API.
5. Run the suite again — all tests pass.
6. Document the extraction in the PR description.

### Understand why the test uses reflection

The test avoids compile-time dependencies on `StorytellingSceneEditor` to:

- Prevent the test from pulling in the full IDE dependency graph at compile
  time (which could trigger annotation processors or resource loading);
- Allow the test to verify structural properties (method names, modifiers)
  without executing any code in the subject class;
- Follow the same pattern as `InnerClassExtractionContractTest` in
  `core/glrender`, which proved effective for pre-refactoring characterization.
