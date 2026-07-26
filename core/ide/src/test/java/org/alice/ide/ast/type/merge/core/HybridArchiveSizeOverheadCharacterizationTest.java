package org.alice.ide.ast.type.merge.core;

import org.alice.ide.ast.export.type.TypeSummary;
import org.alice.ide.ast.export.type.TypeSummaryDataSource;
import org.alice.ide.ast.export.type.TypeSummaryJsonDataSource;
import org.junit.Test;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;

import java.io.File;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.alice.ide.ast.type.merge.core.MergeUtilitiesTestSupport.addField;
import static org.alice.ide.ast.type.merge.core.MergeUtilitiesTestSupport.namedType;
import static org.junit.Assert.assertTrue;

/**
 * Characterizes the on-disk size overhead of the within-archive hybrid
 * Tweedle/XML {@code .a3c} format versus the legacy XML-only payload.
 *
 * <p>A hybrid archive is deliberately <b>additive</b> (see
 * {@code docs/tweedle-hybrid-format.md}): it carries the authoritative legacy
 * XML AST payload <i>and</i> a per-type Tweedle representation plus a
 * gallery-tile summary sidecar. That redundancy is what guarantees hybrid
 * archives are never worse to read than legacy ones — at the cost of extra
 * bytes. This test measures that cost so the overhead is a documented, guarded
 * number rather than a guess.
 *
 * <p>It exports a representative single class exactly as the IDE's
 * {@code ExportTypeToFileDialogOperation} does — {@link IoUtilities#writeType}
 * with both the XML and JSON {@link TypeSummary} sidecars — then sums the
 * uncompressed bytes of each archive region:
 *
 * <ul>
 *   <li><b>XML payload</b> ({@code type.xml}, {@code resources.xml},
 *       {@code resources/…}) — what a legacy archive would carry.</li>
 *   <li><b>Tweedle payload</b> ({@code src/*.twe}, {@code models/…}) — the
 *       additive Tweedle side.</li>
 *   <li><b>Summary sidecars</b> ({@code typeSummary.xml/json}).</li>
 *   <li><b>Shared</b> ({@code manifest.json}, {@code version.txt}).</li>
 * </ul>
 *
 * <p>The breakdown is printed for the docs. The assertions are intentionally
 * loose invariants (both payloads present; the hybrid total is a bounded
 * multiple of the XML-only baseline) so the test documents and guards the
 * overhead without pinning brittle exact byte counts.
 *
 * <p>Headless: pure archive I/O, no UI.
 */
public class HybridArchiveSizeOverheadCharacterizationTest {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  /**
   * Upper bound on hybrid total relative to the XML-only payload. The hybrid
   * archive re-encodes the type as Tweedle and adds two summary sidecars, so a
   * small-type archive is dominated by fixed per-type overhead. 8x is a
   * generous ceiling that still catches a runaway-size regression (e.g. the
   * whole object graph leaking into the Tweedle side).
   */
  private static final double MAX_HYBRID_TO_XML_RATIO = 8.0;

  @Test
  public void hybridArchiveSizeOverheadIsBoundedAndBothPayloadsPresent() throws Exception {
    NamedUserType referenced = namedType("ReferencedScene");
    NamedUserType exported = namedType("LessonClass");
    addField(exported, "referencedScene", referenced);
    addField(exported, "counter", referenced);

    File archive = File.createTempFile("hybrid-size-", ".a3c");
    archive.deleteOnExit();

    TypeSummary typeSummary = new TypeSummary(exported);
    IoUtilities.writeType(archive, exported,
        new TypeSummaryDataSource(typeSummary), new TypeSummaryJsonDataSource(typeSummary));

    long xmlPayload = 0;
    long tweedlePayload = 0;
    long summarySidecars = 0;
    long shared = 0;
    Map<String, Long> perEntry = new LinkedHashMap<>();

    try (ZipFile zipFile = new ZipFile(archive)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          continue;
        }
        String name = entry.getName();
        long size = Math.max(entry.getSize(), 0);
        perEntry.put(name, size);
        if (name.equals("type.xml") || name.equals("programType.xml")
            || name.equals("resources.xml") || name.startsWith("resources/")) {
          xmlPayload += size;
        } else if (name.startsWith("src/") || name.startsWith("models/")) {
          tweedlePayload += size;
        } else if (name.startsWith("typeSummary")) {
          summarySidecars += size;
        } else {
          shared += size;
        }
      }
    }

    long hybridTotal = xmlPayload + tweedlePayload + summarySidecars + shared;
    printBreakdown(perEntry, xmlPayload, tweedlePayload, summarySidecars, shared, hybridTotal);

    assertTrue("Hybrid .a3c must carry the authoritative XML payload (type.xml)",
        perEntry.containsKey("type.xml"));
    assertTrue("Hybrid .a3c must carry the additive Tweedle payload (src/LessonClass.twe)",
        perEntry.containsKey("src/LessonClass.twe"));
    assertTrue("XML payload must be non-empty", xmlPayload > 0);
    assertTrue("Tweedle payload must be non-empty", tweedlePayload > 0);

    assertTrue(
        "Hybrid archive total (" + hybridTotal + " B) must stay within "
            + MAX_HYBRID_TO_XML_RATIO + "x the XML-only payload (" + xmlPayload
            + " B) — a larger ratio suggests the bounded Tweedle side is pulling in "
            + "more than the single extracted type.",
        hybridTotal <= MAX_HYBRID_TO_XML_RATIO * xmlPayload);
  }

  private static void printBreakdown(Map<String, Long> perEntry, long xmlPayload,
      long tweedlePayload, long summarySidecars, long shared, long hybridTotal) {
    System.out.println();
    System.out.println("===== Hybrid .a3c size overhead (uncompressed bytes) =====");
    perEntry.forEach((name, size) -> System.out.printf("  %8d  %s%n", size, name));
    System.out.println("  ---------------------------------------------");
    System.out.printf("  XML payload (legacy-equivalent) : %8d B%n", xmlPayload);
    System.out.printf("  Tweedle payload (additive)      : %8d B%n", tweedlePayload);
    System.out.printf("  Summary sidecars (additive)     : %8d B%n", summarySidecars);
    System.out.printf("  Shared (manifest/version)       : %8d B%n", shared);
    System.out.printf("  HYBRID TOTAL                    : %8d B%n", hybridTotal);
    long additive = tweedlePayload + summarySidecars;
    double pct = xmlPayload == 0 ? 0.0 : (100.0 * additive / xmlPayload);
    System.out.printf("  Additive overhead vs XML payload: %8d B (%.0f%%)%n", additive, pct);
    System.out.printf("  Hybrid / XML ratio              : %.2fx%n",
        xmlPayload == 0 ? 0.0 : (double) hybridTotal / xmlPayload);
    System.out.println();
  }
}
