package org.alice.tools;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Contract tests for the extractIntField helper added in issue #496.
 *
 * <p>The helper extracts an integer value from a JSON-like evidence string
 * using regex, enabling platform-tolerant assertions on renderTargetWidth
 * and renderTargetHeight (which may be 0 on Linux or 24 on Mac).
 *
 * <p>All tests fail initially because extractIntField does not yet exist
 * on {@link EatmeDesktopRunExecutionEvidenceTest}. Implementation adds
 * the private static method; these tests verify it via reflection.
 */
public class ExtractIntFieldContractTest {

  @Test
  public void extractIntFieldMethodExists() {
    try {
      Method method = EatmeDesktopRunExecutionEvidenceTest.class
          .getDeclaredMethod("extractIntField", String.class, String.class);
      assertNotNull("extractIntField should be declared", method);
    } catch (NoSuchMethodException e) {
      fail("extractIntField(String, String) must be added to EatmeDesktopRunExecutionEvidenceTest");
    }
  }

  @Test
  public void extractsZeroValue() {
    assertEquals(0, invokeExtractIntField(
        "{ \"renderTargetWidth\": 0, \"renderTargetHeight\": 0 }",
        "renderTargetWidth"));
  }

  @Test
  public void extractsPositiveMacDimension() {
    assertEquals(24, invokeExtractIntField(
        "{ \"renderTargetWidth\": 24, \"renderTargetHeight\": 18 }",
        "renderTargetWidth"));
  }

  @Test
  public void extractsMultiDigitValue() {
    assertEquals(1920, invokeExtractIntField(
        "{ \"renderTargetWidth\": 1920 }",
        "renderTargetWidth"));
  }

  @Test
  public void returnsNegativeOneForMissingField() {
    assertEquals(-1, invokeExtractIntField(
        "{ \"otherField\": 42 }",
        "renderTargetWidth"));
  }

  @Test
  public void extractsHeightField() {
    assertEquals(18, invokeExtractIntField(
        "{ \"renderTargetWidth\": 24, \"renderTargetHeight\": 18 }",
        "renderTargetHeight"));
  }

  @Test
  public void handlesNegativeDimension() {
    assertEquals(-1, invokeExtractIntField(
        "{ \"renderTargetWidth\": -1 }",
        "renderTargetWidth"));
  }

  private static int invokeExtractIntField(String json, String fieldName) {
    try {
      Method method = EatmeDesktopRunExecutionEvidenceTest.class
          .getDeclaredMethod("extractIntField", String.class, String.class);
      method.setAccessible(true);
      return (int) method.invoke(null, json, fieldName);
    } catch (NoSuchMethodException e) {
      fail("extractIntField(String, String) must be added to "
          + "EatmeDesktopRunExecutionEvidenceTest");
      return -999;
    } catch (Exception e) {
      fail("extractIntField invocation failed: " + e);
      return -999;
    }
  }
}
