# Validate the Project Archive Reopen/Edit Seam

Use this guide to validate the repository-owned Alice project archive seam for
reopening, editing, writing, reopening again, and exporting project archives.

For the full behavior contract, see
[Project Archive Reopen/Edit Seam](../reference/project-archive-reopen-edit-seam.md).
For a guided walkthrough, see
[Tutorial: Trace the Project Archive Reopen/Edit Seam](../tutorials/trace-project-archive-reopen-edit-seam.md).

## Contents

- [Prerequisites](#prerequisites)
- [Prepare the worktree](#prepare-the-worktree)
- [Check the no-op guard root](#check-the-no-op-guard-root)
- [Run focused validation](#run-focused-validation)
- [Review failures](#review-failures)
- [Review claims](#review-claims)

## Prerequisites

Run commands from a git-linked checkout or worktree of this repository. The
focused seam lives in `core/ide`:

```text
core/ide/src/test/java/org/alice/ide/ProjectOpenSaveExportJourneyTest.java
core/ide/src/test/java/org/alice/ide/uricontent/FileProjectLoaderTest.java
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

When refreshing an active project archive IO branch, bring in the current
integration branch with a merge unless the branch can be fast-forwarded without
rewriting active pull request history:

```bash
git fetch origin develop
git merge origin/develop
```

Resolve only conflicts required by the archive reopen/edit seam, its
characterization tests, archive fixtures, or the no-op guard. Do not use this
workflow for broad project IO cleanup.

## Check the no-op guard root

Before relying on a no-op or TDD guard result, verify that the guard evaluates
the actual git worktree root:

```bash
candidate_path="$PWD"
repo_root="$(git -C "$candidate_path" rev-parse --show-toplevel)"
git -C "$repo_root" status --short
```

The guard result is valid only when `repo_root` is the linked worktree under
review. If `git rev-parse --show-toplevel` fails, the guard must fail clearly;
it must not silently inspect a copied session directory or a non-git path.

## Run focused validation

Run both seam tests together from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.ProjectOpenSaveExportJourneyTest,org.alice.ide.uricontent.FileProjectLoaderTest \
  test
```

`ProjectOpenSaveExportJourneyTest` covers the headless archive journey:

```text
write .a3p -> reopen with FileProjectLoader -> edit Project -> write .a3p
-> reopen with FileProjectLoader -> export .a3w -> read exported archive
```

`FileProjectLoaderTest` covers direct loader behavior:

```text
valid archive loads
corrupt archive is rejected
IO failure reaches the loader hook
normal project URI classification is stable
VR-ready URI and should-save classification is stable
named backup and default backup classification are stable
new project classification stays outside file-backed backup behavior
```

## Review failures

Use the failing assertion to choose the smallest responsible seam:

| Failure area | Review surface |
| --- | --- |
| Valid `.a3p` does not reopen | `FileProjectLoader`, `AbstractFileProjectLoader`, and `IoUtilities.projectReader(File)` routing. |
| Corrupt archive succeeds or hides the error path | `AbstractFileProjectLoader.load()` and `handleLoadException(File, Exception)`. |
| Edited state does not survive the second reopen | Project archive write path, `ProjectFileUtilities.saveCopyOfProjectTo(File)`, or `IoUtilities.writeProject(File, Project)`. |
| Exported archive cannot be read | `ProjectFileUtilities.exportCopyOfProjectTo(File)` or `IoUtilities.exportProject(File, Project)`. |
| VR-ready or backup classification changes | `FileProjectLoader.getUri()`, `FileProjectLoader.shouldBeSaved()`, and `UriProjectLoader` classification helpers. |
| No-op guard reports the wrong state | Root detection and git diff/status commands; they must run against `git rev-parse --show-toplevel`. |

Keep fixes local to the archive reopen/edit seam. Do not add display-backed UI
automation, grading behavior, rendering assertions, broad loader rewrites, or
first-lesson workflow claims to satisfy these tests.

## Review claims

A passing focused run supports this claim:

```text
The repository-owned project archive reopen/edit seam is ready: valid .a3p
archives reopen through FileProjectLoader, edited project state can be written,
reopened, and exported headlessly, and invalid archive/loader classification
behavior remains characterized.
```

Do not claim desktop Save completion, full UI automation, visible rendering
correctness, grading, or full first-lesson completion from this validation.
