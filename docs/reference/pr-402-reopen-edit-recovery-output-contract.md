# PR 402 Reopen/Edit Recovery Output Contract

This reference defines the finished default-workflow recovery output contract for
PR 402 when the project archive reopen/edit chain is already ready at the exact
pull request head.

Use it with
[Project Archive Reopen/Edit Seam](./project-archive-reopen-edit-seam.md) and
[Validate the Project Archive Reopen/Edit Seam](../howto/validate-project-archive-reopen-edit-seam.md).

## Contents

- [Contract](#contract)
- [Configuration](#configuration)
- [Evidence command API](#evidence-command-api)
- [Accepted recovery outputs](#accepted-recovery-outputs)
- [Exact-head no-op template](#exact-head-no-op-template)
- [Failure handling](#failure-handling)
- [Boundaries](#boundaries)

## Contract

The PR 402 recovery path is a workflow output contract, not a new archive IO
feature. It recovers a default-workflow implementation step that produced no file
changes by requiring one of two explicit outcomes:

| Outcome | Required output |
| --- | --- |
| Repository changes are needed | `Files modified:` followed by the relative paths changed for project archive reopen/edit readiness. |
| No repository changes are needed | `No-op justification:` tied to the exact PR head, current check state, focused validation, diff scope, merge-base state, and clean worktree evidence. |

A clean worktree is not enough. A zero-change recovery is valid only when the
implementation output contains the explicit `No-op justification:` section and
references the same 40-character head used for PR, local, and remote branch
evidence.

For this recovery, capture the required evidence anchor from the current refs:

```text
PR: 402
Branch: wave6-project-reopen-edit-chain-1778302300
Base: develop
Required head: <exact current value from gh pr view 402 --json headRefOid --jq .headRefOid>
Required merge-base/origin develop: <exact current value from git rev-parse origin/develop>
```

## Configuration

Run recovery commands from the repository root. Use the saved Node memory
preference for Maven-backed validation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Initialize the Tweedle grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Do not use timeout wrapper commands for this recovery. Do not manually merge PR
402. If the branch needs `develop`, merge `origin/develop` normally into the PR
branch, push that branch, and then refresh PR head and check evidence before
recording readiness.

## Evidence command API

Refresh remote refs before comparing branch state so stale local refs cannot
satisfy the evidence contract:

```bash
git fetch origin --prune
```

Collect exact-head evidence with read-only git and GitHub inspection plus focused
archive IO validation:

```bash
local_head="$(git rev-parse HEAD)"
pr_head="$(gh pr view 402 --json headRefOid --jq .headRefOid)"
remote_branch_head="$(git rev-parse origin/wave6-project-reopen-edit-chain-1778302300)"
origin_develop_head="$(git rev-parse origin/develop)"
merge_base="$(git merge-base HEAD origin/develop)"
worktree_status="$(git status --short)"

test "$local_head" = "$pr_head"
test "$local_head" = "$remote_branch_head"
test -n "$origin_develop_head"
test "$merge_base" = "$origin_develop_head"
```

Inspect PR checks:

```bash
gh pr checks 402
```

The recovery output may cite the PR checks as green only when these checks are
successful at the exact current PR head:

```text
GitGuardian Security Checks
Alice Checkstyle CI/build (pull_request)
Alice Coverage Reports/coverage (pull_request)
Alice NetBeans Package CI/package-netbeans (pull_request)
Alice Test CI/test (pull_request)
```

Run the focused archive reopen/edit characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=IoUtilitiesTest \
  test
```

Use the no-op guard at the final evidence step:

```bash
scripts/project-archive-reopen-edit-noop-guard.sh .
```

For a clean worktree, validate the final evidence file against the exact head:

```bash
scripts/project-archive-reopen-edit-noop-guard.sh . \
  --allow-noop-evidence readiness-evidence.md \
  --expected-head "$(git rev-parse HEAD)"
```

## Accepted recovery outputs

When files change, use this shape:

```text
Files modified:
  docs/reference/pr-402-reopen-edit-recovery-output-contract.md
  <other relative paths>
```

List only files changed by the recovery step. Keep changes scoped to project
archive reopen/edit readiness, its characterization tests, its docs, or its
no-op guard.

When no files change, replace `Files modified:` with this shape:

```text
PR head: <current PR head>
Local HEAD: <current PR head>
Remote branch HEAD: <current PR head>
Checks:
  GitGuardian Security Checks successful at current PR head
  Alice Checkstyle CI/build (pull_request) successful at current PR head
  Alice Coverage Reports/coverage (pull_request) successful at current PR head
  Alice NetBeans Package CI/package-netbeans (pull_request) successful at current PR head
  Alice Test CI/test (pull_request) successful at current PR head
No-op justification:
  PR 402 branch wave6-project-reopen-edit-chain-1778302300 already points at
  <current PR head>, local HEAD matches both the PR head
  and remote branch head, origin/develop is
  <current origin/develop head>, merge-base is
  <current origin/develop head>, merge-base equals origin/develop,
  the origin/develop...HEAD diff is limited to project archive reopen/edit
  characterization/readiness surfaces, focused archive reopen/edit validation
  passed at <current PR head>, and the current PR checks
  GitGuardian Security Checks, Alice Checkstyle CI/build (pull_request), Alice
  Coverage Reports/coverage (pull_request), Alice NetBeans Package
  CI/package-netbeans (pull_request), and Alice Test CI/test (pull_request) are
  successful with no scoped PR check blocker requiring a code or docs change.
Positive claim scope:
  Exact-head workflow recovery evidence for the repository-owned project archive
  reopen/edit seam.
Scope exclusions:
  full desktop lesson automation
  full UI automation
  desktop save-menu completion
  visible rendering correctness
  grading
  full save completion
  full first-lesson completion
  player runtime behavior
Stale evidence note:
  This no-op evidence is valid only for PR head, local HEAD, and remote branch
  HEAD <current PR head> with the listed PR checks green;
  refresh refs, rerun validation, and regenerate the evidence if any SHA or
  check state changes.
```

Do not include both `Files modified:` and `No-op justification:` in the same
final implementation output.

## Exact-head no-op template

This template is valid only after replacing every placeholder with the exact PR
head, branch head, merge-base, and check state collected for the current run. It
becomes stale if any SHA changes or any PR check stops being successful.

```text
PR: 402
Branch: wave6-project-reopen-edit-chain-1778302300
Base: develop
PR head: <current PR head>
Local HEAD: <current PR head>
Remote branch HEAD: <current PR head>
origin/develop HEAD: <current origin/develop head>
Merge-base: <current merge-base>
Merge-base status: merge-base equals origin/develop
Worktree status: clean
Checks:
  GitGuardian Security Checks successful at current PR head
  Alice Checkstyle CI/build (pull_request) successful at current PR head
  Alice Coverage Reports/coverage (pull_request) successful at current PR head
  Alice NetBeans Package CI/package-netbeans (pull_request) successful at current PR head
  Alice Test CI/test (pull_request) successful at current PR head
Validation:
  NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
  -Dtest=IoUtilitiesTest test
Result: passed with exit code 0
No-op justification:
  PR 402 branch wave6-project-reopen-edit-chain-1778302300 already points at
  <current PR head>, local HEAD matches both the PR head
  and remote branch head, origin/develop is
  <current origin/develop head>, merge-base is
  <current merge-base>, merge-base equals origin/develop,
  the origin/develop...HEAD diff is limited to project archive reopen/edit
  characterization/readiness surfaces, focused archive reopen/edit validation
  passed at <current PR head>, and the current PR checks
  GitGuardian Security Checks, Alice Checkstyle CI/build (pull_request), Alice
  Coverage Reports/coverage (pull_request), Alice NetBeans Package
  CI/package-netbeans (pull_request), and Alice Test CI/test (pull_request) are
  successful with no scoped PR check blocker requiring a code or docs change.
Positive claim scope:
  Exact-head workflow recovery evidence for the repository-owned project archive
  reopen/edit seam.
Scope exclusions:
  full desktop lesson automation
  full UI automation
  desktop save-menu completion
  visible rendering correctness
  grading
  full save completion
  full first-lesson completion
  player runtime behavior
Stale evidence note:
  This no-op evidence is valid only for PR head, local HEAD, and remote branch
  HEAD <current PR head> with the listed PR checks green;
  refresh refs, rerun validation, and regenerate the evidence if any SHA or
  check state changes.
```

## Failure handling

If any exact-head evidence differs, do not use the no-op output. Make the
smallest repository change needed for the project archive reopen/edit seam and
report it under `Files modified:`.

| Failure | Required response |
| --- | --- |
| PR head, local HEAD, or remote branch head differ | Sync or push the PR branch normally, then collect fresh evidence. |
| Merge-base differs from `origin/develop` | Merge `origin/develop` minimally unless the branch already contains that base. |
| Worktree is dirty before recovery | Preserve unrelated work; list only reviewed recovery-scope paths changed by this step. |
| Focused validation fails | Fix the archive reopen/edit seam or its characterization test. |
| PR check is failing | Fix only scoped blockers tied to project archive reopen/edit readiness. |
| Check is pending | Report it as pending; do not convert it into a readiness claim. |

## Boundaries

This recovery output contract does not claim full desktop lesson automation,
full UI automation, desktop save-menu completion, visible rendering correctness,
grading, full save completion, full first-lesson completion, or player runtime
behavior. It also does not claim native or Swing file chooser automation. It
only describes exact-head workflow recovery evidence for the repository-owned
project archive reopen/edit seam.
