# Run the Save Menu Dialog Write Proof

Use this guide to run the bounded Alice desktop Save proof that activates the real Save menu item, completes Swing Save chooser approval, and writes a non-empty `.a3p` file.

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

A passing proof means the test activated the production Save menu item with `doClick()`, controlled exactly one expected live Swing `JFileChooser`, completed approval on the EDT after verifying the normalized temp-directory `.a3p` target, and observed a non-empty `.a3p` file.

An unproven result is not partial success. Preexisting target files, multiple live `JFileChooser` instances, chooser timeouts, and path mismatches produce `status: not_proven` and must leave approval and write success fields false.

If the proof reports `status: unsupported`, the shard did not exercise the dialog path because the environment is missing the required desktop precondition:

```text
No available non-headless AWT display
```

Run the same command under Xvfb or another usable display to exercise the Save dialog/control/write path.

## Run through the QA scenario wrapper

Use the outside-in scenario wrapper when review needs standard QA evidence in addition to the focused Maven proof output:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-save-menu-dialog-write-proof \
  --evidence-dir qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof
```

The scenario is a gated command smoke. Without `ALICE_QA_RUN_GATED_SMOKES=1`, the runner records a gated-not-run result instead of executing the proof. The scenario does not expand the evidence scope beyond Save menu activation, Swing chooser approval, and a non-empty `.a3p` write.

The scenario runs the checked-in Maven argv directly. It depends on an ambient usable display and inherited environment such as `NODE_OPTIONS`; it does not add `xvfb-run` or set memory options for you. If the host is headless, run the wrapper itself inside Xvfb.

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
| `claim` | Present only when `status` is `proven`; unsupported or not-proven runs use `reporting_summary` instead. |
| `trigger.menu_item_doclick` | `true`; incomplete or shortcut artifacts keep this false and cannot claim menu activation. |
| `observed_dialog.approved_selection` | `true` only after EDT approval completes; scheduled approval is not enough. |
| `observed_dialog.ambiguous_chooser_discovery` | `false`; multiple live `JFileChooser` instances are a blocker. |
| `selected_file.normalized_selected_file` | Temp-relative `.a3p` path, for example `projects/doclick-save-proof.a3p` |
| `written_artifact.target_file` | Temp-relative `.a3p` path, not an absolute machine path. |
| `written_artifact.file_extension` | `a3p` |
| `written_artifact.target_inside_proof_root` | `true` |
| `doesNotClaim` | Includes lesson completion, rendering, grading, physical user click, broad UI automation, and native dialog exclusions. |

Use `stageide-save-menu-doclick-write-proof.json` as the source for the full menu activation, completed chooser approval, selected path, and project-file write claim only when its status is `proven` and `trigger.menu_item_doclick` is `true`. Stored path evidence must be proof-root-relative or redacted, not absolute. `SaveOperationCompletionEvidence` records Save completion fields such as redacted/relative `saved_file`, `saved_file_exists`, `saved_file_size_bytes`, and bounded write facts, but it does not by itself prove Save menu activation. Do not treat either artifact as proof of any Save path other than Save menu activation, Swing chooser approval, and project-file write.

When the display precondition is the review outcome, the Maven proof writes an unsupported-result artifact under its target evidence directory. If a PR cannot provide a display-backed proof and needs persistent review evidence, copy that generated artifact to:

```text
qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof-blocker.json
```

That JSON must keep `status: "unsupported"`, `reason: "No available non-headless AWT display"`, structured `blocker.observed` and `blocker.required` fields, `requiresNextEvidence`, and `wroteFile: false`. It is not a partial proof and does not claim chooser approval, file writing, native dialog coverage, full UI automation, visible rendering, grading, physical user clicks, first-lesson completion, or full Save completion.
