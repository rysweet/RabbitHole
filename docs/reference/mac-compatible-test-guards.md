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
- [macOS native-menu-bar skip guard](#macos-native-menu-bar-skip-guard-issue-500)
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

All contracts are test-only changes. No production code is modified.

## Artifact inventory

| File | Purpose |
| --- | --- |
| `core/ide/src/test/java/org/alice/tools/EatmeDesktopRunExecutionEvidenceTest.java` | Render-target evidence assertions. Contains the `extractIntField` helper and platform-tolerant `>= 0` assertions. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/StageIdeSaveMenuDoClickToWriteProofTest.java` | Save menu do-click proof. Uses `assumeTrue` for headless-skip. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/StageIdeSaveMenuE2EWriteProofTest.java` | Save menu E2E proof. Uses `assumeFalse(GraphicsEnvironment.isHeadless())` for headless-skip. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/JMenuBarRobotClickSaveProofTest.java` | Robot JMenuBar click proof. Uses `assumeFalse(SystemUtilities.isMac())` to skip on macOS where the native menu bar prevents Robot screen-coordinate clicks. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java` | Robot Save menu dialog write/readback proof. Uses `assumeFalse(SystemUtilities.isMac())` in the Robot-driven test method only; the five evidence-contract test methods remain unguarded. |

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

### `SystemUtilities.isMac() → boolean`

Returns `true` when `os.name` starts with `"Mac"`. Located in
`edu.cmu.cs.dennisc.java.lang.SystemUtilities` (`core/util`). Used as the
`assumeFalse` predicate for Robot screen-coordinate menu tests that cannot
work with the macOS native menu bar.

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
Robot menu tests skip (`isMac` guard — headless fires first but `isMac` would
also skip them), and the render-target test passes with platform-reported
dimensions.

On macOS with a graphical display, the two Save menu tests pass (display
available), the two Robot menu tests still skip (`isMac` — the native menu bar
prevents Robot screen-coordinate clicks regardless of display availability), and
the render-target test passes.

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
| macOS with display | **Pass** (dimensions ≥ 0) | **Pass** | **Pass** | **Skip** (isMac) | **Skip** (isMac) | **Pass** |
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
The `isMac` guard is not reached because the headless guard fires first.

### macOS with display output

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 2
```

The two Save menu proof tests pass (display available, `doClick`/`fireAction`
paths unaffected by native menu bar). The two Robot menu proof tests skip
(`isMac` — the native menu bar prevents Robot screen-coordinate clicks). The
render-target evidence test passes with platform-reported dimensions (e.g.,
`renderTargetWidth: 24`). The five evidence-contract methods pass.

## macOS native-menu-bar skip guard (issue #500)

### Problem

On macOS, the system menu bar is rendered natively by the OS outside the
JFrame window. AWT Robot screen-coordinate clicks that target the JMenuBar
inside the JFrame hit empty space because macOS relocates the menu bar to the
top of the screen. Tests that rely on Robot mouse events to open File → Save
through a JMenuBar therefore fail on macOS even when a graphical display is
available.

The `doClick()` and `fireActionPerformed()` paths used by other Save tests
(e.g., `StageIdeSaveMenuDoClickToWriteProofTest`) are unaffected because they
bypass screen coordinates entirely.

### Guard

Both Robot menu test classes add an `assumeFalse` guard that checks
`SystemUtilities.isMac()` from `edu.cmu.cs.dennisc.java.lang.SystemUtilities`.
The guard runs after the existing headless-display check so that CI reports
distinguish between "skipped because headless" and "skipped because macOS
native menu bar":

**`JMenuBarRobotClickSaveProofTest`:**

```java
assumeFalse("requires Xvfb or another headful AWT display", GraphicsEnvironment.isHeadless());
assumeFalse("macOS uses a native menu bar outside the JFrame; Robot screen-coordinate menu clicks miss",
    SystemUtilities.isMac());
```

**`RobotSaveMenuDialogWriteReadbackProofTest`:**

The guard is placed inside the Robot-driven test method
`robotFileSaveApprovesChooserWritesReadableMarkedProjectOrWritesBlocker()`
only. The five companion evidence-contract test methods
(`evidenceArtifactReportsBlockerForHeadlessAwt`, etc.) remain unguarded
because they validate artifact schemas, not Robot UI interactions:

```java
assumeFalse("macOS uses a native menu bar outside the JFrame; Robot screen-coordinate menu clicks miss",
    SystemUtilities.isMac());
```

### `SystemUtilities.isMac()` API

| Method | Class | Returns |
| --- | --- | --- |
| `isMac()` | `edu.cmu.cs.dennisc.java.lang.SystemUtilities` | `true` when `os.name` starts with `"Mac"`. |

The method is already used throughout production code (`FileMenuModel`,
`CopyOperation`, `KeyEventUtilities`, etc.) and is part of `core/util`.

### Expected behavior

| Environment | `JMenuBarRobotClickSaveProofTest` | `RobotSaveMenuDialogWriteReadbackProofTest` (Robot method) | `RobotSaveMenuDialogWriteReadbackProofTest` (evidence methods) |
| --- | --- | --- | --- |
| Linux headless CI | **Skip** (headless) | **Skip** (headless) | **Pass** |
| macOS without Xvfb | **Skip** (isMac) | **Skip** (isMac) | **Pass** |
| macOS with Xvfb | **Skip** (isMac) | **Skip** (isMac) | **Pass** |
| Linux Xvfb or graphical | **Pass** | **Pass** | **Pass** |
| Windows graphical | **Pass** | **Pass** | **Pass** |

The macOS skip applies regardless of whether a graphical display is available
because the native menu bar issue is architectural, not display-dependent.

## Compatibility rules

1. **Never hardcode platform-specific dimension values** in render-target
   evidence assertions. Always extract the actual value and assert a range.

2. **Always use JUnit `Assume` guards** (`assumeTrue` / `assumeFalse`) to skip
   tests that require a graphical display. Do not use `if/return` patterns
   that silently pass.

3. **Use `SystemUtilities.isMac()` to skip Robot screen-coordinate menu
   tests on macOS.** The native menu bar is not inside the JFrame, so Robot
   clicks at JMenuBar coordinates miss. Guard only the Robot-driven methods;
   leave evidence-contract and programmatic-dispatch tests unguarded.

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
- That Robot menu clicks work on macOS (they are explicitly skipped).
- Full desktop automation, installer validation, Sims integration, or
  first-lesson completion.
- That the render-target evidence artifact schema is correct (covered by
  other tests in `EatmeDesktopRunExecutionEvidenceTest`).
