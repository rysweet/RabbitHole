package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.awt.FileDialogUtilities;
import org.junit.After;
import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;
import org.lgna.story.SScene;

import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class SaveOperationFlowFileWriteProofTest {
  private String previousEvidenceDir;
  private String previousDiscoveryEvidenceDir;
  private String previousSelectedPath;

  @After
  public void restoreProperties() {
    restoreProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
    restoreProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, previousDiscoveryEvidenceDir);
    restoreProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, previousSelectedPath);
  }

  @Test
  public void selectedPathSaveFlowWritesProjectFileThroughFileDialogUtilitiesSeam() throws Exception {
    assumeFalse("requires Xvfb or another headful AWT display", GraphicsEnvironment.isHeadless());
    Path testDir = newTestDir().resolve("save-flow-file-write");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path projectsDir = Files.createDirectories(testDir.resolve("projects"));
    File selectedProjectFile = projectsDir.resolve("controlled-save-flow.a3p").toFile();
    configureEvidence(evidenceDir, selectedProjectFile.toPath());
    JFrame owner = displayableOwner("save-flow-file-write-owner");
    try {
      SaveOperationFlow.Result result = SaveOperationFlow.run(
          new FileDialogContext(owner, projectsDir.toFile()),
          file -> true,
          IoUtilities.PROJECT_EXTENSION,
          file -> IoUtilities.writeProject(file, projectWithSceneMethod("saveFlowWriteProof")));
      SaveOperationCompletionEvidence.write(
          evidenceDir,
          "org.alice.ide.croquet.models.projecturi.SaveAsProjectOperation",
          IoUtilities.PROJECT_EXTENSION,
          result);

      assertTrue(result.finished());
      assertFalse(result.canceled());
      assertEquals(1, result.promptCount());
      assertEquals(1, result.saveAttempts());
      assertEquals(selectedProjectFile.getAbsoluteFile(), result.savedFile().getAbsoluteFile());
      assertTrue("Save flow should create the controlled selected .a3p file", selectedProjectFile.isFile());
      long savedSize = Files.size(selectedProjectFile.toPath());
      assertTrue("saved project should be non-empty", savedSize > 0);
      Project savedProject = IoUtilities.readProject(selectedProjectFile);
      assertNotNull(savedProject);
      assertEquals("Program", savedProject.getProgramType().getName());

      String resultJson = Files.readString(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT));
      assertTrue(resultJson, resultJson.contains("\"status\": \"finished\""));
      assertTrue(resultJson, resultJson.contains("\"prompt_count\": 1"));
      assertTrue(resultJson, resultJson.contains("\"save_attempts\": 1"));
      assertTrue(resultJson, resultJson.contains("\"saved_file_exists\": true"));
      assertTrue(resultJson, resultJson.contains("\"saved_file_size_bytes\": " + savedSize));

      String discoveryJson = Files.readString(evidenceDir.resolve(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_TARGET_ARTIFACT));
      assertTrue(discoveryJson, discoveryJson.contains("\"status\": \"target_resolved\""));
      assertTrue(discoveryJson, discoveryJson.contains("\"status\": \"selected_path_injected\""));
      assertTrue(discoveryJson, discoveryJson.contains("\"selected_file\": \"" + FileDialogUtilities.escapeJson(selectedProjectFile.getAbsolutePath()) + "\""));
      assertTrue(discoveryJson, discoveryJson.contains("\"Save dialog displayed\""));
    } finally {
      owner.dispose();
    }
  }

  @Test
  public void selectedPathOutsideRequestedDirectoryCancelsSaveFlowWithoutWritingFile() throws Exception {
    assumeFalse("requires Xvfb or another headful AWT display", GraphicsEnvironment.isHeadless());
    Path testDir = newTestDir().resolve("save-flow-outside-directory");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path projectsDir = Files.createDirectories(testDir.resolve("projects"));
    Path outsideDir = Files.createDirectories(testDir.resolve("outside"));
    File outsideSelectedFile = outsideDir.resolve("outside-save-flow.a3p").toFile();
    configureEvidence(evidenceDir, outsideSelectedFile.toPath());
    JFrame owner = displayableOwner("save-flow-outside-owner");
    try {
      SaveOperationFlow.Result result = SaveOperationFlow.run(
          new FileDialogContext(owner, projectsDir.toFile()),
          file -> true,
          IoUtilities.PROJECT_EXTENSION,
          file -> IoUtilities.writeProject(file, projectWithSceneMethod("shouldNotWrite")));
      SaveOperationCompletionEvidence.write(
          evidenceDir,
          "org.alice.ide.croquet.models.projecturi.SaveAsProjectOperation",
          IoUtilities.PROJECT_EXTENSION,
          result);

      assertFalse(result.finished());
      assertTrue(result.canceled());
      assertEquals(1, result.promptCount());
      assertEquals(0, result.saveAttempts());
      assertFalse("unsupported selected path must not create a project file", outsideSelectedFile.exists());
      String resultJson = Files.readString(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT));
      assertTrue(resultJson, resultJson.contains("\"status\": \"canceled\""));
      assertTrue(resultJson, resultJson.contains("\"saved_file\": null"));
      assertTrue(resultJson, resultJson.contains("\"saved_file_exists\": null"));

      String discoveryJson = Files.readString(evidenceDir.resolve(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_TARGET_ARTIFACT));
      assertTrue(discoveryJson, discoveryJson.contains("\"status\": \"unsupported\""));
      assertTrue(discoveryJson, discoveryJson.contains("\"reason\": \"selected_path_outside_requested_directory\""));
      assertTrue(discoveryJson, discoveryJson.contains("Configured selected Save path must stay under the requested directory."));
    } finally {
      owner.dispose();
    }
  }

  private void configureEvidence(Path evidenceDir, Path selectedPath) {
    previousEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    previousDiscoveryEvidenceDir = System.getProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    previousSelectedPath = System.getProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, selectedPath.toAbsolutePath().toString());
  }

  private static JFrame displayableOwner(String title) {
    JFrame owner = new JFrame(title);
    owner.pack();
    assertTrue(owner.isDisplayable());
    return owner;
  }

  private static void restoreProperty(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }

  private static Project projectWithSceneMethod(String methodName) {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    sceneType.methods.add(new UserMethod(methodName, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement(new Comment("save flow file write proof"))));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(Path.of(
        "target",
        "save-operation-flow-file-write-proof-test",
        UUID.randomUUID().toString()));
  }

  private record FileDialogContext(JFrame owner, File defaultDirectory) implements SaveOperationFlow.Context {
    @Override
    public File getCurrentFile() {
      return null;
    }

    @Override
    public boolean isBackup() {
      return false;
    }

    @Override
    public File getMainProjectFile() {
      return null;
    }

    @Override
    public File getDefaultDirectory() {
      return defaultDirectory;
    }

    @Override
    public File showSaveFileDialog(File directory, String filename, String extension) {
      return FileDialogUtilities.showSaveFileDialog(owner, directory, filename, extension);
    }

    @Override
    public void showWaitCursor() {
    }

    @Override
    public void hideWaitCursor() {
    }

    @Override
    public void showError(String title, String message) {
      throw new AssertionError(title + ": " + message);
    }

    @Override
    public void finish() {
    }

    @Override
    public void cancel() {
    }
  }
}
