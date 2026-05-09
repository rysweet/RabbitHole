# Save Menu Dialog Write/Readback Proof

This reference documents the `save-menu-dialog-write-proof` outside-in QA scenario. The scenario attempts one bounded rendered desktop Save proof path through `RobotSaveMenuDialogWriteReadbackProofTest` and validates the artifact emitted by `SaveOperationCompletionEvidence`.

## Scope

A `status: "proven"` artifact from this scenario proves one path:

```text
rendered File menu
  -> Robot click on production Save item
  -> production Swing Save dialog
  -> controlled JFileChooser selection and approval
  -> .a3p file write
  -> IoUtilities.readProject(savedFile)
  -> readback project contains robotSaveMenuRoundTripMarker
```

The path is intentionally bounded. A proven artifact under this contract shows that the rendered File -> Save desktop path can complete a write/readback cycle in one run. It does not prove Save As, overwrite prompts, cancellation, retry, native file dialogs, every Save variant, lesson completion, grading, or broad desktop automation.

## Scenario contract

The workflow value is:

```text
save-menu-dialog-write-proof
```

The scenario ID is:

```text
alice-desktop-save-menu-dialog-write-proof
```

The automation mode is `gated-command-smoke`. Lightweight validation checks the scenario contract. With `ALICE_QA_RUN_GATED_SMOKES=1`, the runner executes the focused Maven proof and validates the emitted JSON evidence:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-save-menu-dialog-write-proof
```

Run the scenario under `xvfb-run -a` or another usable non-headless desktop session when the host is headless.

## No-timeout rule

This target workflow does not use a workflow-level timeout.

| Location | Contract |
| --- | --- |
| Scenario YAML | `automation.timeoutSeconds` is absent and invalid for `save-menu-dialog-write-proof`. |
| Runner | `run-scenario.sh` executes the checked-in Maven argv directly and does not wrap it with shell `timeout`. |
| Contract tests | Save proof QA tests reject scenario timeout wiring and timeout command construction for this workflow. |
| Java proof | Robot/menu/dialog/write waits are bounded inside the test and become `status: "blocked"` evidence rather than an unbounded hang. |

Other Alice desktop scenarios may still use their own timeout configuration. The no-timeout rule is workflow-specific to the Save proof path.

## Configuration

| Setting | Required value |
| --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` for the QA shell. |
| `ALICE_QA_RUN_GATED_SMOKES` | `1` when executable Maven proof evidence is required. |
| Display | A non-headless AWT display, commonly `xvfb-run -a` on Linux CI. |
| Submodule | `tweedle-lang/Grammar` must exist before broad Maven validation. |

The runner passes the scenario ID, a unique safe run ID, and the canonical evidence path to Maven as JVM properties. The proof must echo those values in the artifact. A mismatch is rejected as stale or tampered evidence.

## Evidence contract

The proof-owned JSON artifact is:

```text
core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json
```

The artifact must use:

```text
eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1
```

A passing scenario requires `status: "proven"` and all required menu, dialog, control, write, readback, and marker fields to be true and internally consistent. A missing artifact, stale artifact, blocked artifact, partial artifact, output-file mismatch, run mismatch, marker mismatch, or unknown blocker kind fails the scenario.

For the complete machine-readable field contract, see [Save Proof Evidence](./save-proof-evidence.md).

## Executable blocker behavior

When the rendered path cannot complete safely, the proof writes the same canonical artifact with `status: "blocked"` and exactly one known `blocker.kind`. The runner still fails the scenario. That failing artifact is the executable blocker for the missing step.

Blocked status is also how the Java proof handles bounded wait exhaustion after shell timeout wiring is removed: no menu, missing dialog, ambiguous chooser, failed chooser control, missing write, readback failure, and marker absence all map to named blockers.

Blocked evidence is not Save completion evidence. Do not combine a blocked artifact with older menu-only, `doClick()`, direct save, or write/readback artifacts to claim this scenario passed.

## Historical baselines

The following proofs remain useful support, but they do not satisfy this scenario by aggregation:

| Baseline | Useful for | Not enough because |
| --- | --- | --- |
| `JMenuBarRobotClickSaveProofTest` | Robot can open the File menu and dispatch the Save menu item. | It does not prove dialog control, file write, readback, or marker verification. |
| `StageIdeSaveMenuDoClickToWriteProofTest` | The Save menu item can reach a dialog/write seam through `doClick()`. | It does not prove Robot activation of the production rendered menu item in the same path. |
| `ProjectApplicationSaveProjectToTest` | Direct project save behavior and readable `.a3p` output. | It bypasses the rendered menu and dialog control path. |

The old `save-menu-dialog-write-proof.json` artifact name is historical only. It does not satisfy this scenario contract.

## Example review

```text
qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof/
  alice-desktop-save-menu-dialog-write-proof/
    <timestamp>/
      status.txt
      command.log

core/ide/target/save-menu-proofs/
  robot-save-menu-dialog-write-readback-proof.json
```

Reviewers first check `status.txt` and `command.log` to confirm that `RobotSaveMenuDialogWriteReadbackProofTest` ran through the Save proof workflow. They then treat the canonical JSON artifact as the source of truth for `status: "proven"` or the exact executable blocker.

For run steps, see [Run the Save Menu Dialog Write/Readback Proof](../howto/run-save-menu-dialog-write-proof.md).
