package org.alice.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TDD contract tests for {@link EatmeScreenshotCapture}, which encapsulates
 * Robot screen capture and PNG writing. Preserves the headless gate.
 *
 * <p>Extracted from EatmeDesktopRunExecutionEvidence: observePixel,
 * observeComponentPixel, writePngAtomically, exceptionObserved.</p>
 */
public class EatmeScreenshotCaptureTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // --- exceptionObserved ---

  @Test
  public void exceptionObservedIncludesClassNameAndMessage() {
    String result = EatmeScreenshotCapture.exceptionObserved(
        new IllegalStateException("component not showing"));
    assertEquals("IllegalStateException: component not showing", result);
  }

  @Test
  public void exceptionObservedReturnsClassNameOnlyWhenMessageIsNull() {
    String result = EatmeScreenshotCapture.exceptionObserved(
        new NullPointerException());
    assertEquals("NullPointerException", result);
  }

  @Test
  public void exceptionObservedReturnsClassNameOnlyWhenMessageIsBlank() {
    String result = EatmeScreenshotCapture.exceptionObserved(
        new RuntimeException("   "));
    assertEquals("RuntimeException", result);
  }

  @Test
  public void exceptionObservedReturnsClassNameOnlyWhenMessageIsEmpty() {
    String result = EatmeScreenshotCapture.exceptionObserved(
        new RuntimeException(""));
    assertEquals("RuntimeException", result);
  }

  @Test
  public void exceptionObservedHandlesNestedExceptionClassName() {
    String result = EatmeScreenshotCapture.exceptionObserved(
        new java.awt.AWTException("Robot not available"));
    assertEquals("AWTException: Robot not available", result);
  }

  // --- writePngAtomically ---

  @Test
  public void writePngAtomicallyCreatesValidPngFile() throws Exception {
    Path target = temporaryFolder.getRoot().toPath().resolve("test.png");
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(5, 5, 0xFFFF0000);

    EatmeScreenshotCapture.writePngAtomically(target, image);

    assertTrue(Files.exists(target));
    assertTrue(Files.size(target) > 0);
  }

  @Test
  public void writePngAtomicallyProducesReadablePng() throws Exception {
    Path target = temporaryFolder.getRoot().toPath().resolve("readable.png");
    BufferedImage original = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
    original.setRGB(12, 12, 0xFF00FF00);

    EatmeScreenshotCapture.writePngAtomically(target, original);

    BufferedImage readBack = ImageIO.read(target.toFile());
    assertEquals(24, readBack.getWidth());
    assertEquals(24, readBack.getHeight());
  }

  @Test
  public void writePngAtomicallyCleansTempFile() throws Exception {
    Path target = temporaryFolder.getRoot().toPath().resolve("clean.png");
    BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);

    EatmeScreenshotCapture.writePngAtomically(target, image);

    assertFalse(Files.exists(target.resolveSibling("clean.png.tmp")));
  }

  @Test
  public void writePngAtomicallyOverwritesExistingFile() throws Exception {
    Path target = temporaryFolder.getRoot().toPath().resolve("overwrite.png");
    BufferedImage first = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
    BufferedImage second = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);

    EatmeScreenshotCapture.writePngAtomically(target, first);
    long firstSize = Files.size(target);

    EatmeScreenshotCapture.writePngAtomically(target, second);
    long secondSize = Files.size(target);

    assertTrue("Second PNG should differ in size", secondSize != firstSize);
  }

  // --- observePixel ---

  @Test
  public void observePixelReturnsBlockedForNonDisplayableComponents() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("blocked-pixel").toPath();
    JPanel renderTarget = new JPanel();
    JPanel renderPanel = new JPanel();

    PixelObservation observation = EatmeScreenshotCapture.observePixel(
        evidenceDir, renderTarget, renderPanel);

    assertEquals("blocked", observation.status);
    assertFalse(observation.isObserved());
    assertFalse(observation.blockers.isEmpty());
  }

  @Test
  public void observePixelSkipsRenderPanelWhenSameAsRenderTarget() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("same-component").toPath();
    JPanel sameComponent = new JPanel();

    PixelObservation observation = EatmeScreenshotCapture.observePixel(
        evidenceDir, sameComponent, sameComponent);

    assertEquals("blocked", observation.status);
    // No render_panel blockers since components are identical
    assertFalse(observation.blockers.stream().anyMatch(
        b -> b.code.startsWith("render_panel")));
  }

  @Test
  public void observePixelMergesBlockersFromBothComponents() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("merged").toPath();
    JPanel renderTarget = new JPanel();
    JPanel renderPanel = new JPanel();

    PixelObservation observation = EatmeScreenshotCapture.observePixel(
        evidenceDir, renderTarget, renderPanel);

    assertFalse(observation.blockers.isEmpty());
  }

  // --- observeComponentPixel ---

  @Test
  public void observeComponentPixelReturnsBlockedForNonDisplayable() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("non-displayable").toPath();
    JPanel panel = new JPanel();

    PixelObservation observation = EatmeScreenshotCapture.observeComponentPixel(
        evidenceDir, panel, "render_target", "renderTarget", "render_target_component");

    assertEquals("blocked", observation.status);
    assertTrue(observation.blockers.stream().anyMatch(
        b -> b.code.contains("not_displayable") || b.code.contains("headless")));
  }

  @Test
  public void observeComponentPixelUsesCorrectPrefixes() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("prefixes").toPath();
    JPanel panel = new JPanel();

    PixelObservation observation = EatmeScreenshotCapture.observeComponentPixel(
        evidenceDir, panel, "render_panel", "renderPanel", "render_panel_component");

    // Blockers should use the provided prefixes
    boolean hasPanelPrefix = observation.blockers.stream().anyMatch(
        b -> b.code.startsWith("render_panel") || b.code.startsWith("java_awt"));
    assertTrue("Expected blockers with render_panel or java_awt prefix", hasPanelPrefix);
  }
}
