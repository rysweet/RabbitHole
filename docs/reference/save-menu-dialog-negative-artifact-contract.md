# Save Menu Dialog Negative Artifact Contract

This reference documents the negative artifact contract for the
`save-menu-dialog-write-proof` QA lane. The contract proves that the existing
Save proof evidence validator fails closed when evidence is missing, malformed,
stale, future-dated, blocked, incomplete, or internally inconsistent.

The contract is intentionally separate from the positive Save write/readback
proof. It does not run Alice, does not open the desktop, does not invoke Maven,
does not add a scenario or workflow, and does not prove full desktop Save
completion.

Keep the claim bounded to fail-closed artifact validation. A passing negative
contract is not evidence for full Save behavior, Save As behavior, visible
rendering correctness, grading, lesson completion, learner assessment, broad UI
automation, or native dialog automation.

## Scope

The executable contract is:

```text
qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

It targets the existing runner validation seam:

```text
qa/outside-in/alice-desktop/runners/run-scenario.sh validate-save-proof-evidence
```

The seam validates the canonical artifact for:

```text
scenario: alice-desktop-save-menu-dialog-write-proof
workflow: save-menu-dialog-write-proof
artifact: robot-save-menu-dialog-write-readback-proof.json
```

The negative contract is accepted only when every bad artifact case exits
non-zero and prints an explicit Save proof artifact diagnostic.

## Contract cases

| Case | Artifact condition | Required validator result |
| --- | --- | --- |
| Missing artifact | Canonical artifact path does not exist. | Non-zero exit and a diagnostic naming the missing Save proof evidence artifact. |
| Wrong artifact name | Artifact path uses any basename other than `robot-save-menu-dialog-write-readback-proof.json`. | Non-zero exit and a diagnostic naming the canonical artifact filename. |
| Symlink artifact | Canonical artifact path is a symlink. | Non-zero exit and a diagnostic rejecting symlinked Save proof evidence. |
| Malformed artifact | File exists but is not valid JSON. | Non-zero exit and a diagnostic naming invalid Save proof evidence JSON. |
| Stale artifact | `generatedAtUtc` or artifact mtime predates the supplied command start time. | Non-zero exit and a diagnostic naming stale Save proof evidence or the freshness fields. |
| Future artifact | `generatedAtUtc` or artifact mtime exceeds the validator clock by more than the 300-second allowed skew. | Non-zero exit and a diagnostic naming future Save proof evidence or the freshness fields. |
| Identity mismatch | Artifact `scenario`, `workflow`, or `runId` differs from the validator arguments. | Non-zero exit and a diagnostic naming the mismatched identity field. |
| Missing required flags | Proven-looking artifact omits required menu, dialog, control, write, or readback fields. | Non-zero exit and a diagnostic naming missing required proven Save proof flags or objects. |
| Inconsistent proven artifact | `status: "proven"` conflicts with write/readback facts, output size, output path, marker, or filesystem state. | Non-zero exit and a diagnostic naming inconsistent proven Save proof evidence. |
| Blocked known kind | Artifact reports `status: "blocked"` with a known blocker such as `dialog_not_observed`. | Non-zero exit and a diagnostic naming non-proven blocked Save proof evidence. |
| Blocked unknown kind | Artifact reports `status: "blocked"` with a blocker outside the Save proof enum. | Non-zero exit and a diagnostic naming an unknown or unsupported blocker kind. |

The fixtures for stale, future-dated, partial, inconsistent, and blocked cases
live under:

```text
qa/outside-in/alice-desktop/tests/fixtures/save-proof-evidence/
```

The test generates temporary missing, malformed, and non-object artifacts at
runtime. Temporary files are created under the shared QA scratch root and
removed on exit.

## Validator API

The negative contract calls the validator directly:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh validate-save-proof-evidence \
  <artifact-path> \
  --scenario alice-desktop-save-menu-dialog-write-proof \
  --workflow save-menu-dialog-write-proof \
  --run-id contract-run-1 \
  --started-at-epoch <command-start-epoch>
```

### Arguments

All context options are required so the validator can bind the artifact to one
specific scenario run and reject stale evidence.

