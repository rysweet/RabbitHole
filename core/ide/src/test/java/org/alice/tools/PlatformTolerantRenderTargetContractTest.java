package org.alice.tools;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Contract tests for platform-tolerant render target dimension assertions.
 *
 * <p>Issue #496: On Mac, an unrealized JPanel can report non-zero dimensions
 * (e.g., width=24) unlike Linux headless where it reports 0. The pixel
 * observation test assertions must accept any non-negative dimension and
 * only assert the {@code render_target_has_no_positive_size} blocker when
 * both dimensions are actually zero.
 *
 * <p>All tests fail initially because they depend on
 * {@code extractIntField(String, String)} which does not yet exist on
 * {@link EatmeDesktopRunExecutionEvidenceTest}. After implementation, they pass.
 */
public class PlatformTolerantRenderTargetContractTest {

  @Test
  public void zeroDimensionsAreAccepted() {
    String json = buildPixelObservationJson(0, 0, true);
    int width = invokeExtractIntField(json, "renderTargetWidth");
    int height = invokeExtractIntField(json, "renderTargetHeight");
    assertTrue("width >= 0", width >= 0);
    assertTrue("height >= 0", height >= 0);
  }

  @Test
  public void macPositiveDimensionsAreAccepted() {
    String json = buildPixelObservationJson(24, 18, false);
    int width = invokeExtractIntField(json, "renderTargetWidth");
    int height = invokeExtractIntField(json, "renderTargetHeight");
    assertTrue("Mac width 24 must be >= 0", width >= 0);
    assertTrue("Mac height 18 must be >= 0", height >= 0);
    assertEquals("extracted width", 24, width);
    assertEquals("extracted height", 18, height);
  }

  @Test
  public void noPozSizeBlockerOmittedWhenDimensionsPositive() {
    String json = buildPixelObservationJson(24, 18, false);
    int width = invokeExtractIntField(json, "renderTargetWidth");
    int height = invokeExtractIntField(json, "renderTargetHeight");
    assertTrue("width should be positive", width > 0);
    assertTrue("height should be positive", height > 0);
    assertFalse(
        "render_target_has_no_positive_size must NOT appear when dimensions are positive",
        json.contains("\"render_target_has_no_positive_size\""));
  }

  @Test
  public void noPozSizeBlockerPresentWhenDimensionsZero() {
    String json = buildPixelObservationJson(0, 0, true);
    int width = invokeExtractIntField(json, "renderTargetWidth");
    int height = invokeExtractIntField(json, "renderTargetHeight");
    assertEquals("width should be 0", 0, width);
    assertEquals("height should be 0", 0, height);
    assertTrue(
        "render_target_has_no_positive_size must appear when both dimensions are 0",
        json.contains("\"render_target_has_no_positive_size\""));
  }

  @Test
  public void sizeDetailConditionalOnZeroDimensions() {
    String positiveJson = buildPixelObservationJson(24, 18, false);
    int width = invokeExtractIntField(positiveJson, "renderTargetWidth");
    int height = invokeExtractIntField(positiveJson, "renderTargetHeight");
    assertTrue("width should be positive", width > 0);
    assertTrue("height should be positive", height > 0);
    assertFalse(
        "observed renderTargetWidth=0 detail must NOT appear when width is positive",
        positiveJson.contains("\"observed\": \"renderTargetWidth=0, renderTargetHeight=0\""));
  }

  private static String buildPixelObservationJson(
      int width, int height, boolean includeNoPosSizeBlocker) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"schema_version\": \"eatme.alice-desktop-run-pixel-observation/v1\",\n");
    sb.append("  \"status\": \"blocked\",\n");
    sb.append("  \"source\": \"desktop_run_render_target_attachment\",\n");
    sb.append("  \"component_state\": {\n");
    sb.append("    \"renderTargetDisplayable\": false,\n");
    sb.append("    \"renderTargetShowing\": false,\n");
    sb.append("    \"renderTargetWidth\": ").append(width).append(",\n");
    sb.append("    \"renderTargetHeight\": ").append(height).append("\n");
    sb.append("  },\n");
    sb.append("  \"blocker\": [\n");
    sb.append("    \"render_target_not_displayable\",\n");
    sb.append("    \"render_target_not_showing\"");
    if (includeNoPosSizeBlocker) {
      sb.append(",\n    \"render_target_has_no_positive_size\"");
    }
    sb.append("\n  ],\n");
    sb.append("  \"details\": [\n");
    sb.append("    { \"observed\": \"renderTargetDisplayable=false\",");
    sb.append(" \"required\": \"renderTargetDisplayable=true\" },\n");
    sb.append("    { \"observed\": \"renderTargetShowing=false\",");
    sb.append(" \"required\": \"renderTargetShowing=true\" }");
    if (includeNoPosSizeBlocker) {
      sb.append(",\n    { \"observed\": \"renderTargetWidth=").append(width);
      sb.append(", renderTargetHeight=").append(height);
      sb.append("\", \"required\": \"renderTargetWidth>0 and renderTargetHeight>0\" }");
    }
    sb.append("\n  ]\n");
    sb.append("}");
    return sb.toString();
  }

  private static int invokeExtractIntField(String json, String fieldName) {
    try {
      Method method = EatmeDesktopRunExecutionEvidenceTest.class
          .getDeclaredMethod("extractIntField", String.class, String.class);
      method.setAccessible(true);
      return (int) method.invoke(null, json, fieldName);
    } catch (NoSuchMethodException e) {
      fail("extractIntField(String, String) must be added to "
          + "EatmeDesktopRunExecutionEvidenceTest");
      return -999;
    } catch (Exception e) {
      fail("extractIntField invocation failed: " + e);
      return -999;
    }
  }
}
