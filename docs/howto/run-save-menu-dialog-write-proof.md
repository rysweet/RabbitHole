# Run the Save Menu Dialog Written-Project Proof

Use this guide as the target runbook for the strengthened Alice desktop Save proof. The feature to build must activate the real Save menu item, complete Swing Save chooser approval, write a `.a3p` file, read the file back as an Alice project, and verify the expected marker in the readback payload.

Implementation status: this document is a retcon spec for the strengthened proof, not evidence that the current shard already performs readback and marker verification. Until the test and artifact are updated to this contract, existing passing runs must be treated as bounded write proof only.

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

## Run the focused proof target after implementation

```bash
xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest \
  test
```

The strengthened proof is intentionally one path only. It does not run the full desktop QA lane and does not grade, render, complete a lesson, or prove every Save variant.

## Read the result

Under the strengthened contract, a passing proof means the test activated the production Save menu item with `doClick()`, controlled exactly one expected live Swing `JFileChooser`, completed approval on the EDT after verifying the normalized temp-directory `.a3p` target, observed a non-empty `.a3p` file, read the saved file back with `IoUtilities.readProject(targetFile)`, and verified that the readback project contains `saveMenuDoClickRoundTripMarker`.

An unproven result must not be treated as partial success. Preexisting target files, multiple live `JFileChooser` instances, chooser timeouts, path mismatches, readback failures, and missing markers must produce `status: not_proven` and must leave approval, write, readback, and marker success fields false.

Once strengthened, if the proof reports `status: unsupported`, the shard did not exercise the dialog path because the environment is missing the required desktop precondition:

```text
No available non-headless AWT display
```

Run the same command under Xvfb or another usable display to exercise the intended Save dialog/control/write/readback/marker path.

## Run through the QA scenario wrapper

Use the outside-in scenario wrapper when review needs standard QA evidence in addition to the focused Maven proof output:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-save-menu-dialog-write-proof \
  --evidence-dir qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof
```

The scenario is a gated command smoke. Without `ALICE_QA_RUN_GATED_SMOKES=1`, the runner records a gated-not-run result instead of executing the proof. Once strengthened, the scenario must not expand the evidence scope beyond Save menu activation, Swing chooser approval, project-file write, readable-project readback, and marker verification.

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

Review `desktop-save-dialog-discovery-target.json` for owner/root/selection-target facts before the chooser appears. The strengthened implementation must write the canonical proof artifact, `stageide-save-menu-doclick-write-proof.json`, under `core/ide/target/stageide-save-menu-doclick-write-proof-test/<run-id>/doclick-to-written-file/evidence/` with these final written-project facts:

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
| `readback.project_readable` | `true` |
| `readback.expected_marker` | `saveMenuDoClickRoundTripMarker` |
| `readback.marker_present` | `true` |
| `doesNotClaim` | Includes full desktop Save completion, lesson completion, rendering, grading, physical user click, broad UI automation, native dialog coverage, and all Save variants. |

After the strengthened proof lands, use `stageide-save-menu-doclick-write-proof.json` as the source for the full menu activation, completed chooser approval, selected path, project-file write, readable-project, and marker claim only when its status is `proven`, `trigger.menu_item_doclick` is `true`, `readback.project_readable` is `true`, and `readback.marker_present` is `true`. Stored path evidence must be proof-root-relative or redacted, not absolute. `SaveOperationCompletionEvidence` records Save completion fields such as redacted/relative `saved_file`, `saved_file_exists`, `saved_file_size_bytes`, and bounded write facts, but it does not by itself prove Save menu activation, readback, or marker content. Do not treat either artifact as proof of any Save path other than Save menu activation, Swing chooser approval, project-file write, readable-project readback, and marker verification.

When the display precondition is the review outcome, the strengthened Maven proof must write an unsupported-result artifact under its target evidence directory. If a PR cannot provide a display-backed proof and needs persistent review evidence, copy that generated artifact to:

```text
qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof-blocker.json
```

That JSON must keep `status: "unsupported"`, `reason: "No available non-headless AWT display"`, structured `blocker.observed` and `blocker.required` fields, `requiresNextEvidence`, `wroteFile: false`, `readback.project_readable: false`, and `readback.marker_present: false`. It is not a partial proof and does not claim chooser approval, file writing, readable-project readback, marker verification, native dialog coverage, full UI automation, visible rendering, grading, physical user clicks, first-lesson completion, or full desktop Save completion.
