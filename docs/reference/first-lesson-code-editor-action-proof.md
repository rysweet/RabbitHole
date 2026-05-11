# First-Lesson Code-Editor Action Proof

This reference defines the focused proof that `scene.eatmeFirstLesson` can be
selected through the declarations editor/code-editor backing seam and receive one
deterministic edit action.

The executable proof is `org.alice.tools.FirstLessonCodeEditorActionProofTest`.
It is a Java characterization around `ProcedureTabSelection`, `CodeComposite`,
`CodeEditor`, and the gated `EatmeEditProcedure` action path. It does not launch
a broad desktop QA workflow and it does not use workflow timeouts, sleeps,
polling timeouts, or timeout-based success criteria.

## Contents

- [Scope](#scope)
- [Implementation status](#implementation-status)
- [Usage](#usage)
- [Behavior contract](#behavior-contract)
- [Evidence contract](#evidence-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Negative checks](#negative-checks)
- [Blocked fallback](#blocked-fallback)
- [Evidence boundaries](#evidence-boundaries)
- [Examples](#examples)

## Scope

The proof covers exactly this deterministic handoff:

```text
scene.eatmeFirstLesson UserMethod
  -> ProcedureTabSelection selects the procedure tab
  -> selected CodeComposite declares the same UserMethod
  -> selected CodeEditor.getCode() reports the same UserMethod
  -> EatmeEditProcedure applies append-comment:wave4-code-editor-action-proof
  -> reopened project state contains that marker only in scene.eatmeFirstLesson
```

The canonical marker is:

```text
wave4-code-editor-action-proof
```

The proof verifies object identity and after-state observation. It avoids full
generated-source comparisons so source formatting changes do not make the proof
brittle.

## Implementation status

This proof is implemented by the focused Java characterization and the
`EatmeEditProcedure` first-lesson action path.

| Surface | Current state |
| --- | --- |
| `ProcedureTabSelection` backing observations | Implemented by the tab/code-editor backing seam and exposes selected declaration, selected `CodeComposite`, selected `CodeEditor` class, and selected `CodeEditor.getCode()` observations. |
| `EatmeEditProcedure.run(...)` | Accepts only `scene.eatmeFirstLesson` for this proof, applies `append-comment:<text>`, fails closed when the target is missing, and writes `first-lesson-code-editor-action-proof.json` on success. |
| Action proof test | Implemented by `org.alice.tools.FirstLessonCodeEditorActionProofTest`. |
| Success artifact | `first-lesson-code-editor-action-proof.json`. |
| Blocked artifact | Not emitted by the implemented success path. |

## Usage

Run the focused characterization from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.FirstLessonCodeEditorActionProofTest \
  test
```

Run the existing backing and edit utility characterizations when reviewing the
supporting APIs:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.EatmeEditProcedureTest,org.alice.ide.declarationseditor.ProcedureTabSelectionTest \
  test
```

Do not wrap this proof in an outside-in QA scenario just to add a timeout. The
proof is complete when the focused Maven characterization passes and its
temporary evidence assertions hold.

## Behavior contract

`FirstLessonCodeEditorActionProofTest` owns the executable claim.

| Step | Required observation |
| --- | --- |
| Target lookup | The fixture contains exactly one scene procedure selected by `scene.eatmeFirstLesson`. |
| Procedure selection | `ProcedureTabSelection.selectProcedureInEditor(...)` selects the same `UserMethod` instance. |
| Selected declaration | `ProcedureTabSelection.getSelectedProcedureCodeComposite(editor).getDeclaration()` is the same target `UserMethod`. |
| Code-editor backing | `ProcedureTabSelection.getSelectedCodeEditorCode(editor)` returns the same target `UserMethod`. |
| Action validation | Only `append-comment:<marker>` is accepted. Blank, unsupported, or malformed actions fail before mutation. |
| Edit application | `EatmeEditProcedure` appends one AST `Comment` statement carrying `wave4-code-editor-action-proof`. |
| After-state observation | Reopening or re-observing the project finds the marker in `scene.eatmeFirstLesson`. |
| Isolation | The same marker is absent from non-target scene procedures. |
| Result evidence | The structured evidence names the target, selected backing objects, action, marker, before/after statement counts, status, and non-claims. |

The proof must fail closed if the selected declaration, selected
`CodeComposite`, selected `CodeEditor.getCode()` value, target procedure, or
marker observation does not match `scene.eatmeFirstLesson`.

This proof must not create a missing `eatmeFirstLesson` method. Missing target
means failure before mutation.

## Evidence contract

The focused test writes evidence only inside its JUnit temporary workspace. Do
not commit generated `.a3p` or JSON proof artifacts.

The success artifact is named:

```text
first-lesson-code-editor-action-proof.json
```

Schema:

```json
{
  "schema_version": "eatme.alice-first-lesson-code-editor-action-proof/v1",
  "status": "proved",
  "procedure_selector": "scene.eatmeFirstLesson",
  "edit_spec": "append-comment:wave4-code-editor-action-proof",
  "input_project_artifact": "<fixture .a3p path>",
  "scene_type": "<scene NamedUserType name>",
  "method_name": "eatmeFirstLesson",
  "selection_mode": "in_editor_procedure_tab_operation",
  "selected_declaration": "eatmeFirstLesson",
  "code_composite_declaration": "eatmeFirstLesson",
  "code_editor_backing": "org.alice.ide.codeeditor.CodeEditor",
  "code_editor_code": "eatmeFirstLesson",
  "operation_fired": true,
  "action": "append-comment",
  "marker": "wave4-code-editor-action-proof",
  "before_statement_count": 1,
  "after_statement_count": 2,
  "statement_count_delta": 1,
  "target_marker_count": 1,
  "wrong_target_marker_count": 0,
  "before_methods": ["eatmeFirstLesson", "<other scene methods>"],
  "after_methods": ["eatmeFirstLesson", "<other scene methods>"],
  "edited_project": "<edited .a3p path>",
  "success": true,
  "doesNotClaim": [
    "full first-lesson completion",
    "first-lesson completion",
    "grading",
    "creative assessment",
    "visible rendering correctness",
    "broad UI automation",
    "Save-menu completion"
  ]
}
```

The test asserts a contract-relevant subset of these fields: `schema_version`,
`status`, `procedure_selector`, `selected_declaration`,
`code_composite_declaration`, `code_editor_backing`, `code_editor_code`,
`action`, `marker`, `before_statement_count`, `after_statement_count`,
`target_marker_count`, `wrong_target_marker_count`, and `doesNotClaim`. The
remaining fields (`edit_spec`, `input_project_artifact`, `scene_type`,
`method_name`, `selection_mode`, `operation_fired`, `statement_count_delta`,
`before_methods`, `after_methods`, `edited_project`) are informational and
present in the artifact but not contract-asserted by the test.

`target_marker_count` must be exactly `1` for the canonical marker.
`wrong_target_marker_count` must be `0`; a marker in any other procedure
invalidates the proof.

The older `procedure-edit.json`, `procedure-edit-command.json`,
`procedure.diff.json`, `procedure-tab-selection.json`, and
`procedure-ui-action-no-go.json` artifacts are not emitted by this success path
and must not substitute for this action proof schema.

## API reference

These APIs are implementation and test utilities. They are not public Alice
product APIs.

### `ProcedureTabSelection`

`ProcedureTabSelection` exposes the backing observations used before the edit
action is accepted.

| Method | Required behavior |
| --- | --- |
| `getSelectionOperation(DeclarationsEditorComposite, UserMethod)` | Returns the Croquet operation for the target procedure tab. Rejects null methods and non-procedures. |
| `selectProcedureInEditor(DeclarationsEditorComposite, UserMethod, UserActivity)` | Fires the tab operation and returns the selected `UserMethod`. Throws if the selected procedure is not the supplied method. |
| `getSelectedProcedure(DeclarationsEditorComposite)` | Returns the selected procedure, or `null` when no procedure tab is selected. |
| `getSelectedProcedureCodeComposite(DeclarationsEditorComposite)` | Returns the selected procedure `CodeComposite`, or `null` when the selected tab is not a procedure code tab. |
| `getSelectedCodeEditorCode(DeclarationsEditorComposite)` | Returns the selected tab's backing `CodeEditor.getCode()` value. Returns `null` when no procedure tab is selected. Throws if a procedure tab is selected but not backed by a `CodeEditor`. |
| `getSelectedCodeEditorBackingClassName(DeclarationsEditorComposite)` | Returns the selected tab's backing code-editor class name so evidence can distinguish the real `CodeEditor` seam from a name-only selection. |

The action proof must assert all three identities before mutation:

```java
assertSame(target, ProcedureTabSelection.getSelectedProcedure(editor));
assertSame(target,
    ProcedureTabSelection.getSelectedProcedureCodeComposite(editor).getDeclaration());
assertSame(target, ProcedureTabSelection.getSelectedCodeEditorCode(editor));
```

### `EatmeEditProcedure.run(...)`

`EatmeEditProcedure` exposes the CLI-style `run(...)` entry point used by the
proof. For this action proof, it fails closed when `scene.eatmeFirstLesson` is
missing and records the selected declaration, selected `CodeComposite`, selected
`CodeEditor` class, and selected `CodeEditor.getCode()` identities.

CLI arguments:

| Argument | Required value |
| --- | --- |
| `--project` | Existing `.a3p` project file from the focused fixture. |
| `--procedure-selector` | `scene.eatmeFirstLesson` for this proof. |
| `--edit-spec` | `append-comment:wave4-code-editor-action-proof`. |
| `--evidence-dir` | Writable temporary evidence directory. |
| `--json` | Required; standard output is one structured result object. |

Exit codes:

| Code | Meaning |
| --- | --- |
| `0` | The action proof succeeded with target/backing identity and target-only marker evidence. |
| `2` | Invalid arguments, missing target, unsupported selector, unsupported action, blank marker, wrong target, or pre-existing marker evidence. Missing target must be gated to fail for this proof. |
| `3` | Runtime or invariant failure after validation, including selected/backing mismatches or after-state marker observation mismatches. |

Supported selectors are intentionally narrow for this proof. The selector must
name the scene procedure `scene.eatmeFirstLesson`; a different selector may be
covered by a different characterization but is not this proof.

Supported edit specs are intentionally whitelisted:

```text
append-comment:<non-blank marker>
```

The action result is valid only when the observed selected method and the target
method are the same object and the after-state marker count matches the expected
target-only observation.

## Configuration

No Alice product preference is required.

| Setting | Required value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Preserves the repository's Node-backed orchestration memory setting when Maven is launched through scripts or wrappers. |
| `tweedle-lang` submodule | Initialized with `git submodule update --init tweedle-lang` | Required before Maven reactor validation. |
| Maven flags | `-DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false` | Keeps validation bounded to the focused no-Sims `core/ide` characterization surface. |

Do not configure QA workflow timeout fields for this proof. The proof has no
timeout configuration.

## Negative checks

The characterization includes focused failure cases. A failure case must not
write a success artifact.

| Negative check | Required result |
| --- | --- |
| Missing `scene.eatmeFirstLesson` target | Fails before mutation and reports the missing target. |
| Wrong procedure target | Fails when selection or action evidence names a method other than `scene.eatmeFirstLesson`. |
| Unsupported action contract | Rejects any edit spec that is not `append-comment:<marker>`. |
| Blank marker | Rejects `append-comment:` and whitespace-only markers. |
| Mismatched marker observation | Fails if the requested marker is not found exactly once in the target after the action. |
| Target isolation failure | Fails if the marker appears in any non-target procedure. |

These checks protect the proof from name-only matches, accidental method
creation, broad mutation, and success-shaped evidence.

## Blocked fallback

The implemented deterministic edit/action proof does not emit the blocked
artifact. If this path regresses and becomes unavailable, the fallback contract is
exactly one blocked artifact instead of the success artifact:

```text
first-lesson-code-editor-action-blocked.json
```

Required schema:

```json
{
  "schema_version": "eatme.first-lesson-code-editor-action-blocked/v1",
  "status": "blocked",
  "target": "scene.eatmeFirstLesson",
  "action": "append-comment:wave4-code-editor-action-proof",
  "exact_missing_contract": "no stable public UI/backend action or invoker exists from `CodeEditor`/`CodeComposite` to select `scene.eatmeFirstLesson`, apply an edit through the desktop code editor, and observe a deterministic invocation result",
  "doesNotClaim": [
    "full first-lesson completion",
    "grading",
    "creative assessment",
    "visible rendering correctness",
    "broad UI automation"
  ]
}
```

Do not emit multiple speculative blocker artifacts. Do not replace this exact
contract with broader wording. A blocked run writes no
`first-lesson-code-editor-action-proof.json` success schema.

## Evidence boundaries

This proof may claim only:

- The declarations editor selected the exact `scene.eatmeFirstLesson`
  `UserMethod`.
- The selected `CodeComposite` and selected `CodeEditor.getCode()` backing model
  matched that target.
- The deterministic `append-comment:wave4-code-editor-action-proof` edit was
  applied to that target.
- The marker was observed only in `scene.eatmeFirstLesson`.
- Focused negative cases fail closed without success artifacts.

This proof must not claim:

- Full first-lesson completion.
- Learner grading, scoring, or creative assessment.
- Visible rendering correctness.
- Broad desktop UI automation.
- Save, Save As, launcher, Select Project, or project persistence beyond the
  temporary test fixture.

## Examples

### Focused command

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.FirstLessonCodeEditorActionProofTest#provesFirstLessonCodeEditorActionAndWritesStructuredEvidence \
  test
```

### Action selector

```text
--procedure-selector scene.eatmeFirstLesson
--edit-spec append-comment:wave4-code-editor-action-proof
```

### Minimal assertion pattern

```java
UserMethod selected = ProcedureTabSelection.selectProcedureInEditor(
    editor,
    target,
    null);

assertSame(target, selected);
assertSame(target,
    ProcedureTabSelection.getSelectedProcedureCodeComposite(editor).getDeclaration());
assertSame(target, ProcedureTabSelection.getSelectedCodeEditorCode(editor));

assertEquals(0, EatmeEditProcedure.run(args, out, err));
assertTrue(Files.readString(evidenceDir.resolve(
    "first-lesson-code-editor-action-proof.json")).contains(
    "\"target_marker_count\": 1"));
assertTrue(Files.readString(evidenceDir.resolve(
    "first-lesson-code-editor-action-proof.json")).contains(
    "\"wrong_target_marker_count\": 0"));
```

The example names the implemented seam. It is not a visible desktop interaction
or a full lesson-completion assertion.
