# core/story-api Test Suite

## Overview

The `core/story-api` module contains the runtime entity implementations for Alice 3's
story world. Test coverage targets **≥ 50%** of the module's ~15,800 lines, up from
a 21.75% baseline. The test suite is fully **headless-safe** — it runs in CI
without a display server, GPU, or AWT peer initialization.

All tests use **JUnit 4** (`org.junit.Test`, `org.junit.Assert`).

---

## Running the Tests

```bash
# Run all story-api tests
mvn test -pl core/story-api -am

# Run a single test class
mvn test -pl core/story-api -Dtest=SceneImpTest

# Run with coverage report (JaCoCo)
mvn verify -pl core/story-api -am -Pcoverage
```

The JaCoCo HTML report is generated at:
```
core/story-api/target/site/jacoco/index.html
```

### Prerequisites

| Requirement | Detail |
|---|---|
| JDK | 17+ (project standard) |
| Maven | 3.9+ |
| Display | Not required — all tests are headless |
| Submodule | `git submodule update --init tweedle-lang` before first build |

---

## Test Architecture

### Class Hierarchy Under Test

```
PropertyOwnerImp
 └─ EntityImp (abstract)
     ├─ SceneImp                          ← tested via SceneImpTest
     └─ AbstractTransformableImp (abstract)
         ├─ StandInImp                    ← test double (also tested transitively)
         └─ TransformableImp (abstract)
             └─ ModelImp (abstract)
                 ├─ VisualScaleModelImp (abstract)
                 │   └─ SingleVisualModelImp (abstract)
                 │       └─ SimpleModelImp (abstract)
                 │           └─ GroundImp ← tested via GroundImpBehaviorTest
                 └─ JointedModelImp       ← tested via JointedModelImpBehaviorTest

ProgramImp (abstract)                     ← tested via ProgramImpTest
 └─ DefaultProgramImp
```

### Test Double Strategy

The implementation classes form deep inheritance chains that reference
scenegraph composites, render targets, and AWT components. The test suite
isolates behavior using three proven patterns:

#### 1. StandInImp — Concrete Leaf for Abstract Classes

`StandInImp` is a production class (not a test double) that extends
`AbstractTransformableImp` with minimal overhead. It wraps a scenegraph
`StandIn` node and supports vehicle hierarchy, transforms, and instance
registration. Tests that need an `EntityImp` or `AbstractTransformableImp`
use `StandInImp` directly.

```java
StandInImp subject = new StandInImp();
subject.setVehicle(vehicle);
subject.setLocalTransformation(AffineMatrix4x4.createTranslation(3, 4, 5));
```

#### 2. TestScene — Concrete SScene Subclass

`SScene` is abstract with one abstract method (`handleActiveChanged`). Each
test file that needs a scene defines a private `TestScene` inner class:

```java
private static class TestScene extends SScene {
    @Override
    protected void handleActiveChanged(boolean isActive, int activationCount) {
        // no-op for test
    }
}
```

The `TestScene` constructor calls `SScene()`, which creates a `SceneImp`
internally, wiring up the scenegraph `Scene`, lights, fog, and background —
all headless-safe.

#### 3. StubFactory — Fake Joint and Visual Data

`JointedModelImp` requires a `JointImplementationAndVisualDataFactory` to
construct joint hierarchies and visual data. Tests supply a `StubFactory`
that returns minimal scenegraph nodes:

```java
static class StubFactory implements JointImplementationAndVisualDataFactory<TestResource> {
    @Override
    public VisualData<TestResource> createVisualData() {
        // Returns stub with empty Visual[] and null SkeletonVisual
    }

    @Override
    public JointImp createJointImplementation(Joint sgJoint, JointId jointId, ...) {
        // Returns a JointImp wrapping the given sgJoint
    }
}
```

This pattern is established in `JointedModelImpDecompositionTest` and reused
for behavior-level testing.

#### 4. TestProgramImp — Null Render Target

`ProgramImp` is abstract and normally coupled to AWT via `OnscreenRenderTarget`.
Tests inject a concrete `TestProgramImp` via the static factory hook:

```java
ProgramImp.ACCEPTABLE_HACK_FOR_NOW_setClassForNextInstance(TestProgramImp.class);
SProgram program = new SProgram();
```

