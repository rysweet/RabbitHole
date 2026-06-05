package org.alice.ide.build;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class BuildRuntimeConfigurationTest {
  @Test
  public void rootPomDefaultsToHeadlessAndDevProfileOptInDisablesHeadless() throws Exception {
    String pom = Files.readString(repositoryRoot().resolve("pom.xml"));

    assertTrue(
        "Root pom.xml must keep the default Maven runtime CI-compatible with java.awt.headless=true",
        pom.contains("<java.awt.headless>true</java.awt.headless>"));
    assertTrue(
        "Root pom.xml must define an opt-in dev profile for local GUI development",
        pom.contains("<id>dev</id>"));
    assertTrue(
        "The dev profile must explicitly set java.awt.headless=false",
        pom.contains("<java.awt.headless>false</java.awt.headless>"));
  }

  @Test
  public void ciWorkflowInstallsXvfbAndRunsValidationUnderVirtualDisplay() throws Exception {
    String workflow = Files.readString(repositoryRoot().resolve(".github/workflows/alice-test-ci.yml"));

    assertTrue(
        "alice-test-ci.yml must install Xvfb before Maven validation on Linux runners",
        workflow.contains("xvfb"));
    assertTrue(
        "alice-test-ci.yml must wrap test execution with xvfb-run so display-bound tests have a virtual display",
        workflow.contains("xvfb-run"));
    assertTrue(
        "alice-test-ci.yml must continue to run the existing headless validation path",
        workflow.contains("--headless"));
  }

  private static Path repositoryRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isDirectory(current.resolve(".github")) && Files.isRegularFile(current.resolve("pom.xml"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Could not locate repository root from " + System.getProperty("user.dir"));
  }
}
