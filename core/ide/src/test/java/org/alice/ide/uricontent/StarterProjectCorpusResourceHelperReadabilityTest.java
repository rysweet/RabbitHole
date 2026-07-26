package org.alice.ide.uricontent;

import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;
import org.lgna.project.io.ProjectIo;
import org.lgna.story.resourceutilities.StorytellingResourcesTreeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Resource-helper readability companion to the pure-library
 * {@code TweedleCorpusRoundTripCharacterizationTest} (core/story-api-migration).
 *
 * <p>That harness reads the checked-in curriculum starter projects headlessly
 * <b>without</b> a {@link org.lgna.story.resourceutilities.ResourceTypeHelper},
 * so 5 of the 34 projects
 * ({@code iceFull}, {@code pacificnorthwest}, {@code snow}, {@code snowFull},
 * {@code snowMinimum}) fail to read with a {@code NullPointerException}
 * ("{@code this.helper is null}") on the model-resource migration path and are
 * reported as {@code UNREADABLE}.
 *
 * <p>This test isolates what that limitation actually is. The concrete
 * {@code ResourceTypeHelper} implementation
 * ({@link StorytellingResourcesTreeUtils#INSTANCE}) lives in this module
 * (core/ide) and is unreachable from core/story-api-migration by design (a
 * dependency wall). Here — where the real helper <i>is</i> reachable — we wire
 * it into the reader exactly as {@link AbstractFileProjectLoader} does at
 * project-open time and re-read the entire corpus. Two things are asserted:
 *
 * <ol>
 *   <li><b>Wiring the helper is strictly non-regressive.</b> Every project the
 *       pure harness can read (the 29 non-helper-dependent ones) still reads
 *       here, and no project regresses to the {@code "this.helper is null"}
 *       failure — that specific cause is eliminated by supplying the helper.</li>
 *   <li><b>The residual limitation is gallery-resource resolution, not helper
 *       wiring.</b> Any project that still fails to read does so <i>past</i> the
 *       helper-null point: {@link StorytellingResourcesTreeUtils} returns a
 *       {@code null} instance-creation when the headless gallery tree has no
 *       node for a referenced art-gallery model resource (the EA/Sims2 gallery
 *       assets are not resolvable in this headless, LFS-skipped census
 *       environment). This corrects the earlier assumption that the 5 projects
 *       fail <i>solely</i> for want of a wired helper.</li>
 * </ol>
 *
 * <p>The test is deliberately <b>environment-tolerant</b>: it never asserts that
 * a helper-dependent project <i>must</i> fail (in a fully asset-populated
 * environment the gallery tree may resolve and the project may read). It only
 * asserts the non-regression floor and the absence of the {@code this.helper}
 * cause, so it is stable whether or not the art gallery is present.
 *
 * <p>Headless: no JavaFX, no 3D rendering, no UI. Reads projects directly from
 * the source tree via a directory walk (mirrors the pure harness), so the
 * whole corpus is exercised without bundling every {@code .a3p} as a test
 * resource.
 *
 * @see AbstractFileProjectLoader
 */
public class StarterProjectCorpusResourceHelperReadabilityTest {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  /** Non-regression floor: projects the pure headless harness reads today. */
  private static final int BASELINE_READABLE_PROJECTS = 29;

  /** Signature of the "no resource helper wired" failure the helper eliminates. */
  private static final String HELPER_NULL_SIGNATURE = "this.helper";

  @Test
  public void wiringRealResourceHelperIsNonRegressiveAndEliminatesHelperNullCause() {
    List<Path> archives = corpusArchives();
    assertTrue(
        "Curriculum corpus should hold the full checked-in starter set (>= 30 projects); found "
            + archives.size(),
        archives.size() >= 30);

    Map<String, Integer> readTypeCounts = new LinkedHashMap<>();
    Map<String, String> readFailures = new LinkedHashMap<>();

    for (Path archive : archives) {
      String name = archive.getFileName().toString();
      try {
        ProjectIo.ProjectReader reader = IoUtilities.projectReader(archive.toFile());
        reader.setResourceTypeHelper(StorytellingResourcesTreeUtils.INSTANCE);
        Project project = reader.readProject(false);
        Set<NamedUserType> types = project.getNamedUserTypes();
        readTypeCounts.put(name, types.size());
      } catch (Exception | LinkageError e) {
        String message = e.getClass().getSimpleName()
            + (e.getMessage() != null ? ": " + e.getMessage() : "");
        readFailures.put(name, message);
      }
    }

    printSummary(readTypeCounts, readFailures);

    // (1) Non-regression: wiring the helper never makes fewer projects readable.
    assertTrue(
        "Wiring the real ResourceTypeHelper must be non-regressive: expected >= "
            + BASELINE_READABLE_PROJECTS + " readable projects but only "
            + readTypeCounts.size() + " read. Failures: " + readFailures,
        readTypeCounts.size() >= BASELINE_READABLE_PROJECTS);

    // A readable project that exposes no user type would be a false positive.
    for (Map.Entry<String, Integer> entry : readTypeCounts.entrySet()) {
      assertTrue(
          "Project read but exposed no NamedUserType (suspicious): " + entry.getKey(),
          entry.getValue() > 0);
    }

    // (2) The helper-null cause is gone: no remaining failure is the
    //     "this.helper is null" NPE — any residual failure is downstream
    //     gallery-resource resolution, not missing helper wiring.
    for (Map.Entry<String, String> failure : readFailures.entrySet()) {
      assertFalse(
          "Supplying the real ResourceTypeHelper must eliminate the "
              + "\"this.helper is null\" failure, but " + failure.getKey()
              + " still reports it: " + failure.getValue(),
          failure.getValue().contains(HELPER_NULL_SIGNATURE));
    }
  }

  private static void printSummary(Map<String, Integer> readTypeCounts,
      Map<String, String> readFailures) {
    System.out.println();
    System.out.println("===== Starter corpus readability with real ResourceTypeHelper =====");
    System.out.printf("readable: %d    unresolved (gallery-resource): %d%n",
        readTypeCounts.size(), readFailures.size());
    for (Map.Entry<String, String> failure : readFailures.entrySet()) {
      System.out.printf("  UNRESOLVED  %-24s %s%n", failure.getKey(), failure.getValue());
    }
    System.out.println();
  }

  /** All checked-in curriculum starter projects, sorted for stable iteration. */
  private static List<Path> corpusArchives() {
    Path dir = starterProjectsDirectory();
    List<Path> archives = new ArrayList<>();
    try {
      Files.list(dir)
          .filter(p -> p.getFileName().toString().endsWith(".a3p"))
          .sorted(Comparator.comparing(p -> p.getFileName().toString()))
          .forEach(archives::add);
    } catch (IOException e) {
      fail("Unable to list curriculum corpus in " + dir + ": " + e.getMessage());
    }
    return archives;
  }

  /**
   * Walks up from the working directory to the checked-in starter-projects
   * corpus (mirrors the pure-library harness locator).
   */
  private static Path starterProjectsDirectory() {
    Path current = Paths.get("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve("core/resources/src/application/resources/starter-projects");
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    fail("Unable to find core/resources/src/application/resources/starter-projects from "
        + Paths.get("").toAbsolutePath());
    return null;
  }
}
