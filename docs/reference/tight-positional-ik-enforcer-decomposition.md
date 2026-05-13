# TightPositionalIkEnforcer Inner Class Extraction

This reference documents the extraction of all 14 inner classes from
`TightPositionalIkEnforcer` (1328 lines) into separate top-level files in
`org.lgna.ik.core.enforcer`. The extraction introduces an `IkEnforcerContext`
interface to replace implicit outer-class references, reducing the enforcer
to ~438 lines.

Issue #557 decomposes the enforcer without changing observable behavior. All
IK solver math, convergence logic, and constraint evaluation are preserved
identically.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Extracted classes](#extracted-classes)
- [File inventory](#file-inventory)
- [IkEnforcerContext interface](#ikenforcercontext-interface)
- [Context injection pattern](#context-injection-pattern)
- [Enforcer call-site changes](#enforcer-call-site-changes)
- [Visibility rules](#visibility-rules)
- [Import fixups](#import-fixups)
- [Static constants](#static-constants)
- [Method visibility widening: Jacobian.matrixWasUpdated](#method-visibility-widening-jacobianmatrixwasupdated)
- [Self-reference fix: AngleDeltas.getForAxis](#self-reference-fix-angledeltasgetforaxis)
- [Dead code preservation](#dead-code-preservation)
- [Error handling contract](#error-handling-contract)
- [Configuration](#configuration)
- [Validation](#validation)
- [Characterization tests](#characterization-tests)
- [Acceptance criteria](#acceptance-criteria)
- [Claim boundaries](#claim-boundaries)

## Motivation

The original `TightPositionalIkEnforcer.java` contained 1328 lines with 14
inner classes implementing a priority-based inverse kinematics solver. The
inner classes covered five distinct concerns:

1. **Joint limits** — `Limit`, `SingleAxisLimit`, `IndependentBallJointLimit`
2. **Linear algebra** — `SvdInfo`, `Jacobian`, `InvertedJacobian`, `Displacement`
3. **Constraint system** — `Constraint`, `PositionConstraint`, `OrientationConstraint`, `PriorityLevel`
4. **Axis management** — `JacobianAxis`, `AngleDeltas`
5. **Nullspace projection** — `NullspaceProjector`

Four of these inner classes (`Jacobian`, `Constraint`, `NullspaceProjector`,
`AngleDeltas`) accessed the enclosing enforcer's `indexToAxis`, `axisToIndex`,
and `getGlobalIndexForAxis()` through the implicit `TightPositionalIkEnforcer.this`
reference. The remaining ten were either static or self-contained.

This mix of responsibilities made the class difficult to navigate, test in
isolation, and extend. Extracting to top-level classes with an explicit context
interface allows each component to be understood and tested independently.

## Architecture

```text
IKCore (public facade — import updated)
└── TightPositionalIkEnforcer (~438 lines, implements IkEnforcerContext)
    │
    ├── IkEnforcerContext (interface, 3 methods)
    │
    ├── Clean standalone classes (no context needed):
    │   ├── Limit (empty base class)
    │   ├── SingleAxisLimit (min/max angle pair)
    │   ├── IndependentBallJointLimit (3-axis ball joint limit)
    │   ├── Displacement (position/orientation delta vector)
    │   ├── JacobianAxis (joint axis with free/locked state)
    │   ├── SvdInfo (SVD cache with damped/regular pseudo-inverse)
    │   └── InvertedJacobian (inverse matrix + source Jacobian pair)
    │
    ├── Context-dependent classes (receive IkEnforcerContext):
    │   ├── Jacobian (matrix with axis-column mapping)
    │   ├── AngleDeltas (cumulative angle delta vector)
    │   ├── NullspaceProjector (nullspace projection matrix)
    │   └── Constraint (abstract constraint base)
    │
    └── Constraint subclasses (context via Constraint base):
        ├── PositionConstraint (position IK constraint)
        └── OrientationConstraint (orientation IK constraint)
```

All 15 classes (1 interface + 14 extracted) live in
`org.lgna.ik.core.enforcer`. The extracted classes are package-private
(matching original inner-class visibility) except for `Displacement`,
`Jacobian`, `InvertedJacobian`, `PriorityLevel`, `Constraint`,
`PositionConstraint`, and `OrientationConstraint` which are `public`
(matching their original `public` inner-class declarations).

## Extracted classes

| Original inner class | New top-level class | Static? | Context? | Visibility | Approx lines |
| --- | --- | --- | --- | --- | --- |
| `Limit` | `Limit` | non-static (no state) | No | package-private | ~5 |
| `SingleAxisLimit` | `SingleAxisLimit` | non-static | No | package-private | ~20 |
| `IndependentBallJointLimit` | `IndependentBallJointLimit` | non-static | No | package-private | ~20 |
| `SvdInfo` | `SvdInfo` | non-static (no outer access) | No | package-private | ~85 |
| `Jacobian` | `Jacobian` | non-static | **Yes** | public | ~170 |
| `InvertedJacobian` | `InvertedJacobian` | non-static (no outer access) | No | public | ~15 |
| `Displacement` | `Displacement` | **static** | No | public | ~25 |
| `PriorityLevel` | `PriorityLevel` | non-static (no outer access) | No | public | ~80 |
| `Constraint` | `Constraint` | non-static | **Yes** | public (abstract) | ~95 |
| `PositionConstraint` | `PositionConstraint` | non-static | Via Constraint | public | ~50 |
| `OrientationConstraint` | `OrientationConstraint` | non-static | Via Constraint | public | ~55 |
| `JacobianAxis` | `JacobianAxis` | non-static (no outer access) | No | package-private | ~80 |
| `NullspaceProjector` | `NullspaceProjector` | non-static | **Yes** | package-private | ~100 |
| `AngleDeltas` | `AngleDeltas` | non-static | **Yes** | package-private | ~40 |

## File inventory

| File | Role | Approx lines |
| --- | --- | --- |
| `TightPositionalIkEnforcer.java` | Enforcer — no inner classes remain. Owns field declarations, constructor, algorithm orchestration (`convergenceLoop`, `clampingLoop`, `priorityLoop`), constraint factory, and angle-application methods. Implements `IkEnforcerContext`. | ~438 |
| `IkEnforcerContext.java` | Interface with 3 methods for axis-index mapping. | ~25 |
| `Limit.java` | Empty base class for joint limits. | ~5 |
| `SingleAxisLimit.java` | Single-axis min/max angle limit. | ~20 |
| `IndependentBallJointLimit.java` | Ball joint limit with 3 axis limits. | ~20 |
| `SvdInfo.java` | SVD decomposition cache with damped and regular pseudo-inverse computation. | ~85 |
| `Jacobian.java` | Jacobian matrix with augmentation constructor, multiplication, and inversion. Receives `IkEnforcerContext`. | ~170 |
| `InvertedJacobian.java` | Wrapper pairing an inverse matrix with its source `Jacobian`. | ~15 |
| `Displacement.java` | Position or orientation displacement vector with subtraction. | ~25 |
| `PriorityLevel.java` | Priority level with constraint list, augmented Jacobian/displacement computation. | ~80 |
| `Constraint.java` | Abstract constraint base with chain, Jacobian update logic. Receives `IkEnforcerContext`. | ~95 |
| `PositionConstraint.java` | Position IK constraint — desired position, Jacobian from linear velocity contributions. | ~50 |
| `OrientationConstraint.java` | Orientation IK constraint — desired orientation via axis-rotation decomposition. | ~55 |
| `JacobianAxis.java` | Joint axis representation with free/locked state. Dead code methods preserved. | ~80 |
| `NullspaceProjector.java` | Nullspace projection matrix. Receives `IkEnforcerContext`. | ~100 |
| `AngleDeltas.java` | Cumulative angle delta vector indexed by global axis index. Receives `IkEnforcerContext`. | ~40 |

All source files reside in
`core/story-api/src/main/java/org/lgna/ik/core/enforcer/`.

## IkEnforcerContext interface

The `IkEnforcerContext` interface abstracts the three pieces of state that
context-dependent inner classes accessed via the implicit outer reference:

```java
package org.lgna.ik.core.enforcer;

import java.util.List;
import java.util.Map;
import org.lgna.story.implementation.JointImp;

interface IkEnforcerContext {
  List<JacobianAxis> getIndexToAxis();
  Map<JacobianAxis, Integer> getAxisToIndex();
  int getGlobalIndexForAxis(JacobianAxis axis);
}
```

`TightPositionalIkEnforcer` implements this interface, exposing its existing
`indexToAxis` list, `axisToIndex` map, and `getGlobalIndexForAxis()` method:

```java
public class TightPositionalIkEnforcer extends IkEnforcer
    implements IkEnforcerContext {

  @Override
  public List<JacobianAxis> getIndexToAxis() {
    return indexToAxis;
  }

  @Override
  public Map<JacobianAxis, Integer> getAxisToIndex() {
    return axisToIndex;
  }

  @Override
  public int getGlobalIndexForAxis(JacobianAxis axis) {
    Integer globalIndex = axisToIndex.get(axis);
    assert globalIndex != null;
    return globalIndex;
  }
  // ...
}
```

The interface is package-private — it exists solely to decouple the extracted
classes from the concrete enforcer, not to create a public extension point.

However, Java interface methods are implicitly `public`. The enforcer's three
implementing methods (`getIndexToAxis()`, `getAxisToIndex()`,
`getGlobalIndexForAxis()`) must be declared `public` on the concrete class.
`getGlobalIndexForAxis()` was previously `private` (L747); the other two are
new methods exposing existing package-private fields. These methods provide
read-only access to axis-mapping state that was already package-private.

## Context injection pattern

Context-dependent classes receive `IkEnforcerContext` through their
constructors. This replaces the implicit `TightPositionalIkEnforcer.this`
reference that non-static inner classes held.

### Jacobian

Jacobian has three constructors, all of which must receive or propagate
context after extraction:

```java
public class Jacobian {
  private final IkEnforcerContext context;

  // Row-dimension constructor — used by NullspaceProjector.createProjected()
  public Jacobian(int rowDimension, JacobianAxis[] inputColumns,
                  IkEnforcerContext context) {
    this.context = context;
    // ...
  }

  // Augmenting constructor — used by PriorityLevel.augmentJacobians()
  // Propagates context from first input Jacobian
  public Jacobian(Jacobian[] jacobians) {
    this.context = jacobians[0].context;
    // ...
  }

  // Matrix constructor — used by Constraint.updateJacobianUsingVelocityContributions()
  public Jacobian(Matrix mj, JacobianAxis[] jacobianAxes,
                  IkEnforcerContext context) {
    this.context = context;
    this.matrix = mj;
    this.columnIndexToJacobianColumn = jacobianAxes;
  }
}
```

The augmenting constructor (`Jacobian(Jacobian[])`) propagates context from
the first input Jacobian. This is safe because all Jacobians in a single
enforcer share the same context.

Additionally, `Jacobian.multiplyDisplacementWithInverseForMoving()` (L268)
creates `new AngleDeltas(indexToAxis.size())`. After extraction this becomes
`new AngleDeltas(context.getIndexToAxis().size(), context)` — a secondary
context propagation path from Jacobian into AngleDeltas.

Beyond constructors, these Jacobian methods access the enforcer through
context after extraction:

| Method | Original outer reference | Extracted form |
| --- | --- | --- |
| `multiplyWithAngleDeltas()` (L260) | `getGlobalIndexForAxis(axis)` | `context.getGlobalIndexForAxis(axis)` |
| `multiplyDisplacementWithInverseForMoving()` (L285) | `indexToAxis.size()` | `context.getIndexToAxis().size()` |
| `multiplyDisplacementWithInverseForMoving()` (L293) | `getGlobalIndexForAxis(axis)` | `context.getGlobalIndexForAxis(axis)` |

### Constraint (and subclasses)

```java
public abstract class Constraint {
  protected final IkEnforcerContext context;

  public Constraint(Chain chain, IkEnforcerContext context) {
    this.chain = chain;
    this.context = context;
  }
}

public class PositionConstraint extends Constraint {
  public PositionConstraint(Chain chain, Point3 eeDesiredPosition,
                            IkEnforcerContext context) {
    super(chain, context);
    this.eeDesiredPosition = eeDesiredPosition;
  }
}
```

Constraint subclasses pass context through `super(chain, context)`. Their
own methods access `context.getIndexToAxis()` where they previously
accessed `indexToAxis` directly.

Specifically, `Constraint.updateJacobianUsingVelocityContributions()` (L466)
iterates over `context.getIndexToAxis()` to build the Jacobian matrix columns,
and creates `new Jacobian(mj, jacobianAxes, context)` at L519 — a context
propagation path from Constraint into Jacobian.

### NullspaceProjector

```java
class NullspaceProjector {
  private final IkEnforcerContext context;

  public NullspaceProjector(int numAxes, IkEnforcerContext context) {
    this.context = context;
    // ...
  }
}
```

NullspaceProjector methods also use context both directly and for propagation:

- `createProjected()` (L775) calls `context.getGlobalIndexForAxis()` for
  index mapping and creates `new Jacobian(..., context)` — a context
  propagation path from NullspaceProjector into Jacobian.
- `subtractInverseTimesJacobian()` (L808) calls
  `context.getGlobalIndexForAxis()` for local-to-global index conversion.

### AngleDeltas

```java
class AngleDeltas {
  private final IkEnforcerContext context;

  public AngleDeltas(int numAllPossibleAxes, IkEnforcerContext context) {
    this.context = context;
    // ...
  }
}
```

### Enforcer call-site changes

The enforcer passes `this` as `IkEnforcerContext` when constructing
context-dependent objects:

```java
// initializeListOfAxes() — L880
nullspaceProjector = new NullspaceProjector(indexToAxis.size(), this);

// convergenceLoop() — L930
angleDeltas = new AngleDeltas(indexToAxis.size(), this);

// createPositionConstraint() — L896
PositionConstraint positionConstraint = new PositionConstraint(chain, endPosition, this);
```

## Visibility rules

| Class | Original visibility | Extracted visibility | Reason |
| --- | --- | --- | --- |
| `Limit` | `class` (package-private inner) | `class` (package-private top-level) | No external references |
| `SingleAxisLimit` | `class` (package-private inner) | `class` (package-private top-level) | No external references |
| `IndependentBallJointLimit` | `class` (package-private inner) | `class` (package-private top-level) | No external references |
| `SvdInfo` | `class` (package-private inner) | `class` (package-private top-level) | No external references |
| `Jacobian` | `public class` inner | `public class` top-level | Referenced by PriorityLevel, Constraint (public) |
| `InvertedJacobian` | `public class` inner | `public class` top-level | Referenced by Jacobian (public) |
| `Displacement` | `public static class` inner | `public class` top-level | Referenced by PriorityLevel, Constraint (public) |
| `PriorityLevel` | `public class` inner | `public class` top-level | Fields/methods accessed by enforcer |
| `Constraint` | `public abstract class` inner | `public abstract class` top-level | Extended by PositionConstraint, OrientationConstraint |
| `PositionConstraint` | `public class` inner | `public class` top-level | Referenced by IKCore.java, IkProgram.java |
| `OrientationConstraint` | `public class` inner | `public class` top-level | Referenced by enforcer |
| `JacobianAxis` | `class` (package-private inner) | `class` (package-private top-level) | No external references |
| `NullspaceProjector` | `class` (package-private inner) | `class` (package-private top-level) | No external references |
| `AngleDeltas` | `class` (package-private inner) | `class` (package-private top-level) | No external references |

Each extracted class preserves the exact access level of its original
inner-class declaration (see table above). Member-level visibility changes
are limited to:

- 3 threshold constants (`private` → package-private) — see [Static constants](#static-constants)
- `Jacobian.matrixWasUpdated()` (`private` → package-private) — see [Method visibility widening](#method-visibility-widening-jacobianmatrixwasupdated)
- `TightPositionalIkEnforcer.getGlobalIndexForAxis()` (`private` → `public`) — required by `IkEnforcerContext` implementation
- 2 new `public` methods on the enforcer (`getIndexToAxis()`, `getAxisToIndex()`) — exposing existing package-private fields to satisfy the `IkEnforcerContext` interface

## Import fixups

Two files outside `org.lgna.ik.core.enforcer` imported an inner class by
qualified name and required a mechanical import update. See
[IK Enforcer Downstream Import Fixups](./ik-enforcer-downstream-import-fixups.md)
for the complete downstream fixup reference.

### IKCore.java (line 50) — fixed in PR #558

```java
// Before
import org.lgna.ik.core.enforcer.TightPositionalIkEnforcer.PositionConstraint;

// After
import org.lgna.ik.core.enforcer.PositionConstraint;
```

### IkProgram.java (line 56) — fixed in issue #557

```java
// Before
import org.lgna.ik.core.enforcer.TightPositionalIkEnforcer.PositionConstraint;

// After
import org.lgna.ik.core.enforcer.PositionConstraint;
```

This downstream consumer in `core/ide` was missed by PR #558 because it
resides in a different module. The import is the same mechanical change.

### TightPositionalIkEnforcer.java

The enforcer adds imports for all 14 extracted classes. Since all classes are
in the same package (`org.lgna.ik.core.enforcer`), no import statements are
actually needed — they are same-package references. The only imports that
remain are the external library imports (`Jama.Matrix`, `org.alice.math.*`,
etc.) already present.

## Static constants

Three static threshold constants remain in `TightPositionalIkEnforcer`:

```java
private static double MIN_ANGLE_IN_RADIANS_BEFORE_CONSTRAINT_IS_MET = Math.PI * .0001;
private static double MIN_DISTANCE_BEFORE_CONSTRAINT_IS_MET = .0001;
private static double MIN_DISTANCE_SQUARED_BEFORE_CONSTRAINT_IS_MET =
    MIN_DISTANCE_BEFORE_CONSTRAINT_IS_MET * MIN_DISTANCE_BEFORE_CONSTRAINT_IS_MET;
```

`PositionConstraint` and `OrientationConstraint` access these via
`TightPositionalIkEnforcer.MIN_DISTANCE_SQUARED_BEFORE_CONSTRAINT_IS_MET`
and `TightPositionalIkEnforcer.MIN_ANGLE_IN_RADIANS_BEFORE_CONSTRAINT_IS_MET`
respectively. The constants are widened from `private` to package-private to
allow this cross-class access within the same package.

| Constant | Used by |
| --- | --- |
| `MIN_ANGLE_IN_RADIANS_BEFORE_CONSTRAINT_IS_MET` | `OrientationConstraint.isMet()` |
| `MIN_DISTANCE_SQUARED_BEFORE_CONSTRAINT_IS_MET` | `PositionConstraint.isMet()` |
| `MIN_DISTANCE_BEFORE_CONSTRAINT_IS_MET` | Only by the squared-distance computation above |

## Method visibility widening: Jacobian.matrixWasUpdated

`Jacobian.matrixWasUpdated()` is `private` in the original inner class (L322).
It is called cross-class by:

- `NullspaceProjector.createProjected()` (L803): `result.matrixWasUpdated()`
- `Constraint.updateJacobianUsingVelocityContributions()` (L539): `jacobian.matrixWasUpdated()`

In the original code, inner classes of the same enclosing class can access each
other's `private` members. After extraction to separate top-level classes, this
no longer works. `matrixWasUpdated()` must be widened from `private` to
package-private.

| Member | Original | Extracted | Callers |
| --- | --- | --- | --- |
| `Jacobian.matrixWasUpdated()` | `private` | package-private | `NullspaceProjector`, `Constraint` |

## Self-reference fix: AngleDeltas.getForAxis

The original inner class `AngleDeltas.getForAxis()` contained a self-reference
through the enclosing class's field:

```java
// Original (inner class, L1006-1010)
public double getForAxis(JacobianAxis axis) {
  int globalIndex = axisToIndex.get(axis);           // outer's axisToIndex
  double delta = angleDeltas.getByGlobalIndex(globalIndex); // outer's angleDeltas field
  return delta;
}
```

The `angleDeltas` reference on line 1008 accessed the enforcer's
`angleDeltas` field. In every call site, `getForAxis()` is called on
that same `angleDeltas` field instance — the reference is always
`this == TightPositionalIkEnforcer.this.angleDeltas`.

After extraction, this becomes:

```java
// Extracted (top-level class)
public double getForAxis(JacobianAxis axis) {
  int globalIndex = context.getAxisToIndex().get(axis);
  double delta = this.getByGlobalIndex(globalIndex);  // self-reference
  return delta;
}
```

The `angleDeltas.getByGlobalIndex()` → `this.getByGlobalIndex()` replacement
is safe because the method is only ever called on the enforcer's `angleDeltas`
field, which is the same instance that held the implicit outer reference.

## Dead code preservation

Several methods in `JacobianAxis` throw `RuntimeException("Not implemented
method")` or `RuntimeException("Not completed method")`. These are dead code
in the current codebase but are preserved exactly as-is:

| Method | Exception message |
| --- | --- |
| `JacobianAxis.applyCorrespondingSingleDelta(double)` | `"Not completed method"` |
| `JacobianAxis.wentOverLimit()` | `"Not implemented method"` |
| `JacobianAxis.setToLimitAndReturnTheDifference()` | `"Not implemented method"` |

The original `applyAngleChangesAndClampingIfNecessary_originalEffort()` method
in the enforcer is also preserved. It calls `axisToIndex.get(axis)` directly
and `angleDeltas.getByGlobalIndex()`, which are updated to use context and
self-reference respectively.

Three unimplemented methods in the enforcer are also preserved:

| Method | Exception message |
| --- | --- |
| `lockViolatedBallJointLimits(...)` | `"Not implemented method"` |
| `turnJointBackToLimits(...)` | `"Not implemented method"` |
| `getViolatedBallJointLimits(...)` | `"Not implemented method"` |

## Error handling contract

All `RuntimeException` throws are preserved with identical messages:

| Location | Message |
| --- | --- |
| `Displacement.createThisMinusOther()` | `"Displacements have different lengths."` |
| `JacobianAxis.applyCorrespondingSingleDelta()` | `"Not completed method"` |
| `JacobianAxis.wentOverLimit()` | `"Not implemented method"` |
| `JacobianAxis.setToLimitAndReturnTheDifference()` | `"Not implemented method"` |
| `TightPositionalIkEnforcer.priorityLoop()` | `"priorities are not ordered " + ...` |
| `TightPositionalIkEnforcer.lockViolatedBallJointLimits()` | `"Not implemented method"` |
| `TightPositionalIkEnforcer.turnJointBackToLimits()` | `"Not implemented method"` |
| `TightPositionalIkEnforcer.getViolatedBallJointLimits()` | `"Not implemented method"` |

All `assert` statements are preserved in their original locations.

## Configuration

There is no runtime configuration for the decomposition. It uses the existing
Maven reactor, JUnit configuration, and JAMA matrix library.

From a fresh checkout or worktree, initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Validation

Run the focused `core/story-api` tests from the repository root:

```bash
mvn -pl core/story-api -am \
  -DfailIfNoTests=false \
  -Dcheckstyle.skip \
  test
```

For a compile-only check:

```bash
mvn -pl core/story-api -am compile
```

## Characterization tests

Eight characterization test files verify the extracted classes behave
identically to the original inner-class implementations. Tests use a
`IkEnforcerContextStub` that provides synthetic axis data without requiring
a full `JointedModelImp`.

All test files are in
`core/story-api/src/test/java/org/lgna/ik/core/enforcer/`.

| Test file | Tests | What it verifies |
| --- | --- | --- |
| `IkEnforcerContextStub.java` | — | Stub implementing `IkEnforcerContext` with configurable axis count. Provides `indexToAxis`, `axisToIndex`, and `getGlobalIndexForAxis()` backed by synthetic `JacobianAxis` instances. |
| `DisplacementTest.java` | 3 | Construction, `createThisMinusOther()` subtraction, length-mismatch `RuntimeException`. |
| `SvdInfoTest.java` | 3 | SVD decomposition, damped pseudo-inverse, regular pseudo-inverse for a known 3×2 matrix. |
| `AngleDeltasTest.java` | 4 | Construction with zero storage, `add()` accumulation, `getByGlobalIndex()`, `getForAxis()` self-reference correctness, `correctDeltaForAxis()`. |
| `JacobianTest.java` | 5 | Row-count constructor, augmenting constructor (multi-Jacobian merge), `multiplyWithAngleDeltas()`, `multiplyDisplacementWithInverseForMoving()`, matrix-update SVD invalidation. |
| `NullspaceProjectorTest.java` | 3 | Identity initialization, `createProjected()` passthrough on identity, `subtractInverseTimesJacobian()` effect on matrix. |
| `PriorityLevelTest.java` | 2 | `computeAugmentedJacobian()` augmentation, `areConstraintsMet()` delegation. |
| `LimitTest.java` | 3 | `SingleAxisLimit` min/max accessors, `IndependentBallJointLimit` field assignment, `Limit` base class instantiation. |

### IkEnforcerContextStub usage

```java
// Create a stub with 6 axes (2 joints × 3 axes each)
IkEnforcerContextStub stub = new IkEnforcerContextStub(6);

// Use it to construct context-dependent classes
AngleDeltas deltas = new AngleDeltas(6, stub);
Jacobian jacobian = new Jacobian(3, stub.getAxesArray(), stub);
NullspaceProjector projector = new NullspaceProjector(6, stub);
```

The stub creates `JacobianAxis` instances with null `JointImp` references
(sufficient for index-mapping tests) and populates `indexToAxis`/`axisToIndex`
with sequential indices.

## Acceptance criteria

1. `TightPositionalIkEnforcer.java` is under 500 lines (target: ~438).
2. All 14 inner classes have been extracted to separate top-level files.
3. `IkEnforcerContext` interface exists with exactly 3 methods.
4. `TightPositionalIkEnforcer` implements `IkEnforcerContext`.
5. No inner class declarations remain in `TightPositionalIkEnforcer.java`.
6. `IKCore.java` compiles with the updated import.
7. All characterization tests pass.
8. `mvn -pl core/story-api -am -DfailIfNoTests=false -Dcheckstyle.skip test` succeeds.
9. Visibility widening limited to: three threshold constants (`private` → package-private), `Jacobian.matrixWasUpdated()` (`private` → package-private), and 3 `IkEnforcerContext` implementation methods on the enforcer (`getGlobalIndexForAxis` from `private` to `public`; `getIndexToAxis` and `getAxisToIndex` as new `public` methods).
10. All `RuntimeException` messages preserved character-for-character.
11. All `assert` statements preserved in their original locations.
12. Dead code methods preserved exactly.

## Claim boundaries

This decomposition claims:

- **Only** the extraction of inner classes from `TightPositionalIkEnforcer`
  into top-level files in the same package.
- **Only** the introduction of `IkEnforcerContext` as a package-private
  interface with 3 methods.
- **Only** the `AngleDeltas.getForAxis()` self-reference fix
  (`angleDeltas.getByGlobalIndex()` → `this.getByGlobalIndex()`).
- **Only** the widening of 3 threshold constants from `private` to
  package-private, `Jacobian.matrixWasUpdated()` from `private` to
  package-private, and 3 `IkEnforcerContext` implementation methods on the
  enforcer (`getGlobalIndexForAxis` from `private` to `public`;
  `getIndexToAxis` and `getAxisToIndex` as new `public` methods exposing
  existing package-private fields).
- **Only** the import change in `IKCore.java` line 50.

This decomposition does **not** claim:

- Any behavioral changes to the IK solver algorithm.
- Any removal of dead code or TODO comments.
- Any changes to `IkEnforcer`, `JointedModelIkEnforcer`, `Chain`, `Bone`,
  `Solver`, or any other class outside `org.lgna.ik.core.enforcer`.
- Any new public API beyond the existing public inner-class surfaces and
  the 3 `IkEnforcerContext` implementation methods.
- Any performance optimizations.
- Any thread-safety changes.
