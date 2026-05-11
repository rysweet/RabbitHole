# Silver Thread Edit-Save-Readback End-to-End Test

This reference defines the connected silver thread test that chains procedure
editing through production save to archive readback. The executable proof is
`org.alice.ide.SilverThreadEditSaveReadbackTest`. It is a JUnit 4 test in
`core/ide` that proves the full edit → save → readback journey works headlessly:
build a starter project, run `EatmeEditProcedure` to append a comment, save via
`ProjectApplication.saveProjectTo`, reopen the saved archive, and verify the
appended comment survived.

The test does not start JavaFX, load gallery assets, render a 3D scene, exercise
drag-and-drop UI, or require a display.

## Contents

- [Scope](#scope)
- [Implementation status](#implementation-status)
- [Design decisions](#design-decisions)
- [Test method](#test-method)
- [Usage](#usage)
- [Behavior contract](#behavior-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Proof artifact](#proof-artifact)
- [Relationship to issue #489](#relationship-to-issue-489)
- [Claim boundaries](#claim-boundaries)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

## Scope

The test covers exactly one journey:

### Edit via EatmeEditProcedure → Save via Production Path → Readback → Verify Comment Survived

```text
1. Build a Project with Scene.eatmeFirstLesson containing Comment("existing")
2. Write project to temp .a3p via IoUtilities.writeProject
3. Run EatmeEditProcedure.run() to append Comment("silver-thread-edit-save-readback-proof")
4. Read back edited-project.a3p via IoUtilities.readProject — verify marker exists
5. resetApplicationSingleton() to clear EatmeEditProcedure's anonymous Application
6. Bootstrap TestProjectApplication with the edited project
7. Save via ProjectApplication.saveProjectTo(savedFile)
8. IoUtilities.readProject(savedFile) — verify marker comment survived full chain
9. Write JSON proof artifact to evidence directory
```

This is the first test that chains all three operations in a single method. The
existing `EatmeEditProcedureTest` proves editing alone. The existing
`SilverThreadSaveTest` proves saving alone. This test proves the operations
compose correctly.

## Implementation status

| Surface | Location |
| --- | --- |
| `SilverThreadEditSaveReadbackTest` | `core/ide/src/test/java/org/alice/ide/SilverThreadEditSaveReadbackTest.java` |
| Maven validation | `mvn -pl core/ide -am -Dtest=SilverThreadEditSaveReadbackTest test` |
| Prerequisite: promote `run()` | `core/ide/src/main/java/org/alice/tools/EatmeEditProcedure.java` line 52: `static int run` → `public static int run` |

## Design decisions

Five deliberate decisions, informed by the existing test suite:

1. **`projectWithSceneMethod("eatmeFirstLesson")` with `Comment("existing")`.**
   This reuses the project-building helper from `EatmeEditProcedureTest` (lines
   398–403). The project has a `Program → Scene` field with a `UserMethod` named
   `eatmeFirstLesson` that already contains one `Comment("existing")` statement.
   This matches the structure that `EatmeEditProcedure` expects.

2. **Edit via `EatmeEditProcedure.run()`, not direct AST manipulation.**
   The existing silver thread tests modify the AST directly. This test instead
   invokes `EatmeEditProcedure.run()` (a static method) with `--project`,
   `--procedure-selector`, `--edit-spec`, `--evidence-dir`, and `--json`
   arguments — the same CLI interface that a CI automation step would use. The
   `--edit-spec` value is `"append-comment:silver-thread-edit-save-readback-proof"`,
   where the text after the colon becomes the `Comment` text. This proves the
   tool's edit path produces output that the save pipeline can consume.

   **Prerequisite change:** `EatmeEditProcedure.run()` is currently
   package-private (no access modifier, visible only within `org.alice.tools`).
   Since this test lives in `org.alice.ide` to match the other silver thread
   tests and to access `ProjectApplication` internals, `run()` must be promoted
   to `public` visibility before the test can compile. This is a one-word change
   in `EatmeEditProcedure.java` line 52: `static int run` → `public static int run`.

3. **`resetApplicationSingleton()` between edit and save.**
   `EatmeEditProcedure.run()` internally calls `ensureCroquetApplication()`,
   which installs an anonymous `Application` singleton. The
   `TestProjectApplication` constructor requires that no singleton exists.
   The `resetApplicationSingleton()` reflection helper (copied from
   `SilverThreadSaveTest`) nulls the singleton between the two phases.

4. **Save via `ProjectApplication.saveProjectTo`, not `IoUtilities.writeProject`.**
   Same rationale as `SilverThreadSaveTest`: the production save path exercises
   `ensureProjectCodeUpToDate`, `createThumbnail`, URI adoption, save backup,
   and recent project recording. This makes the chain honest through the
   production save code path.

5. **Unique comment marker `"silver-thread-edit-save-readback-proof"`.**
   Distinct from `"existing"` (the pre-existing comment in the starter project),
   `"silver-thread-save-marker"` (`SilverThreadSaveTest`), and all other test
   markers. The unique marker prevents false positives when the test verifies
   that the comment survived the chain.

## Test method

### editedCommentSurvivesProductionSaveAndReadback

| Step | API | Assertion |
| --- | --- | --- |
| Build starter project | `projectWithSceneMethod("eatmeFirstLesson")` with `Comment("existing")` | Project is non-null; method body has 1 statement. |
| Write to temp file | `IoUtilities.writeProject(starterFile, project, ...)` | `.a3p` file exists. |
| Run EatmeEditProcedure | `EatmeEditProcedure.run(args, out, err)` (static) | Exit status is 0; `edited-project.a3p` exists in evidence dir. |
| Read edited project | `IoUtilities.readProject(editedFile)` | Edited project is non-null; `eatmeFirstLesson` body has 2 statements; second statement is `Comment("silver-thread-edit-save-readback-proof")`. |
| Reset singleton | `resetApplicationSingleton()` | No assertion — preparation step. |
| Boot application | `applicationWith(editedProject, new InMemoryProjectLoader())` | Application singleton is set. |
| Initialize recent projects | `RecentProjectCountState.getInstance().setValueTransactionlessly(10)` | No assertion — required for `saveProjectTo`. |
| Save via production path | `application.saveProjectTo(savedFile)` | File exists, non-empty. |
| Readback saved project | `IoUtilities.readProject(savedFile)` | Reopened project is non-null. |
| Verify program structure | Navigate `programType.fields[0].valueType` → `findMethod("eatmeFirstLesson")` | Method is non-null. |
| Verify comment survived | `((Comment) body.statements[1]).text.getValue()` | Equals `"silver-thread-edit-save-readback-proof"`. |
| Write proof artifact | JSON to `evidence-dir/edit-save-readback-proof.json` | File exists and validates against schema. |

## Usage

Run from the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadEditSaveReadbackTest \
  test
```

The test runs without a display server, JavaFX toolkit, or network access.

## Behavior contract

### Chain steps exercised

When the test executes, the following chain runs:

| Phase | Production behavior | Test behavior |
| --- | --- | --- |
| **Edit phase** | | |
| `EatmeEditProcedure.run()` | CLI tool that loads project, edits procedure, writes output | **Production code — not stubbed** |
| `ensureCroquetApplication()` | Installs anonymous Application for AST operations | **Production code — not stubbed** |
| Procedure AST modification | `append-comment:<marker>` edit spec appends Comment to method body | **Production code — not stubbed** |
| `IoUtilities.writeProject` (inside EatmeEditProcedure) | Writes edited `.a3p` archive | **Production code — not stubbed** |
| **Save phase** | | |
| `createFrameTitleGenerator()` | Builds title generator from document frame | Returns constant string (`TestProjectApplication` override) |
| `ProjectSaveTargetPlan.choose()` | Selects save strategy | **Production code — not stubbed** |
| URI adoption | Sets `uriProjectLoader` to target | **Production code — not stubbed** |
| `ensureProjectCodeUpToDate()` | Synchronizes code editor state | No-op (`TestProjectApplication` override) |
| `createThumbnail()` | Renders 3D scene | Returns 1×1 `BufferedImage` (`TestProjectApplication` override) |
| `IoUtilities.writeProject` (inside saveProjectTo) | Writes `.a3p` archive with thumbnail and manifest | **Production code — not stubbed** |
| `backupSavedProject()` | Creates save backup | **Production code — not stubbed** |
| `RecentProjectsListData.handleSave()` | Records in recent projects | **Production code — not stubbed** |

Nine of twelve steps are production code. The three overrides (`createFrameTitleGenerator`,
`ensureProjectCodeUpToDate`, `createThumbnail`) are the minimum needed for headless operation.

### Edit fidelity contract

After `EatmeEditProcedure.run()` and before the save phase, the test asserts:

| Property | Expected value |
| --- | --- |
| Method body statement count | 2 |
| First statement | `Comment("existing")` — unchanged |
| Second statement | `Comment("silver-thread-edit-save-readback-proof")` — appended |

### Save-readback fidelity contract

After saving via the production path and reopening via `IoUtilities.readProject`,
the test asserts:

| Property | Expected value |
| --- | --- |
| Program type | Non-null, navigable to Scene type |
| Method name | `"eatmeFirstLesson"` |
| Statement count | 2 |
| Second statement text | `"silver-thread-edit-save-readback-proof"` |

These assertions prove the edit survived both the production save pipeline and
the standard archive reader. A bug in `EatmeEditProcedure`, `saveProjectTo`,
archive serialization, or archive deserialization would break at least one
assertion.

## API reference

| Class | Module | Role in this test |
| --- | --- | --- |
| `EatmeEditProcedure` | `core/ide` | CLI tool; static `run(args, out, err)` applies the edit. |
| `ProjectApplication` | `core/ide` | Production application; `saveProjectTo(File)` is the save method under test. |
| `TestProjectApplication` | test inner class | Headless subclass; overrides `createFrameTitleGenerator` (returns constant string), `updateTitle` (no-op), `createThumbnail` (1×1 image), `ensureProjectCodeUpToDate` (no-op), `forceProjectCodeUpToDate` (no-op), plus required abstract method stubs. |
| `InMemoryProjectLoader` | test inner class | Stub `UriProjectLoader`; `isNewProject()` returns `false`. |
| `IoUtilities` | `core/story-api-migration` | `readProject(File)` — standard archive reader for readback. `writeProject` — archive writer for starter project. |
| `Project` | `core/ast` | Top-level project container. |
| `NamedUserType` | `core/ast` | The program and scene types. |
| `UserMethod` | `core/ast` | Method containing the procedure body. |
| `Comment` | `core/ast` | Marker statement with known text. |
| `BlockStatement` | `core/ast` | Method body container. |
| `Application` | `croquet-core` | Singleton reset via reflection in `resetApplicationSingleton()`. |
| `RecentProjectCountState` | `core/ide` | Singleton initialized before `saveProjectTo`. |

### Inner class patterns

`TestProjectApplication` and `InMemoryProjectLoader` are private inner classes
copied from `SilverThreadSaveTest`. They cannot be shared as library classes
because `TestProjectApplication` depends on protected `ProjectApplication`
methods and `InMemoryProjectLoader` is a minimal stub.

The reflection helpers `resetApplicationSingleton()` and `installLoader()` are
also copied from `SilverThreadSaveTest`.

## Configuration

No system properties or environment variables are required beyond
`NODE_OPTIONS=--max-old-space-size=32768` for the Maven reactor build.

The test uses JUnit 4's `@Rule TemporaryFolder` for:
- The starter project `.a3p` file
- The `EatmeEditProcedure` evidence directory (containing `edited-project.a3p`)
- The production-save target file
- The proof artifact JSON

All temp files are automatically deleted after the test completes, including on
failure.

## Proof artifact

The test writes a JSON proof artifact to the evidence directory:

```json
{
  "schema_version": "eatme.alice-edit-save-readback-proof/v1",
  "test_class": "org.alice.ide.SilverThreadEditSaveReadbackTest",
  "chain_steps": {
    "edit": {
      "tool": "EatmeEditProcedure",
      "edit_spec": "append-comment",
      "marker": "silver-thread-edit-save-readback-proof",
      "status": "pass",
      "edited_project_exists": true
    },
    "save": {
      "method": "ProjectApplication.saveProjectTo",
      "status": "pass",
      "saved_file_exists": true,
      "saved_file_non_empty": true
    },
    "readback": {
      "method": "IoUtilities.readProject",
      "status": "pass",
      "marker_found": true,
      "marker_text": "silver-thread-edit-save-readback-proof",
      "statement_count": 2
    }
  },
  "timestamp": "2026-05-11T03:19:00Z"
}
```

The artifact follows the `EatmeEditProcedure` naming convention for evidence
files. The `schema_version` field uses the `eatme.*` namespace to indicate it
belongs to the edit-automation tool family.

## Relationship to issue #489

[RabbitHole issue #489](https://github.com/rysweet/RabbitHole/issues/489) asks
for "one connected silver thread test that chains edit then Save then readback":

| Requirement | How this test satisfies it |
| --- | --- |
| Load a starter project | `projectWithSceneMethod("eatmeFirstLesson")` builds a project matching `EatmeEditProcedure` expectations. |
| Run EatmeEditProcedure to append a comment | `EatmeEditProcedure.run()` with `--edit-spec "append-comment:silver-thread-edit-save-readback-proof"` appends the marker `Comment`. |
| Save via `ProjectApplication.saveProjectTo` | `TestProjectApplication` bootstrap + `application.saveProjectTo(savedFile)`. |
| Reopen the saved file | `IoUtilities.readProject(savedFile)`. |
| Verify the comment survived | Navigate to `eatmeFirstLesson` → assert second statement is `Comment("silver-thread-edit-save-readback-proof")`. |
| Combine patterns from `SilverThreadSaveTest` and `EatmeEditProcedureTest` | `TestProjectApplication`/`InMemoryProjectLoader`/`resetApplicationSingleton()` from `SilverThreadSaveTest`; `projectWithSceneMethod`/edit invocation from `EatmeEditProcedureTest`. |
| Must work headlessly | No display, JavaFX, 3D rendering, or network required. |

## Claim boundaries

This test proves:

- `EatmeEditProcedure.run()` can edit a procedure on a synthetic starter project
  and produce a valid `.a3p` archive.
- The edited archive can be loaded into a headless `TestProjectApplication` and
  saved through the production `saveProjectTo` pipeline.
- The production-saved archive is readable by `IoUtilities.readProject`.
- The appended `Comment("silver-thread-edit-save-readback-proof")` survives the
  full edit → production save → readback chain.
- A JSON proof artifact documents all three chain steps.

This test does **not** prove:

| Non-claim | Reason |
| --- | --- |
| 3D rendering correctness | Thumbnail is a 1×1 stub. |
| `ensureProjectCodeUpToDate` correctness | Stubbed as no-op. |
| VM execution of saved AST | Not tested; owned by `SilverThreadLaunchBuildRunTest`. |
| Object placement | Not tested; owned by `EatmeEditProcedureTest`. |
| Save backup creation | Not tested; owned by `ProjectApplicationSaveProjectToTest`. |
| Save-As behavior | Not tested; owned by `ProjectApplicationSaveProjectToTest`. |
| Failed save recovery | Not tested; owned by `ProjectApplicationSaveProjectToTest`. |
| Drag-and-drop UI | No Swing/JavaFX automation. |
| Gallery or model resource loading | No resources referenced. |
| JavaFX display | No toolkit initialization. |
| Lesson completion or grading | No assessment logic. |
| Real starter project round-trip | Uses a synthetic project, not `indiaMinimum.a3p`. |
| Desktop UI automation | No menu, dialog, or windowing interaction. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Headless create → build → run → save → reopen | [Silver Thread Launch-Build-Run Test](./silver-thread-launch-build-run-test.md). |
| Production save round-trip (synthetic project) | [Silver Thread Save Round-Trip Test](./silver-thread-save-round-trip-test.md). |
| EatmeEditProcedure standalone editing | [First-Lesson Procedure/Edit Seam](./first-lesson-procedure-edit-seam.md). |
| `saveProjectTo` edge cases (backup, Save-As, failure) | [Project Save and Export Operations](./project-save-export-operations.md). |
| Archive structure and validation | [Project IO Corpus Characterization](./project-io-corpus-characterization.md). |
| Robot Save menu dialog proof | [Robot Save Menu Dialog Write-Readback Proof](./robot-save-menu-dialog-write-readback-proof.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](./silver-thread-status-report.md). |

## Examples

### Running in CI

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadEditSaveReadbackTest \
  test -q
```

The `-q` flag suppresses Maven reactor output. The test prints its own
chain-step progress to stdout.

### Verifying the proof artifact

After a test run, the proof artifact is in the `TemporaryFolder` evidence directory.
During the test, it is validated programmatically:

```java
JsonObject proof = JsonParser.parseReader(new FileReader(proofFile)).getAsJsonObject();
assertEquals("eatme.alice-edit-save-readback-proof/v1",
    proof.get("schema_version").getAsString());
assertEquals("pass",
    proof.getAsJsonObject("chain_steps").getAsJsonObject("readback").get("status").getAsString());
```

### Adapting for a different edit spec

To test a different marker text, change the `--edit-spec` argument (the text
after the colon becomes the `Comment` text):

```java
// In the args array:
"--edit-spec", "append-comment:silver-thread-edit-save-readback-proof",
// Change to:
"--edit-spec", "append-comment:your-new-marker-text",

// Update the expected marker to match:
String expectedMarker = "your-new-marker-text";
```

The rest of the chain (save, readback, verification) remains unchanged because
the save pipeline is agnostic to the edit content.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `require-tweedle-lang-submodule` enforcer failure | Grammar submodule not initialized. | Run `git submodule update --init tweedle-lang`. |
| `EatmeEditProcedure.run()` returns non-zero | Edit spec not recognized or project structure mismatch. | Verify `projectWithSceneMethod("eatmeFirstLesson")` produces a project with a `Scene.eatmeFirstLesson` method. |
| `IllegalStateException: singleton already set` | `resetApplicationSingleton()` not called between edit and save phases. | Verify the reset call appears after `EatmeEditProcedure.run()` and before `TestProjectApplication` construction. |
| `NullPointerException` in `updateTitle` | Using production `updateTitle` instead of no-op override. | Ensure `TestProjectApplication` overrides `updateTitle()`. |
| `IoUtilities.readProject` returns null | Archive file is empty or corrupt. | Check that the preceding write or save completed without exception. |
| `OutOfMemoryError` during Maven build | Insufficient heap for the reactor. | Set `NODE_OPTIONS=--max-old-space-size=32768`. |
| Test not found by Surefire | Wrong test name or package. | Use `-Dtest=org.alice.ide.SilverThreadEditSaveReadbackTest`. |
| `edited-project.a3p` not found | `EatmeEditProcedure.run()` wrote to an unexpected path. | Check that `--evidence-dir` points to the temp directory where the test looks for the file. |
| Second statement is not a Comment | `EatmeEditProcedure` changed its edit behavior. | Verify the `--edit-spec "append-comment:<text>"` contract still appends a `Comment` as the last statement. |
