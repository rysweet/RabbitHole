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
- [Run primary validation](#run-primary-validation)
- [Run compatibility validation when needed](#run-compatibility-validation-when-needed)
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
gh pr view 402 --json number,headRefName,headRepositoryOwner,baseRefName
```

Account for the recovery baseline:

```bash
git merge-base --is-ancestor 50b4d8687a42 HEAD
git log --oneline --decorate --max-count=12
```

Sync with `develop` using the least disruptive path that preserves active PR
review history:

1. If the branch already contains current `origin/develop`, record `already current`.
2. Rebase only when the branch is private to the current recovery task or the PR
   owner has explicitly chosen rewritten history for this update.
3. For a shared or already-reviewed PR branch, prefer a minimal merge of
   `origin/develop` over rewriting active PR history.

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

## Run primary validation

Run the canonical reopen/edit seam characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.IoUtilitiesTest \
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

## Run compatibility validation when needed

Run the historical archive round-trip guard when the change touches archive
parsing, archive writing, XML fallback, JSON manifest handling, Tweedle decode,
resource entries, or `.a3c`/`.a3w` compatibility:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

If no archive parsing or writing code changed and the primary validation passes,
the historical guard is optional for this narrow PR recovery.

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
```

Use this evidence shape:

```text
PR: 402
Branch: wave6-project-reopen-edit-chain-1778302300
Recovery baseline: 50b4d8687a42
Sync method: <already current | rebased onto develop | merged develop>
Final HEAD: <exact git rev-parse HEAD value>
Validation:
  NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl core/story-api-migration -am -Dtest=org.lgna.project.io.IoUtilitiesTest test
Result: <passed with exit code 0 | failed with exit code N and blocker summary>
Compatibility validation: <not run; no parser/writer/routing compatibility surface changed | command and result>
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
