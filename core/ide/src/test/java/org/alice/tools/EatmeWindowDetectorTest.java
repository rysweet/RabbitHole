package org.alice.tools;

import org.junit.Test;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TDD contract tests for {@link EatmeWindowDetector}, which encapsulates
 * component readiness checks as pure functions with no I/O.
 *
 * <p>Extracted from EatmeDesktopRunExecutionEvidence: componentReadinessBlockers,
 * componentClassName, componentName, childComponentCount, firstNonBlank.</p>
 */
public class EatmeWindowDetectorTest {

  // --- componentReadinessBlockers ---

  @Test
  public void reportsNotDisplayableBlocker() {
    JPanel panel = new JPanel();
    List<BlockerDetail> blockers = EatmeWindowDetector.componentReadinessBlockers(
        panel, "render_target", "renderTarget");
    assertTrue(blockers.stream().anyMatch(
        b -> "render_target_not_displayable".equals(b.code)));
    assertTrue(blockers.stream().anyMatch(
        b -> "renderTargetDisplayable=false".equals(b.observed)));
    assertTrue(blockers.stream().anyMatch(
        b -> "renderTargetDisplayable=true".equals(b.required)));
  }

  @Test
  public void reportsNotShowingBlocker() {
    JPanel panel = new JPanel();
    List<BlockerDetail> blockers = EatmeWindowDetector.componentReadinessBlockers(
        panel, "render_target", "renderTarget");
    assertTrue(blockers.stream().anyMatch(
        b -> "render_target_not_showing".equals(b.code)));
    assertTrue(blockers.stream().anyMatch(
        b -> "renderTargetShowing=false".equals(b.observed)));
    assertTrue(blockers.stream().anyMatch(
        b -> "renderTargetShowing=true".equals(b.required)));
  }

  @Test
  public void reportsNoPositiveSizeBlocker() {
    JPanel panel = new JPanel();
    List<BlockerDetail> blockers = EatmeWindowDetector.componentReadinessBlockers(
        panel, "render_target", "renderTarget");
    assertTrue(blockers.stream().anyMatch(
        b -> "render_target_has_no_positive_size".equals(b.code)));
  }

  @Test
  public void sizeBlockerIncludesActualDimensions() {
    JPanel panel = new JPanel();
    List<BlockerDetail> blockers = EatmeWindowDetector.componentReadinessBlockers(
        panel, "render_target", "renderTarget");
    BlockerDetail sizeBlocker = blockers.stream()
        .filter(b -> b.code.contains("no_positive_size"))
        .findFirst()
        .orElse(null);
    assertNotNull(sizeBlocker);
    assertTrue(sizeBlocker.observed, sizeBlocker.observed.contains("renderTargetWidth="));
    assertTrue(sizeBlocker.observed, sizeBlocker.observed.contains("renderTargetHeight="));
    assertTrue(sizeBlocker.required, sizeBlocker.required.contains("renderTargetWidth>0"));
    assertTrue(sizeBlocker.required, sizeBlocker.required.contains("renderTargetHeight>0"));
  }

  @Test
  public void usesCorrectBlockerAndStatePrefixes() {
    JPanel panel = new JPanel();
    List<BlockerDetail> blockers = EatmeWindowDetector.componentReadinessBlockers(
        panel, "render_panel", "renderPanel");
    assertTrue(blockers.stream().anyMatch(
        b -> "render_panel_not_displayable".equals(b.code)));
    assertTrue(blockers.stream().anyMatch(
        b -> "renderPanelDisplayable=false".equals(b.observed)));
    assertTrue(blockers.stream().anyMatch(
        b -> "render_panel_not_showing".equals(b.code)));
    assertTrue(blockers.stream().anyMatch(
        b -> "renderPanelShowing=false".equals(b.observed)));
  }

  @Test
  public void returnsMultipleBlockersForNonReadyComponent() {
    JPanel panel = new JPanel();
    List<BlockerDetail> blockers = EatmeWindowDetector.componentReadinessBlockers(
        panel, "render_target", "renderTarget");
    // Not displayable, not showing, zero size = at least 3 blockers
    assertTrue("Expected at least 3 blockers but got " + blockers.size(),
        blockers.size() >= 3);
  }

  // --- componentClassName ---

  @Test
  public void componentClassNameReturnsFullyQualifiedClassName() {
    JPanel panel = new JPanel();
    assertEquals("javax.swing.JPanel",
        EatmeWindowDetector.componentClassName(panel));
  }

  @Test
  public void componentClassNameReturnsNonEmptyString() {
    JPanel panel = new JPanel();
    String className = EatmeWindowDetector.componentClassName(panel);
    assertNotNull(className);
    assertFalse(className.isEmpty());
  }

  // --- componentName ---

  @Test
  public void componentNameReturnsSetName() {
    JPanel panel = new JPanel();
    panel.setName("test-render-panel");
    assertEquals("test-render-panel",
        EatmeWindowDetector.componentName(panel));
  }

  @Test
  public void componentNameReturnsEmptyStringWhenNameIsNull() {
    JPanel panel = new JPanel();
    panel.setName(null);
    assertEquals("", EatmeWindowDetector.componentName(panel));
  }

  @Test
  public void componentNamePreservesSpecialCharacters() {
    JPanel panel = new JPanel();
    panel.setName("render \"target\"\ncomponent");
    assertEquals("render \"target\"\ncomponent",
        EatmeWindowDetector.componentName(panel));
  }

  // --- childComponentCount ---

  @Test
  public void childComponentCountReturnsCountForContainer() {
    JPanel parent = new JPanel(new BorderLayout());
    parent.add(new JPanel(), BorderLayout.CENTER);
    parent.add(new JPanel(), BorderLayout.NORTH);
    assertEquals(2, EatmeWindowDetector.childComponentCount(parent));
  }

  @Test
  public void childComponentCountReturnsZeroForEmptyContainer() {
    JPanel panel = new JPanel();
    assertEquals(0, EatmeWindowDetector.childComponentCount(panel));
  }

  // --- firstNonBlank ---

  @Test
  public void firstNonBlankReturnsFirstWhenNonBlank() {
    assertEquals("first",
        EatmeWindowDetector.firstNonBlank("first", "second"));
  }

  @Test
  public void firstNonBlankReturnsSecondWhenFirstIsNull() {
    assertEquals("second",
        EatmeWindowDetector.firstNonBlank(null, "second"));
  }

  @Test
  public void firstNonBlankReturnsSecondWhenFirstIsBlank() {
    assertEquals("second",
        EatmeWindowDetector.firstNonBlank("   ", "second"));
  }

  @Test
  public void firstNonBlankReturnsSecondWhenFirstIsEmpty() {
    assertEquals("second",
        EatmeWindowDetector.firstNonBlank("", "second"));
  }

  @Test
  public void firstNonBlankReturnsEmptyWhenBothNull() {
    assertEquals("",
        EatmeWindowDetector.firstNonBlank(null, null));
  }

  @Test
  public void firstNonBlankReturnsEmptyWhenBothBlank() {
    assertEquals("",
        EatmeWindowDetector.firstNonBlank("", "  "));
  }

  @Test
  public void firstNonBlankReturnsFirstPreferentially() {
    assertEquals("AWTException",
        EatmeWindowDetector.firstNonBlank("AWTException", "IOException"));
  }
}
