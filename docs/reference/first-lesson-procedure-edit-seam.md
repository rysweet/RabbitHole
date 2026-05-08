# First-Lesson Procedure/Edit Seam

This reference describes the narrow executable seam that chains deterministic
object placement into deterministic procedure editing on a generated starter
project.

## Contents

- [Scope](#scope)
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

1. `EatmePlaceObject.run(...)` loads a starter Alice project and writes
   `placed-project.a3p` plus narrow placement evidence.
2. `EatmeEditProcedure.run(...)` loads that `placed-project.a3p`, selects
   `scene.eatmeFirstLesson`, finds or creates the targeted scene method, applies
   the supported deterministic procedure edit, and writes `edited-project.a3p`
   plus narrow AST edit evidence.
3. The focused JUnit characterization reopens `edited-project.a3p` and asserts
   both that the placed bunny field is still present and that the expected
   `Comment` statement exists in the targeted scene procedure.

The proof is an AST/project edit seam for `scene.eatmeFirstLesson` on a
generated starter project. It is not proof of the full first-lesson project
shape, desktop rendering, grading, creative assessment, Save, launcher, model
exporter, hotspot, or Select Project PID behavior.

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
| UI boundary | If the AST edit succeeds but no desktop code-editor edit action is exposed, exactly one `procedure-ui-action-no-go.json` blocker artifact records that missing UI action target. |

## Artifact contract

All artifacts are generated under the test evidence directory. Do not commit
generated `.a3p` or JSON evidence files.

| Artifact | Producer | Schema | Purpose |
| --- | --- | --- | --- |
| `placed-project.a3p` | `EatmePlaceObject` | Alice project archive | Project after deterministic bunny placement. |
| `placement.json` | `EatmePlaceObject` | `eatme.alice-object-placement-artifact/v1` | Object identifier, scene type, field name, field type, resource, and placed project file name. |
| `scene.diff.json` | `EatmePlaceObject` | `eatme.alice-object-placement-diff/v1` | Scene field names before and after placement. |
| `edited-project.a3p` | `EatmeEditProcedure` | Alice project archive | Project after deterministic procedure edit. |
| `procedure-edit.json` | `EatmeEditProcedure` | `eatme.alice-procedure-edit-artifact/v1` | Procedure selector, edit spec, input project artifact, scene type, method name, method creation flag, statement counts, and edited project file name. |
| `procedure-edit-command.json` | `EatmeEditProcedure` | `eatme.alice-procedure-edit-command/v1` | Command-level append-comment result and statement-count delta. |
| `procedure.diff.json` | `EatmeEditProcedure` | `eatme.alice-procedure-edit-diff/v1` | Scene method names before and after the edit plus statement-count delta. |
| `procedure-tab-selection.json` | `EatmeEditProcedure` | `eatme.alice-procedure-tab-selection/v1` | In-editor procedure tab selection evidence for the targeted `UserMethod`. |
| `procedure-ui-action-no-go.json` | `EatmeEditProcedure` | `eatme.alice-code-procedure-ui-action-no-go/v1` | Precise blocker when a desktop code-editor edit action target is not available. |

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

## Configuration

No runtime product preference is required for the seam. The proof uses JUnit
temporary directories and hard-coded deterministic tool arguments.

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

Run the focused `core/ide` characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeEditProcedureTest \
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

When the AST edit succeeds but no stable desktop edit action is available, the
blocker artifact uses schema `eatme.alice-code-procedure-ui-action-no-go/v1`. It
must include:

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
- The procedure tab selection helper selected the targeted method when its
  artifact is present.

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

The successful chained proof writes a single evidence directory shaped like:

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

The proof accepts `procedure-ui-action-no-go.json` only as a narrow blocker for
desktop code-editor action invocation. The AST/project chain remains proven by
reopening `edited-project.a3p`, checking that the placed bunny field survived
the handoff, and checking the targeted `UserMethod` body.
