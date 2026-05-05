# Tutorial: Add a ProjectMigrationManager Migration Characterization

This tutorial walks through adding a generated XML-string characterization for a
`ProjectMigrationManager` text migration seam.

## Contents

- [Goal](#goal)
- [1. Choose a narrow seam](#1-choose-a-narrow-seam)
- [2. Build a generated XML-string fixture](#2-build-a-generated-xml-string-fixture)
- [3. Migrate from the historical version](#3-migrate-from-the-historical-version)
- [4. Assert the final and absent names](#4-assert-the-final-and-absent-names)
- [5. Run validation](#5-run-validation)

## Goal

Build a small characterization layer around this current Alice behavior:

```text
ProjectMigrationManager applies version-gated text migrations in order, so a
legacy project XML reference can move through an intermediate class name and end
at the current resource class name in one migration pass.
```

The layer starts with table-level invariants, version-gated applicability, and
one concrete rewrite seam. The concrete seam uses generated strings that look
like Alice project XML. It does not commit a binary project archive, bundled
media, or Git LFS fixture.

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

## 3. Migrate from the historical version

Start from the version before every required mapping:

```java
String migrated = migrateWithoutTestLogNoise(source, "3.1.19.0.0");
```

`3.1.19.0.0` is before the package move to
`org.lgna.story.resources.prop.DresserCentralAsian`, so the package move and the
later resource consolidations both apply.

The helper suppresses migration log noise while still exercising the production
manager:

```java
private String migrateWithoutTestLogNoise(String source, String versionText) {
  PrintStream previousOut = System.out;
  try {
    System.setOut(new PrintStream(new ByteArrayOutputStream()));
    return manager.migrate(source, new Version(versionText));
  } finally {
    System.setOut(previousOut);
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

## 5. Run validation

Initialize the grammar submodule from a fresh checkout or worktree:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the focused migration test:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.migration.ProjectMigrationManagerTest \
  test
```

Run the story API migration module tests:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

Run the Python contract tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests
```

When adding another migration characterization, keep the fixture small, generated
in Java source, and tied to one observable migration behavior.
