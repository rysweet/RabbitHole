# Run-Window Creation/Wiring Contract

This reference defines the bounded Run-window evidence contract for Alice desktop
QA. The feature records that the Run window was created and wired through the
expected product seam. It does not claim active rendering, run execution, world execution correctness, rendering correctness, Save behavior, grading, creative assessment, lesson completion, or full UI automation.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Artifact API](#artifact-api)
- [Java seam API](#java-seam-api)
- [Configuration](#configuration)
- [Path safety](#path-safety)
- [Scenario contract](#scenario-contract)
- [Examples](#examples)
- [Evidence boundaries](#evidence-boundaries)

## Scope

The contract covers exactly this silver-thread seam:

```text
RunComposite#handlePreShowWindow
  -> EatmeRunWindowEvidence.recordRunWindowCreated(...)
  -> run-window-created.json
```

The evidence is metadata that the Run window creation hook reached the evidence
writer with the frame title and active program type available. The artifact is a
creation/wiring record only.

## Usage

Validate the complete QA lane contract from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
bash qa/outside-in/alice-desktop/tests/test-run-window-contract.sh
```

This script checks the scenario contract, JSON Schema allowlist, runner
allowlist, production hook-to-recorder call, prepare-only evidence path, and
enabled gated artifact persistence/validation for the Run-window lane. It is
the shortest review command for contract readiness; it is not a broad desktop automation run.

Run the focused contract characterization from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.EatmeRunWindowEvidenceTest \
  test
```

Prepare the outside-in QA scenario without executing the gated command:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-window-contract
```

Execute the gated scenario only when the reviewer explicitly wants the focused
Maven seam command to run through the QA runner:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-window-contract
```

The scenario command is allowlisted as an argv array. Do not replace it with a
shell command string, broad Maven selector, UI automation runner, or timeout
wrapper.

## Artifact API

The success artifact is always:

```text
run-window-created.json
```

It is written only inside the configured evidence directory. Alternate names,
nested paths, parent traversal, and absolute paths are invalid.

Required fields:

| Field | Type | Required value or meaning |
| --- | --- | --- |
| `schema_version` | string | `eatme.alice-run-window-created/v1`. |
| `status` | string | `created`. |
| `contract_scope` | string | `run-window-creation-wiring`. |
| `evidence_source` | string | `org.alice.stageide.run.RunComposite#handlePreShowWindow`. |
| `artifact` | string | `run-window-created.json`. |
| `frame_title` | string | JSON-escaped Run frame title, or an empty string when unavailable. |
| `program_type` | string | JSON-escaped active program type name, or an empty string when unavailable. |
| `active_rendering_claimed` | boolean | Always `false`. |
| `run_program_claimed` | boolean | Always `false`. |
| `run_execution_claimed` | boolean | Always `false`. |
| `world_execution_claimed` | boolean | Always `false`. |
| `rendering_correctness_claimed` | boolean | Always `false`. |
| `save_claimed` | boolean | Always `false`. |
| `grading_claimed` | boolean | Always `false`. |
| `full_ui_automation_claimed` | boolean | Always `false`. |
| `does_not_claim` | string array | Includes `active-rendering`, `run-execution`, `world-execution-correctness`, `rendering-correctness`, `save`, `grading`, and `full-ui-automation`. |

The v1 artifact does not define separate creative-assessment or
lesson-completion booleans. Those claims are still outside this contract: the
scope is only Run-window creation/wiring, and reviewers must not use the
artifact as creative-assessment or lesson-completion evidence.

Representative artifact:

```json
{
  "schema_version": "eatme.alice-run-window-created/v1",
  "status": "created",
  "contract_scope": "run-window-creation-wiring",
  "evidence_source": "org.alice.stageide.run.RunComposite#handlePreShowWindow",
  "artifact": "run-window-created.json",
  "frame_title": "Run Alice",
  "program_type": "Scene",
  "active_rendering_claimed": false,
  "run_program_claimed": false,
  "run_execution_claimed": false,
  "world_execution_claimed": false,
  "rendering_correctness_claimed": false,
  "save_claimed": false,
  "grading_claimed": false,
  "full_ui_automation_claimed": false,
  "does_not_claim": [
    "active-rendering",
    "run-execution",
    "world-execution-correctness",
    "rendering-correctness",
    "save",
    "grading",
    "full-ui-automation"
  ]
}
```

## Java seam API

`EatmeRunWindowEvidence` is a product-adjacent evidence utility used by the
desktop Run-window seam and focused characterization tests. It is not a public
Alice authoring API.

`RunComposite#handlePreShowWindow` is the production hook that calls
`EatmeRunWindowEvidence.recordRunWindowCreated(frame, programType)`. The shell
contract keeps that hook wired while the Java tests characterize the recorder
and artifact behavior.

| API | Behavior |
| --- | --- |
| `EVIDENCE_DIR_PROPERTY` | JVM property name: `org.alice.eatme.runWindowEvidenceDir`. |
| `RUN_WINDOW_CREATED_ARTIFACT` | Fixed artifact name: `run-window-created.json`. |
| `recordRunWindowCreated(Frame frame, NamedUserType programType)` | Reads the JVM property, records the artifact when configured, and logs evidence write failures without aborting Run-window creation. |

The test seam also characterizes package-local artifact writing, JSON escaping,
and path validation. Those helpers support the contract; they are not a separate
external integration surface.

## Configuration

Enable product-side evidence by setting:

```text
org.alice.eatme.runWindowEvidenceDir=<existing evidence directory>
```

The directory must already exist. The seam does not create missing directories
and does not turn a failed write into a success-shaped artifact.

`NODE_OPTIONS=--max-old-space-size=32768` is used when repository QA commands are
invoked through Node-aware orchestration. It changes process memory only; it does
not broaden the Run-window evidence claim.

## Path safety

The artifact path is fail-closed:

| Unsafe input | Required behavior |
| --- | --- |
| `../run-window-created.json` | Reject parent traversal. |
| `nested/run-window-created.json` | Reject nested artifact paths. |
| `/tmp/run-window-created.json` | Reject absolute paths. |
| Empty artifact name | Reject missing artifact names. |
| Pre-existing artifact symlink | Do not follow the symlink; leave the target unchanged. |
| Symlink evidence directory | Reject the directory before writing any artifact. |
| Missing evidence directory | Fail without creating the directory. |

Frame titles and program type names are metadata only. They must be JSON-escaped
before writing so quotes, backslashes, tabs, newlines, carriage returns, and
control characters cannot corrupt the artifact shape.

## Scenario contract

The outside-in scenario is:

```text
alice-desktop-run-window-contract
```

It uses workflow:

```text
run-window-contract
```

Its automation mode is:

```text
gated-command-smoke
```

The only allowed gated command is:

```text
mvn -DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl core/ide -am -Dtest=org.alice.tools.EatmeRunWindowEvidenceTest test
```

Prepare-only mode writes scenario status and checklist evidence with
`outcome=gated-not-run`. That result verifies scenario wiring and review
instructions only; it does not show artifact contents.

## Examples

Reviewing a generated artifact:

```bash
python3 -m json.tool \
  <existing-evidence-dir>/run-window-created.json
```

Expected non-claim checks:

```text
active_rendering_claimed=false
run_program_claimed=false
run_execution_claimed=false
world_execution_claimed=false
rendering_correctness_claimed=false
save_claimed=false
grading_claimed=false
full_ui_automation_claimed=false
```

If any of those booleans is `true`, the artifact does not satisfy this contract.
The same review boundary excludes creative assessment and lesson completion even
though those exclusions are expressed by scope rather than separate v1 boolean
fields.

## Evidence boundaries

Use this evidence only for Run-window creation/wiring. Adjacent claims stay in
their own lanes:

| Claim | Use instead |
| --- | --- |
| Runtime/display accessibility and target-scoped raw pixel sampling | [Post-open runtime/display accessibility evidence](./post-open-runtime-display-accessibility-evidence.md). |
| Save menu/dialog/write behavior | [Save Menu Dialog Write Proof](./save-menu-dialog-write-proof.md). |
| First-lesson procedure/code-editor action seam | [First-Lesson Code-Editor Action Proof](./first-lesson-code-editor-action-proof.md). |
| Learner-world setup/open/save boundary | [Learner-world assessment boundary](./learner-world-assessment-boundary.md). |
