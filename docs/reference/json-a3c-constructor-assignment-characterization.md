# JSON `.a3c` Constructor Assignment Characterization

This page defines one narrow generated-archive characterization: a JSON `.a3c`
type archive whose Tweedle source declares an integer field and assigns that
field inside the type constructor.

The feature is a compatibility boundary for `IoUtilities.readType(File)`. It is
not a broad `.a3c` migration claim, a general constructor decoder, or a promise
of full Tweedle statement/expression support.

## Test location

Add the characterization to:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

The test must generate the archive at runtime under the JUnit temporary folder.
Do not commit binary `.a3c` fixtures, use Git LFS, read user projects, or depend
on historical Alice payloads.

## Archive contract

Use a resource-free JSON type archive helper for this fixture. The test should
assert the complete archive entry set exactly:

```text
version.txt
manifest.json
src/GeneratedJsonTypeWithConstructorAssignmentBoundary.twe
```

The archive must not contain `resources.xml`, `resources/<name>`, `type.xml`, or
any other fixture payload. If future code adds resources to this scenario, that
is a different characterization and needs different documentation.

## Manifest contract

The manifest identifies the archive as a JSON Alice type archive through
metadata:

```text
metadata.fileType = a3c
metadata.identifier.name = GeneratedJsonTypeWithConstructorAssignmentBoundary
metadata.identifier.type = Library
description.name = GeneratedJsonTypeWithConstructorAssignmentBoundary
```

The manifest contains one Tweedle `TypeReference`:

```text
name = GeneratedJsonTypeWithConstructorAssignmentBoundary
file = src/GeneratedJsonTypeWithConstructorAssignmentBoundary.twe
format = tweedle
```

Keep `metadata.fileType` separate from the `TypeReference`; `fileType` is
manifest metadata, not a field on the type reference.

## Tweedle fixture

The source uses Alice Tweedle assignment syntax:

```text
class GeneratedJsonTypeWithConstructorAssignmentBoundary extends SProgram {
  WholeNumber count;

  GeneratedJsonTypeWithConstructorAssignmentBoundary() {
    this.count <- 1;
  }
}
```

This is only the constructor-body field-assignment boundary. It does not cover
multiple assignments, local variables, method bodies, inherited fields, compound
assignment, arithmetic expressions, object construction, or resource expressions.

## Required API behavior

Read the generated archive through the public production API:

```java
TypeResourcesPair pair = IoUtilities.readType(typeArchive);
```

The expected decoded shape is:

| Observable | Required behavior |
| --- | --- |
| Type name | `GeneratedJsonTypeWithConstructorAssignmentBoundary` |
| Supertype | `SProgram` |
| Declared field | one `UserField` named `count` with value type `JavaType.getInstance(Integer.class)`, decoded from Tweedle `WholeNumber` |
| Constructors | one `NamedUserConstructor` |
| Constructor body | one statement |
| Body statement | `ExpressionStatement` |
| Wrapped expression | `AssignmentExpression` |
| Assignment left-hand side | `FieldAccess` resolving to the decoded `count` field |
| Assignment right-hand side | `IntegerLiteral(1)` |

The constructor body does not directly contain an `AssignmentExpression`; it
contains an `ExpressionStatement` whose expression is the `AssignmentExpression`.
Tests should assert that wrapper shape so future decoder changes cannot pass by
checking only a loose statement count.

## Configuration and validation

The characterization adds no runtime configuration.

Focused validation runs from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

## Non-goals

This feature does not prove full `.a3c`, `.a3p`, `.a3w`, Tweedle, player, or
project migration support. It covers exactly one generated JSON `.a3c` archive
with one `WholeNumber` field and one constructor assignment statement.
