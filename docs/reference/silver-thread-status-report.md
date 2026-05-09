# Alice Desktop Silver-Thread Status Report

This reference documents the repository-owned silver-thread QA report:

```text
qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh
```

The report is a fail-closed shell artifact that aggregates existing executable
Alice desktop evidence for the product-spanning thread:

```text
launch Alice
  -> open or prepare a starter-compatible world/program
  -> change it through object placement and procedure edit seams
  -> observe a run-window or render-affordance seam
  -> report bounded Save/reopen evidence when directly available
```

The script does not add a new QA scenario workflow. It inspects stable
repository-owned seams, marker strings, and scenario/test contracts that already
exercise the underlying behavior.

## Contents

- [Usage](#usage)
- [Status model](#status-model)
- [Required evidence](#required-evidence)
- [Optional Save/reopen evidence](#optional-savereopen-evidence)
- [Output API](#output-api)
- [Configuration](#configuration)
- [Examples](#examples)
- [Claim boundaries](#claim-boundaries)
- [Troubleshooting](#troubleshooting)

## Usage

Run the report from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh
```

The script accepts no arguments. It uses only hard-coded repository-relative
paths and literal marker checks.

Use this report when a pull request needs one compact answer to whether the
current repository still has executable evidence for the bounded Alice desktop
silver thread. Use the lower-level seam tests when a review needs to debug a
specific evidence category.

## Status model

The report has one top-level status line:

```text
status:silver_thread=<covered_bounded|blocked>
```

| Status | Meaning |
| --- | --- |
| `covered_bounded` | Every required evidence category has a direct repository-owned seam and the required literal markers are present. |
| `blocked` | At least one required evidence category is missing or no longer contains the required marker. The report exits non-zero and prints `gap:` lines. |

Exit codes:

| Code | Meaning |
| --- | --- |
| `0` | All required evidence categories are covered within the bounded claim scope. |
| `1` | One or more required evidence categories are missing. |
| `2` | The report itself was invoked from outside the repository layout or hit an internal contract error. |

Optional Save/reopen gaps do not change the exit code. They are printed as
bounded gaps unless direct Save/reopen evidence is found.

## Required evidence

The report requires these evidence categories:

| Category | Required claim | Repository-owned seam |
| --- | --- | --- |
| `launch` | Alice can be launched through the existing desktop QA path. | `qa/outside-in/alice-desktop/runners/run-scenario.sh`, `qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml`, and the `xvfb-real-alice`/`alice-ide-atk` launch markers. |
| `starter_world_or_program_change` | A starter-compatible world/program can be changed by the focused repository seam. | `core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java` with the chained `placed-project.a3p -> edited-project.a3p` proof. |
| `object_placement` | A deterministic object-placement step writes placement evidence. | `EatmePlaceObject.run(...)` coverage in `EatmeEditProcedureTest.java`, including `placed-project.a3p`, `placement.json`, and `scene.diff.json` markers. |
| `procedure_edit` | The targeted first-lesson procedure accepts one deterministic edit and records target-only marker evidence. | `core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java` and `EatmeEditProcedureTest.java`, including `scene.eatmeFirstLesson`, `append-comment`, and `wave4-code-editor-action-proof`. |
| `run_window_or_render_affordance` | A post-open runtime/display or render-affordance seam is observable without claiming rendered-world correctness. | `qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml`, `qa/outside-in/alice-desktop/runners/run-scenario.sh`, and marker strings such as `worldCanvasPixelTarget` and `visible-rendering-pixel-sampling-blocker.json`. |

Each category must be backed by both:

1. A checked-in file at the expected repository-relative path.
2. One or more literal marker strings in that file or its adjacent executable
   contract.

A missing file or missing marker is a gap. The report must not infer coverage
from similarly named files, generated local evidence, prose-only claims, or a
previous successful run.

## Optional Save/reopen evidence

Save/reopen evidence is reported only when the repository contains the direct
bounded seam:

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java
```

The required markers for optional coverage are:

```text
Robot File menu Save activation joined to dialog/write/readback evidence
robot-save-menu-dialog-write-readback-proof.json
robotSaveMenuRoundTripMarker
IoUtilities.readProject
```

When those markers are present, the report prints:

```text
evidence:save_reopen=covered_bounded path=core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java
```

This means only that the bounded Robot File-menu Save/dialog/write/readback
marker proof exists. It does not mean full desktop Save completion, Save As
coverage, native dialog coverage, full project lifecycle coverage, or visible
world execution.

When the seam or markers are missing, the report prints a non-blocking gap:

```text
gap:save_reopen=not_covered_optional reason=direct_save_reopen_seam_missing
```

## Output API

The report writes grep-friendly lines to standard output. Field order is stable
within each line. Paths are repository-relative.

### Status line

```text
status:silver_thread=covered_bounded required=5 covered=5 gaps=0 optional_gaps=0
```

or:

```text
status:silver_thread=blocked required=5 covered=4 gaps=1 optional_gaps=1
```

### Evidence lines

Required coverage emits one line per category:

```text
evidence:<category>=covered_bounded path=<repo-relative-path> marker=<literal-marker>
```

Example:

```text
evidence:procedure_edit=covered_bounded path=core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java marker=wave4-code-editor-action-proof
```

### Gap lines

Missing required evidence emits:

```text
gap:<category>=missing_required path=<repo-relative-path> marker=<literal-marker>
```

If the file is absent, `marker` is omitted:

```text
gap:launch=missing_required path=qa/outside-in/alice-desktop/runners/run-scenario.sh
```

Optional missing Save/reopen evidence emits:

```text
gap:save_reopen=not_covered_optional reason=<reason>
```

### Claim-boundary lines

Every successful and blocked run prints explicit non-claim lines:

```text
claim-boundary:full_ui_automation=not_claimed
claim-boundary:visible_rendering_correctness=not_claimed
claim-boundary:full_world_execution_semantics=not_claimed
claim-boundary:grading=not_claimed
claim-boundary:creative_assessment=not_claimed
claim-boundary:full_desktop_save_completion=not_claimed
claim-boundary:new_scenario_workflow=not_added
```

These lines are part of the contract. Removing one weakens the report because
callers could accidentally overstate what the aggregate proves.

## Configuration

The report has no feature flags and no dynamic path configuration.

| Setting | Value |
| --- | --- |
| Working directory | Repository root, or any child directory from which the script can resolve the repository root. |
| `NODE_OPTIONS` | `--max-old-space-size=32768` for consistency with the Alice desktop QA lane. |
| Arguments | None. Any argument is an invocation error. |
| Network | Not used. |
| Generated artifacts | None. The report prints to standard output only. |

The script intentionally avoids user-controlled paths, globs, command
construction, `eval`, network access, package installation, privileged
operations, and broad UI automation.

## Examples

### Passing bounded report

```text
status:silver_thread=covered_bounded required=5 covered=5 gaps=0 optional_gaps=0
evidence:launch=covered_bounded path=qa/outside-in/alice-desktop/runners/run-scenario.sh marker=alice-ide-atk
evidence:starter_world_or_program_change=covered_bounded path=core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java marker=edited-project.a3p
evidence:object_placement=covered_bounded path=core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java marker=placement.json
evidence:procedure_edit=covered_bounded path=core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java marker=wave4-code-editor-action-proof
evidence:run_window_or_render_affordance=covered_bounded path=qa/outside-in/alice-desktop/runners/run-scenario.sh marker=worldCanvasPixelTarget
evidence:save_reopen=covered_bounded path=core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java marker=robotSaveMenuRoundTripMarker
claim-boundary:full_ui_automation=not_claimed
claim-boundary:visible_rendering_correctness=not_claimed
claim-boundary:full_world_execution_semantics=not_claimed
claim-boundary:grading=not_claimed
claim-boundary:creative_assessment=not_claimed
claim-boundary:full_desktop_save_completion=not_claimed
claim-boundary:new_scenario_workflow=not_added
```

### Blocked required evidence

```text
status:silver_thread=blocked required=5 covered=4 gaps=1 optional_gaps=0
evidence:launch=covered_bounded path=qa/outside-in/alice-desktop/runners/run-scenario.sh marker=alice-ide-atk
gap:procedure_edit=missing_required path=core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java marker=wave4-code-editor-action-proof
claim-boundary:visible_rendering_correctness=not_claimed
```

The non-zero status is intentional. A missing required marker means the aggregate
cannot be used as silver-thread evidence until the lower-level seam is restored
or the report is updated to point at a new direct executable seam.

### Optional Save/reopen gap

```text
status:silver_thread=covered_bounded required=5 covered=5 gaps=0 optional_gaps=1
gap:save_reopen=not_covered_optional reason=direct_save_reopen_seam_missing
claim-boundary:full_desktop_save_completion=not_claimed
```

The report can still pass because Save/reopen is optional for this artifact. The
gap must remain visible so reviewers do not convert the required silver-thread
status into an unsupported Save/reopen claim.

## Claim boundaries

The report may claim only:

- Existing repository-owned seams cover the required bounded silver-thread
  evidence categories.
- Missing required evidence fails closed with explicit `gap:` lines.
- Save/reopen is either covered by the direct bounded Robot proof seam or
  reported as an optional gap.

The report must not claim:

- Full UI automation.
- Visible rendering correctness.
- Full world execution semantics.
- Lesson completion.
- Learner grading, scoring, or creative assessment.
- Full desktop Save completion or every Save variant.
- New scenario workflow coverage.
- Current local generated evidence from a previous run.

## Troubleshooting

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `status:silver_thread=blocked` | A required file or marker is missing. | Read the adjacent `gap:` line, restore the underlying seam, or update the report to the new direct executable seam. |
| `gap:save_reopen=not_covered_optional` | The bounded Robot Save/readback seam was not found. | Keep the gap, or restore direct Save/readback evidence before claiming optional coverage. |
| `claim-boundary:*` lines are missing | The report contract is incomplete. | Restore the non-claim output before using the report in review. |
| The script is run with arguments | The invocation is unsupported. | Run the exact no-argument command from [Usage](#usage). |

Do not fix a blocked report by weakening marker checks to file-existence-only
checks. The report is intentionally brittle around stable evidence markers so it
fails closed when the lower-level seams drift.
