package org.alice.tools;

import org.junit.Test;

import java.awt.Rectangle;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TDD contract tests for {@link PixelObservation}, a pixel result carrier
 * with JSON fragment factories, promoted from the private inner class in
 * EatmeDesktopRunExecutionEvidence.
 *
 * <p>Key design change: {@code observed()} takes String componentClassName and
 * componentName instead of Component, to break the AWT dependency cycle.</p>
 */
public class PixelObservationTest {

  // --- blocked() factory ---

  @Test
  public void blockedFactoryProducesBlockedStatus() {
    List<BlockerDetail> blockers = List.of(
        new BlockerDetail("java_awt_headless",
            "graphicsEnvironmentHeadless=true",
            "graphicsEnvironmentHeadless=false"));
    PixelObservation observation = PixelObservation.blocked(blockers, "");
    assertEquals("blocked", observation.status);
    assertFalse(observation.isObserved());
  }

  @Test
  public void blockedFactoryPreservesBlockerList() {
    List<BlockerDetail> blockers = List.of(
        new BlockerDetail("render_target_not_displayable",
            "renderTargetDisplayable=false", "renderTargetDisplayable=true"),
        new BlockerDetail("render_target_not_showing",
            "renderTargetShowing=false", "renderTargetShowing=true"));
    PixelObservation observation = PixelObservation.blocked(blockers, "AWTException");
    assertEquals(2, observation.blockers.size());
    assertEquals("render_target_not_displayable", observation.blockers.get(0).code);
    assertEquals("render_target_not_showing", observation.blockers.get(1).code);
  }

  @Test
  public void blockedFactoryStoresExceptionType() {
    PixelObservation observation = PixelObservation.blocked(
        List.of(new BlockerDetail("code", "obs", "req")), "AWTException");
    assertEquals("AWTException", observation.exceptionType);
  }

  @Test
  public void blockedFactoryProducesBlockedClaim() {
    PixelObservation observation = PixelObservation.blocked(
        List.of(new BlockerDetail("code", "obs", "req")), "");
    assertEquals("No desktop pixel was sampled.", observation.claim);
  }

  @Test
  public void blockedFactoryProducesBlockerDetailJson() {
    PixelObservation observation = PixelObservation.blocked(
        List.of(new BlockerDetail("java_awt_headless",
            "graphicsEnvironmentHeadless=true",
            "graphicsEnvironmentHeadless=false")),
        "");
    assertNotNull(observation.detailJson);
    assertTrue(observation.detailJson, observation.detailJson.contains("\"blocker\""));
    assertTrue(observation.detailJson, observation.detailJson.contains("\"reason\""));
    assertTrue(observation.detailJson, observation.detailJson.contains("\"codes\""));
    assertTrue(observation.detailJson, observation.detailJson.contains("\"java_awt_headless\""));
    assertTrue(observation.detailJson, observation.detailJson.contains("\"details\""));
  }

