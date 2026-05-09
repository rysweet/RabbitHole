# PR #463 Recovery Gate

This reference documents the `scripts/pr463_recovery_gate.py` gate script that
evaluates focused archive/player recovery evidence before merge-ready claims for
PR #463.

The gate is scoped to one pull request and one boundary. It does not generalize
to other PRs, broader archive migration, desktop workflow automation, rendering,
grading, Save/Open guarantees, or lesson completion.

## Purpose

PR #463 adds bounded archive/player diagnostic evidence for JSON `.a3w` player
archives. When the PR branch becomes stale against `origin/develop`, a focused
repair is required. The recovery gate ensures the repaired branch meets all
merge-readiness criteria before a human reviewer approves.

The gate reads a structured evidence JSON object, runs nine verifiers against it,
and emits a readiness result with explicit blocker codes.

## Evidence shape

The gate consumes a JSON object with these top-level fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `repository` | string | yes | Must be `rysweet/RabbitHole`. |
| `prNumber` | integer | yes | Must be `463`. |
| `branch` | string | yes | Must be `feat/issue-462-restart-rabbithole-archiveplayer-boundary-lane-thr`. |
| `baseRef` | string | yes | Must be `develop` or `origin/develop`. |
| `developBaseSha` | string | yes | The `origin/develop` commit used for reconciliation. |
| `headSha` | string | yes | The repaired branch head SHA. |
| `localHeadSha` | string | yes | Local `git rev-parse HEAD` at evidence collection time. |
| `prHeadSha` | string | yes | Head SHA from `gh pr view` for the remote PR. |
| `mergeable` | string | yes | Must be `MERGEABLE` (case-insensitive). |
| `mergeStateStatus` | string | yes | Must be `CLEAN` (case-insensitive). |
| `worktreeClean` | boolean | yes | Must be `true`. |
| `recoveryMode` | string | yes | Must be `focused-archive-player-repair`. |
| `scope` | string | yes | Must be `archive/player-boundary`. |
| `manualMergePerformed` | boolean | yes | Must be `false`. |
| `replacementPullRequestCreated` | boolean | yes | Must be `false`. |
| `noOpModeUsed` | boolean | yes | Must be `false`. |
| `manualMergeUsed` | boolean | no | Checked as an additional merge safety signal. Must be `false` when present. |
| `noOpJustificationUsed` | boolean | no | Checked alongside `noOpModeUsed`. Must be `false` when present. |
| `repairRequired` | boolean | no | `true` when the branch needs repair. Drives `REPAIR_REQUIRED` vs `NOT_MERGE_READY` status. |
| `pushedRepair` | boolean | no | `true` when a repair has been pushed. Required when `repairRequired` is `true`. |
| `tweedleLangInitialized` | boolean | yes | Must be `true`. |
| `nodeOptions` | string | yes | Must be `--max-old-space-size=32768`. |
| `archivePlayerEvidenceSurfaces` | string[] | yes | Bounded list of evidence surface paths. |
| `repairDiffFiles` | string[] | conditional | Files changed by the repair. Required when `repairRequired` is `true`. |
| `boundaryEvidence` | object | yes | Current-head boundary evidence with currency flags. |
| `qaScenarioContracts` | object | yes | Schema/runner/validator/argv alignment evidence. |
| `validations` | object[] | yes | Focused validation records with command, outcome, and head SHA. |
| `githubActions` | object | yes | Live GitHub check state at the PR head SHA. |
| `prEvidence` | object | yes | PR description merge-readiness evidence. |
| `commands` | string[] | yes | Commands executed during recovery. |
| `externalServiceErrors` | object[] | no | Populated by `--refresh-github` on failure. Each entry has `service`, `operation`, and `message`. |

### Boundary evidence object

The `boundaryEvidence` object must include:

