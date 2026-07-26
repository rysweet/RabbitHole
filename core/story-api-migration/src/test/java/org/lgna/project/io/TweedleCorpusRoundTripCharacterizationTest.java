package org.lgna.project.io;

import org.alice.serialization.tweedle.TweedleEncoderDecoder;
import org.alice.serialization.tweedle.UnsupportedTweedleDecodeException;
import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.NamedUserType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Corpus-wide Tweedle round-trip characterization over the checked-in
 * curriculum starter projects
 * (`core/resources/src/application/resources/starter-projects/*.a3p`).
 *
 * <p>This is the PLAN Phase 6 corpus-validation harness (issue #992). It
 * generalises {@link SilverThreadTweedleDecoderRoundTripTest} — which
 * characterizes a single {@code indiaMinimum.a3p} — to the full, licensed,
 * in-repo corpus of 34 real curriculum projects. For every project it reads
 * the AST, encodes each {@code NamedUserType} to Tweedle source, attempts to
 * decode it back, and reports per file how many types import <b>bounded
 * (Tweedle decode succeeds)</b> versus <b>fallback (decode raises a gap and
 * the reader would route through the legacy XML payload)</b>.
 *
 * <p>The printed report is the corpus census folded into
 * {@code docs/tweedle-decode-gaps.md}. As decoder gaps close (PLAN Phase 5),
 * the bounded ratio rises automatically — so this test asserts only harness
 * invariants (the corpus is present, projects read, types are exercised), not
 * a specific decode ratio. It is therefore a stable, non-brittle
 * characterization test rather than a coverage ratchet.
 *
 * <p>Headless: no JavaFX, no 3D rendering, no UI. Reads projects directly from
 * the source tree via a directory-walk accessor (not the test classpath), so
 * the whole corpus is exercised without bundling every {@code .a3p} as a test
 * resource.
 *
 * @see SilverThreadTweedleDecoderRoundTripTest
 * @see TweedleEncoderDecoder
 */
public class TweedleCorpusRoundTripCharacterizationTest {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final TweedleEncoderDecoder CODEC = new TweedleEncoderDecoder();

  /** Per-project round-trip tally. */
  private static final class ProjectResult {
    final String fileName;
    int totalTypes;
    int boundedTypes;
    final List<String> gaps = new ArrayList<>();
    String readError;

    ProjectResult(String fileName) {
      this.fileName = fileName;
    }

    boolean readable() {
      return readError == null;
    }

    int fallbackTypes() {
      return totalTypes - boundedTypes;
    }
  }

  @Test
  public void reportBoundedVsFallbackAcrossCurriculumCorpus() {
    List<Path> archives = corpusArchives();
    assertFalse(
        "Curriculum corpus must contain at least one .a3p starter project",
        archives.isEmpty());
    assertTrue(
        "Curriculum corpus should hold the full checked-in starter set (>= 30 projects); found "
            + archives.size(),
        archives.size() >= 30);

    List<ProjectResult> results = new ArrayList<>();
    for (Path archive : archives) {
      results.add(characterize(archive));
    }

    printCensus(results);

    long readable = results.stream().filter(ProjectResult::readable).count();
    assertTrue(
        "At least one curriculum project must be readable for the census to be meaningful",
        readable > 0);

    int totalTypesExercised =
        results.stream().filter(ProjectResult::readable).mapToInt(r -> r.totalTypes).sum();
    assertTrue(
        "The corpus census must exercise at least one NamedUserType across all readable projects",
        totalTypesExercised > 0);
  }

  private ProjectResult characterize(Path archive) {
    ProjectResult result = new ProjectResult(archive.getFileName().toString());

    Project project;
    try {
      project = IoUtilities.readProject(archive.toFile());
    } catch (Exception | LinkageError e) {
      // A project that cannot be read is itself a corpus finding, not a
      // harness failure — record it and keep the census going.
      result.readError = e.getClass().getSimpleName()
          + (e.getMessage() != null ? ": " + e.getMessage() : "");
      return result;
    }

    Set<NamedUserType> types = project.getNamedUserTypes();
    Set<AbstractDeclaration> terminals = new HashSet<>(types);
    result.totalTypes = types.size();

    for (NamedUserType type : types) {
      String typeName = type.getName();
      String tweedleSource;
      try {
        tweedleSource = CODEC.encode(type, terminals);
      } catch (Exception | LinkageError e) {
        // Encode failure is a distinct (rarer) gap; count as fallback.
        result.gaps.add(typeName + " [encode]: " + shortMessage(e));
        continue;
      }
      if (tweedleSource == null || tweedleSource.isEmpty()) {
        result.gaps.add(typeName + " [encode]: produced empty Tweedle source");
        continue;
      }

      try {
        AbstractNode decoded = CODEC.decode(tweedleSource, terminals);
        if (decoded instanceof NamedUserType) {
          result.boundedTypes++;
        } else {
          result.gaps.add(typeName + " [decode]: decoded to "
              + (decoded == null ? "null" : decoded.getClass().getSimpleName())
              + ", not NamedUserType");
        }
      } catch (UnsupportedTweedleDecodeException | IllegalArgumentException e) {
        result.gaps.add(typeName + " [decode]: " + shortMessage(e));
      } catch (Exception | LinkageError e) {
        result.gaps.add(typeName + " [decode/" + e.getClass().getSimpleName() + "]: "
            + shortMessage(e));
      }
    }

    return result;
  }

  private void printCensus(List<ProjectResult> results) {
    int corpusTypes = 0;
    int corpusBounded = 0;
    int readableProjects = 0;

    System.out.println();
    System.out.println("===== Tweedle corpus round-trip census (starter-projects) =====");
    System.out.printf("%-28s %8s %9s %9s%n", "project", "types", "bounded", "fallback");
    System.out.println("---------------------------------------------------------------");
    for (ProjectResult r : results) {
      if (!r.readable()) {
        System.out.printf("%-28s   UNREADABLE  (%s)%n", r.fileName, r.readError);
        continue;
      }
      readableProjects++;
      corpusTypes += r.totalTypes;
      corpusBounded += r.boundedTypes;
      System.out.printf("%-28s %8d %9d %9d%n",
          r.fileName, r.totalTypes, r.boundedTypes, r.fallbackTypes());
    }
    System.out.println("---------------------------------------------------------------");
    System.out.printf("%-28s %8d %9d %9d%n",
        "TOTAL (" + readableProjects + " projects)",
        corpusTypes, corpusBounded, corpusTypes - corpusBounded);
    double pct = corpusTypes == 0 ? 0.0 : (100.0 * corpusBounded / corpusTypes);
    System.out.printf("bounded (Tweedle) coverage: %.1f%% of %d types%n", pct, corpusTypes);
    System.out.println();

    // Aggregate gap messages (deduplicated by category prefix) to feed the
    // docs/tweedle-decode-gaps.md backlog.
    List<String> allGaps = new ArrayList<>();
    for (ProjectResult r : results) {
      allGaps.addAll(r.gaps);
    }
    if (!allGaps.isEmpty()) {
      // Category histogram across ALL gaps (drives the Phase 5 backlog in
      // docs/tweedle-decode-gaps.md).
      java.util.Map<String, Integer> byCategory = new java.util.TreeMap<>();
      for (String gap : allGaps) {
        byCategory.merge(categoryOf(gap), 1, Integer::sum);
      }
      System.out.println("Decoder-gap categories (" + allGaps.size() + " fallback types across corpus):");
      byCategory.entrySet().stream()
          .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
          .forEach(e -> System.out.printf("  %5d  %s%n", e.getValue(), e.getKey()));
      System.out.println();
    }
  }

  /**
   * Collapses a per-type gap line into a stable decoder-capability category by
   * stripping type-specific suffixes (type names, member paths), so counts
   * aggregate cleanly for the backlog doc.
   */
  private static String categoryOf(String gap) {
    int idx = gap.indexOf(']');
    String msg = idx >= 0 && idx + 2 < gap.length() ? gap.substring(idx + 2) : gap;
    if (msg.startsWith("Tweedle constructor bodies are not yet supported")) {
      return "constructor body (Tweedle constructor bodies not yet decoded)";
    }
    if (msg.startsWith("Tweedle argument-bearing explicit this method calls")) {
      return "arg-bearing explicit this() call not yet decoded";
    }
    if (msg.startsWith("Tweedle comments are not yet supported")) {
      return "comments not yet decoded";
    }
    if (msg.startsWith("Unable to parse Tweedle type")) {
      return "top-level Tweedle parse failure";
    }
    if (msg.contains("[encode]")) {
      return "encode failure";
    }
    // Fall back to the leading clause of the message.
    int colon = msg.indexOf(':');
    return colon > 0 ? msg.substring(0, colon).trim() : msg;
  }

  private static String shortMessage(Throwable e) {
    String m = e.getMessage();
    if (m == null) {
      return e.getClass().getSimpleName();
    }
    m = m.replaceAll("\\s+", " ").trim();
    return m.length() > 160 ? m.substring(0, 157) + "..." : m;
  }

  /** All checked-in curriculum starter projects, sorted for stable output. */
  private static List<Path> corpusArchives() {
    Path dir = starterProjectsDirectory();
    List<Path> archives = new ArrayList<>();
    try {
      Files.list(dir)
          .filter(p -> p.getFileName().toString().endsWith(".a3p"))
          .sorted(Comparator.comparing(p -> p.getFileName().toString()))
          .forEach(archives::add);
    } catch (Exception e) {
      fail("Unable to list curriculum corpus in " + dir + ": " + e.getMessage());
    }
    return archives;
  }

  /**
   * Walks up from the working directory to the checked-in starter-projects
   * corpus (mirrors {@code StarterProjectXmlFallbackReadabilityTest}).
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
