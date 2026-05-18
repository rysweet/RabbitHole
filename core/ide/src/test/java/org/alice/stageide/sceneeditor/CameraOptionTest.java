package org.alice.stageide.sceneeditor;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link CameraOption} enum.
 */
public class CameraOptionTest {

  @Test
  public void values_hasFiveConstants() {
    assertEquals(5, CameraOption.values().length);
  }

  @Test
  public void valueOf_startingCameraView() {
    assertEquals(CameraOption.STARTING_CAMERA_VIEW,
        CameraOption.valueOf("STARTING_CAMERA_VIEW"));
  }

  @Test
  public void valueOf_layoutSceneView() {
    assertEquals(CameraOption.LAYOUT_SCENE_VIEW,
        CameraOption.valueOf("LAYOUT_SCENE_VIEW"));
  }

  @Test
  public void valueOf_top() {
    assertEquals(CameraOption.TOP, CameraOption.valueOf("TOP"));
  }

  @Test
  public void valueOf_side() {
    assertEquals(CameraOption.SIDE, CameraOption.valueOf("SIDE"));
  }

  @Test
  public void valueOf_front() {
    assertEquals(CameraOption.FRONT, CameraOption.valueOf("FRONT"));
  }

  @Test
  public void ordinals_sequential() {
    assertEquals(0, CameraOption.STARTING_CAMERA_VIEW.ordinal());
    assertEquals(1, CameraOption.LAYOUT_SCENE_VIEW.ordinal());
    assertEquals(2, CameraOption.TOP.ordinal());
    assertEquals(3, CameraOption.SIDE.ordinal());
    assertEquals(4, CameraOption.FRONT.ordinal());
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOf_invalid_throwsException() {
    CameraOption.valueOf("INVALID");
  }

  @Test
  public void name_matchesConstant() {
    for (CameraOption option : CameraOption.values()) {
      assertEquals(option, CameraOption.valueOf(option.name()));
    }
  }

  @Test
  public void toString_returnsName() {
    for (CameraOption option : CameraOption.values()) {
      assertEquals(option.name(), option.toString());
    }
  }

  @Test
  public void allValues_areDistinct() {
    CameraOption[] values = CameraOption.values();
    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertNotEquals(values[i], values[j]);
      }
    }
  }
}
