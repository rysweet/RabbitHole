package org.alice.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * All JSON artifact writing, validation, and atomic I/O for desktop Run execution evidence.
 */
final class EatmeEvidenceWriter {

  private static final String DESKTOP_RUN_EXECUTION_GAP_REPORT_SCHEMA =
      "eatme.alice-desktop-run-execution-gap-report/v1";
  static final List<String> REQUIRED_EXECUTION_GAP_EVIDENCE_ARTIFACTS = List.of(
      EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT,
      EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_BOUNDARY_ARTIFACT,
      EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT,
      EatmeDesktopRunExecutionEvidence.DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT,
      EatmeDesktopRunExecutionEvidence.DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT,
      EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_STATUS_SUMMARY_ARTIFACT);
  private static final Set<String> REQUIRED_EXECUTION_GAP_EVIDENCE_ARTIFACT_SET =
      Set.copyOf(REQUIRED_EXECUTION_GAP_EVIDENCE_ARTIFACTS);
  private static final List<String> SUPPORTING_VM_LISTENER_EVIDENCE_ARTIFACTS = List.of(
      EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_EXECUTION_ARTIFACT,
      EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_RUNTIME_LOG);
  private static final Set<String> SUPPORTING_VM_LISTENER_EVIDENCE_ARTIFACT_SET =
      Set.copyOf(SUPPORTING_VM_LISTENER_EVIDENCE_ARTIFACTS);
  private static final List<String> EXECUTION_GAP_PROHIBITED_CLAIM_CATEGORIES = List.of(
      "full world execution",
      "visible rendering correctness",
      "grading",
      "Save completion",
      "full UI automation");
  private static final Path VALIDATION_DIR = Path.of("evidence");

  private EatmeEvidenceWriter() {
  }

  // =========================================================================
  // Atomic I/O utilities
  // =========================================================================

