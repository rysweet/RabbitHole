# Validate the ExpressionEncoder Extraction

How to verify that the `ExpressionEncoder` extraction from `TweedleEncoder` is
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
ls core/ast/src/main/java/org/alice/serialization/tweedle/ExpressionEncoder.java
```

Expected: file listed with no errors.

## Step 2: Verify package-private visibility

```bash
grep '^class ' core/ast/src/main/java/org/alice/serialization/tweedle/ExpressionEncoder.java
```

Expected: `class ExpressionEncoder {` — no `public` modifier.

## Step 3: Verify constructor takes TweedleEncoder

```bash
grep 'ExpressionEncoder(TweedleEncoder' \
  core/ast/src/main/java/org/alice/serialization/tweedle/ExpressionEncoder.java
```

Expected: `ExpressionEncoder(TweedleEncoder encoder)` or similar.

## Step 4: Verify extracted methods exist on ExpressionEncoder

```bash
grep -E 'appendTargetAndMember|targetIsMath|tweedleModuleForMath|processResourceExpression' \
  core/ast/src/main/java/org/alice/serialization/tweedle/ExpressionEncoder.java
```

Expected: at least four method signatures — one `appendTargetAndMember`, one
`targetIsMath`, one `tweedleModuleForMath`, one `processResourceExpression`.

## Step 5: Verify angleMembers and membersToRename are package-private on TweedleEncoder

```bash
grep -E 'angleMembers|membersToRename' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java | head -4
```

Expected:
- `static final Set<String> angleMembers` — no `private` modifier.
- `static final Map<String, String> membersToRename` — no `private` modifier.

## Step 6: Verify 3 new bridge methods on TweedleEncoder

```bash
grep -E 'forwardProcessExpression|forwardAppendAccessSeparator|forwardAppendEscapedString' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: three method declarations — `forwardProcessExpression(Expression)`,
`forwardAppendAccessSeparator()`, `forwardAppendEscapedString(String)`.

## Step 7: Verify TweedleEncoder delegates to ExpressionEncoder

```bash
grep 'expressionEncoder\.' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: delegation calls in `appendTargetAndMember` and
`processResourceExpression`.

## Step 8: Verify targetIsMath and tweedleModuleForMath are NOT on TweedleEncoder

```bash
grep -E 'private.*targetIsMath|private.*tweedleModuleForMath' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: **zero matches**. Both methods have moved entirely to
`ExpressionEncoder`.

## Step 9: Verify TweedleEncoderDecoder is unchanged

```bash
git diff HEAD~1 -- core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoderDecoder.java
```

Expected: no changes. The public facade is unmodified by this extraction.
Replace `HEAD~1` with the appropriate merge base if your branch diverged.

## Step 10: Verify StatementEncoder is unchanged

```bash
git diff HEAD~1 -- core/ast/src/main/java/org/alice/serialization/tweedle/StatementEncoder.java
```

Expected: no changes. The step 1 extraction is unaffected by step 2.

## Step 11: Run ExpressionEncoderExtractionTest

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ExpressionEncoderExtractionTest \
  test -q
```

Expected: all tests pass, exit code 0.

## Step 12: Run StatementEncoderExtractionTest (regression)

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=StatementEncoderExtractionTest \
  test -q
```

Expected: all tests pass, exit code 0. Step 1 contract is preserved.

## Step 13: Run core/ast encoder tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderTest,TweedleEncoderRenameContractTest,TweedleEncoderDecoderTest,SourceCodeGeneratorTest \
  test -q
```

Expected: all tests pass, exit code 0.

## Step 14: Run story-api-migration round-trip tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test -q
```

Expected: all tests pass, exit code 0.

## Checklist

- [ ] `ExpressionEncoder.java` exists
- [ ] `ExpressionEncoder` is package-private (no `public` keyword)
- [ ] Constructor takes `TweedleEncoder` reference
- [ ] 4 methods present: `appendTargetAndMember`, `targetIsMath`, `tweedleModuleForMath`, `processResourceExpression`
- [ ] `angleMembers` and `membersToRename` are package-private on `TweedleEncoder`
- [ ] 3 new bridge methods on `TweedleEncoder`: `forwardProcessExpression`, `forwardAppendAccessSeparator`, `forwardAppendEscapedString`
- [ ] `TweedleEncoder` delegates via `expressionEncoder.` calls
- [ ] `targetIsMath` and `tweedleModuleForMath` removed from `TweedleEncoder`
- [ ] `TweedleEncoderDecoder.java` is unchanged
- [ ] `StatementEncoder.java` is unchanged
- [ ] `ExpressionEncoderExtractionTest` passes
- [ ] `StatementEncoderExtractionTest` passes
- [ ] `TweedleEncoderTest` passes
- [ ] `TweedleEncoderRenameContractTest` passes
- [ ] `TweedleEncoderDecoderTest` passes
- [ ] `SourceCodeGeneratorTest` passes
- [ ] `core/story-api-migration` tests pass
