# Run the First-Lesson Procedure/Edit Handoff Proof

Use this guide to collect the narrow evidence that deterministic object
placement is handed into deterministic procedure editing for
`scene.eatmeFirstLesson`.

## When to use this guide

Use it when reviewing the procedure/edit seam or the outside-in QA registry entry
for the handoff smoke. The proof is limited to one generated starter project, one
placed bunny artifact, one deterministic procedure edit, and the focused
assertions that reopen the edited project.

For the full artifact and API contract, see the
[First-Lesson Procedure/Edit Seam reference](../reference/first-lesson-procedure-edit-seam.md).

## Before you start

Run commands from the repository root.

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the focused Maven proof

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.EatmeEditProcedureTest#chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff \
  test
```

This command is the canonical handoff proof. It creates a temporary starter
project, runs object placement, passes the resulting `placed-project.a3p` to the
procedure-edit utility, reopens `edited-project.a3p`, and checks both the placed
bunny field and the appended `Comment` statement.

## Run the QA handoff smoke

Validate the scenario catalog:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Run the handoff smoke through the QA runner:

```bash
rm -rf /tmp/alice-procedure-edit-handoff
ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-procedure-edit-handoff-smoke \
  --evidence-dir /tmp/alice-procedure-edit-handoff \
  --timeout-seconds 900
```

The runner writes command evidence for the focused Maven proof. If
`ALICE_QA_RUN_GATED_SMOKES` is not set to `1`, the scenario records a
`gated-not-run` status instead of executing the Maven command.

## Review the evidence

The handoff proof accepts this artifact chain:

```text
starter-project.a3p
  -> EatmePlaceObject.run(...)
  -> placed-project.a3p
  -> EatmeEditProcedure.run(...)
  -> edited-project.a3p
```

Review these facts:

| Evidence | Required fact |
| --- | --- |
| `placement.json` | The supported object identifier is `alice-gallery://animals/bunny`. |
| `scene.diff.json` | The scene field list includes the added bunny field after placement. |
| `procedure-edit.json` | `input_project_artifact` is `placed-project.a3p`. |
| `procedure-edit-command.json` | The `append-comment` command completed and increased the statement count by one. |
| `procedure.diff.json` | The targeted scene method is present after the edit. |
| `procedure-tab-selection.json` | The in-editor procedure tab selection helper selected `eatmeFirstLesson`. |
| `edited-project.a3p` | Reopening the archive finds the placed bunny field and the appended `Comment` statement. |

`procedure-ui-action-no-go.json` is allowed only as the narrow blocker for the
missing desktop code-editor edit action target. It is not evidence that a desktop
UI edit action completed.

## Claim boundaries

This proof may claim that the object-placement artifact is handed into the
procedure-edit utility and that the edited project preserves the placed bunny
while adding the deterministic procedure comment.

Do not cite this proof as evidence for full first-lesson completion, grading,
creative assessment, visible rendering correctness, full UI automation, active
Save completion, launcher behavior, model exporter behavior, hotspot behavior, or
Select Project PID behavior.

