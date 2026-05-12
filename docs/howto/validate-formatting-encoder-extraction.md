# Validate the FormattingEncoder Extraction

How to verify that the `FormattingEncoder` extraction from `TweedleEncoder` is
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
ls core/ast/src/main/java/org/alice/serialization/tweedle/FormattingEncoder.java
```

Expected: file listed with no errors.

## Step 2: Verify package-private visibility

```bash
grep '^class ' core/ast/src/main/java/org/alice/serialization/tweedle/FormattingEncoder.java
```

Expected: `class FormattingEncoder {` — no `public` modifier.

## Step 3: Verify constructor takes TweedleEncoder

```bash
grep 'FormattingEncoder(TweedleEncoder' \
  core/ast/src/main/java/org/alice/serialization/tweedle/FormattingEncoder.java
```

Expected: `FormattingEncoder(TweedleEncoder encoder)` or similar.

## Step 4: Verify indent state moved to FormattingEncoder

```bash
grep -E 'private int indent|INDENTION|MAX_CACHED_INDENT|INDENT_CACHE' \
  core/ast/src/main/java/org/alice/serialization/tweedle/FormattingEncoder.java
```

Expected: all four symbols present — `indent` field, `INDENTION` constant,
`MAX_CACHED_INDENT` constant, `INDENT_CACHE` array.

```bash
grep -E 'private int indent|INDENTION|MAX_CACHED_INDENT|INDENT_CACHE' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: **zero matches**. These have moved entirely to `FormattingEncoder`.

## Step 5: Verify extracted methods exist on FormattingEncoder

```bash
grep -E 'pushIndent|popIndent|indentString|appendIndent|appendVisibilityTag|appendInstantiation|appendArg|appendAnotherArg|appendList|quoteString' \
  core/ast/src/main/java/org/alice/serialization/tweedle/FormattingEncoder.java
```

Expected: at least 13 method signatures covering all extracted methods.

## Step 6: Verify 3 new bridge methods on TweedleEncoder

```bash
grep -E 'forwardAppendChar|forwardParenthesize|forwardGetListSeparator' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: three method declarations — `forwardAppendChar(char)`,
`forwardParenthesize(Runnable)`, `forwardGetListSeparator()`.

## Step 7: Verify TweedleEncoder delegates to FormattingEncoder

```bash
grep 'formattingEncoder\.' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: delegation calls in `appendIndent`, `openBlock`, `closeBlockInline`,
`appendVisibilityTag`, `appendInstantiation`, `appendArg`, `appendAnotherArg`,
`appendList`, and `quoteString`.

## Step 8: Verify TweedleEncoder is under 500 lines

```bash
wc -l core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: at most 499 lines (requires all three extraction steps plus
blank-line trimming to reach this target).

## Step 9: Verify pushIndent, popIndent, and indentString method declarations are absent from TweedleEncoder

```bash
grep -cE '(void|String) (pushIndent|popIndent|indentString)\(' \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: `0` — no method declarations remain. Note: `formattingEncoder.pushIndent()`
and `formattingEncoder.popIndent()` delegation calls in `openBlock()` and
`closeBlockInline()` are expected and do not match this declaration-only pattern.

## Step 10: Verify TweedleEncoderDecoder is unchanged

```bash
git diff HEAD~1 -- core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoderDecoder.java
```

Expected: no changes. The public facade is unmodified by this extraction.
Replace `HEAD~1` with the appropriate merge base if your branch diverged.

## Step 11: Verify StatementEncoder and ExpressionEncoder are unchanged

```bash
git diff HEAD~1 -- \
  core/ast/src/main/java/org/alice/serialization/tweedle/StatementEncoder.java \
  core/ast/src/main/java/org/alice/serialization/tweedle/ExpressionEncoder.java
```

Expected: no changes. Steps 1 and 2 are unaffected by step 3.

## Step 12: Run FormattingEncoderExtractionTest

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=FormattingEncoderExtractionTest \
  test -q
```

Expected: all tests pass, exit code 0.

## Step 13: Run StatementEncoderExtractionTest and ExpressionEncoderExtractionTest (regression)

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=StatementEncoderExtractionTest,ExpressionEncoderExtractionTest \
  test -q
```

Expected: all tests pass, exit code 0. Steps 1 and 2 contracts are preserved.

## Step 14: Run core/ast encoder tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderTest,TweedleEncoderRenameContractTest,TweedleEncoderDecoderTest,SourceCodeGeneratorTest \
  test -q
```

Expected: all tests pass, exit code 0.

## Step 15: Run story-api-migration round-trip tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test -q
```

Expected: all tests pass, exit code 0.

## Step 16: Run core/ide encoder tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='*Encoder*Test,*SourceCode*Test' \
  test -q
```

Expected: all tests pass, exit code 0.

## Checklist

- [ ] `FormattingEncoder.java` exists
- [ ] `FormattingEncoder` is package-private (no `public` keyword)
- [ ] Constructor takes `TweedleEncoder` reference
- [ ] Indent state (`indent`, `INDENTION`, `MAX_CACHED_INDENT`, `INDENT_CACHE`) on `FormattingEncoder`
- [ ] Indent state removed from `TweedleEncoder`
- [ ] 13 methods present on `FormattingEncoder`: `pushIndent`, `popIndent`, `indentString`, `appendIndent` ×2, `appendVisibilityTag`, `appendInstantiation`, `appendArg` ×2, `appendAnotherArg` ×2, `appendList`, `quoteString`
- [ ] 3 new bridge methods on `TweedleEncoder`: `forwardAppendChar`, `forwardParenthesize`, `forwardGetListSeparator`
- [ ] `TweedleEncoder` delegates via `formattingEncoder.` calls
- [ ] `TweedleEncoder` is ≤499 lines (after all three extraction steps + blank-line trimming)
- [ ] `pushIndent`, `popIndent`, `indentString` method declarations absent from `TweedleEncoder` (zero declaration-pattern grep matches; delegation calls are expected)
- [ ] `TweedleEncoderDecoder.java` is unchanged
- [ ] `StatementEncoder.java` is unchanged
- [ ] `ExpressionEncoder.java` is unchanged
- [ ] `FormattingEncoderExtractionTest` passes
- [ ] `StatementEncoderExtractionTest` passes
- [ ] `ExpressionEncoderExtractionTest` passes
- [ ] `TweedleEncoderTest` passes
- [ ] `TweedleEncoderRenameContractTest` passes
- [ ] `TweedleEncoderDecoderTest` passes
- [ ] `SourceCodeGeneratorTest` passes
- [ ] `core/story-api-migration` tests pass
- [ ] `core/ide` encoder tests pass
