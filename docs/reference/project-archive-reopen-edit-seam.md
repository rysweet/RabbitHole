# Project Archive Reopen/Edit Seam

The project archive reopen/edit seam is the repository-owned contract for
writing an editable Alice `.a3p` project archive, reopening it through the
production project archive reader, editing project-owned model state, writing it
again, reopening it again, and exporting the edited project to `.a3w`.

This seam lives in `core/story-api-migration`. It does not require Alice desktop,
Save-menu automation, visible rendering, grading, or first-lesson workflow
completion.

A neighboring `core/ide` bridge may load a saved project through
`FileProjectLoader` and save/export it through `ProjectFileUtilities`, but that
bridge depends on this archive IO contract for byte-level persistence and has
the same non-UI claim boundaries.

Use this reference with
[Validate the Project Archive Reopen/Edit Seam](../howto/validate-project-archive-reopen-edit-seam.md)
and
[Tutorial: Trace the Project Archive Reopen/Edit Seam](../tutorials/trace-project-archive-reopen-edit-seam.md).
For neighboring generated archive coverage, see
[Project Archive Corpus Characterization](./project-io-corpus-characterization.md).

## Contents

- [Contract](#contract)
- [Primary characterization](#primary-characterization)
- [API surfaces](#api-surfaces)
- [Archive behavior](#archive-behavior)
- [Reader routing](#reader-routing)
- [Security and failure boundaries](#security-and-failure-boundaries)
  - [XXE protection](#xxe-protection)
  - [Zip-slip path traversal guard](#zip-slip-path-traversal-guard)
  - [Entry allowlisting](#entry-allowlisting)
  - [Resource leak prevention](#resource-leak-prevention)
  - [Info-leak cleanup](#info-leak-cleanup)
  - [Boundary rules](#boundary-rules)
- [Configuration](#configuration)
- [Validation](#validation)
- [Neighboring headless IDE bridge](#neighboring-headless-ide-bridge)
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

Treat every project archive as untrusted input. The archive IO layer enforces
defense-in-depth protections at read, write, and export boundaries.

### XXE protection

`XmlProjectIo.readArchiveXml()` configures the `DocumentBuilderFactory` with
secure-processing mode and disables DOCTYPE declarations and external entity
resolution before parsing any XML entry in a `.a3p` or `.a3c` archive:

```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
```

This prevents XML External Entity (XXE) injection, billion-laughs expansion, and
server-side request forgery through crafted archives. The protection applies to
every XML path in the archive reader — `programType.xml`, `resources.xml`, and
type-archive XML payloads.

### Zip-slip path traversal guard

`ResourceExportNames.isSafeRelativeEntryName(String)` validates every archive
entry name before it is read or written. The guard rejects:

| Pattern | Rejection reason |
| --- | --- |
| Absolute paths (`/`, `C:\`) | Prevents writes outside the archive target. |
| Backslash separators (`\`) | Blocks Windows path confusion on Unix hosts. |
| Parent traversal (`..`) | Prevents zip-slip escape to parent directories. |
| Self-reference (`.`) | Blocks ambiguous current-directory entries. |
| Windows drive prefixes (`X:`) | Prevents drive-letter rooted extraction. |

Write paths call `ZipEntryContainer.validateSafeEntryName()` to enforce the same
rules before creating any entry in a new archive.

### Entry allowlisting

Archive readers constrain which entries they process:

| Reader | Allowed prefixes | Guard method |
| --- | --- | --- |
| `XmlProjectIo.readResourceData()` | `resources/`, `resources0/`…`resourcesN/` | `ResourceExportNames.isResourceEntryName(String)` |
| `JsonProjectIo.readResource()` | `resources/`, `resources0/`…`resourcesN/` | `ResourceExportNames.isResourceEntryName(String)` |
| `JsonProjectIo.readTweedleType()` | `src/` | `ResourceExportNames.isSourceEntryName(String)` |

Entries that do not match the allowed prefix for their context are skipped
silently rather than processed. This prevents a crafted archive from injecting
unexpected payloads outside the expected resource and source namespaces.

### Resource leak prevention

All archive stream reads use try-with-resources to prevent file descriptor
exhaustion:

- Manifest stream reads in `XmlProjectIo` and `JsonProjectIo`
- Version entry reads in both readers
- Resource data stream reads in `XmlProjectIo.readResourceData()`
- Type/Tweedle entry reads in `JsonProjectIo.readTweedleType()`
- `ZipFile` instances in both readers and in characterization tests

### Info-leak cleanup

Error messages from archive failures use contextual summaries — file path,
entry name, and failure reason — without dumping full manifest payloads, archive
contents, or resource bytes. `JsonProjectIo` truncates unsupported Tweedle
decode reasons to `MAX_UNSUPPORTED_TWEEDLE_REASON_LENGTH` (512 characters) to
prevent diagnostic messages from leaking large source fragments.
`ResourceExportNames.sanitizeFileName()` strips path prefixes and replaces
separators before using file names in log messages or resource identifiers.

### Boundary rules

Reader, writer, and evidence changes must preserve these additional boundaries:

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
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=IoUtilitiesTest \
  test
```

Run the historical archive guard when archive parsing, archive writing, reader
routing, XML fallback, JSON manifest handling, Tweedle decode boundaries, or
`.a3c`/`.a3w` compatibility is touched:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=HistoricalArchiveRoundTripCharacterizationTest \
  test
```

## Neighboring headless IDE bridge

`core/ide/src/test/java/org/alice/ide/ProjectOpenSaveExportJourneyTest.java`
checks the IDE-side handoff without Alice desktop. Its focused edit path loads a
synthetic `.a3p` through `FileProjectLoader`, edits the loaded `Project`, saves a
copy through `ProjectFileUtilities.saveCopyOfProjectTo`, reloads that copy, and
exports through `ProjectFileUtilities.exportCopyOfProjectTo`.

That bridge is useful evidence that the IDE load/save/export helpers preserve
edited project-owned state when they are already given a `Project`. It is not a
replacement for the `IoUtilitiesTest` archive contract, and it does not prove
Save-menu activation, Save dialog completion, rendering, grading, lesson
completion, or player runtime behavior.

## Readiness workflow surfaces

The PR readiness workflow for this seam is a bounded verification path, not a
feature expansion path. It proves that the exact pull request head is synced to
the intended base, still contains only project archive reopen/edit work, and has
fresh focused validation evidence.

| Surface | Required behavior |
| --- | --- |
| Git/PR state verifier | Confirms PR `402`, branch `wave6-project-reopen-edit-chain-1778302300`, base `develop`, local `HEAD`, PR head SHA, remote branch head SHA, `origin/develop` head SHA, exact merge-base SHA, merge-base state, and local worktree status before reporting readiness. Repeats the local, remote branch, and PR head checks after any merge, push, or committed recovery change. |
| Diff scope inspector | Reviews `origin/develop...HEAD` and groups changed files as implementation, characterization test, headless IDE bridge, QA metadata, documentation, or guard scope. Unrelated desktop, rendering, grading, Save-completion, or first-lesson changes are not part of this evidence. |
| Validation runner | Runs focused `core/story-api-migration` archive reopen/edit validation with `NODE_OPTIONS=--max-old-space-size=32768` after any required sync. Runs `ProjectOpenSaveExportJourneyTest` too when the diff includes the headless `core/ide` bridge. |
| No-op guard | Detects whether the worktree has uncommitted changes scoped to project archive reopen/edit recovery. A clean worktree is valid only when the final output includes exact-head no-op evidence for every merge-ready gate; unrelated dirty paths are rejected instead of counted as recovery evidence. |
| GitHub evidence adapter | Uses the existing `gh` CLI only for read-only PR head, PR description, and check-state evidence. Missing authentication, rate limits, unavailable GitHub responses, stale metadata, or pending checks are `NOT_MERGE_READY` blockers rather than success-shaped no-op evidence. |
| Evidence reporter | Records PR, branch, base, PR head, local HEAD, remote branch head, origin/develop head, merge-base SHA/status, worktree status, diff summary, validation result, runnable QA/scenario evidence, docs impact, quality-audit cycles, PR description evidence, check state, and either files modified, `NO_OP_GUARD`, or `NOT_MERGE_READY` blockers. |
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
| `--allow-noop-evidence evidence-file` | Allows a clean worktree only when the evidence file exists, includes exact-head no-op evidence for every merge-ready gate, includes exact base, merge-base, and committed diff scope evidence, and does not also claim `Files modified` or `NOT_MERGE_READY`. |
| `--expected-head sha` | The 40-character PR/local head SHA that must match the resolved worktree `HEAD`, appear as both `PR head` and `Local HEAD`, and be referenced in the no-op justification. Required with `--allow-noop-evidence`. |
| `-h`, `--help` | Prints usage. |

| Exit code | Meaning |
| --- | --- |
| `0` | `--print-root` succeeded, the resolved worktree has only scoped project archive reopen/edit recovery changes, or a clean worktree has valid exact-head no-op evidence. |
| `1` | The resolved worktree is clean with no valid no-op evidence, or it contains committed or uncommitted paths outside the project archive reopen/edit recovery scope. |
| `2` | The candidate path is missing or is not inside a git worktree. |
| `64` | The command line is invalid. |

The guard is not a replacement for readiness evidence. It prevents a
success-shaped workflow output that omits file changes, no-op rationale, or
required merge-ready evidence. When validation, no-timeout QA/scenario evidence,
docs impact review, quality-audit cycles, PR description evidence, and checks
are clean at the exact current head and no files need to change, the correct
output is `NO_OP_GUARD` with a **No-op justification** tied to that SHA. When
any gate is missing or stale, the correct output is `NOT_MERGE_READY` with the
explicit blockers.

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
| Remote branch HEAD | Exact SHA from `git rev-parse origin/wave6-project-reopen-edit-chain-1778302300`; it must match local `HEAD` and the PR head before final evidence is recorded. |
| origin/develop HEAD | Exact SHA from `git rev-parse origin/develop`. |
| Merge-base | Exact SHA from `git merge-base HEAD origin/develop`. |
| Merge-base status | `merge-base equals origin/develop` when the merge-base SHA equals the `origin/develop` HEAD SHA, or `merged origin/develop` when develop drift required a minimal merge. |
| Committed diff scope | Exact `origin/develop...HEAD` paths reviewed as project archive reopen/edit recovery scope. |
| Worktree status | `clean`, or exact `git status --short` entries reviewed as recovery-scope changes before they are listed under `Files modified`. |
| Diff summary | Grouped summary of `origin/develop...HEAD` by implementation, characterization test, headless IDE bridge, QA metadata, documentation, and guard scope. |
| Validation command | The focused `IoUtilitiesTest` command above, with `NODE_OPTIONS=--max-old-space-size=32768`; do not substitute desktop Save, lesson, rendering, or grading validation for this seam. |
| Validation result | Exit status and concise pass/fail outcome. |
| Headless bridge validation | Include the `ProjectOpenSaveExportJourneyTest` command and result when `ProjectOpenSaveExportJourneyTest.java`, `FileProjectLoader`, or `ProjectFileUtilities` handoff behavior changed. Otherwise record why it is not required for the exact diff. |
| Compatibility validation | Include the `HistoricalArchiveRoundTripCharacterizationTest` command and result when `.a3c`, `.a3w`, JSON/XML routing, parser, writer, Tweedle decode, or archive-resource behavior changed. Otherwise record why it is not required for the exact diff. |
| Runnable QA/scenario evidence | Current-head no-timeout scenario catalog validator result, plus diff-scoped scenario smoke evidence when QA metadata changed. |
| Docs impact | Affected docs reviewed or updated at the current head. |
| Quality-audit cycles | At least three SEEK/VALIDATE/FIX/Result cycles, with a clean final cycle. |
| PR description evidence | Current-head PR body review or update result, including stale-head and overclaim checks. |
| Checks | PR check names and states, with blockers limited to archive reopen/edit readiness. |
| Files modified | Relative paths changed by the recovery step. |
| No-op justification | Required instead of `Files modified` when the exact current head already has the required diff, validation, QA/scenario, docs, quality-audit, PR description, and check evidence. |

Do not integrate `develop` when `git merge-base HEAD origin/develop` already
equals `origin/develop`. If develop has drifted, merge `origin/develop`
minimally rather than rewriting the published PR branch. Resolve only conflicts
tied to repository-owned project archive IO seams.

After any merge or committed recovery change creates a new local `HEAD`, push the
branch normally and re-read PR head with
`gh pr view 402 --json headRefOid --jq .headRefOid`. Do not record readiness
until that SHA and
`git rev-parse origin/wave6-project-reopen-edit-chain-1778302300` both match
`git rev-parse HEAD`; readiness evidence must not describe a local-only merge
commit, stale remote branch head, or stale PR check state.

Use this exact-head no-op shape when no files change:

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

## Boundaries

This seam does not claim:

- desktop Save completion;
- native or Swing file chooser automation;
- full UI automation;
- visible rendering correctness;
- grading or learner assessment correctness;
- creative assessment;
- full Tweedle/player decode;
- full first-lesson completion;
- player runtime behavior beyond archive manifest/source readback;
- broad project migration correctness outside the characterized IO boundary.

Use the separate Save-menu proof lane for bounded desktop Save evidence. Keep
this seam focused on repository-owned project archive reader, writer, export,
manifest, and compatibility behavior.
