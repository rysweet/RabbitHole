# SaveOperationCompletionEvidence Extraction

This reference documents the extraction of JSON evidence builders and
file-system guard methods from `SaveOperationCompletionEvidence` (issue #561)
into two new package-private delegate classes: `EvidenceJsonWriter` and
`EvidenceFileOperations`.

## Contents

- [Motivation](#motivation)
- [Extracted responsibilities](#extracted-responsibilities)
- [File inventory](#file-inventory)
- [Delegate pattern](#delegate-pattern)
- [Visibility rules](#visibility-rules)
- [Inner classes retained](#inner-classes-retained)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Security considerations](#security-considerations)
- [Examples](#examples)

## Motivation

`SaveOperationCompletionEvidence.java` was 1009 lines. It mixed three
distinct concerns in a single file:

1. **JSON evidence writing** — 13+ methods that build JSON strings for result,
   dialog-control, save-action-invocation-proof, and save-proof artifacts.
2. **File-system guards** — path traversal protection, symlink rejection,
   canonical directory resolution, and file state inspection.
3. **Orchestration** — the public/package-private API surface (`record`,
   `recordSaveActionInvocation`, `write`, `saveProofEvidence`, etc.) and
   two inner classes (`SaveProofEvidence`, `InvocationTrigger`).

The extraction separates JSON generation and file-system helpers into
dedicated classes, reducing `SaveOperationCompletionEvidence` to ~500 lines
and keeping each file focused on a single responsibility.

## Extracted responsibilities

### EvidenceJsonWriter

All JSON string builder methods move to `EvidenceJsonWriter`:

| Method | Lines | Purpose |
| --- | --- | --- |
| `escapeJson` | ~20 | Escapes JSON special characters including control chars below U+0020. |
| `resultJson` | ~30 | Builds the `desktop-save-operation-result.json` content. |
| `resultClaimOrSummaryJson` | ~10 | Conditional claim vs. reporting-summary selector. |
| `nonEmptyProjectFileWrite` | ~5 | Extension-aware description fragment. |
| `dialogControlTargetJson` | ~45 | Builds the `desktop-save-dialog-control-target.json` content. |
| `saveActionInvocationProofJson` | ~40 | Builds the `desktop-save-action-invocation-proof.json` content. |
| `saveActionInvocationReason` | ~10 | Derives the invocation reason enum string. |
| `saveActionObserved` | ~10 | Maps reason to human-readable observation. |
| `saveActionReportingSummary` | ~10 | Maps reason to human-readable summary. |
| `saveActionRequiresNextEvidenceJson` | ~20 | Builds the conditional `requiresNextEvidence` JSON array. |
| `saveActionDoesNotClaimJson` | ~10 | Builds the conditional `doesNotClaim` JSON array. |
| `savedFileJson` | ~4 | Nullable file path JSON fragment. |
| `savedFileExistsJson` | ~4 | Nullable boolean JSON fragment. |
| `savedFileSizeJson` | ~3 | Nullable long JSON fragment. |
| `stringJson` | ~3 | Generic nullable string JSON fragment. |
| `status` | ~8 | Maps `SaveOperationFlow.Result` to status string. |
| `nullToBlank` | ~3 | Null-coalescing helper. |
| `className` | ~3 | Null-safe `Class.getName()`. |
| `operationSimpleName` | ~5 | Extracts simple class name from FQCN. |
| `SaveProofSectionBuilder` (nested) | ~120 | `SaveProofEvidence` rendering methods: `headerJson`, `menuJson`, `dialogJson`, `controlJson`, `writeJson`, `readbackJson`, `baselinePreservedJson`, `requiresNextEvidenceJson`, `doesNotClaimJson`, `blockerJson`, `proofRelativePath`. Receives a `SaveProofSnapshot` record. |

`EvidenceJsonWriter` also defines:

```java
record SaveProofSnapshot(
    // --- 17 volatile fields (captured atomically) ---
    boolean robotFileMenuOpened,
    boolean robotSaveItemClicked,
    boolean saveActionIdentityMatched,
    boolean chooserObserved,
    boolean approvedSelection,
    boolean ambiguousChooserDiscovery,
    boolean selectedFileVerified,
    boolean targetInsideProofRoot,
    boolean dialogShowing,
    String dialogClass,
    String normalizedSelectedFile,
    int pollCount,
    boolean projectReadable,
    boolean markerPresent,
    String blockerKind,
    String blockerObserved,
    String blockerRequired,
    // --- final fields from SaveProofEvidence ---
    String targetCanonicalPath,
    Path targetPath,
    String targetFileName,
    Path proofRoot,
    // --- derived values computed by json() before snapshot ---
    boolean fileExists,
    long fileSizeBytes,
    boolean fileNonempty,
    boolean fileHasExpectedExtension,
    boolean selectedFileMatchesExpected,
    boolean observedWrite,
    boolean proven,
    Path selectedPath,
    String scenario,
    String runId) { }
```

This record captures all 17 volatile fields in a single pass for JSON generation,
plus 4 final fields and 10 derived values pre-computed by
`SaveProofEvidence.json()`. The two-phase pattern is required because:

1. **File state** (`fileExists`, `fileSizeBytes`, `fileNonempty`) comes from
   `EvidenceFileOperations.regularFileState(targetPath)` — file I/O that must
   happen in `SaveProofEvidence.json()`, not in the zero-I/O `EvidenceJsonWriter`.
2. **Blocker inference** depends on `observedWrite` and `proven`, which are
   derived from file state and volatile fields combined.
3. **System properties** (`scenario`, `runId`) come from
   `configuredSaveProofScenario()` / `configuredSaveProofRunId()` on the facade.
4. `SaveProofEvidence.json()` calls `block()` to set volatile blocker fields
   (for future callers), then creates the snapshot including the blocker values.

The section builder operates on a fully-resolved, immutable snapshot — it never
reads volatile fields, performs I/O, or reads system properties.

`inferBlockerKind` and `inferBlockerObserved` stay on `SaveProofEvidence`
because they are called during phase 1 (before the snapshot exists) to set
volatile blocker fields via `block()`.

Note: `resultJson` currently calls `regularFileState(savedPath)` inline.
After extraction its signature changes to accept a pre-computed
`RegularFileState`, keeping `EvidenceJsonWriter` free of file I/O.

**Total extracted:** ~370 lines.

### EvidenceFileOperations

File-system guard methods move to `EvidenceFileOperations`:

| Method | Lines | Purpose |
| --- | --- | --- |
| `artifactPath` | ~6 | Resolves artifact name under evidence dir with path traversal guard. |
| `requireNonEmptyRegularFile` | ~5 | Throws `IOException` if file missing or empty. |
| `regularFileState` | ~15 | Reads `BasicFileAttributes` and returns `RegularFileState`. |
| `canonicalDirectory` | ~6 | Creates and resolves to real path. |
| `canonicalDirectoryUnderProofRoot` | ~15 | Walks path segments without following symlinks. |
| `ensureDirectoryWithoutFollowingSymlink` | ~15 | Creates or validates single directory segment. |
| `requirePathUnderProofRoot` | ~5 | Throws if path escapes root boundary. |
| `wroteFile` | ~5 | Extension-aware non-empty file check. |
| `hasExtension` | ~8 | Null-safe extension match. |
| `redactedSavedFilePath` / `redactedSavedPath` | ~15 | Redacts user home from artifact paths. Used by `regularFileState` error handler and by `EvidenceJsonWriter.savedFileJson`. |
| `RegularFileState` | ~7 | `record RegularFileState(boolean exists, long sizeBytes)` with `nonEmpty()` and `MISSING` sentinel. |

**Total extracted:** ~110 lines.

## File inventory

| File | Role | Approx lines |
| --- | --- | --- |
| `SaveOperationCompletionEvidence.java` | Facade: imports, constants, public API, config methods, `SaveProofEvidence` inner class (with two-phase `json()` compute + delegate, volatile fields, blocker inference), `InvocationTrigger` inner class. Delegates JSON rendering to `EvidenceJsonWriter` and file operations to `EvidenceFileOperations`. | ~500 |
| `EvidenceJsonWriter.java` | Package-private. All JSON string builders, `SaveProofSnapshot` record (31 fields), `SaveProofSectionBuilder` (11 rendering methods). Zero file I/O. | ~470 |
| `EvidenceFileOperations.java` | Package-private. Path traversal guards, symlink protection, file state inspection, path redaction, `RegularFileState` record. | ~140 |

All source files reside in
`core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/`.

## Delegate pattern

Both extracted classes are **stateless utility classes** with a private
constructor and static methods, matching the pattern of the original
`SaveOperationCompletionEvidence`:

```java
// EvidenceJsonWriter — all static, no state
final class EvidenceJsonWriter {
    private EvidenceJsonWriter() { }

    static String escapeJson(String value) { ... }
    static String resultJson(String operationClass, String extension,
                             SaveOperationFlow.Result result,
                             RegularFileState savedFileState) { ... }
    // ... remaining builders
}
```

```java
// EvidenceFileOperations — all static, no state
final class EvidenceFileOperations {
    private EvidenceFileOperations() { }

    static Path artifactPath(Path evidenceDir, String artifactName) { ... }
    static RegularFileState regularFileState(Path path) { ... }
    static String redactedSavedPath(Path savedPath) { ... }
    // ... remaining guards
}
```

`SaveOperationCompletionEvidence` retains a one-line `escapeJson` delegator
for backward-compatible call sites within the same class:

```java
static String escapeJson(String value) {
    return EvidenceJsonWriter.escapeJson(value);
}
```

### Call flow

```
SaveOperationCompletionEvidence.write(dir, op, ext, result)
  ├─ EvidenceFileOperations.artifactPath(dir, ARTIFACT)
  ├─ EvidenceFileOperations.regularFileState(savedPath)      ← file I/O (pre-computed for resultJson)
  ├─ Files.writeString(artifact, EvidenceJsonWriter.resultJson(op, ext, result, fileState))
  ├─ EvidenceFileOperations.requireNonEmptyRegularFile(artifact, ...)
  └─ writeDialogControlTarget(dir, op, ext, result)
       ├─ EvidenceFileOperations.artifactPath(dir, DIALOG_CONTROL_ARTIFACT)
       ├─ Files.writeString(artifact, EvidenceJsonWriter.dialogControlTargetJson(...))
       └─ EvidenceFileOperations.requireNonEmptyRegularFile(artifact, ...)

SaveProofEvidence.json()
  ├─ [phase 1: compute — stays in SaveProofEvidence]
  │   ├─ read all 17 volatile fields (single-pass snapshot)
  │   ├─ EvidenceFileOperations.regularFileState(targetPath)   ← file I/O
  │   ├─ compute: selectedFileMatchesExpected, observedWrite, proven
  │   ├─ inferBlockerKind(), inferBlockerObserved()             ← stay here (pure logic on locals)
  │   ├─ block(kind, observed, ...) if !proven                  ← volatile mutation
  │   ├─ configuredSaveProofScenario(), configuredSaveProofRunId()
  │   └─ new SaveProofSnapshot(all 31 fields)
  ├─ [phase 2: render — delegated to EvidenceJsonWriter]
  │   └─ EvidenceJsonWriter.SaveProofSectionBuilder.build(snapshot)
  │       ├─ headerJson, blockerJson, menuJson, dialogJson
  │       ├─ controlJson + writeJson (use proofRelativePath)
  │       ├─ readbackJson, baselinePreservedJson
  │       └─ requiresNextEvidenceJson, doesNotClaimJson
  └─ returns assembled JSON string

SaveProofEvidence.write(artifact)
  ├─ EvidenceFileOperations.requirePathUnderProofRoot(parent, proofRoot, ...)
  ├─ EvidenceFileOperations.canonicalDirectoryUnderProofRoot(parent, proofRoot, ...)
  ├─ Files.writeString(temp, json())
  ├─ Files.move(temp, canonical, ATOMIC_MOVE, REPLACE_EXISTING)
  └─ EvidenceFileOperations.requireNonEmptyRegularFile(canonical, ...)
```

### SaveProofEvidence snapshot pattern

`SaveProofEvidence.json()` uses a two-phase approach to satisfy the
zero-I/O constraint on `EvidenceJsonWriter`:

**Phase 1 — compute (stays in `SaveProofEvidence.json()`):**

```java
String json() throws IOException {
    // 1. Snapshot volatile fields in a single pass
    boolean robotFileMenuOpened = this.robotFileMenuOpened;
    boolean robotSaveItemClicked = this.robotSaveItemClicked;
    // ... all 17 volatile fields read once ...

    // 2. File I/O — reads file state from disk
    Path selectedPath = this.normalizedSelectedFile == null
        ? null : Path.of(this.normalizedSelectedFile).normalize();
    RegularFileState targetFileState =
        EvidenceFileOperations.regularFileState(this.targetPath);
    boolean fileExists = targetFileState.exists();
    long fileSizeBytes = targetFileState.sizeBytes();
    boolean fileNonempty = targetFileState.nonEmpty();
    boolean fileHasExpectedExtension = hasExpectedTargetExtension();

    // 3. Derive composite booleans
    boolean selectedFileMatchesExpected = ...;
    boolean observedWrite = fileExists && fileNonempty
        && fileHasExpectedExtension && targetInsideProofRoot;
    boolean proven = robotFileMenuOpened && robotSaveItemClicked && ...;

    // 4. Blocker inference + volatile mutation (stays here — needs volatile reads)
    if (!proven && this.blockerKind == null) {
        block(inferBlockerKind(observedWrite),
              inferBlockerObserved(observedWrite),
              "A complete Robot File menu Save activation...");
    }

    // 5. Read system properties
    String scenario = configuredSaveProofScenario();
    String runId = configuredSaveProofRunId();

    // 6. Build immutable snapshot with ALL 31 fields
    SaveProofSnapshot snapshot = new SaveProofSnapshot(
        robotFileMenuOpened, ..., this.blockerKind, this.blockerObserved,
        this.blockerRequired, ..., fileExists, fileSizeBytes,
        fileNonempty, ..., proven, selectedPath, scenario, runId);

    return EvidenceJsonWriter.SaveProofSectionBuilder.build(snapshot);
}
```

**Phase 2 — render (in `EvidenceJsonWriter.SaveProofSectionBuilder`):**

The section builder receives a fully-resolved `SaveProofSnapshot` and
assembles the JSON string. It performs zero I/O, reads no volatile fields,
and accesses no system properties. The `proofRelativePath` helper
(path-relative-to-proof-root computation) moves here since it is pure
path math using `snapshot.proofRoot()`.

## Visibility rules

### No visibility escalation

All extracted classes are **package-private** (`final class`, no `public`
modifier). No method or field visibility is widened from the original code.

| Symbol | Original visibility | After extraction |
| --- | --- | --- |
| `EvidenceJsonWriter` | N/A (new) | package-private |
| `EvidenceFileOperations` | N/A (new) | package-private |
| `SaveProofSnapshot` | N/A (new) | package-private (nested in `EvidenceJsonWriter`) |
| `SaveProofSectionBuilder` | N/A (new) | package-private (nested in `EvidenceJsonWriter`) |
| `RegularFileState` | private inner record | package-private (in `EvidenceFileOperations`) |
| All moved methods | private static | package-private static (same-package callers only) |

`RegularFileState` widens from `private` to package-private because both
`SaveOperationCompletionEvidence` and `EvidenceJsonWriter` reference it.
This is the **only** visibility change.

### Fields unchanged

No fields on `SaveOperationCompletionEvidence` or `SaveProofEvidence` change
visibility. All inner class fields remain as declared.

## Inner classes retained

Two inner classes **stay** on `SaveOperationCompletionEvidence` because
existing test code and `AbstractSaveOperation` reference them with qualified
names:

- **`SaveOperationCompletionEvidence.SaveProofEvidence`** — Referenced by
  20+ test assertions using `SaveOperationCompletionEvidence.SaveProofEvidence`.
  The inner class is thinned: its `json()` method now delegates section
  building to `EvidenceJsonWriter.SaveProofSectionBuilder`, and its `write()`
  method delegates file guards to `EvidenceFileOperations`. The volatile fields,
  `block()`, `recordReadback()`, `recordSelectedFile()`, `write()`,
  `proofContainsPath()`, `hasExpectedTargetExtension()`, `inferBlockerKind()`,
  and `inferBlockerObserved()` remain (~200 lines total).

- **`SaveOperationCompletionEvidence.InvocationTrigger`** — Referenced by
  `AbstractSaveOperation` as `SaveOperationCompletionEvidence.InvocationTrigger`.
  This 33-line class stays unchanged.

### Why not promote to top-level?

Java has no type aliases. Promoting `SaveProofEvidence` to a top-level class
would break 20+ qualified references in test files and production callers.
Modifying those callers is out of scope for this extraction.

## Validation commands

### Full module test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false -Dcheckstyle.skip test
```

All existing `core/ide` tests must pass with 0 failures, 0 errors.

### Compilation check

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am -Dcheckstyle.skip compile
```

### Line count verification

```bash
wc -l core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/SaveOperationCompletionEvidence.java
wc -l core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceJsonWriter.java
wc -l core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceFileOperations.java
```

- `SaveOperationCompletionEvidence.java` must be **under 600 lines** (target: ~500).
- `EvidenceJsonWriter.java` should be approximately 470 lines.
- `EvidenceFileOperations.java` should be approximately 140 lines.

### Verify new files exist

```bash
test -f core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceJsonWriter.java \
  && echo "OK" || echo "MISSING"
test -f core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceFileOperations.java \
  && echo "OK" || echo "MISSING"
```

## Compatibility rules

1. **No test file modifications.** All existing tests compile and pass without
   changes. No qualified inner-class references are broken.
2. **No API signature changes.** Every `static` method on
   `SaveOperationCompletionEvidence` retains its original signature.
3. **No constant changes.** All `static final` string constants remain on
   `SaveOperationCompletionEvidence` with identical values.
4. **No behavioral changes.** The JSON output format is byte-identical before
   and after extraction. The file-system guard behavior (path traversal
   rejection, symlink refusal, atomic write pattern) is identical.
5. **No new I/O in EvidenceJsonWriter.** `EvidenceJsonWriter` performs zero
   file I/O — it only builds JSON strings. All `Files.write*` and
   `Files.read*` calls remain in `SaveOperationCompletionEvidence` or
   `EvidenceFileOperations`.

## Security considerations

The extraction preserves all existing security properties:

| Property | Enforced by | Location after extraction |
| --- | --- | --- |
| Path traversal guard | `artifactPath()` rejects artifacts escaping evidence dir | `EvidenceFileOperations.artifactPath` |
| Symlink rejection | `ensureDirectoryWithoutFollowingSymlink()` + NOFOLLOW_LINKS | `EvidenceFileOperations.ensureDirectoryWithoutFollowingSymlink` |
| Proof root containment | `requirePathUnderProofRoot()` | `EvidenceFileOperations.requirePathUnderProofRoot` |
| Atomic write | temp-file + `ATOMIC_MOVE` in `SaveProofEvidence.write()` | `SaveOperationCompletionEvidence.SaveProofEvidence.write` (unchanged) |
| JSON injection guard | `escapeJson()` — single implementation | `EvidenceJsonWriter.escapeJson` (facade retains 1-line delegate) |
| Path redaction | `redactedSavedPath()` strips user home | `EvidenceFileOperations.redactedSavedPath` |
| No public API surface | All extracted classes are package-private | Enforced by `final class` without `public` |

## Examples

### Before extraction: monolithic SaveOperationCompletionEvidence

```java
// 1009 lines — JSON builders, file guards, and API mixed together
final class SaveOperationCompletionEvidence {
    // 17 constants
    // 8 public/package-private API methods
    // 20 private JSON builder methods
    // 7 private file-system guard methods
    // 1 private record (RegularFileState)
    // SaveProofEvidence inner class (312 lines)
    //   - 17 volatile fields + 5 final fields
    //   - block(), recordReadback(), recordSelectedFile(), write()
    //   - json() + 13 section builder methods
    //   - 2 blocker inference methods
    // InvocationTrigger inner class (33 lines)
}
```

### After extraction: focused facade with delegates

```java
// ~500 lines — API surface, config methods, and inner classes
final class SaveOperationCompletionEvidence {
    // 17 constants (unchanged)
    // 8 public/package-private API methods (bodies delegate to extracted classes)
    // 4 config/utility methods (configuredSaveProofArtifact, etc.)
    // 1-line escapeJson delegator
    // SaveProofEvidence inner class (~200 lines)
    //   - 17 volatile fields + 5 final fields (unchanged)
    //   - block(), recordReadback(), recordSelectedFile(), write() (unchanged)
    //   - proofContainsPath(), hasExpectedTargetExtension() (unchanged)
    //   - inferBlockerKind(), inferBlockerObserved() (stay — used in phase 1)
    //   - json() phase 1: reads file state, computes derived values, calls block()
    //   - json() phase 2: creates SaveProofSnapshot, delegates to SectionBuilder
    // InvocationTrigger inner class (33 lines, unchanged)
}
```

```java
// ~470 lines — pure JSON builders, zero I/O
final class EvidenceJsonWriter {
    // escapeJson, resultJson(pre-computed RegularFileState), dialogControlTargetJson, ...
    // SaveProofSnapshot record (31 fields: 17 volatile + 4 final + 10 derived)
    // SaveProofSectionBuilder with 11 rendering methods
}
```

```java
// ~140 lines — file-system guards, state inspection, and path redaction
final class EvidenceFileOperations {
    // artifactPath, requireNonEmptyRegularFile, regularFileState, ...
    // canonicalDirectory, canonicalDirectoryUnderProofRoot, ...
    // redactedSavedFilePath, redactedSavedPath
    // RegularFileState record
}
```
