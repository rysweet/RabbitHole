package org.alice.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TDD contract tests for {@link EatmeEvidenceWriter}, which encapsulates ALL
 * JSON artifact writing, validation, and atomic I/O for desktop Run execution evidence.
 *
 * <p>Extracted from EatmeDesktopRunExecutionEvidence: writeDesktopRunExecution,
 * writeStringAtomically, requireNonEmptyArtifact, runtimeLog, writeFirstLessonNextActionContract,
 * writeSaveMenuActionTargetNoGo, writeRunStatusSummary, writeDesktopRunExecutionGapReport,
 * validateDesktopRunExecutionGapReport, blockerCodesJson, blockerDetailsJson, jsonArray,
 * pixelObservationSummary, pixelObservationReportingNote.</p>
 */
public class EatmeEvidenceWriterTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // =========================================================================
  // writeStringAtomically
  // =========================================================================

  @Test
  public void writeStringAtomicallyCreatesFileWithContent() throws Exception {
    Path target = temporaryFolder.getRoot().toPath().resolve("test.json");
    EatmeEvidenceWriter.writeStringAtomically(target, "{\"key\": \"value\"}");
    assertTrue(Files.exists(target));
    assertEquals("{\"key\": \"value\"}", Files.readString(target));
  }

  @Test
  public void writeStringAtomicallyOverwritesExistingFile() throws Exception {
    Path target = temporaryFolder.getRoot().toPath().resolve("test.json");
    EatmeEvidenceWriter.writeStringAtomically(target, "old content");
    EatmeEvidenceWriter.writeStringAtomically(target, "new content");
    assertEquals("new content", Files.readString(target));
  }

  @Test
  public void writeStringAtomicallyCleansTempFile() throws Exception {
    Path target = temporaryFolder.getRoot().toPath().resolve("test.json");
    EatmeEvidenceWriter.writeStringAtomically(target, "content");
    assertFalse(Files.exists(target.resolveSibling("test.json.tmp")));
  }

  // =========================================================================
  // requireNonEmptyArtifact
  // =========================================================================

  @Test
  public void requireNonEmptyArtifactPassesForNonEmptyFile() throws Exception {
    Path file = temporaryFolder.newFile("non-empty.json").toPath();
    Files.writeString(file, "content");
    EatmeEvidenceWriter.requireNonEmptyArtifact(file, "test artifact");
    // Should not throw
  }

  @Test(expected = IOException.class)
  public void requireNonEmptyArtifactThrowsForMissingFile() throws Exception {
    Path file = temporaryFolder.getRoot().toPath().resolve("missing.json");
    EatmeEvidenceWriter.requireNonEmptyArtifact(file, "missing artifact");
  }

  @Test(expected = IOException.class)
  public void requireNonEmptyArtifactThrowsForEmptyFile() throws Exception {
    Path file = temporaryFolder.newFile("empty.json").toPath();
    EatmeEvidenceWriter.requireNonEmptyArtifact(file, "empty artifact");
  }

  // =========================================================================
  // runtimeLog
  // =========================================================================

  @Test
  public void runtimeLogContainsSchemaVersionAndProgramType() {
    String log = EatmeEvidenceWriter.runtimeLog("MyProgram",
        List.of("event1", "event2"));
    assertTrue(log, log.contains(
        "schema_version=eatme.alice-desktop-run-execution-log/v1"));
    assertTrue(log, log.contains("program_type=MyProgram"));
    assertTrue(log, log.contains("recorded_at="));
    assertTrue(log, log.contains("event1"));
    assertTrue(log, log.contains("event2"));
  }

  @Test
  public void runtimeLogHandlesEmptyEventsList() {
    String log = EatmeEvidenceWriter.runtimeLog("Program", List.of());
    assertTrue(log, log.contains("program_type=Program"));
    // Should still end with newline after recorded_at
    assertTrue(log.endsWith("\n"));
  }

  @Test
  public void runtimeLogHandlesNullProgramTypeName() {
    String log = EatmeEvidenceWriter.runtimeLog(null, List.of("event"));
    assertTrue(log, log.contains("program_type=null"));
    assertTrue(log, log.contains("event"));
  }

  // =========================================================================
  // jsonArray
  // =========================================================================

  @Test
  public void jsonArrayProducesEmptyArray() {
    assertEquals("[]", EatmeEvidenceWriter.jsonArray(List.of()));
  }

  @Test
  public void jsonArrayProducesSingleElementArray() {
    assertEquals("[\"value\"]",
        EatmeEvidenceWriter.jsonArray(List.of("value")));
  }

  @Test
  public void jsonArrayProducesMultiElementArrayWithCommas() {
    String result = EatmeEvidenceWriter.jsonArray(List.of("a", "b", "c"));
    assertEquals("[\"a\", \"b\", \"c\"]", result);
  }

  @Test
  public void jsonArrayEscapesSpecialCharacters() {
    String result = EatmeEvidenceWriter.jsonArray(
        List.of("val\"ue", "new\nline"));
    assertTrue(result, result.contains("val\\\"ue"));
    assertTrue(result, result.contains("new\\nline"));
  }

  // =========================================================================
  // pixelObservationSummary
  // =========================================================================

  @Test
  public void pixelObservationSummaryForObservedPixel() {
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name", "file.png",
        new java.awt.Rectangle(0, 0, 10, 10), 10, 10, 5, 5, 0);
    String summary = EatmeEvidenceWriter.pixelObservationSummary(observation);
    assertTrue(summary, summary.contains("desktop pixel sample observed"));
    assertTrue(summary, summary.contains("inspect the screenshot"));
  }

  @Test
  public void pixelObservationSummaryForBlockedPixel() {
    PixelObservation observation = PixelObservation.blocked(
        List.of(new BlockerDetail("code", "obs", "req")), "");
    String summary = EatmeEvidenceWriter.pixelObservationSummary(observation);
    assertTrue(summary, summary.contains("desktop pixel sample blocked"));
    assertTrue(summary, summary.contains("inspect blocker details"));
  }

  // =========================================================================
  // pixelObservationReportingNote
  // =========================================================================

  @Test
  public void pixelObservationReportingNoteForObservedPixel() {
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name", "file.png",
        new java.awt.Rectangle(0, 0, 10, 10), 10, 10, 5, 5, 0);
    String note = EatmeEvidenceWriter.pixelObservationReportingNote(observation);
    assertTrue(note, note.contains(
        "observed only for pixel sampling"));
    assertTrue(note, note.contains(
        "not visible rendering correctness"));
  }

  @Test
  public void pixelObservationReportingNoteForBlockedPixel() {
    PixelObservation observation = PixelObservation.blocked(
        List.of(new BlockerDetail("code", "obs", "req")), "");
    String note = EatmeEvidenceWriter.pixelObservationReportingNote(observation);
    assertTrue(note, note.contains("blocked until"));
    assertTrue(note, note.contains("blocker details are resolved"));
  }

  // =========================================================================
  // writeDesktopRunExecution
  // =========================================================================

  @Test
  public void writeDesktopRunExecutionCreatesValidArtifact() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("execution").toPath();
    Path artifact = EatmeEvidenceWriter.writeDesktopRunExecution(
        evidenceDir, "Program", true, true, 5, 3,
        "executed:Statement", List.of("event1"));
    assertTrue(Files.exists(artifact));
    String json = Files.readString(artifact);
    assertTrue(json, json.contains(
        "\"schema_version\": \"eatme.alice-desktop-run-execution/v1\""));
    assertTrue(json, json.contains(
        "\"status\": \"statement_execution_observed\""));
    assertTrue(json, json.contains("\"program_type\": \"Program\""));
    assertTrue(json, json.contains(
        "\"active_scene_invoke_started\": true"));
    assertTrue(json, json.contains(
        "\"active_scene_invoke_returned\": true"));
    assertTrue(json, json.contains("\"executing_statement_count\": 5"));
    assertTrue(json, json.contains("\"executed_statement_count\": 3"));
    assertTrue(json, json.contains(
        "\"latest_event\": \"executed:Statement\""));
  }

  @Test
  public void writeDesktopRunExecutionReportsPreparingWhenNoStatements()
      throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("preparing").toPath();
    Path artifact = EatmeEvidenceWriter.writeDesktopRunExecution(
        evidenceDir, "Program", false, false, 0, 0, "", List.of());
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"preparing\""));
  }

  @Test
  public void writeDesktopRunExecutionCreatesRuntimeLog() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("log").toPath();
    EatmeEvidenceWriter.writeDesktopRunExecution(
        evidenceDir, "Program", false, false, 0, 0, "",
        List.of("listener-installed"));
    Path logPath = evidenceDir.resolve("desktop-run-runtime.log");
    assertTrue(Files.exists(logPath));
    String log = Files.readString(logPath);
    assertTrue(log, log.contains("listener-installed"));
  }

  @Test
  public void writeDesktopRunExecutionEscapesProgramType() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("escape").toPath();
    Path artifact = EatmeEvidenceWriter.writeDesktopRunExecution(
        evidenceDir, "Program\nType", false, false, 0, 0, "", List.of());
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"program_type\": \"Program\\nType\""));
  }

  @Test
  public void writeDesktopRunExecutionReturnsArtifactPath() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("path").toPath();
    Path artifact = EatmeEvidenceWriter.writeDesktopRunExecution(
        evidenceDir, "P", false, false, 0, 0, "", List.of());
    assertEquals(evidenceDir.resolve("desktop-run-execution.json"), artifact);
  }

  // =========================================================================
  // writeFirstLessonNextActionContract
  // =========================================================================

  @Test
  public void writeFirstLessonNextActionContractCreatesValidArtifact()
      throws Exception {
    Path artifact = temporaryFolder.getRoot().toPath().resolve("next-action.json");
    EatmeEvidenceWriter.writeFirstLessonNextActionContract(artifact);
    assertTrue(Files.exists(artifact));
    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains(
        "\"schema_version\": \"eatme.alice-desktop-first-lesson-next-action/v1\""));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains(
        "\"source\": \"desktop_run_render_target_attachment\""));
    assertTrue(json, json.contains("\"candidate_actions\""));
    assertTrue(json, json.contains("\"desktop_save_menu_action\""));
    assertTrue(json, json.contains(
        "\"desktop_code_editor_or_procedure_action\""));
    assertTrue(json, json.contains("\"blocker\""));
    assertTrue(json, json.contains("\"doesNotClaim\""));
    assertTrue(json, json.contains("grading"));
    assertTrue(json, json.contains("creative assessment"));
  }

  // =========================================================================
  // writeSaveMenuActionTargetNoGo
  // =========================================================================

  @Test
  public void writeSaveMenuActionTargetNoGoCreatesValidArtifact()
      throws Exception {
    Path artifact = temporaryFolder.getRoot().toPath().resolve("save-menu.json");
    EatmeEvidenceWriter.writeSaveMenuActionTargetNoGo(artifact);
    assertTrue(Files.exists(artifact));
    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains(
        "\"schema_version\": \"eatme.alice-desktop-save-menu-action-target/v1\""));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains(
        "44ffba8a-3fb3-4cb5-97b6-55cd93c88e9d"));
    assertTrue(json, json.contains("\"blocker\""));
    assertTrue(json, json.contains(
        "desktop_save_menu_owner_not_available_at_render_attachment"));
    assertTrue(json, json.contains(
        "desktop_save_project_operation_not_invoked"));
    assertTrue(json, json.contains(
        "desktop_save_menu_readiness_not_observed"));
    assertTrue(json, json.contains("\"doesNotClaim\""));
  }

  // =========================================================================
  // writeRunStatusSummary
  // =========================================================================

  @Test
  public void writeRunStatusSummaryCreatesValidArtifactForBlockedPixel()
      throws Exception {
    Path artifact = temporaryFolder.getRoot().toPath().resolve("status.json");
    PixelObservation observation = PixelObservation.blocked(
        List.of(new BlockerDetail("code", "obs", "req")), "");
    EatmeEvidenceWriter.writeRunStatusSummary(artifact, observation);
    assertTrue(Files.exists(artifact));
    String json = Files.readString(artifact);
    assertTrue(json, json.contains(
        "\"schema_version\": \"eatme.alice-desktop-run-status-summary/v1\""));
    assertTrue(json, json.contains("\"status\": \"partial\""));
    assertTrue(json, json.contains(
        "\"pixel_observation_status\": \"blocked\""));
    assertTrue(json, json.contains("\"reporting_note\""));
    assertTrue(json, json.contains("blocked until"));
    assertTrue(json, json.contains("\"artifact_statuses\""));
    assertTrue(json, json.contains("\"missing_evidence\""));
    assertTrue(json, json.contains("\"exact_next_user_action\""));
    assertTrue(json, json.contains("\"remaining_unproven_behavior\""));
  }

  @Test
  public void writeRunStatusSummaryCreatesValidArtifactForObservedPixel()
      throws Exception {
    Path artifact = temporaryFolder.getRoot().toPath().resolve("status-obs.json");
    PixelObservation observation = PixelObservation.observed(
        "role", "class", "name", "file.png",
        new java.awt.Rectangle(0, 0, 10, 10), 10, 10, 5, 5, 0);
    EatmeEvidenceWriter.writeRunStatusSummary(artifact, observation);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains(
        "\"pixel_observation_status\": \"observed\""));
    assertTrue(json, json.contains("desktop pixel sample observed"));
  }

  // =========================================================================
  // validateDesktopRunExecutionGapReport
  // =========================================================================

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsNullEvidenceArtifacts() {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        null, "reason", allDoesNotClaimCategories());
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsEmptyEvidenceArtifacts() {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        List.of(), "reason", allDoesNotClaimCategories());
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsMissingRequiredArtifact() {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        List.of("desktop-run-render-affordance.json"),
        "reason", allDoesNotClaimCategories());
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsVmListenerArtifactInPayload() {
    java.util.List<String> artifacts =
        new java.util.ArrayList<>(requiredEvidenceArtifacts());
    artifacts.add("desktop-run-execution.json");
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        artifacts, "reason", allDoesNotClaimCategories());
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsRuntimeLogInPayload() {
    java.util.List<String> artifacts =
        new java.util.ArrayList<>(requiredEvidenceArtifacts());
    artifacts.add("desktop-run-runtime.log");
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        artifacts, "reason", allDoesNotClaimCategories());
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsBlankBlockerReason() {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        requiredEvidenceArtifacts(), "   ", allDoesNotClaimCategories());
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsNullBlockerReason() {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        requiredEvidenceArtifacts(), null, allDoesNotClaimCategories());
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsMissingDoesNotClaimCategory() {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        requiredEvidenceArtifacts(), "reason",
        List.of("full world execution",
            "visible rendering correctness",
            "grading",
            "Save completion"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateRejectsNullDoesNotClaim() {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        requiredEvidenceArtifacts(), "reason", null);
  }

  @Test
  public void validateAcceptsValidInputs() {
    // Should not throw
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        requiredEvidenceArtifacts(), "Missing proof.",
        allDoesNotClaimCategories());
  }

  // =========================================================================
  // writeDesktopRunExecutionGapReport
  // =========================================================================

  @Test
  public void writeDesktopRunExecutionGapReportCreatesValidArtifact()
      throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("gap-report").toPath();
    Path artifact = EatmeEvidenceWriter.writeDesktopRunExecutionGapReport(
        evidenceDir, requiredEvidenceArtifacts(),
        "Missing deterministic proof.");
    assertTrue(Files.exists(artifact));
    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains(
        "\"schema_version\": \"eatme.alice-desktop-run-execution-gap-report/v1\""));
    assertTrue(json, json.contains(
        "\"report_kind\": \"desktop_run_execution_gap\""));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"executableToday\""));
    assertTrue(json, json.contains("\"evidenceArtifacts\""));
    assertTrue(json, json.contains("\"blockerToFullWorldExecution\""));
    assertTrue(json, json.contains("Missing deterministic proof."));
    assertTrue(json, json.contains("\"failClosedRequirements\""));
    assertTrue(json, json.contains("\"doesNotClaim\""));
  }

  @Test
  public void writeDesktopRunExecutionGapReportReturnsArtifactPath()
      throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("gap-path").toPath();
    Path artifact = EatmeEvidenceWriter.writeDesktopRunExecutionGapReport(
        evidenceDir, requiredEvidenceArtifacts(), "reason");
    assertEquals(
        evidenceDir.resolve("desktop-run-execution-gap-report.json"),
        artifact);
  }

  @Test(expected = IllegalArgumentException.class)
  public void writeDesktopRunExecutionGapReportFailsClosedOnInvalidInput()
      throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("fail-closed").toPath();
    EatmeEvidenceWriter.writeDesktopRunExecutionGapReport(
        evidenceDir, List.of(), "reason");
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private static List<String> requiredEvidenceArtifacts() {
    return List.of(
        "desktop-run-render-affordance.json",
        "desktop-run-pixel-boundary.json",
        "desktop-run-pixel-observation.json",
        "desktop-first-lesson-next-action.json",
        "desktop-save-menu-action-target.json",
        "desktop-run-status-summary.json");
  }

  private static List<String> allDoesNotClaimCategories() {
    return List.of(
        "full world execution",
        "visible rendering correctness",
        "grading",
        "Save completion",
        "full UI automation");
  }
}
