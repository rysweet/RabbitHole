# Run the Silver Thread Edit-Save-Readback End-to-End Test

Use this guide to run and review the connected silver thread test that chains
procedure editing through production save to archive readback. The test proves
the full edit → save → readback journey works headlessly: build a starter
project, run `EatmeEditProcedure` to append a comment, save via
`ProjectApplication.saveProjectTo`, reopen the saved archive, and verify the
appended comment survived.

For the full contract, see the [Silver Thread Edit-Save-Readback Test
reference](../reference/silver-thread-edit-save-readback-test.md).

## Before you start

Confirm the test class exists:

```bash
test -f core/ide/src/test/java/org/alice/ide/SilverThreadEditSaveReadbackTest.java && echo "Test class OK" || echo "Test class MISSING"
```

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the focused test

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadEditSaveReadbackTest \
  test
```

This command does not require a display server, JavaFX toolkit, or network
access. Do not add `xvfb-run`, Swing automation, or timeout-based success
criteria.

## What makes this test "connected"

The existing silver thread tests each prove one operation:

- `EatmeEditProcedureTest` proves editing a procedure via `EatmeEditProcedure`.
- `SilverThreadSaveTest` proves saving via `ProjectApplication.saveProjectTo`.

This test chains both operations into a single journey. It answers the question:
"Does a comment appended by `EatmeEditProcedure` survive the production save
pipeline and remain present when the saved archive is reopened?"

The chain is:

```text
EatmeEditProcedure.run()  →  resetApplicationSingleton()
    →  TestProjectApplication(editedProject)  →  saveProjectTo(savedFile)
    →  IoUtilities.readProject(savedFile)  →  verify Comment survived
```

The critical transition is between steps 1 and 4: the edited project must be
loadable by `TestProjectApplication` and compatible with the production save
path. This transition is not tested by either `EatmeEditProcedureTest` or
`SilverThreadSaveTest` individually.

## Review the test

Review `editedCommentSurvivesProductionSaveAndReadback` for these assertions:

| Step | Assertion | Meaning |
| --- | --- | --- |
| Build starter project | Project has Scene with `eatmeFirstLesson` method containing 1 Comment. | `projectWithSceneMethod` builds a valid starter. |
| Run EatmeEditProcedure | Exit status is 0; `edited-project.a3p` exists. | The CLI tool successfully edited the procedure. |
| Read edited project | Method body has 2 statements; second is `Comment("silver-thread-edit-save-readback-proof")`. | The edit was applied correctly. |
| Reset singleton | No assertion. | Required to clear the anonymous Application from `EatmeEditProcedure`. |
| Boot TestProjectApplication | Application singleton is set. | Headless application accepted the edited project. |
| Save via `saveProjectTo` | `.a3p` file exists and is non-empty. | Production save pipeline serialized the edited project. |
| Readback via `IoUtilities.readProject` | Reopened project is non-null; Comment text is `"silver-thread-edit-save-readback-proof"`. | The appended comment survived the full chain. |
| Proof artifact | JSON file exists with `schema_version: "eatme.alice-edit-save-readback-proof/v1"`. | Machine-readable chain evidence was recorded. |

## Compare with adjacent tests

| Dimension | `EatmeEditProcedureTest` | `SilverThreadSaveTest` | **`SilverThreadEditSaveReadbackTest`** |
| --- | --- | --- | --- |
| Edit path | `EatmeEditProcedure.run()` | Direct AST manipulation | `EatmeEditProcedure.run()` |
| Save path | `IoUtilities.writeProject` (inside EatmeEditProcedure) | `ProjectApplication.saveProjectTo` | `ProjectApplication.saveProjectTo` |
| Readback | `IoUtilities.readProject` on edited output | `IoUtilities.readProject` on saved output | `IoUtilities.readProject` on **production-saved** output |
| Chain coverage | Edit only | Save only | **Edit → Save → Readback** |
| Singleton reset | Not needed | Yes | Yes (between edit and save) |
| Starter project | Synthetic `eatmeFirstLesson` | Synthetic type | Synthetic `eatmeFirstLesson` |

The three tests are complementary:

- `EatmeEditProcedureTest` proves editing works in isolation.
- `SilverThreadSaveTest` proves production saving works in isolation.
- `SilverThreadEditSaveReadbackTest` proves the operations compose correctly.

## Keep the claim narrow

Cite this test only for the headless edit-save-readback chain. Do not cite it
as evidence for:

- 3D rendering correctness
- Drag-and-drop code tile UI
- Gallery or model resource loading
- JavaFX display or scene rendering
- Lesson completion, grading, or assessment
- Desktop UI automation (File menu, dialog interaction)
- Installer or packaging behavior
- Object placement (tested in `EatmeEditProcedureTest`)
- Save backup creation (tested in `ProjectApplicationSaveProjectToTest`)
- VM execution events (tested in `SilverThreadLaunchBuildRunTest`)
- Real starter project compatibility (uses synthetic project)

Adjacent claims are owned by separate documents:

| Claim | Document |
| --- | --- |
| Headless create → build → run → save → reopen | [Run the Silver Thread Launch-Build-Run Test](./run-silver-thread-launch-build-run-test.md). |
| Production save round-trip (synthetic project) | [Run the Silver Thread Save Round-Trip Test](./run-silver-thread-save-round-trip-test.md). |
| EatmeEditProcedure standalone editing | [Run the First-Lesson Procedure/Edit Handoff Proof](./run-first-lesson-procedure-edit-handoff.md). |
| `saveProjectTo` edge cases | [Characterize Project Save and Export Operations](./characterize-project-save-export-operations.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](../reference/silver-thread-status-report.md). |
