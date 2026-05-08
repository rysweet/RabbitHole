# Run the Save Menu Dialog Write Proof

Use this guide to run the bounded Alice desktop Save proof that activates the real Save menu item, approves the Swing Save chooser, and writes a non-empty `.a3p` file.

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

Review `desktop-save-dialog-discovery-target.json` for owner/root/selection-target facts before the chooser appears. Review the canonical proof artifact, `stageide-save-menu-doclick-write-proof.json`, under `core/ide/target/stageide-save-menu-doclick-write-proof-test/<run-id>/doclick-to-written-file/evidence/` for these final write facts:

| Field | Expected value |
| --- | --- |
| `status` | `proven` |
| `dialogType` | `Swing JFileChooser` |
| `wroteFile` | `true` |
| `selected_file.normalized_selected_file` | Temp-relative `.a3p` path, for example `projects/doclick-save-proof.a3p` |
| `written_artifact.file_extension` | `a3p` |
| `written_artifact.target_inside_proof_root` | `true` |
| `doesNotClaim` | Includes lesson completion, rendering, grading, broad UI automation, and native dialog exclusions. |

Use `stageide-save-menu-doclick-write-proof.json` as the source for the full menu activation, chooser approval, selected path, and project-file write claim. `SaveOperationCompletionEvidence` records Save completion fields such as redacted/relative `saved_file`, `saved_file_exists`, `saved_file_size_bytes`, and bounded write facts, but it does not by itself prove Save menu activation. Do not treat either artifact as proof of any Save path other than Save menu activation, Swing chooser approval, and project-file write.
