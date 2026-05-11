# Review Mac-Compatible Test Guards

Use this guide to verify or extend the cross-platform test compatibility
contracts for render-target evidence and headless-skip guards.

## Contents

- [Prerequisites](#prerequisites)
- [Run the validation](#run-the-validation)
- [Review render-target assertions](#review-render-target-assertions)
- [Review headless-skip guards](#review-headless-skip-guards)
- [Review macOS screen menu bar property override](#review-macos-screen-menu-bar-property-override)
- [Add a new platform-tolerant assertion](#add-a-new-platform-tolerant-assertion)
- [Add a new headless-skip guard](#add-a-new-headless-skip-guard)
- [Override the screen menu bar property in new Robot menu tests](#override-the-screen-menu-bar-property-in-new-robot-menu-tests)
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

On headless CI (any platform), expect the two Save menu tests and two Robot
menu tests to skip (headless guard) and the render-target test to pass. On
macOS with a graphical display, all tests pass — the Robot menu tests force
`apple.laf.useScreenMenuBar=false` to keep the JMenuBar inside the JFrame.
On Xvfb or a graphical display (Linux/Windows), expect all tests to pass.

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

## Review macOS screen menu bar property override

Open `JMenuBarRobotClickSaveProofTest.java` and confirm the class defines a
`SCREEN_MENU_BAR_PROPERTY` constant and a `previousScreenMenuBar` field:

```java
private static final String SCREEN_MENU_BAR_PROPERTY = "apple.laf.useScreenMenuBar";
private String previousScreenMenuBar;
```

Confirm the `@Before` method captures the original value and sets to `"false"`:

```java
@Before
public void captureProperties() {
  previousScreenMenuBar = System.getProperty(SCREEN_MENU_BAR_PROPERTY);
  System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false");
  // ... other captures ...
}
```

Confirm the `@After` method restores the property:

```java
@After
public void restorePropertiesAndActiveApplication() throws Exception {
  restoreProperty(SCREEN_MENU_BAR_PROPERTY, previousScreenMenuBar);
  // ... other restores ...
}
```

In the test method body, confirm there is a defensive re-set to `"false"`
after `ide.initialize()`:

```java
ide.initialize(new String[0]);
System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false");
```

Verify there is **no** `assumeFalse(SystemUtilities.isMac())` in either file.
The `SystemUtilities` import should not be present.

Repeat the same checks for `RobotSaveMenuDialogWriteReadbackProofTest.java`.

## Override the screen menu bar property in new Robot menu tests

When writing a new test that uses AWT Robot screen-coordinate clicks on a
JMenuBar:

1. Define a constant: `private static final String SCREEN_MENU_BAR_PROPERTY = "apple.laf.useScreenMenuBar";`
2. Add a `previousScreenMenuBar` field.
3. In `@Before`, capture the original and set to `"false"`:
   ```java
   previousScreenMenuBar = System.getProperty(SCREEN_MENU_BAR_PROPERTY);
   System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false");
   ```
4. In `@After`, restore via `restoreProperty(SCREEN_MENU_BAR_PROPERTY, previousScreenMenuBar)`.
5. After any `ide.initialize()` or `Application.initialize()` call in the test
   body, add `System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false")` as a
   defensive re-set (because `Application.initialize()` sets it to `"true"` on Mac).
6. Do **not** use `assumeFalse(SystemUtilities.isMac())` to skip on macOS.
   The property override makes Robot menu clicks work on macOS.

## Common mistakes

| Mistake | Why it's wrong | Fix |
| --- | --- | --- |
| `assertTrue(json.contains("\"width\": 0"))` | Fails on macOS where AWT reports non-zero dimensions. | Use `extractIntField` + range assertion. |
| `if (isHeadless()) { return; }` | JUnit reports the test as passed, hiding that the proof never ran. | Use `assumeFalse(isHeadless())`. |
| Inlining blocker validation inside the skip guard | Duplicates assertions that belong in a dedicated blocker test. | Move to a separate test method. |
| Asserting exact pixel values | Platform-dependent; fails on HiDPI, different L&F, or virtual displays. | Assert ranges or structural properties. |
| `assumeFalse(SystemUtilities.isMac())` for Robot menu tests | Unnecessarily skips tests on macOS. The menu bar can be forced into the JFrame. | Set `apple.laf.useScreenMenuBar=false` in `@Before` with `@After` restore. |
| Missing defensive re-set after `ide.initialize()` | `Application.initialize()` sets `apple.laf.useScreenMenuBar=true` on Mac, overriding the `@Before` setup. | Add `System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false")` after `ide.initialize()`. |
| Not restoring `apple.laf.useScreenMenuBar` in `@After` | Leaks test state into other tests in the same JVM. | Use `restoreProperty` to restore or clear the original value. |
