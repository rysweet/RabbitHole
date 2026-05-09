# Run the Save Menu Dialog Write/Readback Proof

Use this guide to run the target canonical outside-in Save proof for the Alice desktop Save menu path. A `status: "proven"` artifact exercises one rendered path from File -> Save through the live Save dialog, controlled chooser interaction, `.a3p` write, project readback, and marker verification.

When the artifact reports `status: "proven"`, it proves only that single rendered Save path. It does not prove Save As, overwrite prompts, cancellation, retry, native file dialogs, every Save variant, lesson completion, grading, or broad desktop automation.

## Prerequisites

Run commands from the repository root and initialize the grammar submodule:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Use the saved QA memory option:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Provide a non-headless AWT display. On Linux CI or a headless workstation, run the scenario inside Xvfb. If you need a quick host smoke check before running Maven, `xvfb-run -a true` only proves that Xvfb can start; it does not exercise Alice, Robot, the Save dialog, or evidence validation.

## Run through the QA scenario wrapper

Run the scenario when review needs standard QA evidence and fail-closed validation:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-save-menu-dialog-write-proof \
  --evidence-dir qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof
```

If the host is headless, wrap the scenario command in Xvfb:

```bash
xvfb-run -a bash -lc 'NODE_OPTIONS=--max-old-space-size=32768 ALICE_QA_RUN_GATED_SMOKES=1 qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-save-menu-dialog-write-proof --evidence-dir qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof'
```

The target Save proof scenario intentionally has no workflow timeout. The YAML omits `automation.timeoutSeconds`, and the runner executes the Maven argv directly without shell `timeout`. Do not add `timeoutSeconds`, `timeout`, or timeout wrapper commands to this path. The Java proof itself must use bounded Robot/menu/dialog/write polling and emit `status: "blocked"` for the earliest exhausted wait instead of hanging.

## Run the focused proof target directly

Use the direct Maven command only when you are debugging the proof shard itself:

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

The direct command is for local debugging. Review evidence should come from the QA scenario wrapper after the target runner validation exists, because the wrapper validates missing, stale, partial, blocked, and internally inconsistent evidence before returning success.

## Read the result

The target proof writes one canonical artifact:

```text
core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json
```

Treat the JSON artifact as the source of truth. A successful scenario run means the artifact passed fail-closed validation and reported `status: "proven"` for the same `scenario` and `runId` that the runner passed to Maven.

A proven artifact means the same rendered desktop run completed all required observations:

1. AWT Robot opened the rendered File menu.
2. AWT Robot clicked the production Save item by action identity.
3. The Save operation showed the production Swing Save dialog path.
4. Exactly one expected `JFileChooser` was observed and controlled.
5. The selected proof-root `.a3p` target was approved.
6. The output file was written and its recorded size matched the filesystem.
7. `IoUtilities.readProject(...)` read the file back.
8. The readback project contained `robotSaveMenuRoundTripMarker`.

If any step is missing, the scenario fails and the artifact reports `status: "blocked"` with exactly one known `blocker.kind`. Use a blocked artifact as the executable blocker for the missing step; do not cite it as Save completion evidence.

## Review checklist

Before citing the result, confirm the canonical artifact:

1. Uses `schemaVersion: "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1"`.
2. Names `scenario: "alice-desktop-save-menu-dialog-write-proof"` and the runner `runId`.
3. Has `status: "proven"` and `blocker: null`.
4. Has every required `menu`, `dialog`, `control`, `write`, and `readback` flag set to the proven value.
5. Has `dialog.ambiguousChooserDiscovery: false`.
6. Records `write.outputPath` under the proof root with a `.a3p` extension.
7. Records `write.outputSizeBytes` equal to the file size on disk.
8. Records `readback.projectReadable: true` and `readback.markerPresent: true`.
9. Keeps `doesNotClaim` boundaries for Save variants and non-Save desktop behavior.

The complete artifact contract is documented in [Save Proof Evidence](../reference/save-proof-evidence.md). The scenario-level contract is documented in [Save Menu Dialog Write/Readback Proof](../reference/save-menu-dialog-write-proof.md).

## Validate fail-closed artifact rejection

Run the independent negative artifact contract beside this positive proof when
reviewing the Save proof lane:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

That contract proves only that bad Save proof artifacts are rejected with
explicit diagnostics. It does not run the rendered Save path and does not expand
the positive proof into full desktop Save completion. For details, see [Run the
Save Menu Dialog Negative Artifact Contract](./run-save-menu-dialog-negative-artifact-contract.md).
