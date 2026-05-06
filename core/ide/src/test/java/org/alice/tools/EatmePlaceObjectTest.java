package org.alice.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;
import org.lgna.story.SScene;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class EatmePlaceObjectTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void placesBunnyInSceneAndWritesEatmeProofArtifacts() throws Exception {
    File starterProject = temporaryFolder.newFile("starter.a3p");
    IoUtilities.writeProject(starterProject, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmePlaceObject.run(
        new String[] {
            "--project", starterProject.getAbsolutePath(),
            "--object", "alice-gallery://animals/bunny",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains("\"schema_version\":\"eatme.alice-object-placement-result/v1\""));
    assertTrue(result, result.contains("\"status\":\"placed\""));
    assertTrue(result, result.contains("\"placement_artifact\":\"placement.json\""));
    assertTrue(result, result.contains("\"scene_or_project_diff\":\"scene.diff.json\""));
    assertTrue(Files.size(evidenceDir.resolve("placement.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("scene.diff.json")) > 0);

    Project placedProject = IoUtilities.readProject(evidenceDir.resolve("placed-project.a3p").toFile());
    NamedUserType sceneType = (NamedUserType) placedProject.getProgramType()
        .getDeclaredFields()
        .get(0)
        .getValueType();
    UserField bunny = sceneType.getDeclaredFields().stream()
        .filter(field -> "bunny".equals(field.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull("placed project should contain the bunny field", bunny);
    assertEquals("SBiped", bunny.getValueType().getName());
  }

  @Test
  public void escapesJsonControlCharacters() {
    assertEquals(
        "quote\\\" slash\\\\ backspace\\b formfeed\\f newline\\n return\\r tab\\t low\\u0001",
        EatmePlaceObject.escapeJson("quote\" slash\\ backspace\b formfeed\f newline\n return\r tab\t low\u0001"));
  }

  @Test
  public void writesOnlyResultJsonToStdoutForMigratedStarterProject() throws Exception {
    File starterProject = new File("../resources/src/application/resources/starter-projects/magicMinimum.a3p");
    assumeTrue("starter project fixture is not available in this checkout", starterProject.isFile());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;

    try (PrintStream processStdout = new PrintStream(stdout)) {
      System.setOut(processStdout);
      int status = EatmePlaceObject.run(
          new String[] {
              "--project", starterProject.getAbsolutePath(),
              "--object", "alice-gallery://animals/bunny",
              "--evidence-dir", evidenceDir.toString(),
              "--json"
          },
          System.out,
          new PrintStream(stderr));

      assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    } finally {
      System.setOut(originalOut);
    }

    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.startsWith("{\"schema_version\":\"eatme.alice-object-placement-result/v1\""));
    assertTrue(result, result.endsWith("}\n"));
    assertTrue(Files.size(evidenceDir.resolve("placement.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("scene.diff.json")) > 0);
  }

  @Test
  public void rejectsUnsupportedObjectIdentifierWithoutProofArtifacts() throws Exception {
    File starterProject = temporaryFolder.newFile("starter.a3p");
    IoUtilities.writeProject(starterProject, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmePlaceObject.run(
        new String[] {
            "--project", starterProject.getAbsolutePath(),
            "--object", "alice-gallery://animals/dragon",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("unsupported object identifier"));
    assertTrue(Files.notExists(evidenceDir.resolve("placement.json")));
  }

  private static Project projectWithScene() {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }
}
