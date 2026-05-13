# Desktop Run execution evidence decomposition

This reference describes the decomposition of
`EatmeDesktopRunExecutionEvidence.java` from a single 1103-line class into six
focused classes. The original class remains the public coordinator; extracted
helpers are package-private with no new public surface.

## Contents

- [Scope](#scope)
- [Class inventory](#class-inventory)
- [EatmeDesktopRunExecutionEvidence (coordinator)](#eatmedesktoprunexecutionevidence-coordinator)
- [EatmeEvidenceWriter](#eatmeevidencewriter)
- [EatmeWindowDetector](#eatmewindowdetector)
- [EatmeScreenshotCapture](#eatmescreenshotcapture)
- [PixelObservation](#pixelobservation)
- [BlockerDetail](#blockerdetail)
- [Dependency graph](#dependency-graph)
- [API compatibility](#api-compatibility)
- [Configuration](#configuration)
- [Security invariants](#security-invariants)
- [Validation commands](#validation-commands)
- [Troubleshooting](#troubleshooting)

## Scope

The decomposition applies to this single file in `core/ide`:

```text
core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java
```

The test file is unchanged:

```text
core/ide/src/test/java/org/alice/tools/EatmeDesktopRunExecutionEvidenceTest.java
```

All six classes live in the `org.alice.tools` package. The extracted classes are
`final` and package-private. No test modifications are required because the
coordinator retains forwarding delegates for every package-private method the
tests call.

## Class inventory

After decomposition, the `org.alice.tools` package contains these files for the
desktop Run execution evidence feature:

| File | Lines | Visibility | Role |
| --- | --- | --- | --- |
| `EatmeDesktopRunExecutionEvidence.java` | ≤350 | `public final` | Coordinator: public API, Recorder inner class, constants, forwarding delegates |
| `EatmeEvidenceWriter.java` | ~200 | `final` (package-private) | All JSON artifact writing, validation, atomic I/O, gap report builders |
| `EatmeWindowDetector.java` | ~120 | `final` (package-private) | Component readiness checks, pure functions |
| `EatmeScreenshotCapture.java` | ~140 | `final` (package-private) | Robot screen capture, PNG write, headless gate |
| `PixelObservation.java` | ~80 | `final` (package-private) | Pixel result record + factory methods for `observed`/`blocked` |
| `BlockerDetail.java` | ~30 | `final` (package-private) | Immutable data record: code, observed, required + blocker list JSON formatting |

Total line count across all six files is comparable to the original 1103 lines.
The coordinator is under 350 lines; each extracted class is under 250 lines.

## EatmeDesktopRunExecutionEvidence (coordinator)

The coordinator retains:

- **All public constants** (`EVIDENCE_DIR_PROPERTY`, artifact names,
  `MAX_RECORDED_EVENTS`, claim strings, required artifact lists).
- **All public static methods**: `install(...)`, `recordRenderTargetAttached(...)`.
- **The `Recorder` inner class**: implements `VirtualMachineListener`, lifecycle
  tracking, statement counting.
- **`evidenceDirProperty()`**: package-private, reads the system property.
- **Forwarding delegates** for every package-private method that tests invoke:
  `writeDesktopRunExecution(...)`, `writeRenderTargetAttached(...)`,
  `writeDesktopRunExecutionGapReport(...)`,
  `validateDesktopRunExecutionGapReport(...)`.
- **`writeRenderTargetAttached(...)`**: orchestrates the full render-target
  attachment flow by delegating to the extracted classes.

The coordinator does not contain JSON string building, screenshot capture logic,
or component readiness analysis. It delegates to the appropriate extracted class
for each concern.

## EatmeEvidenceWriter

Owns all JSON artifact I/O and artifact validation:

| Method | Purpose |
| --- | --- |
| `writeDesktopRunExecution(...)` | Builds and writes `desktop-run-execution.json` + runtime log |
| `writeRenderAffordance(...)` | Builds and writes `desktop-run-render-affordance.json` |
| `writePixelBoundary(...)` | Builds and writes `desktop-run-pixel-boundary.json` |
| `writePixelObservation(...)` | Builds and writes `desktop-run-pixel-observation.json` |
| `writeFirstLessonNextActionContract(...)` | Builds and writes `desktop-first-lesson-next-action.json` |
| `writeSaveMenuActionTargetNoGo(...)` | Builds and writes `desktop-save-menu-action-target.json` |
| `writeRunStatusSummary(...)` | Builds and writes `desktop-run-status-summary.json` |
| `writeDesktopRunExecutionGapReport(...)` | Validates then writes `desktop-run-execution-gap-report.json` |
| `validateDesktopRunExecutionGapReport(...)` | Fail-closed report payload validation |
| `writeStringAtomically(...)` | Atomic write via temp file + `ATOMIC_MOVE` |
| `requireNonEmptyArtifact(...)` | Post-write non-empty check |
| `runtimeLog(...)` | Builds the `desktop-run-runtime.log` content |
| `desktopRunExecutionGapReportJson(...)` | Builds the gap report JSON string |
| `executionGapEvidenceArtifactsJson(...)` | Builds the evidence artifacts JSON array for the gap report |
| `executionGapEvidenceDescription(...)` | Per-artifact evidence description (switch expression) |
| `executionGapClaimLimit(...)` | Per-artifact claim limit text (switch expression) |
| `pixelObservationSummary(...)` | Text summary of pixel observation status |
| `pixelObservationReportingNote(...)` | Reporting-note text for the status summary |

All file system writes go through `writeStringAtomically(...)` or
`writePngAtomically(...)` (the PNG variant is in `EatmeScreenshotCapture`). All
string values pass through `EatmeRunWindowEvidence.escapeJson(...)` before
interpolation.

## EatmeWindowDetector

Owns component readiness analysis. Pure functions with no I/O:

| Method | Purpose |
| --- | --- |
| `componentReadinessBlockers(component, blockerPrefix, statePrefix)` | Returns `List<BlockerDetail>` for displayable/showing/size checks |
| `componentClassName(component)` | JSON-escaped class name |
| `componentName(component)` | Null-safe component name |
| `childComponentCount(component)` | Child count (Container-aware) |
| `firstNonBlank(first, second)` | First non-blank string utility |

No AWT Robot, no file I/O, no screenshot capture. These methods take
`java.awt.Component` and return strings or data records.

## EatmeScreenshotCapture

Owns Robot-based screen capture and PNG writing:

| Method | Purpose |
| --- | --- |
| `observePixel(evidenceDir, renderTarget, renderPanel)` | Top-level pixel observation: headless gate → component attempts → fallback merge |
| `observeComponentPixel(evidenceDir, component, blockerPrefix, statePrefix, captureRole)` | Single-component capture: readiness check → screen location → Robot capture → PNG write → center pixel sample |
| `writePngAtomically(target, image)` | Atomic PNG write via temp file + `ATOMIC_MOVE` |
| `exceptionObserved(throwable)` | Formats exception for blocker detail |

The headless gate (`GraphicsEnvironment.isHeadless()`) is checked at the top of
`observePixel(...)` before any Robot construction. On headless environments, the
method returns `PixelObservation.blocked(...)` with the appropriate blocker
codes.

`observeComponentPixel(...)` delegates to `EatmeWindowDetector` for readiness
checks and produces `PixelObservation` results. The one-way dependency is:

```text
EatmeScreenshotCapture → EatmeWindowDetector (readiness checks)
EatmeScreenshotCapture → PixelObservation (result construction)
EatmeScreenshotCapture → BlockerDetail (blocker records)
```

## PixelObservation

Promoted from a private inner class to a top-level package-private class.
Immutable data record holding the pixel observation result:

| Field | Type | Purpose |
| --- | --- | --- |
| `status` | `String` | `"observed"` or `"blocked"` |
| `claim` | `String` | Human-readable claim text |
| `detailJson` | `String` | Pre-built JSON fragment for the observation-specific fields |
| `blockers` | `List<BlockerDetail>` | Blocker details (empty when observed) |
| `exceptionType` | `String` | Exception class name when blocked by exception |

Instance methods:

| Method | Purpose |
| --- | --- |
| `isObserved()` | Returns `true` when `status` is `"observed"` |

Factory methods:

| Factory | Purpose |
| --- | --- |
| `PixelObservation.blocked(blockers, exceptionType)` | Constructs a blocked result with blocker JSON |
| `PixelObservation.observed(captureRole, componentClass, componentName, screenshot, captureArea, width, height, sampleX, sampleY, argb)` | Constructs an observed result with capture details |

The `observed(...)` factory takes `String componentClass` and
`String componentName` instead of `Component`. This breaks the dependency on
`java.awt.Component` and allows the caller (`EatmeScreenshotCapture`) to extract
the component metadata before constructing the observation. The caller uses
`EatmeWindowDetector.componentClassName(...)` and
`EatmeWindowDetector.componentName(...)` to produce those strings.

## BlockerDetail

Promoted from a private inner class to a top-level package-private class.
Immutable data record with static list-formatting utilities:

```java
final class BlockerDetail {
  final String code;
  final String observed;
  final String required;

  BlockerDetail(String code, String observed, String required) { ... }

  static String blockerCodesJson(List<BlockerDetail> blockers) { ... }
  static String blockerDetailsJson(List<BlockerDetail> blockers) { ... }
  static String jsonArray(List<String> values) { ... }
}
```

The `blockerCodesJson(...)`, `blockerDetailsJson(...)`, and `jsonArray(...)` static
methods are promoted from the original class to `BlockerDetail` because they
operate on `List<BlockerDetail>` and are needed by both `PixelObservation`
(inside the `blocked(...)` factory) and `EatmeEvidenceWriter`. Placing them here
avoids a `PixelObservation → EatmeEvidenceWriter` dependency that would create a
circular import (since the writer already depends on `PixelObservation`).

Used by `EatmeWindowDetector`, `EatmeScreenshotCapture`, `PixelObservation`, and
`EatmeEvidenceWriter`. Its only external dependency is `EatmeRunWindowEvidence`
(for `escapeJson` in `blockerDetailsJson` and `jsonArray`). It has no
dependencies on other extracted classes.

## Dependency graph

```text
EatmeDesktopRunExecutionEvidence (coordinator)
  ├── EatmeEvidenceWriter
  │     ├── PixelObservation
  │     ├── BlockerDetail
  │     └── EatmeRunWindowEvidence (existing)
  ├── EatmeWindowDetector
  │     ├── BlockerDetail
  │     └── EatmeRunWindowEvidence (existing, escapeJson in componentClassName)
  ├── EatmeScreenshotCapture
  │     ├── EatmeWindowDetector
  │     ├── PixelObservation
  │     ├── BlockerDetail
  │     └── EatmeRunWindowEvidence (existing)
  ├── PixelObservation
  │     ├── BlockerDetail (blockerCodesJson, blockerDetailsJson)
  │     └── EatmeRunWindowEvidence (existing)
  └── BlockerDetail (data record + blocker list formatting)
        └── EatmeRunWindowEvidence (existing, escapeJson in blockerDetailsJson/jsonArray)
```

There are no circular dependencies. All arrows point downward.
`EatmeWindowDetector` does not depend on `EatmeScreenshotCapture`.
`PixelObservation` does not depend on `EatmeScreenshotCapture`,
`EatmeWindowDetector`, or `EatmeEvidenceWriter`. The `blockerCodesJson` and
`blockerDetailsJson` utilities live on `BlockerDetail` (not `EatmeEvidenceWriter`)
specifically to prevent a `PixelObservation → EatmeEvidenceWriter` circular edge.
`BlockerDetail` depends only on `EatmeRunWindowEvidence` (for `escapeJson` in its
list-formatting methods); it has no dependencies on other extracted classes.

## API compatibility

The decomposition preserves the entire existing API surface:

| API | Preserved how |
| --- | --- |
| `EatmeDesktopRunExecutionEvidence.install(...)` | Stays in coordinator |
| `EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(...)` | Stays in coordinator |
| `EatmeDesktopRunExecutionEvidence.Recorder` | Stays in coordinator |
| `EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY` | Stays in coordinator |
| `EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_EXECUTION_ARTIFACT` (and all artifact name constants) | Stay in coordinator |
| `writeDesktopRunExecution(...)` (package-private) | Forwarding delegate in coordinator → `EatmeEvidenceWriter` |
| `writeRenderTargetAttached(...)` (package-private) | Forwarding delegate in coordinator, orchestrates extracted classes |
| `writeDesktopRunExecutionGapReport(...)` (package-private) | Forwarding delegate → `EatmeEvidenceWriter` |
| `validateDesktopRunExecutionGapReport(...)` (package-private) | Forwarding delegate → `EatmeEvidenceWriter` |

**Zero test modifications required.** The test class
`EatmeDesktopRunExecutionEvidenceTest` calls package-private methods on the
coordinator class. Forwarding delegates ensure every existing call site continues
to work with identical behavior.

## Configuration

No new configuration is introduced. The decomposition is an internal
refactoring.

| Property | Purpose | Changed? |
| --- | --- | --- |
| `org.alice.eatme.desktopRunExecutionEvidenceDir` | Enables desktop Run evidence | No |
| `org.alice.eatme.runWindowEvidenceDir` | Legacy fallback | No |

The system property reads remain in the coordinator's `evidenceDirProperty()`
method. Extracted classes receive validated `Path` values; they never read system
properties directly.

## Security invariants

All pre-existing security properties are preserved:

| Invariant | Maintained by |
| --- | --- |
| All file I/O goes through atomic write | `EatmeEvidenceWriter.writeStringAtomically(...)` and `EatmeScreenshotCapture.writePngAtomically(...)` |
| All artifact paths validated via `EatmeRunWindowEvidence.artifactPath(...)` | Called at write sites in `EatmeEvidenceWriter` |
| All strings JSON-escaped via `EatmeRunWindowEvidence.escapeJson(...)` | Called at interpolation sites in `EatmeEvidenceWriter`, `PixelObservation`, `EatmeWindowDetector`, and `BlockerDetail` |
| Headless gate before Robot construction | `EatmeScreenshotCapture.observePixel(...)` checks `GraphicsEnvironment.isHeadless()` first |
| System property reads only in coordinator | `evidenceDirProperty()` stays in `EatmeDesktopRunExecutionEvidence` |
| `validateDesktopRunExecutionGapReport(...)` stays as a single unit | Moved to `EatmeEvidenceWriter` as one method; fail-closed behavior unchanged |
| All new classes `final` + package-private | Zero new public surface area |

## Validation commands

Run the focused `core/ide` test suite that covers the entire desktop Run
execution evidence feature:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false -Dcheckstyle.skip \
  test
```

Run only the specific evidence test:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest \
  test
```

Verify the coordinator is under 350 lines and each extracted class is under 250
lines:

```bash
wc -l core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java
wc -l core/ide/src/main/java/org/alice/tools/EatmeEvidenceWriter.java
wc -l core/ide/src/main/java/org/alice/tools/EatmeWindowDetector.java
wc -l core/ide/src/main/java/org/alice/tools/EatmeScreenshotCapture.java
wc -l core/ide/src/main/java/org/alice/tools/PixelObservation.java
wc -l core/ide/src/main/java/org/alice/tools/BlockerDetail.java
```

Verify the test file is unmodified:

```bash
git diff HEAD -- \
  core/ide/src/test/java/org/alice/tools/EatmeDesktopRunExecutionEvidenceTest.java
```

For broad Maven validation from a fresh checkout or worktree, initialize the
Tweedle grammar submodule first:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Troubleshooting

| Symptom | Meaning | Next check |
| --- | --- | --- |
| Test calls `writeDesktopRunExecution(...)` and gets `NoSuchMethodError` | Forwarding delegate missing from coordinator | Verify the coordinator has a static method matching the original signature that delegates to `EatmeEvidenceWriter` |
| `PixelObservation` constructor is not accessible | Package visibility not set correctly | Verify `PixelObservation.java` has no access modifier on the class declaration (package-private) |
| `BlockerDetail` constructor is not accessible | Package visibility not set correctly | Verify `BlockerDetail.java` has no access modifier on the class declaration (package-private) |
| Circular dependency compile error | Extracted classes have wrong import | Check the dependency graph above; no extracted class should import the coordinator |
| Test fails with different JSON output | Writer method changed behavior during extraction | Compare the exact JSON string building in `EatmeEvidenceWriter` against the original; it must be character-identical |
| Coordinator exceeds 350 lines | Logic not fully extracted | Move remaining JSON building or readiness checks to the appropriate extracted class |
