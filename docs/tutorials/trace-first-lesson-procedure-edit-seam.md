# Tutorial: Trace the First-Lesson Procedure/Edit Seam

This tutorial shows how to review the executable proof that chains deterministic
object placement into deterministic procedure editing on a synthetic/generated
test project.

## What you will do

You will:

1. Prepare the repository for focused `core/ide` validation.
2. Run the chained placement-to-edit characterization.
3. Run the QA smoke wrapper for the same handoff path.
4. Review the asserted artifact contract.
5. Check the narrow evidence boundaries.

## Before you start

Open a terminal at the repository root:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

If Node-based orchestration wraps the run, set:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Run the focused characterization

Run the handoff test in `EatmeEditProcedureTest`:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.EatmeEditProcedureTest#chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff \
  test
```

That test owns the narrow proof. It creates a synthetic/generated test project
in a JUnit temporary workspace, runs `EatmePlaceObject.run(...)`, then runs
`EatmeEditProcedure.run(...)` against the generated `placed-project.a3p`.

## Step 2: Run the QA smoke wrapper

The outside-in QA runner has a gated smoke for the same handoff proof:

```bash
rm -rf /tmp/alice-procedure-edit-handoff
ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-procedure-edit-handoff-smoke \
  --evidence-dir /tmp/alice-procedure-edit-handoff \
  --timeout-seconds 900
```

Use this path when a review needs scenario-runner evidence in addition to the
focused Maven test. It still proves only the handoff and deterministic AST edit;
it records scenario status and command log evidence rather than preserving the
JUnit JSON or `.a3p` artifacts, and it does not become a full desktop UI
automation proof.

## Step 3: Follow the proof chain

The test follows this chain:

```text
temporary synthetic test project
  -> object placement
  -> placed-project.a3p
  -> procedure edit
  -> edited-project.a3p
```

The placement step uses:

```text
--object alice-gallery://animals/bunny
```

The edit step uses:

```text
--procedure-selector scene.eatmeFirstLesson
--edit-spec append-comment:<deterministic proof text>
```

`EatmeEditProcedure` finds or creates the targeted scene method named by
`scene.eatmeFirstLesson`.

The proof is accepted only after the edited project is reopened, the placed
bunny field is still present, and the targeted scene procedure contains the
expected appended `Comment` statement.

## Step 4: Review placement evidence

The placement step writes:

```text
placed-project.a3p
placement.json
scene.diff.json
```

`placement.json` identifies the object, scene type, field name, field type,
resource, and placed project file name. `scene.diff.json` identifies the scene
field list before and after placement.

Use this evidence only for object-placement facts on the synthetic/generated
test project. It does not prove a real first-lesson starter file, the full
first-lesson project shape, rendering, lesson completion, grading, or Save
behavior.

## Step 5: Review procedure-edit evidence

The procedure-edit step writes:

```text
edited-project.a3p
procedure-edit.json
procedure-edit-command.json
procedure.diff.json
procedure-tab-selection.json
```

The key review points are:

| Artifact | Review point |
| --- | --- |
| `procedure-edit.json` | Names `scene.eatmeFirstLesson`, the edit spec, `placed-project.a3p` input handoff, statement counts, and `edited-project.a3p`. |
| `procedure-edit-command.json` | Confirms the deterministic `append-comment` command completed and changed the statement count by one. |
| `procedure.diff.json` | Records method names before and after the edit and the statement-count delta. |
| `procedure-tab-selection.json` | Records the Croquet/DeclarationsEditor tab-selection helper result for the selected method. |

The final assertion reopens `edited-project.a3p`; the JSON files are asserted
artifacts in the JUnit temporary workspace, not a substitute for the AST/project
assertion and not retained by the QA smoke.

## Step 6: Handle the desktop edit-action blocker

Because the AST edit succeeds but the repository does not expose a stable desktop
code-editor edit action, every successful procedure-edit run currently writes
exactly one blocker:

```text
procedure-ui-action-no-go.json
```

This blocker is acceptable only when it precisely names the missing desktop
edit-action target and includes stable blocker codes. It is not a general
blocker for object placement, project handoff, method lookup/creation, or AST
editing failures. It also does not turn the AST edit into a desktop UI automation
proof.

Review its `doesNotClaim` list before citing the evidence. The seam excludes:

```text
desktop UI action completion
full Alice UI automation
visible rendering correctness
first-lesson completion
full first-lesson project shape
grading
creative assessment
```

## Step 7: Keep generated evidence local

The test writes evidence under a JUnit temporary directory. Do not commit
generated `.a3p` archives or generated JSON evidence. Commit only durable
documentation and the focused characterization test that reproduces the
artifacts.

For task-oriented commands, see
[Run the First-Lesson Procedure/Edit Handoff Proof](../howto/run-first-lesson-procedure-edit-handoff.md).
For the full artifact and API contract, see the
[First-Lesson Procedure/Edit Seam reference](../reference/first-lesson-procedure-edit-seam.md).
