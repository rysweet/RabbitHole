package org.lgna.project.io;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.Resource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class GoldenProjectCorpusValidatorTest {
  private static final String CORPUS_RESOURCE = "/golden-project-corpus/corpus.properties";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void manifestDefinesRepresentativeTextOnlyCorpus() throws Exception {
    GoldenProjectCorpus corpus = GoldenProjectCorpus.load(corpusPath());

    assertEquals(
        Arrays.asList(
            "editable-minimal",
            "editable-with-image-resource",
            "player-export-minimal",
            "player-export-with-image-resource"),
        corpus.caseIds());

    GoldenProjectCorpusCase editable = corpus.caseNamed("editable-minimal");
    assertEquals(GoldenProjectCorpusCase.ArchiveType.EDITABLE_PROJECT, editable.archiveType());
    assertEquals("GoldenEditableMinimal", editable.projectName());
    assertEquals(Project.SceneCameraType.WindowCamera, editable.cameraType());
    assertTrue(editable.resources().isEmpty());
    assertEquals(
        Arrays.asList(ProjectIo.VERSION_ENTRY_NAME, ProjectIo.MANIFEST_ENTRY_NAME, "programType.xml"),
        editable.expectedEntries());
    assertEquals(Arrays.asList("resources.xml"), editable.absentEntries());
    assertTrue(editable.roundTripEditable());
    assertTrue(editable.exportAfterReopen());

    GoldenProjectCorpusCase editableWithImage = corpus.caseNamed("editable-with-image-resource");
    assertEquals(1, editableWithImage.resources().size());
    assertEquals(GoldenProjectCorpusCase.ResourceSpec.Kind.IMAGE, editableWithImage.resources().get(0).kind());
    assertEquals("golden-texture.png", editableWithImage.resources().get(0).fileName());
    assertTrue(editableWithImage.expectedEntries().contains("resources/golden-texture.png"));

    GoldenProjectCorpusCase playerExport = corpus.caseNamed("player-export-minimal");
    assertEquals(GoldenProjectCorpusCase.ArchiveType.PLAYER_EXPORT, playerExport.archiveType());
    assertEquals(Project.SceneCameraType.VRHeadset, playerExport.cameraType());
    assertEquals(Arrays.asList("programType.xml"), playerExport.absentEntries());
    assertFalse(playerExport.roundTripEditable());
    assertFalse(playerExport.exportAfterReopen());
  }

  @Test
  public void manifestRejectsMissingRequiredFieldsWithCaseIdInDiagnostic() {
    Properties properties = validOneCaseProperties("missing-project-name");
    properties.remove("case.missing-project-name.projectName");

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> GoldenProjectCorpus.load(properties));

    assertTrue(thrown.getMessage().contains("missing-project-name"));
    assertTrue(thrown.getMessage().contains("projectName"));
  }

  @Test
  public void manifestRejectsDuplicateCaseIds() {
    Properties properties = validOneCaseProperties("duplicate");
    properties.setProperty("cases", "duplicate,duplicate");

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> GoldenProjectCorpus.load(properties));

    assertTrue(thrown.getMessage().contains("duplicate"));
    assertTrue(thrown.getMessage().contains("case id"));
  }

  @Test
  public void manifestRejectsUnsupportedArchiveTypes() {
    Properties properties = validOneCaseProperties("bad-archive");
    properties.setProperty("case.bad-archive.archiveType", "zip");

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> GoldenProjectCorpus.load(properties));

    assertTrue(thrown.getMessage().contains("bad-archive"));
    assertTrue(thrown.getMessage().contains("archiveType"));
    assertTrue(thrown.getMessage().contains("a3p"));
    assertTrue(thrown.getMessage().contains("a3w"));
  }

  @Test
  public void manifestRejectsUnsafeProjectNames() {
    Properties properties = validOneCaseProperties("unsafe-name");
    properties.setProperty("case.unsafe-name.projectName", "../BadProject");

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> GoldenProjectCorpus.load(properties));

    assertTrue(thrown.getMessage().contains("unsafe-name"));
    assertTrue(thrown.getMessage().contains("projectName"));
  }

  @Test
  public void manifestRejectsUnsafeExpectedArchiveEntries() {
    Properties properties = validOneCaseProperties("unsafe-entry");
    properties.setProperty("case.unsafe-entry.expectedEntries", "version.txt,../evil.txt");

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> GoldenProjectCorpus.load(properties));

    assertTrue(thrown.getMessage().contains("unsafe-entry"));
    assertTrue(thrown.getMessage().contains("../evil.txt"));
  }

  @Test
  public void manifestRejectsUnsupportedResourceDeclarations() {
    Properties properties = validOneCaseProperties("bad-resource");
    properties.setProperty("case.bad-resource.resources", "video:movie.mp4");

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> GoldenProjectCorpus.load(properties));

    assertTrue(thrown.getMessage().contains("bad-resource"));
    assertTrue(thrown.getMessage().contains("video"));
  }

  @Test
  public void fixtureGeneratorCreatesDeterministicProjectsWithoutBinaryFixtures() throws Exception {
    GoldenProjectCorpusCase corpusCase = GoldenProjectCorpus.load(corpusPath()).caseNamed("editable-with-image-resource");
    GoldenProjectFixtureGenerator generator = new GoldenProjectFixtureGenerator();

    Project first = generator.createProject(corpusCase);
    Project second = generator.createProject(corpusCase);

    assertEquals("GoldenEditableWithImageResource", first.getProgramType().getName());
    assertEquals(Project.SceneCameraType.WindowCamera, first.createSaveManifest().projectStructure.sceneCameraType);
    assertEquals(1, first.getResources().size());
    assertEquals(1, second.getResources().size());

    Resource firstResource = only(first.getResources());
    Resource secondResource = only(second.getResources());
    assertTrue(firstResource instanceof ImageResource);
    assertEquals("golden-texture.png", firstResource.getName());
    assertEquals("golden-texture.png", firstResource.getOriginalFileName());
    assertEquals("image.png", firstResource.getContentType());
    assertArrayEquals(firstResource.getData(), secondResource.getData());
    assertTrue("Generated image resources should be tiny test payloads.", firstResource.getData().length <= 128);
  }

  @Test
  public void editableProjectCasesOpenSaveReopenAndExport() throws Exception {
    GoldenProjectCorpus corpus = GoldenProjectCorpus.load(corpusPath());
    GoldenProjectCorpusValidator validator = new GoldenProjectCorpusValidator(temporaryFolder.getRoot());

    for (GoldenProjectCorpusCase corpusCase : corpus.cases()) {
      if (corpusCase.archiveType() != GoldenProjectCorpusCase.ArchiveType.EDITABLE_PROJECT) {
        continue;
      }

      GoldenProjectValidationResult result = validator.validateCase(corpusCase);

      assertTrue(result.primaryArchive().isFile());
      assertTrue(result.secondEditableSave().isFile());
      assertTrue(result.exportArchive().isFile());
      assertEquals(corpusCase.projectName(), result.firstReopen().getProgramType().getName());
      assertEquals(corpusCase.projectName(), result.secondReopen().getProgramType().getName());
      assertEquals(corpusCase.cameraType(), result.secondReopen().createSaveManifest().projectStructure.sceneCameraType);
      assertEquals(IoUtilities.PROJECT_EXTENSION, result.primaryManifest().metadata.fileType);
      assertEquals(IoUtilities.EXPORT_EXTENSION, result.exportManifest().metadata.fileType);

      try (ZipFile primary = new ZipFile(result.primaryArchive());
           ZipFile secondSave = new ZipFile(result.secondEditableSave());
           ZipFile export = new ZipFile(result.exportArchive())) {
        GoldenProjectZipAssertions.assertExpectedEntries(primary, corpusCase.expectedEntries());
        GoldenProjectZipAssertions.assertAbsentEntries(primary, corpusCase.absentEntries());
        GoldenProjectZipAssertions.assertNoUnsafeEntries(primary);
        GoldenProjectZipAssertions.assertExpectedEntries(secondSave, corpusCase.expectedEntries());
        GoldenProjectZipAssertions.assertNoUnsafeEntries(secondSave);
        GoldenProjectZipAssertions.assertExpectedEntries(export, exportEntriesFor(corpusCase));
        GoldenProjectZipAssertions.assertAbsentEntries(export, Arrays.asList("programType.xml"));
        GoldenProjectZipAssertions.assertNoUnsafeEntries(export);
      }
    }
  }

  @Test
  public void playerExportCasesValidateZipShapeWithoutEditableReopen() throws Exception {
    GoldenProjectCorpus corpus = GoldenProjectCorpus.load(corpusPath());
    GoldenProjectCorpusValidator validator = new GoldenProjectCorpusValidator(temporaryFolder.getRoot());

    for (GoldenProjectCorpusCase corpusCase : corpus.cases()) {
      if (corpusCase.archiveType() != GoldenProjectCorpusCase.ArchiveType.PLAYER_EXPORT) {
        continue;
      }

      GoldenProjectValidationResult result = validator.validateCase(corpusCase);

      assertTrue(result.primaryArchive().isFile());
      assertNull("Player export cases are not required to produce a second editable save.", result.secondEditableSave());
      assertNull("Player export cases are ZIP/player-shape checks, not editable reopen checks.", result.firstReopen());
      assertEquals(IoUtilities.EXPORT_EXTENSION, result.primaryManifest().metadata.fileType);
      assertEquals(corpusCase.projectName(), result.primaryManifest().description.name);
      assertEquals(corpusCase.cameraType(), result.primaryManifest().projectStructure.sceneCameraType);

      try (ZipFile export = new ZipFile(result.primaryArchive())) {
        GoldenProjectZipAssertions.assertExpectedEntries(export, corpusCase.expectedEntries());
        GoldenProjectZipAssertions.assertAbsentEntries(export, corpusCase.absentEntries());
        GoldenProjectZipAssertions.assertNoUnsafeEntries(export);
      }
    }
  }

  @Test
  public void zipAssertionsRejectUnsafeEntriesBeforeValidation() throws Exception {
    File archive = writeArchive("unsafe.zip", "../evil.txt", "payload");

    AssertionError thrown = assertThrows(
        AssertionError.class,
        () -> {
          try (ZipFile zipFile = new ZipFile(archive)) {
            GoldenProjectZipAssertions.assertNoUnsafeEntries(zipFile);
          }
        });

    assertTrue(thrown.getMessage().contains("../evil.txt"));
  }

  @Test
  public void zipAssertionsRequireExpectedEntriesToBePresentAndReadable() throws Exception {
    File archive = writeArchive("missing-manifest.zip", ProjectIo.VERSION_ENTRY_NAME, "0.0.0");

    AssertionError thrown = assertThrows(
        AssertionError.class,
        () -> {
          try (ZipFile zipFile = new ZipFile(archive)) {
            GoldenProjectZipAssertions.assertExpectedEntries(
                zipFile,
                Arrays.asList(ProjectIo.VERSION_ENTRY_NAME, ProjectIo.MANIFEST_ENTRY_NAME));
          }
        });

    assertTrue(thrown.getMessage().contains(ProjectIo.MANIFEST_ENTRY_NAME));
  }

  @Test
  public void validatorFailsWhenGeneratedArchiveDoesNotMatchManifestExpectations() throws Exception {
    Properties properties = validOneCaseProperties("drift");
    properties.setProperty("case.drift.expectedEntries", "version.txt,manifest.json,programType.xml,missing-stable-entry.txt");
    GoldenProjectCorpusCase corpusCase = GoldenProjectCorpus.load(properties).caseNamed("drift");
    GoldenProjectCorpusValidator validator = new GoldenProjectCorpusValidator(temporaryFolder.getRoot());

    AssertionError thrown = assertThrows(
        AssertionError.class,
        () -> validator.validateCase(corpusCase));

    assertTrue(thrown.getMessage().contains("drift"));
    assertTrue(thrown.getMessage().contains("missing-stable-entry.txt"));
  }

  private Path corpusPath() throws URISyntaxException {
    URL resource = getClass().getResource(CORPUS_RESOURCE);
    assertNotNull("Missing test resource " + CORPUS_RESOURCE, resource);
    return Path.of(resource.toURI());
  }

  private static Properties validOneCaseProperties(String id) {
    Properties properties = new Properties();
    properties.setProperty("cases", id);
    properties.setProperty("case." + id + ".archiveType", "a3p");
    properties.setProperty("case." + id + ".projectName", "GoldenContractProject");
    properties.setProperty("case." + id + ".cameraType", "WindowCamera");
    properties.setProperty("case." + id + ".resources", "none");
    properties.setProperty("case." + id + ".expectedEntries", "version.txt,manifest.json,programType.xml");
    properties.setProperty("case." + id + ".absentEntries", "resources.xml");
    properties.setProperty("case." + id + ".roundTripEditable", "true");
    properties.setProperty("case." + id + ".exportAfterReopen", "true");
    return properties;
  }

  private static List<String> exportEntriesFor(GoldenProjectCorpusCase corpusCase) {
    List<String> entries = new java.util.ArrayList<>();
    entries.add(ProjectIo.VERSION_ENTRY_NAME);
    entries.add(ProjectIo.MANIFEST_ENTRY_NAME);
    entries.add("src/" + corpusCase.projectName() + ".twe");
    for (GoldenProjectCorpusCase.ResourceSpec resource : corpusCase.resources()) {
      entries.add("resources/" + resource.fileName());
    }
    return entries;
  }

  private File writeArchive(String fileName, String entryName, String contents) throws IOException {
    File archive = temporaryFolder.newFile(fileName);
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(archive))) {
      zipOutputStream.putNextEntry(new ZipEntry(entryName));
      zipOutputStream.write(contents.getBytes(StandardCharsets.UTF_8));
      zipOutputStream.closeEntry();
    }
    return archive;
  }

  private static Resource only(Set<Resource> resources) {
    assertEquals(1, resources.size());
    return resources.iterator().next();
  }
}
