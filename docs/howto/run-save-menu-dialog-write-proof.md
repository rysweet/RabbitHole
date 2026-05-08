# Run the Save Menu Dialog Write/Readback Proof

Use this guide to run the outside-in QA scenario that targets the bounded Robot Save proof. The proof selects File -> Save with AWT Robot events, controls the Swing chooser, writes a `.a3p`, reads it back, and verifies `robotSaveMenuRoundTripMarker`.

This is not a full desktop Save completion guide. It proves only the Robot File-menu Save -> controlled chooser -> written `.a3p` -> readback marker seam.

## Prerequisites

Run commands from the repository root and initialize the grammar submodule:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Use the saved memory option:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Provide a non-headless AWT display. On Linux CI or a headless workstation, use `xvfb-run -a`.

## Run the focused proof target

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest \
  test
```

## Run through the QA scenario wrapper

Use the outside-in scenario wrapper when review needs standard QA evidence:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-save-menu-dialog-write-proof \
  --evidence-dir qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof
```

The scenario runs the checked-in Maven argv directly. If the host is headless, run the wrapper itself inside Xvfb:

```bash
xvfb-run -a bash -lc 'NODE_OPTIONS=--max-old-space-size=32768 ALICE_QA_RUN_GATED_SMOKES=1 qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-save-menu-dialog-write-proof --evidence-dir qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof'
```

## Read the result

The proof writes `robot-save-menu-dialog-write-readback-proof.json` below `core/ide/target/save-menu-proofs/`. Treat the JSON artifact as the source of truth; Maven success alone is not a Save completion claim.

A proven result means Robot opened File, clicked the production Save item by action identity, controlled exactly one Swing `JFileChooser`, approved a proof-root `.a3p` target, wrote a non-empty file, read it with `IoUtilities.readProject(...)`, and found `robotSaveMenuRoundTripMarker`.

A blocked result names the exact missing Robot/Swing precondition. Use blocked evidence only as blocker evidence; it does not prove Robot Save activation, chooser approval, project file write, project readback, marker verification, native dialog coverage, Save As, all Save variants, broad UI automation, or full desktop Save completion.

Do not accept `StageIdeSaveMenuDoClickToWriteProofTest` output or a `save-menu-dialog-write-proof.json` artifact as evidence for this QA scenario. Those names describe the older doClick/write seam, not the Robot menu-to-readback seam.

For the complete artifact contract and review checklist, see [Robot Save Menu Dialog Write/Readback Proof](../reference/robot-save-menu-dialog-write-readback-proof.md).
