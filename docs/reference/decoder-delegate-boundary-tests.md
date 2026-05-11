# Decoder Delegate and IoUtilities Boundary Tests

Focused unit tests for the silver thread modules `core/ast` and
`core/story-api-migration`. These tests target untested public methods and
boundary conditions in the decoder delegate classes (`ExpressionDecoder`,
`StatementDecoder`, `FieldDecoder`) and `IoUtilities` archive handling.

Each test file is under 500 lines. All tests use JUnit 4 and follow the
existing characterization test patterns established in
`DecoderDelegateDecompositionCharacterizationTest`.

RabbitHole issue: #493.

## Contents

- [Overview](#overview)
- [Test files](#test-files)
- [FieldDecoderBoundaryTest](#fielddecoderboundarytest)
- [ExpressionDecoderBoundaryTest](#expressiondecoderboundarytest)
- [StatementDecoderBoundaryTest](#statementdecoderboundarytest)
- [IoUtilitiesBoundaryTest](#ioutilitiesboundarytest)
- [Running the tests](#running-the-tests)
- [Test approach](#test-approach)
- [Coverage scope](#coverage-scope)
- [Error handling contract](#error-handling-contract)
- [Claim boundaries](#claim-boundaries)

## Overview

The decoder delegates (`ExpressionDecoder`, `StatementDecoder`,
`FieldDecoder`) are package-private classes instantiated by `Decoder`. They
cannot be constructed directly in tests. All boundary tests exercise the
delegates through the public facade `TweedleEncoderDecoder.decode(String)`,
passing synthetic Tweedle class source strings that trigger specific delegate
code paths.

The `IoUtilities` boundary tests target the `listProjectFiles`,
`listTypeFiles`, and error-path methods that had zero prior test coverage.
These use JUnit `TemporaryFolder` for deterministic file system isolation.

## Test files

| File | Module | Lines | Target |
| --- | --- | --- | --- |
| `FieldDecoderBoundaryTest.java` | `core/ast` | ~300 | Null init by type, array initializers (sized/element), resource field errors, arithmetic operators |
| `ExpressionDecoderBoundaryTest.java` | `core/ast` | ~280 | All 6 relational operators, logical ops, string concatenation, division ambiguity, unknown identifier error, return expressions |
| `StatementDecoderBoundaryTest.java` | `core/ast` | ~320 | Empty void/non-void methods, local/field assignments, while-in-non-void error, if/else bodies, constructor validation |
| `IoUtilitiesBoundaryTest.java` | `core/story-api-migration` | ~250 | File listing, read errors, extension constants |

All test files live in the same package as their production code to match the
existing test layout.

## FieldDecoderBoundaryTest

**Location:**
`core/ast/src/test/java/org/alice/serialization/tweedle/FieldDecoderBoundaryTest.java`

**Tested boundaries:**

### Null field initializer by type

Fields initialized to `null` are supported for known type aliases
(`WholeNumber`, `DecimalNumber`, `Boolean`, `TextString`), `NamedUserType`
fields, array fields, and resource-typed fields. `FieldDecoder` throws
`UnsupportedTweedleDecodeException` for null initializers on types that are
not in the supported nullable set.

```java
// Tweedle source exercised:
class MyClass extends SScene {
  TextString name <- null;
}
```

Tests verify that each supported nullable category produces a `NullLiteral`
AST node and that unsupported null initializers throw
`UnsupportedTweedleDecodeException`.

### Array initializers — sized vs element

Two array initializer forms are supported:

1. **Sized**: `WholeNumber[] counts <- new WholeNumber[3]` — produces
   `ArrayInstanceCreation` with the declared size.
2. **Element**: `WholeNumber[] counts <- {1, 2, 3}` — produces
   `ArrayInstanceCreation` with decoded element expressions.

Tests verify both forms produce correct AST nodes. Edge cases include
zero-length sized arrays (`new WholeNumber[0]`) and single-element
initializers.

### Resource field initializer boundary

Non-null resource field initializers throw
`UnsupportedTweedleDecodeException` because no archive resource manifest or
binding context is available during standalone Tweedle decode. Null resource
field initializers are accepted.

```java
// This throws UnsupportedTweedleDecodeException:
class MyClass extends SScene {
  ImageResource img <- someValue;
}
```

### Literal arithmetic field initializers

When `Decoder.isLiteralArithmeticAllowed()` returns true, fields can be
initialized with literal-only arithmetic expressions:

```java
class MyClass extends SScene {
  DecimalNumber ratio <- 1.0 / 3.0;
  WholeNumber sum <- 2 + 3;
}
```

Tests verify addition, subtraction, multiplication, and division for both
`WholeNumber` and `DecimalNumber` types. Non-literal operands (e.g.,
identifiers) throw `UnsupportedTweedleDecodeException`.

## ExpressionDecoderBoundaryTest

**Location:**
`core/ast/src/test/java/org/alice/serialization/tweedle/ExpressionDecoderBoundaryTest.java`

**Tested boundaries:**

### All 6 relational operators

Tests verify that each comparison operator maps to the correct
`RelationalInfixExpression.Operator`:

| Tweedle | AST operator |
| --- | --- |
| `==` | `EQUALS` |
| `!=` | `NOT_EQUALS` |
| `<` | `LESS` |
| `<=` | `LESS_EQUALS` |
| `>` | `GREATER` |
| `>=` | `GREATER_EQUALS` |

Each operator is exercised through a method that returns a `Boolean` from
comparing two `WholeNumber` parameters.

### Logical operators

- `&&` maps to `ConditionalInfixExpression.Operator.AND`
- `||` maps to `ConditionalInfixExpression.Operator.OR`
- `!` maps to `LogicalComplement`

Tests use Boolean-typed parameters to build compound expressions.

### String concatenation

The `..` operator produces a `StringConcatenation` AST node. Tests verify
concatenation of two `TextString` parameters.

### Division operator ambiguity

Division has different behavior based on the result type:

- `WholeNumber` division → `INTEGER_DIVIDE`
- `DecimalNumber` division → `REAL_DIVIDE`
- Ambiguous/missing type → `UnsupportedTweedleDecodeException`

Tests verify all three outcomes.

### Unknown identifier error

An `IdentifierReference` that does not match any known local, parameter, or
field throws `UnsupportedTweedleDecodeException` with a message containing
the owner name and unknown identifier name.

### Return expression coverage

Methods that return values are tested for:

- Returning a primitive literal
- Returning a parameter identifier
- Returning a local variable identifier
- Returning a bare field identifier (resolves via `IdentifierReference` → `findField`)
- Returning a field via `this.fieldName` (resolves via `FieldAccess` path)
- Returning a string concatenation expression
- Returning a comparison expression
- Returning a logical expression (`&&`, `||`, `!`)
- Type mismatch on return → `UnsupportedTweedleDecodeException`
- Unknown return identifier → `UnsupportedTweedleDecodeException`

## StatementDecoderBoundaryTest

**Location:**
`core/ast/src/test/java/org/alice/serialization/tweedle/StatementDecoderBoundaryTest.java`

**Tested boundaries:**

### Empty method bodies

- **Void methods**: An empty body produces an empty `BlockStatement`. This is
  the normal case for void methods with no side effects.
- **Non-void methods**: An empty body on a non-void method throws
  `UnsupportedTweedleDecodeException` because a return statement is required.

```java
// Valid — empty void method:
class MyClass extends SScene {
  void doNothing() {}
}

// Invalid — empty non-void method:
class MyClass extends SScene {
  WholeNumber getValue() {}
}
```

### Local variable declarations

Local variables with initializers are decoded to
`LocalDeclarationStatement`. Tests verify:

- Correct type resolution for `WholeNumber`, `DecimalNumber`, `Boolean`,
  `TextString` locals
- Initializer expression is decoded and type-checked
- The local is available for use in subsequent statements

### Field and local assignment statements

Assignment targets can be bare identifiers (resolving to locals or fields)
or `this.field` member expressions. Tests verify:

- Local assignment via bare identifier
- Field assignment via bare identifier
- Field assignment via `this.fieldName`
- Unknown assignment target → `UnsupportedTweedleDecodeException`
- Type mismatch on assignment → `UnsupportedTweedleDecodeException`

### While loop in non-void method

While loops are only supported in void methods. A while loop in a non-void
method throws `UnsupportedTweedleDecodeException`:

```java
// Invalid — while loop in non-void method:
class MyClass extends SScene {
  WholeNumber compute() {
    while (true) { }
    return 0;
  }
}
```

### If/else bodies

- **Simple if (no else)**: Then-body may contain assignments and
  zero-argument method calls.
- **If/else**: Both branches may contain only assignment statements.
- Non-boolean condition → `UnsupportedTweedleDecodeException`

### Constructor body validation

Constructor bodies support:

- Local variable declarations (decoded to `LocalDeclarationStatement`)
- Assignment statements (local via bare identifier, field via bare
  identifier, and field via `this.fieldName`)
- Zero-argument same-class method calls
- Unsupported statement types → `UnsupportedTweedleDecodeException`

Both empty and non-empty constructors produce a
`ConstructorBlockStatement` that contains a
`SuperConstructorInvocationStatement`. The zero-argument
`ConstructorBlockStatement()` constructor sets a default
`SuperConstructorInvocationStatement` with no body statements. A
non-empty constructor passes the decoded body statements alongside
an explicit `SuperConstructorInvocationStatement`.

## IoUtilitiesBoundaryTest

**Location:**
`core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesBoundaryTest.java`

**Tested boundaries:**

### Extension constants

Verifies the three file extension constants:

```java
assertEquals("a3w", IoUtilities.EXPORT_EXTENSION);
assertEquals("a3p", IoUtilities.PROJECT_EXTENSION);
assertEquals("a3c", IoUtilities.TYPE_EXTENSION);
```

### listProjectFiles — empty directory

`IoUtilities.listProjectFiles(directory)` delegates to
`FileUtilities.listFiles(directory, "a3p")`. When the directory contains no
`.a3p` files, the result is an empty array (`File[0]`).

```java
@Rule
public TemporaryFolder tempDir = new TemporaryFolder();

@Test
public void listProjectFiles_emptyDirectory_returnsEmptyArray() {
  File[] files = IoUtilities.listProjectFiles(tempDir.getRoot());
  assertNotNull(files);
  assertEquals(0, files.length);
}
```

### listProjectFiles — matching and non-matching files

When the directory contains both `.a3p` and non-`.a3p` files, only `.a3p`
files are returned.

### listTypeFiles — empty directory

Same pattern as `listProjectFiles` but filters for `.a3c` extension.

### listTypeFiles — matching and non-matching files

Only `.a3c` files are returned; `.a3p` and other files are excluded.

### readProject — nonexistent file

`IoUtilities.readProject(File)` throws `IOException` when the file does not
exist, because the underlying `new ZipFile(file)` fails.

### readProject — non-zip file

`IoUtilities.readProject(File)` throws `IOException` when the file exists
but is not a valid zip archive.

### readProject — string path overload

`IoUtilities.readProject(String)` delegates to `readProject(new File(path))`
and produces the same `IOException` for nonexistent paths.

## Running the tests

### All boundary tests (core/ast)

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='*BoundaryTest' \
  test -q
```

### All boundary tests (core/story-api-migration)

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='*BoundaryTest' \
  test -q
```

### Individual test files

```bash
# FieldDecoder boundaries
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=FieldDecoderBoundaryTest test -q

# ExpressionDecoder boundaries
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ExpressionDecoderBoundaryTest test -q

# StatementDecoder boundaries
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=StatementDecoderBoundaryTest test -q

# IoUtilities boundaries
mvn -pl core/story-api-migration -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=IoUtilitiesBoundaryTest test -q
```

### Regression check — existing tests

After running boundary tests, verify no regressions in existing
characterization tests:

```bash
# Decoder delegate decomposition characterization (57 tests)
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=DecoderDelegateDecompositionCharacterizationTest test -q

# IoUtilities characterization tests
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false test -q
```

## Test approach

All decoder boundary tests follow a consistent pattern:

1. **Build synthetic Tweedle source** — a minimal `class MyClass extends
   SScene { ... }` string containing only the construct under test.
2. **Decode through the public facade** — call
   `TweedleEncoderDecoder.decode(source)` which parses the Tweedle, then
   delegates to `Decoder` → `FieldDecoder` / `ExpressionDecoder` /
   `StatementDecoder`.
3. **Assert on the resulting AST** — inspect the `NamedUserType` returned by
   `decode()` for expected fields, methods, constructor bodies, expression
   types, and operator mappings.
4. **Assert on error paths** — use `assertThrows` to verify that invalid
   Tweedle source produces the expected `UnsupportedTweedleDecodeException`
   with a message containing diagnostic details.

This approach matches the pattern established in
`DecoderDelegateDecompositionCharacterizationTest` and exercises production
decode paths without mocking internal collaborators.

IoUtilities tests use `TemporaryFolder` for file system isolation. Test
files are created with known names and content, then verified through the
public `listProjectFiles`, `listTypeFiles`, and `readProject` methods.

## Coverage scope

| Delegate | Method/path | Prior coverage | Boundary test |
| --- | --- | --- | --- |
| `FieldDecoder` | `decodeNullFieldInitializer` (per type) | Partial (1 type) | All nullable categories |
| `FieldDecoder` | `decodeSizedArrayFieldInitializer` | None | Sized array with literal int |
| `FieldDecoder` | `decodeArrayFieldInitializer` (elements) | None | Element array with literals |
| `FieldDecoder` | `unsupportedResourceFieldInitializer` | None | Non-null resource field |
| `FieldDecoder` | `decodeLiteralArithmeticFieldInitializer` | Partial (add only) | All 4 operators |
| `ExpressionDecoder` | `relationalOperator` (all 6) | Partial (2 of 6) | All 6 operators |
| `ExpressionDecoder` | `decodeLogicalInfixExpression` | None | AND, OR |
| `ExpressionDecoder` | `decodeLogicalNotExpression` | None | NOT |
| `ExpressionDecoder` | `decodeStringConcatenationExpression` | None | Two-operand concat |
| `ExpressionDecoder` | `arithmeticOperator` (division ambiguity) | None | WholeNumber / DecimalNumber / ambiguous |
| `ExpressionDecoder` | `decodeValueExpression` (unknown identifier) | None | IdentifierReference not in locals, params, or fields |
| `ExpressionDecoder` | `decodeMethodReturnExpression` (all forms) | Partial (literal only) | Identifier, bare-field, this.field, concat, comparison, logical returns |
| `StatementDecoder` | `decodeMethodBody` (empty void) | None | Empty void method |
| `StatementDecoder` | `decodeMethodBody` (empty non-void) | None | Error on empty non-void |
| `StatementDecoder` | `decodeMethodAssignmentStatement` (local) | None | Local assignment |
| `StatementDecoder` | `decodeMethodAssignmentStatement` (field) | Partial | Field + this.field |
| `StatementDecoder` | `decodeWhileLoop` (non-void error) | None | While in non-void error |
| `StatementDecoder` | `decodeIfStatement` (else branch) | Partial (then only) | Both if and if/else |
| `StatementDecoder` | `decodeConstructorBody` (empty) | None | Empty constructor (default SuperConstructorInvocationStatement, no body) |
| `StatementDecoder` | `decodeConstructorBody` (non-empty) | None | Local declarations, assignments (bare + this.field), method calls |
| `IoUtilities` | `listProjectFiles` | None | Empty, matching, non-matching |
| `IoUtilities` | `listTypeFiles` | None | Empty, matching, non-matching |
| `IoUtilities` | `readProject(File)` error paths | None | Nonexistent, non-zip |
| `IoUtilities` | `readProject(String)` | None | String path delegation |
| `IoUtilities` | Extension constants | None | All 3 constants |

## Error handling contract

All decoder boundary tests verify that production code throws the expected
exception type with a diagnostic message:

- **`UnsupportedTweedleDecodeException`** — thrown by decoder delegates for
  unsupported Tweedle constructs. The message always includes the owner name
  (method, constructor, or field name) for debuggability.
- **`IOException`** — thrown by `IoUtilities` for file system and archive
  errors.
- **`VersionNotSupportedException`** — declared on `readProject` and
  `readType` signatures but not directly tested here (covered by existing
  `HistoricalArchiveRoundTripCharacterizationTest`).

Error path tests use `assertThrows(ExceptionType.class, () -> ...)` and
optionally verify the exception message contains expected diagnostic
substrings.

## Claim boundaries

These boundary tests explicitly do **not** claim:

- Full Tweedle language coverage. Only constructs reachable through the
  current decoder delegates are tested.
- Resource binding or archive manifest decode. Resource field initializers
  are tested only for the null-init and error boundaries.
- Broad `IoUtilities` write path coverage. Write operations are covered by
  existing `IoUtilitiesTest` round-trip tests.
- Type resolution beyond known type aliases and `JavaType` primitives.
- While loop body content beyond assignments. The existing decoder only
  supports assignment-only while bodies.
- Method call decoding beyond zero-argument same-class calls.

The tests are boundary tests, not characterization tests. They target
specific untested code paths rather than preserving broad decode behavior.
For characterization coverage, see
[decode-coverage-characterization.md](decode-coverage-characterization.md)
and the `DecoderDelegateDecompositionCharacterizationTest`.
