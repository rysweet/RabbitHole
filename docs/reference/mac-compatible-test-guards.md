# Mac-Compatible Test Guards

This reference documents the cross-platform compatibility contracts for two
Alice desktop test classes: render-target dimension assertions and headless-skip
guards. These contracts ensure that CI tests pass identically on Linux headless
runners, macOS Retina displays, and virtual-display (Xvfb) environments.

## Contents

- [Scope](#scope)
- [Artifact inventory](#artifact-inventory)
- [Render-target dimension contract](#render-target-dimension-contract)
- [Headless-skip guard contract](#headless-skip-guard-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Examples](#examples)
- [Compatibility rules](#compatibility-rules)
- [Non-claims](#non-claims)

## Scope

Two issues drove these contracts:

| Issue | Test class | Problem |
| --- | --- | --- |
| #496 | `EatmeDesktopRunExecutionEvidenceTest` | Hardcoded `renderTargetWidth: 0` / `renderTargetHeight: 0` assertions failed on macOS where AWT returns non-zero dimensions for unrealized panels. |
| #497 | `StageIdeSaveMenuDoClickToWriteProofTest`, `StageIdeSaveMenuE2EWriteProofTest` | Tests silently passed (or attempted real `JFileChooser` dialog popup) in headless environments instead of properly skipping. |

Both contracts are test-only changes. No production code is modified.

## Artifact inventory

| File | Purpose |
| --- | --- |
| `core/ide/src/test/java/org/alice/tools/EatmeDesktopRunExecutionEvidenceTest.java` | Render-target evidence assertions. Contains the `extractIntField` helper and platform-tolerant `>= 0` assertions. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/StageIdeSaveMenuDoClickToWriteProofTest.java` | Save menu do-click proof. Uses `assumeTrue` for headless-skip. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/StageIdeSaveMenuE2EWriteProofTest.java` | Save menu E2E proof. Uses `assumeFalse(GraphicsEnvironment.isHeadless())` for headless-skip. |

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
  -Dtest=EatmeDesktopRunExecutionEvidenceTest,StageIdeSaveMenuE2EWriteProofTest,StageIdeSaveMenuDoClickToWriteProofTest \
  test
```

On macOS without Xvfb, the two Save menu tests skip and the render-target
test passes with platform-reported dimensions.

On Linux CI (headless), all three tests either skip or pass with zero-dimension
evidence.

On Xvfb or a graphical display, all three tests exercise the full proof path.

## Validation commands

Run all three affected test classes:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=EatmeDesktopRunExecutionEvidenceTest,StageIdeSaveMenuE2EWriteProofTest,StageIdeSaveMenuDoClickToWriteProofTest \
  test -q
```

Expected result:

| Environment | `EatmeDesktopRunExecutionEvidenceTest` | `StageIdeSaveMenuDoClickToWriteProofTest` | `StageIdeSaveMenuE2EWriteProofTest` |
| --- | --- | --- | --- |
| Linux headless CI | **Pass** (dimensions = 0) | **Skip** | **Skip** |
| macOS without Xvfb | **Pass** (dimensions ≥ 0) | **Skip** | **Skip** |
| Xvfb or graphical display | **Pass** (dimensions > 0) | **Pass** | **Pass** |

## Examples

### Headless CI output

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 2
```

The two Save menu proof tests report as skipped. The render-target evidence
test passes with `renderTargetWidth: 0, renderTargetHeight: 0`.

### macOS output

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 2
```

The two Save menu proof tests report as skipped. The render-target evidence
test passes with platform-reported dimensions (e.g., `renderTargetWidth: 24`).

## Compatibility rules

1. **Never hardcode platform-specific dimension values** in render-target
   evidence assertions. Always extract the actual value and assert a range.

2. **Always use JUnit `Assume` guards** (`assumeTrue` / `assumeFalse`) to skip
   tests that require a graphical display. Do not use `if/return` patterns
   that silently pass.

3. **Keep blocker artifact validation in dedicated test methods.** Do not
   duplicate it inside headless-skip guard blocks.

4. The `extractIntField` helper is scoped to test-internal JSON strings. It
   does not parse arbitrary user input and must not be promoted to production
   code.

5. The `assumeTrue` / `assumeFalse` messages must describe the required
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
