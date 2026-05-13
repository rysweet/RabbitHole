# ModelResourceExporter: Dead Inner Class and Wrapper Removal

This reference describes the removal of the dead `NamedFile` inner class and
three `shouldSuppress*` / `shouldHide*` wrapper methods from
`ModelResourceExporter`, plus the addition of a package-private getter to
support the inlining.

The cleanup is a pure internal refactor. All `core/model-loading` tests pass
without modification. The file drops from 722 lines to approximately 707 lines.

## Contents

- [Motivation](#motivation)
- [Architecture after cleanup](#architecture-after-cleanup)
- [Dead code removed](#dead-code-removed)
  - [NamedFile inner class](#namedfile-inner-class)
  - [shouldSuppressJoint wrapper](#shouldsuppressjoint-wrapper)
  - [shouldSuppressJointInArray wrapper](#shouldsuppressjointinarray-wrapper)
  - [shouldHideJointInArray wrapper](#shouldhidejointinarray-wrapper)
- [New getter added](#new-getter-added)
- [Caller migration in ModelResourceJavaGenerator](#caller-migration-in-modelresourcejavagenerator)
- [Public API](#public-api)
- [Package-private collaboration](#package-private-collaboration)
- [Security boundary](#security-boundary)
- [Configuration](#configuration)
- [Validation](#validation)
- [Acceptance criteria](#acceptance-criteria)

## Motivation

`ModelResourceExporter` still contained a dead `NamedFile` inner class (never
referenced by any code path) and three package-private wrapper methods —
`shouldSuppressJoint`, `shouldSuppressJointInArray`, and
`shouldHideJointInArray` — that did nothing but forward to the identically
named static methods on `ModelResourceJavaGenerator`, passing private list
fields as arguments.

The single caller for all three wrappers is `buildJavaCodeBody` in
`ModelResourceJavaGenerator`, which already has access to the exporter instance.
By adding one small getter (`getArraysToExposeFirstElementOf()`) to expose the
last needed list, the generator can call its own static methods directly and
the wrappers become dead code.

This follows the same extraction pattern used in the prior dead-code-removal
step (see
[model-resource-exporter-dead-code-removal.md](model-resource-exporter-dead-code-removal.md)).

## Architecture after cleanup

```text
ModelResourceExporter (export orchestrator, ~707 lines)
├── Configures model metadata (joints, tags, thumbnails, poses)
├── Manages XML resource file output
├── Delegates Java code generation to ModelResourceJavaGenerator
├── Provides joint tree manipulation (makeCodeReadyTree, etc.)
└── Exposes private state through package-private getters
    ├── getJointIdsToSuppress()         [existing]
    ├── getArraysToHideElementsOf()     [existing]
    └── getArraysToExposeFirstElementOf() [new]

ModelResourceJavaGenerator (Java source generator)
├── buildJavaCodeBody — assembles full enum Java source
│   ├── Calls shouldSuppressJoint(jointString, exporter.getJointIdsToSuppress())     [direct]
│   ├── Calls shouldSuppressJointInArray(jointString, arrayEntries, exporter.getArraysToExposeFirstElementOf()) [direct]
│   └── Calls shouldHideJointInArray(jointString, arrayEntries, exporter.getArraysToHideElementsOf())  [direct]
└── Static utility methods (shouldSuppressJoint, shouldSuppressJointInArray, etc.)
```

## Dead code removed

### NamedFile inner class

```java
// REMOVED from ModelResourceExporter
private class NamedFile {
  public String name;
  public File file;
  public NamedFile(String name, File file) { ... }
}
```

**Lines removed:** ~9 lines (including blank line).

**Why safe to remove:** A text search across all `.java` files confirms zero
references outside the class definition itself. No constructor call, field
declaration, local variable, or cast references `NamedFile` anywhere in the
codebase. The inner class was likely left over from a removed file-export path.

### shouldSuppressJoint wrapper

```java
// REMOVED from ModelResourceExporter
boolean shouldSuppressJoint(String jointString) {
  return ModelResourceJavaGenerator.shouldSuppressJoint(jointString, this.jointIdsToSuppress);
}
```

**Lines removed:** 3 lines.

**Why safe to remove:** The only caller is `buildJavaCodeBody` in
`ModelResourceJavaGenerator`. After inlining, the caller uses
`ModelResourceJavaGenerator.shouldSuppressJoint(jointString, exporter.getJointIdsToSuppress())`
directly — the `getJointIdsToSuppress()` getter already exists.

### shouldSuppressJointInArray wrapper

```java
// REMOVED from ModelResourceExporter
boolean shouldSuppressJointInArray(String jointString, Map<String, List<String>> arrayEntries) {
  return ModelResourceJavaGenerator.shouldSuppressJointInArray(
      jointString, arrayEntries, this.arraysToExposeFirstElementOf);
}
```

**Lines removed:** 3 lines.

**Why safe to remove:** Same single caller as above. The generator uses
`ModelResourceJavaGenerator.shouldSuppressJointInArray(jointString, arrayEntries,
exporter.getArraysToExposeFirstElementOf())` directly. The new getter provides
access to the previously private list.

### shouldHideJointInArray wrapper

```java
// REMOVED from ModelResourceExporter
boolean shouldHideJointInArray(String jointString, Map<String, List<String>> arrayEntries) {
  return ModelResourceJavaGenerator.shouldHideJointInArray(
      jointString, arrayEntries, this.arraysToHideElementsOf);
}
```

**Lines removed:** 3 lines.

**Why safe to remove:** Same single caller. The generator uses
`ModelResourceJavaGenerator.shouldHideJointInArray(jointString, arrayEntries,
exporter.getArraysToHideElementsOf())` directly — the
`getArraysToHideElementsOf()` getter already exists.

## New getter added

```java
// NEW in ModelResourceExporter
List<String> getArraysToExposeFirstElementOf() {
  return this.arraysToExposeFirstElementOf;
}
```

**Lines added:** 3 lines.

This getter follows the exact pattern of the existing `getJointIdsToSuppress()`
and `getArraysToHideElementsOf()` getters. It is package-private, returns the
raw mutable list (consistent with all other exporter getters), and has no
behavioral side effects.

## Caller migration in ModelResourceJavaGenerator

The only changed caller is `buildJavaCodeBody`. Three call sites change from
calling wrappers on the exporter to calling static methods directly:

| Before (exporter wrapper) | After (direct static call) |
|---|---|
| `exporter.shouldSuppressJoint(jointString)` | `shouldSuppressJoint(jointString, exporter.getJointIdsToSuppress())` |
| `exporter.shouldSuppressJointInArray(jointString, arrayEntries)` | `shouldSuppressJointInArray(jointString, arrayEntries, exporter.getArraysToExposeFirstElementOf())` |
| `exporter.shouldHideJointInArray(jointString, arrayEntries)` | `shouldHideJointInArray(jointString, arrayEntries, exporter.getArraysToHideElementsOf())` |

Since the static methods are in the same class (`ModelResourceJavaGenerator`),
the calls use short names with no import changes.

## Public API

There is no change to any public API visible to Alice IDE users or Tweedle
programs. `shouldHideJointsOfArray(String)` remains **public** on
`ModelResourceExporter` — it is not a wrapper for the generator and has callers
outside the package. Only the three package-private `shouldSuppress*` /
`shouldHide*InArray` wrappers are removed.

## Package-private collaboration

```text
ModelResourceExporter
  │
  │  createJavaCode()
  │  └── calls ModelResourceJavaGenerator.buildJavaCodeBody(this)
  │        ├── calls shouldSuppressJoint(joint, exporter.getJointIdsToSuppress())
  │        ├── calls shouldSuppressJointInArray(joint, entries, exporter.getArraysToExposeFirstElementOf())
  │        ├── calls shouldHideJointInArray(joint, entries, exporter.getArraysToHideElementsOf())
  │        └── (other existing calls unchanged)
  │
  │  Getters consumed by ModelResourceJavaGenerator:
  │  ├── getJointIdsToSuppress()                  [existing]
  │  ├── getArraysToHideElementsOf()              [existing]
  │  ├── getArraysToExposeFirstElementOf()         [new]
  │  ├── getClassData(), getJointList(), etc.      [existing]
  │  └── isEnableArraySupport(), getPoses(), etc.  [existing]
```

## Security boundary

No security impact. All changes are internal refactoring within the same Java
package. No new inputs are accepted, no I/O paths change, and no new
dependencies are introduced.

## Configuration

No new configuration is needed. This refactoring does not introduce any
properties, environment variables, or configuration files.

## Validation

Run the full `core/model-loading` test suite:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
  mvn -pl core/model-loading -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

All tests must pass with 0 failures.

Run checkstyle separately:

```bash
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml -pl core/model-loading
```

## Acceptance criteria

1. **Dead inner class removed:** `NamedFile` no longer exists in
   `ModelResourceExporter`.
2. **Dead wrappers removed:** `shouldSuppressJoint(String)`,
   `shouldSuppressJointInArray(String, Map)`, and
   `shouldHideJointInArray(String, Map)` no longer exist in
   `ModelResourceExporter`.
3. **New getter added:** `getArraysToExposeFirstElementOf()` is a package-private
   getter in `ModelResourceExporter` returning `List<String>`.
4. **Caller updated:** `buildJavaCodeBody` in `ModelResourceJavaGenerator` calls
   the static methods directly using the exporter's getters.
5. **Public API unchanged:** `shouldHideJointsOfArray(String)` remains public.
6. **Tests pass:** All `core/model-loading` tests pass.
7. **Checkstyle passes:** No style violations in `core/model-loading`.
8. **Line count:** `ModelResourceExporter` is under 710 lines.
