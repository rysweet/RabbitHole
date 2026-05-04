package org.alice.stageide;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EntryPointHeadlessGuardTest {
  @Test
  public void desktopLaunchRejectsHeadlessEnvironmentBeforeUiStartup() {
    try {
      EntryPoint.requireGraphicalEnvironmentForDesktopLaunch(true);
      fail("Headless desktop launch should fail before Swing or JavaFX startup");
    } catch (IllegalStateException ise) {
      assertEquals("Alice desktop launch requires a graphical environment.", ise.getMessage());
    }
  }

  @Test
  public void desktopLaunchAllowsGraphicalEnvironment() {
    EntryPoint.requireGraphicalEnvironmentForDesktopLaunch(false);
  }
}
