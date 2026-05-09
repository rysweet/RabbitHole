# Default workflow recovery report

This reference describes the recovery workflow report contract used when a
default workflow resumes work on an existing pull request branch. The contract
keeps repository checks tied to the actual Git worktree under review and keeps
the final report structured, even when no files changed.

## Contents

- [Scope](#scope)
- [Repository path resolution](#repository-path-resolution)
- [No-op guard](#no-op-guard)
- [PR metadata and conflicts](#pr-metadata-and-conflicts)
- [Workflow report API](#workflow-report-api)
- [Configuration](#configuration)
- [Examples](#examples)
- [Review rules](#review-rules)
- [Troubleshooting](#troubleshooting)

## Scope

Use this report contract for recovery work that continues an existing PR branch
without rewriting PR history. The workflow may merge the current target branch
into the PR branch, resolve only relevant conflicts, run focused validation, and
emit readiness evidence for the exact checked head.

The report is a workflow artifact. It does not expand product claims, desktop QA
claims, Run execution claims, visible rendering claims, Save claims, grading
claims, Sims validation claims, or deployed installer claims. Product evidence
must come from the focused artifacts documented by the relevant feature
reference, such as [Desktop Run execution gap report](./desktop-run-execution-gap-report.md).

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

## PR metadata and conflicts

Recovery for an existing pull request includes a read-only GitHub metadata check
for the PR head under review. This check is evidence for review language only; it
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

Conflict evidence belongs in `Readiness evidence`, not in product feature
claims. A conflicting PR can still have a valid no-op recovery report when the
resolved worktree is clean and focused validation passed, but the report must
call the result review evidence rather than merge-ready evidence.

## Workflow report API

Every recovery report uses the same top-level sections. Sections are always
present; empty sections use `None` rather than being omitted.

```text
Summary
Files modified
Validation
Scope / bounded claims
Readiness evidence
```

### Summary

State the durable outcome in one or two sentences. For PR branch recovery,
include whether the branch was updated from the target branch, whether conflicts
were resolved, and whether the focused work remains bounded to the intended
feature.

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

NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest \
  test
```

If QA runner contracts were changed, include the relevant focused shell contract
test instead of implying the full desktop lane was exercised.

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

### Readiness evidence

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

## Configuration

| Configuration | Required behavior |
| --- | --- |
| Explicit PR worktree path | Preferred source for `repo_path`; may be the repo root or a subdirectory, and must resolve through `git -C "$input_path" rev-parse --show-toplevel`. |
| Current working directory | Fallback only when no explicit PR worktree path is supplied; resolved through the same Git top-level command. |
| Target branch | Merged into the PR branch when recovery requires current target-branch content without rewriting PR history. |
| PR number | Used only for read-only GitHub metadata evidence through `gh pr view`; conflict status blocks merge-readiness claims. |
| `NODE_OPTIONS` | Use `--max-old-space-size=32768` for focused Node-adjacent QA commands in this repository. |

Paths are untrusted input. The workflow passes them as Git `-C` arguments and
does not construct commands with `eval`, dynamic shell expansion, or unchecked
string interpolation.

## Examples

### Clean recovery report

```text
Summary
Merged current develop into the existing PR branch and kept the desktop Run
execution gap scope unchanged. The branch is ready for review at the exact head
listed below.

Files modified
None

Validation
- git submodule update --init tweedle-lang: passed
- NODE_OPTIONS=--max-old-space-size=32768
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh: passed
- NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ide -am
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest test: passed

Scope / bounded claims
Validated only the desktop Run execution gap report contract. This does not
claim full world execution, playback, visible rendering correctness, full UI
automation, Save completion, grading, Sims validation, or deployed installer
success.

Readiness evidence
- repoPath: /worktrees/wave6-run-execution-gap
- branch: wave6-run-execution-gap-1778302300
- headSha: 0123456789abcdef0123456789abcdef01234567
- git status --short --branch: ## wave6-run-execution-gap-1778302300...origin/wave6-run-execution-gap-1778302300
- pullRequest: #404
- prHeadSha: 0123456789abcdef0123456789abcdef01234567
- mergeable: MERGEABLE
- mergeStateStatus: CLEAN
```

If GitHub reports `mergeable=CONFLICTING`, replace the merge-ready sentence in
the summary with conflict-blocked wording while keeping the exact local evidence:

```text
Summary
Default-workflow recovery checked the resolved PR worktree with no repository
changes. Current-head validation is available for review, but GitHub reports the
pull request as conflicting, so this report does not claim merge readiness.
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

## Troubleshooting

| Symptom | Meaning | Next check |
| --- | --- | --- |
| No-op guard says clean but the PR branch has changes | The guard likely checked the wrong path. | Compare `resolvedRepoPath`, `gitTopLevel`, and `git -C "$repo_path" status --short`. |
| `Files modified` is missing | The report is invalid. | Emit the required section with file paths or `None`. |
| Readiness evidence names a SHA but status is dirty | The branch is not ready. | List remaining files and rerun focused validation after resolving them. |
| GitHub reports `mergeable=CONFLICTING` | The PR cannot be reported as merge-ready from this workflow pass. | State that review evidence exists but merge readiness is blocked by conflicts. |
| PR head SHA differs from local `HEAD` | The validation is not tied to the current PR head. | Fetch or switch to the PR head before reporting current-head evidence. |
| Report claims full execution or rendering correctness | The workflow overclaims the desktop Run evidence. | Replace the claim with bounded Run-window evidence and cite the execution gap blocker. |
| `git -C "$repo_path" rev-parse --show-toplevel` fails | The supplied path is not a valid worktree. | Stop recovery and correct the explicit PR worktree path. |
