# Merge-ready PR recovery

This reference describes the merge-ready PR recovery script that automates the
final validation steps for bringing a pull request to merge-ready status.

## Implementation status

> **Specification only.** The recovery script and its tests do not exist yet.
> This document specifies the CLI contract, recovery steps, and evidence
> template that the implementation must satisfy. Each individual recovery step
> (QA catalog validation, gated smoke, quality audit) can be run manually today
> using the commands listed in [Recovery steps](#recovery-steps).

## Contents

- [Implementation status](#implementation-status)
- [Feature scope](#feature-scope)
- [Usage](#usage)
- [CLI contract](#cli-contract)
- [Recovery steps](#recovery-steps)
- [Evidence template](#evidence-template)
- [Configuration](#configuration)
- [Examples](#examples)
- [Compatibility rules](#compatibility-rules)
- [Validation](#validation)

## Feature scope

The recovery script will live at:

```text
scripts/merge-ready-pr-recovery.py
```

It will automate three merge-ready blockers:

| Blocker | What the script does |
| --- | --- |
| QA scenario | Writes or updates a gadugi-test QA scenario YAML, validates with the catalog validator, and runs the gated command smoke. |
| Quality audit | Runs at least three SEEK/VALIDATE/FIX cycles against the PR diff surface and confirms the final cycle is clean. |
| PR description | Appends a merge-ready evidence template to the PR body with outcome summaries, validation commands, and timestamps. |

The script will not merge the PR. It brings the PR to a state where a reviewer
can approve and merge with confidence that the three blockers are resolved.

Unit tests will live in:

```text
tests/test_merge_ready_pr_recovery_units.py
```

Workflow tests will live in:

```text
tests/test_merge_ready_pr_recovery_workflow.py
```

## Usage

Run the recovery script from the repository root after the PR branch is checked
out and up to date:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 scripts/merge-ready-pr-recovery.py \
  --pr 425 \
  --scenario model-export-boundary-smoke
```

The script exits 0 when all three blockers are resolved and the PR description
is updated. It exits non-zero when any blocker cannot be resolved automatically.

### Dry-run mode

Preview what the script would do without modifying files, running commands, or
updating the PR:

```bash
python3 scripts/merge-ready-pr-recovery.py \
  --pr 425 \
  --scenario model-export-boundary-smoke \
  --dry-run
```

Dry-run mode prints the planned QA scenario path, quality audit scope, and PR
description template without executing them.

## CLI contract

| Argument | Required | Default | Purpose |
| --- | --- | --- | --- |
| `--pr` | Yes | — | The pull request number to bring to merge-ready status. |
| `--scenario` | Yes | — | The workflow name for the QA scenario. Must match a valid `workflow` value in the scenario schema. |
| `--evidence-dir` | No | `/tmp/qa-evidence-<workflow>` | Directory for QA scenario evidence output. |
| `--audit-cycles` | No | `3` | Minimum number of quality audit SEEK/VALIDATE/FIX cycles. Must be at least 3. |
| `--dry-run` | No | `false` | Preview planned actions without executing. |
| `--skip-push` | No | `false` | Commit changes locally but do not push to the remote branch. |

## Recovery steps

The script executes these steps in order:

### Step 1: QA scenario validation

1. Checks whether a scenario YAML exists for the given `--scenario` workflow in
   `qa/outside-in/alice-desktop/scenarios/`.
2. Validates the scenario catalog with `validate-scenarios.sh`.
3. Validates the schema contract with `test-schema-contract.sh`.
4. Runs the gated command smoke with `ALICE_QA_RUN_GATED_SMOKES=1` through
   `run-scenario.sh`.
5. Reads the evidence `status.txt` and confirms `outcome=passed`.

If the scenario YAML does not exist, the script reports the missing file and
exits non-zero. It does not generate scenario YAML automatically; scenario
authoring requires the four-file allowlist synchronization described in
[Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md).

### Step 2: Quality audit cycles

1. Identifies the PR diff surface by comparing the current branch against
   `origin/main`.
2. Runs `--audit-cycles` iterations of SEEK/VALIDATE/FIX:
   - **SEEK**: Examines changed files for bugs, security issues, logic errors,
     consistency problems, and documentation drift.
   - **VALIDATE**: Confirms whether each finding is in the PR diff or
     pre-existing code outside the diff scope.
   - **FIX**: Applies fixes for findings that are in the PR diff. Pre-existing
     findings are reported but not fixed.
3. The final cycle must produce zero findings in the PR diff scope.
4. If the final cycle is not clean after the configured number of cycles, the
   script exits non-zero with a summary of remaining findings.

### Step 3: PR description update

1. Reads the current PR body with `gh pr view`.
2. Appends a `## Merge-Ready Evidence` section with:
   - QA scenario outcome and evidence path.
   - Quality audit cycle count and final-cycle status.
   - Validation commands used.
   - Timestamp.
   - No-op justification when no code changes were needed.
3. Updates the PR body with `gh pr edit`.
4. Pushes any new commits from QA scenario or quality audit fixes.

The script does not merge the PR. The `## Merge-Ready Evidence` section serves
as reviewer-facing proof that the three blockers are resolved.

## Evidence template

The merge-ready evidence section appended to the PR body follows this structure:

```markdown
## Merge-Ready Evidence

| Check | Result |
|-------|--------|
| QA scenario | ✅ `<scenario-id>` passed — <test-count> tests |
| Quality audit | ✅ <cycle-count> cycles, final cycle clean |
| Code changes | No-op / <commit-summary> |

### QA validation commands

\```bash
NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh
NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/tests/test-schema-contract.sh
ALICE_QA_RUN_GATED_SMOKES=1 NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/run-scenario.sh run <scenario-id> \
  --evidence-dir <evidence-dir>
\```

### Quality audit summary

- Cycle 1: <finding-count> findings (<in-diff> in diff, <pre-existing> pre-existing)
- Cycle 2: <finding-count> findings
- Cycle 3: CLEAN (0 findings in diff)

**Timestamp:** <ISO-8601>
```

When no code changes are needed beyond the QA scenario scaffolding, the template
includes a No-op justification line:

```text
No-op justification: All production code changes were already merged. This
commit adds only QA scenario YAML and allowlist synchronization for the
model-export-boundary-smoke lane.
```

## Configuration

| Setting | Default | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Standard memory setting for Alice desktop QA scripts and Maven reactor. |
| `ALICE_QA_RUN_GATED_SMOKES` | unset | Must be set to `1` for the script to execute gated command smokes. |
| `ALICE_QA_SCENARIO_DIR` | `qa/outside-in/alice-desktop/scenarios` | Optional catalog override; leave unset for normal repository validation. |
| `--audit-cycles` | `3` | Minimum number of quality audit cycles. The final cycle must be clean. |

The script inherits the repository's existing build and QA configuration. It
does not introduce new environment variables, CI workflows, or external service
dependencies.

## Examples

### Bring PR #425 to merge-ready with model export QA

```bash
git checkout feature/wave7-model-export-boundary
git pull origin feature/wave7-model-export-boundary

NODE_OPTIONS=--max-old-space-size=32768 python3 scripts/merge-ready-pr-recovery.py \
  --pr 425 \
  --scenario model-export-boundary-smoke \
  --evidence-dir /tmp/qa-evidence-model-export
```

### Run with extra audit cycles

```bash
python3 scripts/merge-ready-pr-recovery.py \
  --pr 425 \
  --scenario model-export-boundary-smoke \
  --audit-cycles 5
```

### Preview without executing

```bash
python3 scripts/merge-ready-pr-recovery.py \
  --pr 425 \
  --scenario model-export-boundary-smoke \
  --dry-run
```

## Compatibility rules

1. The script uses only existing repository tools: `gh`, `mvn`, `python3`,
   `validate-scenarios.sh`, `test-schema-contract.sh`, and `run-scenario.sh`.
2. QA scenario validation follows the same four-file allowlist synchronization
   required by manual scenario authoring: schema, validator, runner, and contract
   test.
3. Quality audit findings are scoped to the PR diff surface. Pre-existing
   findings outside the diff are reported but not fixed.
4. The PR description update is additive. It does not remove or rewrite existing
   PR body content.
5. The script does not merge the PR, force-push, rebase, or modify git history.
6. The script does not create or modify CI workflows.

## Validation

> The test modules below are part of the implementation plan and do not exist
> yet. Until they are created, validate the individual recovery steps manually
> using the commands in [Recovery steps](#recovery-steps) and the
> [tutorial](../tutorials/trace-model-export-pr-recovery.md).

Run the unit tests from the repository root:

```bash
python3 -m unittest tests.test_merge_ready_pr_recovery_units
```

Run the workflow tests that exercise the full recovery pipeline against a
local branch:

```bash
python3 -m unittest tests.test_merge_ready_pr_recovery_workflow
```

Run both test suites together:

```bash
python3 -m unittest \
  tests.test_merge_ready_pr_recovery_units \
  tests.test_merge_ready_pr_recovery_workflow
```

The workflow tests will use temporary branches and evidence directories. They
will not push to remote branches, merge PRs, or modify the scenario catalog.
