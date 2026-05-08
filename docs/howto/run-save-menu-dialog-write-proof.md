# [PLANNED - Implementation Pending] Run the Save Menu Dialog Write Proof

Use this guide to run the planned bounded Alice desktop Save proof that activates the real Save menu item, approves the Swing Save chooser, and writes a non-empty `.a3p` file.

## Prerequisites

Run commands from the repository root. Initialize the grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Set the memory option for this Maven run:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Provide a non-headless AWT display. On Linux CI or a headless workstation, use `xvfb-run -a`.

## Run the focused proof target

```bash
xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest \
  test
```

The proof is intentionally one path only. It does not run the full desktop QA lane and does not grade, render, or complete a lesson.

## Read the result

A passing final implementation means the test activated the production Save menu item with `doClick()`, controlled exactly one expected live Swing `JFileChooser`, approved the normalized temp-directory `.a3p` target, and observed a non-empty `.a3p` file.

If the final implementation reports this blocker, the proof is executable but the environment is missing the required desktop precondition:

```text
No available non-headless AWT display
```

Run the same command under Xvfb or another usable display to exercise the Save dialog/control/write path.

## Collect evidence for review

Add a dialog-discovery evidence directory when reviewing the Save dialog boundary:

```bash
xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -Dorg.alice.eatme.saveDialogDiscoveryEvidenceDir=target/save-dialog-proof-evidence \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest \
  test
```

Review `desktop-save-dialog-discovery-target.json` for owner/root/selection-target facts before the chooser appears. Review the planned canonical proof artifact, `stageide-save-menu-doclick-write-proof.json`, under `core/ide/target/stageide-save-menu-doclick-write-proof-test/<run-id>/doclick-to-written-file/evidence/` for these final write facts:

| Field | Expected value |
| --- | --- |
| `status` | `proven` |
| `dialogType` | `Swing JFileChooser` |
| `wroteFile` | `true` |
| `savedFileExtension` | `a3p` |
| `doesNotClaim` | Includes lesson completion, rendering, grading, broad UI automation, and native dialog exclusions. |

Do not use `SaveOperationCompletionEvidence` as the source for `dialogType`, `wroteFile`, or `selectedFile`; that evidence records Save completion fields such as `saved_file_exists` and `saved_file_size_bytes`. Do not treat either artifact as proof of any Save path other than Save menu activation, Swing chooser approval, and project-file write.
