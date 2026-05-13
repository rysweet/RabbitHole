package org.alice.tools;

/**
 * Immutable data record representing a single blocker preventing desktop pixel observation.
 * Promoted from a private inner class in EatmeDesktopRunExecutionEvidence.
 */
final class BlockerDetail {
  final String code;
  final String observed;
  final String required;

  BlockerDetail(String code, String observed, String required) {
    this.code = code;
    this.observed = observed;
    this.required = required;
  }
}
