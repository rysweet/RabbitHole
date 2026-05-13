package org.alice.tools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TDD contract tests for {@link BlockerDetail}, an immutable data record
 * promoted from the private inner class in EatmeDesktopRunExecutionEvidence.
 *
 * <p>These tests define the public contract: three package-private final fields
 * (code, observed, required) set via the constructor.</p>
 */
public class BlockerDetailTest {

  @Test
  public void constructorStoresCodeObservedRequired() {
    BlockerDetail detail = new BlockerDetail(
        "render_target_not_displayable",
        "renderTargetDisplayable=false",
        "renderTargetDisplayable=true");
    assertEquals("render_target_not_displayable", detail.code);
    assertEquals("renderTargetDisplayable=false", detail.observed);
    assertEquals("renderTargetDisplayable=true", detail.required);
  }

  @Test
  public void fieldsAreAccessibleFromSamePackage() {
    BlockerDetail detail = new BlockerDetail("java_awt_headless",
        "graphicsEnvironmentHeadless=true",
        "graphicsEnvironmentHeadless=false");
    assertEquals("java_awt_headless", detail.code);
    assertEquals("graphicsEnvironmentHeadless=true", detail.observed);
    assertEquals("graphicsEnvironmentHeadless=false", detail.required);
  }

  @Test
  public void handlesSpecialCharactersInAllFields() {
    BlockerDetail detail = new BlockerDetail(
        "code_with_\"quotes\"",
        "observed\nwith\nnewlines",
        "required\\with\\backslashes");
    assertEquals("code_with_\"quotes\"", detail.code);
    assertEquals("observed\nwith\nnewlines", detail.observed);
    assertEquals("required\\with\\backslashes", detail.required);
  }

  @Test
  public void handlesEmptyStrings() {
    BlockerDetail detail = new BlockerDetail("", "", "");
    assertEquals("", detail.code);
    assertEquals("", detail.observed);
    assertEquals("", detail.required);
  }

  @Test
  public void preservesLongDescriptiveValues() {
    BlockerDetail detail = new BlockerDetail(
        "render_target_screenshot_has_no_positive_size",
        "screenshotWidth=0, screenshotHeight=0",
        "screenshotWidth>0 and screenshotHeight>0");
    assertEquals("render_target_screenshot_has_no_positive_size", detail.code);
    assertEquals("screenshotWidth=0, screenshotHeight=0", detail.observed);
    assertEquals("screenshotWidth>0 and screenshotHeight>0", detail.required);
  }
}
