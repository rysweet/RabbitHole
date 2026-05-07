package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.util.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class SaveOperationCompletionEvidence {
  static final String EVIDENCE_DIR_PROPERTY = "org.alice.eatme.saveOperationEvidenceDir";
  static final String ARTIFACT = "desktop-save-operation-result.json";

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

  static Path write(
      Path evidenceDir,
      String operationClass,
      String extension,
      SaveOperationFlow.Result result) throws IOException {
    Objects.requireNonNull(evidenceDir, "evidenceDir");
    Objects.requireNonNull(result, "result");
    Files.createDirectories(evidenceDir);
    Path artifact = evidenceDir.resolve(ARTIFACT).normalize();
    if (!artifact.startsWith(evidenceDir.normalize())) {
      throw new IllegalArgumentException("Save operation artifact escapes evidence dir");
    }
    Files.writeString(
        artifact,
        resultJson(operationClass, extension, result),
        StandardCharsets.UTF_8);
    if (!Files.isRegularFile(artifact) || Files.size(artifact) == 0) {
      throw new IOException("Save operation completion artifact was not written: " + artifact);
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
}
