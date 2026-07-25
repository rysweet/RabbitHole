package org.lgna.project.io;

/** Audit note: this characterization-heavy test is ~2388 LOC and should be split into focused suites in a future refactoring. */

import edu.cmu.cs.dennisc.java.util.zip.ByteArrayDataSource;
import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import edu.cmu.cs.dennisc.pattern.IsInstanceCrawler;
import edu.cmu.cs.dennisc.print.PrintUtilities;
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
import org.lgna.project.ast.BooleanExpressionBodyPair;
import org.lgna.project.ast.ConditionalStatement;
import org.lgna.project.ast.CrawlPolicy;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ResourceExpression;
import org.lgna.project.ast.ThisExpression;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.story.SScene;
import org.lgna.story.SProgram;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    assertEquals(Project.SceneCameraType.WindowCamera, sceneCameraType(editedProject));
    assertTrue(editedProject.getResources().isEmpty());
    ProjectManifest editedReopenManifest = editedProject.createSaveManifest();
    assertEquals(editedProgramName, editedReopenManifest.description.name);
    assertEquals(IoUtilities.PROJECT_EXTENSION, editedReopenManifest.metadata.fileType);
    assertEquals(Project.SceneCameraType.WindowCamera, editedReopenManifest.projectStructure.sceneCameraType);
    assertTrue(editedReopenManifest.resources.isEmpty());
    try (ZipFile zipFile = new ZipFile(editedProjectFile)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      ProjectManifest saveManifest = readProjectManifest(zipFile);
      assertEquals(editedProgramName, saveManifest.description.name);
      assertEquals(IoUtilities.PROJECT_EXTENSION, saveManifest.metadata.fileType);
      assertEquals(Project.SceneCameraType.WindowCamera, saveManifest.projectStructure.sceneCameraType);
      TypeReference savedProgramReference = null;
      for (ResourceReference resourceReference : saveManifest.resources) {
        if (resourceReference instanceof TypeReference typeReference && editedProgramName.equals(typeReference.name)) {
          savedProgramReference = typeReference;
        }
      }
      assertNotNull(savedProgramReference);
      assertEquals("src/" + editedProgramName + ".twe", savedProgramReference.file);
      assertNotNull(zipFile.getEntry(savedProgramReference.file));
      assertNotNull(zipFile.getEntry("programType.xml"));
    }

    IoUtilities.exportProject(exportFile, editedProject);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      ProjectManifest exportManifest = readProjectManifest(zipFile);
      assertEquals(editedProgramName, exportManifest.description.name);
      assertEquals(IoUtilities.EXPORT_EXTENSION, exportManifest.metadata.fileType);
      assertEquals(Project.SceneCameraType.WindowCamera, exportManifest.projectStructure.sceneCameraType);
      assertNull(zipFile.getEntry("programType.xml"));

      TypeReference editedProgramReference = null;
      for (ResourceReference resourceReference : exportManifest.resources) {
        if (resourceReference instanceof TypeReference typeReference && editedProgramName.equals(typeReference.name)) {
          editedProgramReference = typeReference;
        }
      }
      assertNotNull(editedProgramReference);
      assertEquals("src/" + editedProgramName + ".twe", editedProgramReference.file);
      assertNotNull(zipFile.getEntry(editedProgramReference.file));
      assertTrue(readZipEntryText(zipFile, editedProgramReference.file).contains("class " + editedProgramName));
    }
  }

  @Test
  public void hybridProjectArchiveWritesBothXmlAndTweedlePayloads() throws Exception {
    String programName = "HybridStructureProgram";
    Project project = new Project(programType(programName), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("hybrid-structure.a3p");

    IoUtilities.writeProject(projectFile, project);

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("programType.xml"));
      assertNotNull(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("src/" + programName + ".twe"));

      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals(IoUtilities.PROJECT_EXTENSION, manifest.metadata.fileType);
      TypeReference programReference = findTypeReference(manifest, programName);
      assertNotNull(programReference);
      assertEquals("src/" + programName + ".twe", programReference.file);
      assertTrue(readZipEntryText(zipFile, "src/" + programName + ".twe").contains("class " + programName));
    }
  }

  @Test
  public void hybridTypeArchiveWritesBothXmlAndTweedlePayloads() throws Exception {
    String typeName = "HybridStructureType";
    File typeFile = temporaryFolder.newFile("hybrid-structure.a3c");

    IoUtilities.writeType(typeFile, programType(typeName));

    try (ZipFile zipFile = new ZipFile(typeFile)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("type.xml"));
      assertNotNull(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("src/" + typeName + ".twe"));

      TypeManifest manifest = readTypeManifest(zipFile);
      assertEquals(IoUtilities.TYPE_EXTENSION, manifest.metadata.fileType);
      assertNotNull(findTypeReference(manifest, typeName));
    }
  }

  @Test
  public void hybridProjectManifestRecordsBoundedTypeDependencies() throws Exception {
    NamedUserType sceneType = sceneType("DependencyScene");
    NamedUserType programType = programType("DependencyProgram");
    UserField sceneField = new UserField();
    sceneField.name.setValue("scene");
    sceneField.valueType.setValue(sceneType);
    programType.fields.add(sceneField);
    Set<NamedUserType> namedUserTypes = new HashSet<>();
    namedUserTypes.add(sceneType);
    Project project = new Project(
        programType,
        namedUserTypes,
        new HashSet<>(),
        Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("hybrid-dependencies.a3p");

    IoUtilities.writeProject(projectFile, project);

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      ProjectManifest manifest = readProjectManifest(zipFile);

      TypeReference programReference = findTypeReference(manifest, "DependencyProgram");
      assertNotNull(programReference);
      assertNotNull(
          "A type that references another user type must record it in the manifest dependency list",
          programReference.dependencies);
      assertTrue(
          "Program's bounded dependencies must include the referenced scene type",
          programReference.dependencies.contains("DependencyScene"));
      assertFalse(
          "A type must not list itself as a dependency",
          programReference.dependencies.contains("DependencyProgram"));

      TypeReference sceneReference = findTypeReference(manifest, "DependencyScene");
      assertNotNull(sceneReference);
      assertNull(
          "A type with no user-type references must omit the dependencies field",
          sceneReference.dependencies);
    }
  }

  @Test
  public void hybridTypeTweedlePayloadIsBoundedToTheExportedType() throws Exception {
    NamedUserType referencedType = sceneType("ReferencedScene");
    NamedUserType exportedType = programType("BoundedGalleryType");
    UserField field = new UserField();
    field.name.setValue("referencedScene");
    field.valueType.setValue(referencedType);
    exportedType.fields.add(field);
    File typeFile = temporaryFolder.newFile("bounded-gallery-type.a3c");

    IoUtilities.writeType(typeFile, exportedType);

    try (ZipFile zipFile = new ZipFile(typeFile)) {
      TypeManifest manifest = readTypeManifest(zipFile);
      assertNotNull(findTypeReference(manifest, exportedType.getName()));
      assertNull(findTypeReference(manifest, referencedType.getName()));
      assertNull(zipFile.getEntry("src/" + referencedType.getName() + ".twe"));
      String source = readZipEntryText(zipFile, "src/" + exportedType.getName() + ".twe");
      assertTrue(source.contains(referencedType.getName()));
      assertFalse(source.contains("class " + referencedType.getName()));
    }
  }

  @Test
  public void hybridProjectWithImageResourceRoundTripsAndRepointsManifest() throws Exception {
    ImageResource imageResource = new ImageResource(
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
        "hybrid-image.png",
        "png");
    Project project = new Project(
        programTypeReferencingImageResource("ImageResourceHost", imageResource),
        Project.SceneCameraType.WindowCamera);
    project.addResource(imageResource);
    File projectFile = temporaryFolder.newFile("hybrid-image-resource.a3p");

    IoUtilities.writeProject(projectFile, project);

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      ImageReference imageReference = null;
      for (ResourceReference resourceReference : manifest.resources) {
        if ((resourceReference instanceof ImageReference candidate)
            && imageResource.getId().equals(candidate.uuid)) {
          imageReference = candidate;
        }
      }
      assertNotNull(imageReference);
      assertNotNull(
          "Manifest image reference should be repointed to an existing archive resource binary",
          zipFile.getEntry(imageReference.file));
      assertArrayEquals(imageResource.getData(), readZipEntryBytes(zipFile, imageReference.file));
    }

    Project readProject = IoUtilities.readProject(projectFile);
    Resource recovered = resourcesById(readProject).get(imageResource.getId());
    assertSafeReadbackResource(recovered, "hybrid-image.png", imageResource.getData());
  }

  @Test
  public void hybridProjectReadFallsBackToXmlWhenTweedleSourceIsUnreadable() throws Exception {
    String programName = "CorruptTweedleProgram";
    Project project = new Project(programType(programName), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("hybrid-corrupt-source.a3p");
    IoUtilities.writeProject(projectFile, project);

    File corruptedFile = temporaryFolder.newFile("hybrid-corrupt-source-rewritten.a3p");
    rewriteZipEntry(
        projectFile,
        corruptedFile,
        "src/" + programName + ".twe",
        "this is not valid tweedle source {{{".getBytes(StandardCharsets.UTF_8));

    Project readProject = IoUtilities.readProject(corruptedFile);
    assertNotNull(readProject.getProgramType());
    assertEquals(programName, readProject.getProgramType().getName());
  }

  @Test
  public void hybridProjectFallbackDiscardsAllPartialTweedleTypes() throws Exception {
    NamedUserType sceneType = sceneType("FallbackScene");
    NamedUserType programType = programType("FallbackProgram");
    UserField sceneField = new UserField();
    sceneField.name.setValue("scene");
    sceneField.valueType.setValue(sceneType);
    programType.fields.add(sceneField);
    Set<NamedUserType> namedUserTypes = new HashSet<>();
    namedUserTypes.add(sceneType);
    Project project = new Project(
        programType,
        namedUserTypes,
        new HashSet<>(),
        Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("hybrid-partial-fallback.a3p");
    IoUtilities.writeProject(projectFile, project);

    File corruptedFile = temporaryFolder.newFile("hybrid-partial-fallback-rewritten.a3p");
    rewriteZipEntry(
        projectFile,
        corruptedFile,
        "src/" + sceneType.getName() + ".twe",
        "not valid Tweedle".getBytes(StandardCharsets.UTF_8));

    Project readProject = IoUtilities.readProject(corruptedFile);
    NamedUserType readScene = namedUserTypeNamed(readProject, sceneType.getName());
    assertEquals(programType.getId(), readProject.getProgramType().getId());
    assertEquals(sceneType.getId(), readScene.getId());
    assertSame(readScene, onlyField(readProject.getProgramType()).getValueType());
  }

  @Test
  public void fullyDecodableHybridProjectDoesNotReadXmlAstPayload() throws Exception {
    String programName = "TweedleOnlyReadProgram";
    Project project = new Project(programType(programName), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("hybrid-tweedle-read.a3p");
    IoUtilities.writeProject(projectFile, project);

    File corruptedFile = temporaryFolder.newFile("hybrid-tweedle-read-rewritten.a3p");
    rewriteZipEntry(
        projectFile,
        corruptedFile,
        XmlProjectIo.PROGRAM_TYPE_ENTRY_NAME,
        "not XML".getBytes(StandardCharsets.UTF_8));

    Project readProject = IoUtilities.readProject(corruptedFile);
    assertEquals(programName, readProject.getProgramType().getName());
  }

  @Test
  public void hybridProjectWithGenericResourceFallsBackToXmlYetRecoversResource() throws Exception {
    // A resource that is neither an image nor audio (the only two resource types the
    // Tweedle reader reconstructs) is written to resources.xml by the XML side but is
    // NOT recovered by the Tweedle read. The reader's resource-completeness gate must
    // therefore route the whole archive through the XML fallback so the resource is
    // not silently dropped. The program type itself decodes via Tweedle (see
    // fullyDecodableHybridProjectDoesNotReadXmlAstPayload), so the generic resource is
    // the sole reason the XML payload is required here.
    String programName = "GenericResourceHostProgram";
    TestResource genericResource =
        new TestResource("note.txt", "text/plain", "generic-resource".getBytes(StandardCharsets.UTF_8));
    Project project = new Project(programType(programName), Project.SceneCameraType.WindowCamera);
    project.addResource(genericResource);
    File projectFile = temporaryFolder.newFile("hybrid-generic-resource.a3p");
    IoUtilities.writeProject(projectFile, project);

    // The archive is a genuine hybrid (both payloads present); the generic resource
    // lives only in the XML resources.xml, not in the Tweedle manifest.
    try (ZipFile zipFile = new ZipFile(projectFile)) {
      assertNotNull(zipFile.getEntry("src/" + programName + ".twe"));
      assertNotNull(zipFile.getEntry("programType.xml"));
      assertNotNull(zipFile.getEntry("resources.xml"));
    }

    // Uncorrupted: the resource round-trips (via the XML fallback path).
    Project readProject = IoUtilities.readProject(projectFile);
    Resource recovered = resourcesById(readProject).get(genericResource.getId());
    assertNotNull("Generic resource must survive the hybrid round-trip", recovered);
    assertEquals(TestResource.class, recovered.getClass());
    assertArrayEquals(genericResource.getData(), recovered.getData());

    // Corrupting the XML payload proves the read genuinely depends on it: unlike the
    // resource-free case, a Tweedle-only read cannot satisfy this archive.
    File corruptedFile = temporaryFolder.newFile("hybrid-generic-resource-rewritten.a3p");
    rewriteZipEntry(
        projectFile,
        corruptedFile,
        XmlProjectIo.PROGRAM_TYPE_ENTRY_NAME,
        "not XML".getBytes(StandardCharsets.UTF_8));
    assertThrows(IOException.class, () -> IoUtilities.readProject(corruptedFile));
  }

  @Test
  public void hybridProjectManifestDoesNotLeakPlayerLibraryPrerequisite() throws Exception {
    // The Tweedle side of a hybrid archive is produced by JsonProjectIo.writeProject via
    // Project.createExportManifest(), which is the .a3w PLAYER manifest and injects the
    // "SceneGraphLibrary" player prerequisite. That prerequisite is meaningless for a
    // readable .a3c/.a3p and the hybrid merge must strip it (HybridProjectIo.mergeManifest).
    String programName = "PrerequisiteProgram";
    Project project = new Project(programType(programName), Project.SceneCameraType.WindowCamera);

    // Baseline: the standalone Tweedle player writer DOES emit the library prerequisite.
    File playerFile = temporaryFolder.newFile("prerequisite-player.a3w");
    try (FileOutputStream outputStream = new FileOutputStream(playerFile)) {
      JsonProjectIo.writer().writeProject(outputStream, project, new DataSource[0]);
    }
    try (ZipFile zipFile = new ZipFile(playerFile)) {
      ProjectManifest playerManifest = readProjectManifest(zipFile);
      assertTrue(
          "Player (.a3w) manifest is expected to declare the SceneGraphLibrary prerequisite",
          playerManifest.prerequisites.stream().anyMatch(identifier -> "SceneGraphLibrary".equals(identifier.name)));
    }

    // Hybrid .a3p: the merged manifest must NOT carry the player prerequisite.
    File projectFile = temporaryFolder.newFile("prerequisite-hybrid.a3p");
    IoUtilities.writeProject(projectFile, project);
    try (ZipFile zipFile = new ZipFile(projectFile)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals(IoUtilities.PROJECT_EXTENSION, manifest.metadata.fileType);
      assertTrue(
          "Hybrid .a3p manifest must not leak the .a3w player prerequisite",
          manifest.prerequisites.isEmpty());
    }
  }

  @Test
  public void hybridVrReadyReadUsesCompleteXmlMigrationPath() throws Exception {
    Project project = new Project(programType("VrReadyProgram"), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("hybrid-vr-ready.a3p");
    IoUtilities.writeProject(projectFile, project);

    Project readProject = IoUtilities.projectReader(projectFile).readProject(true);

    assertEquals(Project.SceneCameraType.VRHeadset, sceneCameraType(readProject));
  }

  @Test
  public void hybridProjectWriteStaysHybridWhenCallerSuppliesManifestDataSource() throws Exception {
    String programName = "IdeSaveProgram";
    Project project = new Project(programType(programName), Project.SceneCameraType.WindowCamera);
    // Mirror the IDE save path, which passes its own manifest.json (and a thumbnail) as data sources.
    DataSource callerManifest = new ByteArrayDataSource(
        ProjectIo.MANIFEST_ENTRY_NAME,
        ManifestEncoderDecoder.toJson(project.createSaveManifest()));
    DataSource thumbnail = new ByteArrayDataSource(
        "thumbnail.png",
        "fake-thumbnail".getBytes(StandardCharsets.UTF_8));
    File projectFile = temporaryFolder.newFile("hybrid-ide-save.a3p");

    IoUtilities.writeProject(projectFile, project, callerManifest, thumbnail);

    try (ZipFile zipFile = new ZipFile(projectFile)) {
      // The Tweedle payload must still be present — a caller manifest.json must not
      // silently degrade the archive to XML-only.
      assertNotNull(zipFile.getEntry("src/" + programName + ".twe"));
      assertNotNull(zipFile.getEntry("programType.xml"));
      // The caller's thumbnail is preserved via the XML side.
      assertNotNull(zipFile.getEntry("thumbnail.png"));
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals(programName, manifest.description.name);
      assertNotNull(findTypeReference(manifest, programName));
    }
  }

  @Test
  public void reopenedPlayerArchiveWithSiblingTypeCanBeEditedSavedAsProjectAndReopened() throws Exception {
    String originalProgramName = "OriginalPlayerProgram";
    String editedProgramName = "EditedPlayerProgram";
    String originalSceneName = "OriginalPlayerScene";
    String editedSceneName = "EditedPlayerScene";
    NamedUserType sceneType = sceneType(originalSceneName);
    NamedUserType programType = programType(originalProgramName);
    UserField sceneField = new UserField();
    sceneField.name.setValue("scene");
    sceneField.valueType.setValue(sceneType);
    programType.fields.add(sceneField);
    Set<NamedUserType> namedUserTypes = new HashSet<>();
    namedUserTypes.add(sceneType);
    Project project = new Project(programType, namedUserTypes, new HashSet<>(), Project.SceneCameraType.WindowCamera);
    File playerArchive = temporaryFolder.newFile("sibling-player.a3w");
    File savedProjectArchive = temporaryFolder.newFile("edited-sibling-project.a3p");

    IoUtilities.exportProject(playerArchive, project);
    Project reopenedPlayerArchive = IoUtilities.readProject(playerArchive);
    reopenedPlayerArchive.getProgramType().name.setValue(editedProgramName);
    namedUserTypeNamed(reopenedPlayerArchive, originalSceneName).name.setValue(editedSceneName);

    IoUtilities.writeProject(savedProjectArchive, reopenedPlayerArchive);

    Project reopenedProjectArchive = IoUtilities.readProject(savedProjectArchive);
    assertEquals(editedProgramName, reopenedProjectArchive.getProgramType().getName());
    NamedUserType reopenedSceneType = namedUserTypeNamed(reopenedProjectArchive, editedSceneName);
    assertSame(reopenedSceneType, onlyField(reopenedProjectArchive.getProgramType()).getValueType());
    assertEquals(Project.SceneCameraType.WindowCamera, sceneCameraType(reopenedProjectArchive));
  }

  @Test
  public void savedProjectReloadExportsPlayerArchiveAndReadsBackProgramContent() throws Exception {
    String programName = "ArchiveRoundTripProgram";
    Project project = new Project(programType(programName), Project.SceneCameraType.VRHeadset);
    File projectFile = temporaryFolder.newFile("archive-round-trip.a3p");
    File exportFile = temporaryFolder.newFile("archive-round-trip.a3w");

    IoUtilities.writeProject(projectFile, project);
    Project reloadedProject = IoUtilities.readProject(projectFile);
    assertEquals(programName, reloadedProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.VRHeadset, sceneCameraType(reloadedProject));

    IoUtilities.exportProject(exportFile, reloadedProject);

    assertTrue(exportFile.isFile());
    assertTrue(exportFile.length() > 0);
    try (ZipFile zipFile = new ZipFile(exportFile)) {
      ProjectManifest exportManifest = readProjectManifest(zipFile);
      assertEquals(programName, exportManifest.description.name);
      assertEquals(IoUtilities.EXPORT_EXTENSION, exportManifest.metadata.fileType);
      assertEquals(Project.SceneCameraType.VRHeadset, exportManifest.projectStructure.sceneCameraType);
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      assertNull(zipFile.getEntry("programType.xml"));

      TypeReference programReference = null;
      for (ResourceReference resourceReference : exportManifest.resources) {
        if (resourceReference instanceof TypeReference typeReference && programName.equals(typeReference.name)) {
          programReference = typeReference;
        }
      }
      assertNotNull(programReference);
      assertEquals("src/" + programName + ".twe", programReference.file);
      assertNotNull(zipFile.getEntry(programReference.file));
      assertTrue(readZipEntryText(zipFile, programReference.file).contains("class " + programName));
    }

    Project readExportedProject = IoUtilities.readProject(exportFile);
    assertNotNull(readExportedProject.getProgramType());
    assertEquals(programName, readExportedProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.VRHeadset, sceneCameraType(readExportedProject));
    assertTrue(readExportedProject.getResources().isEmpty());
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
  public void exportedPlayerArchiveImageResourceRemainsRecoverableWhenProgramTypeIsUnsupported() throws Exception {
    ImageResource imageResource = new ImageResource(
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
        "picture.png",
        "png");
    Project project = new Project(programTypeReferencingImageResource("Program", imageResource), Project.SceneCameraType.WindowCamera);
    project.addResource(imageResource);
    File exportFile = temporaryFolder.newFile("exported-image.a3w");

    IoUtilities.exportProject(exportFile, project);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertImageReference(manifest, imageResource.getId(), "picture.png", "resources/picture.png");
      assertArrayEquals(imageResource.getData(), readZipEntryBytes(zipFile, "resources/picture.png"));
    }
    Project readProject = IoUtilities.readProject(exportFile);
    assertNull("Unsupported program type should not be treated as a complete project", readProject.getProgramType());
    Resource readResource = onlyResource(readProject);
    assertEquals(ImageResource.class, readResource.getClass());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals("picture.png", readResource.getOriginalFileName());
    assertEquals("picture.png", readResource.getName());
    assertEquals("png", readResource.getContentType());
    assertArrayEquals(imageResource.getData(), readResource.getData());
  }

  @Test
  public void exportedPlayerArchiveAudioResourceRemainsManifestedWhenUnsupportedProgramTypeFailsClosed() throws Exception {
    byte[] audioBytes = new byte[] {0, 1, 2, 3};
    File audioFile = temporaryFolder.newFile("sound.wav");
    Files.write(audioFile.toPath(), audioBytes);
    AudioResource audioResource = new AudioResource(audioFile, "audio.x_wav");
    Project project = new Project(programTypeReferencingResource("Program", AudioResource.class, audioResource), Project.SceneCameraType.WindowCamera);
    project.addResource(audioResource);
    File exportFile = temporaryFolder.newFile("exported-audio.a3w");

    IoUtilities.exportProject(exportFile, project);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertAudioReference(manifest, audioResource.getId(), "sound.wav", "resources/sound.wav");
      assertArrayEquals(audioBytes, readZipEntryBytes(zipFile, "resources/sound.wav"));
    }
    assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
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
  public void jsonPlayerReaderRejectsImageEntryOutsideResourceDirectory() throws Exception {
    UUID imageId = UUID.randomUUID();
    ImageReference imageReference = imageReference(imageId, "legacy-picture.png", "png");
    imageReference.file = "legacy-images/legacy-picture.png";
    byte[] imageData = new byte[] {7, 8, 9};
    File exportFile = temporaryFolder.newFile("safe-relative-image-entry.a3w");
    writePlayerArchive(exportFile, imageReference, imageData);

    try {
      IoUtilities.readProject(exportFile);
      fail("Expected IOException for resource entry outside resources directory");
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("outside resources directory"));
    }
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
  public void jsonPlayerTweedleSimpleIfMethodCallDecodesProgramType() throws Exception {
    File exportFile = temporaryFolder.newFile("json-simple-if-program.a3w");
    writeJsonPlayerArchive(exportFile, "Program", """
        class Program extends SProgram {
          void run(Boolean ready) {
            if (ready) { this.helper(); }
          }
          void helper() { }
        }
        """);

    Project readProject = IoUtilities.readProject(exportFile);

    NamedUserType programType = readProject.getProgramType();
    assertNotNull("Player archive should decode the supported simple-if Tweedle slice.", programType);
    UserMethod run = userMethodNamed(programType, "run");
    UserMethod helper = userMethodNamed(programType, "helper");
    assertEquals(1, run.body.getValue().statements.size());
    assertTrue(run.body.getValue().statements.get(0) instanceof ConditionalStatement);
    ConditionalStatement conditional = (ConditionalStatement) run.body.getValue().statements.get(0);
    assertEquals(1, conditional.booleanExpressionBodyPairs.size());
    BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
    assertEquals(1, pair.body.getValue().statements.size());
    assertTrue(pair.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement statement = (ExpressionStatement) pair.body.getValue().statements.get(0);
    assertTrue(statement.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) statement.expression.getValue();
    assertSame(helper, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
    assertTrue(invocation.variableArguments.isEmpty());
    assertTrue(invocation.keyedArguments.isEmpty());
    assertEquals(0, conditional.elseBody.getValue().statements.size());
  }

  @Test
  public void jsonPlayerTweedleImplicitSameClassMethodCallDecodesProgramType() throws Exception {
    File exportFile = temporaryFolder.newFile("json-implicit-same-class-call-program.a3w");
    writeJsonPlayerArchive(exportFile, "Program", """
        class Program extends SProgram {
          void run(Boolean ready) {
            if (ready) { helper(); }
          }
          void helper() { }
        }
        """);

    Project readProject = IoUtilities.readProject(exportFile);

    NamedUserType programType = readProject.getProgramType();
    assertNotNull("Player archive should decode the supported implicit same-class call Tweedle slice.", programType);
    UserMethod run = userMethodNamed(programType, "run");
    UserMethod helper = userMethodNamed(programType, "helper");
    assertEquals(1, run.body.getValue().statements.size());
    assertTrue(run.body.getValue().statements.get(0) instanceof ConditionalStatement);
    ConditionalStatement conditional = (ConditionalStatement) run.body.getValue().statements.get(0);
    assertEquals(1, conditional.booleanExpressionBodyPairs.size());
    BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
    assertEquals(1, pair.body.getValue().statements.size());
    assertTrue(pair.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement statement = (ExpressionStatement) pair.body.getValue().statements.get(0);
    assertTrue(statement.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) statement.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(helper, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
    assertTrue(invocation.variableArguments.isEmpty());
    assertTrue(invocation.keyedArguments.isEmpty());
    assertEquals(0, conditional.elseBody.getValue().statements.size());
  }

  @Test
  public void jsonPlayerTweedleFieldDecodesProgramType() throws Exception {
    File exportFile = temporaryFolder.newFile("json-field-program.a3w");
    writeJsonPlayerArchive(exportFile, "Program", "class Program { WholeNumber count; }");

    Project readProject = IoUtilities.readProject(exportFile);

    assertSingleIntegerField(readProject.getProgramType(), "count");
  }

  @Test
  public void jsonPlayerTweedlePrimitiveFieldInitializerDecodesProgramType() throws Exception {
    File exportFile = temporaryFolder.newFile("json-initialized-field-program.a3w");
    writeJsonPlayerArchive(exportFile, "Program", "class Program { WholeNumber count <- 7; }");

    Project readProject = IoUtilities.readProject(exportFile);

    assertSingleIntegerFieldWithInitializer(readProject.getProgramType(), "count", 7);
  }

  @Test
  public void unsupportedLegacyProgramJsonArchiveFailsClosedWithClearBoundaryMessage() throws Exception {
    File exportFile = temporaryFolder.newFile("json-unsupported-super-program.a3w");
    writeJsonPlayerArchive(exportFile, "Program", "class Program extends MissingSuper {}");

    assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
  }

  @Test
  public void unsupportedLegacyProgramJsonArchiveWithAudioResourceDoesNotUseImageRecovery() throws Exception {
    TypeReference typeReference = new TypeReference("Program", "src/Program.twe", "tweedle");
    AudioReference audioReference = audioReference(UUID.randomUUID(), "legacy-sound.wav", 1.0);
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(typeReference);
    manifest.resources.add(audioReference);
    File exportFile = temporaryFolder.newFile("unsupported-legacy-program-audio.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class Program extends MissingSuper {}");
    }

    assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
  }

  @Test
  public void unsupportedLegacyProgramJsonArchiveWithImageAndUnsupportedResourceDoesNotPartiallyRecover() throws Exception {
    TypeReference typeReference = new TypeReference("Program", "src/Program.twe", "tweedle");
    ImageReference imageReference = imageReference(UUID.randomUUID(), "legacy-picture.png", "png");
    ModelReference modelReference = new ModelReference();
    modelReference.name = "LegacyModel";
    modelReference.format = "json";
    modelReference.file = "models/LegacyModel/LegacyModel.json";
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(typeReference);
    manifest.resources.add(imageReference);
    manifest.resources.add(modelReference);
    File exportFile = temporaryFolder.newFile("unsupported-legacy-program-image-and-model.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class Program extends MissingSuper {}");
    }

    assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
  }

  @Test
  public void unsupportedLegacyProgramJsonArchiveWithMissingImageDataFailsClosed() throws Exception {
    TypeReference typeReference = new TypeReference("Program", "src/Program.twe", "tweedle");
    ImageReference imageReference = imageReference(UUID.randomUUID(), "missing-legacy-picture.png", "png");
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(typeReference);
    manifest.resources.add(imageReference);
    File exportFile = temporaryFolder.newFile("unsupported-legacy-program-missing-image.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class Program extends MissingSuper {}");
    }

    IOException thrown = assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
    assertNotNull(thrown.getCause());
    assertTrue(thrown.getCause().getMessage().contains(imageReference.file));
  }

  @Test
  public void unsupportedLegacyProgramJsonArchiveWithUnsafeImagePathFailsClosed() throws Exception {
    String[] unsafeImagePaths = {
        "../evil.png",
        "resources/images/../../manifest.json",
        "/tmp/picture.png",
        "C:\\temp\\picture.png",
        "."
    };
    int archiveIndex = 0;
    for (String unsafeImagePath : unsafeImagePaths) {
      TypeReference typeReference = new TypeReference("Program", "src/Program.twe", "tweedle");
      ImageReference imageReference = imageReference(UUID.randomUUID(), "unsafe-legacy-picture.png", "png");
      imageReference.file = unsafeImagePath;
      ProjectManifest manifest = new ProjectManifest();
      manifest.description.name = "Program";
      manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
      manifest.metadata.identifier.name = UUID.randomUUID().toString();
      manifest.metadata.identifier.type = Manifest.ProjectType.World;
      manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
      manifest.resources.add(typeReference);
      manifest.resources.add(imageReference);
      File exportFile = temporaryFolder.newFile("unsupported-legacy-program-unsafe-image-" + archiveIndex++ + ".a3w");

      try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
        writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
        writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
        writeZipEntry(zipOutputStream, typeReference.file, "class Program extends MissingSuper {}");
      }

      IOException thrown = assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
      assertNotNull(thrown.getCause());
      assertTrue(
          "Cause should identify unsafe image path " + unsafeImagePath,
          thrown.getCause().getMessage().contains(unsafeImagePath));
    }
  }

  @Test
  public void unsupportedLegacyProgramJsonArchiveWithImageAndSiblingTypeDoesNotPartiallyRecover() throws Exception {
    TypeReference programTypeReference = new TypeReference("Program", "src/Program.twe", "tweedle");
    TypeReference siblingTypeReference = new TypeReference("Helper", "src/Helper.twe", "tweedle");
    ImageReference imageReference = imageReference(UUID.randomUUID(), "legacy-picture.png", "png");
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(programTypeReference);
    manifest.resources.add(siblingTypeReference);
    manifest.resources.add(imageReference);
    File exportFile = temporaryFolder.newFile("unsupported-legacy-program-image-and-sibling-type.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, programTypeReference.file, "class Program extends MissingSuper {}");
      writeZipEntry(zipOutputStream, siblingTypeReference.file, "class Helper extends MissingSuper {}");
    }

    assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
  }

  @Test
  public void legacyProgramJsonArchiveWithModelReferenceKeepsDecodedProgramReadable() throws Exception {
    TypeReference typeReference = new TypeReference("Program", "src/Program.twe", "tweedle");
    ModelReference modelReference = new ModelReference();
    modelReference.name = "LegacyModel";
    modelReference.format = "gltf";
    modelReference.file = "models/LegacyModel/LegacyModel.gltf";
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(typeReference);
    manifest.resources.add(modelReference);
    File exportFile = temporaryFolder.newFile("legacy-program-model-resource.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class Program { WholeNumber count; }");
      writeZipEntry(zipOutputStream, modelReference.file, "{}");
    }

    Project readProject = IoUtilities.readProject(exportFile);

    assertNotNull(readProject);
    assertSingleIntegerField(readProject.getProgramType(), "count");
    assertTrue(
        "Model references remain manifest entries; they are not binary resources read by this boundary.",
        readProject.getResources().isEmpty());
  }

  @Test
  public void unsupportedLegacyProgramJsonArchiveWithTwoImageReferencesDoesNotPartiallyRecover() throws Exception {
    TypeReference typeReference = new TypeReference("Program", "src/Program.twe", "tweedle");
    ImageReference imageReference1 = imageReference(UUID.randomUUID(), "legacy-picture-1.png", "png");
    ImageReference imageReference2 = imageReference(UUID.randomUUID(), "legacy-picture-2.png", "png");
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(typeReference);
    manifest.resources.add(imageReference1);
    manifest.resources.add(imageReference2);
    File exportFile = temporaryFolder.newFile("unsupported-legacy-program-two-images.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class Program extends MissingSuper {}");
      writeZipEntry(zipOutputStream, imageReference1.file, new byte[] {1, 2, 3});
      writeZipEntry(zipOutputStream, imageReference2.file, new byte[] {4, 5, 6});
    }

    assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
  }

  @Test
  public void diagnosticNameFormatsResourceFileNameAndUuid() {
    UUID uuid = UUID.randomUUID();
    ImageResource resource = new ImageResource(uuid);
    resource.setOriginalFileName("test-picture.png");

    String diagnostic = ResourceExportNames.diagnosticName(resource);

    assertEquals("test-picture.png (" + uuid + ")", diagnostic);
  }

  @Test
  public void diagnosticNameReturnsNullPlaceholderForNullResource() {
    assertEquals("<null>", ResourceExportNames.diagnosticName(null));
  }

  @Test
  public void modelResourceCrawlerCollectsResourceExpressionResources() {
    ImageResource imageResource = new ImageResource(UUID.randomUUID());
    imageResource.setOriginalFileName("crawled.png");
    imageResource.setContent("png", new byte[] {1});
    NamedUserType type = programType("Program");
    BlockStatement body = new BlockStatement();
    UserLocal local = new UserLocal("res", ImageResource.class, true);
    body.statements.add(new LocalDeclarationStatement(local, new ResourceExpression(ImageResource.class, imageResource)));
    UserMethod method = new UserMethod("resourceMethod", Void.TYPE, new org.lgna.project.ast.UserParameter[0], body);
    type.methods.add(method);

    ModelResourceCrawler crawler = new ModelResourceCrawler();
    type.crawl(crawler, CrawlPolicy.COMPLETE);

    assertTrue(crawler.resources.contains(imageResource));
    assertEquals(1, crawler.resources.size());
  }

  @Test
  public void jsonPlayerManifestTypeReadsFieldAndKeepsResourcesReadable() throws Exception {
    String programName = "ProgramWithField";
    TypeReference typeReference = new TypeReference(programName, "src/" + programName + ".twe", "tweedle");
    UUID imageId = UUID.randomUUID();
    ImageReference imageReference = imageReference(imageId, "boundary-picture.png", "png");
    byte[] imageData = new byte[] {1, 2, 3};
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = programName;
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(typeReference);
    manifest.resources.add(imageReference);
    File exportFile = temporaryFolder.newFile("manifest-type-field-resource.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class " + programName + " { WholeNumber count; }");
      writeZipEntry(zipOutputStream, imageReference.file, imageData);
    }

    Project readProject = IoUtilities.readProject(exportFile);

    assertNotNull(readProject);
    assertSingleIntegerField(readProject.getProgramType(), "count");
    Resource readResource = onlyResource(readProject);
    assertEquals(ImageResource.class, readResource.getClass());
    assertEquals(imageId, readResource.getId());
    assertEquals("boundary-picture.png", readResource.getOriginalFileName());
    assertEquals("boundary-picture.png", readResource.getName());
    assertEquals("png", readResource.getContentType());
    assertEquals(1, ((ImageResource) readResource).getWidth());
    assertEquals(1, ((ImageResource) readResource).getHeight());
    assertArrayEquals(imageData, readResource.getData());
  }

  @Test
  public void jsonTypeTweedleFieldDecodesType() throws Exception {
    File typeFile = temporaryFolder.newFile("json-field-type.a3c");
    writeJsonTypeArchive(typeFile, "SyntheticType", "class SyntheticType { WholeNumber count; }");

    TypeResourcesPair readType = IoUtilities.readType(typeFile);

    assertSingleIntegerField(readType.getType(), "count");
  }

  @Test
  public void jsonTypeTweedlePrimitiveFieldInitializerDecodesType() throws Exception {
    File typeFile = temporaryFolder.newFile("json-initialized-field-type.a3c");
    writeJsonTypeArchive(typeFile, "SyntheticType", "class SyntheticType { WholeNumber count <- 7; }");

    TypeResourcesPair readType = IoUtilities.readType(typeFile);

    assertSingleIntegerFieldWithInitializer(readType.getType(), "count", 7);
  }

  @Test
  public void unsupportedJsonTypeTweedleSuperclassFailsClosed() throws Exception {
    File typeFile = temporaryFolder.newFile("json-unsupported-super-type.a3c");
    writeJsonTypeArchive(typeFile, "SyntheticType", "class SyntheticType extends MissingSuper {}");

    assertUnsupportedTypeArchiveFailsClosed(typeFile, "SyntheticType");
  }

  @Test
  public void jsonTypeArchiveResourceRemainsManifestedWhenUnsupportedTypeFailsClosed() throws Exception {
    ImageResource imageResource = imageResource("type-picture.png", 0xFFFF0000);
    NamedUserType type = programTypeReferencingImageResource("Prop", imageResource);
    File typeFile = temporaryFolder.newFile("json-type.a3c");

    try (FileOutputStream outputStream = new FileOutputStream(typeFile)) {
      ((ProjectIo.ProjectWriter) JsonProjectIo.writer()).writeType(outputStream, type, new DataSource[0]);
    }

    try (ZipFile zipFile = new ZipFile(typeFile)) {
      TypeManifest manifest = readTypeManifest(zipFile);
      assertImageReference(manifest, imageResource.getId(), "type-picture.png", "resources/type-picture.png");
      assertArrayEquals(imageResource.getData(), readZipEntryBytes(zipFile, "resources/type-picture.png"));
    }
    assertUnsupportedTypeArchiveFailsClosed(typeFile, "Prop");
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
    try (FileOutputStream outputStream = new FileOutputStream(typeFile)) {
      ProjectIo.ProjectWriter xmlWriter = XmlProjectIo.writer();
      xmlWriter.writeType(outputStream, programType("LegacyType"), new DataSource[0]);
    }

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
  public void projectArchiveContractRejectsMalformedPlayerArchiveMetadataBeforeXmlFallback() throws Exception {
    File exportFile = temporaryFolder.newFile("missing-filetype.a3w");
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = null;
    manifest.resources.add(new TypeReference("Program", "src/Program.twe", "tweedle"));

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, "src/Program.twe", "class Program {}");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.projectReader(exportFile));

    assertTrue(thrown.getMessage().contains(ProjectIo.MANIFEST_ENTRY_NAME));
    assertTrue(thrown.getMessage().contains("fileType"));
  }

  @Test
  public void projectArchiveContractRejectsUnsafeSupplementalEntryNames() throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("unsafe-supplemental-entry.a3p");

    IOException thrown = assertThrows(
        IOException.class,
        () -> IoUtilities.writeProject(
            projectFile,
            project,
            new ByteArrayDataSource("../outside.txt", "unsafe".getBytes(StandardCharsets.UTF_8))));

    assertTrue(thrown.getMessage().contains("../outside.txt"));
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
    assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
  }

  @Test
  public void jsonPlayerExportDoesNotLeakAbsoluteResourcePaths() throws Exception {
    ImageResource unixPath = imageResource("/Users/alice-secret/private-model-assets/unix-picture.png", 0xFFFF0000);
    ImageResource windowsPath = imageResource("C:\\Users\\alice-secret\\private-model-assets\\windows-picture.png", 0xFF00FF00);
    Project project = new Project(
        programTypeReferencingImageResources("Program", unixPath, windowsPath),
        Project.SceneCameraType.WindowCamera);
    File exportFile = temporaryFolder.newFile("absolute-path-resource-entries.a3w");

    String diagnosticOutput = capturePrintUtilities(() -> IoUtilities.exportProject(exportFile, project));
    assertNoLocalPathLeak(diagnosticOutput);

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
    assertUnsupportedLegacyJsonProjectArchiveFailsClosed(exportFile);
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

    String diagnosticOutput = capturePrintUtilities(() -> IoUtilities.writeProject(projectFile, project));
    assertNoLocalPathLeak(diagnosticOutput);

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
  public void xmlProjectMissingResourceWarningDoesNotLeakAbsoluteResourcePath() throws Exception {
    ImageResource resource = imageResource("/Users/alice-secret/private-model-assets/warning-picture.png", 0xFFFF0000);
    Project project = new Project(
        programTypeReferencingImageResources("Program", resource),
        Project.SceneCameraType.WindowCamera);
    File projectFile = temporaryFolder.newFile("warning-path-resource.a3p");

    String output = captureStandardOutput(() -> IoUtilities.writeProject(projectFile, project));

    assertTrue(output.contains("WARNING: adding missing resource"));
    assertNoLocalPathLeak(output);
  }

  @Test
  public void jsonExportMissingResourceWarningDoesNotLeakAbsoluteResourcePath() throws Exception {
    ImageResource resource = imageResource("C:\\Users\\alice-secret\\private-model-assets\\warning-picture.png", 0xFFFF0000);
    Project project = new Project(
        programTypeReferencingImageResources("Program", resource),
        Project.SceneCameraType.WindowCamera);
    File exportFile = temporaryFolder.newFile("warning-path-resource.a3w");

    String output = captureStandardOutput(() -> IoUtilities.exportProject(exportFile, project));

    assertTrue(output.contains("WARNING: added missing resource"));
    assertNoLocalPathLeak(output);
  }

  @Test
  public void resourceEntryNameValidationRejectsUnsafeArchivePaths() {
    assertTrue(ResourceExportNames.isResourceEntryName("resources/image.png"));
    assertTrue(ResourceExportNames.isResourceEntryName("resources2/image.png"));

    for (String entryName : new String[] {
        null,
        "",
        "resources",
        "resources2",
        "resources/../evil.png",
        "resources/./evil.png",
        "resources//evil.png",
        "resources2/../evil.png",
        "resource/evil.png",
        "resourcesx/evil.png",
        "resources-2/evil.png",
        "resources2evil/evil.png",
        "/resources/evil.png",
        "\\resources\\evil.png",
        "C:/resources/evil.png",
        "resources/C:/evil.png",
        "resources\\evil.png",
        "resources/",
        "resources2/",
    }) {
      assertFalse("expected unsafe resource entry to be rejected: " + entryName,
          ResourceExportNames.isResourceEntryName(entryName));
    }
  }

  @Test
  public void sourceEntryNameValidationRejectsUnsafeArchivePaths() {
    assertTrue(ResourceExportNames.isSourceEntryName("src/Program.twe"));

    for (String entryName : new String[] {
        null,
        "",
        "src",
        "src/../Program.twe",
        "src/./Program.twe",
        "src//Program.twe",
        "src2/Program.twe",
        "source/Program.twe",
        "src../Program.twe",
        "/src/Program.twe",
        "\\src\\Program.twe",
        "C:/src/Program.twe",
        "src/C:/Program.twe",
        "src\\Program.twe",
        "src/",
    }) {
      assertFalse("expected unsafe source entry to be rejected: " + entryName,
          ResourceExportNames.isSourceEntryName(entryName));
    }
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
  public void jsonPlayerReaderRejectsResourceReferenceOutsideResourceDirectory() throws Exception {
    ImageReference imageReference = imageReference(UUID.randomUUID(), "evil.png", "png");
    imageReference.file = "evil.png";
    File exportFile = temporaryFolder.newFile("unexpected-resource-location.a3w");
    writePlayerArchive(exportFile, imageReference, new byte[] {1, 2, 3});

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(imageReference.file));
    assertTrue(thrown.getMessage().contains("resources"));
  }

  @Test
  public void jsonPlayerReaderRejectsTraversalTypeReference() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    TypeReference typeReference = new TypeReference("Program", "../Program.twe", "tweedle");
    manifest.resources.add(typeReference);
    File exportFile = temporaryFolder.newFile("traversal-type.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class Program {}");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(typeReference.file));
  }

  @Test
  public void jsonPlayerReaderRejectsTypeReferenceOutsideSourceDirectory() throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = UUID.randomUUID().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    TypeReference typeReference = new TypeReference("Program", "Program.twe", "tweedle");
    manifest.resources.add(typeReference);
    File exportFile = temporaryFolder.newFile("unexpected-type-location.a3w");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class Program {}");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

    assertTrue(thrown.getMessage().contains(typeReference.file));
    assertTrue(thrown.getMessage().contains("src"));
  }

  @Test
  public void jsonTypeReaderRejectsTraversalTypeReference() throws Exception {
    TypeManifest manifest = typeManifest("SyntheticType");
    TypeReference typeReference = new TypeReference("SyntheticType", "../SyntheticType.twe", "tweedle");
    manifest.resources.add(typeReference);
    File typeFile = temporaryFolder.newFile("traversal-type.a3c");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(typeFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
      writeZipEntry(zipOutputStream, typeReference.file, "class SyntheticType {}");
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));

    assertTrue(thrown.getMessage().contains(typeReference.file));
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
  public void xmlProjectReaderRejectsResourceEntryOutsideResourceDirectory() throws Exception {
    File projectFile = temporaryFolder.newFile("unexpected-resource-location.a3p");
    writeXmlProjectArchive(
        projectFile,
        TestResource.class.getName(),
        "evil.txt",
        "evil.txt",
        "not safe".getBytes(StandardCharsets.UTF_8));

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectFile));

    assertTrue(thrown.getMessage().contains("evil.txt"));
    assertTrue(thrown.getMessage().contains("resources"));
  }

  @Test
  public void xmlProjectReaderRejectsExternalEntityInResourcesXml() throws Exception {
    File externalEntityFile = temporaryFolder.newFile("project-xxe.txt");
    Files.write(externalEntityFile.toPath(), "project external entity should not be read".getBytes(StandardCharsets.UTF_8));
    File projectFile = temporaryFolder.newFile("external-entity-resource.a3p");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(projectFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, "programType.xml", encodedProgramTypeXml("Program"));
      writeZipEntry(zipOutputStream, "resources.xml", externalEntityResourcesXml(externalEntityFile));
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectFile));

    assertTrue(thrown.getMessage().contains("resources.xml"));
  }

  @Test
  public void xmlTypeReaderRejectsExternalEntityInResourcesXml() throws Exception {
    File externalEntityFile = temporaryFolder.newFile("type-xxe.txt");
    Files.write(externalEntityFile.toPath(), "type external entity should not be read".getBytes(StandardCharsets.UTF_8));
    File typeFile = temporaryFolder.newFile("external-entity-resource.a3c");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(typeFile))) {
      writeZipEntry(zipOutputStream, ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
      writeZipEntry(zipOutputStream, "type.xml", encodedTypeXml("LibraryType"));
      writeZipEntry(zipOutputStream, "resources.xml", externalEntityResourcesXml(externalEntityFile));
    }

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));

    assertTrue(thrown.getMessage().contains("resources.xml"));
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
    return userType(name, SProgram.class);
  }

  private static NamedUserType sceneType(String name) {
    return userType(name, SScene.class);
  }

  private static NamedUserType userType(String name, Class<?> superClass) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(superClass));
    return type;
  }

  private static NamedUserType namedUserTypeNamed(Project project, String name) {
    for (NamedUserType namedUserType : project.getNamedUserTypes()) {
      if (name.equals(namedUserType.getName())) {
        return namedUserType;
      }
    }
    fail("Missing named user type " + name);
    return null;
  }

  private static UserField onlyField(NamedUserType type) {
    assertEquals(1, type.getDeclaredFields().size());
    return type.getDeclaredFields().get(0);
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

  private static TypeManifest readTypeManifest(ZipFile zipFile) throws IOException {
    return ManifestEncoderDecoder.fromJson(
        readZipEntryText(zipFile, ProjectIo.MANIFEST_ENTRY_NAME),
        TypeManifest.class);
  }

  private static IOException assertUnsupportedLegacyJsonProjectArchiveFailsClosed(File exportFile) {
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));
    assertTrue(thrown.getMessage().contains("Unsupported legacy JSON project archive"));
    assertTrue(thrown.getMessage().contains("Program Tweedle decode is unsupported"));
    assertTrue(thrown.getMessage().contains("no safe legacy resource recovery applies"));
    return thrown;
  }

  private static IOException assertUnsupportedTypeArchiveFailsClosed(File typeFile, String expectedTypeName) {
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeFile));
    assertTrue(thrown.getMessage().contains("Type archive manifest names '" + expectedTypeName + "'"));
    assertTrue(thrown.getMessage().contains("decoded type names are []"));
    return thrown;
  }

  private static void assertImageReference(Manifest manifest, UUID uuid, String name, String file) {
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

  private static void assertAudioReference(Manifest manifest, UUID uuid, String name, String file) {
    for (ResourceReference resourceReference : manifest.resources) {
      if ((resourceReference instanceof AudioReference audioReference) && uuid.equals(audioReference.uuid)) {
        assertEquals(name, audioReference.name);
        assertEquals(file, audioReference.file);
        assertNoLocalPathLeak(audioReference.name);
        assertNoLocalPathLeak(audioReference.file);
        return;
      }
    }
    fail("Missing audio reference for " + uuid);
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
    return new String(readZipEntryBytes(zipFile, entryName), StandardCharsets.UTF_8);
  }

  private static byte[] readZipEntryBytes(ZipFile zipFile, String entryName) throws IOException {
    ZipEntry entry = zipFile.getEntry(entryName);
    assertNotNull(entry);
    try (InputStream inputStream = zipFile.getInputStream(entry)) {
      return inputStream.readAllBytes();
    }
  }

  private static void assertNoLocalPathLeak(String value) {
    assertFalse("Local Unix path leaked in " + value, value.contains("/Users/"));
    assertFalse("Local Windows drive leaked in " + value, value.contains("C:"));
    assertFalse("Local path owner leaked in " + value, value.contains("alice-secret"));
    assertFalse("Local path directory leaked in " + value, value.contains("private-model-assets"));
  }

  private static String capturePrintUtilities(ThrowingRunnable action) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PrintUtilities.pushPrintStream();
    try (PrintStream printStream = new PrintStream(output, true, StandardCharsets.UTF_8.name())) {
      PrintUtilities.setPrintStream(printStream);
      action.run();
    } finally {
      PrintUtilities.popPrintStream();
    }
    return new String(output.toByteArray(), StandardCharsets.UTF_8);
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private static String captureStandardOutput(IoAction action) throws Exception {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    PrintStream capturedOut = new PrintStream(stdout, true, StandardCharsets.UTF_8);
    PrintUtilities.pushPrintStream();
    try {
      PrintUtilities.setPrintStream(capturedOut);
      action.run();
    } finally {
      PrintUtilities.popPrintStream();
      capturedOut.close();
    }
    return stdout.toString(StandardCharsets.UTF_8);
  }

  private interface IoAction {
    void run() throws Exception;
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

  private static void assertSingleIntegerField(NamedUserType type, String expectedName) {
    assertNotNull(type);
    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals(expectedName, field.getName());
    assertSame(JavaType.getInstance(Integer.class), field.getValueType());
  }

  private static void assertSingleIntegerFieldWithInitializer(NamedUserType type, String expectedName, int expectedValue) {
    assertSingleIntegerField(type, expectedName);
    Expression initializer = type.getDeclaredFields().get(0).initializer.getValue();
    assertTrue(initializer instanceof IntegerLiteral);
    assertEquals(expectedValue, ((IntegerLiteral) initializer).value.getValue().intValue());
  }

  private static UserMethod userMethodNamed(NamedUserType type, String name) {
    for (UserMethod method : type.getDeclaredMethods()) {
      if (name.equals(method.getName())) {
        return method;
      }
    }
    fail("Missing method " + name);
    return null;
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

  private static byte[] encodedTypeXml(String name) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    XMLUtilities.write((new XmlEncoderDecoder()).encode(programType(name)), outputStream);
    return outputStream.toByteArray();
  }

  private static String externalEntityResourcesXml(File externalEntityFile) {
    return """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <!DOCTYPE root [
          <!ENTITY archiveXxe SYSTEM "%s">
        ]>
        <root>&archiveXxe;</root>
        """.formatted(externalEntityFile.toURI());
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

  private static TypeReference findTypeReference(Manifest manifest, String typeName) {
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof TypeReference typeReference && typeName.equals(typeReference.name)) {
        return typeReference;
      }
    }
    return null;
  }

  private static void rewriteZipEntry(File source, File destination, String entryToReplace, byte[] newContent)
      throws Exception {
    try (ZipFile zipFile = new ZipFile(source);
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destination))) {
      for (ZipEntry entry : java.util.Collections.list(zipFile.entries())) {
        byte[] content = entry.getName().equals(entryToReplace)
            ? newContent
            : readZipEntryBytes(zipFile, entry.getName());
        writeZipEntry(zipOutputStream, entry.getName(), content);
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
