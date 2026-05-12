package org.alice.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Proof that Alice's Run window can be detected via {@code Window.getWindows()}
 * after launching under Xvfb. This establishes the RabbitHole side of the
 * Run-window detection that eatme issue #246 needs.
 *
 * <p>The proof creates a synthetic {@link JFrame}, polls for it via the standard
 * AWT window enumeration API, records its identity hash and title in structured
 * JSON evidence, and verifies the evidence schema contract.
 *
 * <p>Headful tests (polling, hex-ID verification) are gated behind
 * {@code GraphicsEnvironment.isHeadless()} and skip cleanly on headless CI.
 * Schema and safety tests run everywhere.
 *
 * <p>This test does not claim rendering correctness, run execution, world
 * execution correctness, active rendering, save, grading, or full UI automation.
 *
 * @see EatmeRunWindowEvidence
 */
public class RunWindowDetectionProofTest {

  private static final String SCHEMA_VERSION = "eatme.alice-run-window-detection/v1";
  private static final String ARTIFACT_NAME = "run-window-detection.json";
  private static final String WINDOW_TITLE = "Run Window Detection Proof";
  private static final int POLL_INTERVAL_MS = 100;
  private static final int MAX_POLL_ITERATIONS = 100;
  private static final int MAX_WAIT_SECONDS = 10;
  private static final String[] DOES_NOT_CLAIM = {
      "active-rendering",
      "run-execution",
      "world-execution-correctness",
      "rendering-correctness",
      "save",
      "grading",
      "full-ui-automation"
  };

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // --- Headful proof tests (require Xvfb or native display) ---

