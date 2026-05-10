# Tutorial: Add a ProjectMigrationManager Migration Characterization

This tutorial walks through adding a generated XML-string characterization for a
`ProjectMigrationManager` text migration seam.

For background on why this layer exists, see the
[Migration Hotspot Characterization concepts](../concepts/migration-hotspot-characterization.md).
For the full contract and API reference, see the
[reference](../reference/project-migration-manager-characterization.md).
For a task-oriented checklist, use the
[how-to](../howto/characterize-project-migration-manager.md).

## Contents

- [Goal](#goal)
- [1. Choose a narrow seam](#1-choose-a-narrow-seam)
- [2. Build a generated XML-string fixture](#2-build-a-generated-xml-string-fixture)
- [3. Migrate from the historical version](#3-migrate-from-the-historical-version)
- [4. Assert the final and absent names](#4-assert-the-final-and-absent-names)
- [5. Guard the current-version boundary](#5-guard-the-current-version-boundary)
- [6. Run validation](#6-run-validation)

## Goal

Build a small characterization layer around this current Alice behavior:

```text
ProjectMigrationManager applies version-gated text migrations in order, so a
legacy project XML reference can move through an intermediate class name and end
at the current resource class name in one migration pass.
```

The layer starts with table-level invariants, version-gated applicability,
concrete rewrite seams, and a current-version guard. Each concrete seam uses
generated strings that look like Alice project XML. It does not commit a binary
project archive, bundled media, or Git LFS fixture. Production code stays
unchanged; the tests document existing protected migration hotspots through
characterization coverage.

## 1. Choose a narrow seam

Start with one historical class or resource name that already has mappings in
`ProjectMigrationManager`.

For the dresser characterization, the seam is:

```text
org.lgna.story.resources.dresser.DresserCentralAsian
```

This seam is valuable because the old name does not migrate directly to the
current final name. It first moves to:

```text
org.lgna.story.resources.prop.DresserCentralAsian
```

Then later migrations consolidate it through:

```text
org.lgna.story.resources.prop.Dresser
```

and finally to:

```text
org.lgna.story.resources.prop.DresserResource
```

That cascade protects migration ordering without requiring a broad rewrite of
the migration table.

For a boundary-only characterization, choose one exact rewrite and result
version. The `3.2.111.0.0` BonePile boundary protects this existing rewrite:

| Source version | Input | Output |
| --- | --- | --- |
| `3.2.110.0.0` | `name="BONE_PILE"` with `org.lgna.story.resources.prop.BonesResource` | `name="DEFAULT"` with `org.lgna.story.resources.prop.BonePileResource` |
| `3.2.111.0.0` | Same source text | Unchanged for this selected rewrite |
| `3.2.112.0.0` | Same source text | Unchanged for this selected rewrite |

This boundary test verifies one selected rewrite is source-version gated. It
does not characterize every migration or claim that unrelated boundary-later
source text is unchanged.

## 2. Build a generated XML-string fixture

Add the test to:

```text
core/story-api-migration/src/test/java/org/lgna/project/migration/ProjectMigrationManagerTest.java
```

Before the concrete XML-string seam, add small table-level checks that establish
the migration contract:

| Check | Contract |
| --- | --- |
| Text migration versions | Each result version is valid, round-trippable, and strictly increasing. |
| AST migration versions | The same invariant holds for AST migrations. |
| Applicability threshold | A `3.1.20.0.0` text migration applies to `3.1.19.0.0`, but not to `3.1.20.0.0` or later. |
| Current version guard | The compiled current version has no pending text or AST migrations. |

Use only the serialized fragments needed for the behavior:

```java
String source = String.join("\n",
    "<type name=\"org.lgna.story.resources.dresser.DresserCentralAsian\"/>",
    "<declaringClass name=\"org.lgna.story.resources.dresser.DresserCentralAsian\"/>"
);
```

The two XML fragments cover the common serialized contexts that carry class
names in project text:

| Fragment | Why it matters |
| --- | --- |
| `type` | Protects serialized type references. |
| `declaringClass` | Protects method/member ownership references. |

Keep the fixture as text in the test. Do not add a `.a3p` archive when a string
fixture fully exercises the migration behavior.

Add a representative mixed-name rewrite test when the seam needs broader
coverage than the exact XML fragment. Keep that test generated and deterministic
too; it should prove that known legacy story/resource names still rewrite without
becoming a broad archive corpus.

For the BonePile boundary, the complete fixture is just the selected field and
declaring class text:

```java
String source = "name=\"BONE_PILE\">\n<declaringClass name=\"org.lgna.story.resources.prop.BonesResource\"";
```

For a selected accessor or resource-field migration, keep the fixture just as
small:

```java
String source = String.join("\n",
    "name=\"LEFT_THUMB_1\">",
    "<declaringClass name=\"org.lgna.story.resources.biped.Alien\"",
    "name=\"getRightClavicle\">",
    "<declaringClass name=\"org.lgna.story.SFlyer\""
);
```

When a resource-field test protects one migration table entry, it can select that
`TextMigration` directly and call `migrate(source)`. That style documents the
entry's replacement behavior, not the full `ProjectMigrationManager` pipeline.

## 3. Migrate from the historical version

Start from the version before every required mapping:

```java
String migrated = migrateWithoutTestLogNoise(source, "3.1.19.0.0");
```

`3.1.19.0.0` is before the package move to
`org.lgna.story.resources.prop.DresserCentralAsian`, so the package move and the
later resource consolidations both apply.

For the BonePile boundary, start before the selected migration result version:

```java
assertEquals(expected, migrateWithoutTestLogNoise(source, "3.2.110.0.0"));
assertEquals(source, migrateWithoutTestLogNoise(source, "3.2.111.0.0"));
assertEquals(source, migrateWithoutTestLogNoise(source, "3.2.112.0.0"));
```

The first assertion verifies the `BONE_PILE`/`BonesResource` rewrite. The second
and third assertions verify that this selected `3.2.111.0.0` rewrite is not
back-applied when the source project is at the boundary or later.

The helper is private to `ProjectMigrationManagerTest`; it is not production
API. It suppresses migration log noise while still exercising the production
manager:

```java
private String migrateWithoutTestLogNoise(String source, String versionText) {
  PrintStream previousOut = System.out;
  PrintStream mutedOut = new PrintStream(OutputStream.nullOutputStream());
  try {
    System.setOut(mutedOut);
    return manager.migrate(source, new Version(versionText));
  } finally {
    System.setOut(previousOut);
    mutedOut.close();
  }
}
```

## 4. Assert the final and absent names

Assert the exact final output:

```java
assertEquals(String.join("\n",
    "<type name=\"org.lgna.story.resources.prop.DresserResource\"/>",
    "<declaringClass name=\"org.lgna.story.resources.prop.DresserResource\"/>"
), migrated);
```

Then assert the obsolete and intermediate names are gone:

```java
assertFalse(migrated.contains("org.lgna.story.resources.dresser.DresserCentralAsian"));
assertFalse(migrated.contains("org.lgna.story.resources.prop.DresserCentralAsian"));
assertFalse(migrated.contains("org.lgna.story.resources.prop.Dresser\""));
```

The absence checks make the test fail if a refactor skips a migration, reorders
the migration table, or stops at an intermediate resource class.
The quoted `Dresser"` check catches the generic intermediate XML class name
without matching the final `DresserResource` value.

The complete test method is:

```java
@Test
public void textMigrationCascadesLegacyDresserThroughIntermediateResourceNames() {
  String source = String.join("\n",
      "<type name=\"org.lgna.story.resources.dresser.DresserCentralAsian\"/>",
      "<declaringClass name=\"org.lgna.story.resources.dresser.DresserCentralAsian\"/>"
  );

  String migrated = migrateWithoutTestLogNoise(source, "3.1.19.0.0");

  assertEquals(String.join("\n",
      "<type name=\"org.lgna.story.resources.prop.DresserResource\"/>",
      "<declaringClass name=\"org.lgna.story.resources.prop.DresserResource\"/>"
  ), migrated);
  assertFalse(migrated.contains("org.lgna.story.resources.dresser.DresserCentralAsian"));
  assertFalse(migrated.contains("org.lgna.story.resources.prop.DresserCentralAsian"));
  assertFalse(migrated.contains("org.lgna.story.resources.prop.Dresser\""));
}
```

## 5. Guard the current-version boundary

Add a small guard for the manager's compiled current version:

```java
@Test
public void managerReportsNoPendingMigrationsAtCurrentVersion() {
  Version currentVersion = manager.getCurrentVersion();

  assertFalse(manager.hasTextMigrationsFor(currentVersion));
  assertFalse(manager.hasAstMigrationsFor(currentVersion));
}
```

This guard belongs next to the migration characterization tests because it
protects the table/version relationship that migration refactors can accidentally
break.

## 6. Run validation

Initialize the grammar submodule from a fresh checkout or worktree:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the focused migration test after characterization changes and before relying
on production refactors:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.migration.ProjectMigrationManagerTest \
  test
```

Do not replace this command with CI no-op mode, no-op justification, or a
branch-policy shortcut when the change touches migration characterization.

Run the story API migration module tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

Run repository-level Python contracts separately when you need whole-repository
policy coverage. This suite is not migration-characterization evidence because
it also covers branch, PR, and documentation contracts outside
`ProjectMigrationManager`:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests
```

### Validate the gadugi QA scenario

The `migration-hotspot-characterization-smoke` gadugi scenario runs the same
focused Maven command through the repository's 4-layer argv allowlist. After
characterization changes, confirm it still validates:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

```bash
NODE_OPTIONS=--max-old-space-size=32768 bash \
qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

The gadugi scenario targets the test class name, not individual test methods, so
adding new `@Test` methods does not require scenario changes.

When adding another migration characterization, keep the fixture small, generated
in Java source, and tied to one observable migration behavior.
