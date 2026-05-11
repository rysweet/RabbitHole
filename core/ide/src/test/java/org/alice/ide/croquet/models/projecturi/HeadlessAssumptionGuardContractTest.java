package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Contract tests for headless assumption guard behavior (issue #497).
 *
 * <p>{@link StageIdeSaveMenuDoClickToWriteProofTest#saveMenuDoClickApprovesChooserAndWritesProjectFile()}
 * must use {@code Assume.assumeTrue()} for headless skipping so JUnit
 * reports "skipped" (not "passed") in headless CI environments.
 *
 * <p>The key test {@link #saveMenuDoClickSkipsInHeadlessViaAssumption}
 * fails initially because the current code uses {@code if/return} (silent
 * pass). After implementation replaces it with {@code assumeTrue}, the
 * method throws {@code AssumptionViolatedException} and the test passes.
 */
public class HeadlessAssumptionGuardContractTest {

  /**
   * Sanity check: confirms the CI environment is headless.
   * Skips on headed displays (e.g., local Mac/Linux with X11).
   */
  @Test
  public void headlessEnvironmentDetected() {
    assumeTrue("Only runs in headless CI", GraphicsEnvironment.isHeadless());
    assertTrue("Test is running in headless mode", GraphicsEnvironment.isHeadless());
  }

  /**
   * The main contract test for issue #497.
   *
   * <p>Calls {@code saveMenuDoClickApprovesChooserAndWritesProjectFile()} in a
   * headless environment and expects {@link org.junit.AssumptionViolatedException}
   * (JUnit skip). The current code uses {@code if(!available){...return;}} which
   * returns normally — this test detects that as a failure.
   *
   * <p>After implementation adds {@code assumeTrue(...)}, the method throws
   * the expected exception and this test passes.
   */
  @Test
  public void saveMenuDoClickSkipsInHeadlessViaAssumption() throws Exception {
    assumeTrue("Only meaningful in headless", GraphicsEnvironment.isHeadless());

    StageIdeSaveMenuDoClickToWriteProofTest testInstance =
        new StageIdeSaveMenuDoClickToWriteProofTest();
    testInstance.captureProperties();
    try {
      testInstance.saveMenuDoClickApprovesChooserAndWritesProjectFile();
      fail("Expected AssumptionViolatedException (JUnit skip) but method returned "
          + "normally. The headless guard must use assumeTrue() instead of if/return.");
    } catch (org.junit.internal.AssumptionViolatedException expected) {
      // Correct: test properly skips via Assume.assumeTrue()
    } finally {
      testInstance.restorePropertiesAndResetApplication();
    }
  }
}
