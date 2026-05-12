# Run the Run-Window Detection Proof

Use this guide to run and review the focused Java proof that a window created
under Xvfb can be detected via `java.awt.Window.getWindows()` polling. This
proves the RabbitHole side of the Run-window detection that eatme issue #246
needs.

For the full contract, see the [Run-Window Detection Proof
reference](../reference/run-window-detection-proof.md).

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

Verify that `xvfb-run` is available if you want the test to execute (not skip):

```bash
which xvfb-run
```

## Run under Xvfb

Run the proof with a virtual display:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.RunWindowDetectionProofTest \
  test
```

Expected output includes:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

## Verify headless skip behavior

On a headless CI node without Xvfb wrapping, the test skips cleanly:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.RunWindowDetectionProofTest \
  test 2>&1 | grep "Tests run"
```

Expected output:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
```

The `assumeFalse(GraphicsEnvironment.isHeadless())` gate produces a skip, not a
failure. This is the correct behavior for CI environments without a display.

## Review the proof

Review the test for these assertions:

| Assertion | Meaning |
| --- | --- |
| `assumeFalse(GraphicsEnvironment.isHeadless())` | Test requires a display; skips cleanly otherwise. |
| `JFrame` created with known title | The proof controls the window it detects. |
| `Window.getWindows()` poll finds the frame | The JVM window enumeration API works under the current display. |
| `window_id` is non-empty hex string | `System.identityHashCode` produces a usable window identifier. |
| `window_title` matches expected value | The detected window is the one the proof created. |
| Evidence JSON passes schema assertions | The artifact conforms to `eatme.alice-run-window-detection/v1`. |
| `JFrame.dispose()` in `finally` | No window leak regardless of test outcome. |

## Review evidence artifact shape

The success artifact `run-window-detection.json` must contain:

```text
schema_version=eatme.alice-run-window-detection/v1
status=detected
window_title=<expected title>
window_id=0x<hex digits>
```

Every capability boolean must remain false:

```text
rendering_correctness_claimed=false
run_execution_claimed=false
world_execution_claimed=false
active_rendering_claimed=false
save_claimed=false
grading_claimed=false
full_ui_automation_claimed=false
```

The `does_not_claim` array must include:

```text
rendering-correctness
grading
save
full-ui-automation
active-rendering
run-execution
world-execution-correctness
```

## Keep the review narrow

Accept this proof only as Run-window detection evidence via
`Window.getWindows()` under Xvfb. Do not cite it for:

- Rendering correctness of Alice's Run window.
- Run execution or world execution correctness.
- Active rendering behavior.
- Save behavior.
- Grading, creative assessment, or lesson completion.
- Full desktop UI automation.
- Detection of Alice's actual Run window (this proof uses a test JFrame).

Adjacent claims remain owned by their own documents:

| Claim | Use |
| --- | --- |
| Run-window creation/wiring evidence | [Run-Window Creation/Wiring Contract](../reference/run-window-creation-wiring-contract.md). |
| Save menu/dialog/write behavior | [Save Menu Dialog Write Proof](../reference/save-menu-dialog-write-proof.md). |
| First-lesson code-editor action seam | [First-Lesson Code-Editor Action Proof](../reference/first-lesson-code-editor-action-proof.md). |