| Field | Type | Description |
| --- | --- | --- |
| `headSha` | string | Must match the top-level `headSha`. |
| `referenceDocCurrent` | boolean | Must be `true`. |
| `howtoCurrent` | boolean | Must be `true`. |
| `tutorialCurrent` | boolean | Must be `true`. |
| `archiveScenarioCurrent` | boolean | Must be `true`. |
| `tweedleScenarioCurrent` | boolean | Must be `true`. |
| `characterizationTestsCurrent` | boolean | Must be `true`. |
| `nonclaims` | string[] | Must include all expected nonclaims. |
| `forbiddenClaims` | string[] | Must be empty. |
| `generatedArchivesCommitted` | string[] | Must be empty. |
| `binaryCorpusPayloadsCommitted` | string[] | Must be empty. |

### Validation record shape

Each entry in the `validations` array must include:

| Field | Type | Description |
| --- | --- | --- |
| `name` | string | One of the required validation names. |
| `command` | string | The exact command that was run. |
| `outcome` | string | Must be `passed`. |
| `headSha` | string | Must match the top-level `headSha`. |

Required validation names:

- `python-pr463-contracts`
- `alice-desktop-scenario-catalog`
- `story-api-migration-characterization`
- `core-ast-decoder-boundary`

## Verifiers

The gate runs nine verifiers in order. Each verifier returns zero or more
blocker codes. Any non-empty blocker list blocks merge readiness.

| Verifier | Scope |
| --- | --- |
| `verify_external_service_errors` | Blocks when live GitHub evidence could not be fetched. |
| `verify_pr_state` | Requires correct repository, branch, base, head SHA alignment, clean worktree, no manual merge, no replacement PR, and no no-op mode. |
| `verify_github_actions` | Requires all GitHub Actions checks completed and green for the exact PR head SHA. |
| `verify_repair_scope` | Allows repair diffs only on focused PR #463 guard and archive/player evidence paths. Rejects generated archives, binary payloads, and path traversal. |
| `verify_boundary_evidence` | Requires current bounded docs, scenarios, and characterization tests. Rejects overclaims and missing nonclaims. |
| `verify_qa_scenario_contracts` | Requires schema, runner, validator, and argv alignment for both `archive-fixture-smoke` and `tweedle-decoder-boundary-smoke` workflows. |
| `verify_validation_evidence` | Requires all four focused validations passed at the current head SHA with the saved Node option. |
| `verify_pr_evidence` | Requires current PR description evidence with merge-ready criteria and bounded claims. |
| `verify_command_safety` | Rejects manual merge commands, unexpected pushes, and no-op disposition. |

## Blocker codes

Each blocker code identifies a specific merge-readiness failure:

