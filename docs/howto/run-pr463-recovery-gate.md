# Run the PR #463 Recovery Gate

Use this guide to evaluate PR #463 merge readiness after a focused
archive/player branch repair or to verify that current evidence is sufficient
without repair.

## Prerequisites

- Python 3.10 or later.
- The `gh` CLI authenticated with access to `rysweet/RabbitHole` (required for
  `--refresh-github`).
- A clean worktree on the PR #463 branch:
  `feat/issue-462-restart-rabbithole-archiveplayer-boundary-lane-thr`.
- The tweedle-lang submodule initialized:
  ```bash
  git submodule update --init tweedle-lang
  ```

## Collect evidence

The gate does not run tests or fetch PR state on its own. Assemble the evidence
JSON before invoking the gate.

### 1. Run focused validations

Run each focused validation and record the command, outcome, and current HEAD
SHA:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

# Python contract tests
python3 -m unittest \
  tests.test_pr463_owner_free_recovery_gate \
  tests.test_pr463_archive_player_boundary_contract

# QA scenario/schema contracts
bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh

# Maven archive fixture characterization
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test

# Maven Tweedle decoder boundary
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+zeroArgumentThisMethodCallDecodeRejectsChainedCall \
  test
```

### 2. Build the evidence JSON

Assemble a JSON object with the fields documented in
[PR #463 Recovery Gate Reference](../reference/pr463-recovery-gate.md). At
minimum, populate:

- `repository`, `prNumber`, `branch`, `baseRef`, `developBaseSha`
- `headSha`, `localHeadSha`, `prHeadSha`
- `mergeable`, `mergeStateStatus`, `worktreeClean`
- `recoveryMode`, `scope`
- `manualMergePerformed`, `replacementPullRequestCreated`, `noOpModeUsed`
- `tweedleLangInitialized`, `nodeOptions`
- `archivePlayerEvidenceSurfaces`
- `boundaryEvidence` with all currency flags and nonclaims
- `qaScenarioContracts` with workflow alignment
- `validations` with each focused validation record
- `githubActions` with check results
- `prEvidence` with PR description state
- `commands` listing all commands executed

Save the file as `evidence.json`.

### 3. Capture SHA values

```bash
HEAD_SHA=$(git rev-parse HEAD)
DEVELOP_SHA=$(git rev-parse origin/develop)
echo "headSha: $HEAD_SHA"
echo "developBaseSha: $DEVELOP_SHA"
```

Use the exact final SHA in the evidence JSON. Placeholder values are not
accepted.

## Run the gate

### Basic evaluation

```bash
python3 scripts/pr463_recovery_gate.py evidence.json --pretty
```

The gate prints a JSON readiness result to stdout and exits `0` for
`MERGE_READY` or `1` otherwise.

### With live GitHub refresh

```bash
python3 scripts/pr463_recovery_gate.py evidence.json \
  --refresh-github \
  --pretty \
  --verbose
```

`--refresh-github` fetches current PR state and GitHub Actions check results
using `gh pr view` and overlays them on the evidence before evaluation. This
ensures the evidence reflects the live remote state.

### From stdin

```bash
cat evidence.json | python3 scripts/pr463_recovery_gate.py --pretty
```

When no file argument is provided, the gate reads evidence from stdin.

## Interpret the result

### MERGE_READY

```json
{
  "status": "MERGE_READY",
  "headSha": "b67969f...",
  "blockers": [],
  "repairRequired": false,
  "summary": "PR #463 is merge-ready after focused archive/player repair: ..."
}
```

All verifiers passed. The PR is ready for human review and merge.

### REPAIR_REQUIRED

```json
{
  "status": "REPAIR_REQUIRED",
  "headSha": "abc1234...",
  "blockers": ["archive-player-evidence-stale", "pr-merge-state-not-clean"],
  "repairRequired": true,
  "allowedRepairPaths": [
    "scripts/pr463_recovery_gate.py",
    "tests/test_pr463_owner_free_recovery_gate.py",
    "..."
  ],
  "summary": "Focused archive/player boundary repair is required: ..."
}
```

The branch needs repair. Only modify files listed in `allowedRepairPaths`.
After repair, re-collect evidence and re-run the gate.

### NOT_MERGE_READY

```json
{
  "status": "NOT_MERGE_READY",
  "headSha": "def5678...",
  "blockers": ["github-actions-not-green"],
  "repairRequired": false,
  "summary": "Recovery readiness is blocked: github-actions-not-green"
}
```

The branch is not stale but does not meet merge criteria. Fix the blocker
(e.g., wait for CI to pass) and re-run the gate.

## Common blockers and fixes

| Blocker | Fix |
| --- | --- |
| `dirty-worktree` | Commit or stash uncommitted changes. |
| `tweedle-lang-not-initialized` | Run `git submodule update --init tweedle-lang`. |
| `pr-not-mergeable` | Rebase or resolve conflicts against `origin/develop`. |
| `github-actions-not-complete` | Wait for CI checks to finish. |
| `github-actions-not-green` | Fix the failing CI check. |
| `archive-player-evidence-stale` | Re-collect boundary evidence at the current HEAD SHA. |
| `validation-stale-head` | Re-run validations after the latest commit. |
| `noop-mode-used` | Do not use no-op justification. Perform the actual repair. |
| `manual-merge-performed` | Do not merge manually. Use the normal PR merge flow. |
| `generated-archive-committed` | Remove committed `.a3w` files from the diff. |
| `github-pr-service-unavailable` | Check `gh auth status` and network connectivity. |

## CLI options

| Option | Default | Description |
| --- | --- | --- |
| `evidence` | stdin | Path to evidence JSON file. |
| `--pretty` | off | Pretty-print the readiness JSON. |
| `--refresh-github` | off | Fetch live PR state with `gh pr view` before evaluation. |
| `--github-timeout` | 20.0 | Seconds per `gh pr view` attempt. |
| `--github-retries` | 3 | Number of retry attempts. |
| `--github-retry-delay` | 2.0 | Seconds between retries. |
| `--verbose` | off | Enable informational logging to stderr. |

## Run the contract tests

The gate has two companion test suites:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest \
  tests.test_pr463_owner_free_recovery_gate \
  tests.test_pr463_archive_player_boundary_contract -v
```

`test_pr463_owner_free_recovery_gate` validates gate verifier behavior,
evidence shape requirements, and blocker code correctness.

`test_pr463_archive_player_boundary_contract` validates that documentation,
scenario wording, and test assertions stay within the bounded archive/player
boundary and do not overclaim.

## Claim boundary

This gate proves that PR #463 recovery evidence is current, bounded, and
correctly structured. It does not prove:

- full Tweedle/player decode
- historical archive migration completeness
- full UI automation
- visible rendering correctness
- grading
- Save/Open guarantees
- lesson completion

Human review of the actual characterization test behavior remains necessary.
