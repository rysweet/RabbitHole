package org.alice.netbeans.project;

import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertTrue;

public class Alice3ProjectTemplateAntJavadocTest {
  private static final Path TARGET = Path.of("target");
  private static final Path TEMPLATE_ARCHIVE = Path.of("target/classes/org/alice/netbeans/ProjectTemplate.zip");

  @Test
  public void exportedProjectJavadocTargetWritesDocumentationForGeneratedSources() throws Exception {
    Path smokeRoot = TARGET.resolve("ant-javadoc-smoke");
    Path projectDirectory = smokeRoot.resolve("project");
    deleteRecursively(smokeRoot);
    unzip(TEMPLATE_ARCHIVE, projectDirectory);

    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);
    Path aliceProject = smokeRoot.resolve("javadoc-world.a3p");
    IoUtilities.writeProject(
        aliceProject.toFile(),
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    ProjectCodeGenerator.generateCode(
        aliceProject.toAbsolutePath().normalize().toFile(),
        sourceDirectory.toAbsolutePath().normalize().toFile(),
        null,
        false);
    writeDocumentedProbe(sourceDirectory);

    Path antScratch = smokeRoot.resolve("ant-scratch");
    Files.createDirectories(antScratch);
    Path userProperties = smokeRoot.resolve("user.properties");
    Alice3ProjectTemplateAntSmokeTest.writeLibraryProperties(userProperties, antScratch);

    String antLog = Alice3ProjectTemplateAntSmokeTest.executeAntTarget(
        projectDirectory,
        userProperties,
        antScratch,
        "javadoc",
        "ant-javadoc.log",
        Map.of("javadoc.additionalparam", "-Xdoclint:none"));

    Path javadocDirectory = projectDirectory.resolve("dist/javadoc");
    assertTrue(antLog, Files.exists(javadocDirectory.resolve("index.html")));
    assertTrue(antLog, Files.exists(javadocDirectory.resolve("AliceJavaFXLauncher.html")));
    assertTrue(antLog, Files.exists(javadocDirectory.resolve("JavadocProbe.html")));
    assertTrue(antLog, !antLog.contains("BUILD FAILED"));
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    type.methods.add(mainMethod());
    return type;
  }

  private static UserMethod mainMethod() {
    UserParameter argsParameter = new UserParameter("args", String[].class);
    UserMethod mainMethod = new UserMethod(
        "main",
        Void.TYPE,
        new UserParameter[] {argsParameter},
        new BlockStatement());
    mainMethod.isStatic.setValue(true);
    mainMethod.isSignatureLocked.setValue(true);
    return mainMethod;
  }

  private static void writeDocumentedProbe(Path sourceDirectory) throws Exception {
    Files.writeString(
        sourceDirectory.resolve("JavadocProbe.java"),
        """
        /**
         * Probe class that proves the exported project javadoc target documents sources added
         * alongside the generated Alice launcher and program.
         */
        public class JavadocProbe {
        }
        """,
        StandardCharsets.UTF_8);
  }

  private static void unzip(Path archive, Path destination) throws Exception {
    Files.createDirectories(destination);
    try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
      ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        Path output = destination.resolve(entry.getName()).normalize();
        assertTrue("Zip entry escapes destination: " + entry.getName(), output.startsWith(destination));
        if (entry.isDirectory()) {
          Files.createDirectories(output);
        } else {
          Files.createDirectories(output.getParent());
          Files.copy(input, output);
        }
      }
    }
  }

  private static void deleteRecursively(Path path) throws Exception {
    if (!Files.exists(path)) {
      return;
    }
    try (var paths = Files.walk(path)) {
      for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(child);
      }
    }
  }
}
