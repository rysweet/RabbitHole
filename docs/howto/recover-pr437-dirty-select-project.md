# Recover PR #437 after DIRTY merge state

This how-to describes the dirty-recovery behavior for RabbitHole PR #437. Use it when PR #437 reports `mergeStateStatus=DIRTY` or an equivalent current-base conflict against `develop`. The recovery is an edit-and-push repair: it reconciles the PR branch with current `develop`, resolves only the conflict hunks required for the dirty state, reruns focused Select Project validation, pushes the focused repair commit only after validation passes, and then refreshes finalization evidence against the pushed head.

This lane is not a no-op path. Do not publish `No-op justification:` for a dirty repair.

## Contents

- [Prerequisites](#prerequisites)
- [Safety boundaries](#safety-boundaries)
- [Verify the live PR head](#verify-the-live-pr-head)
- [Reconcile through dirty-recovery workflow](#reconcile-through-dirty-recovery-workflow)
- [Resolve only dirty-recovery conflicts](#resolve-only-dirty-recovery-conflicts)
- [Keep Select Project evidence focused](#keep-select-project-evidence-focused)
- [Run focused validation](#run-focused-validation)
- [Push the repair](#push-the-repair)
- [Refresh finalization evidence](#refresh-finalization-evidence)
- [Report merge readiness](#report-merge-readiness)
- [NOT_MERGE_READY blockers](#not_merge_ready-blockers)

## Prerequisites

Run commands from the repository root on the PR #437 branch.

```bash
export NODE_OPTIONS=--max-old-space-size=32768

git remote -v
gh pr view 437 --repo rysweet/RabbitHole \
  --json number,title,state,headRefName,headRefOid,baseRefName,isDraft,mergeStateStatus,reviewDecision,statusCheckRollup,url
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
test -f core/resources/src/application/resources/starter-projects/AfricaFull.a3p
```

The live Select Project evidence path also needs the AT-SPI stack used by the Alice desktop QA lane:

```bash
test -f /usr/share/java/java-atk-wrapper.jar
python3 - <<'PY'
import pyatspi
print("pyatspi available")
PY
```

If GitHub metadata, checkout, or AT-SPI dependencies are unavailable, report the exact dependency as `NOT_MERGE_READY` instead of using stale metadata or broad UI claims.

## Safety boundaries

The dirty repair has fixed repository and PR boundaries.

| Boundary | Required behavior |
| --- | --- |
| Repository | Work only in `rysweet/RabbitHole`. |
| PR | Repair only PR #437. |
| Branch | Use the live `headRefName`, currently `feat/issue-415-rabbithole-wave7-select-project-starter-lane-follo`. |
| Base | Reconcile against the live `baseRefName`, currently `develop`. |
| Remote | Push only to the PR branch on `origin`. |
| Upstream | Do not push `upstream-source` and do not open issues or pull requests against `TheAliceProject/alice3`. |
| Merge action | Do not merge the PR manually and do not merge into `develop`. |
| No-op | Do not use no-op mode for a dirty repair. |

## Verify the live PR head

Collect current PR metadata before making or validating recovery edits:

```bash
PR_JSON="$(gh pr view 437 --repo rysweet/RabbitHole \
  --json number,title,state,headRefName,headRefOid,baseRefName,isDraft,mergeStateStatus,reviewDecision,statusCheckRollup,url)"
PR_HEAD_OID="$(printf '%s\n' "$PR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["headRefOid"])')"
PR_BRANCH="$(printf '%s\n' "$PR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["headRefName"])')"
BASE_REF="$(printf '%s\n' "$PR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["baseRefName"])')"

gh pr checkout 437 --repo rysweet/RabbitHole
test "$(git rev-parse HEAD)" = "$PR_HEAD_OID"
test "$(git rev-parse --abbrev-ref HEAD)" = "$PR_BRANCH"
git fetch origin "$BASE_REF"
```

Accepted metadata is live metadata from `gh pr view`, not a copied value from an old report. The report records the exact `headRefOid`, branch, base, draft state, merge state, review metadata, and required-check summary used for the repair.

## Reconcile through dirty-recovery workflow

Run the repository's dirty-recovery workflow on the PR branch: fetch the live base, merge that base into the checked-out PR branch, and inspect only Git-reported unmerged paths.

```bash
git fetch origin "$BASE_REF"
git merge --no-commit --no-ff "origin/$BASE_REF"
git diff --name-only --diff-filter=U
```

This reconciles the PR branch against `origin/develop`, reproduces the dirty state, and leaves only real conflict hunks for resolution.

Do not use a manual PR merge, no-op mode, or a broad rebase/cleanup. The workflow output must identify the files that are actually unmerged:

```bash
git diff --name-only --diff-filter=U
```

If no unmerged files remain after reconciliation, the branch still uses the edit-and-push dirty-repair report when any files changed. No-op reporting is reserved for clean current-head verification and is not valid for this DIRTY repair.

## Resolve only dirty-recovery conflicts

Resolve only files reported by Git as unmerged. Preserve PR #437 behavior unless current `develop` contains a required contract change.

| Conflict area | Required resolution |
| --- | --- |
| Scenario YAML | Preserve the Select Project tab-click scenario and its `Africa Full` target starter metadata. |
| Schema, runner, validator, shell tests | Keep workflow values, argv allowlists, target metadata validation, runner promotion fields, and contract tests synchronized as one set. |
| PR finalization tooling | Refresh current PR head/state evidence after the repair commit changes the branch head. |
| Documentation | Keep claims limited to Select Project visibility, Starters tab activation, Africa Full selection/open attempt, and exact blockers. |
| Behavior-sensitive Alice code | Resolve only when focused characterization or QA evidence exists; otherwise report `NOT_MERGE_READY: merge dirtiness`. |

Each resolved conflict records the path, conflict reason, resolution, and the validation command that proves the resolution.

## Keep Select Project evidence focused

Dirty recovery evidence remains the Africa Full Select Project lane described in [Open Africa Full through Select Project with AT-SPI](./open-africa-full-through-select-project-atspi.md) and [Select Project Africa Full AT-SPI evidence reference](../reference/select-project-africa-full-atspi-evidence.md).

Accepted evidence can include only:

- Select Project dialog visibility.
- Starters tab activation before target search.
- `Africa Full` target observation, selection, and guarded open attempt.
- Matching post-open gate evidence after the target-specific open proof.
- One exact blocker when target-specific progress cannot safely continue.

Do not use this recovery to claim visible rendering correctness, Save behavior, grading, lesson completion, broad world interaction, full UI automation, or unrelated Alice desktop behavior.

## Run focused validation

Run the focused QA and PR #437 contract checks before pushing:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-select-project-proof.sh
bash qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh
bash qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh
python3 -m unittest \
  tests/test_pr437_select_project_recovery_contract.py \
  tests/test_pr437_finalization_workflow.py \
  tests/test_pr437_noop_recovery_report_contract.py
```

Run the live AT-SPI scenario when the repair changes the scenario, runner, probe, post-open gate, or documentation evidence for live Select Project success:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

Review `status.txt`, `tab-click-observation.json`, `post-project-open-observation.json`, `x-window-inventory.json`, and `select-project-window.json` from the same run directory before citing live evidence.

## Push the repair

Commit and push only after the focused validation commands pass and the repair diff contains only dirty-recovery changes:

```bash
git status --short --branch
git diff --name-only
git diff --cached --name-only
git commit -m "Repair PR #437 dirty Select Project recovery"
git push origin HEAD:"$PR_BRANCH"
```

The pushed commit message describes the dirty-recovery resolution and does not claim a merge, approval, or broad Alice behavior.

## Refresh finalization evidence

After a successful repair commit and push, the PR head changes. Refresh finalization evidence against the new pushed head:

```bash
NEW_HEAD="$(git rev-parse HEAD)"
python3 scripts/pr437-finalization.py \
  --repo rysweet/RabbitHole \
  --pr-number 437 \
  --expected-head "$NEW_HEAD" \
  --branch "$PR_BRANCH"
```

The finalization report for a dirty repair uses `Report path: EDIT_AND_PUSH` or `Report path: BLOCKED_WITH_REASON`. It does not use `No-op justification:`.

## Report merge readiness

Use this report shape after pushing and re-reading live PR metadata:

```markdown
## Verified evidence

- PR state: PR #437 is open at `<headRefOid>` on `<headRefName>` against `<baseRefName>`.
- Dirty repair: reconciled current `origin/develop` through dirty-recovery workflow and resolved only reported conflict files.
- Focused validation: `validate-scenarios.sh`, Select Project proof/probe scripts, and PR #437 contract tests passed.
- Artifacts: `<run-directory>` with `status.txt`, `tab-click-observation.json`, `post-project-open-observation.json`, `x-window-inventory.json`, and `select-project-window.json`, or `No new live artifact` with the exact reason no live success claim is made.
- Readiness evidence: current branch/head, PR metadata command, worktree cleanliness after push, merge state/check summary, and review metadata.
- Review evidence: Select Project docs/contracts/artifacts reviewed; review decision reported as owner-free/unset when empty.
- Finalization evidence: evidence-ready only; this report does not approve, merge, close, or rebase PR #437.
- Files modified: `<focused dirty-recovery paths>`.

## Unverified assumptions

- None recorded.

## Current blocker

- None.
```

Use `Current blocker: None` only when the current pushed head is verified, merge state is clean or the dirty state was resolved, required checks are successful, focused validation passed, and evidence remains scoped to Select Project starter behavior.

## NOT_MERGE_READY blockers

If any gate fails, do not push incomplete evidence. Publish `NOT_MERGE_READY` with exactly one blocker.

| Blocker | Use when |
| --- | --- |
| `merge dirtiness` | Current `develop` cannot be reconciled safely, conflicts remain, or behavior-sensitive conflicts lack focused proof. |
| `missing evidence` | Merge state is resolved but required Select Project artifacts or report facts are absent. |
| `failing validation` | Any focused QA or PR #437 contract command fails. |
| `environment dependency` | GitHub metadata, checkout, AT-SPI, Xvfb, Java, Maven, or Tweedle prerequisites prevent verification. |

Example blocked report:

```markdown
NOT_MERGE_READY

## Verified evidence

- PR state: PR #437 metadata was read from `gh pr view 437 --repo rysweet/RabbitHole ...`.
- Dirty repair: conflict reproduction stopped at `qa/outside-in/alice-desktop/runners/run-scenario.sh`.
- Focused validation: not run after unresolved conflict.

## Unverified assumptions

- None recorded.

## Current blocker

- merge dirtiness: `run-scenario.sh` has unresolved conflict hunks in Select Project argv handling.
```
