package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TDD tests for EvidenceJsonWriter — the extracted JSON string builder
 * from SaveOperationCompletionEvidence (issue #561).
 *
 * These tests define the contract that EvidenceJsonWriter must satisfy.
 * They will FAIL until the extraction is implemented.
 */
public class EvidenceJsonWriterTest {

  // ── escapeJson ────────────────────────────────────────────────────

  @Test
  public void escapeJsonEscapesBackslash() {
    assertEquals("a\\\\b", EvidenceJsonWriter.escapeJson("a\\b"));
  }

  @Test
  public void escapeJsonEscapesDoubleQuote() {
    assertEquals("a\\\"b", EvidenceJsonWriter.escapeJson("a\"b"));
  }

  @Test
  public void escapeJsonEscapesControlChars() {
    assertEquals("a\\nb", EvidenceJsonWriter.escapeJson("a\nb"));
    assertEquals("a\\rb", EvidenceJsonWriter.escapeJson("a\rb"));
    assertEquals("a\\tb", EvidenceJsonWriter.escapeJson("a\tb"));
    assertEquals("a\\bb", EvidenceJsonWriter.escapeJson("a\bb"));
    assertEquals("a\\fb", EvidenceJsonWriter.escapeJson("a\fb"));
  }

  @Test
  public void escapeJsonEscapesLowControlCharsAsUnicode() {
    // U+0001 should become \u0001
    assertEquals("\\u0001", EvidenceJsonWriter.escapeJson("\u0001"));
    assertEquals("\\u001f", EvidenceJsonWriter.escapeJson("\u001f"));
  }

  @Test
  public void escapeJsonPassesThroughPlainText() {
    assertEquals("hello world", EvidenceJsonWriter.escapeJson("hello world"));
  }

  @Test
  public void escapeJsonHandlesEmptyString() {
    assertEquals("", EvidenceJsonWriter.escapeJson(""));
  }

  // ── stringJson ────────────────────────────────────────────────────

  @Test
  public void stringJsonReturnsNullLiteralForNullInput() {
    assertEquals("null", EvidenceJsonWriter.stringJson(null));
  }

  @Test
  public void stringJsonWrapsValueInQuotes() {
    assertEquals("\"hello\"", EvidenceJsonWriter.stringJson("hello"));
  }

  @Test
  public void stringJsonEscapesSpecialChars() {
    assertEquals("\"a\\\"b\"", EvidenceJsonWriter.stringJson("a\"b"));
  }

  // ── nullToBlank ───────────────────────────────────────────────────

  @Test
  public void nullToBlankReturnsEmptyForNull() {
    assertEquals("", EvidenceJsonWriter.nullToBlank(null));
  }

  @Test
  public void nullToBlankReturnsSameForNonNull() {
    assertEquals("value", EvidenceJsonWriter.nullToBlank("value"));
  }

  // ── className ─────────────────────────────────────────────────────

  @Test
  public void classNameReturnsNullForNullInput() {
    assertEquals(null, EvidenceJsonWriter.className(null));
  }

  @Test
  public void classNameReturnsFqcn() {
    assertEquals("java.lang.String", EvidenceJsonWriter.className("test"));
  }

  // ── operationSimpleName ───────────────────────────────────────────

  @Test
  public void operationSimpleNameExtractsLastSegment() {
    assertEquals("SaveProjectOperation",
        EvidenceJsonWriter.operationSimpleName(
            "org.alice.ide.croquet.models.projecturi.SaveProjectOperation"));
  }

  @Test
  public void operationSimpleNameReturnsInputWithoutDot() {
    assertEquals("SaveProjectOperation",
        EvidenceJsonWriter.operationSimpleName("SaveProjectOperation"));
  }

  @Test
  public void operationSimpleNameHandlesNull() {
    assertEquals("", EvidenceJsonWriter.operationSimpleName(null));
  }

  // ── status ────────────────────────────────────────────────────────

  @Test
  public void statusReturnsFinishedWhenResultFinished() {
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(true, false, 1, 1, null);
    assertEquals("finished", EvidenceJsonWriter.status(result));
  }

  @Test
  public void statusReturnsCanceledWhenResultCanceled() {
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(false, true, 1, 0, null);
    assertEquals("canceled", EvidenceJsonWriter.status(result));
  }

  @Test
  public void statusReturnsIncompleteOtherwise() {
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(false, false, 0, 0, null);
    assertEquals("incomplete", EvidenceJsonWriter.status(result));
  }

  // ── savedFileJson ─────────────────────────────────────────────────

  @Test
  public void savedFileJsonReturnsNullLiteralForNullFile() {
    assertEquals("null", EvidenceJsonWriter.savedFileJson(null));
  }

  @Test
  public void savedFileJsonReturnsRedactedPathForExternalFile() throws Exception {
    Path external = Files.createTempFile("alice-json-test-", ".a3p");
    try {
      String json = EvidenceJsonWriter.savedFileJson(external.toFile());
      assertTrue(json, json.startsWith("\""));
      assertTrue(json, json.endsWith("\""));
      assertTrue(json, json.contains(external.getFileName().toString()));
    } finally {
      Files.deleteIfExists(external);
    }
  }

  // ── savedFileExistsJson ───────────────────────────────────────────

  @Test
  public void savedFileExistsJsonReturnsNullLiteralForNullFile() {
    assertEquals("null", EvidenceJsonWriter.savedFileExistsJson(null, false));
  }

  @Test
  public void savedFileExistsJsonReturnsTrueWhenFileExists() {
    assertEquals("true",
        EvidenceJsonWriter.savedFileExistsJson(new File("test.a3p"), true));
  }

  @Test
  public void savedFileExistsJsonReturnsFalseWhenFileMissing() {
    assertEquals("false",
        EvidenceJsonWriter.savedFileExistsJson(new File("test.a3p"), false));
  }

  // ── savedFileSizeJson ─────────────────────────────────────────────

  @Test
  public void savedFileSizeJsonReturnsNullLiteralForNull() {
    assertEquals("null", EvidenceJsonWriter.savedFileSizeJson(null));
  }

  @Test
  public void savedFileSizeJsonReturnsNumericString() {
    assertEquals("1234", EvidenceJsonWriter.savedFileSizeJson(1234L));
  }

  // ── resultJson ────────────────────────────────────────────────────

  @Test
  public void resultJsonContainsSchemaVersion() {
    String json = resultJsonForFinishedSave();
    assertTrue(json, json.contains(
        "\"schema_version\": \"eatme.alice-desktop-save-operation-result/v1\""));
  }

  @Test
  public void resultJsonContainsOperationAndExtension() {
    String json = resultJsonForFinishedSave();
    assertTrue(json, json.contains("\"operation\": \"org.alice.test.Op\""));
    assertTrue(json, json.contains("\"extension\": \"a3p\""));
  }

  @Test
  public void resultJsonContainsFinishedStatus() {
    String json = resultJsonForFinishedSave();
    assertTrue(json, json.contains("\"status\": \"finished\""));
    assertTrue(json, json.contains("\"finished\": true"));
    assertTrue(json, json.contains("\"canceled\": false"));
  }

  @Test
  public void resultJsonContainsWroteFileTrueForExistingNonEmptyA3p() {
    String json = resultJsonForFinishedSave();
    assertTrue(json, json.contains("\"wroteFile\": true"));
    assertTrue(json, json.contains("\"claim\":"));
  }

  @Test
  public void resultJsonContainsWroteFileFalseForNullSavedFile() {
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(false, true, 1, 0, null);
    EvidenceFileOperations.RegularFileState missing =
        EvidenceFileOperations.RegularFileState.MISSING;
    String json = EvidenceJsonWriter.resultJson(
        "org.alice.test.Op", "a3p", result, missing);
    assertTrue(json, json.contains("\"wroteFile\": false"));
    assertTrue(json, json.contains("\"saved_file\": null"));
    assertFalse(json, json.contains("\"claim\":"));
    assertTrue(json, json.contains("\"reporting_summary\":"));
  }

  @Test
  public void resultJsonContainsDoesNotClaimArray() {
    String json = resultJsonForFinishedSave();
    assertTrue(json, json.contains("\"doesNotClaim\": ["));
    assertTrue(json, json.contains("desktop Save menu item was clicked"));
    assertTrue(json, json.contains("full lesson completion"));
  }

  @Test
  public void resultJsonContainsDialogType() {
    String json = resultJsonForFinishedSave();
    assertTrue(json, json.contains("\"dialogType\": \"Swing JFileChooser\""));
  }

  // ── dialogControlTargetJson ───────────────────────────────────────

  @Test
  public void dialogControlTargetJsonReportsBlockedWhenDialogRequested() {
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(true, false, 1, 1, null);
    String json = EvidenceJsonWriter.dialogControlTargetJson(
        "org.alice.test.Op", "a3p", result);
    assertTrue(json, json.contains(
        "\"schema_version\": \"eatme.alice-desktop-save-dialog-control-target/v1\""));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains(
        "\"reason\": \"desktop_save_dialog_control_not_available\""));
    assertTrue(json, json.contains("\"prompt_count\": 1"));
    assertTrue(json, json.contains("\"swing_file_chooser\""));
  }

  @Test
  public void dialogControlTargetJsonReportsUnsupportedWhenNoDialogRequested() {
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(true, false, 0, 1, null);
    String json = EvidenceJsonWriter.dialogControlTargetJson(
        "org.alice.test.Op", "a3p", result);
    assertTrue(json, json.contains("\"status\": \"unsupported\""));
    assertTrue(json, json.contains(
        "\"reason\": \"save_operation_did_not_request_dialog\""));
  }

  @Test
  public void dialogControlTargetJsonContainsDialogTargets() {
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(true, false, 1, 1, null);
    String json = EvidenceJsonWriter.dialogControlTargetJson(
        "org.alice.test.Op", "a3p", result);
    assertTrue(json, json.contains(
        "org.lgna.croquet.DocumentFrame#showSaveFileDialog(File,String,String)"));
    assertTrue(json, json.contains(
        "edu.cmu.cs.dennisc.java.awt.FileDialogUtilities#showSaveFileDialog(Component,File,String,String)"));
  }

  // ── saveActionInvocationProofJson ─────────────────────────────────

  @Test
  public void saveActionInvocationProofJsonStatusForMissingStageIde() {
    String json = EvidenceJsonWriter.saveActionInvocationProofJson(
        "org.alice.test.Op", "a3p",
        false, false,
        SaveOperationCompletionEvidence.InvocationTrigger.none());
    assertTrue(json, json.contains(
        "\"schema_version\": \"eatme.alice-desktop-save-action-invocation-proof/v1\""));
    assertTrue(json, json.contains("\"status\": \"unsupported\""));
    assertTrue(json, json.contains("\"reason\": \"missing_active_stage_ide\""));
  }

  @Test
  public void saveActionInvocationProofJsonStatusForMissingDocumentFrame() {
    String json = EvidenceJsonWriter.saveActionInvocationProofJson(
        "org.alice.test.Op", "a3p",
        true, false,
        SaveOperationCompletionEvidence.InvocationTrigger.none());
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"reason\": \"missing_project_document_frame\""));
  }

  @Test
  public void saveActionInvocationProofJsonStatusForActionInvoked() {
    String json = EvidenceJsonWriter.saveActionInvocationProofJson(
        "org.alice.test.Op", "a3p",
        true, true,
        SaveOperationCompletionEvidence.InvocationTrigger.none());
    assertTrue(json, json.contains("\"status\": \"action_invoked\""));
    assertTrue(json, json.contains("\"reason\": \"save_action_invoked\""));
  }

  @Test
  public void saveActionInvocationProofJsonHandlesNullTrigger() {
    String json = EvidenceJsonWriter.saveActionInvocationProofJson(
        "org.alice.test.Op", "a3p",
        true, true, null);
    assertTrue(json, json.contains("\"menu_item_dispatch\": false"));
    assertTrue(json, json.contains("\"trigger_class\": null"));
  }

  @Test
  public void saveActionInvocationProofJsonContainsObservedBlock() {
    String json = EvidenceJsonWriter.saveActionInvocationProofJson(
        "org.alice.test.Op", "a3p",
        true, true,
        SaveOperationCompletionEvidence.InvocationTrigger.none());
    assertTrue(json, json.contains("\"observed\":"));
    assertTrue(json, json.contains("\"active_stage_ide_available\": true"));
    assertTrue(json, json.contains("\"project_document_frame_available\": true"));
  }

  @Test
  public void saveActionInvocationProofJsonContainsBlockerAndSummary() {
    String json = EvidenceJsonWriter.saveActionInvocationProofJson(
        "org.alice.test.Op", "a3p",
        false, false,
        SaveOperationCompletionEvidence.InvocationTrigger.none());
    assertTrue(json, json.contains("\"blocker\":"));
    assertTrue(json, json.contains("\"reporting_summary\":"));
    assertTrue(json, json.contains("\"requiresNextEvidence\":"));
    assertTrue(json, json.contains("\"doesNotClaim\":"));
  }

  @Test
  public void saveActionInvocationProofJsonContainsTargetBlock() {
    String json = EvidenceJsonWriter.saveActionInvocationProofJson(
        "org.alice.test.SaveProjectOperation", "a3p",
        true, true,
        SaveOperationCompletionEvidence.InvocationTrigger.none());
    assertTrue(json, json.contains("\"target\":"));
    assertTrue(json, json.contains(
        "\"action\": \"SaveProjectOperation.getInstance().fire(UserActivity)\""));
    assertTrue(json, json.contains(
        "\"dialog_path\": \"application.getDocumentFrame().showSaveFileDialog(directory, filename, extension)\""));
  }

  // ── nonEmptyProjectFileWrite ──────────────────────────────────────

  @Test
  public void nonEmptyProjectFileWriteIncludesExtension() {
    assertEquals("a non-empty .a3p project file write",
        EvidenceJsonWriter.nonEmptyProjectFileWrite("a3p"));
  }

  @Test
  public void nonEmptyProjectFileWriteGenericForNullExtension() {
    assertEquals("a non-empty project file write",
        EvidenceJsonWriter.nonEmptyProjectFileWrite(null));
  }

  @Test
  public void nonEmptyProjectFileWriteGenericForBlankExtension() {
    assertEquals("a non-empty project file write",
        EvidenceJsonWriter.nonEmptyProjectFileWrite(""));
  }

  // ── wroteFile ─────────────────────────────────────────────────────

  @Test
  public void wroteFileReturnsFalseForNullPath() {
    EvidenceFileOperations.RegularFileState state =
        new EvidenceFileOperations.RegularFileState(true, 100);
    assertFalse(EvidenceJsonWriter.wroteFile(null, state, "a3p"));
  }

  @Test
  public void wroteFileReturnsFalseWhenExtensionDoesNotMatch() {
    EvidenceFileOperations.RegularFileState state =
        new EvidenceFileOperations.RegularFileState(true, 100);
    assertFalse(EvidenceJsonWriter.wroteFile(
        Path.of("test.txt"), state, "a3p"));
  }

  @Test
  public void wroteFileReturnsTrueWhenExtensionMatchesAndNonEmpty() {
    EvidenceFileOperations.RegularFileState state =
        new EvidenceFileOperations.RegularFileState(true, 100);
    assertTrue(EvidenceJsonWriter.wroteFile(
        Path.of("test.a3p"), state, "a3p"));
  }

  @Test
  public void wroteFileReturnsFalseWhenFileIsEmpty() {
    EvidenceFileOperations.RegularFileState state =
        new EvidenceFileOperations.RegularFileState(true, 0);
    assertFalse(EvidenceJsonWriter.wroteFile(
        Path.of("test.a3p"), state, "a3p"));
  }

  @Test
  public void wroteFileReturnsFalseWhenFileDoesNotExist() {
    assertFalse(EvidenceJsonWriter.wroteFile(
        Path.of("test.a3p"),
        EvidenceFileOperations.RegularFileState.MISSING,
        "a3p"));
  }

  // ── hasExtension ──────────────────────────────────────────────────

  @Test
  public void hasExtensionReturnsFalseForNullPath() {
    assertFalse(EvidenceJsonWriter.hasExtension(null, "a3p"));
  }

  @Test
  public void hasExtensionReturnsTrueForNullExtension() {
    assertTrue(EvidenceJsonWriter.hasExtension(Path.of("test.a3p"), null));
  }

  @Test
  public void hasExtensionReturnsTrueForBlankExtension() {
    assertTrue(EvidenceJsonWriter.hasExtension(Path.of("test.a3p"), ""));
  }

  @Test
  public void hasExtensionMatchesSuffix() {
    assertTrue(EvidenceJsonWriter.hasExtension(Path.of("test.a3p"), "a3p"));
  }

  @Test
  public void hasExtensionReturnsFalseForMismatch() {
    assertFalse(EvidenceJsonWriter.hasExtension(Path.of("test.txt"), "a3p"));
  }

  // ── resultClaimOrSummaryJson ──────────────────────────────────────

  @Test
  public void resultClaimOrSummaryJsonReturnsClaimWhenWrote() {
    String json = EvidenceJsonWriter.resultClaimOrSummaryJson(true, "finished", "a3p");
    assertTrue(json, json.contains("\"claim\":"));
    assertTrue(json, json.contains("Save control/dialog approval reached"));
    assertTrue(json, json.contains("a non-empty .a3p project file write"));
    assertFalse(json, json.contains("\"reporting_summary\":"));
  }

  @Test
  public void resultClaimOrSummaryJsonReturnsSummaryWhenNotWrote() {
    String json = EvidenceJsonWriter.resultClaimOrSummaryJson(false, "canceled", "a3p");
    assertTrue(json, json.contains("\"reporting_summary\":"));
    assertTrue(json, json.contains("Save operation evidence recorded status canceled"));
    assertFalse(json, json.contains("\"claim\":"));
  }

  // ── saveActionInvocationReason ────────────────────────────────────

  @Test
  public void saveActionInvocationReasonMissingStageIde() {
    assertEquals("missing_active_stage_ide",
        EvidenceJsonWriter.saveActionInvocationReason(
            false, false,
            SaveOperationCompletionEvidence.InvocationTrigger.none()));
  }

  @Test
  public void saveActionInvocationReasonMissingDocumentFrame() {
    assertEquals("missing_project_document_frame",
        EvidenceJsonWriter.saveActionInvocationReason(
            true, false,
            SaveOperationCompletionEvidence.InvocationTrigger.none()));
  }

  @Test
  public void saveActionInvocationReasonActionInvoked() {
    assertEquals("save_action_invoked",
        EvidenceJsonWriter.saveActionInvocationReason(
            true, true,
            SaveOperationCompletionEvidence.InvocationTrigger.none()));
  }

  // ── JSON output is well-formed ────────────────────────────────────

  @Test
  public void resultJsonStartsAndEndsWithBraces() {
    String json = resultJsonForFinishedSave();
    assertTrue(json, json.trim().startsWith("{"));
    assertTrue(json, json.trim().endsWith("}"));
  }

  @Test
  public void dialogControlTargetJsonStartsAndEndsWithBraces() {
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(true, false, 1, 1, null);
    String json = EvidenceJsonWriter.dialogControlTargetJson(
        "org.alice.test.Op", "a3p", result);
    assertTrue(json, json.trim().startsWith("{"));
    assertTrue(json, json.trim().endsWith("}"));
  }

  @Test
  public void saveActionInvocationProofJsonStartsAndEndsWithBraces() {
    String json = EvidenceJsonWriter.saveActionInvocationProofJson(
        "org.alice.test.Op", "a3p",
        true, true,
        SaveOperationCompletionEvidence.InvocationTrigger.none());
    assertTrue(json, json.trim().startsWith("{"));
    assertTrue(json, json.trim().endsWith("}"));
  }

  // ── helper ────────────────────────────────────────────────────────

  private String resultJsonForFinishedSave() {
    EvidenceFileOperations.RegularFileState fileState =
        new EvidenceFileOperations.RegularFileState(true, 1024);
    SaveOperationFlow.Result result =
        new SaveOperationFlow.Result(true, false, 1, 1,
            new File("target/test-save/classroom.a3p"));
    return EvidenceJsonWriter.resultJson(
        "org.alice.test.Op", "a3p", result, fileState);
  }
}
