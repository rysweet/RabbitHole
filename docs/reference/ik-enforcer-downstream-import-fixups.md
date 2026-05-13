# IK Enforcer Downstream Import Fixups

This reference documents the import fixups required in downstream consumers
after PR #558 extracted 14 inner classes from `TightPositionalIkEnforcer` into
top-level classes in `org.lgna.ik.core.enforcer`.

Issue: #557

## Contents

- [Background](#background)
- [Affected files](#affected-files)
- [Import change pattern](#import-change-pattern)
- [API compatibility](#api-compatibility)
- [Validation](#validation)
- [Claim boundaries](#claim-boundaries)

## Background

PR #558 decomposed `TightPositionalIkEnforcer` (1328 lines) by extracting all
14 inner classes into separate top-level files in the same package
(`org.lgna.ik.core.enforcer`). See
[TightPositionalIkEnforcer Inner Class Extraction](./tight-positional-ik-enforcer-decomposition.md)
for the full decomposition reference.

Any file outside `org.lgna.ik.core.enforcer` that previously imported an inner
class via a qualified name like
`org.lgna.ik.core.enforcer.TightPositionalIkEnforcer.PositionConstraint` must
update its import to the new top-level class
`org.lgna.ik.core.enforcer.PositionConstraint`.

## Affected files

Two files in the repository referenced `TightPositionalIkEnforcer` inner
classes by qualified import:

| File | Module | Old import | New import |
| --- | --- | --- | --- |
| `core/story-api/src/main/java/org/lgna/ik/core/IKCore.java` (line 50) | `core/story-api` | `...TightPositionalIkEnforcer.PositionConstraint` | `...enforcer.PositionConstraint` |
| `core/ide/src/main/java/test/ik/IkProgram.java` (line 56) | `core/ide` | `...TightPositionalIkEnforcer.PositionConstraint` | `...enforcer.PositionConstraint` |

The `IKCore.java` fixup was included in PR #558 (same module as the
extraction). The `IkProgram.java` fixup was missed because it resides in the
downstream `core/ide` module, which was not in the PR #558 compile scope.

No other `.java` files in the main source tree reference
`TightPositionalIkEnforcer` inner classes by qualified import.

## Import change pattern

The fix is a mechanical one-line import replacement. The class identity,
package, public API, and runtime behavior are all unchanged — only the
declaration site moved from inner class to top-level.

```java
// Before (inner class import — fails to compile after PR #558)
import org.lgna.ik.core.enforcer.TightPositionalIkEnforcer.PositionConstraint;

// After (top-level class import — compiles correctly)
import org.lgna.ik.core.enforcer.PositionConstraint;
```

All usages of `PositionConstraint` in the file (field declarations, method
calls, local variables) remain unchanged. The unqualified type name
`PositionConstraint` resolves to the same class.

## API compatibility

| Aspect | Status |
| --- | --- |
| Class identity | Same `PositionConstraint` class, same bytecode |
| Package | Same: `org.lgna.ik.core.enforcer` |
| Public methods | Unchanged: `setEeDesiredPosition(Point3)`, `isMet()`, `computeDesiredDisplacement()`, `computeJacobian()` |
| Constructor | Unchanged: `PositionConstraint(Chain, Point3, IkEnforcerContext)` |
| Visibility | `public` (same as original inner class) |
| Superclass | `Constraint` (unchanged) |
| Binary compatibility | Not preserved — recompilation required (inner-to-top-level changes the class file name from `TightPositionalIkEnforcer$PositionConstraint.class` to `PositionConstraint.class`) |

## Validation

Compile the `core/ide` module and its transitive dependencies:

```bash
mvn -pl core/ide -am \
  -DskipTests \
  -DincludeSims=false \
  -Dinstall4j.skip \
  compile
```

Expected: `BUILD SUCCESS` across 21 modules with zero compilation errors.

To verify no remaining inner-class references:

```bash
grep -rn 'TightPositionalIkEnforcer\.' \
  --include='*.java' \
  core/
```

Expected: zero hits for qualified inner-class references
(`TightPositionalIkEnforcer.PositionConstraint`,
`TightPositionalIkEnforcer.OrientationConstraint`, etc.). Hits for
`TightPositionalIkEnforcer` alone (the class name without a dot-suffix) are
expected and correct — those reference the enforcer class itself.

## Claim boundaries

This fixup makes **no behavioral claims**. It is a mechanical import path
correction that restores compilation after the inner-class extraction in
PR #558. The IK solver behavior, constraint evaluation, and test harness are
entirely unaffected.
