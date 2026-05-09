# Save Proof Evidence

This reference defines the canonical evidence artifact for the rendered Alice desktop Save proof. One artifact may prove exactly one path: rendered File menu -> production Save item -> rendered Swing Save dialog -> chooser control -> `.a3p` write -> `IoUtilities.readProject(...)` readback -> `robotSaveMenuRoundTripMarker` verification.

It does not prove Save As, overwrite prompts, cancellation, retry, native file dialogs, all Save variants, lesson completion, grading, or broad desktop automation. Older bounded/menu-only/write-readback artifacts remain supporting evidence only; they cannot be aggregated into Save completion.

## Artifact

The scenario is `alice-desktop-save-menu-dialog-write-proof`; the workflow is `save-menu-dialog-write-proof`; the artifact name is:

```text
robot-save-menu-dialog-write-readback-proof.json
```

The QA runner writes and validates that artifact inside the scenario run directory. Direct Maven debugging defaults to `core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json` unless an evidence path is supplied.

## Required top-level fields

| Field | Required value |
| --- | --- |
| `schemaVersion` | `eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1` |
| `scenario` | `alice-desktop-save-menu-dialog-write-proof` |
| `workflow` | `save-menu-dialog-write-proof` |
| `runId` | Runner-supplied safe token using only letters, digits, `.`, `_`, and `-` |
| `generatedAtUtc` | Fresh ISO-8601 UTC timestamp from the proof run |
| `status` | `proven` or `blocked` |
| `menu` | File menu and Save item attribution booleans |
| `dialog` | Rendered `Swing JFileChooser` observation |
| `control` | Selected path and chooser approval evidence |
| `write` | Output path and size evidence |
| `readback` | Project readback and marker evidence |
| `proofTarget` | `single rendered desktop Save path: menu, dialog, control, write, readback` |
| `claim` | Exact bounded claim text for proven evidence only |
| `reportingSummary` | Blocked-only summary that says the path was not proven |
| `blocker` | `null` for proven evidence; one blocker object for blocked evidence |
| `requiresNextEvidence` | Schema and reviewer guidance for collecting or qualifying the next artifact; not an incomplete-proof signal when `status` is `proven` |
| `doesNotClaim` | Explicit non-claims for Save variants and non-Save desktop behavior |

`claim` and `reportingSummary` are status-specific: proven artifacts include `claim`, blocked artifacts include `reportingSummary`, and neither status includes both.

`requiresNextEvidence` is present to keep reviewer guidance beside the machine evidence. In a proven artifact, it does not downgrade the result or mean more evidence is required for the rendered Save path. In a blocked artifact, it explains what must change before Save completion can be cited.

## Proven evidence

Validation accepts `status: "proven"` only when all of these are true in one artifact:

| Object | Required true fields |
| --- | --- |
| `menu` | `fileMenuOpened`, `saveMenuItemInvoked`, `saveActionIdentityMatched` |
| `dialog` | `saveDialogObserved`, `dialogShowing`; `dialogType` must be `Swing JFileChooser`; `ambiguousChooserDiscovery` must be `false` |
| `control` | `selectedPathSet`, `approvedSelection`, `selectedPathMatchesExpected`, `targetInsideProofRoot` |
| `write` | `fileWritten`, `fileNonempty`, `fileHasExpectedExtension`; `outputSizeBytes` must match the file size on disk |
| `readback` | `projectReadable`, `markerPresent`; `marker` must be `robotSaveMenuRoundTripMarker` |

`blocker` must be `null`. The output file must exist, be fresh for the run, and match the recorded size. The bounded `claim` and required `doesNotClaim` entries must be present so the artifact cannot be used as broad Save, Save As, lesson, grading, rendering, or desktop automation evidence.

## Blocked evidence

Blocked evidence is executable blocker evidence, not partial success. The runner rejects it for a passing scenario run, but it is the precise artifact to attach when the full rendered path cannot complete.

Known `blocker.kind` values are:

```text
headless_awt
robot_unavailable
file_menu_not_showing
save_item_not_attributed
dialog_not_observed
ambiguous_chooser_discovery
chooser_control_failed
target_path_rejected
write_not_observed
readback_failed
marker_missing
```

The blocker object must include non-empty `kind`, `observed`, and `required` fields. Unknown blocker kinds fail validation.

## Fail-closed validation

`run-scenario.sh validate-save-proof-evidence` rejects missing, invalid, stale, future-dated, partial, blocked, unknown-blocker, or internally inconsistent artifacts. It checks scenario/workflow/runId, freshness, required proven flags, output file existence and size, readback marker fields, and blocker shape.

The Save proof scenario has no workflow-level timeout: `automation.timeoutSeconds` is invalid for `save-menu-dialog-write-proof`, and the runner does not wrap this Maven command with shell `timeout`.

## Negative artifact contract

The independent negative contract is:

```text
qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

It calls `run-scenario.sh validate-save-proof-evidence` directly and proves the
validator fails closed for missing, malformed, stale, incomplete, blocked,
unknown-blocker, and internally inconsistent artifacts. Each case must exit
non-zero and print a diagnostic that names the rejected Save proof artifact
condition.

The negative contract does not run the desktop Save path and must not be cited as
Save completion evidence. It exists to prove that only a fresh, canonical,
internally consistent `status: "proven"` artifact can satisfy the positive Save
write/readback proof contract.

For usage, API arguments, configuration, and examples, see
[Save Menu Dialog Negative Artifact Contract](./save-menu-dialog-negative-artifact-contract.md).

## Runner properties

The runner passes these Maven properties to `RobotSaveMenuDialogWriteReadbackProofTest` and also exports equivalent environment variables for forked Surefire JVMs:

| Property | Purpose |
| --- | --- |
| `org.alice.eatme.saveProof.scenario` | Scenario ID expected in the artifact |
| `org.alice.eatme.saveProof.runId` | Run token expected in the artifact |
| `org.alice.eatme.saveProof.evidencePath` | Canonical artifact path to write and validate |

The environment fallbacks are `ALICE_SAVE_PROOF_SCENARIO`, `ALICE_SAVE_PROOF_RUN_ID`, and `ALICE_SAVE_PROOF_EVIDENCE_PATH`.

Use `NODE_OPTIONS=--max-old-space-size=32768` for the QA shell.
