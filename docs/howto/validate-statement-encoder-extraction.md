# Validate the StatementEncoder Extraction

How to verify that the `StatementEncoder` extraction from `TweedleEncoder` is
complete and correct. Use this after merging the extraction or when reviewing
the PR.

## Prerequisites

- Java 17+ and Maven installed
- Tweedle grammar submodule initialized:
  ```bash
  git submodule update --init tweedle-lang
  ```

## Step 1: Confirm the new file exists

```bash
ls core/ast/src/main/java/org/alice/serialization/tweedle/StatementEncoder.java
```

Expected: file listed with no errors.

## Step 2: Verify package-private visibility

```bash
grep '^class ' core/ast/src/main/java/org/alice/serialization/tweedle/StatementEncoder.java
```

Expected: `class StatementEncoder {` — no `public` modifier.

## Step 3: Verify constructor takes TweedleEncoder

```bash
grep 'StatementEncoder(TweedleEncoder' \
  core/ast/src/main/java/org/alice/serialization/tweedle/StatementEncoder.java
```

Expected: `StatementEncoder(TweedleEncoder encoder)` or similar.

## Step 4: Verify extracted methods exist on StatementEncoder

```bash
grep -E 'appendStatementCompletion|appendStatementEnd|pushStatementDisabled' \
  core/ast/src/main/java/org/alice/serialization/tweedle/StatementEncoder.java
```

Expected: at least four method signatures (two `appendStatementCompletion`
overloads, one `appendStatementEnd`, one `pushStatementDisabled`).

## Step 5: Verify NODE_ENABLE is package-private on TweedleEncoder

```bash
grep 'NODE_ENABLE' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: `static final String NODE_ENABLE = ">*";` — no `private` modifier.

## Step 6: Verify TweedleEncoder delegates to StatementEncoder

```bash
grep 'statementEncoder\.' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: delegation calls in `appendStatementCompletion`,
`pushStatementDisabled`, and `appendCodeFlowStatement`.

## Step 7: Verify TweedleEncoderDecoder is unchanged

```bash
git diff HEAD~1 -- core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoderDecoder.java
```

Expected: no changes. The public facade is unmodified by this extraction.
Replace `HEAD~1` with the appropriate merge base if your branch diverged.

## Step 8: Run core/ast encoder tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderTest,TweedleEncoderRenameContractTest,TweedleEncoderDecoderTest,SourceCodeGeneratorTest \
  test -q
```

Expected: all tests pass, exit code 0.

## Step 9: Run story-api-migration round-trip tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test -q
```

Expected: all tests pass, exit code 0.

## Step 10: Verify no appendStatementEnd calls remain directly in TweedleEncoder

```bash
grep 'private.*appendStatementEnd' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: **zero matches**. The `appendStatementEnd` method has moved to
`StatementEncoder`. `TweedleEncoder` calls it via
`statementEncoder.appendStatementEnd(stmt)`.

## Checklist

- [ ] `StatementEncoder.java` exists
- [ ] `StatementEncoder` is package-private (no `public` keyword)
- [ ] Constructor takes `TweedleEncoder` reference
- [ ] Four methods present: `appendStatementCompletion` (×2), `appendStatementEnd`, `pushStatementDisabled`
- [ ] `NODE_ENABLE` is package-private on `TweedleEncoder`
- [ ] `TweedleEncoder` delegates via `statementEncoder.` calls
- [ ] `TweedleEncoderDecoder.java` is unchanged
- [ ] `TweedleEncoderTest` passes
- [ ] `TweedleEncoderRenameContractTest` passes
- [ ] `TweedleEncoderDecoderTest` passes
- [ ] `SourceCodeGeneratorTest` passes
- [ ] `core/story-api-migration` tests pass
- [ ] No private `appendStatementEnd` on `TweedleEncoder`
