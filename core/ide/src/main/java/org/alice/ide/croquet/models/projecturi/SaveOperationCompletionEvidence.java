package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.lgna.croquet.history.UserActivity;
import org.lgna.croquet.triggers.EventObjectTrigger;
import org.lgna.croquet.triggers.Trigger;
import org.lgna.croquet.views.MenuItem;
import org.lgna.croquet.views.ViewController;

import javax.swing.JMenuItem;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EventObject;
import java.util.Objects;
import java.util.UUID;

final class SaveOperationCompletionEvidence {
  static final String EVIDENCE_DIR_PROPERTY = "org.alice.eatme.saveOperationEvidenceDir";
  static final String PROOF_ONLY_PROPERTY = "org.alice.eatme.saveActionInvocationProofOnly";
  static final String ARTIFACT = "desktop-save-operation-result.json";
  static final String DIALOG_CONTROL_ARTIFACT = "desktop-save-dialog-control-target.json";
  static final String SAVE_ACTION_INVOCATION_PROOF_ARTIFACT = "desktop-save-action-invocation-proof.json";
  static final String SAVE_PROOF_ARTIFACT = "robot-save-menu-dialog-write-readback-proof.json";
  static final String SAVE_PROOF_SCHEMA_VERSION =
      "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1";
  static final String SAVE_PROOF_SCENARIO = "alice-desktop-save-menu-dialog-write-proof";
  static final String SAVE_PROOF_WORKFLOW = "save-menu-dialog-write-proof";
  static final String SAVE_PROOF_MARKER = "robotSaveMenuRoundTripMarker";
  static final String SAVE_PROOF_SCENARIO_PROPERTY = "org.alice.eatme.saveProof.scenario";
  static final String SAVE_PROOF_RUN_ID_PROPERTY = "org.alice.eatme.saveProof.runId";
  static final String SAVE_PROOF_EVIDENCE_PATH_PROPERTY = "org.alice.eatme.saveProof.evidencePath";
  static final String SAVE_PROOF_SCENARIO_ENV = "ALICE_SAVE_PROOF_SCENARIO";
  static final String SAVE_PROOF_RUN_ID_ENV = "ALICE_SAVE_PROOF_RUN_ID";
  static final String SAVE_PROOF_EVIDENCE_PATH_ENV = "ALICE_SAVE_PROOF_EVIDENCE_PATH";

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
    recordSaveActionInvocation(
        operationClass,
        extension,
        activeStageIdeAvailable,
        projectDocumentFrameAvailable,
        InvocationTrigger.none());
  }

  static void recordSaveActionInvocation(
      String operationClass,
      String extension,
      boolean activeStageIdeAvailable,
      boolean projectDocumentFrameAvailable,
      InvocationTrigger invocationTrigger) {
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
          projectDocumentFrameAvailable,
          invocationTrigger);
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
    return writeSaveActionInvocationProof(
        evidenceDir,
        operationClass,
        extension,
        activeStageIdeAvailable,
        projectDocumentFrameAvailable,
        InvocationTrigger.none());
  }

  static Path writeSaveActionInvocationProof(
      Path evidenceDir,
      String operationClass,
      String extension,
      boolean activeStageIdeAvailable,
      boolean projectDocumentFrameAvailable,
      InvocationTrigger invocationTrigger) throws IOException {
    Objects.requireNonNull(evidenceDir, "evidenceDir");
    Files.createDirectories(evidenceDir);
    Path artifact = artifactPath(evidenceDir, SAVE_ACTION_INVOCATION_PROOF_ARTIFACT);
    Files.writeString(
        artifact,
        saveActionInvocationProofJson(
            operationClass,
            extension,
            activeStageIdeAvailable,
            projectDocumentFrameAvailable,
            invocationTrigger),
        StandardCharsets.UTF_8);
    if (!Files.isRegularFile(artifact) || Files.size(artifact) == 0) {
      throw new IOException("Save action invocation proof artifact was not written: " + artifact);
    }
    return artifact;
  }

  static SaveProofEvidence saveProofEvidence(File targetFile, Path proofRoot) throws IOException {
    return new SaveProofEvidence(targetFile, proofRoot);
  }

  static Path configuredSaveProofArtifact(Path proofRoot) {
    String configured = propertyOrEnv(SAVE_PROOF_EVIDENCE_PATH_PROPERTY, SAVE_PROOF_EVIDENCE_PATH_ENV);
    if (configured == null || configured.isBlank()) {
      return proofRoot.resolve(SAVE_PROOF_ARTIFACT);
    }
    Path artifact = Path.of(configured).normalize();
    if (!SAVE_PROOF_ARTIFACT.equals(artifact.getFileName().toString())) {
      throw new IllegalArgumentException("Save proof evidence path must end with " + SAVE_PROOF_ARTIFACT);
    }
    return artifact;
  }

  static String configuredSaveProofScenario() {
    String configured = propertyOrEnv(SAVE_PROOF_SCENARIO_PROPERTY, SAVE_PROOF_SCENARIO_ENV);
    return configured == null || configured.isBlank() ? SAVE_PROOF_SCENARIO : configured;
  }

  static String configuredSaveProofRunId() {
    String configured = propertyOrEnv(SAVE_PROOF_RUN_ID_PROPERTY, SAVE_PROOF_RUN_ID_ENV);
    return configured == null || configured.isBlank()
        ? "standalone-" + UUID.randomUUID()
        : configured;
  }

  private static String propertyOrEnv(String propertyName, String envName) {
    String configured = System.getProperty(propertyName);
    if (configured != null && !configured.isBlank()) {
      return configured;
    }
    return System.getenv(envName);
  }

  static InvocationTrigger invocationTrigger(UserActivity activity) {
    if (activity == null || activity.getTrigger() == null) {
      return InvocationTrigger.none();
    }
    Trigger trigger = activity.getTrigger();
    ViewController<?, ?> viewController = trigger.getViewController();
    String awtSourceClass = null;
    if (trigger instanceof EventObjectTrigger<?> eventObjectTrigger) {
      EventObject event = eventObjectTrigger.getEvent();
      Object source = event == null ? null : event.getSource();
      awtSourceClass = source instanceof JMenuItem
          ? JMenuItem.class.getName()
          : className(source);
    }
    return new InvocationTrigger(
        className(trigger),
        viewController == null ? null : viewController.getClass().getName(),
        awtSourceClass);
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
    File savedFile = result.savedFile();
    Path savedPath = savedFile == null ? null : savedFile.toPath();
    boolean savedFileExists = savedPath != null && Files.isRegularFile(savedPath);
    Long fileSizeBytes = savedFileExists ? savedFileSizeBytes(savedPath) : null;
    boolean wroteFile = wroteFile(savedPath, fileSizeBytes, extension);
    String resultStatus = status(result);
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-desktop-save-operation-result/v1\",\n"
        + "  \"status\": \"" + resultStatus + "\",\n"
        + "  \"source\": \"AbstractSaveOperation.perform\",\n"
        + "  \"operation\": \"" + escapeJson(nullToBlank(operationClass)) + "\",\n"
        + "  \"extension\": \"" + escapeJson(nullToBlank(extension)) + "\",\n"
        + "  \"finished\": " + result.finished() + ",\n"
        + "  \"canceled\": " + result.canceled() + ",\n"
        + "  \"prompt_count\": " + result.promptCount() + ",\n"
        + "  \"save_attempts\": " + result.saveAttempts() + ",\n"
        + "  \"saved_file\": " + savedFileJson(savedFile) + ",\n"
        + "  \"saved_file_exists\": " + savedFileExistsJson(savedFile, savedFileExists) + ",\n"
        + "  \"saved_file_size_bytes\": " + savedFileSizeJson(fileSizeBytes) + ",\n"
        + "  \"dialogType\": \"Swing JFileChooser\",\n"
        + "  \"evidencePath\": \"Save dialog control/write path\",\n"
        + "  \"wroteFile\": " + wroteFile + ",\n"
        + "  \"fileExtension\": \"" + escapeJson(nullToBlank(extension)) + "\",\n"
        + resultClaimOrSummaryJson(wroteFile, resultStatus, extension)
        + "  \"doesNotClaim\": [\n"
        + "    \"desktop Save menu item was clicked\",\n"
        + "    \"full lesson completion\",\n"
        + "    \"full Alice UI automation\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"grading correctness\",\n"
        + "    \"broad UI automation coverage\",\n"
        + "    \"native dialog coverage\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String resultClaimOrSummaryJson(boolean wroteFile, String resultStatus, String extension) {
    String fileWrite = nonEmptyProjectFileWrite(extension);
    if (wroteFile) {
      return "  \"claim\": \"" + escapeJson("Save control/dialog approval reached " + fileWrite) + "\",\n";
    }
    return "  \"reporting_summary\": \""
        + escapeJson("Save operation evidence recorded status " + resultStatus + " without proving " + fileWrite)
        + "\",\n";
  }

  private static String nonEmptyProjectFileWrite(String extension) {
    if (extension == null || extension.isBlank()) {
      return "a non-empty project file write";
    }
    return "a non-empty ." + extension + " project file write";
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
        + "  \"saved_file\": " + savedFileJson(result.savedFile()) + ",\n"
        + "  \"dialog_targets\": {\n"
        + "    \"desktop_frame\": \"org.lgna.croquet.DocumentFrame#showSaveFileDialog(File,String,String)\",\n"
        + "    \"swing_file_chooser\": \"edu.cmu.cs.dennisc.java.awt.FileDialogUtilities#showSaveFileDialog(Component,File,String,String)\"\n"
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
      boolean projectDocumentFrameAvailable,
      InvocationTrigger invocationTrigger) {
    InvocationTrigger trigger = invocationTrigger == null ? InvocationTrigger.none() : invocationTrigger;
    String reason = saveActionInvocationReason(activeStageIdeAvailable, projectDocumentFrameAvailable, trigger);
    String status = switch (reason) {
      case "save_action_invoked" -> "action_invoked";
      case "save_menu_item_dispatched" -> "menu_item_dispatched";
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
        + "    \"project_document_frame_available\": " + projectDocumentFrameAvailable + ",\n"
        + "    \"menu_item_dispatch\": " + trigger.isMenuItemDispatch() + ",\n"
        + "    \"trigger_class\": " + stringJson(trigger.triggerClass()) + ",\n"
        + "    \"view_controller_class\": " + stringJson(trigger.viewControllerClass()) + ",\n"
        + "    \"awt_source_class\": " + stringJson(trigger.awtSourceClass()) + "\n"
        + "  },\n"
        + "  \"blocker\": {\n"
        + "    \"observed\": \"" + escapeJson(saveActionObserved(reason)) + "\",\n"
        + "    \"required\": \"active StageIDE with ProjectDocumentFrame before AbstractSaveOperation can request the production Save dialog\"\n"
        + "  },\n"
        + "  \"reporting_summary\": \"" + escapeJson(saveActionReportingSummary(reason)) + "\",\n"
        + saveActionRequiresNextEvidenceJson(reason)
        + saveActionDoesNotClaimJson(trigger)
        + "}\n";
  }

  private static String saveActionInvocationReason(
      boolean activeStageIdeAvailable,
      boolean projectDocumentFrameAvailable,
      InvocationTrigger invocationTrigger) {
    if (!activeStageIdeAvailable) {
      return "missing_active_stage_ide";
    }
    if (!projectDocumentFrameAvailable) {
      return "missing_project_document_frame";
    }
    if (invocationTrigger.isMenuItemDispatch()) {
      return "save_menu_item_dispatched";
    }
    return "save_action_invoked";
  }

  private static String saveActionObserved(String reason) {
    return switch (reason) {
      case "missing_active_stage_ide" -> "SaveProjectOperation.fire(UserActivity) reached AbstractSaveOperation.perform, but StageIDE.getActiveInstance() returned null.";
      case "missing_project_document_frame" -> "StageIDE.getActiveInstance() resolved, but application.getDocumentFrame() returned null.";
      case "save_menu_item_dispatched" -> "SaveProjectOperation Swing menu item doClick dispatched through OperationSwingModel and reached AbstractSaveOperation.perform with an active StageIDE and ProjectDocumentFrame.";
      default -> "SaveProjectOperation.fire(UserActivity) reached AbstractSaveOperation.perform with an active StageIDE and ProjectDocumentFrame.";
    };
  }

  private static String saveActionReportingSummary(String reason) {
    return switch (reason) {
      case "missing_active_stage_ide" -> "The Save action invocation path is executable, but this JVM has no active StageIDE, so the production Save dialog path cannot resolve application.getDocumentFrame().showSaveFileDialog.";
      case "missing_project_document_frame" -> "The Save action invocation path found an active StageIDE, but no ProjectDocumentFrame was available to own the Save dialog.";
      case "save_menu_item_dispatched" -> "The desktop Save menu item dispatch path reached the production Save operation owner; dialog display/control still require FileDialogUtilities evidence.";
      default -> "The Save action invocation reached the production Save operation owner; dialog display/control still require FileDialogUtilities evidence.";
    };
  }

  private static String saveActionRequiresNextEvidenceJson(String reason) {
    if ("save_action_invoked".equals(reason) || "save_menu_item_dispatched".equals(reason)) {
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

  private static String saveActionDoesNotClaimJson(InvocationTrigger invocationTrigger) {
    String menuItemClaim = invocationTrigger.isMenuItemDispatch()
        ? ""
        : "    \"desktop Save menu item was clicked\",\n";
    return "  \"doesNotClaim\": [\n"
        + menuItemClaim
        + "    \"Save dialog displayed\",\n"
        + "    \"desktop Save dialog control\",\n"
        + "    \"selected Save path supplied by UI automation\",\n"
        + "    \"saved file completed\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"grading\"\n"
        + "  ]\n";
  }

  private static String savedFileJson(File savedFile) {
    return savedFile == null
        ? "null"
        : "\"" + escapeJson(redactedSavedFilePath(savedFile)) + "\"";
  }

  private static String redactedSavedFilePath(File savedFile) {
    return redactedSavedPath(savedFile.toPath());
  }

  private static String redactedSavedPath(Path savedPath) {
    Path path = savedPath.normalize();
    if (!path.isAbsolute()) {
      return path.toString();
    }
    Path cwd = Path.of("").toAbsolutePath().normalize();
    if (path.startsWith(cwd)) {
      return cwd.relativize(path).toString();
    }
    Path fileName = path.getFileName();
    return "[redacted]/" + (fileName == null ? "" : fileName.toString());
  }

  private static String savedFileExistsJson(File savedFile, boolean savedFileExists) {
    return savedFile == null
        ? "null"
        : Boolean.toString(savedFileExists);
  }

  private static String savedFileSizeJson(Long savedFileSizeBytes) {
    return savedFileSizeBytes == null ? "null" : savedFileSizeBytes.toString();
  }

  private static Long savedFileSizeBytes(Path savedPath) {
    try {
      return Files.size(savedPath);
    } catch (IOException ioe) {
      Logger.throwable(ioe, "eatme Save operation completion evidence could not measure saved file: " + redactedSavedPath(savedPath));
      return null;
    }
  }

  private static boolean wroteFile(Path savedPath, Long savedFileSizeBytes, String extension) {
    if (!hasExtension(savedPath, extension)) {
      return false;
    }
    return savedFileSizeBytes != null && savedFileSizeBytes > 0;
  }

  private static boolean hasExtension(Path savedPath, String extension) {
    if (savedPath == null) {
      return false;
    }
    if (extension == null || extension.isBlank()) {
      return true;
    }
    Path fileName = savedPath.getFileName();
    return fileName != null && fileName.toString().endsWith("." + extension);
  }

  private static String stringJson(String value) {
    return value == null ? "null" : "\"" + escapeJson(value) + "\"";
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

  private static String className(Object value) {
    return value == null ? null : value.getClass().getName();
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

  static final class SaveProofEvidence {
    volatile boolean robotFileMenuOpened;
    volatile boolean robotSaveItemClicked;
    volatile boolean saveActionIdentityMatched;
    volatile boolean chooserObserved;
    volatile boolean approvedSelection;
    volatile boolean ambiguousChooserDiscovery;
    volatile boolean selectedFileVerified;
    volatile boolean targetInsideProofRoot;
    volatile boolean dialogShowing;
    volatile String dialogClass;
    volatile String normalizedSelectedFile;
    volatile int pollCount;
    volatile boolean projectReadable;
    volatile boolean markerPresent;
    volatile String blockerKind;
    volatile String blockerObserved;
    volatile String blockerRequired;

    private final File targetFile;
    private final Path proofRoot;
    private final String targetCanonicalPath;

    private SaveProofEvidence(File targetFile, Path proofRoot) throws IOException {
      this.targetFile = Objects.requireNonNull(targetFile, "targetFile");
      this.proofRoot = Objects.requireNonNull(proofRoot, "proofRoot").toRealPath();
      this.targetCanonicalPath = targetFile.getCanonicalPath();
    }

    void block(String kind, String observed, String required) {
      if (this.blockerKind == null) {
        this.blockerKind = kind;
        this.blockerObserved = observed;
        this.blockerRequired = required;
      }
    }

    void recordReadback(boolean projectReadable, boolean markerPresent) {
      this.projectReadable = projectReadable;
      this.markerPresent = markerPresent;
    }

    Path write(Path artifact) throws IOException {
      Path normalizedArtifact = artifact.normalize();
      if (!SAVE_PROOF_ARTIFACT.equals(normalizedArtifact.getFileName().toString())) {
        throw new IllegalArgumentException("Save proof artifact must be " + SAVE_PROOF_ARTIFACT);
      }
      Files.createDirectories(normalizedArtifact.getParent());
      Files.writeString(normalizedArtifact, json(), StandardCharsets.UTF_8);
      if (!Files.isRegularFile(normalizedArtifact) || Files.size(normalizedArtifact) == 0) {
        throw new IOException("Save proof artifact was not written: " + normalizedArtifact);
      }
      return normalizedArtifact;
    }

    boolean proofContainsPath(Path path) {
      return path != null && path.normalize().startsWith(this.proofRoot);
    }

    private String json() throws IOException {
      Path targetPath = Path.of(this.targetCanonicalPath).normalize();
      Path selectedPath = this.normalizedSelectedFile == null
          ? null
          : Path.of(this.normalizedSelectedFile).normalize();
      boolean fileExists = this.targetFile.isFile();
      long fileSizeBytes = fileExists ? this.targetFile.length() : 0;
      boolean fileNonempty = fileSizeBytes > 0;
      boolean fileHasExpectedExtension = this.targetFile.getName().endsWith(".a3p");
      boolean selectedFileMatchesExpected =
          this.normalizedSelectedFile != null && this.targetCanonicalPath.equals(this.normalizedSelectedFile);
      boolean observedWrite = fileExists && fileNonempty && fileHasExpectedExtension && this.targetInsideProofRoot;
      boolean proven = this.robotFileMenuOpened
          && this.robotSaveItemClicked
          && this.saveActionIdentityMatched
          && this.chooserObserved
          && this.dialogShowing
          && this.approvedSelection
          && !this.ambiguousChooserDiscovery
          && this.selectedFileVerified
          && selectedFileMatchesExpected
          && observedWrite
          && this.projectReadable
          && this.markerPresent
          && this.blockerKind == null;
      if (!proven && this.blockerKind == null) {
        block(inferBlockerKind(observedWrite),
            inferBlockerObserved(observedWrite),
            "A complete Robot File menu Save activation, rendered dialog approval, write, readback, and marker path");
      }
      String status = proven ? "proven" : "blocked";
      String claimOrSummary = proven
          ? "  \"claim\": \"AWT Robot opened File, clicked the production Save menu item, controlled the rendered Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified " + SAVE_PROOF_MARKER + "\",\n"
          : "  \"reportingSummary\": \"Robot File menu Save dialog/write/readback path was not proven; blocker.kind identifies the first missing or unsafe step.\",\n";
      return "{\n"
          + "  \"schemaVersion\": \"" + SAVE_PROOF_SCHEMA_VERSION + "\",\n"
          + "  \"scenario\": \"" + escapeJson(configuredSaveProofScenario()) + "\",\n"
          + "  \"workflow\": \"" + SAVE_PROOF_WORKFLOW + "\",\n"
          + "  \"runId\": \"" + escapeJson(configuredSaveProofRunId()) + "\",\n"
          + "  \"generatedAtUtc\": \"" + Instant.now() + "\",\n"
          + "  \"status\": \"" + status + "\",\n"
          + "  \"proofTarget\": \"single rendered desktop Save path: menu, dialog, control, write, readback\",\n"
          + claimOrSummary
          + blockerJson(proven)
          + "  \"menu\": {\n"
          + "    \"fileMenuOpened\": " + this.robotFileMenuOpened + ",\n"
          + "    \"saveMenuItemInvoked\": " + this.robotSaveItemClicked + ",\n"
          + "    \"saveActionIdentityMatched\": " + this.saveActionIdentityMatched + "\n"
          + "  },\n"
          + "  \"dialog\": {\n"
          + "    \"saveDialogObserved\": " + this.chooserObserved + ",\n"
          + "    \"dialogType\": \"Swing JFileChooser\",\n"
          + "    \"dialogClass\": " + stringJson(this.dialogClass) + ",\n"
          + "    \"dialogShowing\": " + this.dialogShowing + ",\n"
          + "    \"ambiguousChooserDiscovery\": " + this.ambiguousChooserDiscovery + ",\n"
          + "    \"pollCount\": " + this.pollCount + "\n"
          + "  },\n"
          + "  \"control\": {\n"
          + "    \"selectedPathSet\": " + this.selectedFileVerified + ",\n"
          + "    \"approvedSelection\": " + this.approvedSelection + ",\n"
          + "    \"selectedPathMatchesExpected\": " + selectedFileMatchesExpected + ",\n"
          + "    \"targetInsideProofRoot\": " + this.targetInsideProofRoot + ",\n"
          + "    \"normalizedSelectedPath\": " + stringJson(proofRelativePath(selectedPath)) + ",\n"
          + "    \"expectedPath\": " + stringJson(proofRelativePath(targetPath)) + "\n"
          + "  },\n"
          + "  \"write\": {\n"
          + "    \"fileWritten\": " + (proven && fileExists) + ",\n"
          + "    \"fileNonempty\": " + (proven && fileNonempty) + ",\n"
          + "    \"fileHasExpectedExtension\": " + fileHasExpectedExtension + ",\n"
          + "    \"outputPath\": " + stringJson(proofRelativePath(targetPath)) + ",\n"
          + "    \"outputSizeBytes\": " + fileSizeBytes + "\n"
          + "  },\n"
          + "  \"readback\": {\n"
          + "    \"projectReadable\": " + (proven && this.projectReadable) + ",\n"
          + "    \"marker\": \"" + SAVE_PROOF_MARKER + "\",\n"
          + "    \"markerPresent\": " + (proven && this.markerPresent) + "\n"
          + "  },\n"
          + "  \"baselinePreserved\": [\n"
          + "    \"StageIdeSaveMenuDoClickToWriteProofTest\",\n"
          + "    \"ProjectApplicationSaveProjectToTest\",\n"
          + "    \"JMenuBarRobotClickSaveProofTest\"\n"
          + "  ],\n"
          + "  \"requiresNextEvidence\": [\n"
          + "    \"Run under xvfb-run -a or an equivalent desktop session when blocker.kind is environment-related\",\n"
          + "    \"Use status proven only when Robot menu activation, dialog control, write, readback, and marker verification all succeed in one rendered path\"\n"
          + "  ],\n"
          + "  \"doesNotClaim\": [\n"
          + "    \"Save As coverage\",\n"
          + "    \"all Save variants\",\n"
          + "    \"full lesson completion\",\n"
          + "    \"visible rendering correctness\",\n"
          + "    \"grading correctness\",\n"
          + "    \"physical user click\",\n"
          + "    \"broad UI automation coverage\",\n"
          + "    \"native dialog coverage\"\n"
          + "  ]\n"
          + "}\n";
    }

    private String inferBlockerKind(boolean observedWrite) {
      if (!this.robotFileMenuOpened) {
        return "file_menu_not_showing";
      }
      if (!this.robotSaveItemClicked || !this.saveActionIdentityMatched) {
        return "save_item_not_attributed";
      }
      if (!this.chooserObserved) {
        return "dialog_not_observed";
      }
      if (this.ambiguousChooserDiscovery) {
        return "ambiguous_chooser_discovery";
      }
      if (!this.dialogShowing || !this.selectedFileVerified || !this.approvedSelection) {
        return "chooser_control_failed";
      }
      if (!this.targetInsideProofRoot || !this.targetFile.getName().endsWith(".a3p")) {
        return "target_path_rejected";
      }
      if (!observedWrite) {
        return "write_not_observed";
      }
      if (!this.projectReadable) {
        return "readback_failed";
      }
      return "marker_missing";
    }

    private String inferBlockerObserved(boolean observedWrite) {
      if (!this.robotFileMenuOpened) {
        return "The rendered File menu was not opened by Robot";
      }
      if (!this.robotSaveItemClicked || !this.saveActionIdentityMatched) {
        return "The production Save item click was not attributed to Robot";
      }
      if (!this.chooserObserved) {
        return "No live Swing JFileChooser was observed";
      }
      if (this.ambiguousChooserDiscovery) {
        return "Multiple live Swing JFileChoosers were observed";
      }
      if (!this.dialogShowing || !this.selectedFileVerified || !this.approvedSelection) {
        return "The live Save chooser could not be safely controlled";
      }
      if (!this.targetInsideProofRoot || !this.targetFile.getName().endsWith(".a3p")) {
        return "The selected Save target was outside the proof root or not an .a3p file";
      }
      if (!observedWrite) {
        return "No non-empty .a3p write was observed at the controlled target";
      }
      if (!this.projectReadable) {
        return "The written .a3p file could not be read back as an Alice project";
      }
      return "The readback project did not contain " + SAVE_PROOF_MARKER;
    }

    private String blockerJson(boolean proven) {
      if (proven) {
        return "  \"blocker\": null,\n";
      }
      return "  \"blocker\": {\n"
          + "    \"kind\": \"" + escapeJson(this.blockerKind) + "\",\n"
          + "    \"observed\": \"" + escapeJson(nullToBlank(this.blockerObserved)) + "\",\n"
          + "    \"required\": \"" + escapeJson(nullToBlank(this.blockerRequired)) + "\"\n"
          + "  },\n";
    }

    private String proofRelativePath(Path path) throws IOException {
      if (path == null) {
        return null;
      }
      if (!proofContainsPath(path)) {
        return "[outside-proof-root]";
      }
      return this.proofRoot.relativize(path).toString().replace(File.separatorChar, '/');
    }
  }

  static final class InvocationTrigger {
    private final String triggerClass;
    private final String viewControllerClass;
    private final String awtSourceClass;

    private InvocationTrigger(String triggerClass, String viewControllerClass, String awtSourceClass) {
      this.triggerClass = triggerClass;
      this.viewControllerClass = viewControllerClass;
      this.awtSourceClass = awtSourceClass;
    }

    static InvocationTrigger none() {
      return new InvocationTrigger(null, null, null);
    }

    String triggerClass() {
      return triggerClass;
    }

    String viewControllerClass() {
      return viewControllerClass;
    }

    String awtSourceClass() {
      return awtSourceClass;
    }

    boolean isMenuItemDispatch() {
      return "org.lgna.croquet.triggers.ActionEventTrigger".equals(triggerClass)
          && MenuItem.class.getName().equals(viewControllerClass)
          && JMenuItem.class.getName().equals(awtSourceClass);
    }
  }
}