  static void writeStringAtomically(Path target, String content) throws IOException {
    Path temp = target.resolveSibling(target.getFileName() + ".tmp");
    Files.writeString(temp, content, StandardCharsets.UTF_8);
    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  static void requireNonEmptyArtifact(Path path, String label) throws IOException {
    if (!Files.isRegularFile(path) || Files.size(path) == 0) {
      throw new IOException(label + " was not written: " + path);
    }
  }

  // =========================================================================
  // JSON formatting utilities
  // =========================================================================

  static String runtimeLog(String programTypeName, List<String> events) {
    int programTypeNameLength = programTypeName != null ? programTypeName.length() : 4;
    StringBuilder builder = new StringBuilder(96 + programTypeNameLength + (events.size() * 32));
    builder.append("schema_version=eatme.alice-desktop-run-execution-log/v1\n");
    builder.append("program_type=").append(programTypeName).append('\n');
    builder.append("recorded_at=").append(Instant.now()).append('\n');
    for (String event : events) {
      builder.append(event).append('\n');
    }
    return builder.toString();
  }

  static String jsonArray(List<String> values) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append('"').append(EatmeRunWindowEvidence.escapeJson(values.get(i))).append('"');
    }
    builder.append(']');
    return builder.toString();
  }

  static String pixelObservationSummary(PixelObservation pixelObservation) {
    return pixelObservation.isObserved()
        ? "desktop pixel sample observed; inspect the screenshot and sample details before reporting"
        : "desktop pixel sample blocked; inspect blocker details before reporting";
  }

  static String pixelObservationReportingNote(PixelObservation pixelObservation) {
    return pixelObservation.isObserved()
        ? "Report desktop-run-pixel-observation.json as observed only for pixel sampling, not visible rendering correctness."
        : "Report desktop-run-pixel-observation.json as blocked until its blocker details are resolved or separate manual evidence is supplied.";
  }

  // =========================================================================
  // Artifact writers
  // =========================================================================

  static Path writeDesktopRunExecution(
      Path evidenceDir,
      String programTypeName,
      boolean activeSceneInvokeStarted,
      boolean activeSceneInvokeReturned,
      int executingStatementCount,
      int executedStatementCount,
      String latestEvent,
      List<String> events) throws IOException {
    Files.createDirectories(evidenceDir);
    Path artifact = EatmeRunWindowEvidence.artifactPath(
        evidenceDir, EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_EXECUTION_ARTIFACT);
    Path runtimeLogPath = EatmeRunWindowEvidence.artifactPath(
        evidenceDir, EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_RUNTIME_LOG);
    writeStringAtomically(runtimeLogPath, runtimeLog(programTypeName, events));
    requireNonEmptyArtifact(runtimeLogPath, "desktop Run runtime log");
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-run-execution/v1\",\n"
            + "  \"status\": \"" + (executingStatementCount > 0 ? "statement_execution_observed" : "preparing") + "\",\n"
            + "  \"execution_mode\": \"desktop_run_frame_vm_listener\",\n"
            + "  \"program_type\": \"" + EatmeRunWindowEvidence.escapeJson(programTypeName) + "\",\n"
            + "  \"active_scene_invoke_started\": " + activeSceneInvokeStarted + ",\n"
            + "  \"active_scene_invoke_returned\": " + activeSceneInvokeReturned + ",\n"
            + "  \"executing_statement_count\": " + executingStatementCount + ",\n"
            + "  \"executed_statement_count\": " + executedStatementCount + ",\n"
            + "  \"latest_event\": \"" + EatmeRunWindowEvidence.escapeJson(latestEvent) + "\",\n"
            + "  \"runtime_log\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_RUNTIME_LOG + "\"\n"
            + "}\n");
    requireNonEmptyArtifact(artifact, "desktop Run execution artifact");
    return artifact;
  }

  static void writeFirstLessonNextActionContract(Path artifact) throws IOException {
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-first-lesson-next-action/v1\",\n"
            + "  \"status\": \"blocked\",\n"
            + "  \"source\": \"desktop_run_render_target_attachment\",\n"
            + "  \"evaluated_after\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT + "\",\n"
            + "  \"reporting_summary\": {\n"
            + "    \"observed_so_far\": \"Run window attachment evidence was recorded in desktop-run-render-affordance.json.\",\n"
            + "    \"pixel_status_source\": \"Read desktop-run-pixel-observation.json before reporting whether desktop pixels were sampled.\",\n"
            + "    \"next_action_status\": \"blocked\",\n"
            + "    \"missing_evidence\": [\n"
            + "      \"desktop Save menu readiness or invocation result\",\n"
            + "      \"code editor/procedure action readiness or invocation result\"\n"
            + "    ]\n"
            + "  },\n"
            + "  \"candidate_actions\": [\n"
            + "    \"desktop_save_menu_action\",\n"
            + "    \"desktop_code_editor_or_procedure_action\"\n"
            + "  ],\n"
            + "  \"blocker\": {\n"
            + "    \"reason\": \"The current Run window evidence hook records setup details only and does not receive or invoke a stable desktop Save menu or code editor/procedure action target.\",\n"
            + "    \"codes\": [\n"
            + "      \"desktop_save_menu_action_not_bound\",\n"
            + "      \"procedure_editor_action_not_bound\",\n"
            + "      \"no_ui_action_invoker_at_run_render_attachment\"\n"
            + "    ],\n"
            + "    \"details\": [\n"
            + "      {\n"
            + "        \"observed\": \"the current Run window attachment recorder receives component setup state only\",\n"
            + "        \"required\": \"stable desktop Save command/menu target plus invocation result\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"observed\": \"no code editor or procedure operation target is exposed at this seam\",\n"
            + "        \"required\": \"stable code editor/procedure action target plus invocation result\"\n"
            + "      }\n"
            + "    ]\n"
            + "  },\n"
            + "  \"requiresNextEvidence\": [\n"
            + "    \"desktop Save menu readiness or invocation artifact from the menu/action owner\",\n"
            + "    \"code editor/procedure action readiness or invocation artifact from the editor/action owner\"\n"
            + "  ],\n"
            + "  \"doesNotClaim\": [\n"
            + "    \"full Alice UI automation\",\n"
            + "    \"desktop save-menu completion\",\n"
            + "    \"code editor/procedure action completion\",\n"
            + "    \"first-lesson completion\",\n"
            + "    \"grading\",\n"
            + "    \"creative assessment\"\n"
            + "  ]\n"
            + "}\n");
  }

  static void writeSaveMenuActionTargetNoGo(Path artifact) throws IOException {
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-save-menu-action-target/v1\",\n"
            + "  \"status\": \"blocked\",\n"
            + "  \"source\": \"desktop_run_render_target_attachment\",\n"
            + "  \"target\": {\n"
            + "    \"menu_owner\": \"org.alice.ide.croquet.models.menubar.FileMenuModel#createModels\",\n"
            + "    \"operation\": \"org.alice.ide.croquet.models.projecturi.SaveProjectOperation.getInstance()\",\n"
            + "    \"menu_item\": \"SaveProjectOperation.getInstance().getMenuItemPrepModel()\",\n"
            + "    \"operation_uuid\": \"44ffba8a-3fb3-4cb5-97b6-55cd93c88e9d\"\n"
            + "  },\n"
            + "  \"blocker\": {\n"
            + "    \"reason\": \"The current evidence seam runs after Run render-target attachment and does not receive the desktop File menu owner, ProjectDocumentFrame, or SaveProjectOperation target needed to prove Save menu readiness or invocation.\",\n"
            + "    \"codes\": [\n"
            + "      \"desktop_save_menu_owner_not_available_at_render_attachment\",\n"
            + "      \"desktop_save_project_operation_not_invoked\",\n"
            + "      \"desktop_save_menu_readiness_not_observed\"\n"
            + "    ],\n"
            + "    \"details\": [\n"
            + "      {\n"
            + "        \"observed\": \"recordRenderTargetAttached receives render target, render panel, Run view, and control-panel attachment state only\",\n"
            + "        \"required\": \"FileMenuModel or ProjectDocumentFrame evidence that SaveProjectOperation.getInstance().getMenuItemPrepModel() is present and enabled\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"observed\": \"no SaveProjectOperation invocation result is emitted by this seam\",\n"
            + "        \"required\": \"SaveProjectOperation.getInstance() invocation attempt with a success, prompt, or blocked result artifact\"\n"
            + "      }\n"
            + "    ]\n"
            + "  },\n"
            + "  \"requiresNextEvidence\": [\n"
            + "    \"desktop File menu artifact from FileMenuModel showing SaveProjectOperation.getInstance().getMenuItemPrepModel() is installed\",\n"
            + "    \"desktop SaveProjectOperation.getInstance() readiness or invocation result from the menu/action owner\"\n"
            + "  ],\n"
            + "  \"doesNotClaim\": [\n"
            + "    \"full Alice UI automation\",\n"
            + "    \"visible rendering correctness\",\n"
            + "    \"desktop save-menu completion\",\n"
            + "    \"first-lesson completion\",\n"
            + "    \"grading\",\n"
            + "    \"creative assessment\"\n"
            + "  ]\n"
            + "}\n");
  }

  static void writeRunStatusSummary(Path artifact, PixelObservation pixelObservation) throws IOException {
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-run-status-summary/v1\",\n"
            + "  \"status\": \"partial\",\n"
            + "  \"source\": \"desktop_run_render_target_attachment\",\n"
            + "  \"pixel_observation_status\": \"" + pixelObservation.status + "\",\n"
            + "  \"artifact_statuses\": [\n"
            + "    {\n"
            + "      \"artifact\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"present\",\n"
            + "      \"reports\": \"Run view attachment evidence only\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"artifact\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_BOUNDARY_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"not_observed\",\n"
            + "      \"reports\": \"pixel validation is not part of the Run view attachment evidence\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"artifact\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"" + pixelObservation.status + "\",\n"
            + "      \"reports\": \"" + pixelObservationSummary(pixelObservation) + "\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"artifact\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"blocked\",\n"
            + "      \"reports\": \"desktop action evidence still missing\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"artifact\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"blocked\",\n"
            + "      \"reports\": \"Save menu readiness or invocation not observed here\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"missing_evidence\": [\n"
            + "    {\n"
            + "      \"artifact\": \"first-lesson-code-editor-action-proof.json\",\n"
            + "      \"evidence_present\": false,\n"
            + "      \"status\": \"not_written_by_desktop_run_evidence\",\n"
            + "      \"source_tool\": \"tools/eatme-edit-procedure\",\n"
            + "      \"reports\": \"first-lesson code-editor action proof evidence is separate from Run view attachment evidence\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"evidence\": \"SaveProjectOperation invocation result\",\n"
            + "      \"evidence_present\": false,\n"
            + "      \"status\": \"missing\",\n"
            + "      \"source_needed\": \"desktop File menu or SaveProjectOperation owner\",\n"
            + "      \"reports\": \"Save menu readiness or completion is not proven by the Save menu action-target artifact\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"reporting_note\": \"" + pixelObservationReportingNote(pixelObservation) + "\",\n"
            + "  \"observed_artifacts\": {\n"
            + "    \"run_attachment_observed\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT + "\",\n"
            + "    \"pixel_boundary\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_BOUNDARY_ARTIFACT + "\",\n"
            + "    \"pixel_observation\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT + "\",\n"
            + "    \"next_action\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT + "\",\n"
            + "    \"save_menu_action_target\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT + "\"\n"
            + "  },\n"
            + "  \"exact_next_user_action\": [\n"
            + "    \"Open desktop-run-pixel-observation.json and use its status plus blocker details before claiming desktop pixels were sampled.\",\n"
            + "    \"Open desktop-first-lesson-next-action.json to see which desktop action evidence is still missing.\",\n"
            + "    \"Open desktop-save-menu-action-target.json before claiming Save menu readiness or completion.\"\n"
            + "  ],\n"
            + "  \"remaining_unproven_behavior\": [\n"
            + "    \"visible rendering correctness\",\n"
            + "    \"desktop save-menu completion\",\n"
            + "    \"full Alice UI automation\",\n"
            + "    \"first-lesson completion\",\n"
            + "    \"learner-world grading\",\n"
            + "    \"creative assessment\"\n"
            + "  ]\n"
            + "}\n");
  }

  // =========================================================================
  // Gap report
  // =========================================================================

  static Path writeDesktopRunExecutionGapReport(
      Path evidenceDir,
      List<String> evidenceArtifacts,
      String blockerReason) throws IOException {
    validateDesktopRunExecutionGapReport(
        evidenceArtifacts,
        blockerReason,
        EXECUTION_GAP_PROHIBITED_CLAIM_CATEGORIES);
    Files.createDirectories(evidenceDir);
    Path artifact = EatmeRunWindowEvidence.artifactPath(
        evidenceDir,
        EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_EXECUTION_GAP_REPORT_ARTIFACT);
    writeStringAtomically(artifact, desktopRunExecutionGapReportJson(evidenceArtifacts, blockerReason));
    requireNonEmptyArtifact(artifact, "desktop Run execution gap report artifact");
    return artifact;
  }

  static void validateDesktopRunExecutionGapReport(
      List<String> evidenceArtifacts,
      String blockerReason,
      List<String> doesNotClaim) {
    if (evidenceArtifacts == null || evidenceArtifacts.isEmpty()) {
      throw new IllegalArgumentException("execution gap report requires bounded Run-window evidence artifacts");
    }
    Set<String> evidenceArtifactSet = new HashSet<>(evidenceArtifacts);
    for (String requiredArtifact : REQUIRED_EXECUTION_GAP_EVIDENCE_ARTIFACTS) {
      if (!evidenceArtifactSet.contains(requiredArtifact)) {
        throw new IllegalArgumentException(
            "execution gap report missing required evidence artifact: " + requiredArtifact);
      }
    }
    for (String evidenceArtifact : evidenceArtifacts) {
      EatmeRunWindowEvidence.artifactPath(VALIDATION_DIR, evidenceArtifact);
      if (SUPPORTING_VM_LISTENER_EVIDENCE_ARTIFACT_SET.contains(evidenceArtifact)) {
        throw new IllegalArgumentException(
            "execution gap report executableToday payload must not include VM-listener support artifact: "
                + evidenceArtifact);
      }
      if (!REQUIRED_EXECUTION_GAP_EVIDENCE_ARTIFACT_SET.contains(evidenceArtifact)) {
        throw new IllegalArgumentException(
            "execution gap report executableToday payload has unexpected evidence artifact: "
                + evidenceArtifact);
      }
    }
    if (blockerReason == null || blockerReason.isBlank()) {
      throw new IllegalArgumentException("execution gap report requires blockerToFullWorldExecution.reason");
    }
    if (doesNotClaim == null) {
      throw new IllegalArgumentException("execution gap report requires doesNotClaim categories");
    }
    Set<String> doesNotClaimSet = new HashSet<>(doesNotClaim);
    for (String prohibitedClaimCategory : EXECUTION_GAP_PROHIBITED_CLAIM_CATEGORIES) {
      if (!doesNotClaimSet.contains(prohibitedClaimCategory)) {
        throw new IllegalArgumentException(
            "execution gap report missing doesNotClaim category: " + prohibitedClaimCategory);
      }
    }
  }

  private static String desktopRunExecutionGapReportJson(
      List<String> evidenceArtifacts,
      String blockerReason) {
    return "{\n"
        + "  \"schema_version\": \"" + DESKTOP_RUN_EXECUTION_GAP_REPORT_SCHEMA + "\",\n"
        + "  \"report_kind\": \"desktop_run_execution_gap\",\n"
        + "  \"status\": \"blocked\",\n"
        + "  \"source\": \"desktop_run_render_target_attachment\",\n"
        + "  \"emitted_after\": \"" + EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_STATUS_SUMMARY_ARTIFACT + "\",\n"
        + "  \"executableToday\": {\n"
        + "    \"summary\": \"Existing tooling produces bounded Run-window evidence artifacts.\",\n"
        + "    \"evidenceArtifacts\": " + executionGapEvidenceArtifactsJson(evidenceArtifacts) + "\n"
        + "  },\n"
        + "  \"blockerToFullWorldExecution\": {\n"
        + "    \"reason\": \"" + EatmeRunWindowEvidence.escapeJson(blockerReason) + "\",\n"
        + "    \"missingProof\": \"deterministic_world_advance_through_full_runtime_execution\",\n"
        + "    \"requiredNextEvidence\": [\n"
        + "      \"stable runtime advancement oracle tied to the launched world\",\n"
        + "      \"deterministic evidence that expected world state changes occurred during Run\",\n"
        + "      \"reviewed criteria that distinguish artifact presence from actual world advancement\"\n"
        + "    ]\n"
        + "  },\n"
        + "  \"failClosedRequirements\": [\n"
        + "    \"required Run-window evidence artifact names must be present\",\n"
        + "    \"executableToday evidence list must be non-empty\",\n"
        + "    \"blockerToFullWorldExecution.reason must be non-empty\",\n"
        + "    \"doesNotClaim must include every prohibited claim category\"\n"
        + "  ],\n"
        + "  \"doesNotClaim\": " + jsonArray(EXECUTION_GAP_PROHIBITED_CLAIM_CATEGORIES) + "\n"
        + "}\n";
  }

  private static String executionGapEvidenceArtifactsJson(List<String> evidenceArtifacts) {
    StringBuilder builder = new StringBuilder(evidenceArtifacts.size() * 200 + 16);
    builder.append("[\n");
    for (int i = 0; i < evidenceArtifacts.size(); i++) {
      String evidenceArtifact = evidenceArtifacts.get(i);
      if (i > 0) {
        builder.append(",\n");
      }
      builder.append("      {\n")
          .append("        \"artifact\": \"")
          .append(EatmeRunWindowEvidence.escapeJson(evidenceArtifact))
          .append("\",\n")
          .append("        \"evidence\": \"")
          .append(EatmeRunWindowEvidence.escapeJson(executionGapEvidenceDescription(evidenceArtifact)))
          .append("\",\n")
          .append("        \"claimLimit\": \"")
          .append(EatmeRunWindowEvidence.escapeJson(executionGapClaimLimit(evidenceArtifact)))
          .append("\"\n")
          .append("      }");
    }
    builder.append("\n    ]");
    return builder.toString();
  }

  private static String executionGapEvidenceDescription(String evidenceArtifact) {
    return switch (evidenceArtifact) {
      case EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT -> "Run view attachment signal";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_BOUNDARY_ARTIFACT -> "Pixel validation boundary";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT -> "desktop pixel sample status or exact blocker";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT -> "next desktop action no-go contract";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT -> "Save menu target no-go contract";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_STATUS_SUMMARY_ARTIFACT -> "summary of bounded Run-window artifact statuses";
      default -> "bounded Run-window evidence artifact";
    };
  }

  private static String executionGapClaimLimit(String evidenceArtifact) {
    return switch (evidenceArtifact) {
      case EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT -> "Run view attachment evidence only";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_BOUNDARY_ARTIFACT ->
          "pixel validation is not part of Run view attachment evidence";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT ->
          "pixel sampling status is not visible rendering correctness";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT -> "desktop action evidence remains missing";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT ->
          "Save menu readiness or invocation is not observed here";
      case EatmeDesktopRunExecutionEvidence.DESKTOP_RUN_STATUS_SUMMARY_ARTIFACT -> "summary of partial evidence, not a completion proof";
      default -> "not a full world execution proof";
    };
  }
}
