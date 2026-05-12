# Characterize StorytellingSceneEditor

Use this guide to run and review the `StorytellingSceneEditor` characterization
tests. These tests validate class hierarchy, inner class structure, public API
surface, key field declarations, and aggregate stability guardrails via
reflection — no GUI, no singleton instantiation, no display server.

For the full contract, see the [StorytellingSceneEditor Characterization
reference](../reference/storytelling-scene-editor-characterization.md).

## When to use this guide

Use this guide for changes near:

```text
core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
```

Also run these checks when changing:

- The `StorytellingSceneEditor` class hierarchy (superclass or interfaces);
- Any inner class (`SingletonHolder`, `SceneEditorDropReceptor`,
  `LookingGlassPanel`, `SceneEditorProgramImp`);
- The singleton pattern (`getInstance()`, private constructor);
- Any public method signature on the outer class;
- Camera-related fields (`sceneCameraImp`, `movableSceneCameraImp`,
  `orthographicCameraImp`, `layoutCameraImp`);
- Drag adapter, snap grid, VR flag, or render target fields;
- `SceneEditorProgramImp.getAnimator()` — the key animation override;
- Scene editor field management (`addField`, `setFieldToState`,
  `getCurrentStateCodeForField`, `generateCodeForSetUp`);
- Marker management (`setSelectedObjectMarker`, `getMarkerForField`,
  `getTransformForNewCameraMarker`, `getTransformForNewObjectMarker`).

Do not use this guide for runtime scene rendering, 3D interaction behavior,
drag-and-drop visual effects, or VR pipeline integration. Those behaviors
require a live render target and are outside this characterization lane.

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the characterization suite

```bash
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

## Run alongside all core/ide tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

The characterization suite runs alongside existing `core/ide` tests without
interference.

## Review the results

### Class hierarchy

| Assertion category | Meaning |
| --- | --- |
| Superclass | `StorytellingSceneEditor` extends `AbstractSceneEditor`. A change to the inheritance chain breaks this test. |
| Interface | Must implement `RenderTargetListener`. Removing this interface breaks render callback dispatch. |

### Singleton pattern

| Assertion category | Meaning |
| --- | --- |
| Private constructor | The no-arg constructor must be private. Making it public would break the singleton guarantee. |
| `getInstance()` | Must be public, static, and return `StorytellingSceneEditor`. |

### Inner classes

| Assertion category | Meaning |
| --- | --- |
| Count | Exactly 4 inner classes. Adding or removing an inner class triggers a failure. |
| `SingletonHolder` | Must be private and static (lazy initialization holder). |
| `SceneEditorDropReceptor` | Must be private and non-static (instance inner class). |
| `LookingGlassPanel` | Must be private and non-static (instance inner class). |
| `SceneEditorProgramImp` | Must be public and static (exposed for animation integration). |

### SceneEditorProgramImp key override

| Assertion category | Meaning |
| --- | --- |
| `getAnimator()` | Must be public. This method provides the animation engine to the scene editor runtime. Removing it breaks scene animation playback. |

### Public API surface (37 methods)

| Assertion category | Meaning |
| --- | --- |
| Individual method signatures | Each public method is asserted by name and parameter types. Renaming, changing parameters, or removing a method triggers a failure. |
| Aggregate count ≥ 35 | Guards against bulk removal of public API. |

### RenderTargetListener overrides (5 methods)

| Assertion category | Meaning |
| --- | --- |
| `initialized`, `cleared`, `rendered`, `resized`, `displayChanged` | Each render callback must exist with the correct event parameter type. |

### Key field declarations (21 fields)

| Assertion category | Meaning |
| --- | --- |
| Individual fields | Each field is asserted by name. Renaming or removing a field triggers a failure. |
| Aggregate count ≥ 20 | Guards against bulk removal of internal state. |

## Troubleshooting

### Class not found

If `Class.forName("org.alice.stageide.sceneeditor.StorytellingSceneEditor")`
fails:

1. Verify the `core/ide` module compiles: `mvn -pl core/ide -am compile`.
2. Check that the tweedle-lang submodule is initialized (the Maven enforcer
   requires it).

### Inner class count mismatch

If you added a new inner class to `StorytellingSceneEditor`, the
`innerClassCount_exactly4` test will fail. Update the expected count in the
test and add assertions for the new inner class's name and modifiers.

### Method signature changes

If you changed a method's parameter types, the corresponding `api_*` test
will fail with `NoSuchMethodException`. Update the parameter types in the
test's `assertPublicMethod` call to match the new signature, and document
the API change in the PR description.

### Aggregate count failures

If `publicMethodCount_atLeast35` or `declaredFieldCount_atLeast20` fails:

- A method or field was removed. This is a breaking change — verify the
  removal is intentional and update the bound.
- The `≥` guard prevents false failures from additive changes.

## What this guide does NOT cover

- 3D scene rendering (render targets, camera views, OpenGL pipelines)
- Drag-and-drop visual behavior (ghost objects, drop previews)
- VR integration (`isVrActive`, VR scene setup)
- Object manipulation (handle visibility, snap grid visual behavior)
- Camera navigation (orbit, pan, zoom interaction)
- Runtime animation playback (`SceneEditorProgramImp.getAnimator()` behavior)
- Desktop UI layout, save workflows, or project IO
