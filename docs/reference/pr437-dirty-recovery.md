# PR #437 dirty recovery reference

This reference defines the dirty-recovery contract for RabbitHole PR #437. The contract repairs a current-base `DIRTY` state without no-op mode, keeps evidence limited to the Select Project `Africa Full` starter lane, reruns focused validation, pushes only focused repair commits, and refreshes merge-ready evidence against the pushed head.

For operator steps, see [Recover PR #437 after DIRTY merge state](../howto/recover-pr437-dirty-select-project.md). For the underlying Select Project evidence contract, see [Select Project Africa Full AT-SPI evidence reference](./select-project-africa-full-atspi-evidence.md).

## Contents

- [Mode](#mode)
- [State inputs](#state-inputs)
- [Configuration](#configuration)
- [Finalization tool interface](#finalization-tool-interface)
- [Dirty-recovery state machine](#dirty-recovery-state-machine)
- [Conflict scope](#conflict-scope)
- [Scenario contract surfaces](#scenario-contract-surfaces)
- [Evidence scope](#evidence-scope)
- [Validation matrix](#validation-matrix)
- [Report contract](#report-contract)
- [Blocker taxonomy](#blocker-taxonomy)
- [Push and security rules](#push-and-security-rules)

## Mode

Dirty recovery uses `EDIT_AND_PUSH` or `BLOCKED_WITH_REASON` only.

| Mode | Allowed for DIRTY repair | Meaning |
| --- | --- | --- |
| `EDIT_AND_PUSH` | Yes | Conflict repair changed repository files, validation passed, and the focused repair commit is pushed to the PR branch. |
| `BLOCKED_WITH_REASON` | Yes | A current blocker prevents safe repair, validation, evidence refresh, or push. |
| `NO_OP` | No | No-op reports are invalid for this dirty-repair lane, even if a later metadata read becomes clean. |

The no-op path documented for clean current-head verification remains a separate contract. It must not be used to mask or skip dirty repair.

## State inputs

The recovery controller reads current state from live GitHub metadata, local Git, and focused QA artifacts.

| Input | Required fields |
| --- | --- |
| PR metadata | `number`, `state`, `headRefName`, `headRefOid`, `baseRefName`, `isDraft`, `mergeStateStatus`, `reviewDecision`, `statusCheckRollup`, `url`. |
| Local checkout | `git rev-parse HEAD`, `git rev-parse --abbrev-ref HEAD`, `git status --short --branch`. |
| Base ref | `origin/<baseRefName>` fetched during the recovery run. |
| Conflict set | `git diff --name-only --diff-filter=U` after reconciliation. |
| Focused evidence | Select Project scenario validation, proof/probe shell tests, PR #437 unittest contracts, and live AT-SPI artifacts when a live success claim is made. |

Live PR metadata is mandatory. Cached PR JSON, copied head SHAs, stale CI summaries, or branch-name matches without SHA verification are not accepted.

## Configuration

| Setting | Default or value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Required shell preference for focused validation commands in this lane. |
| `GH_EXTERNAL_ATTEMPTS` | `3` | Retry count for transient GitHub metadata and fetch failures. |
| `GH_EXTERNAL_RETRY_SECONDS` | `2` for shell examples, `0.5` in the finalization helper default | Delay between retry attempts. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS` | unset by default | Set to `1` only for controlled live Alice desktop QA launches. |
| Evidence directory | `qa/outside-in/alice-desktop/evidence/select-project-africa-full` | Ignored local directory for generated Select Project artifacts. |

There is no dirty-recovery configuration flag that enables no-op mode.

## Finalization tool interface

`scripts/pr437-finalization.py` is the local finalization evidence helper. Dirty recovery calls it after the repair commit changes the PR head:

```bash
python3 scripts/pr437-finalization.py \
  --repo rysweet/RabbitHole \
  --pr-number 437 \
  --expected-head "$(git rev-parse HEAD)" \
  --branch feat/issue-415-rabbithole-wave7-select-project-starter-lane-follo
```

### Arguments

| Argument | Required value |
| --- | --- |
| `--repo` | `rysweet/RabbitHole`. |
| `--pr-number` | `437`. |
| `--expected-head` | Current local commit SHA after the dirty-recovery repair commit. |
| `--branch` | Current PR `headRefName`. |

### Service boundary

The helper reads GitHub metadata through `gh pr view` and local state through `git`. It does not call an Alice runtime service. GitHub authentication, network connectivity, rate limiting, or checkout failures are reported as `environment dependency` blockers.

### Output actions

| Action | Dirty-recovery interpretation |
| --- | --- |
| `EDIT_AND_PUSH` | Expected ready path for a repaired dirty branch. The report lists modified files and focused validation evidence. |
| `BLOCKED_WITH_REASON` | Expected blocked path when head, merge state, checks, scope, metadata, or validation gates fail. |
| `NO_OP` | Rejected by dirty recovery. A dirty-repair wrapper or operator converts this to `BLOCKED_WITH_REASON` unless the task is explicitly the separate clean no-op verification path. |

## Dirty-recovery state machine

| State | Entry condition | Exit condition |
| --- | --- | --- |
| `READ_PR_METADATA` | Recovery starts. | Live PR metadata contains the required fields and PR #437 is open. |
| `VERIFY_HEAD` | PR metadata has `headRefOid` and `headRefName`. | Local checkout branch and `HEAD` match the live PR fields. |
| `FETCH_BASE` | Head is verified. | `origin/<baseRefName>` is fetched. |
| `RECONCILE_DIRTY` | Base is current. | `git merge --no-commit --no-ff origin/<baseRefName>` reproduces and reconciles the dirty state on the verified PR branch. |
| `RESOLVE_CONFLICTS` | Unmerged files exist. | Only required conflict hunks are resolved or one blocker is recorded. |
| `VALIDATE_FOCUSED_SCOPE` | Worktree has the focused repair. | Select Project QA and PR #437 contracts pass. |
| `COMMIT_AND_PUSH` | Validation passes and diff is focused. | Repair commit is pushed to the PR branch on `origin`. |
| `REFRESH_FINALIZATION` | Push succeeds. | Current PR metadata and finalization report point at the new head. |
| `READY_OR_BLOCKED` | Final gates evaluated. | Report publishes `Current blocker: None` or `NOT_MERGE_READY` with one blocker. |

## Conflict scope

Only conflict files reported by Git are editable for dirty repair. Unrelated cleanups, broad refactors, speculative fixes, and new feature claims are out of scope.

| Surface | Contract |
| --- | --- |
| Git recovery layer | Reconciles the PR branch with current `origin/develop`; never merges the PR into `develop`. |
| Select Project QA scenarios | Preserve the `alice-desktop-select-project-tab-click-exec` lane and `Africa Full` target metadata. |
| Scenario schema | Keeps workflow enums, target metadata fields, evidence fields, and argv allowlists aligned. |
| Runner and validator | Pass validated target starter metadata to the probe and reject unsafe or drifted scenario input. |
| Shell contract tests | Stay synchronized with schema, runner, validator, and probe field names. |
| PR finalization tooling | Uses the new pushed head and current PR metadata after each repair commit. |
| Documentation | Describes only verified Select Project starter evidence and exact blockers. |

## Scenario contract surfaces

When a dirty conflict touches the Select Project scenario lane, these files change as one synchronized contract set:

| Surface | Repository path |
| --- | --- |
| Scenario YAML | `qa/outside-in/alice-desktop/scenarios/select-project-tab-click-exec.yaml`. |
| Schema | `qa/outside-in/alice-desktop/schema/scenario.schema.json`. |
| Validator | `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`. |
| Runner | `qa/outside-in/alice-desktop/runners/run-scenario.sh`. |
| Select Project proof tests | `qa/outside-in/alice-desktop/tests/test-select-project-proof.sh`, `test-tab-click-probe.sh`, `test-post-project-open-probe.sh`. |
| PR #437 contracts | `tests/test_pr437_select_project_recovery_contract.py`, `tests/test_pr437_finalization_workflow.py`, `tests/test_pr437_noop_recovery_report_contract.py`, `tests/test_pr437_dirty_recovery_workflow.py`. |

The accepted target starter is always:

```yaml
targetStarter:
  displayName: Africa Full
  repositoryPath: core/resources/src/application/resources/starter-projects/AfricaFull.a3p
```

## Evidence scope

Dirty recovery accepts only these evidence claims:

| Claim | Evidence source |
| --- | --- |
| Select Project visibility | `select-project-window.json` and runner status from the same live run. |
| Starters tab activation | `tab-click-observation.json` with `startersTabSafety.activatedBeforeTargetSearch=true` and `targetSearchScope=active-starters-tab`. |
| Africa Full target selection/open attempt | `targetStarter`, `targetStarterObserved`, `targetSelectionObserved`, `openAttempted`, and `projectOpenObserved` from the same target-specific artifact. |
| Post-open gating | `post-project-open-observation.json` only after the matching Africa Full opened gate passes. |
| Exact blocker | `blocker`, `blockerDetail`, and `nextBlocker` from the failed or blocked target-specific artifact. |

The dirty-recovery report must not claim visible rendering correctness, Save completion, grading, creative assessment, world interaction, full lesson execution, full UI automation, installer behavior, decoder behavior, coverage, or unrelated desktop workflows.

## Validation matrix

Run these commands before a dirty-recovery push:

| Command | Required coverage |
| --- | --- |
| `qa/outside-in/alice-desktop/runners/validate-scenarios.sh` | Scenario catalog, workflow, argv, and target metadata validation. |
| `bash qa/outside-in/alice-desktop/tests/test-select-project-proof.sh` | Select Project window proof and non-claim boundaries. |
| `bash qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh` | Target-specific Africa Full tab-click evidence and blocker shape. |
| `bash qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh` | Post-open gate requires prior target-specific opened evidence. |
| `python3 -m unittest tests/test_pr437_select_project_recovery_contract.py tests/test_pr437_finalization_workflow.py tests/test_pr437_noop_recovery_report_contract.py tests/test_pr437_dirty_recovery_workflow.py` | PR #437 recovery/finalization contracts, dirty-recovery workflow, no-overclaim wording, and no-op separation. |

If the repair changes scenario schema or scenario catalog behavior, also run:

```bash
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-scenario-validation.sh
bash qa/outside-in/alice-desktop/tests/test-select-project-completion-contract.sh
```

## Report contract

Dirty recovery publishes one of two report paths.

### Ready after focused push

```markdown
Report path: `EDIT_AND_PUSH`

## Verified evidence

- Current branch: `<headRefName>`.
- Current head: `<pushed headRefOid>`.
- PR metadata command: `gh pr view 437 --repo rysweet/RabbitHole --json number,title,state,headRefName,headRefOid,baseRefName,isDraft,mergeStateStatus,reviewDecision,statusCheckRollup,url`.
- Dirty repair: reconciled current `origin/develop`, resolved only listed conflict files, and kept behavior scoped to Select Project starter evidence.
- Focused validation: focused shell QA and PR #437 contract tests passed at this head.
- Readiness evidence: merge state/check summary from current GitHub metadata and worktree cleanliness after push.
- Review evidence: owner-free/unset review metadata when `reviewDecision` is empty; no approval claim.
- Finalization evidence: evidence-ready only; no approval, merge, close, rebase, or unrelated push.
- Files modified: `<focused dirty-recovery paths>`.

## Unverified assumptions

- None recorded.

## Current blocker

- None.
```

### Blocked

```markdown
NOT_MERGE_READY

Report path: `BLOCKED_WITH_REASON`

## Verified evidence

- Current branch/head facts that were verified before the blocker.
- Last successful command or artifact.
- Conflict files, failing command, missing artifact, or unavailable dependency that blocked recovery.

## Unverified assumptions

- None recorded.

## Current blocker

- <one blocker>: <concrete blocker detail>.
```

## Blocker taxonomy

| Blocker | Required detail |
| --- | --- |
| `merge dirtiness` | Conflict files, conflict reason, and why resolution is unsafe or incomplete. |
| `missing evidence` | Required Select Project artifact, PR metadata field, or finalization evidence that is absent. |
| `failing validation` | Exact command, exit status, and failing contract boundary. |
| `environment dependency` | Missing or unavailable GitHub metadata, checkout, AT-SPI, Xvfb, Java, Maven, or Tweedle dependency. |

The report names one current blocker. Do not publish a mixed list of possible blockers.

## Push and security rules

- Push only to `origin` and only to the live PR #437 branch.
- Do not push `develop`, `upstream-source`, or any `TheAliceProject/alice3` ref.
- Do not print tokens, run `gh auth status --show-token`, or include credential output in artifacts.
- Do not commit generated evidence under `qa/outside-in/alice-desktop/evidence/`.
- Do not broaden evidence with environment dumps, arbitrary process lists, user home paths, saved project contents, grading state, lesson state, or world execution traces.
- Treat stale metadata, missing checks, failed validation, branch/head drift, and unresolved conflicts as blockers rather than success-shaped defaults.
