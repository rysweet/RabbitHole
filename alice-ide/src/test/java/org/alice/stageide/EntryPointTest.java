package org.alice.stageide;

import org.junit.BeforeClass;
import org.junit.Test;
import org.lgna.project.reflect.ClassInfoManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EntryPointTest {
  @BeforeClass
  public static void forceHeadlessMode() {
    System.setProperty("java.awt.headless", "true");
  }

  @Test
  public void mainFailsFastBeforeStartingDesktopUiInHeadlessMode() {
    try {
      EntryPoint.main(new String[0]);
      fail("Headless launch should fail before Swing or JavaFX startup");
    } catch (IllegalStateException ise) {
      assertEquals("Alice desktop launch requires a graphical environment.", ise.getMessage());
    }
  }

  @Test
  public void loadClassInfosRegistersKnownClassInfoEntries() throws Exception {
    Method method = EntryPoint.class.getDeclaredMethod("loadClassInfos");
    method.setAccessible(true);

    method.invoke(null);

    assertNotNull(ClassInfoManager.getInstance("org.lgna.story.SQuadruped"));
    assertNotNull(ClassInfoManager.getInstance("org.lgna.story.StraightenOutJoints"));
  }

  @Test
  public void startAllowsNullPrimaryStageBecauseItIsCurrentlyANoOp() throws Exception {
    new EntryPoint().start(null);
  }

  @Test
  public void mainInstallsProcessTerminatorBoundaryAroundDesktopLaunch() throws IOException {
    String source = Files.readString(
        findRepositoryRoot().resolve("alice-ide/src/main/java/org/alice/stageide/EntryPoint.java"),
        StandardCharsets.UTF_8);

    assertTrue("EntryPoint must install the production process termination handler",
        source.contains("ProcessTerminator.setHandler"));
    assertTrue("EntryPoint must handle fallback termination requests at the launcher boundary",
        source.contains("ProcessTerminationRequestedException"));
    assertTrue("EntryPoint must preserve the requested exit status",
        source.contains("System.exit(request.getStatus())"));
    assertTrue("EntryPoint must restore the previous process termination handler",
        source.contains("ProcessTerminator.setHandler(previous"));
  }

  @Test
  public void lookAndFeelFallbackUsesStructuredLoggingInsteadOfDirectStackTracePrint() throws IOException {
    String source = Files.readString(
        findRepositoryRoot().resolve("alice-ide/src/main/java/org/alice/stageide/EntryPoint.java"),
        StandardCharsets.UTF_8);

    assertTrue("EntryPoint look-and-feel fallback should log the throwable with context",
        source.contains("Logger.throwable("));
    assertFalse("EntryPoint should not print startup fallback stack traces directly",
        source.contains("updateFlatLafThemeException.printStackTrace()"));
  }

  private static Path findRepositoryRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      if (Files.exists(current.resolve(".git")) && Files.isRegularFile(current.resolve("pom.xml"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new AssertionError("Could not find repository root from " + System.getProperty("user.dir"));
  }
}
