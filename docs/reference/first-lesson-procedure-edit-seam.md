# First-Lesson Procedure/Edit Seam

This reference describes the narrow executable seam that chains deterministic
object placement into deterministic procedure editing on a synthetic/generated
test project.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Executable proof](#executable-proof)
- [Artifact contract](#artifact-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation](#validation)
- [Blocker contract](#blocker-contract)
- [Evidence boundaries](#evidence-boundaries)
- [Example evidence flow](#example-evidence-flow)

## Scope

The seam proves one repository-owned behavior:

1. `EatmePlaceObject.run(...)` loads a synthetic/generated test Alice project
   and writes `placed-project.a3p` plus narrow placement evidence.
2. `EatmeEditProcedure.run(...)` loads that `placed-project.a3p`, selects the
   existing `scene.eatmeFirstLesson` method, applies the supported deterministic
   procedure edit, and writes `edited-project.a3p` plus narrow action proof
   evidence.
3. The focused JUnit characterization reopens `edited-project.a3p` and asserts
   both that the placed bunny field is still present and that the expected
   `Comment` statement exists in the targeted scene procedure.

The proof is an AST/project edit seam for `scene.eatmeFirstLesson` on a
synthetic/generated test project. It is not proof of a real first-lesson starter
file, the full first-lesson project shape, desktop rendering, grading, creative
assessment, Save, launcher, model exporter, hotspot, or Select Project PID
behavior.

The adjacent live-desktop seam is [First-Lesson Live Procedure Target
Action Seam](./first-lesson-live-procedure-target-observation.md). That shard
starts after Select Project opens the first-lesson starter and records whether
the live `scene.eatmeFirstLesson` procedure/code-editor target is ready for a
public desktop edit action, or blocked by the missing public
`CodeEditor`/`CodeComposite` edit invocation contract. It does not replace this
AST/project edit proof, and this AST/project edit proof does not replace the live
target/action seam.

The focused backing-action seam is [First-Lesson Code-Editor Action
Proof](./first-lesson-code-editor-action-proof.md). That proof owns the narrow
claim that the selected `scene.eatmeFirstLesson` `CodeComposite` and
`CodeEditor.getCode()` backing model accept one deterministic append-comment
action and produce target-only marker evidence.

## Usage

Use the seam when a review needs evidence that a deterministic object placement
artifact is accepted by the deterministic procedure-edit step.

Run the focused handoff proof:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.EatmeEditProcedureTest#chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff \
  test
```

Run the outside-in QA wrapper for the same handoff command:

```bash
rm -rf /tmp/alice-procedure-edit-handoff
ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-procedure-edit-handoff-smoke \
  --evidence-dir /tmp/alice-procedure-edit-handoff \
  --timeout-seconds 900
```

Run the narrower procedure-edit seam smoke when the review is about edit
artifact assertions and the precise desktop edit-action no-go artifact rather
than the object-placement handoff:

```bash
rm -rf /tmp/alice-procedure-edit-seam
ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-procedure-edit-seam-smoke \
  --evidence-dir /tmp/alice-procedure-edit-seam \
  --timeout-seconds 900
```

Review the focused JUnit assertions as the durable handoff proof. The Maven test
asserts the generated evidence files in a JUnit temporary workspace; the QA
runner records command-level smoke evidence for that Maven proof rather than
preserving those JSON and `.a3p` artifacts for review. Do not substitute Save,
launcher, model exporter, hotspot, Select Project PID, rendering,
lesson-completion, grading, or creative-assessment evidence for this seam.

## Executable proof

The proof lives in:

```text
core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java
```

The characterization must use a temporary workspace owned by JUnit. It first
calls the object-placement tool and then passes the generated project to the
procedure-edit tool:

```text
starter-project.a3p
  -> EatmePlaceObject.run(...)
  -> placed-project.a3p
  -> EatmeEditProcedure.run(...)
  -> edited-project.a3p
```

Accepted success criteria:

| Step | Required observation |
| --- | --- |
| Placement | `placed-project.a3p`, `placement.json`, and `scene.diff.json` exist in the evidence directory. |
| Edit | `edited-project.a3p` and `first-lesson-code-editor-action-proof.json` exist in the evidence directory. |
| AST assertion | Reopening `edited-project.a3p` finds the placed bunny field and the existing `scene.eatmeFirstLesson` method with the deterministic appended `Comment` statement. |
| UI boundary | Successful runs do not emit `procedure-ui-action-no-go.json`. |

The focused Maven test creates these artifacts inside a JUnit `TemporaryFolder`
and asserts their contents before the workspace is discarded. The
`alice-desktop-procedure-edit-handoff-smoke` QA scenario is a gated command
smoke for that Maven proof; its reviewable evidence is the scenario status and
command log, not a retained copy of the JSON or `.a3p` artifact chain.

## Artifact contract

All artifacts are generated under the test evidence directory owned by the tool
or JUnit run. Do not commit generated `.a3p` or JSON evidence files. In the
focused JUnit proof, the files below are asserted artifacts, not retained review
artifacts. In the QA smoke, the scenario evidence proves that the focused Maven
command completed successfully; it does not persist the JSON or `.a3p` artifacts
from the JUnit temporary workspace.

| Artifact | Producer | Schema | Purpose |
| --- | --- | --- | --- |
| `placed-project.a3p` | `EatmePlaceObject` | Alice project archive | Project after deterministic bunny placement. |
| `placement.json` | `EatmePlaceObject` | `eatme.alice-object-placement-artifact/v1` | Object identifier, scene type, field name, field type, resource, and placed project file name. |
| `scene.diff.json` | `EatmePlaceObject` | `eatme.alice-object-placement-diff/v1` | Scene field names before and after placement. |
| `edited-project.a3p` | `EatmeEditProcedure` | Alice project archive | Project after deterministic procedure edit. |
| `first-lesson-code-editor-action-proof.json` | `EatmeEditProcedure` | `eatme.alice-first-lesson-code-editor-action-proof/v1` | Procedure selector, edit spec, input project artifact, selected tab/code-editor backing evidence, statement counts, marker counts, and edited project file name. |

`EatmePlaceObject` also writes one JSON result object to standard output:

| Field | Meaning |
| --- | --- |
| `schema_version` | `eatme.alice-object-placement-result/v1`. |
| `status` | `placed` when the project archive and placement evidence were written. |
| `object_identifier` | The requested object identifier. |
| `placement_artifact` | Always `placement.json`. |
| `scene_or_project_diff` | Always `scene.diff.json`. |

`EatmeEditProcedure` writes one JSON result object to standard output:

| Field | Meaning |
| --- | --- |
| `schema_version` | `eatme.alice-first-lesson-code-editor-action-proof-result/v1`. |
| `status` | `proved` when the project archive and action proof evidence were written. |
| `procedure_selector` | The requested selector, such as `scene.eatmeFirstLesson`. |
| `edited_project_artifact` | Always `edited-project.a3p`. |
| `action_proof` | Always `first-lesson-code-editor-action-proof.json`. |

## API reference

These seams are implementation/test utilities in `org.alice.tools`. They are not
public Alice product APIs.

### `EatmePlaceObject.run(...)`

```java
static int run(String[] args, PrintStream out, PrintStream err)
```

Required arguments:

| Argument | Value |
| --- | --- |
| `--project` | Existing starter `.a3p` file. |
| `--object` | Supported deterministic object identifier. The proof uses `alice-gallery://animals/bunny`. |
| `--evidence-dir` | Directory where placement artifacts are written. |
| `--json` | Required flag. Standard output is a single result JSON object. |

Exit codes:

| Code | Meaning |
| --- | --- |
| `0` | Placement succeeded and artifacts were written. |
| `2` | Invalid arguments, unsupported object identifier, missing project, or unsupported project version. |
| `3` | Runtime placement failure. |

Placement writes a private managed `SBiped` field backed by
`org.lgna.story.resources.biped.BunnyResource.DEFAULT`. If `bunny` already
exists, the utility chooses the next available `bunny<N>` field name.

### `EatmeEditProcedure.run(...)`

```java
static int run(String[] args, PrintStream out, PrintStream err)
```

Required arguments:

| Argument | Value |
| --- | --- |
| `--project` | Existing `.a3p` file. The chained proof passes `placed-project.a3p`. |
| `--procedure-selector` | Scene procedure selector. The proof uses `scene.eatmeFirstLesson`. |
| `--edit-spec` | Supported deterministic edit spec. The proof uses `append-comment:<text>`. |
| `--evidence-dir` | Directory where edit artifacts are written. |
| `--json` | Required flag. Standard output is a single result JSON object. |

Exit codes:

| Code | Meaning |
| --- | --- |
| `0` | Procedure edit succeeded and artifacts were written. |
| `2` | Invalid arguments, unsupported selector, unsupported edit spec, missing project, missing scene, or unsupported project version. |
| `3` | Runtime procedure-edit failure. |

The supported selector form is `scene.<methodName>`, where `<methodName>` is one
Java-style identifier. `EatmeEditProcedure` finds that scene method or creates it
when missing. The supported edit form is `append-comment:<non-blank text>`.

The edit is accepted only after `edited-project.a3p` is written. Reopening the
archive must show the selected scene method and the appended AST `Comment`
statement. In the chained handoff proof, reopening the archive must also show
that the placed bunny field survived the edit.

## Configuration

No runtime product preference is required for the seam. The proof uses temporary
evidence directories and deterministic tool arguments.

| Setting | Required value | Used by |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Surrounding Node-based orchestration and QA wrapper commands. |
| `ALICE_QA_RUN_GATED_SMOKES` | `1` | `run-scenario.sh` when executing `gated-command-smoke` scenarios. |
| `--evidence-dir` | A writable directory outside committed source. In the focused JUnit proof this is a temporary folder; in the QA smoke it is the scenario evidence directory, such as `/tmp/alice-procedure-edit-handoff`. | Placement/edit utilities and QA scenario runner. |
| `tweedle-lang` submodule | Initialized with `git submodule update --init tweedle-lang`. | Focused Maven reactor validation. |

Use the saved orchestration memory setting when invoking Maven through the
Node-based workflow wrapper:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Initialize the Tweedle grammar submodule before broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Validation

Run the focused handoff characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.EatmeEditProcedureTest#chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff \
  test
```

This command is the validation surface for the chained placement-to-procedure
proof. It does not validate active Save behavior, model export, hotspots, Select
Project PID handling, launcher behavior, rendering, or broad desktop automation.

## Blocker contract

The implemented proof emits `first-lesson-code-editor-action-proof.json` on
success and does not emit `procedure-ui-action-no-go.json`. If object placement,
project handoff, target lookup, backing selection, marker isolation, or AST
editing cannot be exercised, the characterization must fail or surface that
error directly; it must not write success-shaped evidence.

## Evidence boundaries

This seam may claim only:

- Generated placement and first-lesson action proof artifacts exist.
- `placed-project.a3p` is accepted as the input to the deterministic edit step.
- `edited-project.a3p` reopens, still contains the placed bunny field, and
  contains the expected AST-level procedure edit.
- The Croquet/DeclarationsEditor tab-selection helper selected the targeted
  method and the selected `CodeEditor.getCode()` backing matched that method.

This seam must not claim:

- Full first-lesson project shape.
- Full first-lesson completion.
- Grading, scoring, or creative assessment.
- Visible rendering correctness.
- Active Save proof.
- Desktop code-editor action completion unless a repository test directly
  invokes and observes that action.
- Launcher, Select Project PID, model exporter, or hotspot behavior.

## Example evidence flow

The successful chained proof asserts a single temporary evidence directory shaped
like:

```text
evidence/
  edited-project.a3p
  placed-project.a3p
  placement.json
  first-lesson-code-editor-action-proof.json
  scene.diff.json
```

The focused Maven test asserts this shape in a JUnit temporary directory. The QA
smoke records that the Maven proof ran successfully; it does not preserve this
directory. The AST/project chain remains proven by reopening
`edited-project.a3p`, checking that the placed bunny field survived the handoff,
and checking the targeted `UserMethod` body.
