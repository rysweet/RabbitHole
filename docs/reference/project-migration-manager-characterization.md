# ProjectMigrationManager Migration Characterization

This reference describes the `core/story-api-migration` characterization layer
for `ProjectMigrationManager` text migration behavior.

## Contents

- [Package](#package)
- [Characterization scope](#characterization-scope)
- [Protected behavior](#protected-behavior)
- [Characterized contracts](#characterized-contracts)
- [Usage](#usage)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Package

`ProjectMigrationManager` and its text migration pipeline live in:

```text
core/story-api-migration/src/main/java/org/lgna/project/migration/
```

The characterization tests live in the matching test package:

```text
core/story-api-migration/src/test/java/org/lgna/project/migration/ProjectMigrationManagerTest.java
```

Tests in this package can use package-visible and protected migration seams
without widening production APIs.

## Characterization scope

The characterization feature protects existing migration behavior before
`ProjectMigrationManager` is extracted or refactored. It does not rewrite the
large migration table and does not add external archive fixtures.

Production code is intentionally unchanged; this documents existing protected
migration hotspots through characterization coverage.

The covered surface is deliberately narrow:

| Scope | Contract |
| --- | --- |
| Migration owner | `ProjectMigrationManager` remains the authoritative ordered table for Alice project text and AST migrations. |
| Table invariants | Text and AST migration result versions stay valid, round-trippable, and strictly increasing. |
| Version gates | A migration applies only when the saved project version is older than that migration's result version. |
| Test fixture shape | Characterization uses generated Java strings that look like Alice project XML fragments. |
| Fixture storage | No `.a3p`, `.a3w`, `.a3c`, media, or Git LFS payload is required. |
| Protected behavior | Selected legacy story/resource names rewrite through the existing text migrations that apply to their saved source versions. |
| Refactor boundary | Extraction is safe only when the same source version produces the same final migrated text. |

This feature is a compatibility guard, not a new migration framework or a
complete characterization of every migration entry.

## Protected behavior

Alice project files can contain serialized class names in XML attributes such as
`type` and `declaringClass`. Historical projects may refer to old resource
packages and old resource class names.

The characterization layer protects four kinds of behavior:

1. The migration tables remain sorted by increasing result version, and each
   result version can round-trip through `Version`.
2. Text migrations remain version-gated; a migration with result version
   `3.1.20.0.0` applies to `3.1.19.0.0`, but not to `3.1.20.0.0` or later.
3. Selected legacy text rewrites still compose across migration versions in one
   `migrate(String, Version)` call.
4. One selected boundary rewrite remains source-version gated at
   `3.2.111.0.0`: `BONE_PILE` on `BonesResource` migrates to `DEFAULT` on
   `BonePileResource` only when the saved project version predates that
   boundary.

The focused dresser seam starts from Alice version `3.1.19.0.0` and protects
this migration chain:

| Step | Input | Output |
| --- | --- | --- |
| Historical package move | `org.lgna.story.resources.dresser.DresserCentralAsian` | `org.lgna.story.resources.prop.DresserCentralAsian` |
| Family resource consolidation | `org.lgna.story.resources.prop.DresserCentralAsian` | `org.lgna.story.resources.prop.Dresser` |
| Current resource type consolidation | `org.lgna.story.resources.prop.Dresser` | `org.lgna.story.resources.prop.DresserResource` |

The important behavior is the cascade across versions. A migration that stops at
`org.lgna.story.resources.prop.DresserCentralAsian` or
`org.lgna.story.resources.prop.Dresser` is incomplete for old projects that
start before all mappings. A migration that applies the mappings out of order can
also leave an intermediate class name behind.

The same rule applies whether the serialized class name appears as a type or as
a declaring class:

```xml
<type name="org.lgna.story.resources.dresser.DresserCentralAsian"/>
<declaringClass name="org.lgna.story.resources.dresser.DresserCentralAsian"/>
```

Both entries migrate to:

```xml
<type name="org.lgna.story.resources.prop.DresserResource"/>
<declaringClass name="org.lgna.story.resources.prop.DresserResource"/>
```

The focused BonePile boundary starts from Alice version `3.2.110.0.0` and
protects one existing rewrite in the `3.2.111.0.0` text migration:

| Source version | Input | Output |
| --- | --- | --- |
| `3.2.110.0.0` | `name="BONE_PILE"` with `org.lgna.story.resources.prop.BonesResource` | `name="DEFAULT"` with `org.lgna.story.resources.prop.BonePileResource` |
| `3.2.111.0.0` | Same source text | Unchanged for this selected rewrite |
| `3.2.112.0.0` | Same source text | Unchanged for this selected rewrite |

This boundary test verifies that one existing rewrite is gated by source
version at the boundary or later. It does not claim that every migration is
idempotent or that every boundary-later source is unchanged for unrelated
rewrites.

## Usage

Use this characterization when changing migration ordering, splitting the
migration table, extracting helper classes, or reviewing a refactor around
`ProjectMigrationManager`.

The safe workflow is:

1. Choose one historical behavior seam.
2. Build the smallest XML-string fixture that exercises it.
3. Start from the version that makes all required migrations applicable.
4. Assert the exact final migrated text.
5. Assert the legacy and intermediate names are absent.
6. Run the focused migration tests before making production extraction changes.

For the dresser seam, the fixture starts at `3.1.19.0.0` because that version is
before the package move to `org.lgna.story.resources.prop.DresserCentralAsian`.
Starting at `3.1.20.0.0` deliberately does not back-apply that package move;
Alice migration behavior is version-gated by the saved project version.

For the BonePile boundary, the fixture starts at `3.2.110.0.0` to make the
`3.2.111.0.0` rewrite applicable. Starting at `3.2.111.0.0` leaves the selected
`BONE_PILE`/`BonesResource` source text unchanged because the project is already
at the rewrite result version. Starting at `3.2.112.0.0` protects the same
selected rewrite from being back-applied after the boundary.

## Characterized contracts

The `ProjectMigrationManagerTest` suite covers these contracts:

| Test contract | Behavior protected |
| --- | --- |
| Text migration result versions are valid, round-trippable, and increasing. | Refactors cannot reorder, duplicate, or corrupt text migration version boundaries. |
| AST migration result versions are valid, round-trippable, and increasing. | The same ordering invariant is preserved for AST migration entries. |
| Text migration applicability is strictly before the result version. | A saved project at the threshold version does not receive that threshold migration again. |
| Known legacy story and resource names rewrite from `3.1.19.0.0`. | Representative existing mappings such as `Program`, `INDIA_BRICK_D`, mouse-click event classes, `STurnable`, and dresser resources still migrate. |
| Legacy dresser XML fragments cascade to `DresserResource`. | The old package name moves through both intermediate prop names and reaches the current resource type. |
| Version `3.2.111.0.0` BonePile rewrite is source-version gated. | `BONE_PILE` on `BonesResource` rewrites to `DEFAULT` on `BonePileResource` from `3.2.110.0.0`, but the selected source text remains unchanged from `3.2.111.0.0`. |
| At-threshold or later selected text is unchanged. | Version-gated behavior remains observable and prevents accidental back-application of the selected rewrite at `3.2.111.0.0` or `3.2.112.0.0`. |

These contracts describe the feature boundary. They do not require a broad
fixture corpus, binary project archives, or a rewrite of the migration manager.

## API reference

### `ProjectMigrationManager`

Use the singleton migration manager:

```java
ProjectMigrationManager manager = ProjectMigrationManager.getInstance();
```

`ProjectMigrationManager` owns the ordered arrays of `TextMigration` and
`AstMigration` instances for Alice projects. The ordering is part of the
compatibility contract because each applied migration advances the working
version.

### `MigrationManager.migrate(String, Version)`

Text migration uses the `MigrationManager` API:

```java
String migrated = manager.migrate(source, new Version("3.1.19.0.0"));
```

The method applies every `TextMigration` whose result version is newer than the
current working version. After each applicable migration runs, the working
version advances to that migration's result version. Later migrations therefore
see the text produced by earlier migrations.

### `TextMigration`

`TextMigration` stores ordered regex replacement pairs:

```java
new TextMigration(new Version("3.1.20.0.0"), "oldName", "newName")
```

Each pair runs against the current text and returns the replacement result. The
characterization treats these replacements as observable migration behavior, so
refactors must preserve both applicability and ordering.

## Configuration

There is no Alice runtime configuration for this characterization. The behavior
is fixed by the saved project version and the migration table compiled into
`ProjectMigrationManager`.

`TextMigration` has an optional diagnostic system property:

```bash
-Dorg.lgna.project.migration.TextMigration.isSanityCheckingDesired=true
```

The characterization does not require that property. It is only a duplicate
replacement diagnostic for developers investigating the migration table.

From a fresh checkout or worktree, initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Focused migration characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.migration.ProjectMigrationManagerTest \
  test
```

Full story API migration module validation:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

Repository-level Python contract validation:

This suite is useful for whole-repository policy checks. It is not
migration-characterization evidence because it also covers branch, PR, and
documentation contracts outside `ProjectMigrationManager`.

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests
```

## Compatibility rules

Tests and refactors around `ProjectMigrationManager` preserve these rules:

1. Text migrations remain ordered by increasing result version.
2. A migration applies only when the saved project version is older than the migration result version.
3. After a text migration applies, later migrations receive the migrated text and the advanced working version.
4. Legacy names can migrate through intermediate names in a single `migrate(String, Version)` call.
5. A fixture that starts before the dresser package move reaches `org.lgna.story.resources.prop.DresserResource`.
6. A fixture that starts at the package-move threshold does not back-apply older migrations.
7. The `3.2.111.0.0` BonePile rewrite applies to `3.2.110.0.0` source text and is not back-applied to the selected source text at `3.2.111.0.0` or `3.2.112.0.0`.
8. Characterization fixtures stay generated, lightweight, deterministic, and LFS-free.
9. Refactors do not broaden regex replacements beyond known Alice legacy class and resource names.
10. Tests assert both the expected final names and the absence of obsolete or intermediate names when the selected seam has intermediate names.
11. Production migration behavior stays compatible with the current Alice 3 baseline unless a behavior change is explicitly documented and covered.

## Examples

### Legacy dresser XML fragment

Input:

```xml
<type name="org.lgna.story.resources.dresser.DresserCentralAsian"/>
<declaringClass name="org.lgna.story.resources.dresser.DresserCentralAsian"/>
```

Call:

```java
String migrated = ProjectMigrationManager.getInstance()
    .migrate(source, new Version("3.1.19.0.0"));
```

Expected output:

```xml
<type name="org.lgna.story.resources.prop.DresserResource"/>
<declaringClass name="org.lgna.story.resources.prop.DresserResource"/>
```

The final output must not contain:

```text
org.lgna.story.resources.dresser.DresserCentralAsian
org.lgna.story.resources.prop.DresserCentralAsian
org.lgna.story.resources.prop.Dresser"
```

The quoted `Dresser"` check targets the exact generic intermediate XML class
name while allowing the final `DresserResource` class name to remain present.

### Already-at-threshold version

Input:

```text
org.lgna.story.resources.dresser.DresserCentralAsian
```

Call:

```java
String migrated = ProjectMigrationManager.getInstance()
    .migrate(source, new Version("3.1.20.0.0"));
```

Expected output:

```text
org.lgna.story.resources.dresser.DresserCentralAsian
```

This preserves the current version-gated behavior: the `3.1.20.0.0` package move
is not applied to a project that already claims version `3.1.20.0.0`.

### BonePile `3.2.111.0.0` boundary

Input:

```text
name="BONE_PILE">
<declaringClass name="org.lgna.story.resources.prop.BonesResource"
```

Call before the boundary:

```java
String migrated = ProjectMigrationManager.getInstance()
    .migrate(source, new Version("3.2.110.0.0"));
```

Expected output:

```text
name="DEFAULT"> <declaringClass name="org.lgna.story.resources.prop.BonePileResource"
```

Call at the boundary:

```java
String migrated = ProjectMigrationManager.getInstance()
    .migrate(source, new Version("3.2.111.0.0"));
```

Expected output is the original source text for this selected rewrite. This
documents only the `BONE_PILE`/`BonesResource` rewrite boundary, not every
migration that may exist before or after `3.2.111.0.0`.
