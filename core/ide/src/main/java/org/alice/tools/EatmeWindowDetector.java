package org.alice.tools;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Component readiness checks for desktop Run execution evidence.
 * Pure functions with no I/O.
 */
final class EatmeWindowDetector {

  private EatmeWindowDetector() {
  }

  static List<BlockerDetail> componentReadinessBlockers(
      Component component,
      String blockerPrefix,
      String statePrefix) {
    List<BlockerDetail> blockers = new ArrayList<>();
    boolean displayable = component.isDisplayable();
    boolean showing = component.isShowing();
    int width = component.getWidth();
    int height = component.getHeight();
    if (!displayable) {
      blockers.add(new BlockerDetail(
          blockerPrefix + "_not_displayable",
          statePrefix + "Displayable=false",
          statePrefix + "Displayable=true"));
    }
    if (!showing) {
      blockers.add(new BlockerDetail(
          blockerPrefix + "_not_showing",
          statePrefix + "Showing=false",
          statePrefix + "Showing=true"));
    }
    if (width <= 0 || height <= 0) {
      blockers.add(new BlockerDetail(
          blockerPrefix + "_has_no_positive_size",
          statePrefix + "Width=" + width + ", " + statePrefix + "Height=" + height,
          statePrefix + "Width>0 and " + statePrefix + "Height>0"));
    }
    return blockers;
  }

  static String componentClassName(Component component) {
    return component.getClass().getName();
  }

  static String componentName(Component component) {
    String name = component.getName();
    return name != null ? name : "";
  }

  static int childComponentCount(Component component) {
    if (component instanceof java.awt.Container container) {
      return container.getComponentCount();
    }
    return 0;
  }

  static String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    if (second != null && !second.isBlank()) {
      return second;
    }
    return "";
  }
}
