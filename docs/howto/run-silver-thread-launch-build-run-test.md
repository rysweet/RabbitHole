# Run the Silver Thread Launch-Build-Run End-to-End Test

Use this guide to run and review the first real end-to-end silver thread test
for Alice. The test proves the core student journey works headlessly: create a
project, add a statement, save, reopen, execute through the virtual machine,
verify execution, and verify round-trip fidelity.

For the full contract, see the [Silver Thread Launch-Build-Run Test
reference](../reference/silver-thread-launch-build-run-test.md).

## Before you start

Confirm the test class and test resource exist. If either is missing, the test
has not been implemented yet — see the [reference](../reference/silver-thread-launch-build-run-test.md)
for the design specification.

```bash
test -f core/ide/src/test/java/org/alice/ide/SilverThreadLaunchBuildRunTest.java && echo "Test class OK" || echo "Test class MISSING"
test -f core/ide/src/test/resources/starters/indiaMinimum.a3p && echo "Starter OK" || echo "Starter MISSING"
```

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the focused test

Run both test methods:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadLaunchBuildRunTest \
  test
```

This command does not require a display server, JavaFX toolkit, or network
access. Do not add `xvfb-run`, Swing automation, or timeout-based success
criteria.

## Run a single test method

To run only the full journey test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='org.alice.ide.SilverThreadLaunchBuildRunTest#createProjectAddCommentSaveReopenExecuteAndVerifyRoundTrip' \
  test
```

To run only the starter project test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='org.alice.ide.SilverThreadLaunchBuildRunTest#loadRealStarterProjectInspectSaveCopyAndReopen' \
  test
```

## Review the full journey test

Review `createProjectAddCommentSaveReopenExecuteAndVerifyRoundTrip` for these
assertions:

| Step | Assertion | Meaning |
| --- | --- | --- |
| Create project | Project and program type are non-null. | Synthetic project construction works without IDE. |
| Add Comment | Statement count in method body is 1. | The AST accepted the statement. |
| Save | `.a3p` file exists and is non-empty. | `IoUtilities.writeProject` serialized successfully. |
| Reopen | Loaded project is non-null; program type name matches. | `TestFileProjectLoader` deserialized the archive. |
| Execute via VM | Listener records exactly 4 events: `executing:BlockStatement`, `executing:Comment`, `executed:Comment`, `executed:BlockStatement`. | `ReleaseVirtualMachine` dispatched all expected callbacks. |
| Round-trip save | Second save file exists. | Re-serialization succeeds. |
| Round-trip reopen | Program type name, method name, statement count, and Comment text all match originals. | Full AST fidelity across two serialization cycles. |

## Review the starter project test

Review `loadRealStarterProjectInspectSaveCopyAndReopen` for these assertions:

| Step | Assertion | Meaning |
| --- | --- | --- |
| Load starter | Project is non-null. | Real `.a3p` archive parsed successfully. |
| Inspect program type | Name is not null and not empty. | Archive contains a named program type. |
| Inspect program type | Type is assignable to `SProgram`. | Archive's program type extends the expected base. |
| Save copy | Copy file exists on disk. | Re-serialization of a real project succeeds. |
| Reopen copy | Program type name equals original. | Round-trip preserves the real project's identity. |

## Keep the claim narrow

Cite this test only for the headless create→build→run→save→reopen silver thread
and the starter-project load→inspect→copy→reopen journey. Do not cite it as
evidence for:

- 3D rendering correctness
- Drag-and-drop code tile UI
- Gallery or model resource loading
- JavaFX display or scene rendering
- Lesson completion, grading, or assessment
- Desktop UI automation
- Installer or packaging behavior

Adjacent claims are owned by separate documents:

| Claim | Document |
| --- | --- |
| Headless VM events (standalone) | `VirtualMachineHeadlessRuntimeEventTest` in `core/ast`. |
| Project save/export operations | [Characterize Project Save and Export Operations](./characterize-project-save-export-operations.md). |
| First-lesson code-editor action | [Run the First-Lesson Code-Editor Action Proof](./run-first-lesson-code-editor-action-proof.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](../reference/silver-thread-status-report.md). |
