# Tutorial: Trace the StorytellingSceneEditor Characterization

This tutorial walks through the `StorytellingSceneEditor` characterization
test suite. You will trace how class hierarchy, inner class structure, public
API surface, and field declarations are tested via pure reflection — without
launching the IDE or creating a render target.

For the full contract, see the [StorytellingSceneEditor Characterization
reference](../reference/storytelling-scene-editor-characterization.md).

## Contents

- [Goal](#goal)
- [1. Understand the class structure](#1-understand-the-class-structure)
- [2. Trace class hierarchy assertions](#2-trace-class-hierarchy-assertions)
- [3. Trace the singleton pattern](#3-trace-the-singleton-pattern)
- [4. Trace inner class assertions](#4-trace-inner-class-assertions)
- [5. Trace the SceneEditorProgramImp key override](#5-trace-the-sceneeditorprogramimp-key-override)
- [6. Trace public API surface assertions](#6-trace-public-api-surface-assertions)
- [7. Trace RenderTargetListener overrides](#7-trace-rendertargetlistener-overrides)
- [8. Trace key field declarations](#8-trace-key-field-declarations)
- [9. Trace aggregate stability guardrails](#9-trace-aggregate-stability-guardrails)
- [10. Run the tests](#10-run-the-tests)
- [11. Understand the boundaries](#11-understand-the-boundaries)

## Goal

Understand how the characterization test suite documents
`StorytellingSceneEditor`'s structural contract at each layer. After this
tutorial you will be able to explain:

- Why reflection is used instead of direct instantiation
- How the singleton pattern is verified without calling `getInstance()`
- How inner class modifiers signal architectural intent
- Why `SceneEditorProgramImp.getAnimator()` gets its own test
- How aggregate guardrails protect against bulk removals
- What the test does and does not prove

Open these source files alongside this guide:

```text
core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java
core/ide/src/test/java/org/alice/stageide/sceneeditor/StorytellingSceneEditorCharacterizationTest.java
```

## 1. Understand the class structure

`StorytellingSceneEditor` is a 1259-line singleton class that serves as the
3D scene editor in the Alice IDE. It manages:

```text
StorytellingSceneEditor (1259 lines)
  extends  AbstractSceneEditor
  implements  RenderTargetListener
  │
  ├── SingletonHolder          (private static)  — lazy init holder
  ├── SceneEditorDropReceptor  (private instance) — gallery drag-and-drop
  ├── LookingGlassPanel        (private instance) — render target JPanel
  └── SceneEditorProgramImp    (public static)   — animation engine bridge
```

Key responsibilities:
- Camera management (perspective, orthographic, layout, scene cameras)
- Object field management (add, remove, copy, state capture)
- Marker management (camera markers, object markers)
- Rendering control (enable/disable, screen capture)
- Snap grid configuration
- VR scene support

The characterization tests lock down all these structural properties so that
future refactoring (e.g., inner class extraction) can proceed safely.

## 2. Trace class hierarchy assertions

Open `StorytellingSceneEditorCharacterizationTest.java`. The `@BeforeClass`
method loads the class by name:

```java
@BeforeClass
public static void loadClass() {
  clazz = Class.forName("org.alice.stageide.sceneeditor.StorytellingSceneEditor");
}
```

**Why `Class.forName` instead of `StorytellingSceneEditor.class`?** Loading
by name avoids a compile-time dependency on the full IDE dependency graph.
The test only needs the class's structural metadata — it never creates an
instance.

The hierarchy assertions verify:

```java
@Test
public void extendsAbstractSceneEditor() {
  assertEquals("org.alice.ide.sceneeditor.AbstractSceneEditor",
      clazz.getSuperclass().getName());
}

@Test
public void implementsRenderTargetListener() {
  // Checks that RenderTargetListener is in the interfaces array
}
```

**Why this matters.** If someone changes the superclass (e.g., to extend
`JPanel` directly), the test fails immediately. The `AbstractSceneEditor`
base class provides field management, project lifecycle hooks, and the
`getDropReceptor()` / `addField()` / `setSelectedField()` contract. Losing
that inheritance would break the entire scene editor integration.

## 3. Trace the singleton pattern

Three tests verify the singleton without calling `getInstance()`:

```java
@Test
public void hasPrivateConstructor() {
  // Iterates getDeclaredConstructors(), finds a private no-arg one
}

@Test
public void hasGetInstanceMethod() {
  assertPublicMethod("getInstance");
}

@Test
public void getInstanceReturnsOwnType() {
  Method m = clazz.getDeclaredMethod("getInstance");
  assertTrue(Modifier.isStatic(m.getModifiers()));
  assertEquals(clazz, m.getReturnType());
}
```

**Why not call `getInstance()`?** Calling it would trigger the full IDE
bootstrap: Swing initialization, OpenGL render target creation, camera setup,
drag adapter wiring, and the animation engine. In headless CI, this would
fail. The reflection-based approach verifies the singleton *structure*
without triggering any *behavior*.

**The lazy initialization holder pattern.** `SingletonHolder` is a private
static inner class containing a `static final` instance. The JVM guarantees
thread-safe lazy initialization of static class members, making this a
lock-free singleton pattern. The test verifies `SingletonHolder` exists with
the correct modifiers.

## 4. Trace inner class assertions

The test enumerates all inner classes via `clazz.getDeclaredClasses()`:

```java
@Test
public void innerClassCount_exactly4() {
  assertEquals(4, innerClasses().length);
}
```

For each inner class, the test verifies existence and access modifiers:

| Inner class | Assertions |
| --- | --- |
| `SingletonHolder` | Exists, is private, is static |
| `SceneEditorDropReceptor` | Exists, is private, is NOT static |
| `LookingGlassPanel` | Exists, is private, is NOT static |
| `SceneEditorProgramImp` | Exists, is public, is static |

**Why modifiers matter:**

- **`private static` for `SingletonHolder`**: Static because it holds a class-level
  singleton. Private because external code should never access the holder.
- **`private` (non-static) for `SceneEditorDropReceptor`**: Non-static because
  it accesses the enclosing `StorytellingSceneEditor` instance's fields
  (e.g., `lookingGlassPanel`, `globalDragAdapter`). Private because it's an
  internal implementation detail.
- **`private` (non-static) for `LookingGlassPanel`**: Same pattern — accesses
  enclosing instance's render target.
- **`public static` for `SceneEditorProgramImp`**: Public because external code
  (the animation engine) needs to access it. Static because it can exist
  independently of the enclosing instance's lifecycle.

If you later extract `SceneEditorDropReceptor` to a top-level class, the
inner class count drops from 4 to 3, and the test fails — telling you to
update the contract.

## 5. Trace the SceneEditorProgramImp key override

```java
@Test
public void sceneEditorProgramImp_hasGetAnimator() {
  Class<?> c = findInner("SceneEditorProgramImp");
  Method m = c.getDeclaredMethod("getAnimator");
  assertTrue(Modifier.isPublic(m.getModifiers()));
}
```

**Why a separate test for one method?** `getAnimator()` is the key override
that connects the scene editor to Alice's animation engine. Without it, the
scene editor cannot play animations, preview motions, or run procedural
code in the viewport. It's the single most important method on any inner
class, so it gets its own explicit assertion.

## 6. Trace public API surface assertions

The test has 37 individual `api_*` methods, each calling `assertPublicMethod`:

```java
private static void assertPublicMethod(String name, Class<?>... paramTypes) {
  Method m = clazz.getDeclaredMethod(name, paramTypes);
  assertTrue(name + " must be public", Modifier.isPublic(m.getModifiers()));
}
```

Parameter types are resolved at runtime via the `resolve()` helper:

```java
private static Class<?> resolve(String fqcn) {
  return Class.forName(fqcn);
}
```

**Example trace — `addField`:**

```java
@Test
public void api_addField() {
  assertPublicMethod("addField",
      resolve("org.lgna.project.ast.UserType"),
      resolve("org.lgna.project.ast.UserField"),
      int.class,
      resolve("[Lorg.lgna.project.ast.Statement;"));
}
```

The `[Lorg.lgna.project.ast.Statement;` notation is the JVM internal name for
`Statement[]`. This is how `Class.forName` resolves array types.

**Why assert parameter types?** If someone changes `addField(UserType,
UserField, int, Statement[])` to `addField(UserType, UserField,
Statement[])` (dropping the `int` index parameter), all callers break. The
parameter type assertion catches this at the characterization level before
any caller tests fail.

## 7. Trace RenderTargetListener overrides

Five tests verify the `RenderTargetListener` contract:

```java
@Test
public void renderTargetListener_initialized() {
  assertPublicMethod("initialized",
      resolve("edu.cmu.cs.dennisc.render.event.RenderTargetInitializeEvent"));
}
```

Each render callback takes a specific event type. The test verifies that
`StorytellingSceneEditor` declares all five methods with the correct event
parameter types:

| Method | Event type | When called |
| --- | --- | --- |
| `initialized` | `RenderTargetInitializeEvent` | Render target is first created |
| `cleared` | `RenderTargetRenderEvent` | Frame buffer is cleared before rendering |
| `rendered` | `RenderTargetRenderEvent` | Frame has been rendered |
| `resized` | `RenderTargetResizeEvent` | Render target is resized |
| `displayChanged` | `RenderTargetDisplayChangeEvent` | Display configuration changes |

If any of these is missing, the render target cannot dispatch events to the
scene editor, and the 3D viewport becomes unresponsive.

## 8. Trace key field declarations

Twenty-one tests verify field existence by name:

```java
private static void assertDeclaredField(String name) {
  clazz.getDeclaredField(name);  // throws NoSuchFieldException if missing
}
```

Fields are grouped by responsibility:

**Camera implementations (4 fields):**
- `sceneCameraImp` — main scene camera
- `movableSceneCameraImp` — user-controllable camera
- `orthographicCameraImp` — top-down view camera
- `layoutCameraImp` — layout mode camera

**UI components (4 fields):**
- `expandButton`, `contractButton`, `runButton` — toolbar buttons
- `mainCameraNavigatorWidget` — camera navigation controls

**Core infrastructure (5 fields):**
- `onscreenRenderTarget` — the OpenGL render target
- `animator` — the animation engine
- `globalDragAdapter` — 3D drag interaction
- `lookingGlassPanel` — render target wrapper panel
- `dropReceptor` — gallery drag-and-drop receptor

**State tracking (4 fields):**
- `isVrScene` — VR mode flag
- `isInitialized` — initialization flag
- `selectionIsFromInstanceSelector` — selection source tracking
- `selectionIsFromMain` — selection source tracking

**Scene persistence (4 fields):**
- `snapGrid` — snap grid for object positioning
- `mainCameraMarkerList` — camera marker list
- `savedSceneEditorViewSelection` — view state persistence
- `automaticDisplayListener` — display change listener

**Why assert field names?** During refactoring, fields may be renamed,
merged, or removed. Each field assertion acts as a tripwire — if a field
disappears, the test tells you which one, so you can verify all references
have been updated.

## 9. Trace aggregate stability guardrails

Two tests provide coarse-grained safety nets:

```java
@Test
public void publicMethodCount_atLeast35() {
  long count = Arrays.stream(clazz.getDeclaredMethods())
      .filter(m -> Modifier.isPublic(m.getModifiers()))
      .count();
  assertTrue("Expected ≥35 public methods", count >= 35);
}

@Test
public void declaredFieldCount_atLeast20() {
  int count = clazz.getDeclaredFields().length;
  assertTrue("Expected ≥20 declared fields", count >= 20);
}
```

**Why `≥` instead of `==`?**

- Adding a new public method or field does not fail the test (additive
  changes are safe).
- Removing a method or field might not be caught by individual tests if the
  individual test was also removed in the same commit. The aggregate
  guardrail catches bulk removals.

**Example scenario:** A developer extracts 10 methods into a helper class
and forgets to update the characterization tests. The individual `api_*`
tests fail, but even if those are also deleted, the aggregate
`publicMethodCount_atLeast35` test catches the drop from 37 to 27.

## 10. Run the tests

From the repository root:

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

All 80 test methods pass without a display server, render target, or network
access.

## 11. Understand the boundaries

| Question | Answer |
| --- | --- |
| Does this prove the scene editor renders correctly? | No. No render target is created, no pixels are drawn. |
| Does this prove drag-and-drop works? | No. `SceneEditorDropReceptor` is verified to exist but never invoked. |
| Does this prove camera navigation works? | No. Camera fields are verified to exist but never used. |
| Does this prove `getInstance()` returns a working editor? | No. `getInstance()` is verified to exist with the right signature but never called. |
| Does this prove the VR pipeline works? | No. `isVrActive` is verified to exist but never invoked. |
| Does this prove animation playback works? | No. `SceneEditorProgramImp.getAnimator()` is verified to exist but never called. |
| Does this prove the snap grid renders? | No. `snapGrid` field and `setShowSnapGrid`/`setSnapGridSpacing` methods are verified to exist but never invoked. |
| What does it prove? | The class hierarchy, singleton pattern, inner class structure, public API surface, render callback contract, and key field declarations are all structurally present and correctly modified — establishing a safety net before any extraction or refactoring of this 1259-line class. |

The characterization suite is intentionally focused on structural contracts.
Each test exercises one structural property via reflection. Behavioral tests
that require a live scene editor belong in the outside-in QA lane with a
display server and render target.