`TestProgramImp` passes `null` for the render target and provides a no-op
`Animator`, isolating simulation-speed logic and property queries from
the rendering pipeline.

---

## Test Files

### SceneImpTest

**File:** `src/test/java/org/lgna/story/implementation/SceneImpTest.java`

Tests `SceneImp` construction and property management through a `TestScene`
subclass. Covers:

| Category | Methods Tested |
|---|---|
| Construction | `SceneImp` creates scenegraph `Scene`, ambient light, directional light, fog, background |
| Atmosphere color | `setAtmosphereColor()` / `getAtmosphereColor()` round-trip |
| Fog density | `setFogDensity()` / `getFogDensity()` with boundary values (0.0, 1.0) |
| Above/below light | `setAboveLightColor()` / `setBelowLightColor()` and brightness |
| Event manager | `getEventManager()` returns non-null after construction |
| Camera registry | `findFirstCamera()` returns null when no camera added |
| Entity tracking | `addEntity()` / `removeEntity()` lifecycle |

**Transitive coverage:** `EntityImp` → `PropertyOwnerImp`, plus scenegraph types `Scene`,
`AmbientLight`, `DirectionalLight`, `ExponentialFog`, `Background`. Note: `SceneImp`
extends `EntityImp` directly (not through `AbstractTransformableImp` or `TransformableImp`).

### GroundImpBehaviorTest

**File:** `src/test/java/org/lgna/story/implementation/GroundImpBehaviorTest.java`

Tests `GroundImp` via `new SGround()`. The `SGround` constructor creates
a `GroundImp` internally, exercising the full chain `GroundImp` →
`SimpleModelImp` → `SingleVisualModelImp` → `VisualScaleModelImp` →
`ModelImp` → `TransformableImp` → `AbstractTransformableImp` → `EntityImp`.

| Category | Methods Tested |
|---|---|
| Construction | `SGround()` produces non-null `GroundImp` with valid scenegraph visual |
| Abstraction | `getAbstraction()` returns the owning `SGround` |
| Paint property | `setPaint()` / `getPaint()` via `SGround` facade |
| Opacity | `setOpacity()` round-trip through property system |
| Scenegraph | `getSgComposite()` returns a `Transformable` with geometry |
| Vehicle | Default vehicle is null; `setVehicle()` with `StandInImp` works |
| Mesh data | `GroundMeshData.VERTICES` has 593 entries (regression guard) |
| Resizers | `getResizers()` returns empty array (GroundImp does not support resizing) |
| Size | `setSize()` is a no-op guard (`assert false`) — tested to document this |

**Transitive coverage:** `SingleVisualModelImp.createVisual()`, `VisualScaleModelImp`,
`ModelImp` paint/opacity properties, `GroundMeshData` vertex and polygon arrays.

### EntityImpBehaviorTest

**File:** `src/test/java/org/lgna/story/implementation/EntityImpBehaviorTest.java`

Tests `EntityImp` (abstract) via `StandInImp`. Focuses on the core entity
contract: naming, vehicle hierarchy, instance registry, and transformation
queries.

| Category | Methods Tested |
|---|---|
| Instance registry | `putInstance()` / `getInstance()` scenegraph → Imp lookup |
| Naming | `setName()` / `getName()` round-trip |
| Vehicle hierarchy | `setVehicle()`, `getVehicle()`, `isDescendantOf()` |
| Self-vehicle guard | `setVehicle(self)` throws `LgnaIllegalArgumentException` |
| Transformations | `getAbsoluteTransformation()`, `getTransformation(other)` |
| StandIn creation | `createStandIn()` / `createOffsetStandIn()` return valid `StandInImp` |
| Abstraction | `getAbstraction()` returns null for `StandInImp` (by design) |
| Scene query | `getScene()` returns null when entity is unattached |
| Bounding box | `getAxisAlignedMinimumBoundingBox()` returns non-null |

**Transitive coverage:** `PropertyOwnerImp`, `ReferenceFrame` interface,
`UserDialogDelegate` (signature presence verified in `EntityImpStructureTest`).

### JointedModelImpBehaviorTest

**File:** `src/test/java/org/lgna/story/implementation/JointedModelImpBehaviorTest.java`

