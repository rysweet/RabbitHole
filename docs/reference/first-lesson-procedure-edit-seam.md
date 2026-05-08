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
2. `EatmeEditProcedure.run(...)` loads that `placed-project.a3p`, selects
   `scene.eatmeFirstLesson`, finds or creates the targeted scene method, applies
   the supported deterministic procedure edit, and writes `edited-project.a3p`
   plus narrow AST edit evidence.
3. The focused JUnit characterization reopens `edited-project.a3p` and asserts
   both that the placed bunny field is still present and that the expected
   `Comment` statement exists in the targeted scene procedure.

The proof is an AST/project edit seam for `scene.eatmeFirstLesson` on a
synthetic/generated test project. It is not proof of a real first-lesson starter
file, the full first-lesson project shape, desktop rendering, grading, creative
assessment, Save, launcher, model exporter, hotspot, or Select Project PID
behavior.

The adjacent planned live-desktop seam is [First-Lesson Live Procedure Target
Observation](./first-lesson-live-procedure-target-observation.md). That future
shard starts after Select Project opens the first-lesson starter and only
observes whether the live desktop exposes a stable procedure tab or code-editor
target for `scene.eatmeFirstLesson`. It will not replace this AST/project edit
proof, and this AST/project edit proof does not replace the planned live target
observation.

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
| Edit | `edited-project.a3p`, `procedure-edit.json`, `procedure-edit-command.json`, `procedure.diff.json`, and `procedure-tab-selection.json` exist in the evidence directory. |
| AST assertion | Reopening `edited-project.a3p` finds the placed bunny field and finds or creates `scene.eatmeFirstLesson` with the deterministic appended `Comment` statement. |
| UI boundary | Every successful procedure-edit run emits exactly one `procedure-ui-action-no-go.json` blocker artifact recording the missing desktop code-editor edit action target. |

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
| `procedure-edit.json` | `EatmeEditProcedure` | `eatme.alice-procedure-edit-artifact/v1` | Procedure selector, edit spec, input project artifact, scene type, method name, method creation flag, statement counts, and edited project file name. |
| `procedure-edit-command.json` | `EatmeEditProcedure` | `eatme.alice-procedure-edit-command/v1` | Command-level append-comment result and statement-count delta. |
| `procedure.diff.json` | `EatmeEditProcedure` | `eatme.alice-procedure-edit-diff/v1` | Scene method names before and after the edit plus statement-count delta. |
| `procedure-tab-selection.json` | `EatmeEditProcedure` | `eatme.alice-procedure-tab-selection/v1` | Croquet/DeclarationsEditor tab-selection helper evidence for the targeted `UserMethod`. |
| `procedure-ui-action-no-go.json` | `EatmeEditProcedure` | `eatme.alice-code-procedure-ui-action-no-go/v1` | Precise blocker when a desktop code-editor edit action target is not available. |

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
| `schema_version` | `eatme.alice-procedure-edit-result/v1`. |
| `status` | `edited` when the project archive and edit evidence were written. |
| `procedure_selector` | The requested selector, such as `scene.eatmeFirstLesson`. |
| `edited_project_artifact` | Always `edited-project.a3p`. |
| `procedure_edit_command` | Always `procedure-edit-command.json`. |
| `procedure_or_code_diff` | Always `procedure.diff.json`. |
| `procedure_tab_selection` | Always `procedure-tab-selection.json`. |
| `procedure_ui_action_no_go` | Always `procedure-ui-action-no-go.json`. |

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

`procedure-ui-action-no-go.json` is scoped only to the missing desktop
code-editor UI edit action target. If object placement, project handoff, method
lookup/creation, or AST editing cannot be exercised, the characterization must
fail or surface that error directly; it must not repurpose
`procedure-ui-action-no-go.json` as a general placement-to-AST blocker.

For the current seam, every successful `EatmeEditProcedure` run emits
`procedure-ui-action-no-go.json` because the AST edit is implemented but no
stable desktop code-editor edit action target is available yet. The blocker
artifact uses schema `eatme.alice-code-procedure-ui-action-no-go/v1` and must
include:

| Field | Required content |
| --- | --- |
| `status` | `blocked` |
| `source` | `EatmeEditProcedure` |
| `procedure_selector` | The targeted selector, such as `scene.eatmeFirstLesson`. |
| `edit_spec` | The requested edit spec. |
| `exact_missing_ui_edit_action_target` | The precise desktop code-editor edit target that is unavailable. |
| `blocker_codes` | Stable machine-readable codes for the missing seam. |
| `required_next` | The minimum implementation steps needed to unblock the seam. |
| `doesNotClaim` | Explicit exclusions for UI action completion, rendering, lesson completion, grading, and creative assessment. |

The blocker must not be accompanied by additional speculative blocker files.

## Evidence boundaries

This seam may claim only:

- Generated placement and procedure-edit artifacts exist.
- `placed-project.a3p` is accepted as the input to the deterministic edit step.
- `edited-project.a3p` reopens, still contains the placed bunny field, and
  contains the expected AST-level procedure edit.
- The Croquet/DeclarationsEditor tab-selection helper selected the targeted
  method when its artifact is present.

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
  procedure-edit-command.json
  procedure-edit.json
  procedure-tab-selection.json
  procedure.diff.json
  procedure-ui-action-no-go.json
  scene.diff.json
```

The focused Maven test asserts this shape in a JUnit temporary directory. The QA
smoke records that the Maven proof ran successfully; it does not preserve this
directory. The proof accepts `procedure-ui-action-no-go.json` only as a narrow
blocker for desktop code-editor action invocation. The AST/project chain remains
proven by reopening `edited-project.a3p`, checking that the placed bunny field
survived the handoff, and checking the targeted `UserMethod` body.
