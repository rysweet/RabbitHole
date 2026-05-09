# Run merge-ready PR recovery

Use this guide when bringing a pull request to merge-ready status. The recovery
script automates QA scenario validation, quality audit cycles, and PR
description updates.

For the full API, configuration, and evidence template contract, see the
[Merge-ready PR recovery reference](../reference/merge-ready-pr-recovery.md).

## When to use this guide

Use this guide when a pull request needs three blockers resolved before merge:

1. A gadugi-test QA scenario must be validated and run.
2. A quality audit with at least three SEEK/VALIDATE/FIX cycles must be clean.
3. The PR description must include a merge-ready evidence template.

Do not use this guide for:

- PRs that have no QA scenario requirement;
- broad test-suite runs or CI pipeline debugging;
- manual merge or force-push workflows;
- creating new QA scenarios from scratch (use
  [Alice desktop outside-in QA](./alice-desktop-outside-in-qa.md) for scenario
  authoring).

## Prerequisites

Confirm the repository checkout is ready:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
gh auth status
```

The script requires `gh` for PR description updates. Confirm the CLI is
authenticated for the target repository.

## 1. Check out the PR branch

```bash
gh pr checkout 425
git pull origin HEAD
```

Replace `425` with the target PR number.

## 2. Preview the recovery plan

Run the script in dry-run mode to confirm the planned actions:

```bash
python3 scripts/merge-ready-pr-recovery.py \
  --pr 425 \
  --scenario model-export-boundary-smoke \
  --dry-run
```

Confirm the output shows:

- The correct scenario YAML path.
- The quality audit diff scope (files changed in the PR).
- The PR description template that will be appended.

## 3. Run the full recovery

Execute the recovery with the standard memory setting:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 scripts/merge-ready-pr-recovery.py \
  --pr 425 \
  --scenario model-export-boundary-smoke \
  --evidence-dir /tmp/qa-evidence-model-export
```

The script runs three phases:

1. **QA scenario validation** — validates the catalog, runs the schema contract
   test, and executes the gated command smoke.
2. **Quality audit** — performs three SEEK/VALIDATE/FIX cycles against the PR
   diff surface.
3. **PR description update** — appends the merge-ready evidence template and
   pushes any new commits.

## 4. Review the output

On success, the script exits 0 and prints a summary:

```text
✅ QA scenario alice-desktop-model-export-boundary-smoke passed (23 tests)
✅ Quality audit: 3 cycles, final cycle clean
✅ PR #425 description updated with merge-ready evidence
```

On failure, review the reported blocker:

| Failure | Action |
| --- | --- |
| Scenario YAML missing | Create the scenario and synchronize the four-file allowlist. See [Alice desktop outside-in QA](./alice-desktop-outside-in-qa.md). |
| Scenario validation failed | Fix the scenario YAML or allowlist entries and rerun. |
| Gated smoke failed | Fix the underlying test failure and rerun. |
| Quality audit final cycle not clean | Address the remaining findings and rerun with `--audit-cycles` increased if needed. |
| PR description update failed | Check `gh auth status` and repository permissions. |

## 5. Verify the PR state

Confirm the PR is still open and the description includes the evidence template:

```bash
gh pr view 425 --json state,body -q '"\(.state)\n\(.body)"' | head -20
```

The PR is now merge-ready for reviewer approval. Do not merge manually; let the
reviewer complete the merge.

## 6. Run the recovery tests independently

When modifying the recovery script, run its focused tests:

```bash
python3 -m unittest tests.test_merge_ready_pr_recovery_units
python3 -m unittest tests.test_merge_ready_pr_recovery_workflow
```

These tests use temporary branches and evidence directories. They do not push
to remote branches or modify the scenario catalog.

## Troubleshooting

### Tweedle submodule not initialized

```text
[ERROR] require-tweedle-lang-submodule failed
```

Run `git submodule update --init tweedle-lang` and retry.

### Gated smokes not enabled

The script sets `ALICE_QA_RUN_GATED_SMOKES=1` automatically during the QA
scenario phase. If you see `outcome=gated-not-run`, confirm you are not
overriding the variable in your shell environment.

### Quality audit finding in pre-existing code

The script reports pre-existing findings but does not fix them. If the final
audit cycle reports findings that are all pre-existing (outside the PR diff),
the cycle is still considered clean and the script proceeds.

### PR body too long for `gh pr edit`

If the PR body exceeds the GitHub API limit after appending the evidence
template, the script writes the template to a local file and reports the path.
Paste the template manually or trim the existing PR body.
