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
- [Check guard root detection](#check-guard-root-detection)
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

Only when refreshing an active pull request recovery branch, bring in the current
integration branch with a merge unless the branch can be fast-forwarded without
rewriting active pull request history:

```bash
git fetch origin develop
git merge origin/develop
```

Resolve only conflicts required by the archive reopen/edit seam, its
characterization tests, archive fixtures, or a guard script used by the branch.
Do not use this workflow for broad project archive reader/writer cleanup.

## Check guard root detection

If a branch uses a no-op or TDD guard script, verify that the guard evaluates the
actual git worktree root:

```bash
candidate_path="$PWD"
repo_root="$(git -C "$candidate_path" rev-parse --show-toplevel)"
git -C "$repo_root" status --short
```

The guard result is valid only when `repo_root` is the linked worktree under
review. If `git rev-parse --show-toplevel` fails, the guard must fail clearly;
it must not silently inspect a copied session directory or a non-git path. This
is a contract for any guard used by the seam, not a claim that this guide names a
specific script.

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

`ProjectOpenSaveExportJourneyTest` must cover the complete headless archive
journey before the reopen/edit seam is complete:

```text
write .a3p -> reopen with FileProjectLoader -> edit Project -> write .a3p
-> reopen with FileProjectLoader -> export .a3w -> read exported archive
```

A test that writes the reopened project without a deterministic edit covers only
the narrower reopen/write/reopen/export path. Add the edit assertion before
making complete reopen/edit claims.

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
| Edited state does not survive the second reopen | Project archive write path or `IoUtilities.writeProject(File, Project)` in the headless journey; review `ProjectFileUtilities.saveCopyOfProjectTo(File)` separately when changing IDE save-copy behavior. |
| Exported archive cannot be read | `IoUtilities.exportProject(File, Project)` in the headless journey; review `ProjectFileUtilities.exportCopyOfProjectTo(File)` separately when changing IDE export-copy behavior. |
| VR-ready or backup classification changes | `FileProjectLoader.getUri()`, `FileProjectLoader.shouldBeSaved()`, and `UriProjectLoader` classification helpers. |
| Guard script reports the wrong state | Root detection and git diff/status commands; they must run against `git rev-parse --show-toplevel`. |

Keep fixes local to the archive reopen/edit seam. Do not add display-backed UI
automation, grading behavior, rendering assertions, broad loader rewrites, or
first-lesson workflow claims to satisfy these tests.

## Review claims

After the focused tests include the edit assertion, a passing run supports this
claim:

```text
The repository-owned project archive reopen/edit seam is ready when valid .a3p
archives reopen through FileProjectLoader, deterministic edited project state can
be written, reopened, and exported headlessly, and invalid archive/loader
classification behavior remains characterized.
```

Do not claim desktop Save completion, full UI automation, visible rendering
correctness, grading, or full first-lesson completion from this validation.
