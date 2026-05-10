# Run the Silver Thread Save Round-Trip End-to-End Test

Use this guide to run and review the honest save round-trip silver thread test
for Alice. The test proves the production save path works headlessly: create a
project, modify the AST, save via `ProjectApplication.saveProjectTo` (not the
lower-level `IoUtilities.writeProject`), verify the archive on disk, reopen, and
verify full AST fidelity.

For the full contract, see the [Silver Thread Save Round-Trip Test
reference](../reference/silver-thread-save-round-trip-test.md).

## Before you start

Confirm the test class exists:

```bash
test -f core/ide/src/test/java/org/alice/ide/SilverThreadSaveRoundTripTest.java && echo "Test class OK" || echo "Test class MISSING"
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
  -Dtest=org.alice.ide.SilverThreadSaveRoundTripTest \
  test
```

This command does not require a display server, JavaFX toolkit, or network
access. Do not add `xvfb-run`, Swing automation, or timeout-based success
criteria.

## What makes this test "honest"

The existing `SilverThreadLaunchBuildRunTest` saves via `IoUtilities.writeProject`,
which is the low-level archive writer. This test instead saves via
`ProjectApplication.saveProjectTo`, the production code path that:

- Adopts the new file as the project URI (via `ProjectSaveTargetPlan`)
- Calls `ensureProjectCodeUpToDate` (no-op in headless TestProjectApplication)
- Creates a thumbnail (1×1 pixel stub in headless TestProjectApplication)
- Writes the archive through `IoUtilities.writeProject` with thumbnail and manifest
- Creates a save backup of the previous archive
- Records the save in the recent projects list

This is the path that runs when a student clicks File → Save in the IDE.

## Review the test

Review `createModifiedAst_saveViaProjectApplication_reopenAndVerifyAstFidelity`
for these assertions:

| Step | Assertion | Meaning |
| --- | --- | --- |
| Create project | Project and program type are non-null. | `AstUtilities.createType` works without IDE. |
| Modify AST | Method body contains one `Comment` statement with known marker text. | The AST accepted the modification. |
| Boot TestProjectApplication | Application singleton is set and project is loaded. | Headless `ProjectApplication` boots without a display. |
| Save via `saveProjectTo` | `.a3p` file exists and is non-empty. | Production save pipeline serialized successfully. |
| Verify URI adoption | Application URI points to the saved file. | `saveProjectTo` adopted the target as the active project. |
| Reopen via `IoUtilities.readProject` | Loaded project is non-null; program type name matches. | Archive is a valid `.a3p` readable by the standard reader. |
| Verify AST fidelity | Method name, statement count, and `Comment` text all match originals. | Full AST fidelity across the production save → standard reopen cycle. |

## Keep the claim narrow

Cite this test only for the headless production-save round-trip. Do not cite it
as evidence for:

- 3D rendering correctness
- Drag-and-drop code tile UI
- Gallery or model resource loading
- JavaFX display or scene rendering
- Lesson completion, grading, or assessment
- Desktop UI automation (File menu, dialog interaction)
- Installer or packaging behavior
- Save backup creation (tested in `ProjectApplicationSaveProjectToTest`)
- VM execution events (tested in `SilverThreadLaunchBuildRunTest`)

Adjacent claims are owned by separate documents:

| Claim | Document |
| --- | --- |
| Headless create → build → run → save → reopen | [Run the Silver Thread Launch-Build-Run Test](./run-silver-thread-launch-build-run-test.md). |
| `saveProjectTo` backup, URI adoption, and Save-As behavior | [Project Save and Export Operations](./characterize-project-save-export-operations.md). |
| Low-level archive structure and validation | [Project IO Corpus Characterization](../reference/project-io-corpus-characterization.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](../reference/silver-thread-status-report.md). |
