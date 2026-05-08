# Post-open runtime/display accessibility evidence

This reference documents the finished Alice desktop outside-in QA scenario for
post-open runtime/display accessibility evidence.

The scenario proves one narrow claim: after Alice opens a project through the
existing supported launch/open path, the live accessibility tree exposes at
least one runtime/display candidate. It does not prove full visible rendering
correctness, deployed installer success, full world execution, grading, lesson
completion, active Save behavior, active Select Project behavior, or decoder
behavior.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Configuration](#configuration)
- [Evidence API](#evidence-api)
- [Examples](#examples)
- [Review rules](#review-rules)
- [Validation commands](#validation-commands)
- [Troubleshooting](#troubleshooting)

## Scope

The scenario is part of the Alice desktop outside-in QA lane:

```text
qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml
qa/outside-in/alice-desktop/runners/post-open-runtime-display-probe.py
qa/outside-in/alice-desktop/runners/run-scenario.sh
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/schema/scenario.schema.json
```

It reuses the existing real Alice launch/open path under Xvfb and AT-SPI. The
runner starts Alice with the `alice-ide-atk` Maven execution, performs the
supported project-open setup, then invokes the read-only runtime/display probe.

The probe is observational. It does not click controls, save projects, select
new starters, execute worlds, grade work, inspect decoder output, or mutate
project data.

## Usage

Run commands from the repository root.

Validate the scenario catalog first:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Collect post-open runtime/display accessibility evidence:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-open-runtime-display \
  --timeout-seconds 300
```

The runner writes a timestamped directory:

```text
qa/outside-in/alice-desktop/evidence/post-open-runtime-display/
  alice-desktop-post-open-runtime-display-accessibility-evidence/
    <timestamp>/
```

Generated evidence is local run output. Keep it uncommitted.

## Configuration

| Variable | Purpose |
| --- | --- |
| `NODE_OPTIONS=--max-old-space-size=32768` | Preferred memory setting for surrounding Node-based QA orchestration. The shell runner itself does not require Node. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` | Enables isolated first-run License Agreement acceptance state for controlled QA launches only. |
| `ALICE_QA_DISPLAY` | Reuses a specific X display instead of selecting one automatically. |
| `ALICE_QA_SCREEN` | Sets Xvfb screen geometry. Defaults to `1280x900x24`. |
| `ALICE_QA_READY_WAIT_SECONDS` | Overrides the scenario readiness wait before capture/probe steps. |

Runtime prerequisites:

| Requirement | Why it is needed |
| --- | --- |
| Java 21 and Maven | Build and launch Alice through the existing Maven path. |
| `git submodule update --init tweedle-lang` | Initializes the required Tweedle grammar submodule before broad Maven validation. |
| Xvfb | Provides the controlled display session. |
| `python3-pyatspi` | Lets the probe read the AT-SPI accessibility tree. |
| `/usr/share/java/java-atk-wrapper.jar` | Makes Swing accessibility data visible to AT-SPI for the spawned Java process. |
| AT-SPI2 accessibility bus | Provides the live accessibility registry for the user session. |

## Evidence API

The decision artifact is:

```text
post-open-runtime-display-accessibility-evidence.json
```

`status.txt` points to that artifact with:

```text
runtimeDisplayAccessibilityEvidence=post-open-runtime-display-accessibility-evidence.json
runtimeDisplayAccessibilityStatus=<observed|blocked>
runtimeDisplayAccessibilityBlocker=<blocker>
outcome=<passed|blocked>
```

### Observed artifact

An observed result has this shape:

```json
{
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "runtimeDisplayCandidateCount": 1,
  "runtimeDisplayCandidates": [
    {
      "role": "canvas",
      "name": "Scene display",
      "childCount": 0,
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "blocker": "none",
  "blockerDetail": ""
}
```

An observed result means the live post-open accessibility tree exposed at least
one runtime/display candidate. It is not a visual rendering oracle and is not a
claim about project execution, grading, lesson completion, Save, Select Project,
installer success, or decoder behavior.

### Blocked artifact

When the runner cannot collect the narrow evidence, it still writes the same
artifact with a precise blocker:

```json
{
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "blocked",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "postOpenRuntimeDisplayAccessibilityObserved": false,
  "runtimeDisplayCandidateCount": 0,
  "runtimeDisplayCandidates": [],
  "blocker": "runtime-display-accessible-candidate-not-found",
  "blockerDetail": "No accepted runtime/display candidate was found after project open."
}
```

Supported blocker values include:

| Blocker | Meaning |
| --- | --- |
| `x-server-unavailable` | Xvfb or another required X server path is unavailable. |
| `display-allocation-unavailable` | The runner could not allocate a controlled display. |
| `pyatspi-not-installed` | No Python interpreter with `pyatspi` is available. |
| `at-spi-registry-unavailable` | The AT-SPI registry is not reachable. |
| `atk-wrapper-not-loaded` | The Java ATK wrapper is unavailable or not active for the Alice process. |
| `post-open-window-not-observed` | The prerequisite post-open Alice window signal was not observed. |
| `runtime-display-accessible-candidate-not-found` | The probe reached the accessibility tree but found no accepted runtime/display candidate. |
| `java-pid-not-in-inventory` | The probe could not map the Alice Java process from the window inventory. |
| `input-unreadable` | A required input artifact could not be read. |

The artifact must stay small and safe: no credentials, environment dumps,
arbitrary process dumps, unrelated desktop windows, saved project contents,
decoder output, grading state, lesson state, or world execution traces.

## Examples

### Review a successful observation

```bash
run_dir=qa/outside-in/alice-desktop/evidence/post-open-runtime-display/\
alice-desktop-post-open-runtime-display-accessibility-evidence/<timestamp>

python3 -m json.tool \
  "$run_dir/post-open-runtime-display-accessibility-evidence.json"

sed -n '1,120p' "$run_dir/status.txt"
```

Accept the run only when the JSON artifact records:

```text
status=observed
postOpenRuntimeDisplayAccessibilityObserved=true
runtimeDisplayCandidateCount > 0
blocker=none
```

Also review `launch.log`, `xvfb.log`, `x-window-inventory.json`, and the
captured screenshot as supporting evidence for the run environment.

### Review a blocked run

```bash
python3 -m json.tool \
  qa/outside-in/alice-desktop/evidence/post-open-runtime-display/\
alice-desktop-post-open-runtime-display-accessibility-evidence/*/\
post-open-runtime-display-accessibility-evidence.json
```

Treat `status=blocked` as a machine-readable gap report. Do not replace it with
a manual claim that rendering, world execution, grading, lesson completion,
Save, Select Project, installer success, or decoder behavior passed.

## Review rules

1. The JSON artifact is the decision artifact.
2. `status=observed` is accepted only with `blocker=none` and at least one
   runtime/display candidate.
3. `status=blocked` is an honest blocked result, not a failed documentation
   claim and not a success substitute.
4. Launch, window, pixel, and post-open accessibility artifacts support this
   lane, but none of them expands it into full rendering correctness.
5. Generated evidence stays under `qa/outside-in/alice-desktop/evidence/` or a
   caller-provided evidence directory and remains uncommitted.

## Validation commands

Use the focused desktop QA checks when changing this lane:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-contract.sh
bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-probe.sh
```

The contract tests cover schema/validator/runner parity, the runtime/display
artifact name, status fields, blocked fallback behavior, and the narrow claim
token.

## Troubleshooting

| Symptom | Meaning | Action |
| --- | --- | --- |
| `pyatspi-not-installed` | The runner cannot find a Python interpreter with `pyatspi`. | Install `python3-pyatspi` or run in an environment where it is available to the selected interpreter. |
| `atk-wrapper-not-loaded` | Swing accessibility is not visible through AT-SPI. | Confirm `/usr/share/java/java-atk-wrapper.jar` exists and the `alice-ide-atk` launch path is active. |
| `post-open-window-not-observed` | The prerequisite project-open setup did not prove a post-open Alice window. | Review `post-project-open-observation.json`, `tab-click-observation.json`, and `launch.log`. |
| `runtime-display-accessible-candidate-not-found` | The probe reached the post-open accessibility tree but did not find an accepted runtime/display candidate. | Preserve the blocker artifact and use it to guide the next implementation step; do not broaden the evidence claim. |
| `x-server-unavailable` or `display-allocation-unavailable` | Controlled display setup failed before runtime/display observation. | Fix the display environment or collect the same scenario in a supported desktop QA environment. |
