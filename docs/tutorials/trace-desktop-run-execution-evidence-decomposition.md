# Tutorial: Trace the desktop Run execution evidence decomposition

This tutorial walks through the decomposition of
`EatmeDesktopRunExecutionEvidence.java` from a single 1103-line class into six
focused classes. Each section traces where a specific responsibility moved and
why.

## Before you start

Open these files side-by-side or in tabs:

```text
core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java  (coordinator)
core/ide/src/main/java/org/alice/tools/EatmeEvidenceWriter.java
core/ide/src/main/java/org/alice/tools/EatmeWindowDetector.java
core/ide/src/main/java/org/alice/tools/EatmeScreenshotCapture.java
core/ide/src/main/java/org/alice/tools/PixelObservation.java
core/ide/src/main/java/org/alice/tools/BlockerDetail.java
core/ide/src/test/java/org/alice/tools/EatmeDesktopRunExecutionEvidenceTest.java  (unchanged)
```

## 1. Start at BlockerDetail — the leaf

`BlockerDetail` was a `private static final class` nested inside the original.
It holds three immutable fields: `code`, `observed`, `required`. It has zero
dependencies on any other extracted class.

**Why extract it first?** It is a leaf in the dependency graph. Every other
extracted class (`EatmeWindowDetector`, `EatmeScreenshotCapture`,
`PixelObservation`, `EatmeEvidenceWriter`) references `BlockerDetail`. Promoting
it to a top-level package-private class allows all of them to import it directly
instead of through the coordinator.

**What changed:** The access modifier on the class and constructor widened from
`private` to package-private. The field access widened from `private` to
package-private. The `blockerCodesJson(...)`, `blockerDetailsJson(...)`, and
`jsonArray(...)` static methods moved here from the enclosing class because they
operate on `List<BlockerDetail>` and are needed by both `PixelObservation`
(inside its `blocked(...)` factory) and `EatmeEvidenceWriter`. Placing them on
`BlockerDetail` avoids a `PixelObservation → EatmeEvidenceWriter` edge that
would create a circular dependency.

## 2. PixelObservation — the result record

`PixelObservation` was also a `private static final class` nested inside the
original. It holds the observation result: `status`, `claim`, `detailJson`,
`blockers`, `exceptionType`.

**Key design decision:** The `observed(...)` factory method originally took a
`Component` parameter to extract `componentClassName(component)` and
`componentName(component)`. The promoted version takes `String componentClass`
and `String componentName` instead. This breaks the dependency on
`java.awt.Component` in `PixelObservation` itself, keeping it a pure data class.

Trace the call chain:

```text
EatmeScreenshotCapture.observeComponentPixel(...)
  → componentClass = EatmeWindowDetector.componentClassName(component)
  → componentName  = EatmeWindowDetector.componentName(component)
  → PixelObservation.observed(captureRole, componentClass, componentName, ...)
```

The caller extracts the strings before constructing the observation. The JSON
output is character-identical.

## 3. EatmeWindowDetector — pure component analysis

Open `EatmeWindowDetector.java` and find these methods:

- `componentReadinessBlockers(component, blockerPrefix, statePrefix)` — builds a
  `List<BlockerDetail>` by checking `isDisplayable()`, `isShowing()`, and
  `getWidth()`/`getHeight()`.
- `componentClassName(component)` — `escapeJson(component.getClass().getName())`.
- `componentName(component)` — null-safe `component.getName()`.
- `childComponentCount(component)` — casts to `Container` if applicable.
- `firstNonBlank(first, second)` — returns the first non-blank string.

**Why these go together:** They are all pure functions that inspect AWT component
state and return strings or lists. None of them perform I/O, write files, or
construct Robot objects.

Trace the original locations:

| Method | Original location | New location |
| --- | --- | --- |
| `componentReadinessBlockers(...)` | Lines 880–908 | `EatmeWindowDetector` |
| `componentClassName(...)` | Lines 960–962 | `EatmeWindowDetector` |
| `componentName(...)` | Lines 964–967 | `EatmeWindowDetector` |
| `childComponentCount(...)` | Lines 969–974 | `EatmeWindowDetector` |
| `firstNonBlank(...)` | Lines 910–918 | `EatmeWindowDetector` |