| Code | Meaning |
| --- | --- |
| `missing-pr-head-evidence` | Head SHA fields are incomplete. |
| `wrong-repository` | Repository is not `rysweet/RabbitHole`. |
| `wrong-pr-number` | PR number is not `463`. |
| `wrong-authoritative-branch` | Branch does not match the expected feature branch. |
| `wrong-base-ref` | Base ref is not `develop`. |
| `missing-develop-base-sha` | The `origin/develop` reconciliation base is missing. |
| `dirty-worktree` | The worktree has uncommitted changes. |
| `local-head-sha-mismatch` | Local HEAD does not match the evidence head SHA. |
| `pr-head-sha-mismatch` | Remote PR head does not match the evidence head SHA. |
| `pr-not-mergeable` | The PR is not in a MERGEABLE state. |
| `pr-merge-state-not-clean` | The PR merge state is not CLEAN. |
| `wrong-recovery-mode` | Recovery mode is not `focused-archive-player-repair`. |
| `wrong-recovery-scope` | Recovery scope is not `archive/player-boundary`. |
| `manual-merge-performed` | A manual merge was used. |
| `manual-merge-used` | A `gh pr merge` or `git merge` command was detected. |
| `replacement-pr-created` | A replacement pull request was created instead of repairing the existing one. |
| `noop-mode-used` | A no-op justification was used when repair was needed. |
| `github-pr-service-unavailable` | Live GitHub PR state could not be refreshed. |
| `missing-github-actions-evidence` | No GitHub Actions evidence is present. |
| `github-actions-stale-head` | GitHub Actions evidence is for a different head SHA. |
| `missing-github-actions-checks` | No individual check results are present. |
| `github-actions-not-complete` | One or more checks have not completed. |
| `github-actions-not-green` | One or more checks did not conclude with success. |
| `github-actions-required-check-missing` | A required check name is missing from the rollup. |
| `unfocused-diff-scope` | Repair diffs include files outside the focused repair paths. |
| `generated-archive-committed` | A generated `.a3w` archive was committed. |
| `binary-corpus-payload-committed` | A binary corpus payload was committed. |
| `missing-boundary-evidence` | No boundary evidence object is present. |
| `archive-player-evidence-surfaces-missing` | Evidence surfaces list is empty. |
| `archive-player-evidence-scope-broadened` | Evidence surfaces include paths outside the known set. |
| `archive-player-evidence-stale` | Boundary evidence is not current for the head SHA. |
| `archive-player-evidence-overclaims` | Forbidden claims were detected in boundary evidence. |
| `archive-player-nonclaims-missing` | Required nonclaims are missing from boundary evidence. |
| `missing-qa-scenario-contract-evidence` | No QA scenario contract evidence is present. |
| `qa-scenario-validation-not-run` | Scenario validation was not performed. |
| `qa-workflow-contract-missing` | A required workflow contract is missing. |
| `qa-workflow-validator-not-allowlisted` | A workflow is not allowlisted in the validator. |
| `qa-workflow-runner-not-allowlisted` | A workflow is not allowlisted in the runner. |
| `qa-workflow-schema-not-allowlisted` | A workflow is not listed in the JSON Schema. |
| `qa-workflow-automation-mode-not-gated` | A workflow is not using `gated-command-smoke` mode. |
| `qa-workflow-argv-not-focused` | A workflow argv does not match the expected focused command. |
| `missing-validation-evidence` | No validation records are present. |
| `tweedle-lang-not-initialized` | The tweedle-lang submodule was not initialized. |
| `missing-node-options` | The Node memory option is missing or wrong. |
| `validation-command-missing` | A validation record has no command. |
| `validation-stale-head` | A validation was run against a different head SHA. |
| `python-contract-validation-failed` | Python contract tests did not pass. |
| `qa-scenario-validation-failed` | QA scenario/schema validation did not pass. |
| `focused-maven-validation-missing` | Required Maven validations are missing. |
| `focused-maven-validation-failed` | Maven characterization tests did not pass. |
| `missing-pr-evidence` | No PR description evidence is present. |
| `pr-evidence-stale-head` | PR evidence is for a different head SHA. |
| `pr-evidence-missing-current-head` | PR evidence does not confirm current head. |
| `pr-evidence-missing-merge-ready-criteria` | PR description was not updated with merge-ready criteria. |
| `pr-evidence-overclaims-archive-player-boundary` | PR evidence contains overclaims beyond the boundary scope. |
| `pr-evidence-open-blockers` | PR evidence lists open blockers. |
| `missing-command-evidence` | No commands were recorded. |
| `noop-used-despite-required-repair` | A no-op justification was used when repair was required. |
| `unexpected-push-without-repair` | A `git push` was detected when no repair was required. |
| `focused-repair-not-pushed` | A required repair was not pushed. |
| `missing-focused-repair-diff` | Repair was required but no diff files were recorded. |
| `external-service-error` | A non-GitHub external service produced an error during evidence refresh. |

## Readiness result

The gate emits a JSON object to stdout:

