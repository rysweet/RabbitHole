package org.alice.ide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.lgna.common.Resource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ResourceExpression;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.io.IoUtilities;
import org.lgna.project.io.ProjectIo;
import org.lgna.story.SProgram;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

public class ProjectFileUtilitiesTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final ProjectFileUtilities utilities = new ProjectFileUtilities(null);

  @Test
  public void savedProjectBackupDirectoryUsesSiblingBakDirectory() throws IOException {
    File project = temporaryFolder.newFile("world.a3p");

    Path backupDirectory = utilities.backupDirectory(project, false);

    assertEquals(temporaryFolder.getRoot().toPath().resolve("world.bak"), backupDirectory);
    assertTrue(Files.isDirectory(backupDirectory));
  }

  @Test
  public void nonProjectFileBackupDirectoryUsesFullFileName() throws IOException {
    File project = temporaryFolder.newFile("world.txt");

    Path backupDirectory = utilities.backupDirectory(project, false);

    assertEquals(temporaryFolder.getRoot().toPath().resolve("world.txt.bak"), backupDirectory);
    assertTrue(Files.isDirectory(backupDirectory));
  }

  @Test
  public void backupFileUsesParentDirectory() throws IOException {
    File backupDirectory = temporaryFolder.newFolder("world.bak");
    File backupFile = new File(backupDirectory, "auto20240102_120000.a3p");

    Path resolvedDirectory = utilities.backupDirectory(backupFile, true);

    assertEquals(backupDirectory.toPath(), resolvedDirectory);
  }

  @Test
  public void parentlessBackupFileHasNoBackupDirectory() {
    File backupFile = new File("auto20240102_120000.a3p");

    Path resolvedDirectory = utilities.backupDirectory(backupFile, true);

    assertNull(resolvedDirectory);
  }

  @Test
  public void namedBackupPathFileReturnsNull() throws IOException {
    File project = temporaryFolder.newFile("world.a3p");
    Path collidingBackupPath = temporaryFolder.getRoot().toPath()
        .resolve("world.bak");
    Files.writeString(
        collidingBackupPath,
        "not a directory",
        StandardCharsets.UTF_8);

    Path backupDirectory = utilities.backupDirectory(project, false);

    assertNull(backupDirectory);
    assertTrue(Files.isRegularFile(collidingBackupPath));
    assertEquals(
        "not a directory",
        Files.readString(collidingBackupPath, StandardCharsets.UTF_8));
  }

  @Test
  public void missingParentBackupDirectoryReturnsNull() {
    File project = temporaryFolder.getRoot().toPath()
        .resolve("missing-parent/world.a3p")
        .toFile();

    Path backupDirectory = utilities.backupDirectory(project, false);

    assertNull(backupDirectory);
    assertFalse(Files.exists(project.toPath().getParent()));
  }

  @Test
  public void pr426BackupPathContractKeepsNamedBackupDirectoryBesideProjectFile()
      throws IOException {
    Path lessonDirectory = temporaryFolder.newFolder("classroom").toPath();
    File project = lessonDirectory.resolve("lesson.a3p").toFile();
    Files.writeString(project.toPath(), "project", StandardCharsets.UTF_8);

    Path backupDirectory = utilities.backupDirectory(project, false);

    assertEquals(lessonDirectory.resolve("lesson.bak"), backupDirectory);
    assertTrue(backupDirectory.startsWith(lessonDirectory));
    assertTrue(Files.isDirectory(backupDirectory));
  }

  @Test
  public void copyDefaultBackupDirectoryMovesAutoProjectBackupsToNamedBackupDirectory() throws IOException {
    Path defaultBackupDirectory = temporaryFolder.newFolder(".defaultbak").toPath();
    Path firstBackup = defaultBackupDirectory.resolve("auto20240102_120000.a3p");
    Path secondBackup = defaultBackupDirectory.resolve("auto20240102_130000.a3p");
    Path savedBackup = defaultBackupDirectory.resolve("save20240102_130000.a3p");
    Path nonProjectBackup = defaultBackupDirectory.resolve("auto20240102_140000.txt");
    Files.writeString(firstBackup, "first", StandardCharsets.UTF_8);
    Files.writeString(secondBackup, "second", StandardCharsets.UTF_8);
    Files.writeString(savedBackup, "saved", StandardCharsets.UTF_8);
    Files.writeString(nonProjectBackup, "text", StandardCharsets.UTF_8);
    ProjectFileUtilities backupUtilities = defaultBackupDirectoryUtilities(defaultBackupDirectory);
    File savedProject = new File(temporaryFolder.getRoot(), "world.a3p");

    backupUtilities.copyDefaultBackupDirectory(savedProject);

    Path namedBackupDirectory = temporaryFolder.getRoot().toPath().resolve("world.bak");
    assertFalse(Files.exists(firstBackup));
    assertFalse(Files.exists(secondBackup));
    assertEquals("first", Files.readString(namedBackupDirectory.resolve(firstBackup.getFileName()), StandardCharsets.UTF_8));
    assertEquals("second", Files.readString(namedBackupDirectory.resolve(secondBackup.getFileName()), StandardCharsets.UTF_8));
    assertEquals("saved", Files.readString(savedBackup, StandardCharsets.UTF_8));
    assertEquals("text", Files.readString(nonProjectBackup, StandardCharsets.UTF_8));
  }

  @Test
  public void copyDefaultBackupDirectoryDoesNothingWhenDefaultBackupDirectoryIsMissing() throws IOException {
    Path defaultBackupDirectory = temporaryFolder.getRoot().toPath().resolve(".defaultbak");
    ProjectFileUtilities backupUtilities = defaultBackupDirectoryUtilities(defaultBackupDirectory);
    File savedProject = new File(temporaryFolder.getRoot(), "world.a3p");

    backupUtilities.copyDefaultBackupDirectory(savedProject);

    assertFalse(Files.exists(defaultBackupDirectory));
    assertFalse(Files.exists(temporaryFolder.getRoot().toPath().resolve("world.bak")));
  }

  @Test
  public void defaultBackupCopyPreservesPathCollision() throws IOException {
    Path defaultBackupDirectory = temporaryFolder.newFolder(".defaultbak")
        .toPath();
    Path defaultBackup = defaultBackupDirectory
        .resolve("auto20240102_120000.a3p");
    Files.writeString(defaultBackup, "backup", StandardCharsets.UTF_8);
    Path collidingBackupPath = temporaryFolder.getRoot().toPath()
        .resolve("world.bak");
    Files.writeString(collidingBackupPath, "collision", StandardCharsets.UTF_8);
    ProjectFileUtilities backupUtilities =
        defaultBackupDirectoryUtilities(defaultBackupDirectory);
    File savedProject = new File(temporaryFolder.getRoot(), "world.a3p");

    IOException thrown = assertThrows(
        IOException.class,
        () -> backupUtilities.copyDefaultBackupDirectory(savedProject));

    assertTrue(
        thrown.getMessage().contains("Unable to create backup directory"));
    assertEquals(
        "backup",
        Files.readString(defaultBackup, StandardCharsets.UTF_8));
    assertEquals(
        "collision",
        Files.readString(collidingBackupPath, StandardCharsets.UTF_8));
  }

  @Test
  public void copyDefaultBackupDirectoryDoesNothingWhenDefaultBackupDirectoryIsEmpty() throws IOException {
    Path defaultBackupDirectory = temporaryFolder.newFolder(".defaultbak").toPath();
    ProjectFileUtilities backupUtilities = defaultBackupDirectoryUtilities(defaultBackupDirectory);
    File savedProject = new File(temporaryFolder.getRoot(), "world.a3p");

    backupUtilities.copyDefaultBackupDirectory(savedProject);

    assertTrue(Files.isDirectory(defaultBackupDirectory));
    assertFalse(Files.exists(temporaryFolder.getRoot().toPath().resolve("world.bak")));
  }

  @Test
  public void exportCopyWritesPlayerArchiveWithThumbnailAndManifest() throws IOException {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    ProjectFileUtilities exportUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getForcedUpToDateProject() {
        return project;
      }

      @Override
      BufferedImage createThumbnail() {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
      }
    };
    File exportFile = temporaryFolder.newFile("exported.a3p");

    exportUtilities.exportCopyOfProjectTo(exportFile);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      assertNotNull(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("thumbnail.png"));
      assertNotNull(zipFile.getEntry("src/Program.twe"));
      String manifest = new String(
          zipFile.getInputStream(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME)).readAllBytes(),
          StandardCharsets.UTF_8);
      assertTrue(manifest, manifest.contains("\"name\":\"Program\""));
      assertTrue(manifest, manifest.contains("\"icon\":\"thumbnail.png\""));
    }
  }

  @Test
  public void exportCopyUsesForcedUpToDateProjectSnapshot() throws IOException {
    Project forcedProject = new Project(programType("ForcedProgram"), Project.SceneCameraType.WindowCamera);
    ProjectFileUtilities exportUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getForcedUpToDateProject() {
        return forcedProject;
      }

      @Override
      Project getUpToDateProject() {
        fail("Export should use the forced up-to-date project snapshot");
        return null;
      }
    };
    File exportFile = temporaryFolder.newFile("forced-export.a3p");

    exportUtilities.exportCopyOfProjectTo(exportFile);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      assertNotNull(zipFile.getEntry("src/ForcedProgram.twe"));
    }
  }

  @Test
  public void exportCopyWritesReferencedImageResourceReadableThroughJsonIo() throws Exception {
    ImageResource imageResource = new ImageResource(
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
        "picture.png",
        "png");
    Project project = new Project(programTypeReferencingImageResource("Program", imageResource), Project.SceneCameraType.WindowCamera);
    project.addResource(imageResource);
    ProjectFileUtilities exportUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getForcedUpToDateProject() {
        return project;
      }
    };
    File exportFile = temporaryFolder.newFile("exported-resource.a3p");

    exportUtilities.exportCopyOfProjectTo(exportFile);

    try (ZipFile zipFile = new ZipFile(exportFile)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      assertNotNull(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("src/Program.twe"));
      assertNotNull(zipFile.getEntry("resources/picture.png"));
      assertArrayEquals(imageResource.getData(), zipFile.getInputStream(zipFile.getEntry("resources/picture.png")).readAllBytes());
      String manifest = new String(
          zipFile.getInputStream(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME)).readAllBytes(),
          StandardCharsets.UTF_8);
      assertTrue(manifest, manifest.contains("\"file\":\"resources/picture.png\""));
    }
    Project readProject = IoUtilities.readProject(exportFile);
    assertNull("Tweedle decoding is still not implemented for player archives", readProject.getProgramType());
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
  public void saveCopyWritesReadableEditorArchiveWithResourceManifestAndThumbnail() throws Exception {
    byte[] data = "hello alice".getBytes(StandardCharsets.UTF_8);
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    TestResource resource = new TestResource("note.txt", "text/plain", data);
    project.addResource(resource);
    ProjectFileUtilities saveUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getUpToDateProject() {
        return project;
      }

      @Override
      BufferedImage createThumbnail() {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
      }
    };
    File saveFile = temporaryFolder.newFile("saved-copy.a3p");

    saveUtilities.saveCopyOfProjectTo(saveFile);

    try (ZipFile zipFile = new ZipFile(saveFile)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      assertNotNull(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("thumbnail.png"));
      assertNotNull(zipFile.getEntry("programType.xml"));
      assertNotNull(zipFile.getEntry("resources.xml"));
      assertNotNull(zipFile.getEntry("resources/note.txt"));
      String manifest = new String(
          zipFile.getInputStream(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME)).readAllBytes(),
          StandardCharsets.UTF_8);
      assertTrue(manifest, manifest.contains("\"name\":\"Program\""));
      assertTrue(manifest, manifest.contains("\"icon\":\"thumbnail.png\""));
    }

    Project readProject = IoUtilities.readProject(saveFile);
    assertEquals("Program", readProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.WindowCamera, readProject.createSaveManifest().projectStructure.sceneCameraType);
    assertEquals(1, readProject.getResources().size());
    Resource readResource = readProject.getResources().iterator().next();
    assertEquals(TestResource.class, readResource.getClass());
    assertEquals(resource.getId(), readResource.getId());
    assertEquals("note.txt", readResource.getOriginalFileName());
    assertEquals("note.txt", readResource.getName());
    assertEquals("text/plain", readResource.getContentType());
    assertArrayEquals(data, readResource.getData());
  }

  @Test
  public void saveCopyReopensAstReferencedImageResource() throws Exception {
    ImageResource imageResource = new ImageResource(
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
        "picture.png",
        "png");
    Project project = new Project(programTypeReferencingImageResource("Program", imageResource), Project.SceneCameraType.WindowCamera);
    project.addResource(imageResource);
    ProjectFileUtilities saveUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getUpToDateProject() {
        return project;
      }
    };
    File saveFile = temporaryFolder.newFile("saved-image-resource.a3p");

    saveUtilities.saveCopyOfProjectTo(saveFile);

    try (ZipFile zipFile = new ZipFile(saveFile)) {
      assertNotNull(zipFile.getEntry("programType.xml"));
      assertNotNull(zipFile.getEntry("resources.xml"));
      assertNotNull(zipFile.getEntry("resources/picture.png"));
      assertArrayEquals(imageResource.getData(), zipFile.getInputStream(zipFile.getEntry("resources/picture.png")).readAllBytes());
    }
    Project readProject = IoUtilities.readProject(saveFile);
    assertEquals("Program", readProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.WindowCamera, readProject.createSaveManifest().projectStructure.sceneCameraType);
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
  public void saveCopyUsesUpToDateProjectSnapshot() throws Exception {
    Project project = new Project(programType("SavedProgram"), Project.SceneCameraType.WindowCamera);
    ProjectFileUtilities saveUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getUpToDateProject() {
        return project;
      }

      @Override
      Project getForcedUpToDateProject() {
        fail("Save copy should use the normal up-to-date project snapshot");
        return null;
      }
    };
    File saveFile = temporaryFolder.newFile("snapshot-save.a3p");

    saveUtilities.saveCopyOfProjectTo(saveFile);

    Project readProject = IoUtilities.readProject(saveFile);
    assertEquals("SavedProgram", readProject.getProgramType().getName());
  }

  @Test
  public void saveProjectToNormalProjectWritesReadableSaveBackupBesideTarget() throws Exception {
    Project project = new Project(programType("SavedProgram"), Project.SceneCameraType.WindowCamera);
    File saveFile = new File(temporaryFolder.getRoot(), "world.a3p");
    ProjectFileUtilities saveUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getUpToDateProject() {
        return project;
      }

      @Override
      File savedProjectFile() {
        return saveFile;
      }
    };

    saveUtilities.saveProjectTo(saveFile, false);

    assertEquals("SavedProgram", IoUtilities.readProject(saveFile).getProgramType().getName());
    File[] backups = new File(temporaryFolder.getRoot(), "world.bak")
        .listFiles(file -> file.isFile() && file.getName().startsWith("save") && file.getName().endsWith(".a3p"));
    assertNotNull(backups);
    assertEquals(1, backups.length);
    assertEquals("SavedProgram", IoUtilities.readProject(backups[0]).getProgramType().getName());
  }

  @Test
  public void saveProjectToBackupTargetDoesNotCreateSaveBackupDirectory() throws Exception {
    Project project = new Project(programType("BackupProgram"), Project.SceneCameraType.WindowCamera);
    File backupDirectory = temporaryFolder.newFolder("world.bak");
    File backupFile = new File(backupDirectory, "auto20240102_120000.a3p");
    ProjectFileUtilities saveUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getUpToDateProject() {
        return project;
      }

      @Override
      File savedProjectFile() {
        return backupFile;
      }
    };

    saveUtilities.saveProjectTo(backupFile, true);

    assertEquals("BackupProgram", IoUtilities.readProject(backupFile).getProgramType().getName());
    File[] saveBackups = backupDirectory.listFiles(file -> file.isFile() && file.getName().startsWith("save"));
    assertNotNull(saveBackups);
    assertEquals(0, saveBackups.length);
  }

  @Test
  public void saveCopyPropagatesTargetWriteFailure() throws Exception {
    Project project = new Project(programType("SavedProgram"), Project.SceneCameraType.WindowCamera);
    ProjectFileUtilities saveUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getUpToDateProject() {
        return project;
      }
    };
    File targetDirectory = temporaryFolder.newFolder("save-copy-target-directory");

    assertThrows(IOException.class, () -> saveUtilities.saveCopyOfProjectTo(targetDirectory));
  }

  @Test
  public void saveProjectToPropagatesSaveCopyFailure() throws Exception {
    Project project = new Project(programType("SavedProgram"), Project.SceneCameraType.WindowCamera);
    ProjectFileUtilities saveUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getUpToDateProject() {
        return project;
      }
    };
    File targetDirectory = temporaryFolder.newFolder("save-project-target-directory");

    assertThrows(IOException.class, () -> saveUtilities.saveProjectTo(targetDirectory, true));
  }

  @Test
  public void exportCopyPropagatesTargetWriteFailure() throws Exception {
    Project project = new Project(programType("ExportedProgram"), Project.SceneCameraType.WindowCamera);
    ProjectFileUtilities exportUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getForcedUpToDateProject() {
        return project;
      }
    };
    File targetDirectory = temporaryFolder.newFolder("export-copy-target-directory");

    assertThrows(IOException.class, () -> exportUtilities.exportCopyOfProjectTo(targetDirectory));
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static ProjectFileUtilities defaultBackupDirectoryUtilities(Path defaultBackupDirectory) {
    return new ProjectFileUtilities(null) {
      @Override
      Path defaultBackupDirectory(boolean createIfMissing) {
        assertFalse(createIfMissing);
        return defaultBackupDirectory;
      }
    };
  }

  private static NamedUserType programTypeReferencingImageResource(String name, ImageResource imageResource) {
    NamedUserType type = programType(name);
    UserLocal image = new UserLocal("image", ImageResource.class, true);
    UserMethod userMethod = new UserMethod(
        "rememberImage",
        Void.TYPE,
        new org.lgna.project.ast.UserParameter[0],
        new BlockStatement(new LocalDeclarationStatement(image, new ResourceExpression(ImageResource.class, imageResource))));
    type.methods.add(userMethod);
    return type;
  }

  public static class TestResource extends Resource {
    public TestResource(String fileName, String contentType, byte[] data) {
      super(fileName, contentType, data);
    }

    private TestResource(UUID uuid) {
      super(uuid);
    }

    public static TestResource valueOf(String uuidText) {
      return new TestResource(UUID.fromString(uuidText));
    }
  }
}