The coordinator has one-line forwarding methods for `componentClassName`,
`componentName`, and `childComponentCount` only if future callers outside the
coordinator need them. Currently, the coordinator's `writeRenderTargetAttached`
can call `EatmeWindowDetector` directly for these, so forwarding delegates are
not strictly needed for these three methods.

## 4. EatmeScreenshotCapture — Robot and PNG isolation

Open `EatmeScreenshotCapture.java` and find:

- `observePixel(evidenceDir, renderTargetComponent, renderPanelComponent)` —
  headless gate, then tries render target, then render panel, then merges
  blockers.
- `observeComponentPixel(evidenceDir, component, blockerPrefix, statePrefix,
  captureRole)` — readiness check → screen location → Robot capture → PNG write →
  center pixel sample.
- `writePngAtomically(target, image)` — atomic PNG via temp + `ATOMIC_MOVE`.
- `exceptionObserved(throwable)` — formats exception for blocker details.

**Why these go together:** They all involve `java.awt.Robot`, `BufferedImage`,
`ImageIO`, and screen coordinate geometry. The headless gate
(`GraphicsEnvironment.isHeadless()`) is checked at the top of `observePixel(...)`
before any Robot construction.

Trace the dependency:

```text
EatmeScreenshotCapture.observeComponentPixel(...)
  → EatmeWindowDetector.componentReadinessBlockers(...)  [readiness check]
  → new Robot().createScreenCapture(...)                 [capture]
  → writePngAtomically(...)                              [write]
  → PixelObservation.observed(...) or .blocked(...)      [result]
```

`EatmeScreenshotCapture` depends on `EatmeWindowDetector` (one-way) but
`EatmeWindowDetector` does not depend on `EatmeScreenshotCapture`. This breaks
the potential bidirectional coupling.

## 5. EatmeEvidenceWriter — all JSON and validation

Open `EatmeEvidenceWriter.java`. This is the largest extracted class. It owns:

- Every `write*` method that produces a JSON artifact file.
- `validateDesktopRunExecutionGapReport(...)` — the fail-closed validation.
- `writeStringAtomically(...)` — the atomic text file write.
- `requireNonEmptyArtifact(...)` — post-write existence check.
- `runtimeLog(...)` — builds the runtime log content.
- `pixelObservationSummary(...)`, `pixelObservationReportingNote(...)` — text
  summaries used by `writeRunStatusSummary(...)`.
- `executionGapEvidenceDescription(...)`, `executionGapClaimLimit(...)` — switch
  expressions for the gap report's per-artifact evidence descriptions.
- `desktopRunExecutionGapReportJson(...)`,
  `executionGapEvidenceArtifactsJson(...)` — the gap report JSON builder.

Note: `blockerCodesJson(...)`, `blockerDetailsJson(...)`, and `jsonArray(...)`
do NOT go here — they live on `BlockerDetail` because `PixelObservation.blocked()`
needs them, and the writer already depends on `PixelObservation`.

**Why everything JSON goes here:** The original class interleaved JSON building,
file I/O, validation, and component analysis. By consolidating all string-to-file
operations in one class, the boundary is clear: callers prepare data, the writer
formats and persists it.

Trace the gap report validation flow:

```text
EatmeDesktopRunExecutionEvidence.writeDesktopRunExecutionGapReport(...)  [coordinator delegate]
  → EatmeEvidenceWriter.writeDesktopRunExecutionGapReport(...)
      → EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(...)    [fail-closed checks]
      → EatmeEvidenceWriter.desktopRunExecutionGapReportJson(...)        [build JSON string]
      → EatmeEvidenceWriter.writeStringAtomically(...)                   [atomic write]
      → EatmeEvidenceWriter.requireNonEmptyArtifact(...)                 [post-write check]
```

The validation method stays as a single unit. It was not split across classes.

## 6. The coordinator — what stays and why

Open `EatmeDesktopRunExecutionEvidence.java`. After decomposition it contains:

**Constants** — all public `static final String` artifact names and the private
constants (`MAX_RECORDED_EVENTS`, `RENDER_AFFORDANCE_CLAIM`, required artifact
lists, prohibited claim categories). These stay because they are part of the
public contract and are referenced by tests.

