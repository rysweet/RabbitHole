# Validate SaveOperationCompletionEvidence Extraction

Use this guide to verify the extraction of JSON builders and file-system
guards from `SaveOperationCompletionEvidence` into `EvidenceJsonWriter` and
`EvidenceFileOperations`.

For the full contract, see the [SaveOperationCompletionEvidence Extraction
reference](../reference/save-operation-completion-evidence-extraction.md).

## When to use this guide

Use this guide when:

- Reviewing changes that extract methods from `SaveOperationCompletionEvidence`
- Modifying `EvidenceJsonWriter` or `EvidenceFileOperations`
- Adding new JSON evidence artifacts
- Changing file-system guard behavior
- Verifying that inner-class qualified references remain valid

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Verify compilation

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am -Dcheckstyle.skip compile
```

All three files must compile without errors:

- `SaveOperationCompletionEvidence.java`
- `EvidenceJsonWriter.java`
- `EvidenceFileOperations.java`

## Step 2: Verify new files exist

```bash
test -f core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceJsonWriter.java \
  && echo "EvidenceJsonWriter: OK" || echo "EvidenceJsonWriter: MISSING"
test -f core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceFileOperations.java \
  && echo "EvidenceFileOperations: OK" || echo "EvidenceFileOperations: MISSING"
```

## Step 3: Verify line counts

```bash
wc -l core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/SaveOperationCompletionEvidence.java
wc -l core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceJsonWriter.java
wc -l core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceFileOperations.java
```

Expected results:

| File | Target | Threshold |
| --- | --- | --- |
| `SaveOperationCompletionEvidence.java` | ~500 lines | Must be under 600 |
| `EvidenceJsonWriter.java` | ~470 lines | Informational |
| `EvidenceFileOperations.java` | ~140 lines | Informational |

## Step 4: Verify no visibility escalation

```bash
grep -c 'public class\|public static\|public final class' \
  core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceJsonWriter.java \
  core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceFileOperations.java
```

Expected: `0` matches in both files. All extracted classes are package-private.

## Step 5: Verify inner class references compile

```bash
grep -rn 'SaveOperationCompletionEvidence\.SaveProofEvidence\|SaveOperationCompletionEvidence\.InvocationTrigger' \
  core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/ \
  core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/AbstractSaveOperation.java \
  2>/dev/null | head -30
```

All qualified references must still resolve. No test files should be modified
by this extraction.

## Step 6: Verify EvidenceJsonWriter has no file I/O

```bash
grep -n 'Files\.\|FileOutputStream\|FileWriter\|FileChannel' \
  core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/EvidenceJsonWriter.java
```

Expected: no matches. `EvidenceJsonWriter` must perform zero file I/O.

## Step 7: Run the full test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false -Dcheckstyle.skip test
```

All existing tests must pass with 0 failures, 0 errors.

## Step 8: Verify JSON output equivalence (manual)

If you need to verify that JSON output is byte-identical before and after
extraction, write a small test or use the existing evidence tests:

```bash
# Run the save proof evidence tests (if present)
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='*SaveOperationCompletionEvidence*,*SaveProof*' \
  -Dcheckstyle.skip test
```

## Troubleshooting

### Compilation error: cannot find symbol RegularFileState

`RegularFileState` moved from a `private` inner record on
`SaveOperationCompletionEvidence` to a package-private record on
`EvidenceFileOperations`. Ensure all references use
`EvidenceFileOperations.RegularFileState`.

### Compilation error: cannot find symbol escapeJson

`SaveOperationCompletionEvidence.escapeJson` is retained as a 1-line
delegator to `EvidenceJsonWriter.escapeJson`. If you see this error, verify
the delegator exists.

### Test failure: qualified inner class not found

`SaveProofEvidence` and `InvocationTrigger` must remain as inner classes of
`SaveOperationCompletionEvidence`. If they were accidentally promoted to
top-level, revert and follow the reference document's inner class retention
rules.
