/*
 * Copyright (c) 2006-2025, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of Carnegie
 *    Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes the Alice Gallery of art assets and animations
 *    must reproduce the above copyright notice.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND CONTRIBUTORS "AS IS" AND ANY
 * AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT SHALL THE
 * AUTHORS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.alice.ide.ast.type.merge.core;

import org.alice.tweedle.file.ManifestEncoderDecoder;
import org.alice.tweedle.file.ResourceReference;
import org.alice.tweedle.file.TypeManifest;
import org.alice.tweedle.file.TypeReference;
import org.junit.Test;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.alice.ide.ast.type.merge.core.MergeUtilitiesTestSupport.addField;
import static org.alice.ide.ast.type.merge.core.MergeUtilitiesTestSupport.namedType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end guard for the hybrid single-class extraction user story
 * (PLAN "extract a class from a curriculum project without pulling its whole
 * scene"): exports one {@code NamedUserType} to a real {@code .a3c} archive and
 * proves the persisted export contract and the IDE import contract agree.
 *
 * <p>Two independent code paths compute the bounded dependency set:
 * <ul>
 *   <li><b>Export</b> — {@code JsonResourceEntryWriter} crawls the type and
 *       records {@code TypeReference.dependencies} into {@code manifest.json}.</li>
 *   <li><b>Import</b> — {@link ImportDependencyResolver#collectDependencyNames}
 *       re-crawls the type at Gallery-import time to drive the missing-dependency
 *       prompt.</li>
 * </ul>
 * If these two crawls silently diverge, an archive would advertise different
 * dependencies than the import prompt checks — a defect that unit tests of
 * either side alone cannot catch. This test locks their equality against a
 * genuinely written-then-read archive, and re-asserts boundedness (the
 * referenced type's Tweedle source is not bundled) at the same boundary.
 *
 * <p>Headless: pure archive I/O and AST crawl, no UI.
 */
public class ImportDependencyExportParityTest {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  @Test
  public void exportedManifestDependenciesMatchImportResolverCrawl() throws Exception {
    NamedUserType referenced = namedType("ReferencedScene");
    NamedUserType exported = namedType("BoundedGalleryType");
    addField(exported, "referencedScene", referenced);

    File archive = File.createTempFile("import-parity-", ".a3c");
    archive.deleteOnExit();
    IoUtilities.writeType(archive, exported);

    Set<String> manifestDependencies;
    try (ZipFile zipFile = new ZipFile(archive)) {
      TypeManifest manifest = ManifestEncoderDecoder.fromJson(
          readEntry(zipFile, "manifest.json"), TypeManifest.class);
      TypeReference exportedReference = findTypeReference(manifest, "BoundedGalleryType");
      assertNotNull("The exported type must appear in the archive manifest", exportedReference);
      assertNotNull(
          "A type referencing another user type must persist a manifest dependency list",
          exportedReference.dependencies);
      manifestDependencies = new TreeSet<>(exportedReference.dependencies);

      // Boundedness: only the exported type's source is bundled, not the graph.
      assertNull(
          "Bounded export must not bundle the referenced type's Tweedle source",
          zipFile.getEntry("src/ReferencedScene.twe"));
      assertNull(
          "Bounded export must not record the referenced type as its own manifest entry",
          findTypeReference(manifest, "ReferencedScene"));
    }

    Set<String> resolverDependencies = ImportDependencyResolver.collectDependencyNames(exported);

    assertEquals(
        "Persisted export manifest dependencies must equal the import resolver crawl; "
            + "if these diverge the import prompt checks a different set than the archive advertises",
        resolverDependencies,
        manifestDependencies);
    assertTrue(
        "The referenced user type must be part of the bounded dependency contract",
        manifestDependencies.contains("ReferencedScene"));
  }

  @Test
  public void singleClassExtractionResolvesAgainstDestinationProject() throws Exception {
    NamedUserType referenced = namedType("ReferencedScene");
    NamedUserType exported = namedType("BoundedGalleryType");
    addField(exported, "referencedScene", referenced);

    File archive = File.createTempFile("import-resolve-", ".a3c");
    archive.deleteOnExit();
    IoUtilities.writeType(archive, exported);

    Set<String> manifestDependencies;
    try (ZipFile zipFile = new ZipFile(archive)) {
      TypeManifest manifest = ManifestEncoderDecoder.fromJson(
          readEntry(zipFile, "manifest.json"), TypeManifest.class);
      manifestDependencies = new TreeSet<>(findTypeReference(manifest, "BoundedGalleryType").dependencies);
    }

    // Importing into a fresh project (no matching types) surfaces every
    // persisted dependency as missing — the prompt-worthy case.
    Set<String> missingInEmptyProject = missingAgainst(manifestDependencies, Set.of());
    assertEquals(
        "Importing the extracted class into an empty project must flag its scene dependency",
        Set.of("ReferencedScene"),
        missingInEmptyProject);

    // Importing into a project that already contains the dependency resolves
    // cleanly with no prompt.
    Set<String> missingWhenPresent = missingAgainst(manifestDependencies, Set.of("ReferencedScene"));
    assertTrue(
        "Importing where the dependency already exists must not flag anything missing",
        missingWhenPresent.isEmpty());

    // The resolver (import-time crawl) agrees with the persisted contract.
    assertEquals(
        ImportDependencyResolver.findMissingDependencyNames(exported, Set.of()),
        missingInEmptyProject);
  }

  private static Set<String> missingAgainst(Set<String> dependencies, Set<String> availableTypeNames) {
    Set<String> missing = new TreeSet<>();
    for (String dependency : dependencies) {
      if (!availableTypeNames.contains(dependency)) {
        missing.add(dependency);
      }
    }
    return missing;
  }

  private static String readEntry(ZipFile zipFile, String entryName) throws IOException {
    ZipEntry entry = zipFile.getEntry(entryName);
    assertNotNull("Archive must contain " + entryName, entry);
    try (var stream = zipFile.getInputStream(entry)) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static TypeReference findTypeReference(TypeManifest manifest, String typeName) {
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof TypeReference typeReference && typeName.equals(typeReference.name)) {
        return typeReference;
      }
    }
    return null;
  }
}
