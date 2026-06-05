package org.alice.netbeans.project;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class JavaFxDisplayPrerequisiteContractTest {
  @Test
  public void xvfbJavaFxLauncherTestUsesAssumptionsBeforeDisplayBoundWork() throws Exception {
    String source = Files.readString(Path.of(
        System.getProperty("user.dir"),
        "src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStandaloneProjectTest.java"));
    String method = methodBody(source, "templatePackagedLauncherWithRealJavaFxModulesRunsOnXvfbDisplay");

    int firstAssumption = method.indexOf("org.junit.Assume.assumeTrue");
    int projectExtraction = method.indexOf("extractProjectTemplate");
    int forkedLaunch = method.indexOf("runJarWithJavaFxModulesUnderXvfb");

    assertTrue(
        "The real JavaFX/Xvfb test must use JUnit 4 assumptions for missing display/runtime prerequisites",
        firstAssumption >= 0);
    assertTrue(
        "Prerequisite assumptions must run before project extraction and JavaFX launch work",
        firstAssumption < projectExtraction && firstAssumption < forkedLaunch);
    assertTrue(
        "The test must gate on xvfb-run availability",
        method.contains("findExecutableOnPath(\"xvfb-run\")"));
    assertTrue(
        "The test must gate on JavaFX runtime modules",
        method.contains("assumeJavaFxRuntimeModulePathForDisplayTest()"));
    assertTrue(
        "The test must prove xvfb-run can start Java before running the display-bound launcher",
        method.contains("xvfbRunStartsJava"));
    assertTrue(
        "The test must prove the JavaFX display runtime initializes under Xvfb before running the launcher",
        method.contains("javaFxDisplayRuntimeStartsUnderXvfb"));
    assertTrue(
        "Expensive JavaFX/Xvfb prerequisite probes must be cached for the test class",
        source.contains("javaFxRuntimeModulePathCache")
            && source.contains("XVFB_RUN_STARTS_JAVA_CACHE")
            && source.contains("JAVAFX_XVFB_DISPLAY_CACHE"));
  }

  private static String methodBody(String source, String methodName) {
    int methodNameIndex = source.indexOf(methodName + "()");
    if (methodNameIndex < 0) {
      throw new AssertionError("Missing test method " + methodName);
    }
    int openBrace = source.indexOf('{', methodNameIndex);
    int depth = 0;
    for (int i = openBrace; i < source.length(); i++) {
      char ch = source.charAt(i);
      if (ch == '{') {
        depth++;
      } else if (ch == '}') {
        depth--;
        if (depth == 0) {
          return source.substring(openBrace, i + 1);
        }
      }
    }
    throw new AssertionError("Could not parse body for " + methodName);
  }
}
