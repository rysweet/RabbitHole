# [PLANNED - Implementation Pending] First-Lesson Live Procedure Target Observation

This reference defines the intended outside-in QA shard that will open the
first-lesson starter through Select Project and observe whether the live desktop
exposes a stable procedure tab or code-editor target for
`scene.eatmeFirstLesson`.

The scenario, schema entries, runner workflow, and contract tests are not
implemented yet. Do not treat the commands and artifact examples below as
runnable until the implementation lands and this document removes the
`[PLANNED - Implementation Pending]` marker.

## Contents

- [Scope](#scope)
- [Implementation status](#implementation-status)
- [Planned runner interface](#planned-runner-interface)
- [Scenario contract](#scenario-contract)
- [Artifact API](#artifact-api)
- [Configuration](#configuration)
- [Examples](#examples)
- [Security and safety rules](#security-and-safety-rules)
- [Claim boundaries](#claim-boundaries)
- [Relationship to adjacent shards](#relationship-to-adjacent-shards)

## Scope

The planned shard covers exactly one transition in the first-lesson flow:

```text
Select Project opens the first-lesson project
  -> live Alice desktop is post-open
  -> procedure tab or code-editor target for scene.eatmeFirstLesson is observed
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

The planned shard will not mutate the project. It will not invoke a procedure
edit, Save, Run, rendering assertion, grading rule, or learner assessment. A
passing run will mean only that the live desktop exposed the target that the
next desktop edit shard can safely act on.

## Implementation status

This is a document-driven contract for the feature to build. The current
repository does not contain:

- `qa/outside-in/alice-desktop/scenarios/first-lesson-live-procedure-target-observation.yaml`
- a schema/validator workflow value for `first-lesson-live-procedure-target-observation`
- runner support for `alice-desktop-first-lesson-live-procedure-target-observation`
- contract tests for the new workflow, argv allowlist, or evidence artifact

Until those pieces exist, active QA docs and supported workflow lists must not
claim this shard is runnable.

## Planned runner interface

[PLANNED] Validate the scenario catalog after the scenario/schema/runner changes
exist:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

[PLANNED] Run the live procedure target observation shard:

```bash
rm -rf /tmp/alice-first-lesson-live-procedure-target
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir /tmp/alice-first-lesson-live-procedure-target \
  --timeout-seconds 300
```

[PLANNED] Review the newest timestamped run directory under:

```text
/tmp/alice-first-lesson-live-procedure-target/alice-desktop-first-lesson-live-procedure-target-observation/
```

The planned decision artifact is:

```text
first-lesson-live-procedure-target-observation.json
```

[PLANNED] Use the branch-installable wrapper the same way when reviewing a pull
request branch:

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

When implemented, the scenario file will be:

```text
qa/outside-in/alice-desktop/scenarios/first-lesson-live-procedure-target-observation.yaml
```

Planned scenario identity:

| Field | Value |
| --- | --- |
| `id` | `alice-desktop-first-lesson-live-procedure-target-observation` |
| `workflow` | `first-lesson-live-procedure-target-observation` |
| `automationMode` | `xvfb-real-alice` |
| `targetStarter.displayName` | Display name of the first-lesson starter selected through Select Project. |
| `targetStarter.repositoryPath` | Repository-relative first-lesson starter `.a3p` path. |

Planned required evidence:

| Artifact | Purpose |
| --- | --- |
| `first-lesson-live-procedure-target-observation.json` | Machine-readable observed-or-blocked decision for the procedure/code-editor target. |
| `status.txt` | Final runner status, including the procedure-target observation status and outcome. |
| `tab-click-observation.json` | Supporting Select Project evidence that the target starter was selected and opened. |
| `post-project-open-observation.json` | Supporting post-open main-window evidence. |
| `x-window-inventory.json` | Bounded Alice-related X window inventory. |
| `launch.log` and `xvfb.log` | Desktop launch and display logs. |

The implementation must reject unknown workflows, unknown argv values, absolute
output artifact paths, path traversal, and symlink targets that would write
outside the run evidence directory.

## Artifact API

The planned `first-lesson-live-procedure-target-observation.json` artifact uses
schema:

```text
eatme.first-lesson-live-procedure-target-observation/v1
```

Planned required top-level fields:

| Field | Type | Meaning |
| --- | --- | --- |
| `schemaVersion` | string | Always `eatme.first-lesson-live-procedure-target-observation/v1`. |
| `scenario` | string | Always `alice-desktop-first-lesson-live-procedure-target-observation`. |
| `workflow` | string | Always `first-lesson-live-procedure-target-observation`. |
| `automationMode` | string | Always `xvfb-real-alice`. |
| `status` | enum | `observed` when a stable target is found; `blocked` when the exact missing target is recorded. |
| `seam` | string | Always `live-first-lesson-project-open-to-procedure-target-observable`. |
| `project` | object | First-lesson starter metadata and Select Project open status. |
| `requiredTarget` | object | The procedure/code-editor target the next shard needs. |
| `observedTarget` | object or null | Bounded target metadata when `status=observed`; `null` when blocked. |
| `blocker` | string | `none` when observed; otherwise a stable blocker code. |
| `blockerDetail` | string | Human-readable blocker detail. |
| `downstreamBlockedStep` | string | Always `desktop-procedure-edit`. |
| `outOfScope` | string array | Explicit non-claims for edit, Save, rendering correctness, learner assessment, and full lesson completion. |

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
| `procedureSelector` | string | Always `scene.eatmeFirstLesson`. |
| `targetKind` | string | `procedure-tab-or-code-editor`. |
| `minimumStableAutomationTarget` | string | The least specific stable target accepted for the next desktop edit shard. |

`observedTarget` fields when `status=observed`:

| Field | Type | Meaning |
| --- | --- | --- |
| `targetKind` | string | Observed target category, such as `procedure-tab` or `code-editor`. |
| `procedureSelector` | string | The procedure selector associated with the observed target. |
| `accessibleName` | string or null | Bounded accessibility name, when available. |
| `accessibleRole` | string or null | Bounded accessibility role, when available. |
| `automationPath` | string | Stable runner-facing path or selector for reacquiring the target. |
| `readyForDesktopEditAction` | boolean | `true` only when the target can be reacquired without brittle coordinate or screenshot matching. |

Accepted blocker codes:

| Code | Meaning |
| --- | --- |
| `none` | The target was observed. |
| `select-project-open-not-observed` | Supporting Select Project evidence did not open the first-lesson starter. |
| `post-open-window-not-observed` | The main window was not observed after project open. |
| `procedure-target-not-found` | The live desktop did not expose the procedure tab or code-editor target. |
| `procedure-target-not-stable` | A candidate existed but could not be reacquired by a stable automation path. |
| `at-spi-or-atk-unavailable` | Accessibility infrastructure was unavailable. |
| `display-prerequisite-unavailable` | Xvfb, display allocation, or screen capture prerequisites failed before target observation. |

## Configuration

| Setting | Required value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Saved preference for Node-backed QA orchestration. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS` | `1` | Allows the runner to prepare isolated first-run license acceptance state for controlled QA launches. |
| `--evidence-dir` | Writable directory outside committed source | Parent directory for timestamped evidence runs. |
| `--timeout-seconds` | Positive integer, commonly `300` | Upper bound for live desktop launch and observation. |
| `ALICE_QA_SCENARIO_DIR` | Optional catalog override | Used only when validating or running a custom scenario catalog. |

No product preference or Alice project behavior change should be required. The
shard must be read-only after Select Project opens the first-lesson starter.

## Examples

Planned observed decision artifact:

```json
{
  "schemaVersion": "eatme.first-lesson-live-procedure-target-observation/v1",
  "scenario": "alice-desktop-first-lesson-live-procedure-target-observation",
  "workflow": "first-lesson-live-procedure-target-observation",
  "automationMode": "xvfb-real-alice",
  "status": "observed",
  "seam": "live-first-lesson-project-open-to-procedure-target-observable",
  "project": {
    "targetStarterDisplayName": "TBD first-lesson starter display name",
    "targetStarterRepositoryPath": "TBD repository-relative first-lesson starter .a3p path",
    "openedViaSelectProject": true,
    "postOpenWindowObserved": true
  },
  "requiredTarget": {
    "procedureSelector": "scene.eatmeFirstLesson",
    "targetKind": "procedure-tab-or-code-editor",
    "minimumStableAutomationTarget": "reacquirable live desktop procedure tab or code-editor target"
  },
  "observedTarget": {
    "targetKind": "code-editor",
    "procedureSelector": "scene.eatmeFirstLesson",
    "accessibleName": "eatmeFirstLesson",
    "accessibleRole": "panel",
    "automationPath": "at-spi:/Alice/DeclarationsEditor/eatmeFirstLesson",
    "readyForDesktopEditAction": true
  },
  "blocker": "none",
  "blockerDetail": "",
  "downstreamBlockedStep": "desktop-procedure-edit",
  "outOfScope": [
    "desktop procedure edit mutation",
    "Save",
    "rendering correctness",
    "learner assessment",
    "full first-lesson completion"
  ]
}
```

Planned blocked decision artifact:

```json
{
  "schemaVersion": "eatme.first-lesson-live-procedure-target-observation/v1",
  "scenario": "alice-desktop-first-lesson-live-procedure-target-observation",
  "workflow": "first-lesson-live-procedure-target-observation",
  "automationMode": "xvfb-real-alice",
  "status": "blocked",
  "seam": "live-first-lesson-project-open-to-procedure-target-observable",
  "project": {
    "targetStarterDisplayName": "TBD first-lesson starter display name",
    "targetStarterRepositoryPath": "TBD repository-relative first-lesson starter .a3p path",
    "openedViaSelectProject": true,
    "postOpenWindowObserved": true
  },
  "requiredTarget": {
    "procedureSelector": "scene.eatmeFirstLesson",
    "targetKind": "procedure-tab-or-code-editor",
    "minimumStableAutomationTarget": "reacquirable live desktop procedure tab or code-editor target"
  },
  "observedTarget": null,
  "blocker": "procedure-target-not-found",
  "blockerDetail": "The first-lesson project opened, but the live desktop did not expose a stable procedure tab or code-editor target for scene.eatmeFirstLesson.",
  "downstreamBlockedStep": "desktop-procedure-edit",
  "outOfScope": [
    "desktop procedure edit mutation",
    "Save",
    "rendering correctness",
    "learner assessment",
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
- Keep observation read-only: no AST mutation, no desktop edit action, no Save,
  no Run, no rendering oracle, and no learner assessment.
- Fail loudly for malformed scenarios, unknown workflows, missing supporting
  artifacts, invalid JSON, or missing required artifact fields.

## Claim boundaries

When implemented, this shard may claim only:

- Select Project opened the configured first-lesson starter when supporting
  `tab-click-observation.json` says so.
- The post-open Alice main window was observed when supporting
  `post-project-open-observation.json` says so.
- The live desktop procedure/code-editor target for `scene.eatmeFirstLesson` was
  either observed through a stable automation target or blocked with a precise
  machine-readable reason.

This shard must not claim:

- A desktop procedure edit was performed.
- AST/project-level procedure editing was performed by the live desktop.
- Save, Save As, backup, or project write behavior.
- Visible rendering correctness or world-canvas pixel correctness.
- Learner grading, rubric scoring, correctness assessment, or creativity
  assessment.
- Full first-lesson completion.

## Relationship to adjacent shards

Build and use this shard after the Select Project target starter evidence and
before any desktop procedure edit attempt. It is intended to become the
live-desktop target-discovery link between these existing references:

- [Select Project Africa Full AT-SPI evidence](./select-project-africa-full-atspi-evidence.md)
- [First-Lesson Procedure/Edit Seam](./first-lesson-procedure-edit-seam.md)
- [Desktop Procedure Edit and Save Automation](./desktop-procedure-edit-and-save-automation.md)
- [Save Menu Dialog Write Proof](./save-menu-dialog-write-proof.md)
- [Post-open runtime/display accessibility evidence](./post-open-runtime-display-accessibility-evidence.md)

After implementation, when this shard is `observed`, the next safe shard will be
a read/write desktop procedure edit action proof that reacquires the same target
and performs the smallest supported edit. When this shard is `blocked`, the next
implementation step will be to expose or locate a stable live desktop procedure
tab or code-editor automation target before attempting a real desktop edit.
