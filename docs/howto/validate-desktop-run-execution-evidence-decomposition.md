# Validate the desktop Run execution evidence decomposition

This guide explains how to verify the decomposition of
`EatmeDesktopRunExecutionEvidence.java` into six focused classes.

## Prerequisites

Initialize the Tweedle grammar submodule:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Step 1: Verify all six files exist

```bash
ls -1 core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java \
      core/ide/src/main/java/org/alice/tools/EatmeEvidenceWriter.java \
      core/ide/src/main/java/org/alice/tools/EatmeWindowDetector.java \
      core/ide/src/main/java/org/alice/tools/EatmeScreenshotCapture.java \
      core/ide/src/main/java/org/alice/tools/PixelObservation.java \
      core/ide/src/main/java/org/alice/tools/BlockerDetail.java
```

All six files must be present in `core/ide/src/main/java/org/alice/tools/`.

## Step 2: Verify line counts

```bash
wc -l core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java \
      core/ide/src/main/java/org/alice/tools/EatmeEvidenceWriter.java \
      core/ide/src/main/java/org/alice/tools/EatmeWindowDetector.java \
      core/ide/src/main/java/org/alice/tools/EatmeScreenshotCapture.java \
      core/ide/src/main/java/org/alice/tools/PixelObservation.java \
      core/ide/src/main/java/org/alice/tools/BlockerDetail.java
```

Expected:

| File | Target |
| --- | --- |
| `EatmeDesktopRunExecutionEvidence.java` | ≤350 lines |
| `EatmeEvidenceWriter.java` | ≤250 lines |
| `EatmeWindowDetector.java` | ≤150 lines |
| `EatmeScreenshotCapture.java` | ≤200 lines |
| `PixelObservation.java` | ≤100 lines |
| `BlockerDetail.java` | ≤50 lines |

The coordinator must be under 500 lines (hard limit). Under 350 is the design
target.

## Step 3: Verify visibility

Check that extracted classes are package-private (no `public` keyword):

```bash
head -5 core/ide/src/main/java/org/alice/tools/EatmeEvidenceWriter.java
head -5 core/ide/src/main/java/org/alice/tools/EatmeWindowDetector.java
head -5 core/ide/src/main/java/org/alice/tools/EatmeScreenshotCapture.java
head -5 core/ide/src/main/java/org/alice/tools/PixelObservation.java
head -5 core/ide/src/main/java/org/alice/tools/BlockerDetail.java
```

Each extracted class must declare `final class ClassName` (no `public`
modifier). Only `EatmeDesktopRunExecutionEvidence` is `public final class`.

## Step 4: Run the focused test suite

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false -Dcheckstyle.skip \
  test
```

All existing tests must pass. No test file changes are expected.

## Step 5: Verify the test file is unmodified

```bash
git diff HEAD -- \
  core/ide/src/test/java/org/alice/tools/EatmeDesktopRunExecutionEvidenceTest.java
```

Expected output: empty (no changes). The forwarding delegates in the coordinator
preserve the full package-private API surface that the test class exercises.

## Step 6: Verify no circular dependencies

Extracted classes must not import the coordinator:

```bash
grep -l 'import.*EatmeDesktopRunExecutionEvidence' \
  core/ide/src/main/java/org/alice/tools/EatmeEvidenceWriter.java \
  core/ide/src/main/java/org/alice/tools/EatmeWindowDetector.java \
  core/ide/src/main/java/org/alice/tools/EatmeScreenshotCapture.java \
  core/ide/src/main/java/org/alice/tools/PixelObservation.java \
  core/ide/src/main/java/org/alice/tools/BlockerDetail.java
```

Expected output: no matches. `EatmeWindowDetector` must not import
`EatmeScreenshotCapture`. `PixelObservation` must not import
`EatmeScreenshotCapture` or `EatmeWindowDetector`.

## Step 7: Verify forwarding delegates

The coordinator must still expose these package-private static methods used by
tests:

```bash
grep -n 'static.*writeDesktopRunExecution\b' \
  core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java

grep -n 'static.*writeDesktopRunExecutionGapReport\b' \
  core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java

grep -n 'static.*validateDesktopRunExecutionGapReport\b' \
  core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java
```

Each must show a match in the coordinator file. These methods delegate to
`EatmeEvidenceWriter` internally.

## Step 8: Verify security invariants

Check that atomic writes are preserved:

```bash
grep -rn 'ATOMIC_MOVE' \
  core/ide/src/main/java/org/alice/tools/EatmeEvidenceWriter.java \
  core/ide/src/main/java/org/alice/tools/EatmeScreenshotCapture.java
```

Check that JSON escaping is used at interpolation sites:

```bash
grep -c 'escapeJson' \
  core/ide/src/main/java/org/alice/tools/EatmeEvidenceWriter.java \
  core/ide/src/main/java/org/alice/tools/PixelObservation.java \
  core/ide/src/main/java/org/alice/tools/BlockerDetail.java
```

All three files must have escapeJson calls.

## Reviewing the decomposition in a PR

When reviewing the PR:

1. Confirm all six files exist in `org.alice.tools`.
2. Confirm the coordinator is under 500 lines.
3. Confirm the test file has zero diff.
4. Confirm `mvn test` passes for `core/ide`.
5. Confirm no `public` modifier on extracted classes.
6. Confirm no circular imports.

Accepted PR wording:

```text
Decomposes EatmeDesktopRunExecutionEvidence (1103 lines) into a thin coordinator
plus five focused helpers: EatmeEvidenceWriter (JSON/IO), EatmeWindowDetector
(component readiness), EatmeScreenshotCapture (Robot capture), PixelObservation
(result record), and BlockerDetail (blocker record). Coordinator is under 350
lines. Zero test changes. All forwarding delegates preserve the package-private
API surface.
```
