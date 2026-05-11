# Mac-Compatible Test Guards

This reference documents the cross-platform compatibility contracts for five
Alice desktop test classes: render-target dimension assertions, headless-skip
guards, and macOS native-menu-bar skip guards. These contracts ensure that CI
tests pass identically on Linux headless runners, macOS Retina displays, and
virtual-display (Xvfb) environments.

## Contents

- [Scope](#scope)
- [Artifact inventory](#artifact-inventory)
- [Render-target dimension contract](#render-target-dimension-contract)
- [Headless-skip guard contract](#headless-skip-guard-contract)
- [macOS screen menu bar property override](#macos-screen-menu-bar-property-override-issues-500-502)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Examples](#examples)
- [Compatibility rules](#compatibility-rules)
- [Non-claims](#non-claims)

## Scope

Three issues drove these contracts:

| Issue | Test class | Problem |
| --- | --- | --- |
| #496 | `EatmeDesktopRunExecutionEvidenceTest` | Hardcoded `renderTargetWidth: 0` / `renderTargetHeight: 0` assertions failed on macOS where AWT returns non-zero dimensions for unrealized panels. |
| #497 | `StageIdeSaveMenuDoClickToWriteProofTest`, `StageIdeSaveMenuE2EWriteProofTest` | Tests silently passed (or attempted real `JFileChooser` dialog popup) in headless environments instead of properly skipping. |
| #500 | `JMenuBarRobotClickSaveProofTest`, `RobotSaveMenuDialogWriteReadbackProofTest` | AWT Robot screen-coordinate menu clicks miss on macOS because the menu bar is native (owned by the OS, not inside the JFrame). |
| #502 | `JMenuBarRobotClickSaveProofTest`, `RobotSaveMenuDialogWriteReadbackProofTest` | Replace `assumeFalse(isMac())` skip with `apple.laf.useScreenMenuBar=false` property override so Robot menu tests run and pass on macOS. |

All contracts are test-only changes. No production code is modified.

## Artifact inventory

| File | Purpose |
| --- | --- |
| `core/ide/src/test/java/org/alice/tools/EatmeDesktopRunExecutionEvidenceTest.java` | Render-target evidence assertions. Contains the `extractIntField` helper and platform-tolerant `>= 0` assertions. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/StageIdeSaveMenuDoClickToWriteProofTest.java` | Save menu do-click proof. Uses `assumeTrue` for headless-skip. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/StageIdeSaveMenuE2EWriteProofTest.java` | Save menu E2E proof. Uses `assumeFalse(GraphicsEnvironment.isHeadless())` for headless-skip. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/JMenuBarRobotClickSaveProofTest.java` | Robot JMenuBar click proof. Uses `@Before`/`@After` to set `apple.laf.useScreenMenuBar=false` (capturing and restoring the original value) so the JMenuBar stays inside the JFrame where Robot coordinates work, including on macOS. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java` | Robot Save menu dialog write/readback proof. Uses `@Before`/`@After` to set `apple.laf.useScreenMenuBar=false` (capturing and restoring the original value). The Robot-driven test method and the five evidence-contract test methods all run on macOS. |

## Render-target dimension contract

### Before (issue #496)

The test asserted exact zero dimensions:

```java
assertTrue(json, json.contains("\"renderTargetWidth\": 0"));
assertTrue(json, json.contains("\"renderTargetHeight\": 0"));
assertTrue(json, json.contains("\"render_target_has_no_positive_size\""));
```

On macOS, AWT may report non-zero preferred dimensions for an unrealized
`JPanel` render target, even when the panel is not displayable or showing.
This caused a hard test failure because the JSON artifact correctly recorded
the platform-reported dimensions.

### After (fixed)

The test extracts the actual dimension values from the evidence JSON and
asserts they are non-negative:

```java
int renderTargetWidth = extractIntField(pixelObservationJson, "renderTargetWidth");
int renderTargetHeight = extractIntField(pixelObservationJson, "renderTargetHeight");
assertTrue("renderTargetWidth should be >= 0 but was " + renderTargetWidth, renderTargetWidth >= 0);
assertTrue("renderTargetHeight should be >= 0 but was " + renderTargetHeight, renderTargetHeight >= 0);
```

The `render_target_has_no_positive_size` blocker assertion is now conditional:

```java
if (renderTargetWidth <= 0 || renderTargetHeight <= 0) {
  assertTrue(json, json.contains("\"render_target_has_no_positive_size\""));
}
```

The size-detail `observed` field assertion uses the extracted values:

```java
assertTrue(json, json.contains(
    "\"observed\": \"renderTargetWidth=" + renderTargetWidth
    + ", renderTargetHeight=" + renderTargetHeight + "\""));
```

This accepts any non-negative dimension value the platform reports while still
verifying the evidence artifact records the actual dimensions and the correct
blockers.

### `extractIntField` helper

```java
private static int extractIntField(String json, String fieldName) {
  Matcher m = Pattern.compile(
      "\"" + Pattern.quote(fieldName) + "\":\\s*(\\d+)")
      .matcher(json);
  assertTrue("expected field '" + fieldName + "' in JSON", m.find());
  return Integer.parseInt(m.group(1));
}
```

The helper uses `Pattern.quote` for the field name and matches only
non-negative integer values (`\\d+`). It fails with a clear assertion message
when the field is absent.

## Headless-skip guard contract

### Before (issue #497)

`StageIdeSaveMenuDoClickToWriteProofTest` used a manual `if/return` pattern:

```java
if (!SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable()) {
  // ... validate blocker artifact ...
  return;
}
```

This caused two problems:

1. JUnit reported the test as **passed** instead of **skipped**, hiding the
   fact that the real proof path never ran.
2. On macOS without Xvfb, the test could attempt to pop up a real
   `JFileChooser` dialog.

### After (fixed)

Both Save menu proof tests use JUnit `Assume` guards:

**`StageIdeSaveMenuDoClickToWriteProofTest`:**

```java
@Test(timeout = 60000)
public void saveMenuDoClickApprovesChooserAndWritesProjectFile() throws Exception {
  assumeTrue("Requires non-headless AWT display",
      SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable());
  // ... rest of test ...
}
```

**`StageIdeSaveMenuE2EWriteProofTest`:**

```java
@Test(timeout = 90000)
public void saveMenuE2ETriggersWriteFromFileMenuAction() throws Exception {
  assumeFalse("requires Xvfb or another headful AWT display",
      GraphicsEnvironment.isHeadless());
  // ... rest of test ...
}
```

When the assumption fails, JUnit marks the test as **skipped** (not passed or
failed). CI dashboards correctly show the test was not exercised, and no
`JFileChooser` dialog is created.

### Dedicated blocker validation

The inline blocker artifact validation that was previously in the `if/return`
block is covered by the dedicated test method
`blockerArtifactReportsNoAvailableNonHeadlessAwtDisplay` in the same class.
Removing the inline copy does not reduce assertion coverage.

## macOS screen menu bar property override (issues #500, #502)

### Problem (issue #500)

On macOS, the system menu bar is rendered natively by the OS outside the
JFrame window. AWT Robot screen-coordinate clicks that target the JMenuBar
inside the JFrame hit empty space because macOS relocates the menu bar to the
top of the screen. Tests that rely on Robot mouse events to open File → Save
through a JMenuBar therefore fail on macOS even when a graphical display is
available.

The `doClick()` and `fireActionPerformed()` paths used by other Save tests
(e.g., `StageIdeSaveMenuDoClickToWriteProofTest`) are unaffected because they
bypass screen coordinates entirely.

### Original workaround (issue #500)

Both Robot menu test classes added an `assumeFalse(SystemUtilities.isMac())`
guard that skipped the test entirely on macOS. This meant the Robot menu
proof path was never exercised on macOS, even when a graphical display was
available.

### Fix (issue #502): force menu into JFrame

Instead of skipping on macOS, both test classes now force the JMenuBar to
stay inside the JFrame by setting the `apple.laf.useScreenMenuBar` system
property to `"false"`. This tells the macOS Swing look-and-feel to render
the menu bar inside the JFrame window rather than in the native macOS menu
bar, allowing Robot screen-coordinate clicks to work.

Each test class uses a `@Before`/`@After` pair to capture the original
property value, set it to `"false"`, and restore the original value after
the test:

```java
private static final String SCREEN_MENU_BAR_PROPERTY = "apple.laf.useScreenMenuBar";
private String previousScreenMenuBar;

@Before
public void captureProperties() {
  previousScreenMenuBar = System.getProperty(SCREEN_MENU_BAR_PROPERTY);
  System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false");
  // ... other property captures ...
}

@After
public void restorePropertiesAndActiveApplication() throws Exception {
  restoreProperty(SCREEN_MENU_BAR_PROPERTY, previousScreenMenuBar);
  // ... other property restores ...
}
```

#### Defensive re-set after `ide.initialize()`

`Application.initialize()` (in `org.lgna.croquet.Application`) unconditionally
sets `apple.laf.useScreenMenuBar` to `"true"` on macOS. Because `StageIDE`
extends `Application`, the `ide.initialize(new String[0])` call inside each
test method re-enables the native menu bar, overriding the `@Before` setup.

To handle this, both test classes add a defensive re-set to `"false"`
immediately after `ide.initialize()` completes:

```java
StageIDE ide = new StageIDE(new CrashDetector(TestClass.class));
ide.initialize(new String[0]);
// Application.initialize() sets apple.laf.useScreenMenuBar=true on Mac;
// force it back to false so the menu stays inside the JFrame for Robot.
System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false");
```

This ensures the property is `"false"` before any JFrame or JMenuBar creation
regardless of platform. On non-Mac platforms, the property has no effect and
the defensive re-set is harmless.

#### Property restore via `restoreProperty`

Both classes use the existing `restoreProperty` helper to restore the original
value in `@After`:

```java
private static void restoreProperty(String name, String value) {
  if (value == null) {
    System.clearProperty(name);
  } else {
    System.setProperty(name, value);
  }
}
```

If the property was not set before the test, it is cleared (not set to an
empty string). If it had a prior value (including `"true"`), that value is
restored.

## API reference

### `extractIntField(String json, String fieldName) → int`

| Parameter | Type | Description |
| --- | --- | --- |
| `json` | `String` | The JSON evidence artifact content. |
| `fieldName` | `String` | The JSON field name to extract (e.g., `"renderTargetWidth"`). |
| **Returns** | `int` | The non-negative integer value of the field. |
| **Throws** | `AssertionError` | If the field is not found in the JSON string. |

### `SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable() → boolean`

Returns `true` when a non-headless AWT display is available for Swing
component realization. Used as the `assumeTrue` predicate.

### `SCREEN_MENU_BAR_PROPERTY` constant

```java
private static final String SCREEN_MENU_BAR_PROPERTY = "apple.laf.useScreenMenuBar";
```

Defined in both `JMenuBarRobotClickSaveProofTest` and
`RobotSaveMenuDialogWriteReadbackProofTest`. Used in `@Before` to capture and
override, in `@After` to restore, and as a defensive re-set after
`ide.initialize()`.

### `GraphicsEnvironment.isHeadless() → boolean`

Standard JDK API. Returns `true` in CI headless environments. Used as the
`assumeFalse` predicate.

## Configuration

No new configuration is required. The tests use the same JVM properties and
Maven profiles as before:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=EatmeDesktopRunExecutionEvidenceTest,StageIdeSaveMenuE2EWriteProofTest,StageIdeSaveMenuDoClickToWriteProofTest,JMenuBarRobotClickSaveProofTest,RobotSaveMenuDialogWriteReadbackProofTest \
  test
```

On macOS without Xvfb, the two Save menu tests skip (headless guard), the two
Robot menu tests skip (headless guard), and the render-target test passes with
platform-reported dimensions.

On macOS with a graphical display, all four Save/Robot menu tests pass
(display available, `apple.laf.useScreenMenuBar` forced to `"false"` keeps the
menu bar inside the JFrame for Robot coordinate clicks), and the render-target
test passes.

On Linux CI (headless), all four Save/Robot tests skip (headless guard) and the
render-target evidence test passes with zero-dimension evidence.

On Xvfb or a graphical display (Linux/Windows), all five tests exercise the
full proof path.

## Validation commands

Run all five affected test classes:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=EatmeDesktopRunExecutionEvidenceTest,StageIdeSaveMenuE2EWriteProofTest,StageIdeSaveMenuDoClickToWriteProofTest,JMenuBarRobotClickSaveProofTest,RobotSaveMenuDialogWriteReadbackProofTest \
  test -q
```

Expected result:

| Environment | `EatmeDesktopRunExecution...` | `StageIdeSaveMenuDoClick...` | `StageIdeSaveMenuE2EWrite...` | `JMenuBarRobotClickSave...` | `RobotSaveMenuDialogWrite...` (Robot) | `RobotSaveMenuDialogWrite...` (evidence) |
| --- | --- | --- | --- | --- | --- | --- |
| Linux headless CI | **Pass** (dimensions = 0) | **Skip** (headless) | **Skip** (headless) | **Skip** (headless) | **Skip** (headless) | **Pass** |
| macOS without display | **Pass** (dimensions ≥ 0) | **Skip** (headless) | **Skip** (headless) | **Skip** (headless) | **Skip** (headless) | **Pass** |
| macOS with display | **Pass** (dimensions ≥ 0) | **Pass** | **Pass** | **Pass** (menu in JFrame) | **Pass** (menu in JFrame) | **Pass** |
| Xvfb or graphical (Linux/Windows) | **Pass** (dimensions > 0) | **Pass** | **Pass** | **Pass** | **Pass** | **Pass** |

## Examples

### Headless CI output (Linux)

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 4
```

The two Save menu proof tests and two Robot menu proof tests report as skipped
(all four due to headless guard). The render-target evidence test passes with
`renderTargetWidth: 0, renderTargetHeight: 0`. The five evidence-contract
methods in `RobotSaveMenuDialogWriteReadbackProofTest` pass (no Robot
interaction needed).

### macOS headless output

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 4
```

Same as Linux headless: all four Save/Robot tests skip due to headless guard.

### macOS with display output

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
```

All tests pass. The two Robot menu proof tests force `apple.laf.useScreenMenuBar`
to `"false"`, keeping the JMenuBar inside the JFrame where Robot screen-coordinate
clicks work. The two Save menu proof tests pass (display available,
`doClick`/`fireAction` paths unaffected). The render-target evidence test passes
with platform-reported dimensions. The five evidence-contract methods pass.

## Compatibility rules

1. **Never hardcode platform-specific dimension values** in render-target
   evidence assertions. Always extract the actual value and assert a range.

2. **Always use JUnit `Assume` guards** (`assumeTrue` / `assumeFalse`) to skip
   tests that require a graphical display. Do not use `if/return` patterns
   that silently pass.

3. **Use `apple.laf.useScreenMenuBar=false` to force the menu bar into the
   JFrame for Robot screen-coordinate menu tests on macOS.** Capture the
   original value in `@Before`, set to `"false"`, and restore in `@After`.
   Add a defensive re-set to `"false"` after `ide.initialize()` because
   `Application.initialize()` unconditionally sets the property to `"true"`
   on macOS.

4. **Keep blocker artifact validation in dedicated test methods.** Do not
   duplicate it inside headless-skip guard blocks.

5. The `extractIntField` helper is scoped to test-internal JSON strings. It
   does not parse arbitrary user input and must not be promoted to production
   code.

6. The `assumeTrue` / `assumeFalse` messages must describe the required
   environment so CI reports are actionable.

## Non-claims

These contracts do not prove:

- Visible rendering correctness on any platform.
- That macOS AWT dimensions match specific expected values.
- That `JFileChooser` or Swing UI elements render correctly on macOS.
- That Save, Save As, or Export operations complete on macOS.
- Full desktop automation, installer validation, Sims integration, or
  first-lesson completion.
- That the render-target evidence artifact schema is correct (covered by
  other tests in `EatmeDesktopRunExecutionEvidenceTest`).
