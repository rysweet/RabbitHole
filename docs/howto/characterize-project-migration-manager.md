# Characterize ProjectMigrationManager migrations

Use this guide when a change touches `ProjectMigrationManager`, migration
ordering, text migration helpers, or a protected migration hotspot.

## Contents

- [Prerequisites](#prerequisites)
- [1. Identify the migration behavior](#1-identify-the-migration-behavior)
- [2. Build the smallest generated fixture](#2-build-the-smallest-generated-fixture)
- [3. Migrate from the source version that proves the behavior](#3-migrate-from-the-source-version-that-proves-the-behavior)
- [4. Assert final output and rejected intermediates](#4-assert-final-output-and-rejected-intermediates)
- [5. Guard current-version completion](#5-guard-current-version-completion)
- [6. Validate the characterization](#6-validate-the-characterization)
- [Review checklist](#review-checklist)

## Prerequisites

Start from a branch based on current `develop` and initialize the grammar
submodule before Maven validation:

```sh
git fetch origin develop
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

The characterization suite is documented in the
[ProjectMigrationManager migration reference](../reference/project-migration-manager-characterization.md).
For a guided example, use the
[ProjectMigrationManager migration tutorial](../tutorials/project-migration-manager-characterization.md).
For background on why this layer exists, see the
[Migration Hotspot Characterization concepts](../concepts/migration-hotspot-characterization.md).

## 1. Identify the migration behavior

Choose one observable compatibility behavior that already exists in
`ProjectMigrationManager`. Keep table-level invariants separate from concrete
rewrite seams so the test name and assertion explain what behavior is protected.

Baseline invariants protect the migration table itself:

| Invariant | Good characterization |
| --- | --- |
| Result-version ordering | Assert that text and AST migration result versions are valid, round-trippable, and strictly increasing. |
| Applicability threshold | Assert that a migration applies only when the saved project version is older than its result version. |

Concrete migration seams protect selected compatibility rewrites:

| Seam | Good characterization |
| --- | --- |
| Legacy class rename | Start before the rename and assert the final migrated class name. |
| Multi-step resource consolidation | Assert the final resource name and the absence of old and intermediate names. |
| Boundary rewrite | Assert the rewrite before the boundary and unchanged selected text at or after the boundary. |
| Field or accessor rewrite | Assert the selected legacy field/accessor reaches the current name without broad archive fixtures. Use a direct `TextMigration` seam when the behavior under review is one table entry rather than full manager ordering. |

Avoid broad archive fixtures when a generated XML string exercises the behavior.
The characterization layer protects migration compatibility without adding
binary `.a3p`, `.a3w`, `.a3c`, media, or Git LFS payloads.

## 2. Build the smallest generated fixture

Put new migration characterization in:

```text
core/story-api-migration/src/test/java/org/lgna/project/migration/ProjectMigrationManagerTest.java
```

Use only the serialized fragments needed for the seam. For a class-name rewrite,
cover the XML contexts that carry the name:

```java
String source = String.join("\n",
    "<type name=\"org.lgna.story.resources.dresser.DresserCentralAsian\"/>",
    "<declaringClass name=\"org.lgna.story.resources.dresser.DresserCentralAsian\"/>"
);
```

For a boundary field rewrite, the fixture can stay narrower:

```java
String source = "name=\"BONE_PILE\">\n<declaringClass name=\"org.lgna.story.resources.prop.BonesResource\"";
```

For a selected field/accessor rewrite, keep only the field or method name and the
declaring class that makes the migration meaningful:

```java
String source = String.join("\n",
    "name=\"LEFT_THUMB_1\">",
    "<declaringClass name=\"org.lgna.story.resources.biped.Alien\"",
    "name=\"getRightClavicle\">",
    "<declaringClass name=\"org.lgna.story.SFlyer\""
);
```

Keep fixtures deterministic and generated in Java source. Do not copy Alice
sample projects or investigation artifacts into the test tree.

## 3. Migrate from the source version that proves the behavior

Use the saved project version that makes the intended migrations applicable.
For the legacy dresser cascade, start before the package move:

```java
String migrated = migrateWithoutTestLogNoise(source, "3.1.19.0.0");
```

`migrateWithoutTestLogNoise` is a private helper in
`ProjectMigrationManagerTest`, not production API. It only mutes test log output
while still calling the production `ProjectMigrationManager` migration path.

For the `3.2.111.0.0` BonePile boundary, assert both sides of the gate:

```java
assertEquals(expected, migrateWithoutTestLogNoise(source, "3.2.110.0.0"));
assertEquals(source, migrateWithoutTestLogNoise(source, "3.2.111.0.0"));
assertEquals(source, migrateWithoutTestLogNoise(source, "3.2.112.0.0"));
```

This proves that the selected rewrite applies before the boundary and is not
back-applied to source text already at or after the boundary.

## 4. Assert final output and rejected intermediates

Assert the exact migrated text when the seam has a single expected shape:

```java
assertEquals(String.join("\n",
    "<type name=\"org.lgna.story.resources.prop.DresserResource\"/>",
    "<declaringClass name=\"org.lgna.story.resources.prop.DresserResource\"/>"
), migrated);
```

Then assert that obsolete and intermediate names are absent:

```java
assertFalse(migrated.contains("org.lgna.story.resources.dresser.DresserCentralAsian"));
assertFalse(migrated.contains("org.lgna.story.resources.prop.DresserCentralAsian"));
assertFalse(migrated.contains("org.lgna.story.resources.prop.Dresser\""));
```

Absence checks are part of the contract. They catch refactors that apply only the
first migration, stop at an intermediate resource class, or run the table out of
order.

## 5. Guard current-version completion

Keep a small guard that proves the compiled current version has no remaining
text or AST migrations:

```java
Version currentVersion = manager.getCurrentVersion();

assertFalse(manager.hasTextMigrationsFor(currentVersion));
assertFalse(manager.hasAstMigrationsFor(currentVersion));
```

This guard catches accidental version metadata regressions and migration-table
entries whose result version moves beyond the current project version.

## 6. Validate the characterization

Run the focused migration characterization after characterization changes and
before relying on production refactors:

```sh
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.migration.ProjectMigrationManagerTest \
  test
```

Do not use CI no-op mode, no-op justification, or branch-policy shortcuts as a
replacement for this focused validation.

Run the full story API migration module when the migration change is not purely
test or documentation work:

```sh
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

## Review checklist

Before merging a migration characterization or refactor, confirm:

| Question | Required answer |
| --- | --- |
| Is the fixture generated in source and LFS-free? | Yes. |
| Does the test start from the saved project version that proves the behavior? | Yes. |
| Are result-version and applicability rules still protected? | Yes. |
| Are expected final names asserted? | Yes. |
| Are obsolete or intermediate names rejected where the seam has intermediates? | Yes. |
| Does production behavior remain compatible with the current Alice baseline? | Yes. |
