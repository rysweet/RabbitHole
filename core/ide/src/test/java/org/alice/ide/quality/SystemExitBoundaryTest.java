package org.alice.ide.quality;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class SystemExitBoundaryTest {
  private static final Pattern METHOD_NAME_PATTERN = Pattern.compile(
      "(?:public|protected|private|static|final|synchronized|native|strictfp|\\s)+[\\w<>\\[\\].?]+\\s+(\\w+)\\s*\\([^;{}]*\\)\\s*(?:throws\\s+[\\w\\s,.$]+)?\\{");

  @Test
  public void systemExitIsOnlyUsedFromMainEntryPoints() throws Exception {
    Path repositoryRoot = repositoryRoot();
    List<String> offenders = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(repositoryRoot)) {
      for (Path sourceFile : paths
          .filter(path -> path.toString().endsWith(".java"))
          .filter(path -> path.toString().contains("/src/main/java/"))
          .filter(path -> !path.toString().contains("/target/"))
          .toList()) {
        offenders.addAll(nonEntryPointSystemExitUses(repositoryRoot, sourceFile));
      }
    }

    assertTrue(
        "System.exit() must stay at true process entry points. Refactor library/framework exits to throw or return caller-controlled failures:\n"
            + String.join("\n", offenders),
        offenders.isEmpty());
  }

  private static List<String> nonEntryPointSystemExitUses(Path repositoryRoot, Path sourceFile) throws IOException {
    List<String> lines = Files.readAllLines(sourceFile);
    List<String> offenders = new ArrayList<>();
    boolean inBlockComment = false;

    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      String code = line;
      if (inBlockComment) {
        int end = code.indexOf("*/");
        if (end < 0) {
          continue;
        }
        code = code.substring(end + 2);
        inBlockComment = false;
      }
      while (code.contains("/*")) {
        int start = code.indexOf("/*");
        int end = code.indexOf("*/", start + 2);
        if (end < 0) {
          code = code.substring(0, start);
          inBlockComment = true;
          break;
        }
        code = code.substring(0, start) + code.substring(end + 2);
      }
      int lineComment = code.indexOf("//");
      if (lineComment >= 0) {
        code = code.substring(0, lineComment);
      }
      if (!code.contains("System.exit(")) {
        continue;
      }

      String methodName = enclosingMethodName(lines, i);
      if (!"main".equals(methodName)) {
        offenders.add(repositoryRoot.relativize(sourceFile) + ":" + (i + 1) + " in " + methodName);
      }
    }
    return offenders;
  }

  private static String enclosingMethodName(List<String> lines, int lineIndex) {
    StringBuilder signature = new StringBuilder();
    for (int i = lineIndex; i >= 0; i--) {
      String candidate = lines.get(i).strip();
      if (candidate.isEmpty() || candidate.startsWith("//")) {
        continue;
      }
      signature.insert(0, candidate + " ");
      Matcher matcher = METHOD_NAME_PATTERN.matcher(signature);
      if (matcher.find()) {
        return matcher.group(1);
      }
      if (candidate.endsWith(";")) {
        signature.setLength(0);
      }
    }
    return "<unknown>";
  }

  private static Path repositoryRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isDirectory(current.resolve(".github")) && Files.isRegularFile(current.resolve("pom.xml"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Could not locate repository root from " + System.getProperty("user.dir"));
  }
}