  @Test
  public void blockedFactoryIncludesExceptionTypeInDetailJson() {
    PixelObservation observation = PixelObservation.blocked(
        List.of(new BlockerDetail("robot_code", "obs", "req")), "AWTException");
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"exceptionType\": \"AWTException\""));
  }

  @Test
  public void blockedFactoryWithEmptyExceptionType() {
    PixelObservation observation = PixelObservation.blocked(
        List.of(new BlockerDetail("code", "obs", "req")), "");
    assertEquals("", observation.exceptionType);
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"exceptionType\": \"\""));
  }

  @Test
  public void blockedFactoryWithMultipleBlockers() {
    List<BlockerDetail> blockers = List.of(
        new BlockerDetail("render_target_not_displayable",
            "renderTargetDisplayable=false", "renderTargetDisplayable=true"),
        new BlockerDetail("render_target_not_showing",
            "renderTargetShowing=false", "renderTargetShowing=true"),
        new BlockerDetail("render_target_has_no_positive_size",
            "renderTargetWidth=0, renderTargetHeight=0",
            "renderTargetWidth>0 and renderTargetHeight>0"));
    PixelObservation observation = PixelObservation.blocked(blockers, "");
    assertEquals(3, observation.blockers.size());
    assertTrue(observation.detailJson,
        observation.detailJson.contains("render_target_not_displayable"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("render_target_not_showing"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("render_target_has_no_positive_size"));
  }

  // --- observed() factory (takes Strings, not Component) ---

  @Test
  public void observedFactoryProducesObservedStatus() {
    PixelObservation observation = PixelObservation.observed(
        "render_target_component",
        "javax.swing.JPanel",
        "myPanel",
        "desktop-run-render-target.png",
        new Rectangle(100, 200, 800, 600),
        800, 600,
        400, 300,
        0xFF0000FF);
    assertEquals("observed", observation.status);
    assertTrue(observation.isObserved());
  }

  @Test
  public void observedFactoryProducesObservedClaim() {
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name", "file.png",
        new Rectangle(0, 0, 10, 10), 10, 10, 5, 5, 0);
    assertTrue(observation.claim,
        observation.claim.contains("desktop screenshot"));
    assertTrue(observation.claim,
        observation.claim.contains("center pixel was sampled"));
  }

  @Test
  public void observedFactoryHasEmptyBlockerListAndExceptionType() {
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name", "file.png",
        new Rectangle(0, 0, 10, 10), 10, 10, 5, 5, 0);
    assertTrue(observation.blockers.isEmpty());
    assertEquals("", observation.exceptionType);
  }

  @Test
  public void observedFactoryProducesCaptureTargetInDetailJson() {
    PixelObservation observation = PixelObservation.observed(
        "render_target_component",
        "javax.swing.JPanel",
        "myPanel",
        "desktop-run-render-target.png",
        new Rectangle(10, 20, 100, 50),
        100, 50, 50, 25, 0xFF00FF00);
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"captureTarget\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"role\": \"render_target_component\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"componentClass\": \"javax.swing.JPanel\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"componentName\": \"myPanel\""));
  }

  @Test
  public void observedFactoryProducesScreenshotInDetailJson() {
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name",
        "desktop-run-render-target.png",
        new Rectangle(0, 0, 800, 600),
        800, 600, 400, 300, 0);
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"screenshot\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"file\": \"desktop-run-render-target.png\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"width\": 800"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"height\": 600"));
  }

  @Test
  public void observedFactoryProducesCaptureAreaInDetailJson() {
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name", "file.png",
        new Rectangle(50, 100, 200, 150),
        200, 150, 100, 75, 0);
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"captureArea\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"coordinateSystem\": \"screen\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"x\": 50"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"y\": 100"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"width\": 200"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"height\": 150"));
  }

  @Test
  public void observedFactoryProducesSampleInDetailJson() {
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name", "file.png",
        new Rectangle(0, 0, 100, 100),
        100, 100, 50, 50, 0xFF0000FF);
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"sample\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"coordinateSystem\": \"screenshot\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"x\": 50"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"y\": 50"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"argb\": \"0xFF0000FF\""));
  }

  @Test
  public void observedFactoryFormatsArgbAsHex() {
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name", "file.png",
        new Rectangle(0, 0, 10, 10),
        10, 10, 5, 5, 0x00000000);
    assertTrue(observation.detailJson,
        observation.detailJson.contains("\"argb\": \"0x00000000\""));
  }

  @Test
  public void observedFactoryEscapesSpecialCharactersInStrings() {
    PixelObservation observation = PixelObservation.observed(
        "role\nwith_newline",
        "class.with.\"quotes\"",
        "name\\backslash",
        "file.png",
        new Rectangle(0, 0, 10, 10), 10, 10, 5, 5, 0);
    assertTrue(observation.detailJson,
        observation.detailJson.contains("role\\nwith_newline"));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("class.with.\\\"quotes\\\""));
    assertTrue(observation.detailJson,
        observation.detailJson.contains("name\\\\backslash"));
  }

  // --- blockerCodesJson (moved from EatmeEvidenceWriter) ---

  @Test
  public void blockerCodesJsonProducesCorrectArray() {
    List<BlockerDetail> blockers = List.of(
        new BlockerDetail("code_a", "obs_a", "req_a"),
        new BlockerDetail("code_b", "obs_b", "req_b"));
    String result = PixelObservation.blockerCodesJson(blockers);
    assertEquals("[\"code_a\", \"code_b\"]", result);
  }

  @Test
  public void blockerCodesJsonProducesEmptyArrayForEmptyList() {
    String result = PixelObservation.blockerCodesJson(List.of());
    assertEquals("[]", result);
  }

  @Test
  public void blockerCodesJsonEscapesSpecialCharactersInCodes() {
    List<BlockerDetail> blockers = List.of(
        new BlockerDetail("code_with_\"quotes\"", "obs", "req"));
    String result = PixelObservation.blockerCodesJson(blockers);
    assertTrue(result, result.contains("code_with_\\\"quotes\\\""));
  }

  // --- blockerDetailsJson (moved from EatmeEvidenceWriter) ---

  @Test
  public void blockerDetailsJsonProducesCorrectJsonArray() {
    List<BlockerDetail> blockers = List.of(
        new BlockerDetail("render_target_not_displayable",
            "renderTargetDisplayable=false",
            "renderTargetDisplayable=true"));
    String result = PixelObservation.blockerDetailsJson(blockers);
    assertTrue(result, result.contains(
        "\"code\": \"render_target_not_displayable\""));
    assertTrue(result, result.contains(
        "\"observed\": \"renderTargetDisplayable=false\""));
    assertTrue(result, result.contains(
        "\"required\": \"renderTargetDisplayable=true\""));
  }

  @Test
  public void blockerDetailsJsonHandlesMultipleBlockers() {
    List<BlockerDetail> blockers = List.of(
        new BlockerDetail("code1", "obs1", "req1"),
        new BlockerDetail("code2", "obs2", "req2"));
    String result = PixelObservation.blockerDetailsJson(blockers);
    assertTrue(result, result.contains("\"code\": \"code1\""));
    assertTrue(result, result.contains("\"code\": \"code2\""));
    assertTrue(result, result.contains("\"observed\": \"obs1\""));
    assertTrue(result, result.contains("\"observed\": \"obs2\""));
  }

  @Test
  public void blockerDetailsJsonEscapesSpecialCharacters() {
    List<BlockerDetail> blockers = List.of(
        new BlockerDetail("code",
            "value\"with quotes",
            "required\nnewline"));
    String result = PixelObservation.blockerDetailsJson(blockers);
    assertTrue(result, result.contains("value\\\"with quotes"));
    assertTrue(result, result.contains("required\\nnewline"));
  }

  @Test
  public void blockerDetailsJsonProducesEmptyArrayForEmptyList() {
    String result = PixelObservation.blockerDetailsJson(List.of());
    assertTrue(result.startsWith("["));
    assertTrue(result.endsWith("]"));
  }
}
