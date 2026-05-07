package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.pattern.Tuple2;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModelResourceArrayUtilitiesTest {
  @Test
  public void arrayIndexParsesTrailingJointIndex() {
    assertEquals(0, ModelResourceArrayUtilities.getArrayIndexForJoint("WHEEL_000"));
    assertEquals(12, ModelResourceArrayUtilities.getArrayIndexForJoint("WHEEL_12"));
    assertEquals(-1, ModelResourceArrayUtilities.getArrayIndexForJoint("WHEEL"));
  }

  @Test
  public void arrayNameUsesCustomMappingAndSkipList() {
    Map<String, String> customNames = new HashMap<String, String>();
    customNames.put("FRONT_WHEEL", "WHEEL");

    assertEquals("WHEEL", ModelResourceArrayUtilities.getArrayNameForJoint("FRONT_WHEEL_01", customNames, null));
    assertNull(ModelResourceArrayUtilities.getArrayNameForJoint("FRONT_WHEEL_01", null, new String[] {"FRONT_WHEEL"}));
  }

  @Test
  public void arrayEntriesGroupSortAndSuppressJointNames() throws Exception {
    List<String> joints = Arrays.asList("WHEEL_02", "AXLE_00", "WHEEL_00", "WHEEL_01");

    Map<String, List<String>> entries = ModelResourceArrayUtilities.getArrayEntries(joints, null, Collections.singletonList("AXLE_00"), null);

    assertEquals(Collections.singleton("WHEEL"), entries.keySet());
    assertEquals(Arrays.asList("WHEEL_00", "WHEEL_01", "WHEEL_02"), entries.get("WHEEL"));
  }

  @Test
  public void arrayEntriesAcceptJointPairs() throws Exception {
    List<Tuple2<String, String>> joints = Arrays.asList(
        Tuple2.createInstance("FINGER_01", "HAND"),
        Tuple2.createInstance("FINGER_00", "HAND"));

    assertTrue(ModelResourceArrayUtilities.hasArray("FINGER", joints));
    assertFalse(ModelResourceArrayUtilities.hasArray("TOE", joints));
    assertEquals(Arrays.asList("FINGER_00", "FINGER_01"),
        ModelResourceArrayUtilities.getArrayEntriesFromJointList(joints, null, null, null).get("FINGER"));
  }

  @Test
  public void arrayEntriesGroupAcrossCustomNamesBeforeSorting() throws Exception {
    Map<String, String> customNames = new HashMap<String, String>();
    customNames.put("LEFT_WHEEL", "WHEEL");
    customNames.put("RIGHT_WHEEL", "WHEEL");

    Map<String, List<String>> entries = ModelResourceArrayUtilities.getArrayEntries(
        Arrays.asList("LEFT_WHEEL_01", "RIGHT_WHEEL_00"), customNames, null, null);

    assertEquals(Collections.singleton("WHEEL"), entries.keySet());
    assertEquals(Arrays.asList("RIGHT_WHEEL_00", "LEFT_WHEEL_01"), entries.get("WHEEL"));
  }

  @Test
  public void arrayNameSkipListAppliesAfterCustomMappingAndIgnoresCase() {
    Map<String, String> customNames = new HashMap<String, String>();
    customNames.put("FRONT_WHEEL", "Wheel");

    assertNull(ModelResourceArrayUtilities.getArrayNameForJoint(
        "FRONT_WHEEL_01", customNames, new String[] {"wheel"}));
  }

  @Test
  public void arrayEntriesRejectDuplicateResolvedIndices() {
    PrintStream originalErr = System.err;
    ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
    System.setErr(new PrintStream(capturedErr));
    try {
      ModelResourceArrayUtilities.getArrayEntries(
          Arrays.asList("WHEEL_0", "WHEEL_00"), null, null, null);
      fail("Expected duplicate resolved array indices to be rejected");
    } catch (DataFormatException exception) {
      assertTrue(exception.getMessage().contains("ERROR COMPARING ARRAY NAME INDICES"));
      assertTrue(exception.getMessage().contains("WHEEL_0"));
      assertTrue(exception.getMessage().contains("WHEEL_00"));
      assertTrue(capturedErr.toString().contains("ERROR COMPARING ARRAY NAME INDICES"));
    } finally {
      System.setErr(originalErr);
    }
  }
}
