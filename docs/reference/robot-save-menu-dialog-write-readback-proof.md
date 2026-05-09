# Robot Save Menu Dialog Write/Readback Proof

This reference documents the focused Java proof shard behind the `save-menu-dialog-write-proof` outside-in scenario. A `status: "proven"` artifact from this shard means AWT Robot used the rendered Alice desktop File menu, followed the production Save dialog path, controlled the live Swing chooser, wrote a `.a3p`, read it back, and verified `robotSaveMenuRoundTripMarker`.

Until `RobotSaveMenuDialogWriteReadbackProofTest` records observations through `SaveOperationCompletionEvidence` and emits the canonical `schemaVersion` artifact, treat this page as the implementation target rather than a statement about legacy proof-local evidence.

For the canonical JSON field contract and validation rules, see [Save Proof Evidence](./save-proof-evidence.md). For the scenario-level QA contract, see [Save Menu Dialog Write/Readback Proof](./save-menu-dialog-write-proof.md).

## Purpose

`RobotSaveMenuDialogWriteReadbackProofTest` uses one continuous rendered path and keeps the historical seams as support only:

| Supporting seam | What it proves |
| --- | --- |
| `JMenuBarRobotClickSaveProofTest` | AWT Robot can open a rendered File menu and dispatch the Save menu item through Swing/Croquet action events. |
| `StageIdeSaveMenuDoClickToWriteProofTest` | The real Save menu item can reach a live Swing chooser and write a `.a3p` through `doClick()`. |
| `ProjectApplicationSaveProjectToTest` | `ProjectApplication.saveProjectTo(File)` writes readable projects and preserves direct save semantics. |

The Robot proof is the only one of these paths that can satisfy the Save proof scenario. The supporting seams remain useful regression baselines, but their artifacts cannot be aggregated into a completed rendered Save proof.

## Canonical proof shard

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java
```

The target shard writes exactly one canonical artifact:

```text
core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json
```

## Proof chain

A `status: "proven"` artifact means every step in this chain completed in order during the same run:

```text
rendered StageIDE/Croquet JMenuBar
  -> AWT Robot click on File menu
  -> File popup becomes visible
  -> AWT Robot click on Save menu item identified by SaveProjectOperation action identity
  -> SaveProjectOperation dispatches through AbstractSaveOperation.perform(UserActivity)
  -> DocumentFrame.showSaveFileDialog(...)
  -> FileDialogUtilities.showSaveFileDialog(...)
  -> exactly one live Swing JFileChooser observed
  -> test-owned .a3p target selected inside core/ide/target/save-menu-proofs/
  -> chooser approval completes
  -> ProjectApplication.saveProjectTo(File)
  -> non-empty .a3p written
  -> IoUtilities.readProject(savedFile)
  -> readback project contains robotSaveMenuRoundTripMarker
```

The proof fails closed. It cannot report `status: "proven"` unless Robot menu activation, Save item attribution, dialog observation, chooser control, file write, project readback, and marker verification are all true in the same run. All Robot/menu/dialog/write waits are bounded inside the Java proof; exhausted waits produce `status: "blocked"` evidence and a failing JUnit result instead of relying on shell timeout.

## Evidence status

The shard delegates final evidence semantics to `SaveOperationCompletionEvidence`.

| Status | Shard behavior |
| --- | --- |
| `proven` | The JUnit test passes only after the canonical artifact is written with all required fields true and internally consistent. |
| `blocked` | The JUnit test writes the canonical artifact with exactly one known blocker and then fails so the scenario cannot pass on partial evidence. |

Blocked output is the executable blocker for the missing step. It is not proof of Save completion.

## API boundaries

This proof is test-only. It does not add a public application API. The stable review interface is the machine-readable artifact documented in [Save Proof Evidence](./save-proof-evidence.md).

| Component | Role |
| --- | --- |
| `FileMenuModel` | Builds the production File menu used by the rendered menu bar. |
| `SaveProjectOperation` | Supplies the production Save action identity and dispatch target. |
| `AbstractSaveOperation.perform(UserActivity)` | Adapts Croquet action dispatch to active `StageIDE`, dialogs, wait cursor, and save callback. |
| `FileDialogUtilities.showSaveFileDialog(...)` | Shows the Swing Save chooser boundary controlled by the proof. |
| `ProjectApplication.saveProjectTo(File)` | Writes the selected `.a3p` project. |
| `IoUtilities.readProject(File)` | Reads the saved `.a3p` back as an Alice project. |
| Alice project/domain APIs | Verify that the readback project contains `robotSaveMenuRoundTripMarker`. |

Do not implement this proof by calling `SaveOperationFlow`, `ProjectApplication.saveProjectTo(File)`, or a direct dialog bypass. Those paths belong to lower-level characterization and do not exercise the rendered menu/dialog/control chain.

## Configuration

Run from the repository root. Initialize the grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Use the saved QA memory option:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Run under a usable desktop display:

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest \
  -Dorg.alice.eatme.saveProof.scenario=alice-desktop-save-menu-dialog-write-proof \
  -Dorg.alice.eatme.saveProof.runId=save-proof-$(date -u +%Y%m%dT%H%M%SZ)-manual \
  -Dorg.alice.eatme.saveProof.evidencePath=core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json \
  test
```

The outside-in runner supplies the same proof properties automatically and validates the artifact after Maven exits. Prefer the scenario wrapper for review evidence:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-save-menu-dialog-write-proof
```

## Non-claims

Even when `status` is `proven`, this proof does not claim:

1. Save As, overwrite prompt, cancellation, retry, unwritable-file, repeated-save, or every Save variant.
2. Native `java.awt.FileDialog` coverage.
3. Physical user click evidence.
4. Visible rendering correctness beyond the observed controls needed for this path.
5. Lesson completion, grading, or learner workflow correctness.
6. Broad Alice desktop automation coverage.
