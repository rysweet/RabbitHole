# ModelResourceExporter Dead Code Removal and Static Method Extraction

This reference describes the cleanup of `ModelResourceExporter` (previously
886 lines) by removing dead code and extracting public static code-generation
utilities into `ModelResourceJavaGenerator`.

The cleanup is a pure internal refactor. The package-private collaboration
between `ModelResourceExporter` and `ModelResourceJavaGenerator` is unchanged
from the caller's perspective. All 49 existing `core/model-loading` tests pass
without modification.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Dead code removed](#dead-code-removed)
- [Methods extracted to ModelResourceJavaGenerator](#methods-extracted-to-modelresourcejavagenerator)
  - [getExistingJointIds](#getexistingjointids)
  - [getAccessorMethodsForResourceClass](#getaccessormethodsforresourceclass)
  - [getJointAccessCodeForClass](#getjointaccesscodeforclass)
  - [main (developer CLI)](#main-developer-cli)
- [Caller migration](#caller-migration)
  - [Imports added to ModelResourceJavaGenerator](#imports-added-to-modelresourcejavagenerator)
- [Public API](#public-api)
- [Package-private collaboration](#package-private-collaboration)
- [Security boundary](#security-boundary)
- [Configuration](#configuration)
- [Validation](#validation)
- [Acceptance criteria](#acceptance-criteria)

## Motivation

`ModelResourceExporter` accumulated dead code over time — methods with zero
callers, a mutable static cache field for an unused heuristic, private wrapper
methods that duplicated logic already in `ModelResourceJavaGenerator`, and
developer-only utilities that belong in the code-generation helper class rather
than the export coordinator.

RabbitHole issue #524 removes the dead code and extracts the static
code-generation utilities into `ModelResourceJavaGenerator`, where they sit
alongside the other Java source generation logic. This reduces
`ModelResourceExporter` to its core responsibility: orchestrating the export of
3D model resources to XML and Java enum source files.

## Architecture

```text
ModelResourceExporter (export orchestrator, ~707 lines)
├── Configures model metadata (joints, tags, thumbnails, poses)
├── Manages XML resource file output
├── Delegates Java code generation to ModelResourceJavaGenerator
├── Provides joint tree manipulation (makeCodeReadyTree, etc.)
└── Exposes private state through package-private getters
    ├── getJointIdsToSuppress()
    ├── getArraysToHideElementsOf()
    └── getArraysToExposeFirstElementOf()

ModelResourceJavaGenerator (Java source generator, ~530 lines after extraction)
├── buildJavaCodeBody — assembles full enum Java source
├── getExistingJointIds — reflects JointId fields from parent class  [extracted]
├── getAccessorMethodsForResourceClass — generates accessor source  [extracted]
├── getJointAccessCodeForClass — generates joint access method source  [extracted]
├── getAccessorMethodName — converts enum name to accessor method name  [existing]
├── needsAccessorMethodForFieldName — checks interface requirements  [existing]
└── main — developer CLI entry point for diagnostics  [extracted]
```

## Dead code removed

The following members were removed from `ModelResourceExporter` because they
have zero callers across the entire codebase.

| Member | Kind | Lines | Reason for removal |
|--------|------|-------|--------------------|
| `isMoreRecentThan(Date, File)` | method | 247–254 | Unreferenced date comparison — no callers |
| `isMoreRecentThan(Date, Date)` | method | 256–261 | Unreferenced date comparison — no callers |
| `POTENTIAL_MODEL_CLASS_DATA_OPTIONS` | static field | 529 | Mutable cache for removed heuristic |
| `getBestClassDataForJointList(List<Tuple2>)` | method | 531–568 | Heuristic joint-matching — never invoked |
| `getExistingJointIdPairs(Class<?>)` | method | 571–598 | Only called by `getBestClassDataForJointList` |
| `getAccessorMethodName(String)` | private method | 707–709 | Dead wrapper — duplicates `ModelResourceJavaGenerator.getAccessorMethodName` |
| `needsAccessorMethodForFieldName(ModelClassData, String)` | private method | 711–715 | Dead wrapper — delegates to `ModelResourceJavaGenerator.needsAccessorMethodForFieldName` |
| Commented-out `getBestClassDataForJointList` call | comment | 362–365 | Stale reference to removed method |
| Commented-out sample accessor method | comment | 864–868 | Stale example — moves with extracted `getJointAccessCodeForClass` |

### Why these methods are safe to remove

- **`isMoreRecentThan`**: A text search across all `.java` files confirms zero
  references outside the method definitions themselves. The export pipeline uses
  `hasNewData` and `forceRebuildCode` flags instead.

- **`getBestClassDataForJointList` and `POTENTIAL_MODEL_CLASS_DATA_OPTIONS`**:
  The heuristic was designed to auto-detect the best `ModelClassData` for a
  joint list by scoring against known resource superclasses. No code path calls
  it — callers pass `ModelClassData` explicitly via the constructor.

- **`getExistingJointIdPairs`**: A package-private helper used exclusively by
  `getBestClassDataForJointList`. With that method removed, this helper has no
  remaining callers. The similar `getExistingJointIds` (which returns
  `List<String>` rather than `List<Tuple2>`) remains in use and was extracted.

- **`getAccessorMethodName` (private)**: An instance method on the Exporter
  with identical logic to `ModelResourceJavaGenerator.getAccessorMethodName`.
  No caller invokes the private version — all call sites already use the
  Generator's static version directly (e.g., line 704).

- **`needsAccessorMethodForFieldName` (private)**: A one-line wrapper that
  delegates to `ModelResourceJavaGenerator.needsAccessorMethodForFieldName`.
  No caller invokes this wrapper — all call sites use the Generator directly.

## Methods extracted to ModelResourceJavaGenerator

These methods are static utilities that generate or inspect Java source
code. They belong in `ModelResourceJavaGenerator`, which already contains all
other Java code-generation logic.

### getExistingJointIds

```java
// ModelResourceJavaGenerator.java
static List<String> getExistingJointIds(Class<?> resourceClass)
```

Recursively reflects `JointId` fields from `resourceClass` and all its
superinterfaces. Returns a flat list of field names representing joint IDs
already defined in the parent resource class hierarchy.

**Usage:** Called by `ModelResourceJavaGenerator.buildJavaCodeBody()` to
determine which joint IDs are inherited and should not be re-declared in the
generated enum source.

**Example:**

```java
// Get joint IDs already defined by BipedResource
List<String> existingIds = ModelResourceJavaGenerator.getExistingJointIds(BipedResource.class);
// Returns: ["LEFT_HIP", "RIGHT_HIP", "PELVIS_LOWER_BODY", ...]
```

### getAccessorMethodsForResourceClass

```java
// ModelResourceJavaGenerator.java
public static String getAccessorMethodsForResourceClass(Class<? extends JointedModelResource> resourceClass)
```

Reflects `JointId` fields from `resourceClass` (via `getExistingJointIds`) and
generates Java source code for `get*()` accessor methods — one method per joint
ID. Returns the generated source as a single `String`.

**Usage:** Developer diagnostic tool — prints the accessor method source that
a resource implementation would need. Not called by the automated export pipeline.

**Example:**

```java
String accessorSource = ModelResourceJavaGenerator.getAccessorMethodsForResourceClass(BipedResource.class);
// Returns Java source like:
// public Joint getLeftHip() {
//     return org.lgna.story.Joint.getJoint( this, org.lgna.story.resources.BipedResource.LEFT_HIP);
// }
// ...
```

### getJointAccessCodeForClass

```java
// ModelResourceJavaGenerator.java
public static String getJointAccessCodeForClass(Class<?> resourceClass)
```

Near-duplicate of `getAccessorMethodsForResourceClass` above. The key difference
is that this method emits **fully-qualified** type names
(`org.lgna.story.Joint`, `resourceClass.getName()`) while
`getAccessorMethodsForResourceClass` uses short names (`Joint`,
`resourceClass.getCanonicalName()`). Both iterate `getExistingJointIds` and
produce one `get*()` method per joint ID.

**Usage:** Developer diagnostic tool — prints the accessor method source that
a generated enum would need. Not called by the automated export pipeline.

**Example output:**

```java
public org.lgna.story.Joint getLeftHip() {
	return org.lgna.story.Joint.getJoint( this, org.lgna.story.resources.BipedResource.LEFT_HIP );
}
```

### main (developer CLI)

```java
// ModelResourceJavaGenerator.java
public static void main(String[] args)
```

A developer convenience entry point that prints the joint access code for
`BipedResource` to stdout. Useful for inspecting what accessor methods a new
biped enum resource would need.

**Usage:**

```bash
mvn -pl core/model-loading exec:java \
  -Dexec.mainClass=org.lgna.story.resourceutilities.ModelResourceJavaGenerator
```

## Caller migration

The only internal caller that changed is the `buildJavaCodeBody` method in
`ModelResourceJavaGenerator`:

| Before | After |
|--------|-------|
| `ModelResourceExporter.getExistingJointIds(...)` | `ModelResourceJavaGenerator.getExistingJointIds(...)` |

Since both classes are in the same package (`org.lgna.story.resourceutilities`)
and `getExistingJointIds` was already package-private `static`, the migration is
a one-line change.

No callers exist outside `core/model-loading` for any of the moved methods.

### Imports added to ModelResourceJavaGenerator

The extracted methods reference types not yet imported by the Generator:

| Import | Needed by |
|--------|-----------|
| `org.lgna.story.implementation.alice.AliceResourceClassUtilities` | `getAccessorMethodsForResourceClass`, `getJointAccessCodeForClass` (calls `getAliceMethodNameForEnum`) |
| `org.lgna.story.resources.JointedModelResource` | `getAccessorMethodsForResourceClass` (parameter type) |

`BipedResource` is already imported in `ModelResourceJavaGenerator` (used by
existing pose code), so the extracted `main` method needs no new import.

## Public API

There is no change to any public API visible to Alice IDE users or Tweedle
programs. `ModelResourceExporter` and `ModelResourceJavaGenerator` are
package-private collaborators within `org.lgna.story.resourceutilities`. The
generated Java enum source files are identical before and after this refactor.

## Package-private collaboration

```text
ModelResourceExporter
  │
  │  createJavaCode()
  │  ├── calls ModelResourceJavaGenerator.buildJavaCodeBody(this)
  │  │     ├── calls ModelResourceJavaGenerator.getExistingJointIds(superClass)  ← moved here
  │  │     ├── calls ModelResourceJavaGenerator.appendPreambleAndEnumConstants(sb, this)
  │  │     ├── calls ModelResourceJavaGenerator.getAccessorMethodName(arrayName)
  │  │     └── calls ModelResourceJavaGenerator.getArrayNameFromMapForJoint(joint, map)
  │  └── writes result to javaFile
  │
  │  createXMLFile()
  │  └── writes XML resource descriptor
  │
  └── accessors (getClassData, getJointList, getJavaClassName, etc.)
       └── consumed by ModelResourceJavaGenerator via exporter parameter
```

## Security boundary

No security impact. All changes are internal refactoring within the same Java
package. No new inputs are accepted, no I/O paths change, and no new
dependencies are introduced.

## Configuration

No new configuration is needed. The refactoring does not introduce any
properties, environment variables, or configuration files.

## Validation

Run the full `core/model-loading` test suite:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
  mvn -pl core/model-loading -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

All 49 tests must pass with 0 failures.

Run checkstyle separately:

```bash
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml -pl core/model-loading
```

## Acceptance criteria

1. **Dead code removed:** `isMoreRecentThan` (both overloads),
   `POTENTIAL_MODEL_CLASS_DATA_OPTIONS`, `getBestClassDataForJointList`,
   `getExistingJointIdPairs`, private `getAccessorMethodName`, and private
   `needsAccessorMethodForFieldName` no longer exist in `ModelResourceExporter`.
2. **Methods extracted:** `getExistingJointIds`,
   `getAccessorMethodsForResourceClass`, `getJointAccessCodeForClass`, and
   `main` exist in `ModelResourceJavaGenerator` with identical signatures and
   behavior.
3. **Caller updated:** `buildJavaCodeBody` calls
   `ModelResourceJavaGenerator.getExistingJointIds` instead of
   `ModelResourceExporter.getExistingJointIds`.
4. **Tests pass:** All 49 `core/model-loading` tests pass.
5. **Checkstyle passes:** No style violations in `core/model-loading`.
6. **No unused imports:** After removal, verify no unused explicit imports
   remain in `ModelResourceExporter`. Note: `AliceResourceClassUtilities` is
   still used by `getJointRootsField` (line 620) and `getJavaClassName`
   (line 722), so its explicit import stays. `Date` remains used by the
   `lastEdited` field (line 104) and setters (lines 224, 228) — it comes via
   `java.util.*`. `BipedResource` is no longer referenced after extracting
   `main` and removing the commented-out example, but it enters via the
   wildcard `org.lgna.story.resources.*` which stays for `JointId`,
   `JointedModelResource`, `ImplementationAndVisualType`, etc.