Tests `JointedModelImp` joint hierarchy construction and querying using
the `StubFactory` pattern with a `TestResource` that defines a 5-joint skeleton
(`ROOT`, `SPINE`, `HEAD`, `LEFT_ARM`, `TAIL_ROOT`).

| Category | Methods Tested |
|---|---|
| Joint map | `getJointImplementation(JointId)` returns correct `JointImp` |
| Tree walk | `treeWalk(JointId, observer)` visits joints in pre-order |
| Joint arrays | `getJointArrayIds()` returns declared `JointArrayId` fields |
| IK chain | `getJointsBetween(startId, endId)` returns ordered chain |
| Visual data | `getSgVisuals()` returns factory-provided visuals |
| Scale | `setScale(Dimension3)` applies scenegraph scale |
| Pose | `setPose(Pose)` applies joint orientations; `straightenOutJoints()` resets |
| Bounds | `getAxisAlignedMinimumBoundingBox()` returns non-null after setup |

**Transitive coverage:** `JointImp`, `JointedModelResourceBinder`,
`JointHierarchyManager`, `JointedModelVisualManager`, `SkeletonVisual`,
`Joint` scenegraph nodes.

### ProgramImpTest

**File:** `src/test/java/org/lgna/story/implementation/ProgramImpTest.java`

Tests `ProgramImp` via its static factory hook and a `TestProgramImp`
that passes `null` for the render target.

| Category | Methods Tested |
|---|---|
| Static factory | `ACCEPTABLE_HACK_FOR_NOW_setClassForNextInstance()` injects class |
| Construction | `new SProgram()` creates the injected `ProgramImp` subclass |
| Simulation speed | `setSimulationSpeedFactor()` / `getSimulationSpeedFactor()` |
| Speed format | `getSpeedFormat()` returns `"speed: %dx"` by default |
| Animator | `getAnimator()` returns the injected stub animator |
| Close lifecycle | `isClosed()` returns false initially |

**Transitive coverage:** `SProgram`, `DefaultProgramImp` (if not overridden),
`ProgramImp` static lock and factory machinery.

### AbstractTransformableImpBehaviorTest

**File:** `src/test/java/org/lgna/story/implementation/AbstractTransformableImpBehaviorTest.java`

Extends the existing `AbstractTransformableImpCharacterizationTest` with
additional coverage for facade methods not previously tested: distance
calculations with 3D offsets, facing detection at boundary angles,
rotation composition, and multi-hop vehicle chains.

| Category | Methods Tested |
|---|---|
| Distance 3D | `getDistanceTo()` with all three axes non-zero |
| Facing boundary | `isFacing()` at exactly 90° returns false |
| Rotation compose | Sequential `applyRotationInRadians()` composes correctly |
| Multi-hop vehicle | 3-level vehicle chain preserves absolute transforms |
| Position-only | `setPositionOnly()` preserves orientation |
| Orient to face | `orientToFace(other)` sets correct heading |
| Turn/roll/turn | `animateApplyRotation` with 0-duration for all axes |

**Transitive coverage:** `AbstractTransformableImp` internal helpers,
`StandIn` scenegraph node, vehicle-relative transform math.

---

## Headless Safety

Every test in this suite is designed to run without a display:

1. **No `OnscreenRenderTarget` creation** — tests that touch `ProgramImp`
   inject a null render target via the static factory hook.
2. **No `GlrRenderFactory`** — no OpenGL context is initialized.
3. **No AWT peers** — scenegraph nodes (`Scene`, `Transformable`, `Visual`,
   `StandIn`, `Joint`) are pure data objects that do not create native windows.
4. **`EventManager.initialize()` is headless-safe** — it is called in the
   `SceneImp` constructor, but only registers a `SceneActivationListener`
   on the timer. It does not create AWT peers or touch the render loop.

If a test accidentally triggers AWT initialization, it will fail with a
`HeadlessException` in CI. This is intentional — it catches regressions
immediately.

### @Assume Guard (Optional)

For tests that *might* need a display in the future, use:

```java
@Before
public void requireHeadless() {
    Assume.assumeTrue("Requires headless or display",
        GraphicsEnvironment.isHeadless() || GraphicsEnvironment.getLocalGraphicsEnvironment() != null);
}
```

Currently no tests require this guard.

---

## Coverage Model

### Direct Coverage

Each test file directly exercises methods on the class under test. The
estimated line coverage per file:

