package org.alice.stageide.sceneeditor.side;

import org.junit.Test;
import static org.junit.Assert.*;

public class PropertyAndMarkerPanelOptionsTest {

  @Test
  public void values_hasTwoConstants() {
    assertEquals(2, PropertyAndMarkerPanelOptions.values().length);
  }

  @Test
  public void valueOf_properties() {
    assertEquals(PropertyAndMarkerPanelOptions.PROPERTIES,
        PropertyAndMarkerPanelOptions.valueOf("PROPERTIES"));
  }

  @Test
  public void valueOf_markers() {
    assertEquals(PropertyAndMarkerPanelOptions.MARKERS,
        PropertyAndMarkerPanelOptions.valueOf("MARKERS"));
  }

  @Test
  public void ordinals_correct() {
    assertEquals(0, PropertyAndMarkerPanelOptions.PROPERTIES.ordinal());
    assertEquals(1, PropertyAndMarkerPanelOptions.MARKERS.ordinal());
  }

  @Test
  public void name_matchesConstant() {
    for (PropertyAndMarkerPanelOptions opt : PropertyAndMarkerPanelOptions.values()) {
      assertEquals(opt, PropertyAndMarkerPanelOptions.valueOf(opt.name()));
    }
  }

  @Test
  public void toString_matchesName() {
    for (PropertyAndMarkerPanelOptions opt : PropertyAndMarkerPanelOptions.values()) {
      assertEquals(opt.name(), opt.toString());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOf_invalid_throws() {
    PropertyAndMarkerPanelOptions.valueOf("INVALID");
  }

  @Test
  public void valuesArray_containsBothConstants() {
    PropertyAndMarkerPanelOptions[] values = PropertyAndMarkerPanelOptions.values();
    assertSame(PropertyAndMarkerPanelOptions.PROPERTIES, values[0]);
    assertSame(PropertyAndMarkerPanelOptions.MARKERS, values[1]);
  }
}
