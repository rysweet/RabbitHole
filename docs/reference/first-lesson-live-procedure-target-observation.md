# First-Lesson Live Procedure Target Action Seam

This reference defines the outside-in QA shard that opens the configured
first-lesson flow starter through Select Project, observes the live
`scene.eatmeFirstLesson` procedure/code-editor target, and records whether that
target is ready for a public desktop edit action.

## Contents

- [Scope](#scope)
- [Runner interface](#runner-interface)
- [Scenario contract](#scenario-contract)
- [Artifact API](#artifact-api)
- [Configuration](#configuration)
- [Examples](#examples)
- [Security and safety rules](#security-and-safety-rules)
- [Claim boundaries](#claim-boundaries)
- [Relationship to adjacent shards](#relationship-to-adjacent-shards)

## Scope

The shard covers exactly one transition in the first-lesson flow:

```text
Select Project opens the first-lesson project
  -> live Alice desktop is post-open
  -> procedure tab or code-editor target for scene.eatmeFirstLesson is observed
  -> target is classified as desktop-edit ready or blocked by a named contract gap
```

It exists because earlier shards already cover adjacent boundaries:

| Boundary | Existing evidence |
| --- | --- |
| Select Project opens a committed starter | `alice-desktop-select-project-tab-click-exec` and `tab-click-observation.json`. |
| AST/project-level procedure edit | `alice-desktop-procedure-edit-seam-smoke` and the procedure edit evidence artifacts. |
| Object placement into procedure edit handoff | `alice-desktop-procedure-edit-handoff-smoke`. |
| Save menu/dialog/write path | `alice-desktop-save-menu-dialog-write-proof`. |
| Runtime/display accessibility and rendering blocker | `alice-desktop-post-open-runtime-display-accessibility-evidence`. |
| Learner assessment boundary | `qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json`. |

The shard does not mutate the project. It does not click into an edit control,
invoke a private implementation method, use reflection, synthesize a desktop edit,
Save, Run, assert rendering correctness, grade learner work, or assess creative
quality. A passing run means only that the live target/action seam is
machine-readable: either the observed target is ready for a supported public
desktop edit invocation, or the artifact names the exact contract that prevents
the next proof.

## Runner interface

Validate the scenario catalog:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Run the live procedure target action seam shard:

```bash
rm -rf /tmp/alice-first-lesson-live-procedure-target
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir /tmp/alice-first-lesson-live-procedure-target \
  --timeout-seconds 300
```

Review the newest timestamped run directory under:

```text
/tmp/alice-first-lesson-live-procedure-target/alice-desktop-first-lesson-live-procedure-target-observation/
```

The decision artifact is:

```text
first-lesson-live-procedure-target-observation.json
```

Use the branch-installable wrapper the same way when reviewing a pull request
branch:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> \
  amplihack alice-qa run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir /tmp/alice-first-lesson-live-procedure-target \
  --timeout-seconds 300
```

Replace `<branch>` with the branch or commit under review.

## Scenario contract

The scenario file is:

```text
qa/outside-in/alice-desktop/scenarios/first-lesson-live-procedure-target-observation.yaml
```

Scenario identity:

| Field | Value |
| --- | --- |
| `id` | `alice-desktop-first-lesson-live-procedure-target-observation` |
| `workflow` | `first-lesson-live-procedure-target-observation` |
| `automationMode` | `xvfb-real-alice` |
| `targetStarter.displayName` | `Africa Full`, the configured starter selected through the Select Project shard. |
| `targetStarter.repositoryPath` | `core/resources/src/application/resources/starter-projects/AfricaFull.a3p`. |

Required evidence:

| Artifact | Purpose |
| --- | --- |
| `first-lesson-live-procedure-target-observation.json` | Machine-readable edit-ready or blocked decision for the first-lesson procedure/code-editor action seam. |
| `status.txt` | Final runner status, including the action-seam status and outcome. |
| `tab-click-observation.json` | Supporting Select Project evidence that the target starter was selected and opened. |
| `post-project-open-observation.json` | Supporting post-open main-window evidence. |
| `x-window-inventory.json` | Bounded Alice-related X window inventory. |
| `launch.log` and `xvfb.log` | Desktop launch and display logs. |

The runner preserves the probe artifact fields exactly. It does not add
synthetic UI clicks, fake edit actions, reflection calls, Save calls, render
assertions, grading rules, or lesson-completion claims. Unknown workflows and
unknown argv values are rejected before launch. The decision artifact is written
to the runner-created evidence directory, and the producer refuses to overwrite
symlink artifact targets.

## Artifact API

The `first-lesson-live-procedure-target-observation.json` artifact uses schema:

```text
eatme.first-lesson-live-procedure-target-observation/v1
```

Required top-level fields:

| Field | Type | Meaning |
| --- | --- | --- |
| `schemaVersion` | string | Always `eatme.first-lesson-live-procedure-target-observation/v1`. |
| `scenario` | string | Always `alice-desktop-first-lesson-live-procedure-target-observation`. |
| `workflow` | string | Always `first-lesson-live-procedure-target-observation`. |
| `automationMode` | string | Always `xvfb-real-alice`. |
| `status` | enum | `edit-ready` when the observed target has a public desktop edit invocation contract; `blocked` when the seam cannot proceed. |
| `seam` | string | Always `live-first-lesson-procedure-target-to-desktop-edit-action`. |
| `project` | object | First-lesson starter metadata and Select Project open status. |
| `requiredTarget` | object | The procedure/code-editor target the next shard needs. |
| `observedTarget` | object or null | Bounded target metadata when the target is observed; `null` when target observation itself is blocked. |
| `desktopEditAction` | object | Public edit-action readiness evidence for the observed target, or a precise no-go blocker. |
| `blocker` | object | Top-level machine-readable blocker. `kind=none` only when `status=edit-ready`. |
| `downstreamBlockedStep` | string | `none` when edit-ready; otherwise `desktop-procedure-edit-action-proof`. |
| `outOfScope` | string array | Explicit non-claims for edit mutation, Save, rendering correctness, learner assessment, creative assessment, and full lesson completion. |

`project` fields:

| Field | Type | Meaning |
| --- | --- | --- |
| `targetStarterDisplayName` | string | Starter display name from the scenario. |
| `targetStarterRepositoryPath` | string | Repository-relative target starter path from the scenario. |
| `openedViaSelectProject` | boolean | `true` only when the supporting Select Project evidence opened the target starter. |
| `postOpenWindowObserved` | boolean | `true` only when the supporting post-open probe observed the Alice main window after project open. |

`requiredTarget` fields:

| Field | Type | Meaning |
| --- | --- | --- |
| `procedureName` | string | Always `scene.eatmeFirstLesson`. |
| `kind` | string | Always `procedure-or-code-editor-target`. |
| `minimumStableAutomationTarget` | string | The least specific stable target accepted for the next desktop edit shard. |

`observedTarget` fields when a target is observed:

| Field | Type | Meaning |
| --- | --- | --- |
| `procedureName` | string | Always `scene.eatmeFirstLesson`. |
| `kind` | enum | `procedure-tab`, `code-editor`, or `procedure-code-editor-composite`. |
| `accessibleName` | string or null | Bounded accessibility name, when available. |
| `accessibleRole` | string or null | Bounded accessibility role, when available. |
| `automationPath` | string | Stable runner-facing path or selector for reacquiring the target. |
| `readyForDesktopEditAction` | boolean | `true` only when a public desktop edit-action invocation contract is available for this target. |

`desktopEditAction` fields:

| Field | Type | Meaning |
| --- | --- | --- |
| `status` | enum | `ready` or `blocked`. |
| `readyForDesktopEditAction` | boolean | Mirrors `observedTarget.readyForDesktopEditAction` when a target is observed; `false` when target observation failed. |
| `targetSelector` | string | Always `scene.eatmeFirstLesson`. |
| `invocationContract` | string or null | Public desktop edit invocation contract name when ready; `null` when blocked. |
| `blocker.kind` | string | `none` when ready; otherwise a stable blocker code. |
| `blocker.message` | string | Empty when ready; otherwise the exact human-readable blocker. |
| `doesNotClaim` | string array | Explicit non-claims for edit mutation, Save, rendering correctness, learner assessment, creative assessment, and full lesson completion. |

Accepted `blocker.kind` values:

| Kind | Required `blocker.message` | Meaning |
| --- | --- | --- |
| `none` | Empty string | The target was observed and is ready for a public desktop edit action. |
| `missing-desktop-edit-action-contract` | `missing public CodeEditor/CodeComposite edit invocation contract` | The target was observed, but no callable public desktop edit invocation contract exists for the next proof. |
| `select-project-open-not-observed` | `Select Project did not open the configured first-lesson starter` | Supporting Select Project evidence did not open the first-lesson starter. |
| `post-open-window-not-observed` | `post-open Alice main window was not observed` | The main window was not observed after project open. |
| `procedure-target-not-found` | `scene.eatmeFirstLesson procedure/code-editor target was not found` | The live desktop did not expose the procedure tab or code-editor target. |
| `procedure-target-not-stable` | `scene.eatmeFirstLesson target was not reacquirable through a stable automation path` | A candidate existed but could not be reacquired by a stable automation path. |
| `at-spi-or-atk-unavailable` | `AT-SPI/ATK accessibility infrastructure was unavailable` | Accessibility infrastructure was unavailable. |
| `display-prerequisite-unavailable` | `Xvfb display prerequisite was unavailable` | Xvfb, display allocation, or screen capture prerequisites failed before target observation. |

For the next first-lesson action slice, the focused artifact test accepts only
two action-seam outcomes:

1. `status=edit-ready`, `observedTarget.readyForDesktopEditAction=true`,
   `desktopEditAction.status=ready`, and `blocker.kind=none`.
2. `status=blocked`, `observedTarget.procedureName=scene.eatmeFirstLesson`,
   `observedTarget.readyForDesktopEditAction=false`,
   `blocker.kind=missing-desktop-edit-action-contract`, and
   `blocker.message=missing public CodeEditor/CodeComposite edit invocation contract`.

Target-only observation with no edit-readiness classification is not a passing
artifact for this shard.

## Configuration

| Setting | Required value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Saved preference for Node-backed QA orchestration. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS` | `1` | Allows the runner to prepare isolated first-run license acceptance state for controlled QA launches. |
| `--evidence-dir` | Writable directory outside committed source | Parent directory for timestamped evidence runs. |
| `--timeout-seconds` | Positive integer, commonly `300` | Upper bound for live desktop launch and observation. |
| `ALICE_QA_SCENARIO_DIR` | Optional catalog override | Used only when validating or running a custom scenario catalog. |

No product preference or Alice project behavior change is required. The shard is
read-only after Select Project opens the first-lesson starter.

## Examples

Edit-ready decision artifact:

```json
{
  "schemaVersion": "eatme.first-lesson-live-procedure-target-observation/v1",
  "scenario": "alice-desktop-first-lesson-live-procedure-target-observation",
  "workflow": "first-lesson-live-procedure-target-observation",
  "automationMode": "xvfb-real-alice",
  "status": "edit-ready",
  "seam": "live-first-lesson-procedure-target-to-desktop-edit-action",
  "project": {
    "targetStarterDisplayName": "Africa Full",
    "targetStarterRepositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
    "openedViaSelectProject": true,
    "postOpenWindowObserved": true
  },
  "requiredTarget": {
    "procedureName": "scene.eatmeFirstLesson",
    "kind": "procedure-or-code-editor-target",
    "minimumStableAutomationTarget": "reacquirable live desktop procedure tab or code-editor target"
  },
  "observedTarget": {
    "procedureName": "scene.eatmeFirstLesson",
    "kind": "code-editor",
    "accessibleName": "eatmeFirstLesson",
    "accessibleRole": "panel",
    "automationPath": "at-spi:/Alice/DeclarationsEditor/eatmeFirstLesson",
    "readyForDesktopEditAction": true
  },
  "desktopEditAction": {
    "status": "ready",
    "readyForDesktopEditAction": true,
    "targetSelector": "scene.eatmeFirstLesson",
    "invocationContract": "public CodeEditor/CodeComposite edit invocation contract",
    "blocker": {
      "kind": "none",
      "message": ""
    },
    "doesNotClaim": [
      "desktop procedure edit mutation",
      "Save",
      "rendering correctness",
      "learner assessment",
      "creative assessment",
      "full first-lesson completion"
    ]
  },
  "blocker": {
    "kind": "none",
    "message": ""
  },
  "downstreamBlockedStep": "none",
  "outOfScope": [
    "desktop procedure edit mutation",
    "Save",
    "rendering correctness",
    "learner assessment",
    "creative assessment",
    "full first-lesson completion"
  ]
}
```

Named no-go decision artifact when the target is observed but the public edit
invocation contract is missing:

```json
{
  "schemaVersion": "eatme.first-lesson-live-procedure-target-observation/v1",
  "scenario": "alice-desktop-first-lesson-live-procedure-target-observation",
  "workflow": "first-lesson-live-procedure-target-observation",
  "automationMode": "xvfb-real-alice",
  "status": "blocked",
  "seam": "live-first-lesson-procedure-target-to-desktop-edit-action",
  "project": {
    "targetStarterDisplayName": "Africa Full",
    "targetStarterRepositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
    "openedViaSelectProject": true,
    "postOpenWindowObserved": true
  },
  "requiredTarget": {
    "procedureName": "scene.eatmeFirstLesson",
    "kind": "procedure-or-code-editor-target",
    "minimumStableAutomationTarget": "reacquirable live desktop procedure tab or code-editor target"
  },
  "observedTarget": {
    "procedureName": "scene.eatmeFirstLesson",
    "kind": "code-editor",
    "accessibleName": "eatmeFirstLesson",
    "accessibleRole": "panel",
    "automationPath": "at-spi:/Alice/DeclarationsEditor/eatmeFirstLesson",
    "readyForDesktopEditAction": false
  },
  "desktopEditAction": {
    "status": "blocked",
    "readyForDesktopEditAction": false,
    "targetSelector": "scene.eatmeFirstLesson",
    "invocationContract": null,
    "blocker": {
      "kind": "missing-desktop-edit-action-contract",
      "message": "missing public CodeEditor/CodeComposite edit invocation contract"
    },
    "doesNotClaim": [
      "desktop procedure edit mutation",
      "Save",
      "rendering correctness",
      "learner assessment",
      "creative assessment",
      "full first-lesson completion"
    ]
  },
  "blocker": {
    "kind": "missing-desktop-edit-action-contract",
    "message": "missing public CodeEditor/CodeComposite edit invocation contract"
  },
  "downstreamBlockedStep": "desktop-procedure-edit-action-proof",
  "outOfScope": [
    "desktop procedure edit mutation",
    "Save",
    "rendering correctness",
    "learner assessment",
    "creative assessment",
    "full first-lesson completion"
  ]
}
```

## Security and safety rules

- Store scenario automation as argv lists, not shell command strings.
- Keep the workflow and argv allowlists narrow; do not allow arbitrary commands.
- Write artifacts only under the runner-created evidence directory.
- Reject absolute paths, `..`, realpath escapes, and unsafe symlink overwrites.
- Do not write credentials, environment dumps, usernames, screenshots of source
  contents, saved project contents, broad accessibility trees, or unrelated
  desktop windows into the JSON artifact.
- Keep the shard read-only: no AST mutation, no desktop edit action, no Save,
  no Run, no rendering oracle, no learner assessment, no creative assessment,
  and no lesson-completion assertion.
- Fail loudly for malformed scenarios, unknown workflows, missing supporting
  artifacts, invalid JSON, missing required artifact fields, vague blockers, or
  target-only observation without edit-readiness evidence.

## Claim boundaries

This shard may claim only:

- Select Project opened the configured first-lesson starter when supporting
  `tab-click-observation.json` says so.
- The post-open Alice main window was observed when supporting
  `post-project-open-observation.json` says so.
- The live desktop target for `scene.eatmeFirstLesson` was observed when
  `observedTarget.procedureName=scene.eatmeFirstLesson` and `observedTarget.kind`
  names a procedure/code-editor target.
- The observed target is ready for desktop edit action only when
  `observedTarget.readyForDesktopEditAction=true`,
  `desktopEditAction.status=ready`, and `blocker.kind=none`.
- The next action proof is blocked by the missing edit contract only when
  `blocker.kind=missing-desktop-edit-action-contract` and
  `blocker.message=missing public CodeEditor/CodeComposite edit invocation contract`.

This shard must not claim:

- A desktop procedure edit was performed.
- AST/project-level procedure editing was performed by the live desktop.
- Save, Save As, backup, or project write behavior.
- Visible rendering correctness or world-canvas pixel correctness.
- Learner grading, rubric scoring, correctness assessment, or creativity
  assessment.
- Full first-lesson completion.

## Relationship to adjacent shards

Use this shard after the Select Project target starter evidence and before any
desktop procedure edit attempt. It is the live-desktop target/action link between
these references:

- [Select Project Africa Full AT-SPI evidence](./select-project-africa-full-atspi-evidence.md)
- [First-Lesson Procedure/Edit Seam](./first-lesson-procedure-edit-seam.md)
- [Desktop Procedure Edit and Save Automation](./desktop-procedure-edit-and-save-automation.md)
- [Save Menu Dialog Write Proof](./save-menu-dialog-write-proof.md)
- [Post-open runtime/display accessibility evidence](./post-open-runtime-display-accessibility-evidence.md)

When the artifact is edit-ready, the next safe shard is a read/write desktop
procedure edit action proof that reacquires the same target and performs the
smallest supported edit through the named public contract. When the artifact is
blocked by `missing-desktop-edit-action-contract`, the next implementation step
is to expose or document the public `CodeEditor`/`CodeComposite` edit invocation
contract before attempting a real desktop edit.
