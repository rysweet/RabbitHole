# Tutorial: Trace the Run-Window Detection Proof

This tutorial walks through the focused Run-window detection proof from the
headless gate through window creation, polling detection, evidence writing, and
the explicit non-claim boundary. The proof establishes that
`java.awt.Window.getWindows()` can detect a window under Xvfb, which is the
RabbitHole side of the Run-window detection that eatme issue #246 needs.

## What you will do

1. Prepare the repository for focused `core/ide` validation.
2. Understand the headless display gate.
3. Run `RunWindowDetectionProofTest` under Xvfb.
4. Trace the window creation and polling detection algorithm.
5. Review the evidence artifact shape.
6. Verify the atomic write and path safety patterns.
7. Stop at the explicit non-claim boundary.

## Before you start

Open a terminal at the repository root:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

Confirm Xvfb is available:

```bash
which xvfb-run && echo "Xvfb available" || echo "Xvfb not available — test will skip"
```

## Step 1: Understand the display gate

The test begins with:

```java
assumeFalse("Requires a display (Xvfb or native)",
    GraphicsEnvironment.isHeadless());
```

This is not a failure condition. On headless CI without Xvfb, the test is
skipped via JUnit's `Assume` mechanism. Surefire reports it as `Skipped: 1`
with zero failures. This behavior is correct and intentional — the proof
requires a display server to create and enumerate AWT windows.

## Step 2: Run the proof under Xvfb

Run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.RunWindowDetectionProofTest \
  test
```

`xvfb-run -a` allocates a virtual display and sets `DISPLAY` before launching
Maven. The `-a` flag auto-selects an available display number to avoid
conflicts with other Xvfb sessions.

Expected result:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

## Step 3: Trace the window creation

The test creates a controlled `JFrame`:

```java
JFrame frame = new JFrame("Run Window Detection Proof");
frame.setSize(200, 200);
frame.setVisible(true);
```

This is a synthetic window owned by the test, not Alice's real Run window. The
proof establishes that the detection mechanism works; integration with Alice's
`RunComposite` Run window is a separate concern handled by the
[Run-Window Creation/Wiring Contract](../reference/run-window-creation-wiring-contract.md).

## Step 4: Trace the polling detection

After `setVisible(true)`, the test polls:

```java
String title = "Run Window Detection Proof";
// ...
for (int i = 0; i < 100; i++) {
    for (Window w : Window.getWindows()) {
        if (w instanceof JFrame jf
            && jf.getTitle().contains(title)
            && jf.isShowing()) {
            // detected — record window_id and window_title
        }
    }
    Thread.sleep(100);  // 100ms poll interval
}
```

Key design points:

| Parameter | Value | Rationale |
| --- | --- | --- |
| Poll interval | 100ms | Fast enough to detect promptly; slow enough to avoid CPU spin. |
| Max iterations | 100 | 100 × 100ms = 10 seconds maximum wait. |
| Match criterion | Title substring + `isShowing()` | Title match identifies the right window; `isShowing()` confirms it is mapped on the display. |
| Window ID | `System.identityHashCode(window)` as hex | Unique per-JVM identifier without depending on native window handles. |

The algorithm is bounded. It does not retry indefinitely, use robot-based pixel
sampling, traverse the accessibility tree, or call window-manager IPC.

## Step 5: Trace the evidence artifact

On detection, the test writes `run-window-detection.json` to its JUnit
`TemporaryFolder`:

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
    "rendering-correctness",
    "grading",
    "save",
    "full-ui-automation",
    "active-rendering",
    "run-execution",
    "world-execution-correctness"
  ]
}
```

The `window_id` value varies per run; the hex format is fixed.

If detection fails (not expected for this proof since the test owns the window),
the artifact records:

```json
{
  "schema_version": "eatme.alice-run-window-detection/v1",
  "status": "not_detected",
  "failure_reason": "window not found within 10s"
}
```

Both paths include the polling parameters and the full non-claim set.

## Step 6: Trace the safety patterns

The test reuses safety patterns from `EatmeRunWindowEvidence`:

| Pattern | How it is used |
| --- | --- |
| `artifactPath()` | Validates the artifact path is a single relative file name. Rejects parent traversal, nesting, and absolute paths. |
| `escapeJson()` | JSON-escapes the window title before embedding in the artifact. Handles quotes, backslashes, tabs, newlines, carriage returns, and control characters. |
| Atomic write | `Files.createTempFile` + `Files.move(ATOMIC_MOVE)` prevents partial-write artifacts. |
| Symlink pre-check | Refuses to overwrite a symlinked artifact path. |
| `JFrame.dispose()` in `finally` | Prevents window leaks regardless of test outcome. |

These patterns are shared with `EatmeRunWindowEvidence` via package-private
access. The test is in the same package (`org.alice.tools`).

## Step 7: Check the non-claim boundary

The artifact must keep these booleans false:

```json
{
  "rendering_correctness_claimed": false,
  "run_execution_claimed": false,
  "world_execution_claimed": false,
  "active_rendering_claimed": false,
  "save_claimed": false,
  "grading_claimed": false,
  "full_ui_automation_claimed": false
}
```

The `does_not_claim` array must include all seven exclusions. These fields are
not boilerplate — they are part of the evidence boundary and must stay
reviewable in every artifact.

## Step 8: Stop at the seam

This tutorial covers only Run-window detection via `Window.getWindows()` under
Xvfb. It does not cover:

- Rendering correctness of any window.
- Run execution or world execution correctness.
- Active rendering behavior.
- Save behavior.
- Grading, scoring, or creative assessment.
- Lesson completion.
- Full UI automation.
- Detection of Alice's actual `RunComposite` Run window (this proof uses a
  synthetic `JFrame`).

For the full artifact contract, see the [Run-Window Detection Proof
reference](../reference/run-window-detection-proof.md). For the task-oriented
review guide, see [Run the Run-Window Detection
Proof](../howto/run-run-window-detection-proof.md).
