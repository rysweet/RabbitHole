package org.alice.ide;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class SystemExitBoundaryTest {
  private static final Set<String> APPROVED_SYSTEM_EXIT_FILES = Set.of(
      "alice-ide/src/main/java/org/alice/stageide/EntryPoint.java",
      "core/ide/src/main/java/org/alice/tools/EatmeSaveProject.java",
      "core/ide/src/main/java/org/alice/tools/EatmePlaceObject.java",
      "core/ide/src/main/java/org/alice/tools/EatmeEditProcedure.java",
      "core/ide/src/main/java/org/alice/tools/EatmeReopenProject.java",
      "core/ide/src/main/java/org/alice/tools/EatmeRunWorld.java");

  @Test
  public void productionSystemExitCallsAreRestrictedToApprovedEntryPoints() throws IOException {
    Path repositoryRoot = findRepositoryRoot();
    List<String> offenders = new ArrayList<>();

    try (var paths = Files.walk(repositoryRoot)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .filter(SystemExitBoundaryTest::isProductionJavaSource)
          .filter(path -> containsSystemExit(path))
          .map(path -> normalize(repositoryRoot.relativize(path)))
          .filter(path -> !APPROVED_SYSTEM_EXIT_FILES.contains(path))
          .forEach(offenders::add);
    }
    Collections.sort(offenders);

    assertTrue(
        "Only approved process entry points may call System.exit directly. "
            + "Move reusable termination paths to ProcessTerminator.requestExit: "
            + offenders,
        offenders.isEmpty());
  }

  @Test
  public void allowlistNamesExistingEntryPointFilesOnly() throws IOException {
    Path repositoryRoot = findRepositoryRoot();
    Set<String> missing = new HashSet<>();
    Set<String> withoutExit = new HashSet<>();

    for (String approvedFile : APPROVED_SYSTEM_EXIT_FILES) {
      Path path = repositoryRoot.resolve(approvedFile);
      if (!Files.isRegularFile(path)) {
        missing.add(approvedFile);
      } else if (!containsSystemExit(path)) {
        withoutExit.add(approvedFile);
      }
    }

    assertTrue("System.exit allowlist contains missing files: " + missing, missing.isEmpty());
    assertTrue("System.exit allowlist entries must be true process boundaries that call System.exit: " + withoutExit, withoutExit.isEmpty());
  }

  private static boolean isProductionJavaSource(Path path) {
    String normalized = normalize(path);
    return normalized.contains("/src/main/java/");
  }

  private static boolean containsSystemExit(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8).contains("System.exit(");
    } catch (IOException ioe) {
      throw new AssertionError("Unable to read " + path, ioe);
    }
  }

  private static Path findRepositoryRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      if (Files.exists(current.resolve(".git")) && Files.isRegularFile(current.resolve("pom.xml"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new AssertionError("Could not find repository root from " + System.getProperty("user.dir"));
  }

  private static String normalize(Path path) {
    return path.toString().replace('\\', '/');
  }
}
