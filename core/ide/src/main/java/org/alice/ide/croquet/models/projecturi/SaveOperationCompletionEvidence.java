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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EventObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Thin facade retaining constants, API signatures, and inner classes.
 * JSON builders live in {@link EvidenceJsonWriter}; file-system guards
 * live in {@link EvidenceFileOperations}.
 */
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
    File savedFile = result.savedFile();
    Path savedPath = savedFile == null ? null : savedFile.toPath();
    EvidenceFileOperations.RegularFileState savedFileState =
        EvidenceFileOperations.regularFileState(savedPath);
    Path artifact = EvidenceFileOperations.artifactPath(evidenceDir, ARTIFACT);
    Files.writeString(
        artifact,
        EvidenceJsonWriter.resultJson(operationClass, extension, result, savedFileState),
        StandardCharsets.UTF_8);
    EvidenceFileOperations.requireNonEmptyRegularFile(
        artifact, "Save operation completion artifact was not written");
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
    Path artifact = EvidenceFileOperations.artifactPath(evidenceDir, DIALOG_CONTROL_ARTIFACT);
    Files.writeString(
        artifact,
        EvidenceJsonWriter.dialogControlTargetJson(operationClass, extension, result),
        StandardCharsets.UTF_8);
    EvidenceFileOperations.requireNonEmptyRegularFile(
        artifact, "Save dialog control target artifact was not written");
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
    Path artifact = EvidenceFileOperations.artifactPath(
        evidenceDir, SAVE_ACTION_INVOCATION_PROOF_ARTIFACT);
    Files.writeString(
        artifact,
        EvidenceJsonWriter.saveActionInvocationProofJson(
            operationClass,
            extension,
            activeStageIdeAvailable,
            projectDocumentFrameAvailable,
            invocationTrigger),
        StandardCharsets.UTF_8);
    EvidenceFileOperations.requireNonEmptyRegularFile(
        artifact, "Save action invocation proof artifact was not written");
    return artifact;
  }

  static SaveProofEvidence saveProofEvidence(File targetFile, Path proofRoot) throws IOException {
    return new SaveProofEvidence(targetFile, proofRoot);
  }

  static Path configuredSaveProofArtifact(Path proofRoot) {
    Objects.requireNonNull(proofRoot, "proofRoot");
    Path canonicalProofRoot = EvidenceFileOperations.canonicalDirectory(proofRoot, "Save proof root");
    String configured = propertyOrEnv(SAVE_PROOF_EVIDENCE_PATH_PROPERTY, SAVE_PROOF_EVIDENCE_PATH_ENV);
    if (configured == null || configured.isBlank()) {
      return canonicalProofRoot.resolve(SAVE_PROOF_ARTIFACT);
    }
    Path artifact = Path.of(configured).toAbsolutePath().normalize();
    if (!SAVE_PROOF_ARTIFACT.equals(artifact.getFileName().toString())) {
      throw new IllegalArgumentException("Save proof evidence path must end with " + SAVE_PROOF_ARTIFACT);
    }
    Path parent = artifact.getParent();
    if (parent == null) {
      throw new IllegalArgumentException("Save proof evidence path must have a parent directory");
    }
    EvidenceFileOperations.requirePathUnderProofRoot(
        parent, canonicalProofRoot, "Save proof evidence path must stay under the proof root");
    try {
      Path canonicalParent = EvidenceFileOperations.canonicalDirectoryUnderProofRoot(
          parent,
          canonicalProofRoot,
          "Save proof evidence path must stay under the proof root");
      return canonicalParent.resolve(SAVE_PROOF_ARTIFACT);
    } catch (IOException ioe) {
      throw new IllegalArgumentException(
          "Save proof evidence parent must be a writable canonical directory", ioe);
    }
  }

  static String configuredSaveProofScenario() {
    String configured = propertyOrEnv(SAVE_PROOF_SCENARIO_PROPERTY, SAVE_PROOF_SCENARIO_ENV);
    String scenario = configured == null || configured.isBlank() ? SAVE_PROOF_SCENARIO : configured;
    if (!SAVE_PROOF_SCENARIO.equals(scenario)) {
      throw new IllegalArgumentException("Save proof scenario must be " + SAVE_PROOF_SCENARIO);
    }
    return scenario;
  }

  static String configuredSaveProofRunId() {
    String configured = propertyOrEnv(SAVE_PROOF_RUN_ID_PROPERTY, SAVE_PROOF_RUN_ID_ENV);
    String runId = configured == null || configured.isBlank()
        ? "standalone-" + UUID.randomUUID()
        : configured;
    if (!runId.matches("[A-Za-z0-9._-]+")) {
      throw new IllegalArgumentException("Save proof runId must be a non-empty safe token");
    }
    return runId;
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
          : EvidenceJsonWriter.className(source);
    }
    return new InvocationTrigger(
        EvidenceJsonWriter.className(trigger),
        viewController == null ? null : viewController.getClass().getName(),
        awtSourceClass);
  }

  static String escapeJson(String value) {
    return EvidenceJsonWriter.escapeJson(value);
  }

  // ── Inner classes (kept for backward-compatible qualified references) ──

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
    private final Path targetPath;
    private final String targetFileName;

    private SaveProofEvidence(File targetFile, Path proofRoot) throws IOException {
      this.targetFile = Objects.requireNonNull(targetFile, "targetFile");
      this.proofRoot = Objects.requireNonNull(proofRoot, "proofRoot").toRealPath();
      this.targetCanonicalPath = targetFile.getCanonicalPath();
      this.targetPath = Path.of(this.targetCanonicalPath).normalize();
      this.targetFileName = targetFile.getName();
      this.targetInsideProofRoot = proofContainsPath(this.targetPath);
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
      Path normalizedArtifact = artifact.toAbsolutePath().normalize();
      if (!SAVE_PROOF_ARTIFACT.equals(normalizedArtifact.getFileName().toString())) {
        throw new IllegalArgumentException("Save proof artifact must be " + SAVE_PROOF_ARTIFACT);
      }
      Path parent = normalizedArtifact.getParent();
      if (parent == null) {
        throw new IllegalArgumentException("Save proof artifact must have a parent directory");
      }
      EvidenceFileOperations.requirePathUnderProofRoot(
          parent, this.proofRoot, "Save proof artifact escapes proof root");
      Path canonicalParent = EvidenceFileOperations.canonicalDirectoryUnderProofRoot(
          parent, this.proofRoot, "Save proof artifact escapes proof root");
      Path canonicalArtifact = canonicalParent.resolve(SAVE_PROOF_ARTIFACT);
      if (Files.exists(canonicalArtifact, LinkOption.NOFOLLOW_LINKS)
          && Files.isSymbolicLink(canonicalArtifact)) {
        throw new IOException(
            "Save proof artifact refuses to overwrite symlink: " + canonicalArtifact);
      }
      Path tempArtifact = Files.createTempFile(canonicalParent, SAVE_PROOF_ARTIFACT, ".tmp");
      try {
        Files.writeString(tempArtifact, json(), StandardCharsets.UTF_8);
        Files.move(
            tempArtifact,
            canonicalArtifact,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } finally {
        Files.deleteIfExists(tempArtifact);
      }
      EvidenceFileOperations.requireNonEmptyRegularFile(
          canonicalArtifact, "Save proof artifact was not written");
      return canonicalArtifact;
    }

    boolean proofContainsPath(Path path) {
      return path != null && path.normalize().startsWith(this.proofRoot);
    }

    boolean recordSelectedFile(File selectedFile) throws IOException {
      this.normalizedSelectedFile = selectedFile == null ? null : selectedFile.getCanonicalPath();
      this.selectedFileVerified =
          this.normalizedSelectedFile != null
              && this.targetCanonicalPath.equals(this.normalizedSelectedFile);
      return this.selectedFileVerified && this.targetInsideProofRoot
          && this.targetFileName.endsWith(".a3p");
    }

    private String json() {
      return EvidenceJsonWriter.saveProofJson(snapshot());
    }

    private EvidenceJsonWriter.SaveProofSnapshot snapshot() {
      return new EvidenceJsonWriter.SaveProofSnapshot(
          this.robotFileMenuOpened,
          this.robotSaveItemClicked,
          this.saveActionIdentityMatched,
          this.chooserObserved,
          this.approvedSelection,
          this.ambiguousChooserDiscovery,
          this.selectedFileVerified,
          this.targetInsideProofRoot,
          this.dialogShowing,
          this.dialogClass,
          this.normalizedSelectedFile,
          this.pollCount,
          this.projectReadable,
          this.markerPresent,
          this.blockerKind,
          this.blockerObserved,
          this.blockerRequired,
          this.targetCanonicalPath,
          this.targetPath,
          this.targetFileName,
          this.proofRoot,
          configuredSaveProofScenario(),
          configuredSaveProofRunId());
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
