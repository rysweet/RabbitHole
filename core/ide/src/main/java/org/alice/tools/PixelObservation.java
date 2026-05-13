package org.alice.tools;

import java.awt.Rectangle;
import java.util.List;

/**
 * Carries pixel observation results and provides JSON fragment factories.
 * Promoted from a private inner class in EatmeDesktopRunExecutionEvidence.
 *
 * <p>The {@link #observed} factory takes String parameters for component class name
 * and component name (not {@code java.awt.Component}), breaking the AWT dependency
 * from the result carrier.</p>
 */
final class PixelObservation {
  final String status;
  final String claim;
  final String detailJson;
  final List<BlockerDetail> blockers;
  final String exceptionType;

  private PixelObservation(
      String status,
      String claim,
      String detailJson,
      List<BlockerDetail> blockers,
      String exceptionType) {
    this.status = status;
    this.claim = claim;
    this.detailJson = detailJson;
    this.blockers = blockers;
    this.exceptionType = exceptionType;
  }

  boolean isObserved() {
    return "observed".equals(status);
  }

  static PixelObservation blocked(List<BlockerDetail> blockers, String exceptionType) {
    return new PixelObservation(
        "blocked",
        "No desktop pixel was sampled.",
        "  \"blocker\": {\n"
            + "    \"reason\": \"A desktop screenshot requires a non-headless graphics environment, "
            + "a showing Run render target, positive component size, and screen-capture access.\",\n"
            + "    \"codes\": " + blockerCodesJson(blockers) + ",\n"
            + "    \"details\": " + blockerDetailsJson(blockers) + ",\n"
            + "    \"exceptionType\": \"" + EatmeRunWindowEvidence.escapeJson(exceptionType) + "\"\n"
            + "  },\n",
        blockers,
        exceptionType);
  }

  static String blockerCodesJson(List<BlockerDetail> blockers) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < blockers.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append('"').append(EatmeRunWindowEvidence.escapeJson(blockers.get(i).code)).append('"');
    }
    builder.append(']');
    return builder.toString();
  }

  static String blockerDetailsJson(List<BlockerDetail> blockers) {
    StringBuilder builder = new StringBuilder(blockers.size() * 160 + 16);
    builder.append("[\n");
    for (int i = 0; i < blockers.size(); i++) {
      BlockerDetail blocker = blockers.get(i);
      if (i > 0) {
        builder.append(",\n");
      }
      builder.append("      {\n")
          .append("        \"code\": \"").append(EatmeRunWindowEvidence.escapeJson(blocker.code)).append("\",\n")
          .append("        \"observed\": \"").append(EatmeRunWindowEvidence.escapeJson(blocker.observed)).append("\",\n")
          .append("        \"required\": \"").append(EatmeRunWindowEvidence.escapeJson(blocker.required)).append("\"\n")
          .append("      }");
    }
    builder.append("\n    ]");
    return builder.toString();
  }

  static PixelObservation observed(
      String captureRole,
      String componentClassName,
      String componentName,
      String screenshot,
      Rectangle captureArea,
      int screenshotWidth,
      int screenshotHeight,
      int sampleX,
      int sampleY,
      int argb) {
    return new PixelObservation(
        "observed",
        "A desktop screenshot of the Run render target area was captured and its center pixel was sampled.",
        "  \"captureTarget\": {\n"
            + "    \"role\": \"" + EatmeRunWindowEvidence.escapeJson(captureRole) + "\",\n"
            + "    \"componentClass\": \"" + EatmeRunWindowEvidence.escapeJson(componentClassName) + "\",\n"
            + "    \"componentName\": \"" + EatmeRunWindowEvidence.escapeJson(componentName) + "\"\n"
            + "  },\n"
            + "  \"screenshot\": {\n"
            + "    \"file\": \"" + EatmeRunWindowEvidence.escapeJson(screenshot) + "\",\n"
            + "    \"width\": " + screenshotWidth + ",\n"
            + "    \"height\": " + screenshotHeight + "\n"
            + "  },\n"
            + "  \"captureArea\": {\n"
            + "    \"coordinateSystem\": \"screen\",\n"
            + "    \"x\": " + captureArea.x + ",\n"
            + "    \"y\": " + captureArea.y + ",\n"
            + "    \"width\": " + captureArea.width + ",\n"
            + "    \"height\": " + captureArea.height + "\n"
            + "  },\n"
            + "  \"sample\": {\n"
            + "    \"coordinateSystem\": \"screenshot\",\n"
            + "    \"x\": " + sampleX + ",\n"
            + "    \"y\": " + sampleY + ",\n"
            + "    \"argb\": \"" + String.format("0x%08X", argb) + "\"\n"
            + "  },\n",
        List.of(),
        "");
  }
}
