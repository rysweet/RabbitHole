# Validate the Decoder Delegate Decomposition

This guide describes how to verify that the Decoder decomposition into
`ExpressionDecoder`, `StatementDecoder`, and `FieldDecoder` preserves all
existing behavior.

## Prerequisites

From a fresh checkout or worktree, initialize the Tweedle grammar submodule:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Set the Node memory limit for the Maven reactor:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Run the core AST decoder tests

These tests exercise every supported decode path through the public
`TweedleEncoderDecoder` facade:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderDecoderTest \
  test
```

All assertions must pass. This suite covers expressions, statements, fields,
constructors, control flow, return values, error messages, and unsupported
construct boundaries.

## Step 2: Run the story-api-migration tests

These tests exercise the decoder through real archive round-trips:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

All assertions must pass. The `SilverThreadTweedleDecoderRoundTripTest` in
`core/story-api-migration` exercises decode with real project terminals.

## Step 3: Run the silver-thread round-trip test

This test loads a real `.a3p` starter project and round-trips every
`NamedUserType` through encode → decode:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.SilverThreadTweedleDecoderRoundTripTest \
  test
```

Structural equality assertions for class names, method counts, method names,
field counts, and constructor presence must all pass.

## Step 4: Verify line counts

After the decomposition, confirm the coordinator is within the target:

```bash
wc -l core/ast/src/main/java/org/alice/serialization/tweedle/Decoder.java
```

Expected: ≤ 500 lines.

Confirm all delegates exist:

```bash
ls -la core/ast/src/main/java/org/alice/serialization/tweedle/{ExpressionDecoder,StatementDecoder,FieldDecoder}.java
```

## Step 5: Verify no public API changes

`TweedleEncoderDecoder.java` must be unchanged:

```bash
git diff HEAD -- core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoderDecoder.java
```

Expected: no output (no changes).

## Step 6: Verify delegate visibility

All three delegate classes must be package-private (no `public` keyword):

```bash
head -20 core/ast/src/main/java/org/alice/serialization/tweedle/ExpressionDecoder.java
head -20 core/ast/src/main/java/org/alice/serialization/tweedle/StatementDecoder.java
head -20 core/ast/src/main/java/org/alice/serialization/tweedle/FieldDecoder.java
```

Expected: `class ExpressionDecoder {`, `class StatementDecoder {`,
`class FieldDecoder {` — no `public` modifier.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `require-tweedle-lang-submodule` enforcer failure | Grammar submodule not initialized. | Run `git submodule update --init tweedle-lang`. |
| Compile error: method not visible | A `private` method was not widened to package-private. | Change the method from `private` to package-private in `Decoder.java`. |
| Test assertion failure after extraction | Method moved to wrong delegate or error message changed. | Compare the failing assertion against the pre-extraction test output. Error messages must be character-for-character identical. |
| `OutOfMemoryError` during Maven build | Insufficient heap. | Set `NODE_OPTIONS=--max-old-space-size=32768`. |
| `ClassCastException` on decode result | Delegate returns wrong type. | Verify the delegate method signature matches the original `Decoder` method exactly. |

## What this does not verify

This guide validates behavioral preservation. It does not verify:

- New decode capabilities (none are added by this decomposition).
- Performance characteristics (decomposition is structural).
- Encoder behavior (`Encoder.java` is not modified).
- Full Tweedle language support (known gaps remain unchanged).

See [Decoder Delegate Decomposition](../reference/decoder-delegate-decomposition.md)
for the full reference.