| Test File | Primary Class | Est. Lines Covered |
|---|---|---|
| SceneImpTest | SceneImp (371 LOC) | ~250 |
| GroundImpBehaviorTest | GroundImp (98 LOC) + SimpleModelImp | ~300 |
| EntityImpBehaviorTest | EntityImp (~400 LOC) | ~280 |
| JointedModelImpBehaviorTest | JointedModelImp (462 LOC) + managers (~812 LOC) | ~500 |
| ProgramImpTest | ProgramImp (404 LOC) | ~150 |
| AbstractTransformableImpBehaviorTest | AbstractTransformableImp (367 LOC) | ~200 |
| **Total new coverage** | | **~1,680** |

### Transitive Coverage

Constructor calls and method invocations transitively exercise superclass
chains, scenegraph node constructors, property system methods, and math
utilities. This multiplier typically adds **1.5–2×** the direct count:

| Transitive Path | Est. Additional Lines |
|---|---|
| PropertyOwnerImp property machinery | ~200 |
| Scenegraph constructors (Scene, Visual, StandIn, Joint) | ~400 |
| Math types (AffineMatrix4x4, OrthogonalMatrix3x3, Point3) | ~300 |
| EventManager construction + listener setup | ~150 |
| GroundMeshData arrays (class-load coverage) | ~100 |
| ModelImp / VisualScaleModelImp / SingleVisualModelImp plumbing | ~200 |
| **Total transitive** | **~1,350** |

### Projected Final Coverage

```
Baseline covered:    3,434 lines  (21.75%)
New direct:         +1,680 lines
New transitive:     +1,350 lines
─────────────────────────────────
Projected covered:   6,464 lines  (~40.9%)
```

If coverage falls short of 50%, buffer tests will be added for:
- `PropertyTest` — property change listeners, value validation
- `CameraImp` — camera transform and projection queries
- `MarkerImp` / `AxesImp` — lightweight visual entities
- `EventManager` — timer event handler scheduling

---

## Relationship to Existing Tests

The new behavior tests complement (not replace) existing test files:

| Existing File | Role | Relationship |
|---|---|---|
| `AbstractTransformableImpCharacterizationTest` | Documents pre-refactoring behavior | New test extends coverage, not the class |
| `EntityImpStructureTest` | Verifies API surface after decomposition | New test exercises runtime behavior |
| `GroundImpStructureTest` | Verifies mesh data extraction | New test exercises construction + properties |
| `JointedModelImpDecompositionTest` | TDD for manager decomposition | New test exercises behavior through managers |
| `JointHierarchyManagerDecompositionTest` | TDD for hierarchy manager extraction | Complementary — new tests exercise JointedModelImp facade |
| `TransformOperationsTest` | Math utility tests | Independent — no overlap |
| `TransformAnimatorTest` | Transform animation logic | Independent — animation internals |
| `TransformAnimatorExtractionTest` | Animator extraction refactoring | Independent — animation internals |
| `SmoothPositionAnimationsTest` | Smooth position interpolation | Independent — animation internals |
| `PlaceAnimationTest` | Place animation logic | Independent — animation internals |
| `OrientationDataTest` | Orientation data handling | Independent — data structure tests |
| `VehicleManagerTest` | Vehicle hierarchy logic | Complementary — new tests cover EntityImp side |
| `AbstractEventHandlerAsyncTest` | Event handler async dispatch | Independent — tests isFiringMap lifecycle |

### AbstractEventHandlerAsyncTest

**File:** `src/test/java/org/lgna/story/implementation/eventhandling/AbstractEventHandlerAsyncTest.java`

Tests `AbstractEventHandler` event dispatch lifecycle using a minimal
`TestEventHandler` subclass. All tests are async-safe and use `CountDownLatch`
+ polling with bounded timeouts (5 seconds).

| Category | Test method | Status |
|---|---|---|
| Enqueue policy | `enqueuePolicyDeliversQueuedEventsAfterActiveListenerCompletes` | ✅ Exists |
| Silence/restore | `silenceAndRestoreToggleEventDelivery` | ✅ Exists |
| Exception safety | `isFiringMapClearedEvenWhenFireThrows` | ✅ Exists |

