# Save Menu Dialog Write/Readback Proof

This reference documents the `save-menu-dialog-write-proof` outside-in QA scenario. The scenario now targets the bounded Robot proof shard, `RobotSaveMenuDialogWriteReadbackProofTest`.

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

The scenario is a `gated-command-smoke`. With `ALICE_QA_RUN_GATED_SMOKES=1`, the runner executes the focused Maven proof:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest \
  test
```

Run the proof under `xvfb-run -a` or another usable non-headless desktop session when the host is headless.

## Evidence

The QA wrapper records `status.txt` and `command.log`. The proof-owned JSON artifact is:

```text
robot-save-menu-dialog-write-readback-proof.json
```

A proven artifact must show Robot File-menu Save activation, production Save action attribution, controlled chooser approval, a non-empty proof-root `.a3p` write, readable project readback, and `robotSaveMenuRoundTripMarker`. A blocked artifact names the exact missing Robot/Swing precondition and is not partial Save success.

For the full artifact schema, blocker contract, examples, and review checklist, see [Robot Save Menu Dialog Write/Readback Proof](./robot-save-menu-dialog-write-readback-proof.md).
