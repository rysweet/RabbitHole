package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.util.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class SaveOperationCompletionEvidence {
  static final String EVIDENCE_DIR_PROPERTY = "org.alice.eatme.saveOperationEvidenceDir";
  static final String PROOF_ONLY_PROPERTY = "org.alice.eatme.saveActionInvocationProofOnly";
  static final String ARTIFACT = "desktop-save-operation-result.json";
  static final String DIALOG_CONTROL_ARTIFACT = "desktop-save-dialog-control-target.json";
  static final String SAVE_ACTION_INVOCATION_PROOF_ARTIFACT = "desktop-save-action-invocation-proof.json";

  private SaveOperationCompletionEvidence() {
  }

  static void record(String operationClass, String extension, SaveOperationFlow.Result result) {
    String evidenceDir = System.getProperty(EVIDENCE_DIR_PROPERTY);
    if (evidenceDir == null || evidenceDir.isBlank() || result == null) {
      return;
    }
    try {
      write(Path.of(evidenceDir), operationClass, extension, result);
    } catch (IOException | RuntimeException ex) {
      Logger.throwable(ex, "eatme Save operation completion evidence write failed: " + evidenceDir);
    }
  }

  static boolean isSaveActionInvocationProofOnly() {
    return Boolean.getBoolean(PROOF_ONLY_PROPERTY);
  }

  static void recordSaveActionInvocation(
      String operationClass,
      String extension,
      boolean activeStageIdeAvailable,
      boolean projectDocumentFrameAvailable) {
    String evidenceDir = System.getProperty(EVIDENCE_DIR_PROPERTY);
    if (evidenceDir == null || evidenceDir.isBlank()) {
      return;
    }
    try {
      writeSaveActionInvocationProof(
          Path.of(evidenceDir),
          operationClass,
          extension,
          activeStageIdeAvailable,
          projectDocumentFrameAvailable);
    } catch (IOException | RuntimeException ex) {
      Logger.throwable(ex, "eatme Save action invocation evidence write failed: " + evidenceDir);
    }
  }

  static Path write(
      Path evidenceDir,
      String operationClass,
      String extension,
      SaveOperationFlow.Result result) throws IOException {
    Objects.requireNonNull(evidenceDir, "evidenceDir");
    Objects.requireNonNull(result, "result");
    Files.createDirectories(evidenceDir);
    Path artifact = artifactPath(evidenceDir, ARTIFACT);
    Files.writeString(
        artifact,
        resultJson(operationClass, extension, result),
        StandardCharsets.UTF_8);
    if (!Files.isRegularFile(artifact) || Files.size(artifact) == 0) {
      throw new IOException("Save operation completion artifact was not written: " + artifact);
    }
    writeDialogControlTarget(evidenceDir, operationClass, extension, result);
    return artifact;
  }

  static Path writeDialogControlTarget(
      Path evidenceDir,
      String operationClass,
      String extension,
      SaveOperationFlow.Result result) throws IOException {
    Objects.requireNonNull(evidenceDir, "evidenceDir");
    Objects.requireNonNull(result, "result");
    Files.createDirectories(evidenceDir);
    Path artifact = artifactPath(evidenceDir, DIALOG_CONTROL_ARTIFACT);
    Files.writeString(
        artifact,
        dialogControlTargetJson(operationClass, extension, result),
        StandardCharsets.UTF_8);
    if (!Files.isRegularFile(artifact) || Files.size(artifact) == 0) {
      throw new IOException("Save dialog control target artifact was not written: " + artifact);
    }
    return artifact;
  }

  static Path writeSaveActionInvocationProof(
      Path evidenceDir,
      String operationClass,
      String extension,
      boolean activeStageIdeAvailable,
      boolean projectDocumentFrameAvailable) throws IOException {
    Objects.requireNonNull(evidenceDir, "evidenceDir");
    Files.createDirectories(evidenceDir);
    Path artifact = artifactPath(evidenceDir, SAVE_ACTION_INVOCATION_PROOF_ARTIFACT);
    Files.writeString(
        artifact,
        saveActionInvocationProofJson(
            operationClass,
            extension,
            activeStageIdeAvailable,
            projectDocumentFrameAvailable),
        StandardCharsets.UTF_8);
    if (!Files.isRegularFile(artifact) || Files.size(artifact) == 0) {
      throw new IOException("Save action invocation proof artifact was not written: " + artifact);
    }
    return artifact;
  }

  static String escapeJson(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '\\' -> escaped.append("\\\\");
        case '"' -> escaped.append("\\\"");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (ch < 0x20) {
            escaped.append(String.format("\\u%04x", (int) ch));
          } else {
            escaped.append(ch);
          }
        }
      }
    }
    return escaped.toString();
  }

  private static String resultJson(String operationClass, String extension, SaveOperationFlow.Result result) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-desktop-save-operation-result/v1\",\n"
        + "  \"status\": \"" + status(result) + "\",\n"
        + "  \"source\": \"AbstractSaveOperation.perform\",\n"
        + "  \"operation\": \"" + escapeJson(nullToBlank(operationClass)) + "\",\n"
        + "  \"extension\": \"" + escapeJson(nullToBlank(extension)) + "\",\n"
        + "  \"finished\": " + result.finished() + ",\n"
        + "  \"canceled\": " + result.canceled() + ",\n"
        + "  \"prompt_count\": " + result.promptCount() + ",\n"
        + "  \"save_attempts\": " + result.saveAttempts() + ",\n"
        + "  \"saved_file\": " + savedFileJson(result) + ",\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"desktop Save menu item was clicked\",\n"
        + "    \"Save dialog control\",\n"
        + "    \"full Alice UI automation\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"grading\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String dialogControlTargetJson(String operationClass, String extension, SaveOperationFlow.Result result) {
    boolean dialogWasRequested = result.promptCount() > 0;
    String status = dialogWasRequested ? "blocked" : "unsupported";
    String reason = dialogWasRequested
        ? "desktop_save_dialog_control_not_available"
        : "save_operation_did_not_request_dialog";
    String summary = dialogWasRequested
        ? "SaveOperationFlow requested the production Save dialog seam, but no desktop dialog discovery/control evidence exists yet."
        : "No Save dialog was requested, so this artifact cannot prove dialog discovery or control.";
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-desktop-save-dialog-control-target/v1\",\n"
        + "  \"status\": \"" + status + "\",\n"
        + "  \"reason\": \"" + reason + "\",\n"
        + "  \"source\": \"AbstractSaveOperation.perform\",\n"
        + "  \"operation\": \"" + escapeJson(nullToBlank(operationClass)) + "\",\n"
        + "  \"extension\": \"" + escapeJson(nullToBlank(extension)) + "\",\n"
        + "  \"result_status\": \"" + status(result) + "\",\n"
        + "  \"prompt_count\": " + result.promptCount() + ",\n"
        + "  \"save_attempts\": " + result.saveAttempts() + ",\n"
        + "  \"saved_file\": " + savedFileJson(result) + ",\n"
        + "  \"dialog_targets\": {\n"
        + "    \"desktop_frame\": \"org.lgna.croquet.DocumentFrame#showSaveFileDialog(File,String,String)\",\n"
        + "    \"native_chooser\": \"edu.cmu.cs.dennisc.java.awt.FileDialogUtilities#showSaveFileDialog(Component,File,String,String)\"\n"
        + "  },\n"
        + "  \"reporting_summary\": \"" + escapeJson(summary) + "\",\n"
        + "  \"missing_evidence\": [\n"
        + "    \"desktop Save dialog discovery\",\n"
        + "    \"desktop Save dialog control\",\n"
        + "    \"selected Save path supplied by UI automation\"\n"
        + "  ],\n"
        + "  \"requiresNextEvidence\": [\n"
        + "    \"desktop Save dialog owner/component artifact\",\n"
        + "    \"desktop Save dialog control result artifact\"\n"
        + "  ],\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"desktop Save menu item was clicked\",\n"
        + "    \"desktop Save dialog control\",\n"
        + "    \"full Alice UI automation\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"grading\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String saveActionInvocationProofJson(
      String operationClass,
      String extension,
      boolean activeStageIdeAvailable,
      boolean projectDocumentFrameAvailable) {
    String reason = saveActionInvocationReason(activeStageIdeAvailable, projectDocumentFrameAvailable);
    String status = switch (reason) {
      case "save_action_invoked" -> "action_invoked";
      case "missing_active_stage_ide" -> "unsupported";
      default -> "blocked";
    };
    String operationSimpleName = operationSimpleName(operationClass);
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-desktop-save-action-invocation-proof/v1\",\n"
        + "  \"status\": \"" + status + "\",\n"
        + "  \"reason\": \"" + reason + "\",\n"
        + "  \"source\": \"AbstractSaveOperation.perform\",\n"
        + "  \"operation\": \"" + escapeJson(nullToBlank(operationClass)) + "\",\n"
        + "  \"extension\": \"" + escapeJson(nullToBlank(extension)) + "\",\n"
        + "  \"target\": {\n"
        + "    \"action\": \"" + escapeJson(operationSimpleName) + ".getInstance().fire(UserActivity)\",\n"
        + "    \"menu_item\": \"" + escapeJson(operationSimpleName) + ".getInstance().getMenuItemPrepModel()\",\n"
        + "    \"dialog_path\": \"application.getDocumentFrame().showSaveFileDialog(directory, filename, extension)\",\n"
        + "    \"required_active_application\": \"org.alice.stageide.StageIDE.getActiveInstance()\"\n"
        + "  },\n"
        + "  \"observed\": {\n"
        + "    \"active_stage_ide_available\": " + activeStageIdeAvailable + ",\n"
        + "    \"project_document_frame_available\": " + projectDocumentFrameAvailable + "\n"
        + "  },\n"
        + "  \"blocker\": {\n"
        + "    \"observed\": \"" + escapeJson(saveActionObserved(reason)) + "\",\n"
        + "    \"required\": \"active StageIDE with ProjectDocumentFrame before AbstractSaveOperation can request the production Save dialog\"\n"
        + "  },\n"
        + "  \"reporting_summary\": \"" + escapeJson(saveActionReportingSummary(reason)) + "\",\n"
        + saveActionRequiresNextEvidenceJson(reason)
        + "  \"doesNotClaim\": [\n"
        + "    \"desktop Save menu item was clicked\",\n"
        + "    \"Save dialog displayed\",\n"
        + "    \"desktop Save dialog control\",\n"
        + "    \"selected Save path supplied by UI automation\",\n"
        + "    \"saved file completed\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"grading\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String saveActionInvocationReason(boolean activeStageIdeAvailable, boolean projectDocumentFrameAvailable) {
    if (!activeStageIdeAvailable) {
      return "missing_active_stage_ide";
    }
    if (!projectDocumentFrameAvailable) {
      return "missing_project_document_frame";
    }
    return "save_action_invoked";
  }

  private static String saveActionObserved(String reason) {
    return switch (reason) {
      case "missing_active_stage_ide" -> "SaveProjectOperation.fire(UserActivity) reached AbstractSaveOperation.perform, but StageIDE.getActiveInstance() returned null.";
      case "missing_project_document_frame" -> "StageIDE.getActiveInstance() resolved, but application.getDocumentFrame() returned null.";
      default -> "SaveProjectOperation.fire(UserActivity) reached AbstractSaveOperation.perform with an active StageIDE and ProjectDocumentFrame.";
    };
  }

  private static String saveActionReportingSummary(String reason) {
    return switch (reason) {
      case "missing_active_stage_ide" -> "The Save action invocation path is executable, but this JVM has no active StageIDE, so the production Save dialog path cannot resolve application.getDocumentFrame().showSaveFileDialog.";
      case "missing_project_document_frame" -> "The Save action invocation path found an active StageIDE, but no ProjectDocumentFrame was available to own the Save dialog.";
      default -> "The Save action invocation reached the production Save operation owner; dialog display/control still require FileDialogUtilities evidence.";
    };
  }

  private static String saveActionRequiresNextEvidenceJson(String reason) {
    if ("save_action_invoked".equals(reason)) {
      return "  \"requiresNextEvidence\": [\n"
          + "    \"desktop Save dialog discovery artifact with target_resolved\",\n"
          + "    \"desktop Save dialog control result artifact\",\n"
          + "    \"selected Save path supplied by UI automation\"\n"
          + "  ],\n";
    }
    if ("missing_project_document_frame".equals(reason)) {
      return "  \"requiresNextEvidence\": [\n"
          + "    \"invoke SaveProjectOperation from an initialized Alice desktop with a ProjectDocumentFrame\",\n"
          + "    \"desktop Save dialog discovery artifact with target_resolved\",\n"
          + "    \"desktop Save dialog control result artifact\",\n"
          + "    \"selected Save path supplied by UI automation\"\n"
          + "  ],\n";
    }
    return "  \"requiresNextEvidence\": [\n"
        + "    \"invoke SaveProjectOperation from a running Alice desktop with StageIDE.getActiveInstance() resolved\",\n"
        + "    \"desktop Save dialog discovery artifact with target_resolved\",\n"
        + "    \"desktop Save dialog control result artifact\",\n"
        + "    \"selected Save path supplied by UI automation\"\n"
        + "  ],\n";
  }

  private static String savedFileJson(SaveOperationFlow.Result result) {
    return result.savedFile() == null
        ? "null"
        : "\"" + escapeJson(result.savedFile().getPath()) + "\"";
  }

  private static String status(SaveOperationFlow.Result result) {
    if (result.finished()) {
      return "finished";
    }
    if (result.canceled()) {
      return "canceled";
    }
    return "incomplete";
  }

  private static String nullToBlank(String value) {
    return value == null ? "" : value;
  }

  private static String operationSimpleName(String operationClass) {
    String value = nullToBlank(operationClass);
    int lastDot = value.lastIndexOf('.');
    return lastDot >= 0 ? value.substring(lastDot + 1) : value;
  }

  private static Path artifactPath(Path evidenceDir, String artifactName) {
    Path artifact = evidenceDir.resolve(artifactName).normalize();
    if (!artifact.startsWith(evidenceDir.normalize())) {
      throw new IllegalArgumentException("Save operation artifact escapes evidence dir");
    }
    return artifact;
  }
}
