package org.alice.tools;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.lgna.croquet.views.Frame;
import org.lgna.project.ast.NamedUserType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EatmeRunWindowEvidence {
  public static final String EVIDENCE_DIR_PROPERTY = "org.alice.eatme.runWindowEvidenceDir";
  public static final String RUN_WINDOW_CREATED_ARTIFACT = "run-window-created.json";
  private static final String SCHEMA_VERSION = "eatme.alice-run-window-created/v1";
  private static final String CONTRACT_SCOPE = "run-window-creation-wiring";
  private static final String EVIDENCE_SOURCE = "org.alice.stageide.run.RunComposite#handlePreShowWindow";
  private static final String[] DOES_NOT_CLAIM = {
      "active-rendering",
      "run-execution",
      "world-execution-correctness",
      "rendering-correctness",
      "save",
      "grading",
      "full-ui-automation"
  };

  private EatmeRunWindowEvidence() {
  }

  public static void recordRunWindowCreated(Frame frame, NamedUserType programType) {
    String evidenceDir = System.getProperty(EVIDENCE_DIR_PROPERTY);
    if (evidenceDir == null || evidenceDir.isBlank()) {
      return;
    }
    try {
      writeRunWindowCreated(Path.of(evidenceDir), title(frame), typeName(programType));
    } catch (IOException | SecurityException | IllegalArgumentException ex) {
      Logger.throwable(ex, "eatme Run-window evidence write failed: " + evidenceDir);
    }
  }

  static Path writeRunWindowCreated(Path evidenceDir, String frameTitle, String programTypeName) throws IOException {
    Files.createDirectories(evidenceDir);
    Path artifact = artifactPath(evidenceDir, RUN_WINDOW_CREATED_ARTIFACT);
    Files.writeString(
        artifact,
        "{\n"
            + "  \"schema_version\": \"" + SCHEMA_VERSION + "\",\n"
            + "  \"status\": \"created\",\n"
            + "  \"contract_scope\": \"" + CONTRACT_SCOPE + "\",\n"
            + "  \"evidence_source\": \"" + EVIDENCE_SOURCE + "\",\n"
            + "  \"artifact\": \"" + RUN_WINDOW_CREATED_ARTIFACT + "\",\n"
            + "  \"frame_title\": \"" + escapeJson(frameTitle) + "\",\n"
            + "  \"program_type\": \"" + escapeJson(programTypeName) + "\",\n"
            + "  \"active_rendering_claimed\": false,\n"
            + "  \"run_program_claimed\": false,\n"
            + "  \"run_execution_claimed\": false,\n"
            + "  \"world_execution_claimed\": false,\n"
            + "  \"rendering_correctness_claimed\": false,\n"
            + "  \"save_claimed\": false,\n"
            + "  \"grading_claimed\": false,\n"
            + "  \"full_ui_automation_claimed\": false,\n"
            + doesNotClaimJson()
            + "}\n",
        StandardCharsets.UTF_8);
    if (!Files.isRegularFile(artifact) || Files.size(artifact) == 0) {
      throw new IOException("Run-window evidence artifact was not written: " + artifact);
    }
    return artifact;
  }

  static Path artifactPath(Path evidenceDir, String relativePath) {
    Path path = Path.of(relativePath);
    Path normalized = path.normalize();
    if (path.isAbsolute()
        || normalized.toString().isEmpty()
        || normalized.getNameCount() != 1
        || normalized.startsWith("..")) {
      throw new IllegalArgumentException("artifact path must be a single relative file name: " + relativePath);
    }
    Path normalizedEvidenceDir = evidenceDir.normalize();
    Path resolved = normalizedEvidenceDir.resolve(normalized).normalize();
    if (!resolved.startsWith(normalizedEvidenceDir)) {
      throw new IllegalArgumentException("artifact path escapes evidence dir: " + relativePath);
    }
    return resolved;
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

  private static String title(Frame frame) {
    return frame != null ? frame.getTitle() : "";
  }

  static String typeName(NamedUserType programType) {
    return programType != null ? programType.getName() : "";
  }
}
