package org.alice.stageide.sceneeditor;

import org.junit.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CameraOptionExtendedTest {

  @Test
  public void values_orderMatchesDeclaration() {
    assertArrayEquals(new CameraOption[]{
            CameraOption.STARTING_CAMERA_VIEW,
            CameraOption.LAYOUT_SCENE_VIEW,
            CameraOption.TOP,
            CameraOption.SIDE,
            CameraOption.FRONT
        },
        CameraOption.values());
  }

  @Test
  public void enumNames_matchExpectedConstants() {
    List<String> names = Arrays.stream(CameraOption.values()).map(Enum::name).collect(Collectors.toList());

    assertEquals(Arrays.asList("STARTING_CAMERA_VIEW", "LAYOUT_SCENE_VIEW", "TOP", "SIDE", "FRONT"), names);
  }

  @Test
  public void perspectiveViews_precedeOrthographicViews() {
    assertTrue(CameraOption.STARTING_CAMERA_VIEW.compareTo(CameraOption.TOP) < 0);
    assertTrue(CameraOption.LAYOUT_SCENE_VIEW.compareTo(CameraOption.TOP) < 0);
  }

  @Test
  public void orthographicViews_areContiguousAtEnd() {
    CameraOption[] values = CameraOption.values();

    assertArrayEquals(new CameraOption[]{CameraOption.TOP, CameraOption.SIDE, CameraOption.FRONT},
        Arrays.copyOfRange(values, 2, values.length));
  }

  @Test
  public void enumSet_containsEveryCameraOption() {
    EnumSet<CameraOption> options = EnumSet.allOf(CameraOption.class);

    assertEquals(5, options.size());
    assertTrue(options.contains(CameraOption.STARTING_CAMERA_VIEW));
    assertTrue(options.contains(CameraOption.LAYOUT_SCENE_VIEW));
    assertTrue(options.contains(CameraOption.TOP));
    assertTrue(options.contains(CameraOption.SIDE));
    assertTrue(options.contains(CameraOption.FRONT));
  }

  @Test
  public void enumMap_canStoreValuesByOption() {
    EnumMap<CameraOption, String> labels = new EnumMap<>(CameraOption.class);
    labels.put(CameraOption.STARTING_CAMERA_VIEW, "start");
    labels.put(CameraOption.FRONT, "front");

    assertEquals("start", labels.get(CameraOption.STARTING_CAMERA_VIEW));
    assertEquals("front", labels.get(CameraOption.FRONT));
  }

  @Test
  public void compareTo_reflectsDeclaredOrdering() {
    assertTrue(CameraOption.STARTING_CAMERA_VIEW.compareTo(CameraOption.LAYOUT_SCENE_VIEW) < 0);
    assertTrue(CameraOption.TOP.compareTo(CameraOption.FRONT) < 0);
  }

  @Test
  public void declaringClass_isCameraOption() {
    assertSame(CameraOption.class, CameraOption.FRONT.getDeclaringClass());
  }
}
