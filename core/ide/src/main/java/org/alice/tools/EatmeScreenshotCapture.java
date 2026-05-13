package org.alice.tools;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Robot screen capture and PNG writing for desktop Run execution evidence.
 * Preserves the headless gate before attempting any Robot operations.
 */
final class EatmeScreenshotCapture {

  static final String DESKTOP_RUN_RENDER_TARGET_SCREENSHOT = "desktop-run-render-target.png";

  private EatmeScreenshotCapture() {
  }

  static String exceptionObserved(Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null || message.isBlank()) {
      return throwable.getClass().getSimpleName();
    }
    return throwable.getClass().getSimpleName() + ": " + message;
  }

  static void writePngAtomically(Path target, BufferedImage image) throws IOException {
    Path temp = target.resolveSibling(target.getFileName() + ".tmp");
    if (!ImageIO.write(image, "png", temp.toFile())) {
      throw new IOException("PNG writer unavailable: " + target);
    }
    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  static PixelObservation observePixel(
      Path evidenceDir,
      Component renderTargetComponent,
      Component renderPanelComponent) {
    if (GraphicsEnvironment.isHeadless()) {
      List<BlockerDetail> blockers = new ArrayList<>();
      blockers.add(new BlockerDetail(
          "java_awt_headless",
          "graphicsEnvironmentHeadless=true",
          "graphicsEnvironmentHeadless=false"));
      blockers.addAll(EatmeWindowDetector.componentReadinessBlockers(
          renderTargetComponent, "render_target", "renderTarget"));
      if (renderTargetComponent != renderPanelComponent) {
        blockers.addAll(EatmeWindowDetector.componentReadinessBlockers(
            renderPanelComponent, "render_panel", "renderPanel"));
      }
      return PixelObservation.blocked(blockers, "");
    }

    PixelObservation renderTargetObservation = observeComponentPixel(
        evidenceDir,
        renderTargetComponent,
        "render_target",
        "renderTarget",
        "render_target_component");
    if (renderTargetObservation.isObserved() || renderTargetComponent == renderPanelComponent) {
      return renderTargetObservation;
    }

    PixelObservation renderPanelObservation = observeComponentPixel(
        evidenceDir,
        renderPanelComponent,
        "render_panel",
        "renderPanel",
        "render_panel_component");
    if (renderPanelObservation.isObserved()) {
      return renderPanelObservation;
    }

    List<BlockerDetail> blockers = new ArrayList<>(renderTargetObservation.blockers);
    blockers.addAll(renderPanelObservation.blockers);
    return PixelObservation.blocked(blockers, EatmeWindowDetector.firstNonBlank(
        renderTargetObservation.exceptionType,
        renderPanelObservation.exceptionType));
  }

  static PixelObservation observeComponentPixel(
      Path evidenceDir,
      Component component,
      String blockerPrefix,
      String statePrefix,
      String captureRole) {
    List<BlockerDetail> blockers = EatmeWindowDetector.componentReadinessBlockers(
        component, blockerPrefix, statePrefix);
    Point screenLocation = null;
    if (blockers.isEmpty()) {
      try {
        screenLocation = component.getLocationOnScreen();
      } catch (IllegalComponentStateException ex) {
        blockers.add(new BlockerDetail(
            blockerPrefix + "_screen_location_unavailable",
            exceptionObserved(ex),
            captureRole + " screen location available"));
      } catch (SecurityException ex) {
        blockers.add(new BlockerDetail(
            blockerPrefix + "_screen_location_denied",
            exceptionObserved(ex),
            "screen-location access permitted"));
      }
    }

    if (!blockers.isEmpty()) {
      return PixelObservation.blocked(blockers, "");
    }

    try {
      Rectangle captureArea = new Rectangle(
          screenLocation.x,
          screenLocation.y,
          component.getWidth(),
          component.getHeight());
      BufferedImage screenshot = new Robot().createScreenCapture(captureArea);
      if (screenshot.getWidth() <= 0 || screenshot.getHeight() <= 0) {
        return PixelObservation.blocked(List.of(new BlockerDetail(
            blockerPrefix + "_screenshot_has_no_positive_size",
            "screenshotWidth=" + screenshot.getWidth() + ", screenshotHeight=" + screenshot.getHeight(),
            "screenshotWidth>0 and screenshotHeight>0")), "");
      }
      Path screenshotPath = EatmeRunWindowEvidence.artifactPath(
          evidenceDir, DESKTOP_RUN_RENDER_TARGET_SCREENSHOT);
      writePngAtomically(screenshotPath, screenshot);
      int sampleX = screenshot.getWidth() / 2;
      int sampleY = screenshot.getHeight() / 2;
      int argb = screenshot.getRGB(sampleX, sampleY);
      return PixelObservation.observed(
          captureRole,
          EatmeWindowDetector.componentClassName(component),
          EatmeWindowDetector.componentName(component),
          DESKTOP_RUN_RENDER_TARGET_SCREENSHOT,
          captureArea,
          screenshot.getWidth(),
          screenshot.getHeight(),
          sampleX,
          sampleY,
          argb);
    } catch (AWTException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          "java_awt_robot_unavailable",
          exceptionObserved(ex),
          "java.awt.Robot screen capture available")), ex.getClass().getSimpleName());
    } catch (IOException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          blockerPrefix + "_screenshot_write_failed",
          exceptionObserved(ex),
          "desktop-run-render-target.png writable in evidence directory")), ex.getClass().getSimpleName());
    } catch (IllegalArgumentException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          blockerPrefix + "_screen_capture_area_invalid",
          exceptionObserved(ex),
          "valid positive screen capture rectangle")), ex.getClass().getSimpleName());
    } catch (SecurityException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          blockerPrefix + "_screen_capture_denied",
          exceptionObserved(ex),
          "screen-capture access permitted")), ex.getClass().getSimpleName());
    } catch (RuntimeException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          blockerPrefix + "_pixel_sample_failed",
          exceptionObserved(ex),
          "center pixel sample readable from captured image")), ex.getClass().getSimpleName());
    }
  }
}
