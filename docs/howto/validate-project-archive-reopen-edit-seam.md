# Validate the Project Archive Reopen/Edit Seam

Use this guide to validate the repository-owned project archive IO seam for
saving an editable `.a3p`, reopening it, editing project-owned state, saving it
again, reopening it again, and exporting the edited project as `.a3w`.

For the full behavior contract, see
[Project Archive Reopen/Edit Seam](../reference/project-archive-reopen-edit-seam.md).
For a guided walkthrough, see
[Tutorial: Trace the Project Archive Reopen/Edit Seam](../tutorials/trace-project-archive-reopen-edit-seam.md).

## Contents

- [Prerequisites](#prerequisites)
- [Prepare the worktree](#prepare-the-worktree)
- [Sync PR 402 recovery branch](#sync-pr-402-recovery-branch)
- [Inspect the exact diff](#inspect-the-exact-diff)
- [Run primary validation](#run-primary-validation)
- [Run headless bridge validation when needed](#run-headless-bridge-validation-when-needed)
- [Run compatibility validation when needed](#run-compatibility-validation-when-needed)
- [Inspect PR checks](#inspect-pr-checks)
- [Use the no-op guard](#use-the-no-op-guard)
- [Review failures](#review-failures)
- [Record readiness evidence](#record-readiness-evidence)
- [Review claims](#review-claims)

## Prerequisites

Run commands from a git-linked checkout or worktree of this repository. The
focused seam lives in `core/story-api-migration`:

```text
core/story-api-migration/src/main/java/org/lgna/project/io/IoUtilities.java
core/story-api-migration/src/main/java/org/lgna/project/io/XmlProjectIo.java
core/story-api-migration/src/main/java/org/lgna/project/io/JsonProjectIo.java
core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

Use the saved Node memory preference:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Prepare the worktree

Resolve the repository root through git and initialize the Tweedle grammar
submodule there:

```bash
repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Preserve unrelated worktree state. An untracked `default-workflow-attempt.log`
is not part of this archive IO seam unless it is already a required repository
artifact.

Record the local worktree state before making sync or readiness decisions:

```bash
git status --short
```

Resolve, commit, or explicitly preserve unrelated local changes before treating
them as recovery evidence.

## Sync PR 402 recovery branch

Fetch the integration branch and PR branch:

```bash
git fetch origin develop wave6-project-reopen-edit-chain-1778302300
```

Switch to the local recovery branch. If the branch does not exist in the
worktree yet, create it from the remote tracking branch:

```bash
git switch wave6-project-reopen-edit-chain-1778302300
# If that fails because the local branch does not exist:
git switch --track origin/wave6-project-reopen-edit-chain-1778302300
```

Confirm PR 402 still points at this branch:

```bash
gh pr view 402 --json number,headRefName,headRefOid,headRepositoryOwner,baseRefName
```

Record the exact local, remote, base, and merge-base commits before making
readiness claims:

```bash
local_head="$(git rev-parse HEAD)"
pr_head="$(gh pr view 402 --json headRefOid --jq .headRefOid)"
remote_branch_head="$(git rev-parse origin/wave6-project-reopen-edit-chain-1778302300)"
origin_develop_head="$(git rev-parse origin/develop)"
merge_base="$(git merge-base HEAD origin/develop)"
worktree_status="$(git status --short)"
```

Confirm the local worktree is the checked-out PR head and remote branch head:

```bash
test "$local_head" = "$pr_head"
test "$local_head" = "$remote_branch_head"
```

Account for the recovery baseline:

```bash
git merge-base --is-ancestor 50b4d8687a42 HEAD
```

Do not integrate `develop` when the branch already contains current
`origin/develop`:

```bash
test "$merge_base" = "$origin_develop_head"
```

If that test passes, record `merge-base equals origin/develop`. If it fails,
merge `origin/develop` minimally rather than rebasing or force-pushing the
published PR branch:

```bash
git merge --no-ff origin/develop
```

Resolve only conflicts tied to repository-owned project archive IO seams:

```text
core/story-api-migration/src/main/java/org/lgna/project/io/
core/story-api-migration/src/test/java/org/lgna/project/io/
docs/reference/project-archive-reopen-edit-seam.md
docs/howto/validate-project-archive-reopen-edit-seam.md
docs/tutorials/trace-project-archive-reopen-edit-seam.md
```

Do not use this recovery path for unrelated desktop QA, rendering, grading,
first-lesson, or broad migration-manager work.

After any merge or committed local recovery change creates a new local `HEAD`,
push the recovery branch normally so PR 402 points at the commit that will be
validated:

```bash
git push origin HEAD:wave6-project-reopen-edit-chain-1778302300
```

Then refresh PR head evidence and compare it to the local commit before running
or recording final evidence:

```bash
pr_head="$(gh pr view 402 --json headRefOid --jq .headRefOid)"
remote_branch_head="$(git rev-parse origin/wave6-project-reopen-edit-chain-1778302300)"
local_head="$(git rev-parse HEAD)"
test "$local_head" = "$pr_head"
test "$local_head" = "$remote_branch_head"
```

Do not record readiness for a local-only merge commit, stale remote branch head,
or stale PR head.

## Inspect the exact diff

Review the exact pull request diff after any required sync:

```bash
git status --short
git diff --stat origin/develop...HEAD
git diff --name-status origin/develop...HEAD
```

Group the changed files in readiness evidence:

| Group | Expected paths |
| --- | --- |
| Implementation | `core/story-api-migration/src/main/java/org/lgna/project/io/` when archive reader/writer behavior changed. |
| Characterization test | `core/story-api-migration/src/test/java/org/lgna/project/io/` for focused reopen/edit or compatibility coverage. |
| Headless IDE bridge | `core/ide/src/test/java/org/alice/ide/ProjectOpenSaveExportJourneyTest.java` when `FileProjectLoader` or `ProjectFileUtilities` handoff behavior changed. |
| QA metadata | Repository-owned QA scenario, schema, or runner files only when they directly support this archive IO seam. |
| Documentation | `docs/reference/project-archive-reopen-edit-seam.md`, this guide, and the matching tutorial. |
| Guard scope | `scripts/project-archive-reopen-edit-noop-guard.sh` and `tests/test_project_archive_reopen_edit_noop_guard.py` when the no-op guard itself changes. |

Stop and narrow the work before recording readiness if the diff includes
unrelated desktop Save completion, full UI automation, visible rendering,
grading, broad historical compatibility, or first-lesson completion changes.

## Run primary validation

Run the canonical reopen/edit seam characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=IoUtilitiesTest \
  test
```

The primary test is
`IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`.
It must prove:

```text
write .a3p -> read .a3p -> edit Project -> write edited .a3p
-> read edited .a3p -> inspect edited .a3p manifest
-> export .a3w -> inspect exported .a3w manifest and Tweedle source entry
```

The seam is not complete if the test only proves file creation, first reopen, or
export existence. The edited program type must survive the second
`IoUtilities.readProject` call.

## Run headless bridge validation when needed

Run the `core/ide` bridge test when the diff includes
`ProjectOpenSaveExportJourneyTest.java`, `FileProjectLoader`, or
`ProjectFileUtilities` handoff behavior:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.ProjectOpenSaveExportJourneyTest \
  test
```

This validates only the headless loaded-project bridge. It does not replace the
primary `IoUtilitiesTest` archive seam validation and does not prove desktop
Save-menu completion, Save dialog automation, rendering, grading, lesson
completion, or player runtime behavior.

## Run compatibility validation when needed

Run the historical archive round-trip guard when the change touches archive
parsing, archive writing, XML fallback, JSON manifest handling, Tweedle decode,
resource entries, or `.a3c`/`.a3w` compatibility:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=HistoricalArchiveRoundTripCharacterizationTest \
  test
```

If no archive parsing or writing code changed and the primary validation passes,
the historical guard is optional for this narrow PR recovery.

## Inspect PR checks

Inspect PR check state after validation:

```bash
gh pr checks 402
```

Resolve only failing checks that are directly tied to project archive
reopen/edit readiness. Report pending checks as pending. Do not turn unrelated
desktop, rendering, grading, Save-completion, or first-lesson check state into a
claim about this seam.

## Use the no-op guard

Run the guard when the recovery workflow reaches the final evidence step:

```bash
scripts/project-archive-reopen-edit-noop-guard.sh .
```

Interpret it this way:

| Result | Evidence requirement |
| --- | --- |
| Exit `0` | The worktree has uncommitted changes; list the relative paths under `Files modified` after reviewing that they are scoped to this seam. |
| Exit `1` | The worktree is clean; include **No-op justification** tied to the exact current `HEAD`, merge-base status, diff scope, validation, and PR checks. |
| Exit `2` or `64` | Fix the command path or arguments before recording readiness evidence. |

Use `--print-root` to confirm the guard is evaluating the intended linked
worktree:

```bash
scripts/project-archive-reopen-edit-noop-guard.sh core/story-api-migration --print-root
```

To make a clean-worktree no-op explicit and machine-checkable, save the final
evidence text and pass the exact current head. The evidence must use
`No-op justification` instead of `Files modified`, and the justification must
reference that same head:

```bash
scripts/project-archive-reopen-edit-noop-guard.sh . \
  --allow-noop-evidence readiness-evidence.md \
  --expected-head "$(git rev-parse HEAD)"
```

## Review failures

Use the failing assertion to choose the smallest responsible seam:

| Failure area | Review surface |
| --- | --- |
| Original `.a3p` does not reopen | `IoUtilities.projectReader(File)`, `IoUtilities.readProject(File)`, and `.a3p` XML fallback routing. |
| Edited name does not survive the second reopen | `IoUtilities.writeProject`, `XmlProjectIo`, project manifest creation, or XML program payload writing. |
| Edited `.a3p` manifest is stale | Project manifest creation and write path for editable project archives. |
| Exported `.a3w` manifest or Tweedle entry is stale | `IoUtilities.exportProject`, `JsonProjectIo`, and Tweedle source export naming. |
| Corrupt or unsupported archives read successfully | `IoUtilities` reader selection and JSON/XML failure handling. |
| Historical `.a3c` or `.a3w` guard fails | `HistoricalArchiveRoundTripCharacterizationTest` and the touched archive routing/writer surface. |

Fix only repository-owned project archive IO behavior needed for these tests. Do
not add broad fallback behavior that turns malformed archives into partial
successes.

Archive inputs are untrusted. Do not relax archive entry safety, do not add XML
parsing that resolves external entities or remote resources, and do not record
full archive contents, full manifests, source payloads, or resource bytes in PR
readiness evidence.

## Record readiness evidence

Record readiness after sync and validation at the exact final HEAD:

```bash
final_head="$(git rev-parse HEAD)"
pr_head="$(gh pr view 402 --json headRefOid --jq .headRefOid)"
test "$final_head" = "$pr_head"
git status --short
```

Use this evidence shape:

```text
PR: 402
Branch: wave6-project-reopen-edit-chain-1778302300
Base: develop
PR head: <exact SHA from gh pr view 402 --json headRefOid --jq .headRefOid>
Local HEAD: <exact git rev-parse HEAD value>
Remote branch HEAD: <exact git rev-parse origin/wave6-project-reopen-edit-chain-1778302300 value>
origin/develop HEAD: <exact git rev-parse origin/develop value>
Merge-base: <exact git merge-base HEAD origin/develop value>
Merge-base status: <merge-base equals origin/develop | merged origin/develop, with exact base/merge-base SHAs above>
Worktree status: <clean | exact git status --short entries reviewed as recovery scope>
Diff summary:
  Implementation: <paths or none>
  Characterization test: <paths or none>
  Headless IDE bridge: <paths or none>
  QA metadata: <paths or none>
  Documentation: <paths or none>
  Guard scope: <paths or none>
Validation:
  NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=IoUtilitiesTest test
Result: <passed with exit code 0 | failed with exit code N and blocker summary>
Headless bridge validation: <not run; no core/ide bridge surface changed | command and result>
Compatibility validation: <not run; no parser/writer/routing compatibility surface changed | command and result>
Checks: <PR check names and states, with scoped blockers only>
Files modified: <relative paths changed by this recovery step>
```

If the guard reports a clean worktree, replace `Files modified` with:

```text
No-op justification:
  PR 402 branch wave6-project-reopen-edit-chain-1778302300 already points at
  <HEAD>, local HEAD matches both the PR head and remote branch head,
  origin/develop is <origin/develop HEAD>, merge-base is <merge-base>, merge-base
  equals origin/develop, the origin/develop...HEAD diff is limited to project
  archive reopen/edit characterization/readiness surfaces, focused archive
  reopen/edit validation passed at <HEAD>, and no scoped PR check blocker
  requires a code or docs change.
```

Run and record compatibility validation when `.a3c`, `.a3w`, JSON/XML routing,
parser, writer, Tweedle decode, or archive-resource behavior changed. Do not
record broad desktop or UI claims in this evidence.

## Review claims

A passing primary validation supports this bounded claim:

```text
The repository-owned project archive reopen/edit seam is characterized at
<final HEAD>: IoUtilities writes an editable .a3p, reads it back, preserves a
deterministic in-memory project edit through a second .a3p write/read cycle, and
exports coherent .a3w archive metadata/source entries.
```

Do not claim desktop Save completion, full UI automation, visible rendering
correctness, grading, or full first-lesson completion from this validation.
