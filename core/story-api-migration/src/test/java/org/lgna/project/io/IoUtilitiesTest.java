package org.lgna.project.io;

import edu.cmu.cs.dennisc.java.util.zip.ByteArrayDataSource;
import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import edu.cmu.cs.dennisc.pattern.IsInstanceCrawler;
import edu.cmu.cs.dennisc.xml.XMLUtilities;
import org.alice.serialization.xml.XmlEncoderDecoder;
import org.alice.tweedle.file.AliceTextureReference;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ManifestEncoderDecoder;
import org.alice.tweedle.file.AudioReference;
import org.alice.tweedle.file.ImageReference;
import org.alice.tweedle.file.ModelReference;
import org.alice.tweedle.file.ProjectManifest;
import org.alice.tweedle.file.ResourceReference;
import org.alice.tweedle.file.StructureReference;
import org.alice.tweedle.file.TypeManifest;
import org.alice.tweedle.file.TypeReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.Resource;
import org.lgna.common.resources.AudioResource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;
import org.lgna.project.ProjectVersion;
import org.lgna.project.Version;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.CrawlPolicy;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ResourceExpression;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.story.SProgram;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class IoUtilitiesTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void writesReadableSyntheticProjectWithoutBinaryFixture() throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("synthetic.a3p");

    IoUtilities.writeProject(projectFile, project);

    Project readProject = IoUtilities.readProject(projectFile);
    assertEquals("Program", readProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.WindowCamera, readProject.createSaveManifest().projectStructure.sceneCameraType);
    assertTrue(readProject.getResources().isEmpty());
  }

  @Test
  public void writtenProjectContainsVersionManifestAndProgramTypeEntries() throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("synthetic.a3p");

    IoUtilities.writeProject(projectFile, project);

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals("Program", manifest.description.name);
      assertEquals(IoUtilities.PROJECT_EXTENSION, manifest.metadata.fileType);
      assertNotNull(zipFile.getEntry("programType.xml"));
      assertNull(zipFile.getEntry("resources.xml"));
    }
  }

  @Test
  public void savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported() throws Exception {
    String originalProgramName = "OriginalProgram";
    String editedProgramName = "EditedProgram";
    Project project = new Project(programType(originalProgramName), Project.SceneCameraType.WindowCamera);
    File originalProjectFile = temporaryFolder.newFile("original-program.a3p");
    File editedProjectFile = temporaryFolder.newFile("edited-program.a3p");
    File exportFile = temporaryFolder.newFile("edited-program.a3w");

    IoUtilities.writeProject(originalProjectFile, project);
    Project reopenedProject = IoUtilities.readProject(originalProjectFile);
    NamedUserType reopenedProgramType = reopenedProject.getProgramType();
    assertNotNull(reopenedProgramType);
    assertEquals(originalProgramName, reopenedProgramType.getName());

    reopenedProgramType.name.setValue(editedProgramName);
    IoUtilities.writeProject(editedProjectFile, reopenedProject);

    Project editedProject = IoUtilities.readProject(editedProjectFile);
    NamedUserType editedProgramType = editedProject.getProgramType();
    assertNotNull(editedProgramType);
    assertEquals(editedProgramName, editedProgramType.getName());
    try (ZipFile zipFile = new ZipFile(editedProjectFile)) {
      ProjectManifest saveManifest = readProjectManifest(zipFile);
      assertEquals(editedProgramName, saveManifest.description.name);
      assertEquals(IoUtilities.PROJECT_EXTENSION, saveManifest.metadata.fileType);
      assertNotNull(zipFile.getEntry("programType.xml"));
    }

    IoUtilities.exportProject(exportFile, editedProject);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      ProjectManifest exportManifest = readProjectManifest(zipFile);
      assertEquals(editedProgramName, exportManifest.description.name);
      assertEquals(IoUtilities.EXPORT_EXTENSION, exportManifest.metadata.fileType);
      assertNotNull(zipFile.getEntry("src/" + editedProgramName + ".twe"));
    }
  }

  @Test
  public void writesAndReadsSyntheticProjectResource() throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    TestResource resource = new TestResource("note.txt", "text/plain", "hello alice".getBytes(StandardCharsets.UTF_8));
    project.addResource(resource);
    File projectFile = temporaryFolder.newFile("synthetic-resource.a3p");

    IoUtilities.writeProject(projectFile, project);

    Project readProject = IoUtilities.readProject(projectFile);
    assertEquals(1, readProject.getResources().size());
    Resource readResource = readProject.getResources().iterator().next();
    assertEquals(TestResource.class, readResource.getClass());
    assertEquals(resource.getId(), readResource.getId());
    assertEquals("note.txt", readResource.getOriginalFileName());
    assertEquals("note.txt", readResource.getName());
    assertEquals("text/plain", readResource.getContentType());
    assertArrayEquals("hello alice".getBytes(StandardCharsets.UTF_8), readResource.getData());

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      assertNotNull(zipFile.getEntry("resources.xml"));
      assertNotNull(zipFile.getEntry("resources/note.txt"));
    }
  }

  @Test
  public void writeProjectPreservesBlankResourceOriginalFileName() throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    TestResource resource = new TestResource("note.txt", "text/plain", "hello alice".getBytes(StandardCharsets.UTF_8));
    resource.setOriginalFileName("");
    resource.setName("friendly note");
    project.addResource(resource);
    File projectFile = temporaryFolder.newFile("blank-original-resource.a3p");

    IoUtilities.writeProject(projectFile, project);

    Project readProject = IoUtilities.readProject(projectFile);
    Resource readResource = onlyResource(readProject);
    assertEquals("friendly note", readResource.getName());
    assertEquals("", readResource.getOriginalFileName());
    assertArrayEquals(resource.getData(), readResource.getData());
  }

  @Test
  public void xmlProjectReaderReportsMissingResourceDataArchiveEntry() throws Exception {
    File projectFile = temporaryFolder.newFile("missing-xml-resource-data.a3p");
    String entryName = "resources/missing.txt";
    String resourceName = "missing.txt";
    writeXmlProjectArchive(projectFile, TestResource.class.getName(), resourceName, entryName, null);

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectFile));

    assertTrue(thrown.getMessage().contains(resourceName));
    assertTrue(thrown.getMessage().contains(entryName));
  }

  @Test
  public void xmlProjectReaderReportsUnknownResourceClassWithContext() throws Exception {
    File projectFile = temporaryFolder.newFile("unknown-xml-resource-class.a3p");
    String entryName = "resources/ghost.txt";
    String resourceName = "ghost.txt";
    String className = "org.lgna.project.io.DoesNotExistResource";
    writeXmlProjectArchive(projectFile, className, resourceName, entryName, "ghost".getBytes(StandardCharsets.UTF_8));

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectFile));

    assertTrue(thrown.getMessage().contains(className));
    assertTrue(thrown.getMessage().contains(resourceName));
    assertTrue(thrown.getMessage().contains(entryName));
  }

  @Test
  public void writeProjectIncludesProvidedThumbnailAndManifestIcon() throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("synthetic-thumbnail.a3p");

    IoUtilities.writeProject(
        projectFile,
        project,
        new ByteArrayDataSource("thumbnail.png", thumbnailPng()));

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      assertNotNull(zipFile.getEntry("thumbnail.png"));
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals("thumbnail.png", manifest.description.icon);
    }
  }

  @Test
  public void writeProjectRemainsReadableWithoutThumbnailEntry() throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("synthetic-no-thumbnail.a3p");

    IoUtilities.writeProject(projectFile, project);

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      assertNull(zipFile.getEntry("thumbnail.png"));
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals("Program", manifest.description.name);
    }
    Project readProject = IoUtilities.readProject(projectFile);
    assertEquals("Program", readProject.getProgramType().getName());
  }

  @Test
  public void readsExportedPlayerArchiveImageResource() throws Exception {
    ImageResource imageResource = new ImageResource(
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
        "picture.png",
        "png");
    Project project = new Project(programTypeReferencingImageResource("Program", imageResource), Project.SceneCameraType.WindowCamera);
    project.addResource(imageResource);
    File exportFile = temporaryFolder.newFile("exported-image.a3w");

    IoUtilities.exportProject(exportFile, project);

    Project readProject = IoUtilities.readProject(exportFile);
    assertNull("Archives with unsupported Tweedle members remain undecoded.", readProject.getProgramType());
    assertEquals(1, readProject.getResources().size());
    Resource readResource = readProject.getResources().iterator().next();
    assertEquals(ImageResource.class, readResource.getClass());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals("picture.png", readResource.getOriginalFileName());
    assertEquals("picture.png", readResource.getName());
    assertEquals("png", readResource.getContentType());
    assertArrayEquals(imageResource.getData(), readResource.getData());
  }

  @Test
  public void readsExportedPlayerArchiveAudioResource() throws Exception {
    byte[] audioBytes = new byte[] {0, 1, 2, 3};
    File audioFile = temporaryFolder.newFile("sound.wav");
    Files.write(audioFile.toPath(), audioBytes);
    AudioResource audioResource = new AudioResource(audioFile, "audio.x_wav");
    Project project = new Project(programTypeReferencingResource("Program", AudioResource.class, audioResource), Project.SceneCameraType.WindowCamera);
    project.addResource(audioResource);
    File exportFile = temporaryFolder.newFile("exported-audio.a3w");

    IoUtilities.exportProject(exportFile, project);

    Project readProject = IoUtilities.readProject(exportFile);
    assertNull("Archives with unsupported Tweedle members remain undecoded.", readProject.getProgramType());
    assertEquals(1, readProject.getResources().size());
    Resource readResource = readProject.getResources().iterator().next();
    assertEquals(AudioResource.class, readResource.getClass());
    assertEquals(audioResource.getId(), readResource.getId());
    assertEquals("sound.wav", readResource.getOriginalFileName());
    assertEquals("sound.wav", readResource.getName());
    assertEquals("audio.x_wav", readResource.getContentType());
    assertArrayEquals(audioBytes, readResource.getData());
    assertEquals(0.0, ((AudioResource) readResource).getDuration(), 0.0);
  }

  @Test
  public void jsonPlayerReaderPreservesSceneCameraTypeFromManifestWithSimpleTweedleDecoding() throws Exception {
    Project project = new Project(programType("VrProgram"), Project.SceneCameraType.VRHeadset);
    File exportFile = temporaryFolder.newFile("exported-vr.a3w");

    IoUtilities.exportProject(exportFile, project);

    Project readProject = IoUtilities.readProject(exportFile);
    assertNotNull(readProject.getProgramType());
    assertEquals("VrProgram", readProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.VRHeadset, sceneCameraType(readProject));
    assertTrue(readProject.getResources().isEmpty());
  }

  @Test
  public void jsonPlayerReaderReportsMissingImageResourceData() throws Exception {
    ImageReference imageReference = imageReference(UUID.randomUUID(), "missing.png", "png");
    File exportFile = temporaryFolder.newFile("missing-image.a3w");
    writePlayerArchiveManifestOnly(exportFile, imageReference);

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(imageReference.file));
  }

  @Test
  public void jsonPlayerReaderReportsMissingAudioResourceData() throws Exception {
    AudioReference audioReference = audioReference(UUID.randomUUID(), "missing.wav", 1.0);
    File exportFile = temporaryFolder.newFile("missing-audio.a3w");
    writePlayerArchiveManifestOnly(exportFile, audioReference);

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(audioReference.file));
  }

  @Test
  public void jsonPlayerReaderReportsMissingImageResourceUuid() throws Exception {
    ImageReference imageReference = imageReference(null, "missing-id.png", "png");
    File exportFile = temporaryFolder.newFile("missing-image-id.a3w");
    writePlayerArchive(exportFile, imageReference, new byte[] {1, 2, 3});

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(imageReference.name));
    assertTrue(thrown.getMessage().contains("UUID"));
  }

  @Test
  public void jsonPlayerReaderReportsMissingAudioResourceUuid() throws Exception {
    AudioReference audioReference = audioReference(null, "missing-id.wav", 1.0);
    File exportFile = temporaryFolder.newFile("missing-audio-id.a3w");
    writePlayerArchive(exportFile, audioReference, new byte[] {0, 1, 2, 3});

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(audioReference.name));
    assertTrue(thrown.getMessage().contains("UUID"));
  }

  @Test
  public void readsExportedPlayerArchiveModelAndGeneratedTypeReferencesWithoutBinaryResources() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    ModelReference modelReference = new ModelReference();
    modelReference.name = "SyntheticDynamicProp";
    modelReference.format = "json";
    modelReference.file = "models/SyntheticDynamicProp/SyntheticDynamicProp.json";
    manifest.resources.add(modelReference);
    manifest.resources.add(new TypeReference("SyntheticDynamicPropResource", "src/SyntheticDynamicPropResource.twe", "tweedle"));
    File exportFile = temporaryFolder.newFile("exported-model-references.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, "models/SyntheticDynamicProp/SyntheticDynamicProp.json", "{}");
      writeZipEntry(zipOutputStream, "src/SyntheticDynamicPropResource.twe", "class SyntheticDynamicPropResource {}");
    }

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      assertNotNull(zipFile.getEntry("models/SyntheticDynamicProp/SyntheticDynamicProp.json"));
      assertNotNull(zipFile.getEntry("src/SyntheticDynamicPropResource.twe"));
      ProjectManifest archiveManifest = readProjectManifest(zipFile);
      assertEquals(2, archiveManifest.resources.size());
      assertTrue(archiveManifest.resources.get(0) instanceof ModelReference);
      assertTrue(archiveManifest.resources.get(1) instanceof TypeReference);
    }

    Project readProject = IoUtilities.readProject(exportFile);
    assertNotNull(readProject);
    assertNull("Generated type references are not treated as the program without a manifest name.", readProject.getProgramType());
    assertTrue(
        "Model and generated type references are manifest entries, not binary Resources",
        readProject.getResources().isEmpty());
  }

  @Test
  public void jsonPlayerReaderDecodesProgramTypeWhenManifestReferencesSimpleTweedleSource() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "ProgramFromManifest";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.VRHeadset;
    manifest.resources.add(new TypeReference("ProgramFromManifest", "src/ProgramFromManifest.twe", "tweedle"));
    File exportFile = temporaryFolder.newFile("manifest-program-boundary.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, "src/ProgramFromManifest.twe", "class ProgramFromManifest {}");
    }

    Project readProject = IoUtilities.readProject(exportFile);
    assertNotNull(readProject);
    assertNotNull(readProject.getProgramType());
    assertEquals("ProgramFromManifest", readProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.VRHeadset, sceneCameraType(readProject));
    assertTrue(readProject.getResources().isEmpty());
  }

  @Test
  public void jsonPlayerReaderReportsMissingProgramTypeReferenceForNamedManifest() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "ProgramWithoutTypeReference";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    File exportFile = temporaryFolder.newFile("manifest-without-program-type-reference.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue("Missing program type errors should name the manifest program.",
        thrown.getMessage().contains("ProgramWithoutTypeReference"));
    assertTrue("Missing program type errors should point at the absent type reference.",
        thrown.getMessage().contains("type reference"));
  }

  @Test
  public void jsonPlayerReaderReportsManifestProgramNameMismatchInsteadOfReturningNullProgram() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "ExpectedProgram";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    TypeReference typeReference = new TypeReference("OtherProgram", "src/OtherProgram.twe", "tweedle");
    manifest.resources.add(typeReference);
    File exportFile = temporaryFolder.newFile("manifest-program-name-mismatch.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class OtherProgram {}");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue("Program mismatch errors should name the manifest program.",
        thrown.getMessage().contains("ExpectedProgram"));
    assertTrue("Program mismatch errors should name the decoded type.",
        thrown.getMessage().contains("OtherProgram"));
  }

  @Test
  public void jsonPlayerReaderDefaultsMissingProjectStructureToWindowCamera() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "ProgramWithoutStructure";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure = null;
    TypeReference typeReference = new TypeReference("ProgramWithoutStructure", "src/ProgramWithoutStructure.twe", "tweedle");
    manifest.resources.add(typeReference);
    File exportFile = temporaryFolder.newFile("missing-project-structure.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class ProgramWithoutStructure {}");
    }

    Project readProject = IoUtilities.readProject(exportFile);
    assertNotNull(readProject);
    assertEquals("ProgramWithoutStructure", readProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.WindowCamera, sceneCameraType(readProject));
  }

  @Test
  public void xmlProjectReaderDefaultsMissingProjectStructureToWindowCamera() throws Exception {
    File projectFile = temporaryFolder.newFile("xml-missing-project-structure.a3p");
    String manifestJson = """
        {
          "description": {
            "name": "ProgramWithoutStructure"
          },
          "metadata": {
            "fileType": "a3p"
          }
        }
        """;

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(projectFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, manifestJson);
      writeZipEntry(zipOutputStream, "programType.xml", encodedProgramTypeXml("ProgramWithoutStructure"));
    }

    Project readProject = IoUtilities.readProject(projectFile);

    assertEquals("ProgramWithoutStructure", readProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.WindowCamera, sceneCameraType(readProject));
  }

  @Test
  public void ignoresUnsupportedJsonResourceReferencesWithoutCrashing() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    AliceTextureReference textureReference = new AliceTextureReference();
    textureReference.name = "SyntheticTexture";
    textureReference.format = "png";
    textureReference.file = "models/SyntheticProp/SyntheticTexture.png";
    StructureReference structureReference = new StructureReference();
    structureReference.name = "SyntheticStructure";
    structureReference.format = "glb";
    structureReference.file = "models/SyntheticProp/SyntheticStructure.glb";
    manifest.resources.add(textureReference);
    manifest.resources.add(structureReference);
    File exportFile = temporaryFolder.newFile("exported-unsupported-references.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, textureReference.file, new byte[] {1, 2, 3});
      writeZipEntry(zipOutputStream, structureReference.file, new byte[] {4, 5, 6});
    }

    Project readProject = IoUtilities.readProject(exportFile);
    assertNotNull(readProject);
    assertNull("Archives without Tweedle type references still have no decoded program type.", readProject.getProgramType());
    assertTrue(
        "Unsupported manifest references are not binary Project Resources",
        readProject.getResources().isEmpty());
  }

  @Test
  public void readsSimpleJsonTypeArchiveTweedleClass() throws Exception {
    File typeFile = temporaryFolder.newFile("json-simple-type.a3c");
    writeJsonTypeArchive(typeFile, "SyntheticType", "class SyntheticType {}");

    TypeResourcesPair readType = IoUtilities.readType(typeFile);

    assertNotNull("Simple Tweedle type archives should decode a NamedUserType.", readType.getType());
    assertEquals("SyntheticType", readType.getType().getName());
  }

  @Test
  public void xmlTypeRoundTripPreservesResourceDataAndExpressionBinding() throws Exception {
    TestResource resource = new TestResource("type-note.txt", "text/plain", "hello type".getBytes(StandardCharsets.UTF_8));
    NamedUserType type = programTypeReferencingResource("Prop", TestResource.class, resource);
    File typeFile = temporaryFolder.newFile("xml-type-resource.a3c");

    IoUtilities.writeType(typeFile, type);

    TypeResourcesPair readType = IoUtilities.readType(typeFile);
    assertNotNull(readType.getType());
    assertEquals("Prop", readType.getType().getName());
    Resource readResource = onlyResource(readType.getResources());
    assertEquals(TestResource.class, readResource.getClass());
    assertEquals(resource.getId(), readResource.getId());
    assertEquals("type-note.txt", readResource.getName());
    assertEquals("text/plain", readResource.getContentType());
    assertArrayEquals(resource.getData(), readResource.getData());
    assertSame(readResource, firstResourceExpressionResource(readType.getType()));
  }

  @Test
  public void jsonTypeReaderReportsManifestNameMismatchInsteadOfFallback() throws Exception {
    TypeManifest manifest = typeManifest("ExpectedType");
    TypeReference typeReference = new TypeReference("OtherType", "src/OtherType.twe", "tweedle");
    manifest.resources.add(typeReference);
    File typeFile = temporaryFolder.newFile("json-type-name-mismatch.a3c");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(typeFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class OtherType {}");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));

    assertTrue(thrown.getMessage().contains("ExpectedType"));
    assertTrue(thrown.getMessage().contains("OtherType"));
  }

  @Test
  public void jsonTypeReaderReportsMissingTypeReferenceInsteadOfReturningNull() throws Exception {
    File typeFile = temporaryFolder.newFile("json-type-without-type-reference.a3c");
    TypeManifest manifest = typeManifest("SyntheticType");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(typeFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));

    assertTrue(thrown.getMessage().contains("SyntheticType"));
    assertTrue(thrown.getMessage().contains("type reference"));
  }

  @Test
  public void jsonProjectReaderReportsUnsupportedTypeReferenceFormat() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    TypeReference typeReference = new TypeReference("Program", "src/Program.xml", "xml");
    manifest.resources.add(typeReference);
    File exportFile = temporaryFolder.newFile("unsupported-type-reference-format.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "<type/>");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains("Program"));
    assertTrue(thrown.getMessage().contains("xml"));
    assertTrue(thrown.getMessage().contains(typeReference.file));
  }

  @Test
  public void jsonTypeReaderReportsUnsupportedTypeReferenceFormat() throws Exception {
    TypeManifest manifest = typeManifest("SyntheticType");
    TypeReference typeReference = new TypeReference("SyntheticType", "src/SyntheticType.xml", "xml");
    manifest.resources.add(typeReference);
    File typeFile = temporaryFolder.newFile("unsupported-type-reference-format.a3c");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(typeFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "<type/>");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));

    assertTrue(thrown.getMessage().contains("SyntheticType"));
    assertTrue(thrown.getMessage().contains("xml"));
    assertTrue(thrown.getMessage().contains(typeReference.file));
  }

  @Test
  public void jsonPlayerReaderReportsMissingTweedleTypeEntry() throws Exception {
    TypeReference typeReference = new TypeReference("Program", "src/MissingProgram.twe", "tweedle");
    File exportFile = temporaryFolder.newFile("missing-tweedle-entry.a3w");
    writePlayerArchiveManifestOnly(exportFile, typeReference);

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(typeReference.file));
  }

  @Test
  public void jsonTypeReaderReportsMissingTweedleTypeEntry() throws Exception {
    TypeManifest manifest = typeManifest("SyntheticType");
    TypeReference typeReference = new TypeReference("SyntheticType", "src/MissingSyntheticType.twe", "tweedle");
    manifest.resources.add(typeReference);
    File typeFile = temporaryFolder.newFile("missing-tweedle-entry.a3c");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(typeFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));

    assertTrue(thrown.getMessage().contains(typeReference.file));
  }

  @Test
  public void jsonPlayerReaderWrapsMalformedTweedleTypeEntry() throws Exception {
    File exportFile = temporaryFolder.newFile("malformed-tweedle-entry.a3w");
    writeJsonPlayerArchive(exportFile, "Program", "class Program extends {}");

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains("Unable to decode Tweedle type entry"));
    assertTrue(thrown.getMessage().contains("src/Program.twe"));
  }

  @Test
  public void jsonTypeReaderWrapsMalformedTweedleTypeEntry() throws Exception {
    File typeFile = temporaryFolder.newFile("malformed-tweedle-entry.a3c");
    writeJsonTypeArchive(typeFile, "SyntheticType", "class SyntheticType extends {}");

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));

    assertTrue(thrown.getMessage().contains("Unable to decode Tweedle type entry"));
    assertTrue(thrown.getMessage().contains("src/SyntheticType.twe"));
  }

  @Test
  public void readsSimpleJsonPlayerArchiveTweedleProgram() throws Exception {
    File exportFile = temporaryFolder.newFile("json-simple-program.a3w");
    writeJsonPlayerArchive(exportFile, "Program", "class Program extends SProgram models Program {}");

    Project readProject = IoUtilities.readProject(exportFile);

    assertNotNull("Simple Tweedle player archives should decode a program type.", readProject.getProgramType());
    assertEquals("Program", readProject.getProgramType().getName());
  }

  @Test
  public void unsupportedJsonPlayerTweedleConstructsRemainUndecoded() throws Exception {
    File exportFile = temporaryFolder.newFile("json-unsupported-program.a3w");
    writeJsonPlayerArchive(exportFile, "Program", "class Program { WholeNumber count; }");

    Project readProject = IoUtilities.readProject(exportFile);

    assertNull("Unsupported Tweedle members remain documented null program type behavior for now.", readProject.getProgramType());
  }

  @Test
  public void unsupportedJsonTypeTweedleConstructsRemainUndecoded() throws Exception {
    File typeFile = temporaryFolder.newFile("json-unsupported-type.a3c");
    writeJsonTypeArchive(typeFile, "SyntheticType", "class SyntheticType { WholeNumber count; }");

    TypeResourcesPair readType = IoUtilities.readType(typeFile);

    assertNull("Unsupported Tweedle members remain documented null behavior for now.", readType.getType());
  }

  @Test
  public void unsupportedJsonTypeTweedleSuperclassRemainsUndecoded() throws Exception {
    File typeFile = temporaryFolder.newFile("json-unsupported-super-type.a3c");
    writeJsonTypeArchive(typeFile, "SyntheticType", "class SyntheticType extends MissingSuper {}");

    TypeResourcesPair readType = IoUtilities.readType(typeFile);

    assertNull("Unsupported Tweedle superclasses remain documented null behavior for now.", readType.getType());
  }

  @Test
  public void readsJsonTypeArchiveResourcesWhenUnsupportedTweedleRemainsUndecoded() throws Exception {
    ImageResource imageResource = imageResource("type-picture.png", 0xFFFF0000);
    NamedUserType type = programTypeReferencingImageResource("Prop", imageResource);
    File typeFile = temporaryFolder.newFile("json-type.a3c");

    try (FileOutputStream outputStream = new FileOutputStream(typeFile)) {
      ((ProjectIo.ProjectWriter) JsonProjectIo.writer()).writeType(outputStream, type, new DataSource[0]);
    }

    TypeResourcesPair readType = IoUtilities.readType(typeFile);
    assertNull("Archives with unsupported Tweedle members remain undecoded.", readType.getType());
    assertEquals(1, readType.getResources().size());
    Resource readResource = readType.getResources().iterator().next();
    assertEquals(ImageResource.class, readResource.getClass());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals("type-picture.png", readResource.getOriginalFileName());
    assertEquals("type-picture.png", readResource.getName());
    assertEquals("png", readResource.getContentType());
    assertArrayEquals(imageResource.getData(), readResource.getData());
  }

  @Test
  public void jsonTypeReaderReportsFutureVersion() throws Exception {
    String futureVersion = "999.0.0.0";
    File typeFile = temporaryFolder.newFile("future-type.a3c");
    writeTypeArchive(typeFile, futureVersion);

    Version version = IoUtilities.projectReader(typeFile).checkForFutureVersion();

    assertEquals(futureVersion, version.toString());
  }

  @Test
  public void jsonTypeReaderReportsMissingVersion() throws Exception {
    File typeFile = temporaryFolder.newFile("missing-version-type.a3c");
    writeArchive(typeFile, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(typeManifest()));

    IOException thrown = assertThrows(
        IOException.class,
        () -> IoUtilities.projectReader(typeFile).checkForFutureVersion());

    assertTrue(thrown.getMessage().contains(ProjectIo.VERSION_ENTRY_NAME));
  }

  @Test
  public void jsonTypeReaderMatchesPlayerReaderForCorruptVersion() throws Exception {
    String corruptVersion = "not-a-version";
    File typeFile = temporaryFolder.newFile("corrupt-version-type.a3c");
    File exportFile = temporaryFolder.newFile("corrupt-version-export.a3w");
    writeTypeArchive(typeFile, corruptVersion);
    writePlayerArchive(exportFile, corruptVersion);

    assertNull(IoUtilities.projectReader(typeFile).checkForFutureVersion());
    assertNull(IoUtilities.projectReader(exportFile).checkForFutureVersion());
  }

  @Test
  public void jsonPlayerReaderReportsFutureVersion() throws Exception {
    String futureVersion = "999.0.0.0";
    File exportFile = temporaryFolder.newFile("future-export.a3w");
    writePlayerArchive(exportFile, futureVersion);

    Version version = IoUtilities.projectReader(exportFile).checkForFutureVersion();

    assertEquals(futureVersion, version.toString());
  }

  @Test
  public void jsonPlayerReaderReportsMissingVersion() throws Exception {
    File exportFile = temporaryFolder.newFile("missing-version-export.a3w");
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    String manifestJson = ManifestEncoderDecoder.toJson(project.createExportManifest());
    writeArchive(exportFile, ProjectIo.MANIFEST_ENTRY_NAME, manifestJson);

    IOException thrown = assertThrows(
        IOException.class,
        () -> IoUtilities.projectReader(exportFile).checkForFutureVersion());

    assertTrue(thrown.getMessage().contains(ProjectIo.VERSION_ENTRY_NAME));
  }

  @Test
  public void corruptManifestDoesNotFallBackToXmlReader() throws Exception {
    File exportFile = temporaryFolder.newFile("corrupt-manifest-export.a3w");
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      String currentVersion = ProjectVersion.getCurrentVersion().toString();
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, currentVersion);
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, "{not-json");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.projectReader(exportFile));

    assertTrue(thrown.getMessage().contains(ProjectIo.MANIFEST_ENTRY_NAME));
  }

  @Test
  public void corruptTypeManifestDoesNotFallBackToXmlReader() throws Exception {
    File typeFile = temporaryFolder.newFile("corrupt-manifest-type.a3c");
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(typeFile))) {
      String currentVersion = ProjectVersion.getCurrentVersion().toString();
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, currentVersion);
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, "{not-json");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.projectReader(typeFile));

    assertTrue(thrown.getMessage().contains(ProjectIo.MANIFEST_ENTRY_NAME));
  }

  @Test
  public void missingTypeManifestUsesXmlTypeFallback() throws Exception {
    File typeFile = temporaryFolder.newFile("xml-type-without-manifest.a3c");
    IoUtilities.writeType(typeFile, programType("LegacyType"));

    try (ZipFile zipFile = new ZipFile(typeFile)) {
      assertNull(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("type.xml"));
    }
    TypeResourcesPair readType = IoUtilities.readType(typeFile);

    assertEquals("LegacyType", readType.getType().getName());
  }

  @Test
  public void jsonStyleTypeArchiveWithoutManifestFailsInXmlFallback() throws Exception {
    File typeFile = temporaryFolder.newFile("json-type-without-manifest.a3c");
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(typeFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, "src/SyntheticType.twe", "class SyntheticType {}");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));

    assertTrue(thrown.getMessage().contains("type.xml"));
  }

  @Test
  public void jsonStylePlayerArchiveWithoutManifestFailsInXmlFallback() throws Exception {
    File exportFile = temporaryFolder.newFile("json-player-without-manifest.a3w");
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, "src/Program.twe", "class Program {}");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains("programType.xml"));
  }

  @Test
  public void jsonPlayerExportUsesSafeDistinctResourceEntries() throws Exception {
    ImageResource first = imageResource("image.png", 0xFFFF0000);
    ImageResource duplicate = imageResource("image.png", 0xFF00FF00);
    ImageResource pathLike = imageResource("../folder/picture.png", 0xFF0000FF);
    Project project = new Project(
        programTypeReferencingImageResources("Program", first, duplicate, pathLike),
        Project.SceneCameraType.WindowCamera);
    File exportFile = temporaryFolder.newFile("safe-resource-entries.a3w");

    IoUtilities.exportProject(exportFile, project);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      assertNotNull(zipFile.getEntry("resources/image.png"));
      assertNotNull(zipFile.getEntry("resources2/image.png"));
      assertNotNull(zipFile.getEntry("resources/.._folder_picture.png"));
      assertNull(zipFile.getEntry("resources/../folder/picture.png"));
    }
    Project readProject = IoUtilities.readProject(exportFile);
    Map<UUID, Resource> resourcesById = resourcesById(readProject);
    assertArrayEquals(first.getData(), resourcesById.get(first.getId()).getData());
    assertArrayEquals(duplicate.getData(), resourcesById.get(duplicate.getId()).getData());
    assertArrayEquals(pathLike.getData(), resourcesById.get(pathLike.getId()).getData());
  }

  @Test
  public void jsonPlayerExportDoesNotLeakAbsoluteResourcePaths() throws Exception {
    ImageResource unixPath = imageResource("/Users/alice-secret/private-model-assets/unix-picture.png", 0xFFFF0000);
    ImageResource windowsPath = imageResource("C:\\Users\\alice-secret\\private-model-assets\\windows-picture.png", 0xFF00FF00);
    Project project = new Project(
        programTypeReferencingImageResources("Program", unixPath, windowsPath),
        Project.SceneCameraType.WindowCamera);
    File exportFile = temporaryFolder.newFile("absolute-path-resource-entries.a3w");

    IoUtilities.exportProject(exportFile, project);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      assertNotNull(zipFile.getEntry("resources/unix-picture.png"));
      assertNotNull(zipFile.getEntry("resources/windows-picture.png"));
      assertZipEntryNamesDoNotLeakLocalPaths(zipFile);
      assertNoLocalPathLeak(readZipEntryText(zipFile, ProjectIo.MANIFEST_ENTRY_NAME));
      assertNoLocalPathLeak(readZipEntryText(zipFile, "src/Program.twe"));

      ProjectManifest manifest = readProjectManifest(zipFile);
      assertImageReference(manifest, unixPath.getId(), "unix-picture.png", "resources/unix-picture.png");
      assertImageReference(manifest, windowsPath.getId(), "windows-picture.png", "resources/windows-picture.png");
    }
    Project readProject = IoUtilities.readProject(exportFile);
    Map<UUID, Resource> resourcesById = resourcesById(readProject);
    assertSafeReadbackResource(resourcesById.get(unixPath.getId()), "unix-picture.png", unixPath.getData());
    assertSafeReadbackResource(resourcesById.get(windowsPath.getId()), "windows-picture.png", windowsPath.getData());
  }

  @Test
  public void xmlProjectUsesSafeDistinctResourceEntries() throws Exception {
    ImageResource first = imageResource("image.png", 0xFFFF0000);
    ImageResource duplicate = imageResource("image.png", 0xFF00FF00);
    ImageResource slashPath = imageResource("../folder/picture.png", 0xFF0000FF);
    ImageResource backslashPath = imageResource("folder\\sound.png", 0xFFFFFF00);
    ImageResource dot = imageResource(".", 0xFFFF00FF);
    ImageResource dotdot = imageResource("..", 0xFF00FFFF);
    Project project = new Project(
        programTypeReferencingImageResources("Program", first, duplicate, slashPath, backslashPath, dot, dotdot),
        Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("safe-xml-resource-entries.a3p");

    IoUtilities.writeProject(projectFile, project);

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      assertNotNull(zipFile.getEntry("resources/image.png"));
      assertNotNull(zipFile.getEntry("resources2/image.png"));
      assertNotNull(zipFile.getEntry("resources/.._folder_picture.png"));
      assertNotNull(zipFile.getEntry("resources/folder_sound.png"));
      assertNotNull(zipFile.getEntry("resources/" + dot.getId()));
      assertNotNull(zipFile.getEntry("resources/" + dotdot.getId()));
      assertNull(zipFile.getEntry("resources/../folder/picture.png"));
      assertNull(zipFile.getEntry("resources/folder\\sound.png"));
      assertNull(zipFile.getEntry("resources/."));
      assertNull(zipFile.getEntry("resources/.."));
    }
    Project readProject = IoUtilities.readProject(projectFile);
    Map<UUID, Resource> resourcesById = resourcesById(readProject);
    assertArrayEquals(first.getData(), resourcesById.get(first.getId()).getData());
    assertArrayEquals(duplicate.getData(), resourcesById.get(duplicate.getId()).getData());
    assertArrayEquals(slashPath.getData(), resourcesById.get(slashPath.getId()).getData());
    assertArrayEquals(backslashPath.getData(), resourcesById.get(backslashPath.getId()).getData());
    assertArrayEquals(dot.getData(), resourcesById.get(dot.getId()).getData());
    assertArrayEquals(dotdot.getData(), resourcesById.get(dotdot.getId()).getData());
  }

  @Test
  public void xmlProjectExportDoesNotLeakAbsoluteResourcePaths() throws Exception {
    ImageResource unixPath = imageResource("/Users/alice-secret/private-model-assets/unix-picture.png", 0xFFFF0000);
    ImageResource windowsPath = imageResource("C:\\Users\\alice-secret\\private-model-assets\\windows-picture.png", 0xFF00FF00);
    Project project = new Project(
        programTypeReferencingImageResources("Program", unixPath, windowsPath),
        Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("absolute-path-resource-entries.a3p");

    IoUtilities.writeProject(projectFile, project);

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      assertNotNull(zipFile.getEntry("resources/unix-picture.png"));
      assertNotNull(zipFile.getEntry("resources/windows-picture.png"));
      assertZipEntryNamesDoNotLeakLocalPaths(zipFile);
      assertNoLocalPathLeak(readZipEntryText(zipFile, ProjectIo.MANIFEST_ENTRY_NAME));
      assertNoLocalPathLeak(readZipEntryText(zipFile, "programType.xml"));
      assertNoLocalPathLeak(readZipEntryText(zipFile, "resources.xml"));
    }
    Project readProject = IoUtilities.readProject(projectFile);
    Map<UUID, Resource> resourcesById = resourcesById(readProject);
    assertSafeReadbackResource(resourcesById.get(unixPath.getId()), "unix-picture.png", unixPath.getData());
    assertSafeReadbackResource(resourcesById.get(windowsPath.getId()), "windows-picture.png", windowsPath.getData());
  }

  @Test
  public void jsonPlayerImageReadsWithSameUuidDoNotMutateEarlierRead() throws Exception {
    UUID sharedId = UUID.randomUUID();
    byte[] firstData = new byte[] {1, 2, 3};
    byte[] secondData = new byte[] {4, 5, 6};
    File firstArchive = temporaryFolder.newFile("first-image.a3w");
    File secondArchive = temporaryFolder.newFile("second-image.a3w");
    writePlayerArchive(firstArchive, imageReference(sharedId, "first.png", "png"), firstData);
    writePlayerArchive(secondArchive, imageReference(sharedId, "second.png", "png"), secondData);

    ImageResource firstRead = (ImageResource) onlyResource(IoUtilities.readProject(firstArchive));
    ImageResource secondRead = (ImageResource) onlyResource(IoUtilities.readProject(secondArchive));

    assertNotSame(firstRead, secondRead);
    assertEquals(sharedId, firstRead.getId());
    assertEquals(sharedId, secondRead.getId());
    assertEquals("first.png", firstRead.getName());
    assertArrayEquals(firstData, firstRead.getData());
    assertEquals("second.png", secondRead.getName());
    assertArrayEquals(secondData, secondRead.getData());
  }

  @Test
  public void jsonPlayerReaderRejectsTraversalResourceReference() throws Exception {
    ImageReference imageReference = imageReference(UUID.randomUUID(), "evil.png", "png");
    imageReference.file = "../evil.png";
    File exportFile = temporaryFolder.newFile("traversal-resource.a3w");
    writePlayerArchive(exportFile, imageReference, new byte[] {1, 2, 3});

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(imageReference.file));
  }

  @Test
  public void jsonPlayerAudioReadsWithSameUuidDoNotMutateEarlierRead() throws Exception {
    UUID sharedId = UUID.randomUUID();
    byte[] firstData = new byte[] {1, 2, 3};
    byte[] secondData = new byte[] {4, 5, 6};
    File firstArchive = temporaryFolder.newFile("first-audio.a3w");
    File secondArchive = temporaryFolder.newFile("second-audio.a3w");
    writePlayerArchive(firstArchive, audioReference(sharedId, "first.wav", 1.0), firstData);
    writePlayerArchive(secondArchive, audioReference(sharedId, "second.wav", 2.0), secondData);

    AudioResource firstRead = (AudioResource) onlyResource(IoUtilities.readProject(firstArchive));
    AudioResource secondRead = (AudioResource) onlyResource(IoUtilities.readProject(secondArchive));

    assertNotSame(firstRead, secondRead);
    assertEquals(sharedId, firstRead.getId());
    assertEquals(sharedId, secondRead.getId());
    assertEquals("first.wav", firstRead.getName());
    assertArrayEquals(firstData, firstRead.getData());
    assertEquals(1.0, firstRead.getDuration(), 0.0);
    assertEquals("second.wav", secondRead.getName());
    assertArrayEquals(secondData, secondRead.getData());
    assertEquals(2.0, secondRead.getDuration(), 0.0);
  }

  @Test
  public void xmlProjectReaderRejectsTraversalResourceEntry() throws Exception {
    File projectFile = temporaryFolder.newFile("traversal-resource.a3p");
    UUID resourceId = UUID.randomUUID();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(projectFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, "programType.xml", encodedProgramTypeXml("Program"));
      writeZipEntry(zipOutputStream, "resources.xml",
          """
          <?xml version="1.0" encoding="UTF-8" standalone="no"?>
          <root>
            <resource
                className="%s"
                entryName="../evil.txt"
                uuid="%s"/>
          </root>
          """.formatted(TestResource.class.getName(), resourceId));
      writeZipEntry(zipOutputStream, "../evil.txt", "not safe");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectFile));

    assertTrue(thrown.getMessage().contains("../evil.txt"));
  }

  @Test
  public void xmlProjectImageReadsWithSameUuidDoNotMutateEarlierRead() throws Exception {
    UUID sharedId = UUID.randomUUID();
    byte[] firstData = new byte[] {1, 2, 3};
    byte[] secondData = new byte[] {4, 5, 6};
    File firstArchive = temporaryFolder.newFile("first-image.a3p");
    File secondArchive = temporaryFolder.newFile("second-image.a3p");
    ImageResource first = imageResource(sharedId, "first.png", firstData);
    ImageResource second = imageResource(sharedId, "second.png", secondData);
    IoUtilities.writeProject(firstArchive, projectReferencingResource("Program", ImageResource.class, first));
    IoUtilities.writeProject(secondArchive, projectReferencingResource("Program", ImageResource.class, second));

    Project firstProject = IoUtilities.readProject(firstArchive);
    ImageResource firstRead = (ImageResource) onlyResource(firstProject);
    Project secondProject = IoUtilities.readProject(secondArchive);
    ImageResource secondRead = (ImageResource) onlyResource(secondProject);

    assertNotSame(firstRead, secondRead);
    assertSame(firstRead, firstResourceExpressionResource(firstProject));
    assertSame(secondRead, firstResourceExpressionResource(secondProject));
    assertEquals(sharedId, firstRead.getId());
    assertEquals(sharedId, secondRead.getId());
    assertEquals("first.png", firstRead.getName());
    assertArrayEquals(firstData, firstRead.getData());
    assertEquals("second.png", secondRead.getName());
    assertArrayEquals(secondData, secondRead.getData());
  }

  @Test
  public void xmlProjectAudioReadsWithSameUuidDoNotMutateEarlierRead() throws Exception {
    UUID sharedId = UUID.randomUUID();
    byte[] firstData = new byte[] {1, 2, 3};
    byte[] secondData = new byte[] {4, 5, 6};
    File firstArchive = temporaryFolder.newFile("first-audio.a3p");
    File secondArchive = temporaryFolder.newFile("second-audio.a3p");
    AudioResource first = audioResource(sharedId, "first.wav", firstData, 1.0);
    AudioResource second = audioResource(sharedId, "second.wav", secondData, 2.0);
    IoUtilities.writeProject(firstArchive, projectReferencingResource("Program", AudioResource.class, first));
    IoUtilities.writeProject(secondArchive, projectReferencingResource("Program", AudioResource.class, second));

    Project firstProject = IoUtilities.readProject(firstArchive);
    AudioResource firstRead = (AudioResource) onlyResource(firstProject);
    Project secondProject = IoUtilities.readProject(secondArchive);
    AudioResource secondRead = (AudioResource) onlyResource(secondProject);

    assertNotSame(firstRead, secondRead);
    assertSame(firstRead, firstResourceExpressionResource(firstProject));
    assertSame(secondRead, firstResourceExpressionResource(secondProject));
    assertEquals(sharedId, firstRead.getId());
    assertEquals(sharedId, secondRead.getId());
    assertEquals("first.wav", firstRead.getName());
    assertArrayEquals(firstData, firstRead.getData());
    assertEquals(1.0, firstRead.getDuration(), 0.0);
    assertEquals("second.wav", secondRead.getName());
    assertArrayEquals(secondData, secondRead.getData());
    assertEquals(2.0, secondRead.getDuration(), 0.0);
  }

  @Test
  public void xmlProjectTestResourceReadsWithSameUuidDoNotMutateEarlierRead() throws Exception {
    UUID sharedId = UUID.randomUUID();
    byte[] firstData = "first".getBytes(StandardCharsets.UTF_8);
    byte[] secondData = "second".getBytes(StandardCharsets.UTF_8);
    File firstArchive = temporaryFolder.newFile("first-test-resource.a3p");
    File secondArchive = temporaryFolder.newFile("second-test-resource.a3p");
    TestResource first = testResource(sharedId, "first.txt", "text/plain", firstData);
    TestResource second = testResource(sharedId, "second.txt", "text/plain", secondData);
    IoUtilities.writeProject(firstArchive, projectReferencingResource("Program", TestResource.class, first));
    IoUtilities.writeProject(secondArchive, projectReferencingResource("Program", TestResource.class, second));

    Project firstProject = IoUtilities.readProject(firstArchive);
    TestResource firstRead = (TestResource) onlyResource(firstProject);
    Project secondProject = IoUtilities.readProject(secondArchive);
    TestResource secondRead = (TestResource) onlyResource(secondProject);

    assertNotSame(firstRead, secondRead);
    assertSame(firstRead, firstResourceExpressionResource(firstProject));
    assertSame(secondRead, firstResourceExpressionResource(secondProject));
    assertEquals(sharedId, firstRead.getId());
    assertEquals(sharedId, secondRead.getId());
    assertEquals("first.txt", firstRead.getName());
    assertArrayEquals(firstData, firstRead.getData());
    assertEquals("second.txt", secondRead.getName());
    assertArrayEquals(secondData, secondRead.getData());
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static NamedUserType programTypeReferencingImageResource(String name, ImageResource imageResource) {
    return programTypeReferencingImageResources(name, imageResource);
  }

  private static NamedUserType programTypeReferencingImageResources(String name, ImageResource... imageResources) {
    return programTypeReferencingResource(name, ImageResource.class, imageResources);
  }

  private static <T extends Resource> NamedUserType programTypeReferencingResource(String name, Class<T> resourceClass, T... resources) {
    NamedUserType type = programType(name);
    BlockStatement body = new BlockStatement();
    for (int i = 0; i < resources.length; i++) {
      UserLocal resource = new UserLocal("resource" + i, resourceClass, true);
      body.statements.add(new LocalDeclarationStatement(resource, new ResourceExpression(resourceClass, resources[i])));
    }
    UserMethod userMethod = new UserMethod("rememberResources", Void.TYPE, new org.lgna.project.ast.UserParameter[0], body);
    type.methods.add(userMethod);
    return type;
  }

  private static ImageResource imageResource(String fileName, int rgb) throws Exception {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, rgb);
    return new ImageResource(image, fileName, "png");
  }

  private static ImageResource imageResource(UUID uuid, String fileName, byte[] data) {
    ImageResource resource = new ImageResource(uuid);
    resource.setOriginalFileName(fileName);
    resource.setName(fileName);
    resource.setContent("png", data);
    resource.setWidth(1);
    resource.setHeight(1);
    return resource;
  }

  private static AudioResource audioResource(UUID uuid, String fileName, byte[] data, double duration) {
    AudioResource resource = new AudioResource(uuid);
    resource.setOriginalFileName(fileName);
    resource.setName(fileName);
    resource.setContent("audio.x_wav", data);
    resource.setDuration(duration);
    return resource;
  }

  private static TestResource testResource(UUID uuid, String fileName, String contentType, byte[] data) {
    TestResource resource = new TestResource(uuid);
    resource.setOriginalFileName(fileName);
    resource.setName(fileName);
    resource.setContent(contentType, data);
    return resource;
  }

  private static <T extends Resource> Project projectReferencingResource(String name, Class<T> resourceClass, T resource) {
    Project project = new Project(programTypeReferencingResource(name, resourceClass, resource), Project.SceneCameraType.WindowCamera);
    project.addResource(resource);
    return project;
  }

  private static Map<UUID, Resource> resourcesById(Project project) {
    Map<UUID, Resource> resources = new HashMap<>();
    for (Resource resource : project.getResources()) {
      resources.put(resource.getId(), resource);
    }
    return resources;
  }

  private static ProjectManifest readProjectManifest(ZipFile zipFile) throws IOException {
    return ManifestEncoderDecoder.fromJson(
        readZipEntryText(zipFile, ProjectIo.MANIFEST_ENTRY_NAME),
        ProjectManifest.class);
  }

  private static void assertImageReference(ProjectManifest manifest, UUID uuid, String name, String file) {
    for (ResourceReference resourceReference : manifest.resources) {
      if ((resourceReference instanceof ImageReference imageReference) && uuid.equals(imageReference.uuid)) {
        assertEquals(name, imageReference.name);
        assertEquals(file, imageReference.file);
        assertNoLocalPathLeak(imageReference.name);
        assertNoLocalPathLeak(imageReference.file);
        return;
      }
    }
    fail("Missing image reference for " + uuid);
  }

  private static void assertSafeReadbackResource(Resource resource, String expectedName, byte[] expectedData) {
    assertNotNull(resource);
    assertEquals(expectedName, resource.getName());
    assertEquals(expectedName, resource.getOriginalFileName());
    assertNoLocalPathLeak(resource.getName());
    assertNoLocalPathLeak(resource.getOriginalFileName());
    assertArrayEquals(expectedData, resource.getData());
  }

  private static void assertZipEntryNamesDoNotLeakLocalPaths(ZipFile zipFile) {
    zipFile.stream().forEach(entry -> assertNoLocalPathLeak(entry.getName()));
  }

  private static String readZipEntryText(ZipFile zipFile, String entryName) throws IOException {
    ZipEntry entry = zipFile.getEntry(entryName);
    assertNotNull(entry);
    try (InputStream inputStream = zipFile.getInputStream(entry)) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static void assertNoLocalPathLeak(String value) {
    assertFalse("Local Unix path leaked in " + value, value.contains("/Users/"));
    assertFalse("Local Windows drive leaked in " + value, value.contains("C:"));
    assertFalse("Local path owner leaked in " + value, value.contains("alice-secret"));
    assertFalse("Local path directory leaked in " + value, value.contains("private-model-assets"));
  }

  private static byte[] thumbnailPng() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", outputStream);
    return outputStream.toByteArray();
  }

  private static Resource onlyResource(Project project) {
    return onlyResource(project.getResources());
  }

  private static Resource onlyResource(Collection<Resource> resources) {
    assertEquals(1, resources.size());
    return resources.iterator().next();
  }

  private static Resource firstResourceExpressionResource(Project project) {
    return firstResourceExpressionResource(project.getProgramType());
  }

  private static Resource firstResourceExpressionResource(NamedUserType type) {
    IsInstanceCrawler<ResourceExpression> crawler = new IsInstanceCrawler<ResourceExpression>(ResourceExpression.class) {
      @Override
      protected boolean isAcceptable(ResourceExpression resourceExpression) {
        return true;
      }
    };
    type.crawl(crawler, CrawlPolicy.COMPLETE);
    assertFalse(crawler.getList().isEmpty());
    return crawler.getList().get(0).resource.getValue();
  }

  private static Project.SceneCameraType sceneCameraType(Project project) throws Exception {
    Field field = Project.class.getDeclaredField("sceneCameraType");
    field.setAccessible(true);
    return (Project.SceneCameraType) field.get(project);
  }

  private static ImageReference imageReference(UUID uuid, String name, String format) {
    ImageReference reference = new ImageReference();
    reference.uuid = uuid;
    reference.name = name;
    reference.format = format;
    reference.file = "resources/" + name;
    reference.width = 1;
    reference.height = 1;
    return reference;
  }

  private static AudioReference audioReference(UUID uuid, String name, double duration) {
    AudioReference reference = new AudioReference();
    reference.uuid = uuid;
    reference.name = name;
    reference.format = "audio.x_wav";
    reference.file = "resources/" + name;
    reference.duration = duration;
    return reference;
  }

  private static void writePlayerArchive(File file, String version) throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, version);
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(project.createExportManifest()));
    }
  }

  private static void writeTypeArchive(File file, String version) throws Exception {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, version);
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(typeManifest()));
    }
  }

  private static TypeManifest typeManifest() {
    return typeManifest("SyntheticType");
  }

  private static TypeManifest typeManifest(String name) {
    TypeManifest manifest = new TypeManifest();
    manifest.description.name = name;
    manifest.metadata.fileType = IoUtilities.TYPE_EXTENSION;
    manifest.metadata.identifier.name = name;
    manifest.metadata.identifier.type = Manifest.ProjectType.Library;
    return manifest;
  }

  private static void writeJsonTypeArchive(File file, String typeName, String tweedleSource) throws Exception {
    TypeManifest manifest = typeManifest(typeName);
    TypeReference typeReference = new TypeReference(typeName, "src/" + typeName + ".twe", "tweedle");
    manifest.resources.add(typeReference);
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, tweedleSource);
    }
  }

  private static void writeJsonPlayerArchive(File file, String programName, String tweedleSource) throws Exception {
    Project project = new Project(programType(programName), Project.SceneCameraType.WindowCamera);
    Manifest manifest = project.createExportManifest();
    TypeReference typeReference = new TypeReference(programName, "src/" + programName + ".twe", "tweedle");
    manifest.resources.add(typeReference);
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, tweedleSource);
    }
  }

  private static void writePlayerArchive(File file, ResourceReference resourceReference, byte[] data) throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    Manifest manifest = project.createExportManifest();
    TypeReference typeReference = new TypeReference("Program", "src/Program.twe", "tweedle");
    manifest.resources.add(typeReference);
    manifest.resources.add(resourceReference);
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class Program {}");
      writeZipEntry(zipOutputStream, resourceReference.file, data);
    }
  }

  private static void writePlayerArchiveManifestOnly(File file, ResourceReference resourceReference) throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    Manifest manifest = project.createExportManifest();
    manifest.resources.add(resourceReference);
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
    }
  }

  private static void writeArchive(File file, String entryName, String content) throws Exception {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeZipEntry(zipOutputStream, entryName, content);
    }
  }

  private static byte[] encodedProgramTypeXml(String name) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    XMLUtilities.write((new XmlEncoderDecoder()).encode(programType(name)), outputStream);
    return outputStream.toByteArray();
  }

  private static void writeXmlProjectArchive(
      File file,
      String resourceClassName,
      String resourceName,
      String resourceEntryName,
      byte[] resourceData) throws Exception {
    String manifestJson = """
        {
          "description": {
            "name": "Program"
          },
          "metadata": {
            "fileType": "a3p"
          },
          "projectStructure": {
            "sceneCameraType": "WindowCamera"
          }
        }
        """;
    String resourcesXml = String.format(
        """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <root>
              <resource className="%s" uuid="%s" entryName="%s" name="%s" originalFileName="%s" contentType="text/plain"/>
            </root>
            """,
        resourceClassName,
        UUID.randomUUID(),
        resourceEntryName,
        resourceName,
        resourceName);
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, manifestJson);
      writeZipEntry(zipOutputStream, "programType.xml", encodedProgramTypeXml("Program"));
      writeZipEntry(zipOutputStream, "resources.xml", resourcesXml);
      if (resourceData != null) {
        writeZipEntry(zipOutputStream, resourceEntryName, resourceData);
      }
    }
  }

  private static void writeZipEntry(ZipOutputStream zipOutputStream, String name, String content) throws Exception {
    zipOutputStream.putNextEntry(new ZipEntry(name));
    zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }

  private static void writeZipEntry(ZipOutputStream zipOutputStream, String name, byte[] content) throws Exception {
    zipOutputStream.putNextEntry(new ZipEntry(name));
    zipOutputStream.write(content);
    zipOutputStream.closeEntry();
  }

  public static class TestResource extends Resource {
    private static final Map<UUID, TestResource> uuidToResourceMap = new HashMap<>();

    public TestResource(String fileName, String contentType, byte[] data) {
      super(fileName, contentType, data);
      uuidToResourceMap.put(this.getId(), this);
    }

    private TestResource(UUID uuid) {
      super(uuid);
    }

    public static TestResource valueOf(String uuidText) {
      UUID uuid = UUID.fromString(uuidText);
      TestResource resource = uuidToResourceMap.get(uuid);
      if (resource == null) {
        resource = new TestResource(uuid);
        uuidToResourceMap.put(uuid, resource);
      }
      return resource;
    }
  }
}