| Argument | Meaning |
| --- | --- |
| `<artifact-path>` | Path to the canonical `robot-save-menu-dialog-write-readback-proof.json` artifact. The basename must match the canonical artifact name. |
| `--scenario` | Expected scenario identity. For this lane, use `alice-desktop-save-menu-dialog-write-proof`. |
| `--workflow` | Expected workflow identity. For this lane, use `save-menu-dialog-write-proof`. |
| `--run-id` | Expected run token. It must be a non-empty safe token containing only letters, digits, `.`, `_`, and `-`. |
| `--started-at-epoch` | Unix epoch used to reject stale artifacts whose `generatedAtUtc` or mtime predates command start. The validator also rejects timestamps more than 300 seconds ahead of its clock. |

### Success and failure API

The validator prints `Save proof evidence proven` and exits zero only for a
fresh, canonical, internally consistent `status: "proven"` artifact. Every other
condition exits non-zero.

The negative contract relies on failure diagnostics being specific enough to
identify the rejected evidence class. A failure that is only generic, silent, or
success-shaped does not satisfy the contract.

## Required proven evidence checks

The validator accepts `status: "proven"` only when all proven evidence fields are
present and consistent:

1. Identity fields match the requested schema version, scenario, workflow, and run ID.
2. `claim` equals the bounded Save proof claim and `doesNotClaim` includes every required non-claim.
3. `blocker` is `null`.
4. `menu.fileMenuOpened`, `menu.saveMenuItemInvoked`, and `menu.saveActionIdentityMatched` are `true`.
5. `dialog.saveDialogObserved` and `dialog.dialogShowing` are `true`, `dialog.dialogType` is `Swing JFileChooser`, and `dialog.ambiguousChooserDiscovery` is `false`.
6. `control.selectedPathSet`, `control.approvedSelection`, `control.selectedPathMatchesExpected`, and `control.targetInsideProofRoot` are `true`.
7. `write.fileWritten`, `write.fileNonempty`, and `write.fileHasExpectedExtension` are `true`.
8. `write.outputPath` resolves under the artifact directory, exists, and has the recorded `write.outputSizeBytes`.
9. `readback.projectReadable` is `true`, `readback.marker` is `robotSaveMenuRoundTripMarker`, and `readback.markerPresent` is `true`.
10. `generatedAtUtc` is an ISO timestamp, is fresh relative to `--started-at-epoch`, and is no more than 300 seconds ahead of the validator clock.

## Configuration

Run the contract from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

The contract is shell/Python validation only. It does not require Xvfb,
`ALICE_QA_RUN_GATED_SMOKES`, or a non-headless AWT display. Those settings are
required only when collecting positive rendered Save proof evidence.

## Amplihack CLI wrapper

The branch-installable QA wrapper exposes the same contract as:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> \
  amplihack alice-qa save-negative-contract
```

Run the command from the root, or a child directory, of the checkout being
reviewed. The installed wrapper locates that checkout and delegates to:

```text
qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

`amplihack alice-qa save-negative-contract` accepts no extra arguments. Extra
arguments are a usage error. Its success and failure meaning is identical to the
direct shell contract: success proves only that invalid Save proof artifacts
fail closed with explicit diagnostics.

## Review rules

Use the negative contract to guard the artifact validator, not to claim Save
success. A passing negative contract means bad Save proof artifacts are rejected
with explicit diagnostics. It does not mean the rendered File-menu Save path has
run, written a project, read it back, or completed the positive proof. It also
does not make claims about Save As, visible rendering correctness, grading,
lesson completion, learner assessment, broad UI automation, or native dialog
automation.

Reviewers should cite the positive
`robot-save-menu-dialog-write-readback-proof.json` artifact only when the
positive scenario reports `status: "proven"` and passes fail-closed validation.
Use this negative contract as evidence that missing, wrong-name, symlinked,
stale, future-dated, identity-mismatched, blocked, partial, and inconsistent
artifacts cannot be accepted as that proof.

## Example diagnostics

Expected diagnostics include stable phrases such as:

```text
missing Save proof evidence artifact robot-save-menu-dialog-write-readback-proof.json
invalid Save proof evidence JSON
stale Save proof evidence: generatedAtUtc/mtime predates command start
future Save proof evidence: generatedAtUtc/mtime exceeds validator clock skew
Save proof evidence scenario mismatch
Save proof evidence workflow mismatch
Save proof evidence runId mismatch
Save proof evidence artifact must not be a symlink
Save proof evidence path must use canonical filename robot-save-menu-dialog-write-readback-proof.json
missing required proven Save proof flag(s)
inconsistent proven Save proof evidence
blocked Save proof evidence is non-proven status
unknown unsupported blocker kind
```

Exact paths, run IDs, sizes, and timestamps are run-specific.
