# Validate the TweedleEncoder Rename

How to verify that the `Encoder` → `TweedleEncoder` rename is complete and
correct. Use this after merging the rename or when reviewing the PR.

## Prerequisites

- Java 17+ and Maven installed
- Tweedle grammar submodule initialized:
  ```bash
  git submodule update --init tweedle-lang
  ```

## Step 1: Confirm no stale references

Search the entire Java source tree for the old fully-qualified import:

```bash
grep -r 'org\.alice\.serialization\.tweedle\.Encoder[^D]' \
  --include='*.java' .
```

Expected: **zero matches**. Any match indicates an incomplete rename.

Also confirm the old file is gone:

```bash
test ! -f core/ast/src/main/java/org/alice/serialization/tweedle/Encoder.java \
  && echo "OK: Encoder.java removed"
```

## Step 2: Confirm the new file exists

```bash
test -f core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java \
  && echo "OK: TweedleEncoder.java exists"
```

Verify the class declaration matches:

```bash
head -25 core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java \
  | grep 'class TweedleEncoder'
```

Expected output: `public class TweedleEncoder extends SourceCodeGenerator {`

## Step 3: Run core/ast tests

This suite includes 57 characterization tests that exercise the encode path:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false test -q
```

Expected: all tests pass, exit code 0.

## Step 4: Run story-api-migration tests

The round-trip tests encode projects through `TweedleEncoderDecoder` and verify
fidelity:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test -q
```

Expected: all tests pass, exit code 0.

## Step 5: Run silver thread round-trip

The silver thread test encodes every `NamedUserType` from a real starter
project, decodes the Tweedle source, and asserts structural identity:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadTweedleDecoderRoundTripTest test -q
```

Expected: all tests pass, exit code 0.

## Step 6: Verify git history preservation

Confirm `git mv` preserved history:

```bash
git log --follow --oneline -5 \
  core/ast/src/main/java/org/alice/serialization/tweedle/TweedleEncoder.java
```

Expected: commits from before the rename appear in the log.

## Checklist

- [ ] Zero matches for old `tweedle.Encoder` import
- [ ] `Encoder.java` removed
- [ ] `TweedleEncoder.java` exists with correct class name
- [ ] `core/ast` tests pass (57 characterization + others)
- [ ] `core/story-api-migration` tests pass
- [ ] Silver thread round-trip test passes
- [ ] Git history preserved across rename
