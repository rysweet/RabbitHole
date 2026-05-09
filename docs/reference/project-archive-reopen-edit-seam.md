# Project Archive Reopen/Edit Seam

The project archive reopen/edit seam is the repository-owned contract for
writing an editable Alice `.a3p` project archive, reopening it through the
production project archive reader, editing project-owned model state, writing it
again, reopening it again, and exporting the edited project to `.a3w`.

This seam lives in `core/story-api-migration`. It does not require Alice desktop,
Save-menu automation, visible rendering, grading, or first-lesson workflow
completion.

Use this reference with
[Validate the Project Archive Reopen/Edit Seam](../howto/validate-project-archive-reopen-edit-seam.md)
and
[Tutorial: Trace the Project Archive Reopen/Edit Seam](../tutorials/trace-project-archive-reopen-edit-seam.md).
For neighboring generated archive coverage, see
[Project IO Corpus Characterization](./project-io-corpus-characterization.md).

## Contents

- [Contract](#contract)
- [Primary characterization](#primary-characterization)
- [API surfaces](#api-surfaces)
- [Archive behavior](#archive-behavior)
- [Reader routing](#reader-routing)
- [Security and failure boundaries](#security-and-failure-boundaries)
- [Configuration](#configuration)
- [Validation](#validation)
- [Readiness workflow surfaces](#readiness-workflow-surfaces)
- [No-op guard command API](#no-op-guard-command-api)
- [PR 402 readiness evidence](#pr-402-readiness-evidence)
- [Boundaries](#boundaries)

## Contract

The seam proves this bounded journey:

```text
Given a valid in-memory Alice Project
When IoUtilities.writeProject writes the original .a3p archive
And IoUtilities.readProject reopens that .a3p archive
And the reopened Project is edited in memory
And IoUtilities.writeProject writes the edited .a3p archive
And IoUtilities.readProject reopens the edited .a3p archive
And IoUtilities.exportProject exports the edited Project as .a3w
Then the edited project-owned state survives the second reopen
And the edited .a3p and exported .a3w archives contain coherent manifest data
```

The edit assertion is mandatory. A file-exists assertion, non-empty archive
assertion, or successful first reopen alone is not enough because those checks
would miss stale-save regressions that write the pre-edit project state.

## Primary characterization

`IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`
is the canonical characterization for the reopen/edit seam.

The test uses generated temporary archives and only production project archive
APIs:

| Step | Surface | Required behavior |
| --- | --- | --- |
| Create fixture | `new Project(programType("OriginalProgram"), Project.SceneCameraType.WindowCamera)` | Builds a deterministic editable project without a checked-in binary fixture. |
| Save original archive | `IoUtilities.writeProject(originalProjectFile, project)` | Writes an editable `.a3p` archive. |
| Reopen original archive | `IoUtilities.readProject(originalProjectFile)` | Returns a `Project` whose program type is present and named `OriginalProgram`. |
| Edit reopened project | `NamedUserType.name.setValue("EditedProgram")` | Mutates project-owned AST data after the first reopen. |
| Save edited archive | `IoUtilities.writeProject(editedProjectFile, reopenedProject)` | Writes a second editable `.a3p` archive from the edited project. |
| Reopen edited archive | `IoUtilities.readProject(editedProjectFile)` | Returns a `Project` whose program type is present and named `EditedProgram`. |
| Inspect edited archive | `ZipFile` and `manifest.json` | Confirms the edited `.a3p` manifest names `EditedProgram`, declares file type `a3p`, preserves scene-camera metadata, and contains `programType.xml`. |
| Export edited project | `IoUtilities.exportProject(exportFile, editedProject)` | Writes a `.a3w` player archive from the edited project. |
| Inspect export archive | `ZipFile` and `manifest.json` | Confirms the `.a3w` manifest names `EditedProgram`, declares file type `a3w`, and references `src/EditedProgram.twe`. |

Keep this as the single canonical full-chain test. Add neighboring coverage to
`IoUtilitiesTest` or `HistoricalArchiveRoundTripCharacterizationTest` when a
change touches archive routing, manifest parsing, resources, XML fallback,
Tweedle decode boundaries, or historical archive round trips.

## API surfaces

| Surface | Role |
| --- | --- |
| `org.lgna.project.io.IoUtilities.readProject(File)` | Public project archive reader for `.a3p` and `.a3w` files. |
| `org.lgna.project.io.IoUtilities.readProject(String)` | Path-based overload for the same checked project read boundary. |
| `org.lgna.project.io.IoUtilities.writeProject(File, Project, DataSource...)` | Editable `.a3p` writer that creates parent directories and writes through production archive code. |
| `org.lgna.project.io.IoUtilities.writeProject(OutputStream, Project, DataSource...)` | Stream-backed editable project writer. |
| `org.lgna.project.io.IoUtilities.exportProject(File, Project, DataSource...)` | Player `.a3w` exporter for project archive readback and manifest assertions. |
| `org.lgna.project.io.XmlProjectIo` | XML project/type archive implementation used by editable `.a3p` fallback and XML `.a3c` behavior. |
| `org.lgna.project.io.JsonProjectIo` | JSON manifest/Tweedle archive implementation used by readable `.a3w` and JSON `.a3c` behavior. |
| `org.lgna.project.io.IoUtilities.PROJECT_EXTENSION` | Public extension constant for editable project archives: `a3p`. |
| `org.lgna.project.io.IoUtilities.EXPORT_EXTENSION` | Public extension constant for player archives: `a3w`. |
| `org.lgna.project.io.IoUtilities.TYPE_EXTENSION` | Public extension constant for type archives: `a3c`. |

`IoUtilities.readProject` declares checked archive failures. Unsupported,
malformed, mismatched, or corrupt archives must fail clearly at the IO boundary
instead of returning a success-shaped partial `Project`.

## Archive behavior

### Editable project `.a3p`

`IoUtilities.writeProject(File, Project, DataSource...)` writes editable project
archives. A generated project archive includes:

```text
version.txt
manifest.json
programType.xml
```

Resource-bearing project archives also include:

```text
resources.xml
resources/<resource-name>
```

The manifest identifies the archive as an Alice project:

| Manifest field | Required behavior |
| --- | --- |
| `metadata.fileType` | `a3p` |
| `metadata.identifier.type` | `World` |
| `description.name` | Matches the current project program type name. |
| `projectStructure.sceneCameraType` | Preserves the project scene-camera type. |

Generated editable `.a3p` archives keep XML program payloads. They do not use
`src/<ProgramType>.twe` as the primary editable project payload.

### Player export `.a3w`

`IoUtilities.exportProject(File, Project, DataSource...)` writes player archives.
A generated simple player archive includes:

```text
version.txt
manifest.json
src/<ProgramType>.twe
```

The manifest identifies the export as a player archive:

| Manifest field | Required behavior |
| --- | --- |
| `metadata.fileType` | `a3w` |
| `metadata.identifier.type` | `World` |
| `description.name` | Matches the exported project program type name. |
| `projectStructure.sceneCameraType` | Preserves the exported scene-camera type. |
| type reference | Points at `src/<ProgramType>.twe` with format `tweedle`. |

Export readback and manifest assertions are archive IO evidence only. They do
not prove player runtime behavior or visible rendering correctness.

### Type archive `.a3c`

Type archive behavior is adjacent to the reopen/edit seam. It is protected by
`HistoricalArchiveRoundTripCharacterizationTest` and lower-level `IoUtilities`
coverage when reader/writer routing changes affect `.a3c` compatibility.

## Reader routing

`IoUtilities` selects the archive reader from `manifest.json`:

| Manifest state | Reader behavior |
| --- | --- |
| Readable manifest with `metadata.fileType` equal to `a3w` | Uses the JSON project reader. |
| Readable manifest with `metadata.fileType` equal to `a3c` | Uses the JSON project/type reader where applicable. |
| Missing manifest | Uses the XML reader. |
| Readable manifest with `metadata.fileType` equal to `a3p` | Uses XML fallback for editable project payloads. |
| Corrupt manifest | Fails with manifest-read context instead of silently falling back. |

This routing is compatibility-sensitive. A change that moves editable `.a3p`
archives away from XML fallback must update the API behavior, archive contract,
and characterization tests together.

## Security and failure boundaries

Treat every project archive as untrusted input. Reader, writer, and evidence
changes must preserve these boundaries:

| Boundary | Required behavior |
| --- | --- |
| Corrupt or mismatched manifests | Fail clearly with archive-read context. Do not silently downgrade a readable but invalid manifest into XML fallback or a partial success. |
| Archive entries | Keep entry handling archive-local. Do not relax existing path, resource, or payload safety checks when adding `.a3p`, `.a3w`, or `.a3c` coverage. |
| XML parsing | Do not add XML parsing behavior that resolves external entities, loads remote resources, or depends on network access. |
| Evidence output | Record commands, SHAs, exit statuses, and concise failure summaries only. Do not log full archive contents, full manifest payloads, source payloads, or resource bytes as readiness evidence. |

## Configuration

Use the saved Node memory preference when running Maven validation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Initialize the Tweedle grammar submodule in every fresh checkout or worktree
before focused or broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

No desktop display, credentials, network service, new product preference, or
checked-in binary archive corpus is required for this seam.

## Validation

Run the focused primary characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.IoUtilitiesTest \
  test
```

Run the historical archive guard when archive parsing, archive writing, reader
routing, XML fallback, JSON manifest handling, Tweedle decode boundaries, or
`.a3c`/`.a3w` compatibility is touched:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

## Readiness workflow surfaces

The PR readiness workflow for this seam is a bounded verification path, not a
feature expansion path. It proves that the exact pull request head is synced to
the intended base, still contains only project archive reopen/edit work, and has
fresh focused validation evidence.

| Surface | Required behavior |
| --- | --- |
| Git/PR state verifier | Confirms PR `402`, branch `wave6-project-reopen-edit-chain-1778302300`, base `develop`, local `HEAD`, PR head SHA, merge-base state, and local worktree status before reporting readiness. Repeats the PR head check after any merge, push, or committed recovery change. |
| Diff scope inspector | Reviews `origin/develop...HEAD` and groups changed files as implementation, characterization test, QA metadata, documentation, or guard scope. Unrelated desktop, rendering, grading, Save-completion, or first-lesson changes are not part of this evidence. |
| Validation runner | Runs focused `core/story-api-migration` archive reopen/edit validation with `NODE_OPTIONS=--max-old-space-size=32768` after any required sync. |
| No-op guard | Detects whether the worktree has uncommitted recovery changes. A clean worktree is valid only when the final output includes an exact-head no-op justification. |
| Evidence reporter | Records PR, branch, base, PR head, local HEAD, merge-base status, worktree status, diff summary, validation result, check state, and either files modified or a no-op justification. |
| CI/check reconciler | Inspects PR checks and resolves only blockers directly tied to project archive reopen/edit readiness. Pending unrelated checks are reported as pending, not converted into broad readiness claims. |

## No-op guard command API

`scripts/project-archive-reopen-edit-noop-guard.sh` is the repo-owned guard for
default-workflow recovery steps that expect file changes. It resolves linked
worktrees through git and evaluates the actual repository root rather than the
caller-provided nested path.

```bash
scripts/project-archive-reopen-edit-noop-guard.sh [candidate-path] [--print-root]
scripts/project-archive-reopen-edit-noop-guard.sh [candidate-path] --allow-noop-evidence evidence-file --expected-head sha
```

| Argument | Behavior |
| --- | --- |
| `candidate-path` | Optional path inside the target git worktree. Defaults to the current directory. |
| `--print-root` | Prints the resolved linked worktree root and exits without checking for changes. |
| `--allow-noop-evidence evidence-file` | Allows a clean worktree only when the evidence file exists, includes an exact-head **No-op justification**, and does not also claim `Files modified`. |
| `--expected-head sha` | The 40-character PR/local head SHA that must match the resolved worktree `HEAD`, appear as both `PR head` and `Local HEAD`, and be referenced in the no-op justification. Required with `--allow-noop-evidence`. |
| `-h`, `--help` | Prints usage. |

| Exit code | Meaning |
| --- | --- |
| `0` | `--print-root` succeeded, the resolved worktree has uncommitted changes, or a clean worktree has valid exact-head no-op evidence. |
| `1` | The resolved worktree is clean and no valid no-op evidence was supplied. |
| `2` | The candidate path is missing or is not inside a git worktree. |
| `64` | The command line is invalid. |

The guard is not a replacement for readiness evidence. It only prevents a
success-shaped workflow output that omits both file changes and no-op rationale.
When validation and checks are clean at the exact current head and no files need
to change, the correct output is a **No-op justification** tied to that SHA.

## PR 402 readiness evidence

For PR 402 recovery work on branch
`wave6-project-reopen-edit-chain-1778302300`, readiness evidence records the
exact branch state that passed the focused archive IO characterization. The
evidence is a handoff record, not a new behavior claim.

Record:

| Field | Required value |
| --- | --- |
| PR | `402` |
| Branch | `wave6-project-reopen-edit-chain-1778302300` |
| Base | `develop` |
| PR head | Exact SHA from `gh pr view 402 --json headRefOid --jq .headRefOid`. |
| Local HEAD | Exact SHA from `git rev-parse HEAD`; it must match the PR head before final evidence is recorded. |
| Merge-base status | `merge-base equals origin/develop` when no integration is needed, or `merged origin/develop` when develop drift required a minimal merge. |
| Worktree status | `clean`, or exact `git status --short` entries reviewed as recovery-scope changes before they are listed under `Files modified`. |
| Diff summary | Grouped summary of `origin/develop...HEAD` by implementation, characterization test, QA metadata, documentation, and guard scope. |
| Validation command | The focused `IoUtilitiesTest` command above, with `NODE_OPTIONS=--max-old-space-size=32768`. |
| Validation result | Exit status and concise pass/fail outcome. |
| Compatibility validation | Include the `HistoricalArchiveRoundTripCharacterizationTest` command and result when `.a3c`, `.a3w`, JSON/XML routing, parser, writer, Tweedle decode, or archive-resource behavior changed. Otherwise record why it is not required for the exact diff. |
| Checks | PR check names and states, with blockers limited to archive reopen/edit readiness. |
| Files modified | Relative paths changed by the recovery step. |
| No-op justification | Required instead of `Files modified` when the exact current head already has the required diff, validation, and check evidence. |

Do not integrate `develop` when `git merge-base HEAD origin/develop` already
equals `origin/develop`. If develop has drifted, merge `origin/develop`
minimally rather than rewriting the published PR branch. Resolve only conflicts
tied to repository-owned project archive IO seams.

After any merge or committed recovery change creates a new local `HEAD`, push the
branch normally and re-read PR head with
`gh pr view 402 --json headRefOid --jq .headRefOid`. Do not record readiness
until that SHA matches `git rev-parse HEAD`; readiness evidence must not describe
a local-only merge commit or stale PR check state.

Use this exact-head no-op shape when no files change:

```text
No-op justification:
  PR 402 branch wave6-project-reopen-edit-chain-1778302300 already points at
  <HEAD>, local HEAD matches the PR head, merge-base equals origin/develop, the
  origin/develop...HEAD diff is limited to project archive reopen/edit
  characterization/readiness surfaces, focused archive reopen/edit validation
  passed at <HEAD>, and no scoped PR check blocker requires a code or docs
  change.
```

## Boundaries

This seam does not claim:

- desktop Save completion;
- native or Swing file chooser automation;
- full UI automation;
- visible rendering correctness;
- grading or learner assessment correctness;
- full first-lesson completion;
- player runtime behavior beyond archive manifest/source readback;
- broad project migration correctness outside the characterized IO boundary.

Use the separate Save-menu proof lane for bounded desktop Save evidence. Keep
this seam focused on repository-owned project archive reader, writer, export,
manifest, and compatibility behavior.
