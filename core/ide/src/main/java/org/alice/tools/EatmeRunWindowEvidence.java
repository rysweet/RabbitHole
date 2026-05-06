package org.alice.tools;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.lgna.croquet.views.Frame;
import org.lgna.project.ast.NamedUserType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class EatmeRunWindowEvidence {
  public static final String EVIDENCE_DIR_PROPERTY = "org.alice.eatme.runWindowEvidenceDir";
  public static final String RUN_WINDOW_CREATED_ARTIFACT = "run-window-created.json";

  private EatmeRunWindowEvidence() {
  }

  public static void recordRunWindowCreated(Frame frame, NamedUserType programType) {
    String evidenceDir = System.getProperty(EVIDENCE_DIR_PROPERTY);
    if (evidenceDir == null || evidenceDir.isBlank()) {
      return;
    }
    try {
      writeRunWindowCreated(Path.of(evidenceDir), title(frame), typeName(programType));
    } catch (IOException | InvalidPathException | SecurityException ex) {
      Logger.throwable(ex, "eatme Run-window evidence write failed: " + evidenceDir);
    }
  }

  static Path writeRunWindowCreated(Path evidenceDir, String frameTitle, String programTypeName) throws IOException {
    Files.createDirectories(evidenceDir);
    Path artifact = artifactPath(evidenceDir, RUN_WINDOW_CREATED_ARTIFACT);
    Files.writeString(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-run-window-created/v1\",\n"
            + "  \"status\": \"created\",\n"
            + "  \"frame_title\": \"" + escapeJson(frameTitle) + "\",\n"
            + "  \"program_type\": \"" + escapeJson(programTypeName) + "\"\n"
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
    Path resolved = evidenceDir.resolve(normalized).normalize();
    if (!resolved.startsWith(evidenceDir)) {
      throw new IllegalArgumentException("artifact path escapes evidence dir: " + relativePath);
    }
    return resolved;
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