  @Test
  public void detectsJFrameViaWindowGetWindowsAndWritesDetectionEvidence() throws Exception {
    assumeFalse("Requires a display (Xvfb or native)",
        GraphicsEnvironment.isHeadless());
    Path evidenceDir = temporaryFolder.newFolder("detection-evidence").toPath();

    JFrame frame = new JFrame(WINDOW_TITLE);
    try {
      frame.setSize(200, 200);
      frame.setVisible(true);

      DetectionResult result = pollForWindow(WINDOW_TITLE);

      assertNotNull("Window should be detected under display", result);
      assertEquals("detected", result.status);
      assertTrue("window_id should be hex format",
          result.windowId.startsWith("0x"));
      assertTrue("window_title should contain expected title",
          result.windowTitle.contains(WINDOW_TITLE));

      Path artifact = writeDetectionEvidence(evidenceDir, result);

      assertTrue(Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS));
      assertTrue(Files.size(artifact) > 0);
      String json = Files.readString(artifact);
      assertSuccessSchemaFields(json);
      assertAllFalseClaimBooleans(json);
      assertDoesNotClaimArray(json);
    } finally {
      frame.dispose();
    }
  }

  @Test
  public void windowIdIsHexFormattedIdentityHashCode() throws Exception {
    assumeFalse("Requires a display (Xvfb or native)",
        GraphicsEnvironment.isHeadless());
    String uniqueTitle = WINDOW_TITLE + " hex-" + System.nanoTime();
    JFrame frame = new JFrame(uniqueTitle);
    try {
      frame.setSize(100, 100);
      frame.setVisible(true);
      String expectedHex = "0x" + Integer.toHexString(System.identityHashCode(frame));

      DetectionResult result = pollForWindow(uniqueTitle);

      assertNotNull("Window should be detected", result);
      assertEquals("Window ID must be hex identity hash", expectedHex, result.windowId);
    } finally {
      frame.dispose();
    }
  }

  @Test
  public void writesNotDetectedEvidenceWhenWindowNotFoundWithinTimeout() throws Exception {
    assumeFalse("Requires a display (Xvfb or native)",
        GraphicsEnvironment.isHeadless());
    Path evidenceDir = temporaryFolder.newFolder("failure-evidence").toPath();

    DetectionResult result = pollForWindow(
        "nonexistent-window-title-" + System.nanoTime(), 3, POLL_INTERVAL_MS);

    assertNotNull("Failure result must not be null", result);
    assertEquals("not_detected", result.status);
    assertNotNull("failure_reason must be set", result.failureReason);

    Path artifact = writeDetectionEvidence(evidenceDir, result);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"not_detected\""));
    assertTrue(json, json.contains("\"failure_reason\":"));
    assertFalse("failure evidence must not contain window_id",
        json.contains("\"window_id\""));
    assertFalse("failure evidence must not contain window_title",
        json.contains("\"window_title\""));
    assertAllFalseClaimBooleans(json);
    assertDoesNotClaimArray(json);
  }

  @Test
  public void disposesFrameRegardlessOfDetectionOutcome() throws Exception {
    assumeFalse("Requires a display (Xvfb or native)",
        GraphicsEnvironment.isHeadless());
    String uniqueTitle = WINDOW_TITLE + " dispose-" + System.nanoTime();
    JFrame frame = new JFrame(uniqueTitle);
    try {
      frame.setSize(100, 100);
      frame.setVisible(true);
      pollForWindow(uniqueTitle);
    } finally {
      frame.dispose();
    }
    boolean stillShowing = false;
    for (Window w : Window.getWindows()) {
      if (w instanceof JFrame jf
          && jf.getTitle().contains(uniqueTitle)
          && jf.isShowing()) {
        stillShowing = true;
      }
    }
    assertFalse("Frame must not be showing after dispose()", stillShowing);
  }

  // --- Schema contract tests (headless-safe) ---

  @Test
  public void successEvidenceSchemaMatchesDocumentedContract() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("schema-success").toPath();
    DetectionResult result = new DetectionResult(
        "detected", "Test Window", "0xdeadbeef", null);

    Path artifact = writeDetectionEvidence(evidenceDir, result);
    String json = Files.readString(artifact);

    assertTrue(json, json.contains("\"schema_version\": \"" + SCHEMA_VERSION + "\""));
    assertTrue(json, json.contains("\"status\": \"detected\""));
    assertTrue(json, json.contains("\"window_title\": \"Test Window\""));
    assertTrue(json, json.contains("\"window_id\": \"0xdeadbeef\""));
    assertTrue(json, json.contains("\"poll_interval_ms\": 100"));
    assertTrue(json, json.contains("\"max_poll_iterations\": 100"));
    assertTrue(json, json.contains("\"max_wait_seconds\": 10"));
    assertAllFalseClaimBooleans(json);
    assertDoesNotClaimArray(json);
  }

  @Test
  public void failureEvidenceSchemaMatchesDocumentedContract() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("schema-failure").toPath();
    DetectionResult result = new DetectionResult(
        "not_detected", null, null, "window not found within 10s");

    Path artifact = writeDetectionEvidence(evidenceDir, result);
    String json = Files.readString(artifact);

    assertTrue(json, json.contains("\"schema_version\": \"" + SCHEMA_VERSION + "\""));
    assertTrue(json, json.contains("\"status\": \"not_detected\""));
    assertTrue(json, json.contains("\"failure_reason\": \"window not found within 10s\""));
    assertFalse("failure evidence must not contain window_id",
        json.contains("\"window_id\""));
    assertFalse("failure evidence must not contain window_title",
        json.contains("\"window_title\""));
    assertTrue(json, json.contains("\"poll_interval_ms\": 100"));
    assertTrue(json, json.contains("\"max_poll_iterations\": 100"));
    assertTrue(json, json.contains("\"max_wait_seconds\": 10"));
    assertAllFalseClaimBooleans(json);
    assertDoesNotClaimArray(json);
  }

  @Test
  public void escapesUntrustedWindowTitleInEvidence() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("escaping-evidence").toPath();
    DetectionResult result = new DetectionResult(
        "detected", "Run \"Alice\"\t\u0001", "0xabcdef01", null);

    Path artifact = writeDetectionEvidence(evidenceDir, result);
    String json = Files.readString(artifact);

    assertTrue(json, json.contains("\"window_title\": \"Run \\\"Alice\\\"\\t\\u0001\""));
    assertTrue(json, json.contains("\"schema_version\": \"" + SCHEMA_VERSION + "\""));
  }

  @Test
  public void escapesUntrustedFailureReasonInEvidence() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("failure-escaping-evidence").toPath();
    DetectionResult result = new DetectionResult(
        "not_detected", null, null, "reason with \"quotes\" and \ttab");

    Path artifact = writeDetectionEvidence(evidenceDir, result);
    String json = Files.readString(artifact);

    assertTrue(json, json.contains("\"failure_reason\": \"reason with \\\"quotes\\\" and \\ttab\""));
  }

  // --- Safety tests (headless-safe) ---

  @Test
  public void atomicWriteRejectsSymlinkArtifact() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("symlink-evidence").toPath();
    Path outsideTarget = temporaryFolder.newFile("outside-target.json").toPath();
    Files.writeString(outsideTarget, "outside");
    Files.createSymbolicLink(evidenceDir.resolve(ARTIFACT_NAME), outsideTarget);

    DetectionResult result = new DetectionResult(
        "detected", WINDOW_TITLE, "0x12345678", null);
    try {
      writeDetectionEvidence(evidenceDir, result);
      fail("Symlink artifact should be rejected");
    } catch (IOException expected) {
      assertEquals("outside", Files.readString(outsideTarget));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsParentTraversalArtifactPath() {
    EatmeRunWindowEvidence.artifactPath(
        temporaryFolder.getRoot().toPath(), "../run-window-detection.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsNestedArtifactPath() {
    EatmeRunWindowEvidence.artifactPath(
        temporaryFolder.getRoot().toPath(), "nested/run-window-detection.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsAbsoluteArtifactPath() {
    EatmeRunWindowEvidence.artifactPath(
        temporaryFolder.getRoot().toPath(), "/tmp/run-window-detection.json");
  }

  @Test
  public void replacesHardLinkedArtifactWithoutCorruptingLinkedTarget() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("hardlink-evidence").toPath();
    Path outsideFile = temporaryFolder.newFile("outside-hardlinked.json").toPath();
    Files.writeString(outsideFile, "outside");
    Path hardLinked = evidenceDir.resolve(ARTIFACT_NAME);
    try {
      Files.createLink(hardLinked, outsideFile);
    } catch (IOException | SecurityException | UnsupportedOperationException ex) {
      assumeTrue("hard links unavailable in this test environment", false);
    }

    DetectionResult result = new DetectionResult(
        "detected", WINDOW_TITLE, "0xfeedface", null);
    writeDetectionEvidence(evidenceDir, result);

    assertEquals("outside", Files.readString(outsideFile));
    String json = Files.readString(hardLinked);
    assertTrue(json, json.contains("\"status\": \"detected\""));
  }

  @Test
  public void doesNotClaimArrayOrderMatchesEatmeRunWindowEvidenceConvention() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("order-evidence").toPath();
    DetectionResult result = new DetectionResult(
        "detected", WINDOW_TITLE, "0x1234", null);
    Path artifact = writeDetectionEvidence(evidenceDir, result);
    String json = Files.readString(artifact);

    int prevIndex = -1;
    for (String claim : DOES_NOT_CLAIM) {
      int index = json.indexOf("\"" + claim + "\"");
      assertTrue("does_not_claim must contain: " + claim, index >= 0);
      assertTrue("does_not_claim order: " + claim + " must follow previous entry",
          index > prevIndex);
      prevIndex = index;
    }
  }

  // --- Detection infrastructure ---

  private static final class DetectionResult {
    final String status;
    final String windowTitle;
    final String windowId;
    final String failureReason;

    DetectionResult(String status, String windowTitle, String windowId, String failureReason) {
      this.status = status;
      this.windowTitle = windowTitle;
      this.windowId = windowId;
      this.failureReason = failureReason;
    }
  }

  private static DetectionResult pollForWindow(String titleSubstring) throws InterruptedException {
    return pollForWindow(titleSubstring, MAX_POLL_ITERATIONS, POLL_INTERVAL_MS);
  }

  private static DetectionResult pollForWindow(String titleSubstring,
      int maxIterations, int intervalMs) throws InterruptedException {
    for (int i = 0; i < maxIterations; i++) {
      for (Window w : Window.getWindows()) {
        if (w instanceof JFrame jf
            && jf.getTitle().contains(titleSubstring)
            && jf.isShowing()) {
          String windowId = "0x" + Integer.toHexString(System.identityHashCode(jf));
          return new DetectionResult("detected", jf.getTitle(), windowId, null);
        }
      }
      Thread.sleep(intervalMs);
    }
    int totalSeconds = (maxIterations * intervalMs) / 1000;
    return new DetectionResult("not_detected", null, null,
        "window not found within " + totalSeconds + "s");
  }

  private Path writeDetectionEvidence(Path evidenceDir, DetectionResult result) throws IOException {
    Path artifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, ARTIFACT_NAME);
    String content;
    if ("detected".equals(result.status)) {
      content = successJson(result.windowTitle, result.windowId);
    } else {
      content = failureJson(result.failureReason);
    }
    writeArtifactAtomically(evidenceDir, artifact, content);
    if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS) || Files.size(artifact) == 0) {
      throw new IOException("Detection evidence artifact was not written: " + artifact);
    }
    return artifact;
  }

  private static String successJson(String windowTitle, String windowId) {
    return "{\n"
        + "  \"schema_version\": \"" + SCHEMA_VERSION + "\",\n"
        + "  \"status\": \"detected\",\n"
        + "  \"window_title\": \"" + EatmeRunWindowEvidence.escapeJson(windowTitle) + "\",\n"
        + "  \"window_id\": \"" + windowId + "\",\n"
        + pollingParametersJson()
        + claimedFieldsJson()
        + doesNotClaimJson()
        + "}\n";
  }

  private static String failureJson(String failureReason) {
    return "{\n"
        + "  \"schema_version\": \"" + SCHEMA_VERSION + "\",\n"
        + "  \"status\": \"not_detected\",\n"
        + "  \"failure_reason\": \"" + EatmeRunWindowEvidence.escapeJson(failureReason) + "\",\n"
        + pollingParametersJson()
        + claimedFieldsJson()
        + doesNotClaimJson()
        + "}\n";
  }

  private static String pollingParametersJson() {
    return "  \"poll_interval_ms\": " + POLL_INTERVAL_MS + ",\n"
        + "  \"max_poll_iterations\": " + MAX_POLL_ITERATIONS + ",\n"
        + "  \"max_wait_seconds\": " + MAX_WAIT_SECONDS + ",\n";
  }

  private static String claimedFieldsJson() {
    return "  \"rendering_correctness_claimed\": false,\n"
        + "  \"run_execution_claimed\": false,\n"
        + "  \"world_execution_claimed\": false,\n"
        + "  \"active_rendering_claimed\": false,\n"
        + "  \"save_claimed\": false,\n"
        + "  \"grading_claimed\": false,\n"
        + "  \"full_ui_automation_claimed\": false,\n";
  }

  private static String doesNotClaimJson() {
    StringBuilder json = new StringBuilder("  \"does_not_claim\": [\n");
    for (int i = 0; i < DOES_NOT_CLAIM.length; i++) {
      json.append("    \"").append(DOES_NOT_CLAIM[i]).append("\"");
      if (i + 1 < DOES_NOT_CLAIM.length) {
        json.append(",");
      }
      json.append("\n");
    }
    json.append("  ]\n");
    return json.toString();
  }

  private static void writeArtifactAtomically(Path evidenceDir, Path artifact, String content) throws IOException {
    if (Files.isSymbolicLink(artifact)) {
      throw new IOException("Detection evidence artifact refuses to overwrite symlink: " + artifact);
    }
    Path temp = Files.createTempFile(evidenceDir, ARTIFACT_NAME, ".tmp");
    try {
      Files.writeString(temp, content, StandardCharsets.UTF_8);
      Files.move(temp, artifact,
          StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  // --- Assertion helpers ---

  private static void assertSuccessSchemaFields(String json) {
    assertTrue(json, json.contains("\"schema_version\": \"" + SCHEMA_VERSION + "\""));
    assertTrue(json, json.contains("\"status\": \"detected\""));
    assertTrue(json, json.contains("\"window_title\":"));
    assertTrue(json, json.contains("\"window_id\": \"0x"));
    assertTrue(json, json.contains("\"poll_interval_ms\": 100"));
    assertTrue(json, json.contains("\"max_poll_iterations\": 100"));
    assertTrue(json, json.contains("\"max_wait_seconds\": 10"));
  }

  private static void assertAllFalseClaimBooleans(String json) {
    assertFalseClaim(json, "rendering_correctness_claimed");
    assertFalseClaim(json, "run_execution_claimed");
    assertFalseClaim(json, "world_execution_claimed");
    assertFalseClaim(json, "active_rendering_claimed");
    assertFalseClaim(json, "save_claimed");
    assertFalseClaim(json, "grading_claimed");
    assertFalseClaim(json, "full_ui_automation_claimed");
  }

  private static void assertFalseClaim(String json, String fieldName) {
    assertTrue(json, json.contains("\"" + fieldName + "\": false"));
  }

  private static void assertDoesNotClaimArray(String json) {
    assertTrue(json, json.contains("\"does_not_claim\""));
    for (String claim : DOES_NOT_CLAIM) {
      assertTrue("does_not_claim must include: " + claim,
          json.contains("\"" + claim + "\""));
    }
  }
}
