# Save Menu Dialog Write/Readback Proof

This reference documents the `save-menu-dialog-write-proof` outside-in QA scenario. The scenario targets the bounded Robot proof shard, `RobotSaveMenuDialogWriteReadbackProofTest`, while preserving the existing workflow name for QA compatibility.

## Scope

The scenario proves only this seam:

```text
Robot File-menu Save
  -> controlled Swing JFileChooser
  -> written .a3p
  -> IoUtilities.readProject(savedFile)
  -> readback project contains robotSaveMenuRoundTripMarker
```

It does not claim full desktop Save completion, Save As coverage, native dialog coverage, lesson completion, grading, broad UI automation, or all Save variants.

## Scenario contract

The workflow value remains:

```text
save-menu-dialog-write-proof
```

The scenario ID remains:

```text
alice-desktop-save-menu-dialog-write-proof
```

The automation mode is `gated-command-smoke`. Lightweight QA validation records checklist/status evidence without running the Maven proof. With `ALICE_QA_RUN_GATED_SMOKES=1`, the runner executes the focused Maven proof:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest \
  test
```

Run the proof under `xvfb-run -a` or another usable non-headless desktop session when the host is headless.

## Configuration

| Setting | Required value |
| --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` for the QA shell. |
| `ALICE_QA_RUN_GATED_SMOKES` | `1` when executable Maven proof evidence is required. |
| Display | A non-headless AWT display, commonly `xvfb-run -a` on Linux CI. |
| Submodule | `tweedle-lang/Grammar` must exist before broad Maven validation. |

The QA runner accepts only the checked-in Maven argv for this workflow. The scenario does not allow dynamic test-class selection, extra Maven flags, shell passthrough, or arbitrary artifact names.

## Evidence contract

The QA wrapper records `status.txt` and `command.log`. The proof-owned JSON artifact is:

```text
robot-save-menu-dialog-write-readback-proof.json
```

A proven artifact must show Robot File-menu Save activation, production Save action attribution, controlled chooser approval, a non-empty proof-root `.a3p` write, readable project readback, and `robotSaveMenuRoundTripMarker`. A blocked artifact names the exact missing Robot/Swing precondition and is not partial Save success.

The old `StageIdeSaveMenuDoClickToWriteProofTest` evidence and any `save-menu-dialog-write-proof.json` artifact are historical baselines only. They do not satisfy this scenario contract.

## Example review

```text
qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof/
  status.txt
  command.log

core/ide/target/save-menu-proofs/
  robot-save-menu-dialog-write-readback-proof.json
```

Reviewers first check `status.txt` and `command.log` to confirm that `RobotSaveMenuDialogWriteReadbackProofTest` ran. They then treat `robot-save-menu-dialog-write-readback-proof.json` as the source of truth for `status=proven` or the named Robot/Swing blocker.

For the full artifact schema, blocker contract, examples, and review checklist, see [Robot Save Menu Dialog Write/Readback Proof](./robot-save-menu-dialog-write-readback-proof.md).
