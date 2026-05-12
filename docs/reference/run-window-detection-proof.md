# Run-Window Detection Proof

This reference defines the focused proof that Alice's Run window can be detected
after creation under a headful display (Xvfb or native). The feature uses
`java.awt.Window.getWindows()` polling to observe a newly created `JFrame`,
records its window ID and title, and writes a structured JSON evidence artifact.
It does not claim rendering correctness, run execution, world execution
correctness, active rendering, Save behavior, grading, or full UI automation.

The executable proof is `org.alice.tools.RunWindowDetectionProofTest`. It is
gated behind a `GraphicsEnvironment.isHeadless()` check and skips cleanly on
headless CI. Under Xvfb or a native display, it creates a `JFrame`, polls for
its appearance via `Window.getWindows()`, and writes atomic evidence JSON.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Artifact API](#artifact-api)
- [Java test API](#java-test-api)
- [Configuration](#configuration)
- [Path safety](#path-safety)
- [Evidence schema](#evidence-schema)
- [Detection algorithm](#detection-algorithm)
- [Negative checks](#negative-checks)
- [Evidence boundaries](#evidence-boundaries)
- [Examples](#examples)

## Scope

The proof covers exactly this detection seam:

```text
JFrame created + setVisible(true)
  -> Window.getWindows() poll (100ms × 100 iterations, 10s max)
  -> match by title substring
  -> record window_id (System.identityHashCode as hex) + window_title
  -> run-window-detection.json
```

The proof verifies that `Window.getWindows()` can observe a newly shown window
within a bounded polling interval. It does not verify that the window renders
correctly, runs a program, advances a lesson, saves a project, or performs any
Alice-specific behavior beyond detection.

This test proves the RabbitHole side of the Run-window detection that eatme
issue #246 needs. It establishes that the JVM window enumeration API works
reliably under Xvfb for downstream tooling to build upon.

## Usage

Run the focused proof from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

NODE_OPTIONS=--max-old-space-size=32768 \
xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.RunWindowDetectionProofTest \
  test
```

On a headless CI node without Xvfb wrapping, the test skips via `assumeFalse`:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.RunWindowDetectionProofTest \
  test
```

The test is skipped (not failed) when `GraphicsEnvironment.isHeadless()` returns
`true`. Surefire reports `Tests run: 1, Skipped: 1`.

## Artifact API

The success artifact is:

```text
run-window-detection.json
```

It is written only inside the JUnit `TemporaryFolder` evidence directory.

Required fields on success:

| Field | Type | Required value or meaning |
| --- | --- | --- |
| `schema_version` | string | `eatme.alice-run-window-detection/v1`. |
| `status` | string | `detected`. |
| `window_title` | string | JSON-escaped title of the detected window. |
| `window_id` | string | `System.identityHashCode(window)` formatted as a hex string (e.g., `0x1a2b3c4d`). |
| `poll_interval_ms` | number | `100`. |
| `max_poll_iterations` | number | `100`. |
| `max_wait_seconds` | number | `10`. |
| `rendering_correctness_claimed` | boolean | Always `false`. |
| `run_execution_claimed` | boolean | Always `false`. |
| `world_execution_claimed` | boolean | Always `false`. |
| `active_rendering_claimed` | boolean | Always `false`. |
| `save_claimed` | boolean | Always `false`. |
| `grading_claimed` | boolean | Always `false`. |
| `full_ui_automation_claimed` | boolean | Always `false`. |
| `does_not_claim` | string array | Includes `active-rendering`, `run-execution`, `world-execution-correctness`, `rendering-correctness`, `save`, `grading`, `full-ui-automation`. |

Required fields on failure:

| Field | Type | Required value or meaning |
| --- | --- | --- |
| `schema_version` | string | `eatme.alice-run-window-detection/v1`. |
| `status` | string | `not_detected`. |
| `failure_reason` | string | Human-readable reason (e.g., `"window not found within 10s"`). |
| `poll_interval_ms` | number | `100`. |
| `max_poll_iterations` | number | `100`. |
| `max_wait_seconds` | number | `10`. |
| All `*_claimed` booleans | boolean | Always `false`. |
| `does_not_claim` | string array | Same as success. |

## Java test API

`RunWindowDetectionProofTest` is a JUnit 4 test in the `org.alice.tools` package
(same package as `EatmeRunWindowEvidence`). It uses package-private helpers from
`EatmeRunWindowEvidence`:

| API | Usage |
| --- | --- |
| `EatmeRunWindowEvidence.artifactPath(evidenceDir, relativeName)` | Validates the artifact path is a single relative file name inside the evidence directory. Rejects traversal, nesting, and absolute paths. |
| `EatmeRunWindowEvidence.escapeJson(value)` | JSON-escapes window titles so quotes, backslashes, tabs, newlines, carriage returns, and control characters cannot corrupt the artifact shape. |

The test uses `@Rule TemporaryFolder` for evidence isolation and disposes the
`JFrame` in a `finally` block to prevent window leaks.

## Configuration

No Alice product preference or JVM property is required. The test is
self-contained.

| Setting | Required value | Purpose |
| --- | --- | --- |
| Display | Xvfb (`xvfb-run -a`) or native display | Required for `Window.getWindows()` to enumerate AWT windows. |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Preserves the repository's Node-backed orchestration memory setting. |
| `tweedle-lang` submodule | Initialized with `git submodule update --init tweedle-lang` | Required before Maven reactor validation. |
| Maven flags | `-DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false` | Keeps validation bounded to the focused no-Sims `core/ide` characterization surface. |

The headless gate uses `org.junit.Assume.assumeFalse(GraphicsEnvironment.isHeadless())`.
No timeout configuration, sleep, or polling-timeout-based success criteria is
used beyond the detection algorithm's own bounded poll.

## Path safety

The artifact path validation reuses `EatmeRunWindowEvidence.artifactPath()`:

| Unsafe input | Required behavior |
| --- | --- |
| `../run-window-detection.json` | Reject parent traversal. |
| `nested/run-window-detection.json` | Reject nested artifact paths. |
| `/tmp/run-window-detection.json` | Reject absolute paths. |
| Empty artifact name | Reject missing artifact names. |

Window titles are JSON-escaped via `EatmeRunWindowEvidence.escapeJson()` before
writing so untrusted display metadata cannot corrupt the artifact shape.

The atomic write uses `Files.createTempFile` + `Files.move(ATOMIC_MOVE)` with a
symlink pre-check, following the same pattern as `EatmeRunWindowEvidence`.

## Evidence schema

Representative success artifact:

```json
{
  "schema_version": "eatme.alice-run-window-detection/v1",
  "status": "detected",
  "window_title": "Run Window Detection Proof",
  "window_id": "0x1a2b3c4d",
  "poll_interval_ms": 100,
  "max_poll_iterations": 100,
  "max_wait_seconds": 10,
  "rendering_correctness_claimed": false,
  "run_execution_claimed": false,
  "world_execution_claimed": false,
  "active_rendering_claimed": false,
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

Representative failure artifact:

```json
{
  "schema_version": "eatme.alice-run-window-detection/v1",
  "status": "not_detected",
  "failure_reason": "window not found within 10s",
  "poll_interval_ms": 100,
  "max_poll_iterations": 100,
  "max_wait_seconds": 10,
  "rendering_correctness_claimed": false,
  "run_execution_claimed": false,
  "world_execution_claimed": false,
  "active_rendering_claimed": false,
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

## Detection algorithm

The polling algorithm is bounded and deterministic:

1. Create a `JFrame` with a known title and call `setVisible(true)`.
2. Poll `Window.getWindows()` every 100ms.
3. On each iteration, search for a `Window` whose title contains the expected
   substring and whose `isShowing()` returns `true`.
4. If found within 100 iterations (10 seconds), record `status: detected` with
   the window's `System.identityHashCode` (hex) and title.
5. If not found after 100 iterations, record `status: not_detected` with a
   `failure_reason`.
6. Dispose the `JFrame` in a `finally` block regardless of outcome.

The algorithm does not use `Thread.sleep()` with unbounded retry, robot-based
pixel sampling, accessibility tree traversal, or window-manager IPC. It uses
only the standard `java.awt.Window` enumeration API.

## Negative checks

The test asserts the success path (since it controls the JFrame creation), but
the evidence infrastructure supports both paths:

| Condition | Expected result |
| --- | --- |
| Headless environment | `assumeFalse` skips the test cleanly; no artifact written. |
| Window not found within 10s | `status: not_detected` with `failure_reason` in artifact. |
| Window found | `status: detected` with `window_id` and `window_title`. |
| JFrame leak | `dispose()` in `finally` prevents leak regardless of outcome. |

## Evidence boundaries

Use this evidence only for Run-window detection via `Window.getWindows()`. This
proof may claim only:

- A `JFrame` created by the test is detectable via `Window.getWindows()` polling
  under Xvfb or a native display.
- The detected window's identity hash and title can be recorded in structured
  JSON evidence.
- The detection completes within a bounded 10-second polling interval.

The existing `EatmeRunWindowEvidence` creation/wiring schema includes
`run_program_claimed` (specific to the `runProgram()` invocation path). That
field is intentionally absent from the detection proof schema because
Run-window detection does not interact with program execution at all.

This proof must not claim:

- Rendering correctness of the Run window.
- Run execution or world execution correctness.
- Active rendering behavior.
- Save, Save As, or project persistence.
- Grading, scoring, or creative assessment.
- Lesson completion.
- Full desktop UI automation.
- That Alice's actual Run window (as opposed to a test JFrame) is detectable.
  This proof establishes the detection mechanism; integration with the real Run
  window is a separate concern.

Adjacent claims stay in their own lanes:

| Claim | Use instead |
| --- | --- |
| Run-window creation/wiring evidence | [Run-Window Creation/Wiring Contract](./run-window-creation-wiring-contract.md). |
| Runtime/display accessibility and pixel sampling | [Post-open runtime/display accessibility evidence](./post-open-runtime-display-accessibility-evidence.md). |
| Save menu/dialog/write behavior | [Save Menu Dialog Write Proof](./save-menu-dialog-write-proof.md). |
| First-lesson procedure/code-editor action seam | [First-Lesson Code-Editor Action Proof](./first-lesson-code-editor-action-proof.md). |

## Examples

### Focused command under Xvfb

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.RunWindowDetectionProofTest \
  test
```

### Headless skip verification

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.RunWindowDetectionProofTest \
  test 2>&1 | grep "Tests run"
# Expected: Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
```

### Reviewing a generated artifact

```bash
python3 -m json.tool <evidence-dir>/run-window-detection.json
```

Expected non-claim checks:

```text
rendering_correctness_claimed=false
run_execution_claimed=false
world_execution_claimed=false
active_rendering_claimed=false
save_claimed=false
grading_claimed=false
full_ui_automation_claimed=false
```

If any of those booleans is `true`, the artifact does not satisfy this contract.
