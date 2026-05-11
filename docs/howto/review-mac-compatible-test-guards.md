# Review Mac-Compatible Test Guards

Use this guide to verify or extend the cross-platform test compatibility
contracts for render-target evidence and headless-skip guards.

## Contents

- [Prerequisites](#prerequisites)
- [Run the validation](#run-the-validation)
- [Review render-target assertions](#review-render-target-assertions)
- [Review headless-skip guards](#review-headless-skip-guards)
- [Review macOS native-menu-bar skip guards](#review-macos-native-menu-bar-skip-guards)
- [Add a new platform-tolerant assertion](#add-a-new-platform-tolerant-assertion)
- [Add a new headless-skip guard](#add-a-new-headless-skip-guard)
- [Add a macOS native-menu-bar skip guard](#add-a-macos-native-menu-bar-skip-guard)
- [Common mistakes](#common-mistakes)

## Prerequisites

Run commands from the repository root. Initialize the grammar submodule:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

If a surrounding Node-based orchestrator is running:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the validation

Run all five affected test classes in one command:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=EatmeDesktopRunExecutionEvidenceTest,StageIdeSaveMenuE2EWriteProofTest,StageIdeSaveMenuDoClickToWriteProofTest,JMenuBarRobotClickSaveProofTest,RobotSaveMenuDialogWriteReadbackProofTest \
  test -q
```

On headless CI, expect the two Save menu tests and two Robot menu tests to
skip and the render-target test to pass. On macOS, the Robot menu tests also
skip due to the native menu bar. On a graphical display or Xvfb (Linux/Windows),
expect all tests to pass.

## Review render-target assertions

Open `EatmeDesktopRunExecutionEvidenceTest.java` and find the
`pixelObservationReportsBlockersForUnrealizedPanel` test method.

Check that dimension assertions use `extractIntField` followed by `>= 0`
bounds, not hardcoded `contains("\"renderTargetWidth\": 0")` strings:

```java
int renderTargetWidth = extractIntField(pixelObservationJson, "renderTargetWidth");
assertTrue("renderTargetWidth should be >= 0 but was " + renderTargetWidth, renderTargetWidth >= 0);
```

Check that the `render_target_has_no_positive_size` blocker assertion is
guarded by the actual extracted dimension values:

```java
if (renderTargetWidth <= 0 || renderTargetHeight <= 0) {
  assertTrue(json, json.contains("\"render_target_has_no_positive_size\""));
}
```

This prevents the assertion from firing on macOS where AWT may report non-zero
preferred dimensions for unrealized panels.

## Review headless-skip guards

Open `StageIdeSaveMenuDoClickToWriteProofTest.java` and find the
`saveMenuDoClickApprovesChooserAndWritesProjectFile` method. Confirm the first
line is:

```java
assumeTrue("Requires non-headless AWT display",
    SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable());
```

Open `StageIdeSaveMenuE2EWriteProofTest.java` and find the
`saveMenuE2ETriggersWriteFromFileMenuAction` method. Confirm the first line is:

```java
assumeFalse("requires Xvfb or another headful AWT display",
    GraphicsEnvironment.isHeadless());
```

Both methods must use JUnit `Assume` guards, not `if/return` patterns that
silently pass.

## Add a new platform-tolerant assertion

When writing a new render-evidence test that checks dimension values from a
JSON artifact:

1. Use `extractIntField(json, "fieldName")` to get the actual value.
2. Assert a range (`>= 0`, `> 0`) rather than an exact value.
3. Make any blocker assertions conditional on the extracted dimensions.
4. Use the extracted value in `observed` detail assertions so the expected
   string matches whatever the platform reports.

Example:

```java
int width = extractIntField(json, "componentWidth");
assertTrue("componentWidth should be >= 0", width >= 0);
if (width <= 0) {
  assertTrue(json, json.contains("\"component_has_no_width\""));
}
```

## Add a new headless-skip guard

When writing a test that requires a graphical display (Swing component
realization, `JFileChooser`, Robot, or JavaFX):

1. Add the guard as the first statement in the test method.
2. Use `assumeFalse(GraphicsEnvironment.isHeadless())` for simple headless
   detection.
3. Use `assumeTrue(SomeProbe.isNonHeadlessAwtDisplayAvailable())` when a
   more specific availability check exists.
4. Include a descriptive message so CI reports explain why the test skipped.
5. Do not duplicate blocker artifact validation inside the guard block. Write
   a dedicated test method for blocker validation instead.

Example:

```java
@Test(timeout = 60000)
public void myGuiProofTest() throws Exception {
  assumeFalse("requires a graphical display", GraphicsEnvironment.isHeadless());
  // ... test body ...
}
```

## Review macOS native-menu-bar skip guards

Open `JMenuBarRobotClickSaveProofTest.java` and find the
`robotClickFileMenuInVisibleJMenuBarSelectsSaveDispatchesToSaveOperation`
method. Confirm that **after** the headless guard, there is a macOS guard:

```java
assumeFalse("requires Xvfb or another headful AWT display", GraphicsEnvironment.isHeadless());
assumeFalse("macOS uses a native menu bar outside the JFrame; Robot screen-coordinate menu clicks miss",
    SystemUtilities.isMac());
```

Both guards use `assumeFalse` so JUnit reports **Skip**, not pass.

Open `RobotSaveMenuDialogWriteReadbackProofTest.java` and find the
`robotFileSaveApprovesChooserWritesReadableMarkedProjectOrWritesBlocker`
method. Confirm the macOS guard appears at the top of the method, before
the proof root setup:

```java
assumeFalse("macOS uses a native menu bar outside the JFrame; Robot screen-coordinate menu clicks miss",
    SystemUtilities.isMac());
```

Verify that the five evidence-contract test methods in the same class
(`evidenceArtifactReportsBlockerForHeadlessAwt`, etc.) do **not** contain
an `isMac()` guard — they validate artifact schemas without Robot interaction.

## Add a macOS native-menu-bar skip guard

When writing a test that uses AWT Robot screen-coordinate clicks on a
JMenuBar:

1. Add the guard after the headless check (both may apply independently).
2. Use `assumeFalse("macOS uses a native menu bar...", SystemUtilities.isMac())`.
3. Import `edu.cmu.cs.dennisc.java.lang.SystemUtilities`.
4. Only guard the Robot-driven method, not companion artifact-validation
   methods that don't use Robot interaction.
5. Include a message explaining the macOS native menu bar issue.

Example:

```java
import edu.cmu.cs.dennisc.java.lang.SystemUtilities;
import static org.junit.Assume.assumeFalse;

@Test
public void myRobotMenuTest() throws Exception {
  assumeFalse("requires a graphical display", GraphicsEnvironment.isHeadless());
  assumeFalse("macOS uses a native menu bar outside the JFrame; "
      + "Robot screen-coordinate menu clicks miss", SystemUtilities.isMac());
  // ... Robot menu click test body ...
}
```

## Common mistakes

| Mistake | Why it's wrong | Fix |
| --- | --- | --- |
| `assertTrue(json.contains("\"width\": 0"))` | Fails on macOS where AWT reports non-zero dimensions. | Use `extractIntField` + range assertion. |
| `if (isHeadless()) { return; }` | JUnit reports the test as passed, hiding that the proof never ran. | Use `assumeFalse(isHeadless())`. |
| Inlining blocker validation inside the skip guard | Duplicates assertions that belong in a dedicated blocker test. | Move to a separate test method. |
| Asserting exact pixel values | Platform-dependent; fails on HiDPI, different L&F, or virtual displays. | Assert ranges or structural properties. |
| Robot menu click test without `isMac()` guard | Fails on macOS because the native menu bar is outside the JFrame. | Add `assumeFalse(SystemUtilities.isMac())` after the headless guard. |
| Adding `isMac()` guard to evidence-contract methods | Unnecessarily skips artifact-schema validation that doesn't use Robot. | Guard only the Robot-driven method, not companion validators. |