**The `Recorder` inner class** — implements `VirtualMachineListener`, tracks
lifecycle events and statement counts, calls `writeArtifacts()` which delegates
to `EatmeEvidenceWriter.writeDesktopRunExecution(...)`.

**`install(...)` and `recordRenderTargetAttached(...)`** — the two public entry
points. They stay because they form the public API.

**`evidenceDirProperty()`** — the system property reader. Stays because extracted
classes should not read system properties directly.

**`writeRenderTargetAttached(...)`** — the orchestration method. It calls
`EatmeEvidenceWriter` for JSON writes, `EatmeScreenshotCapture` for pixel
observation, and `EatmeWindowDetector` for component metadata. This is the main
coordination logic.

**Forwarding delegates** — thin methods that preserve the package-private API:

```java
static Path writeDesktopRunExecution(...) throws IOException {
    return EatmeEvidenceWriter.writeDesktopRunExecution(...);
}

static Path writeDesktopRunExecutionGapReport(...) throws IOException {
    return EatmeEvidenceWriter.writeDesktopRunExecutionGapReport(...);
}

static void validateDesktopRunExecutionGapReport(...) {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(...);
}
```

These delegates exist solely so that `EatmeDesktopRunExecutionEvidenceTest` can
call the same methods it called before the decomposition.

## 7. Verify the test is unchanged

The test file `EatmeDesktopRunExecutionEvidenceTest.java` has zero modifications.
Every method it calls is either:

- A public method on the coordinator (`recordRenderTargetAttached`,
  `EVIDENCE_DIR_PROPERTY`, artifact name constants).
- A package-private method on the coordinator (forwarding delegate).
- A method on `EatmeRunWindowEvidence` (unchanged).

Run the test to confirm:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false -Dcheckstyle.skip \
  test
```

## Summary of extraction moves

| Original lines | Responsibility | Destination |
| --- | --- | --- |
| 265–295 | `writeDesktopRunExecution` JSON + log | `EatmeEvidenceWriter` |
| 297–375 | `writeRenderTargetAttached` orchestration | Coordinator (calls extracted classes) |
| 377–429 | `writeFirstLessonNextActionContract` JSON | `EatmeEvidenceWriter` |
| 431–475 | `writeSaveMenuActionTargetNoGo` JSON | `EatmeEvidenceWriter` |
| 477–555 | `writeRunStatusSummary` JSON | `EatmeEvidenceWriter` |
| 557–614 | Gap report write + validation | `EatmeEvidenceWriter` |
| 616–696 | Gap report JSON builders + switch expressions | `EatmeEvidenceWriter` |
| 698–708 | `pixelObservationSummary`, `pixelObservationReportingNote` | `EatmeEvidenceWriter` |
| 710–750 | `writePixelObservation` (JSON part) | `EatmeEvidenceWriter` |
| 752–794 | `observePixel` (headless gate + multi-component) | `EatmeScreenshotCapture` |
| 796–878 | `observeComponentPixel` (Robot + PNG) | `EatmeScreenshotCapture` |
| 880–908 | `componentReadinessBlockers` | `EatmeWindowDetector` |
| 910–918 | `firstNonBlank` | `EatmeWindowDetector` |
| 920–926 | `exceptionObserved` | `EatmeScreenshotCapture` |
| 928–932 | `writeStringAtomically` | `EatmeEvidenceWriter` |
| 934–940 | `writePngAtomically` | `EatmeScreenshotCapture` |
| 942–946 | `requireNonEmptyArtifact` | `EatmeEvidenceWriter` |
| 948–958 | `runtimeLog` | `EatmeEvidenceWriter` |
| 960–974 | `componentClassName`, `componentName`, `childComponentCount` | `EatmeWindowDetector` |
| 976–1053 | `PixelObservation` inner class | `PixelObservation.java` top-level |
| 1055–1065 | `BlockerDetail` inner class | `BlockerDetail.java` top-level |
| 1067–1102 | `blockerCodesJson`, `blockerDetailsJson`, `jsonArray` | `BlockerDetail` (static utilities on the data record) |
