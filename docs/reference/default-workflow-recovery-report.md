# Default workflow recovery report

This reference describes the implemented default-workflow recovery report helper
and no-timeout merge-ready extension for existing pull request branches. Both
contracts keep repository checks tied to the actual Git worktree
under review and keep the final report structured, even when no files changed.

## Contents

- [Scope](#scope)
- [Design inventory](#design-inventory)
- [No-timeout merge-ready recovery](#no-timeout-merge-ready-recovery)
- [Repository path resolution](#repository-path-resolution)
- [No-op guard](#no-op-guard)
- [Develop-head conflict recovery](#develop-head-conflict-recovery)
- [PR metadata and conflicts](#pr-metadata-and-conflicts)
- [Workflow report API](#workflow-report-api)
- [Configuration](#configuration)
- [Examples](#examples)
- [Review rules](#review-rules)
- [Troubleshooting](#troubleshooting)

## Scope

Use this report contract for recovery work that evaluates an existing PR branch
without rewriting PR history or manually merging the pull request. The report is
evidence about the exact checked head, focused validation, and readiness gates;
it does not authorize merging the pull request into the target branch or pushing
outside the pull request branch.

The report is a workflow artifact. It does not expand product claims, desktop QA
claims, Run execution claims, visible rendering claims, Save claims, grading
claims, Sims validation claims, or deployed installer claims. Product evidence
must come from the focused artifacts documented by the relevant feature
reference, such as [Desktop Run execution gap report](./desktop-run-execution-gap-report.md).

## Design inventory

The recovery design explicitly adds these files:

```yaml
new_files:
  - docs/howto/recover-pr-with-default-workflow.md
  - docs/reference/default-workflow-recovery-report.md
  - docs/reference/desktop-run-execution-gap-report.md
  - docs/tutorials/trace-no-timeout-pr-recovery.md
  - qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh
  - scripts/default_workflow_recovery.py
  - tests/test_default_workflow_merge_ready_contract.py
  - tests/test_default_workflow_recovery_contract.py
```

The design also updates existing desktop QA, scenario, evidence, and index files
to wire the bounded Run-window/debug evidence contract into the existing
documentation and validation surfaces. Do not summarize this feature as
`new_files: []`; reviewers need the added docs, helper, and contract tests listed
explicitly to audit the recovery scope.

## No-timeout merge-ready recovery

**Status:** implemented in `scripts/default_workflow_recovery.py`.

No-timeout recovery evaluates an existing pull request branch at the exact
GitHub PR head without adding outer shell `timeout` wrappers around evidence
commands, synthetic polling success gates, or manual PR-into-base merge steps.
It is a fail-closed review workflow: green checks and completed workflows are
necessary evidence, but they are not enough to claim merge readiness.

The no-timeout rule applies to the recovery evidence command line. Existing QA
scenario timeout fields and internal runner guards remain part of the QA system;
they are not prohibited by this recovery contract.

The recovery decision is one of:

| Decision | Required meaning |
| --- | --- |
| `MERGE_READY` | The exact local `HEAD` matches the current PR `headRefOid`; the current `origin/develop` head used for conflict recovery or no-op evidence is recorded; the worktree is clean; GitHub Actions are green for that head; mergeability is clean; focused runnable QA evidence passed; docs impact was reviewed; diff scope is focused; PR description evidence is accurate; at least three quality-audit SEEK / VALIDATE / FIX cycles are documented; and the final cycle is clean. |
| `NOT_MERGE_READY` | One or more gates are missing, stale, dirty, conflicting, partial, or blocked. The report must list explicit blockers instead of implying readiness. |

Use `NOT_MERGE_READY` when evidence is unavailable or ambiguous. Do not convert
partial desktop evidence, generated checklist files, or green CI alone into a
readiness claim.

### Gate order

Run the gates in this order so later evidence is tied to the correct head:

1. Verify local `HEAD` equals the current PR `headRefOid`.
2. Fetch and record current `origin/develop`; when a task supplies an expected
   develop SHA, verify it before changing the PR branch.
3. If the PR is dirty, update only the PR branch against that develop head and
   resolve conflicts there.
4. Confirm the repository has no conflicting merge state and no unrelated dirty
   worktree changes.
5. Collect current-head GitHub Actions and status-check evidence.
6. Inspect the base-to-head diff for focused scope.
7. Run focused QA, scenario, and test evidence applicable to the diff.
8. Review docs impact and PR description evidence for accuracy and bounded
   claims.
9. Record three quality-audit SEEK / VALIDATE / FIX cycles.
10. Emit `MERGE_READY` only when every gate is proven; otherwise emit
    `NOT_MERGE_READY` with blockers.

### No-timeout command rule

Recovery commands run directly. These examples are valid:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest \
  test
```

These wrappers are invalid in recovery evidence:

```bash
timeout 600 qa/outside-in/alice-desktop/runners/validate-scenarios.sh
gtimeout 600 mvn test
perl -e 'alarm 600; exec @ARGV' mvn test
```

Use the CI system's normal job lifecycle and the operator's shell session
instead of adding outer command-level timeout wrappers to the evidence. Do not
remove or reinterpret scenario-level timeout fields that are already part of the
QA runner contract.

## Repository path resolution

The workflow resolves one authoritative repository path before running Git
checks, no-op detection, validation, or readiness reporting.

Resolution order:

1. Start from the explicit PR worktree path when one is supplied by the workflow
   invocation. The input may be the repository root or a directory inside that
   worktree.
2. Otherwise, start from the current working directory.
3. Resolve the candidate path to its Git top-level with
   `git -C "$input_path" rev-parse --show-toplevel`.
4. Use that verified top-level as `resolvedRepoPath` for every later Git and
   validation command.
5. Fail loudly if the candidate is not a Git worktree or the verified top-level
   is not the intended PR checkout.

The resolved path is part of the report evidence. A stale path, a path outside
the PR checkout, or a non-Git linked-worktree path is an error; the workflow must
not silently fall back to a different repository. An explicit subdirectory input
is valid only when its Git top-level is the intended PR checkout.

### Resolver API

| Field | Meaning |
| --- | --- |
| `inputPath` | The explicit path supplied by the workflow, or `current-working-directory` when no path was supplied. |
| `resolvedRepoPath` | Canonical Git top-level used for every Git and validation command. |
| `gitTopLevel` | Output from `git -C "$inputPath" rev-parse --show-toplevel`; it must match `resolvedRepoPath`. |
| `branch` | Output from `git -C "$resolvedRepoPath" branch --show-current`. |
| `headSha` | Output from `git -C "$resolvedRepoPath" rev-parse HEAD`. |
| `status` | `resolved` when the path is verified; otherwise `blocked`. |
| `blocker` | Machine-readable reason when resolution fails, such as `not-a-git-worktree` or `resolved-path-mismatch`. |

## No-op guard

The no-op guard determines whether the workflow made repository changes. It
runs only against the resolved repository path:

```bash
git -C "$repo_path" status --short
git -C "$repo_path" diff --stat
```

The guard reports the checked path, branch, head SHA, short status, and diff
summary. It must not inspect a cached checkout path, a stale linked-worktree
path, the session directory, or the directory from which the workflow launcher
happened to run.

### No-op outcomes

| Outcome | Meaning | Required report behavior |
| --- | --- | --- |
| `changes-present` | `git status --short` or `git diff --stat` shows modified files. | List the files in `Files modified` and include validation evidence. |
| `no-changes` | The resolved Git worktree is clean after recovery. | Emit a `Files modified` section with `None` as its body and include clean-tree readiness evidence. |
| `blocked` | The repository path cannot be verified or Git checks fail. | Emit the blocker and do not claim readiness. |

## Develop-head conflict recovery

When a pull request becomes dirty because the base branch moved, recovery uses
the current `origin/develop` head as the only base input. It updates the pull
request branch, resolves conflicts in that branch, and records the exact base
SHA used for the recovery. It never merges the pull request into `develop`, never
pushes to `develop`, and never treats a web/manual merge as evidence.

The recovery flow records these base facts:

| Field | Meaning |
| --- | --- |
| `baseBranch` | Target branch used for conflict recovery. For this repository's desktop Run recovery lane, the value is `develop`. |
| `baseHeadSha` | `git rev-parse origin/develop` at the time the recovery began. |
| `baseHeadVerified` | Whether the observed base SHA matched a task-supplied expected SHA when one was supplied. |
| `conflictResolutionMode` | `no-op`, `resolved-in-pr-branch`, or `blocked`. |
| `conflictedFiles` | Files that required conflict resolution, or `None` when the branch was already clean against the base. |

Resolution policy for shared documentation, scenario, and QA files is
develop-first unless that would remove the pull request's bounded evidence
contract. Restored pull request content must stay limited to the desktop
Run-window/debug evidence contract and must preserve non-claims for full world
execution, playback, visible rendering correctness, full UI automation, Save
completion, grading, Sims validation, deployed installer success, and broad UI
automation.

If the branch is already clean against current `origin/develop`, the workflow
must not manufacture a change. The report uses the no-op path and ties `Files
modified: None` to the current PR head, current develop head, current checks,
focused QA evidence, docs impact, PR wording review, quality-audit cycles, and
preserved bounded scope.

## PR metadata and conflicts

The merge-ready extension includes a read-only GitHub metadata check for
the PR head under review. This check is evidence for review language only; it
does not merge, rebase, push, resolve conflicts, create issues, or update pull
request state.

Use the GitHub CLI from the resolved repository context:

```bash
gh pr view "$pr_number" \
  --repo rysweet/RabbitHole \
  --json number,title,headRefName,headRefOid,baseRefName,mergeStateStatus,mergeable,state,isDraft,reviewDecision,statusCheckRollup,url
```

The report records the returned PR number, head branch, head SHA, base branch,
state, draft status, review decision, merge state, mergeability, and status check
rollup summary. The PR head SHA must match the resolved Git worktree `HEAD`
before the report can use local validation as current-head evidence.

### Conflict outcomes

| GitHub result | Required report behavior |
| --- | --- |
| `mergeable=CONFLICTING` or conflict-like `mergeStateStatus` | State that GitHub reports conflicts and do not claim merge readiness. Local validation may still be reported as current-head evidence. |
| Passing checks with conflicting mergeability | Report both facts. Passing checks do not override a GitHub conflict result. |
| Unknown, missing, or stale PR metadata | Treat merge readiness as blocked until fresh metadata is available. |
| Head SHA mismatch between GitHub and local worktree | Treat current-head validation as blocked for that PR head; fetch or switch to the intended head before reporting readiness. |

Conflict evidence belongs in `GitHub and PR evidence`, not in product feature
claims. A conflicting PR can still have a valid no-op recovery report when the
resolved worktree is clean and focused validation passed, but the report must
call the result review evidence rather than merge-ready evidence.

## Workflow report API

The implemented helper in `scripts/default_workflow_recovery.py` renders and
validates this section order:

```text
Summary
Files modified
Validation
Scope / bounded claims
Readiness evidence
```

The merge-ready extension keeps those concepts but expands the report
into explicit review gates:

```text
Summary
Files modified
Validation
QA / scenario evidence
Docs impact
Scope / bounded claims
GitHub and PR evidence
Quality-audit cycles
Readiness decision
```

`scripts/default_workflow_recovery.py` owns report rendering and validation. For
the merge-ready extension, the helper includes a read-only `gh` CLI service
adapter for GitHub PR metadata and workflow status. QA results, docs review, and
quality-audit cycles are still supplied explicitly to the report builder. The
helper must fail closed when required evidence is missing; it must not infer
readiness from absent GitHub or QA data.

Sections are always present in the relevant report shape. Empty sections use
`None` rather than being omitted.

### Summary

State the durable outcome in one or two sentences. For PR branch recovery,
record only completed, operator-authorized evidence: whether the local checkout
matched the current PR head, whether repository files changed, whether GitHub
reported mergeability blockers, and whether the focused work remains bounded to
the intended feature. Do not use the report summary as permission to merge
branches or resolve conflicts.

### Files modified

List every modified file relative to the resolved repository root. When the
workflow made no repository changes, write:

```text
Files modified
None
```

Do not omit this section. A missing `Files modified` section is an invalid
workflow report because reviewers cannot distinguish a clean no-op from an
incomplete report.

### Validation

List only commands that were actually run, with their result. For desktop Run
execution gap recovery, the focused validation set is:

```bash
git submodule update --init tweedle-lang

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh

NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest \
  test
```

If QA runner contracts were changed, include the relevant focused shell contract
test instead of implying the full desktop lane was exercised.

### Readiness evidence

The implemented helper records repository-local readiness facts in this section,
such as the resolved repository path, branch, head SHA, and clean short-branch
status. This section is not a merge approval by itself. The merge-ready
extension replaces this compact field with explicit `GitHub and PR evidence`,
`Quality-audit cycles`, and `Readiness decision` sections.

### QA / scenario evidence

List runnable evidence that was actually generated or validated. For
desktop-adjacent recovery, include the scenario validator and the focused
scenario or contract tests that match the changed files.

Acceptable evidence is command-backed:

```text
- validate-scenarios.sh: passed
- test-run-execution-gap-contract.sh: passed
- EatmeDesktopRunExecutionEvidenceTest: passed
```

Checklist generation, manual review directories, screenshots, or notes can be
listed only as bounded supporting evidence. They do not prove full UI
automation, visible rendering correctness, grading, creative assessment, full
lesson completion, full world execution, playback, Save completion, Sims
validation, deployed installer success, or full Tweedle/player decode.

### Docs impact

State whether the PR changes documentation, requires documentation updates, or
has no docs impact. When docs are relevant, name the exact docs files reviewed
or changed. When docs are not relevant, write an explicit no-docs-impact
justification tied to the focused diff.

Also review the pull request body or generated recovery text for overbroad
claims. PR text is acceptable only when it matches the runnable evidence and
uses bounded wording.

### Scope / bounded claims

Name the exact evidence boundary. For desktop Run execution gap recovery, the
accepted claim is limited to the presence and validation of the bounded Run
execution gap report and its blocker.

The report must explicitly avoid claiming:

```text
full world execution
playback
visible rendering correctness
full UI automation
Save completion
grading
Sims validation
deployed installer success
```

### GitHub and PR evidence

When the resolved repository is clean, include exact-head evidence:

```bash
git -C "$repo_path" rev-parse HEAD
git -C "$repo_path" status --short --branch
gh pr view "$pr_number" --repo rysweet/RabbitHole --json headRefOid,mergeStateStatus,mergeable,statusCheckRollup
```

Report the resulting SHA and the clean short-branch status. If the worktree is
not clean, list the remaining files instead of claiming readiness. If GitHub
reports conflicts, keep the local clean-head evidence but state that merge
readiness is blocked by the PR conflict result.

GitHub Actions evidence must be current for the same SHA:

```bash
gh run list \
  --repo rysweet/RabbitHole \
  --branch "$head_branch" \
  --commit "$head_sha" \
  --json databaseId,name,status,conclusion,headSha,url
```

Every required workflow for the PR must be `completed` with a successful
conclusion for `head_sha`. A queued, in-progress, skipped, cancelled, failed,
missing, or stale workflow is a `NOT_MERGE_READY` blocker unless the repository
explicitly documents that the check is non-required for this PR type.

For conflict recovery, include the base head that made the branch clean:

```bash
git -C "$repo_path" rev-parse origin/develop
```

If the recovery task supplied an expected develop SHA, the report must state
whether the observed `origin/develop` SHA matched it before resolution began.

### Quality-audit cycles

Record at least three SEEK / VALIDATE / FIX cycles. Each cycle must have all
three fields:

| Field | Meaning |
| --- | --- |
| `SEEK` | The risk, missing evidence, overclaim, stale check, scope issue, or docs concern being inspected. |
| `VALIDATE` | The exact command, GitHub query, diff review, document review, or artifact check used to verify the risk. |
| `FIX` | The repository change, verified no-op decision, or explicit blocker recorded for that cycle. |

The final cycle must be clean before `MERGE_READY` is allowed. A clean final
cycle means the cycle found no new missing evidence, no unsupported claims, no
dirty files, no stale checks, and no unfixed scope or documentation issue.

Example:

```text
Quality-audit cycles
1. SEEK: Verify local evidence is tied to the current PR head.
   VALIDATE: Compared git rev-parse HEAD with gh pr view headRefOid.
   FIX: No-op; SHAs matched.
2. SEEK: Check bounded desktop Run claims.
   VALIDATE: Reviewed desktop-run-execution-gap-report.json wording and PR body.
   FIX: No-op; wording stayed limited to bounded Run-window evidence.
3. SEEK: Confirm no remaining merge-ready blockers.
   VALIDATE: Checked clean worktree, current-head green Actions, focused QA,
   docs impact, diff scope, and PR evidence.
   FIX: Clean final cycle; no blockers found.
```

If the third cycle finds a blocker, the decision is `NOT_MERGE_READY`. Record
the blocker; do not add a fourth success-shaped cycle unless the blocker is
actually resolved and validated.

### Readiness decision

The final section contains exactly one decision token:

```text
MERGE_READY
```

or:

```text
NOT_MERGE_READY
```

`MERGE_READY` requires every gate in [No-timeout merge-ready recovery](#no-timeout-merge-ready-recovery).
`NOT_MERGE_READY` requires a blocker list. Common blockers include:

```text
- local HEAD does not match current PR headRefOid
- GitHub Actions are missing, stale, failed, cancelled, skipped, queued, or in progress
- GitHub reports CONFLICTING, DIRTY, UNKNOWN, or blocked mergeability
- runnable QA or focused scenario evidence was not run
- docs impact or PR description evidence was not reviewed
- diff contains unrelated scope
- fewer than three quality-audit cycles are documented
- final quality-audit cycle is not clean
- PR text overclaims UI automation, rendering, grading, lesson completion,
  full world execution, full Tweedle/player decode, or similar unproven behavior
```

## Configuration

| Configuration | Required behavior |
| --- | --- |
| Explicit PR worktree path | Preferred source for `repo_path`; may be the repo root or a subdirectory, and must resolve through `git -C "$input_path" rev-parse --show-toplevel`. |
| Current working directory | Fallback only when no explicit PR worktree path is supplied; resolved through the same Git top-level command. |
| Target branch | Base for diff review, GitHub mergeability evidence, and conflict recovery into the PR branch. No-timeout recovery does not merge the PR into the target branch. |
| PR number | Used only for read-only GitHub metadata evidence through `gh pr view`; conflict status blocks merge-readiness claims. |
| `NODE_OPTIONS` | Use `--max-old-space-size=32768` for focused Node-adjacent QA commands in this repository. |
| Timeout wrappers | Outer command-level wrappers are not allowed in recovery evidence. Run commands directly; do not wrap them with `timeout`, `gtimeout`, alarm scripts, or equivalent shims. Existing scenario timeout fields and internal runner guards remain valid. |
| Quality-audit cycle count | At least three cycles are required; the final cycle must be clean before `MERGE_READY`. |
| No-op recovery | Allowed only when tied to the current PR head, current develop head, current checks, reviewed evidence, preserved bounded scope, and either all gates pass or explicit `NOT_MERGE_READY` blockers are listed. |

Paths are untrusted input. The workflow passes them as Git `-C` arguments and
does not construct commands with `eval`, dynamic shell expansion, or unchecked
string interpolation.

GitHub evidence is collected through a service adapter, not by interpolating PR
metadata into a shell command. Transient external failures may be retried, but
failed PR metadata or workflow collection is surfaced as a `NOT_MERGE_READY`
blocker rather than a success-shaped default.

## Examples

### Clean recovery report

```text
Summary
Default-workflow recovery evaluated the existing PR branch at the current PR
head with no outer command-level timeout wrappers. All merge-ready gates passed
for the exact head listed below.

Files modified
None

Validation
- git submodule update --init tweedle-lang: passed
- NODE_OPTIONS=--max-old-space-size=32768
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh: passed
- NODE_OPTIONS=--max-old-space-size=32768
  qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh: passed
- NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ide -am
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest test: passed

QA / scenario evidence
- Scenario schema validation passed.
- Focused desktop Run execution gap contract passed.

Docs impact
- Reviewed docs/reference/desktop-run-execution-gap-report.md.
- PR description evidence uses bounded Run-window wording.

Scope / bounded claims
Validated only the desktop Run execution gap report contract. This does not
claim full world execution, playback, visible rendering correctness, full UI
automation, Save completion, grading, Sims validation, or deployed installer
success.

GitHub and PR evidence
- repoPath: /worktrees/wave6-run-execution-gap
- branch: wave6-run-execution-gap-1778302300
- headSha: 0123456789abcdef0123456789abcdef01234567
- baseBranch: develop
- baseHeadSha: fedcba9876543210fedcba9876543210fedcba98
- conflictResolutionMode: no-op
- git status --short --branch: ## wave6-run-execution-gap-1778302300...origin/wave6-run-execution-gap-1778302300
- pullRequest: #404
- prHeadSha: 0123456789abcdef0123456789abcdef01234567
- mergeable: MERGEABLE
- mergeStateStatus: CLEAN
- required GitHub Actions: completed successfully for headSha

Quality-audit cycles
1. SEEK: Head identity.
   VALIDATE: Compared local HEAD with PR headRefOid.
   FIX: No-op; exact match.
2. SEEK: QA and claim boundary.
   VALIDATE: Ran focused QA and reviewed bounded claim text.
   FIX: No-op; no overclaim found.
3. SEEK: Final merge-ready gate.
   VALIDATE: Rechecked clean worktree, current green Actions, docs impact,
   focused diff, and PR evidence.
   FIX: Clean final cycle.

Readiness decision
MERGE_READY
```

If GitHub reports `mergeable=CONFLICTING`, replace the merge-ready sentence in
the summary with conflict-blocked wording while keeping the exact local evidence:

```text
Summary
Default-workflow recovery checked the resolved PR worktree with no repository
changes. Current-head validation is available for review, but GitHub reports the
pull request as conflicting, so this report does not claim merge readiness.

Readiness decision
NOT_MERGE_READY

Blockers
- GitHub reports mergeable=CONFLICTING.
```

### Recovery report with documentation changes

```text
Files modified
- docs/reference/desktop-run-execution-gap-report.md
- docs/reference/default-workflow-recovery-report.md
- docs/index.md
```

Keep the same section even when only documentation changed. Reviewers should not
need to infer modified files from Git output outside the report.

## Review rules

Rules 1-6 apply to the base helper. Rules 7-13 apply to the merge-ready
extension.

1. Require the report to name the resolved repository path and verify it with
   `git -C "$repo_path" rev-parse --show-toplevel`.
2. Reject reports whose no-op guard inspected a stale path, session directory, or
   non-Git path.
3. Require `Files modified` in every report, with `None` when the resolved
   worktree is clean.
4. Require validation commands to be exact commands that were run.
5. Require bounded-claim text for feature-specific evidence.
6. Require final HEAD SHA and `git status --short --branch` evidence before
   claiming clean readiness.
7. Require read-only GitHub PR metadata before making any merge-readiness claim.
8. Reject merge-ready wording when GitHub reports conflicts, even if local
   validation and pull request checks passed.
9. Reject reports that use outer shell timeout wrappers or equivalent command
   wrappers as part of recovery evidence.
10. Require focused runnable QA or scenario evidence when the PR scope touches QA,
    scenarios, desktop evidence, or feature contracts.
11. Require docs impact and PR description evidence review before readiness.
12. Require at least three quality-audit SEEK / VALIDATE / FIX cycles, with a
    clean final cycle.
13. Require `NOT_MERGE_READY` blockers whenever any gate is missing or partial.
14. For conflict recovery, require the report to name the current `origin/develop`
    head used for resolution and reject claims based on an unverified or stale
    base head.
15. Reject any recovery report that implies the pull request was merged into
    `develop`, pushed to `develop`, or manually merged through GitHub as part of
    evidence collection.

## Troubleshooting

| Symptom | Meaning | Next check |
| --- | --- | --- |
| No-op guard says clean but the PR branch has changes | The guard likely checked the wrong path. | Compare `resolvedRepoPath`, `gitTopLevel`, and `git -C "$repo_path" status --short`. |
| `Files modified` is missing | The report is invalid. | Emit the required section with file paths or `None`. |
| GitHub and PR evidence names a SHA but status is dirty | The branch is not ready. | List remaining files and rerun focused validation after resolving them. |
| GitHub reports `mergeable=CONFLICTING` | The PR cannot be reported as merge-ready from this workflow pass. | State that review evidence exists but merge readiness is blocked by conflicts. |
| PR head SHA differs from local `HEAD` | The validation is not tied to the current PR head. | Fetch or switch to the PR head before reporting current-head evidence. |
| Report claims full execution or rendering correctness | The workflow overclaims the desktop Run evidence. | Replace the claim with bounded Run-window evidence and cite the execution gap blocker. |
| `git -C "$repo_path" rev-parse --show-toplevel` fails | The supplied path is not a valid worktree. | Stop recovery and correct the explicit PR worktree path. |
| GitHub Actions are green but QA evidence is missing | CI is necessary but not sufficient for merge readiness. | Run or list the focused runnable QA evidence, or emit a `NOT_MERGE_READY` blocker. |
| Only two quality-audit cycles are documented | The report is incomplete. | Add a third SEEK / VALIDATE / FIX cycle; if it is not clean, emit `NOT_MERGE_READY`. |
| A recovery evidence command is wrapped with `timeout` or `gtimeout` | The evidence violates no-timeout recovery. | Rerun the command directly or treat the gate as blocked; do not remove scenario-level timeout fields from QA definitions. |
| No-op report omits the current develop head | The clean-tree claim is not tied to the base that made the PR clean. | Record `git rev-parse origin/develop` and rerun the no-op guard against the resolved PR worktree. |
