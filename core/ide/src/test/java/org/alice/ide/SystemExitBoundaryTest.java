package org.alice.ide;

import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemExitBoundaryTest {
  private static final Set<String> APPROVED_SYSTEM_EXIT_FILES = Set.of(
      "alice-ide/src/main/java/org/alice/stageide/EntryPoint.java",
      "core/ide/src/main/java/org/alice/tools/EatmeSaveProject.java",
      "core/ide/src/main/java/org/alice/tools/EatmePlaceObject.java",
      "core/ide/src/main/java/org/alice/tools/EatmeEditProcedure.java",
      "core/ide/src/main/java/org/alice/tools/EatmeReopenProject.java",
      "core/ide/src/main/java/org/alice/tools/EatmeRunWorld.java",
      "core/ide/src/main/java/org/alice/tools/EatmeTransformObject.java",
      "core/ide/src/main/java/org/alice/tools/EatmeObjectTransformWorkflow.java");

  @Test
  public void productionSystemExitCallsAreRestrictedToApprovedEntryPoints() throws IOException {
    Path repositoryRoot = findRepositoryRoot();
    List<String> offenders = collectOffenders(repositoryRoot);
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

  /**
   * Walks the canonical repository sources, pruning non-canonical trees (sibling worktrees, build
   * output, and nested git working trees / repositories) so that copies of the source tree created
   * by parallel worktree runs do not produce false System.exit offenders.
   */
  static List<String> collectOffenders(Path repositoryRoot) throws IOException {
    List<String> offenders = new ArrayList<>();
    Files.walkFileTree(repositoryRoot, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (isExcludedDirectory(repositoryRoot, dir)) {
          return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (attrs.isRegularFile()
            && file.getFileName().toString().endsWith(".java")
            && isProductionJavaSource(file)
            && containsSystemExit(file)) {
          String relative = normalize(repositoryRoot.relativize(file));
          if (!APPROVED_SYSTEM_EXIT_FILES.contains(relative)) {
            offenders.add(relative);
          }
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
      }
    });
    return offenders;
  }

  /**
   * Classifies a directory as a non-canonical tree that must be pruned from the scan. The
   * repository root itself is never excluded (its own {@code .git} must not trigger exclusion).
   */
  private static boolean isExcludedDirectory(Path repositoryRoot, Path dir) {
    if (dir.equals(repositoryRoot)) {
      return false;
    }
    String relative = normalize(repositoryRoot.relativize(dir));
    if (isExcludedRelativePath(relative)) {
      return true;
    }
    // A nested git working tree (linked worktree) has a .git file; a nested repository has a .git
    // directory. Either way the subtree is not part of the canonical sources.
    return Files.exists(dir.resolve(".git"));
  }

  /**
   * Pure, directly unit-testable predicate: returns {@code true} when the given repo-root-relative
   * path (forward-slash separated) sits under a pruned tree, matched on whole path segments so that
   * legitimately named files are never falsely excluded.
   */
  static boolean isExcludedRelativePath(String repoRelativePath) {
    if (repoRelativePath == null || repoRelativePath.isEmpty()) {
      return false;
    }
    for (String segment : repoRelativePath.split("/")) {
      if (segment.equals("worktrees") || segment.equals("target")) {
        return true;
      }
    }
    return false;
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

  // --- Regression coverage for the worktree/build-output exclusion (issue #998) ---

  @Test
  public void isExcludedRelativePathPrunesWorktreesAndTargetSegments() {
    // Copies of the source tree created by parallel git worktrees, and build output, are pruned.
    assertTrue(isExcludedRelativePath("worktrees/feat/x/core/ide/src/main/java/p/A.java"));
    assertTrue(isExcludedRelativePath("core/ide/target/generated-sources/p/A.java"));
    assertTrue(isExcludedRelativePath("worktrees"));
    assertTrue(isExcludedRelativePath("core/ide/target"));

    // Canonical sources are never excluded.
    assertFalse(isExcludedRelativePath("core/ide/src/main/java/org/alice/tools/EatmeRunWorld.java"));

    // Whole-segment matching: similarly named directories must NOT be excluded.
    assertFalse(isExcludedRelativePath("worktrees-archive/src/main/java/p/A.java"));
    assertFalse(isExcludedRelativePath("core/targeting/src/main/java/p/A.java"));

    assertFalse(isExcludedRelativePath(""));
    assertFalse(isExcludedRelativePath(null));
  }

  @Test
  public void collectOffendersIgnoresSiblingWorktreesTargetAndNestedGitTrees() throws IOException {
    Path root = Files.createTempDirectory("sysexit-boundary-");
    try {
      // Canonical offender: a production source that calls System.exit and is not allow-listed.
      writeJavaWithExit(root.resolve("somemodule/src/main/java/org/example/Real.java"));

      // 1) A sibling worktree copy of the sources — must be pruned.
      writeJavaWithExit(root.resolve("worktrees/feat/x/somemodule/src/main/java/org/example/Fake.java"));
      // 2) Build output — must be pruned.
      writeJavaWithExit(root.resolve("somemodule/target/generated-sources/org/example/Gen.java"));
      // 3) A nested git working tree (marked by a .git file) — must be pruned.
      Path nested = root.resolve("nested-checkout");
      Files.createDirectories(nested);
      Files.writeString(nested.resolve(".git"), "gitdir: /somewhere/else\n");
      writeJavaWithExit(nested.resolve("somemodule/src/main/java/org/example/Nested.java"));

      List<String> offenders = collectOffenders(root);

      assertTrue(
          "the canonical offender must be detected: " + offenders,
          offenders.contains("somemodule/src/main/java/org/example/Real.java"));
      assertEquals(
          "only the canonical offender should be reported (worktrees/target/nested-.git pruned): "
              + offenders,
          1,
          offenders.size());
    } finally {
      deleteRecursively(root);
    }
  }

  private static void writeJavaWithExit(Path file) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(
        file,
        "package org.example;\n"
            + "public final class Sample {\n"
            + "  public static void main(String[] args) { System.exit(0); }\n"
            + "}\n");
  }

  private static void deleteRecursively(Path root) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    try (var paths = Files.walk(root)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException ioe) {
                  throw new UncheckedIOException(ioe);
                }
              });
    }
  }
}