```json
{
  "status": "MERGE_READY",
  "headSha": "b67969f...",
  "blockers": [],
  "repairRequired": false,
  "recoveryMode": "focused-archive-player-repair",
  "allowedRepairPaths": [],
  "mayUseNoOpJustification": false,
  "summary": "PR #463 is merge-ready after focused archive/player repair: ..."
}
```

| Field | Type | Description |
| --- | --- | --- |
| `status` | string | `MERGE_READY`, `REPAIR_REQUIRED`, or `NOT_MERGE_READY`. |
| `headSha` | string | The evaluated branch head SHA. |
| `blockers` | string[] | Blocker codes preventing merge. Empty when `MERGE_READY`. |
| `repairRequired` | boolean | Whether the branch needs repair before merge readiness. |
| `recoveryMode` | string | The recovery mode from the evidence. Present only when `MERGE_READY`. |
| `allowedRepairPaths` | string[] | Paths allowed in the repair diff. Non-empty only when `REPAIR_REQUIRED`. |
| `mayUseNoOpJustification` | boolean | Always `false`. No-op mode is not permitted for this recovery. |
| `summary` | string | Human-readable summary of the readiness state. |

Exit code `0` means `MERGE_READY`. Exit code `1` means blocked.

## Required status checks

The gate requires these GitHub Actions check names to be present and green:

- `Alice Coverage Reports/coverage`
- `package-netbeans`
- `test`
- `build`
- `GitGuardian Security Checks`

## Focused repair paths

When repair is needed, only these files may appear in the repair diff:

```text
scripts/pr463_recovery_gate.py
tests/test_pr463_owner_free_recovery_gate.py
tests/test_pr463_archive_player_boundary_contract.py
docs/reference/player-archive-unsupported-tweedle-diagnostics.md
docs/howto/characterize-player-archive-unsupported-tweedle-diagnostics.md
docs/tutorials/player-archive-unsupported-this-call-diagnostic.md
qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml
qa/outside-in/alice-desktop/scenarios/tweedle-decoder-boundary-smoke.yaml
```

These paths separate into two categories: archive/player evidence surfaces
(docs, scenarios, characterization tests) and repair infrastructure (gate
script, Python contract tests). The gate tracks both but reports them
separately.

## Archive/player evidence surfaces

The gate protects these evidence surfaces:

```text
docs/reference/player-archive-unsupported-tweedle-diagnostics.md
docs/howto/characterize-player-archive-unsupported-tweedle-diagnostics.md
docs/tutorials/player-archive-unsupported-this-call-diagnostic.md
qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml
qa/outside-in/alice-desktop/scenarios/tweedle-decoder-boundary-smoke.yaml
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java
```

## Expected nonclaims

Every boundary evidence object must list these nonclaims:

- full Tweedle/player decode
- historical archive migration completeness
- full UI automation
- visible rendering correctness
- grading
- Save/Open guarantees
- lesson completion

## Security

- No `shell=True` in subprocess calls; all commands use list-form arguments.
- `SENSITIVE_OUTPUT_REDACTIONS` scrubs bearer tokens, PATs, and passwords from
  error output before logging or display.
- Path traversal is blocked by `_has_parent_traversal()`.
- Generated archive and binary corpus payload commits are detected and blocked.
- No credentials are embedded in the gate script, its tests, or its
  documentation.

## Configuration

The gate has no runtime configuration files. All expected values are hardcoded
constants matching PR #463's branch, repository, and evidence scope.

The only environment dependency is `NODE_OPTIONS=--max-old-space-size=32768`,
which is a saved build preference not specific to the gate.

The `gh` CLI must be authenticated and on `PATH` when `--refresh-github` is
used.

## Limitations

- The gate is scoped to PR #463. It is not a general-purpose PR gate.
- It does not run tests; it evaluates pre-collected evidence.
- It does not prove archive decode behavior; it proves that the evidence
  proving decode behavior is current, bounded, and free of overclaims.
- It cannot detect semantic overclaims that are phrased to avoid the
  keyword-based nonclaim check. Human review remains necessary.
