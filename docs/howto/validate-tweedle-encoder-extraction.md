# Validate the TweedleEncoder Extraction

How to verify that the `TweedleEncoder` delegate extraction is complete and
correct. Use this after merging the extraction or when reviewing the PR.

## Prerequisites

- Java 17+ and Maven installed
- Tweedle grammar submodule initialized:
  ```bash
  git submodule update --init tweedle-lang
  ```

## Step 1: Confirm new delegate files exist

```bash
cd core/ast/src/main/java/org/alice/serialization/tweedle
ls -1 EncoderMappings.java StatementEncoder.java \
      ExpressionEncoder.java ResourceStructureEncoder.java
```

Expected: all four files listed with no errors.

## Step 2: Verify delegate visibility

All delegates must be package-private (no `public` modifier on the class):

```bash
grep '^class ' EncoderMappings.java StatementEncoder.java \
                ExpressionEncoder.java ResourceStructureEncoder.java
```

Expected: each line starts with `class` (not `public class`). For example:

```text
EncoderMappings.java:class EncoderMappings {
StatementEncoder.java:class StatementEncoder {
ExpressionEncoder.java:class ExpressionEncoder {
ResourceStructureEncoder.java:class ResourceStructureEncoder {
```

## Step 3: Verify TweedleEncoder line count

```bash
wc -l TweedleEncoder.java
```

Expected: ≤ 500 lines.

## Step 4: Verify TweedleEncoder remains public

```bash
grep 'public class TweedleEncoder' TweedleEncoder.java
```

Expected: `public class TweedleEncoder extends SourceCodeGenerator {`

## Step 5: Verify TweedleEncoderDecoder is unchanged

```bash
git diff main -- TweedleEncoderDecoder.java
```

Expected: no changes. The public facade is unmodified by this extraction.
If your branch diverged from a different base, replace `main` with the
appropriate merge base.

## Step 6: Run core/ast encoder tests

This suite includes characterization tests that exercise the encode path:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderTest,TweedleEncoderRenameContractTest,TweedleEncoderDecoderTest \
  test -q
```

Expected: all tests pass, exit code 0.

## Step 7: Run decoder delegate decomposition characterization

This test exercises the full encode→decode round-trip and verifies structural
identity across the delegate boundary:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=DecoderDelegateDecompositionCharacterizationTest \
  test -q
```

Expected: all tests pass, exit code 0.

## Step 8: Run story-api-migration tests

The round-trip tests encode projects through `TweedleEncoderDecoder` and verify
fidelity:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test -q
```

Expected: all tests pass, exit code 0.

## Step 9: Run silver thread round-trip

The silver thread test encodes every `NamedUserType` from a real starter
project, decodes the Tweedle source, and asserts structural identity:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SilverThreadTweedleDecoderRoundTripTest test -q
```

Expected: all tests pass, exit code 0.

## Step 10: Verify no stale references to moved methods

Check that no external callers reference methods that moved to delegates:

```bash
grep -r 'appendResourceConstructor\|appendResourceFields\|appendResourceInstances' \
  --include='*.java' core/ \
  | grep -v ResourceStructureEncoder | grep -v TweedleEncoder
```

Expected: **zero matches** outside the two expected files. External code only
calls `TweedleEncoder` public/protected methods and `encode(ProcessableNode)`.

## Checklist

- [ ] Four new delegate files exist
- [ ] All delegates are package-private
- [ ] `TweedleEncoder.java` ≤ 500 lines
- [ ] `TweedleEncoder` class remains `public`
- [ ] `TweedleEncoderDecoder.java` unchanged
- [ ] `TweedleEncoderTest` passes
- [ ] `TweedleEncoderRenameContractTest` passes
- [ ] `TweedleEncoderDecoderTest` passes
- [ ] `DecoderDelegateDecompositionCharacterizationTest` passes
- [ ] `core/story-api-migration` tests pass
- [ ] Silver thread round-trip test passes
- [ ] No stale external references to moved methods