The exception-safety test (`isFiringMapClearedEvenWhenFireThrows`) is the
characterization test for the try-finally fix in `newEventCall()`. It
confirms that the `isFiringMap` flag is always cleared even when `fire()`
throws. See [Event Handler Thread Safety](../../docs/architecture/event-handler-thread-safety.md)
for the full design rationale.

---

## Conventions

1. **Test class naming**: `<Class>BehaviorTest` for runtime behavior tests,
   `<Class>StructureTest` for structural/API surface tests,
   `<Class>CharacterizationTest` for pre-refactoring behavior snapshots.

2. **Method naming**: `<methodUnderTest>_<scenario>_<expectedOutcome>` or
   descriptive camelCase (e.g., `groundConstructorCreatesValidVisual`).

3. **Assertions**: Prefer `assertEquals` with epsilon for doubles,
   `assertNotNull` for construction verification, `assertTrue`/`assertFalse`
   with descriptive messages.

4. **No mocking frameworks**: Tests use production classes (`StandInImp`)
   or hand-written test doubles (`TestScene`, `StubFactory`). This avoids
   framework dependencies and keeps tests stable across refactoring.

5. **One concept per test**: Each `@Test` method tests one behavior. Setup
   is shared via `@Before` methods.

---

## Troubleshooting

### `HeadlessException` in CI

A test accidentally created an AWT component. Ensure the test does not:
- Call `ProgramImp.getOnscreenRenderTarget()`
- Call `EventManager.addListenersTo(OnscreenRenderTarget)` — this touches AWT components
- Create `JFrame`, `JPanel`, or other Swing components

### `NullPointerException` in SceneImp

`SceneImp` methods that traverse the camera registry or entity list will
return null if no camera or entities have been added. Tests should check
for null before asserting on returned values, or assert null is the
expected result.

### `LgnaIllegalArgumentException` from setVehicle

`setVehicle(self)` is intentionally guarded. This is tested explicitly in
`EntityImpBehaviorTest`. If you see this in another test, the vehicle
hierarchy setup has a cycle.

### Missing Tweedle grammar

If Maven reports missing generated parser classes:
```bash
git submodule update --init tweedle-lang
```

---

## Expanded coverage packages

The expanded story-api suite covers the IK solver math layer, interact
conditions, handle/input state, manipulator snap math, implementation
helpers, and event data classes. All follow the same headless patterns
and test double strategies above.

For the full test inventory, see the test packages listed below.

For running and troubleshooting instructions, see the Running the Tests section above.

### New test packages

| Package | Files | Focus |
| --- | --- | --- |
| `o.l.ik.core.solver` | 5 | Bone.Axis rotation math, chain traversal, solver convergence |
| `o.l.ik.core` | 1 | Top-level IK orchestration |
| `o.l.ik.core.enforcer` | 2 | IK enforcer data flow and thresholds |
| `o.a.interact.condition` | 7 | Input conditions: mouse, key, drag, modifier matching |
| `o.a.interact.handle` | 2 | Handle set registration and filtering |
| `o.a.interact` | 2 | InputState tracking and PickHint flags |
| `o.a.interact.manipulator` | 5 | Snap math, grid alignment, rotation increment |
| `o.l.story.implementation` | 2 | Camera marker, dialog delegate |
| `o.l.story.implementation.alice` | 2 | Dynamic resource, resource utilities |
| `o.l.story.resourceutilities` | 2 | Resource loading and storytelling resources |

### IK solver test pattern

```java
@Test
public void axis_invertDirection_negatesVector() {
  Bone bone = createTestBone();
  Bone.Axis axis = new Bone.Axis(bone, 0);
  axis.setCurrentValue(new Vector3(1, 0, 0));
  axis.invertDirection();
  Vector3 result = axis.getCurrentValue();
  assertEquals(-1.0, result.x, 1e-10);
}
```

### Interact condition test pattern

```java
@Test
public void mouseDragCondition_stateChanged_detectsNewDrag() {
  InputState current = new InputState();
  current.setMouseState(MouseEvent.BUTTON1, true);
  current.setIsDragEvent(true);
  InputState previous = new InputState();
  MouseDragCondition condition = new MouseDragCondition(
      MouseEvent.BUTTON1, new PickCondition(PickHint.getAnything()));
  assertTrue(condition.stateChanged(current, previous));
}
```
