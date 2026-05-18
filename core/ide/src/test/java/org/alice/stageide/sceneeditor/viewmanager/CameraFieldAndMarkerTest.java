package org.alice.stageide.sceneeditor.viewmanager;

import org.junit.Test;
import org.lgna.project.ast.UserField;
import org.lgna.story.CameraMarker;
import org.lgna.story.OrthographicCameraMarker;
import org.lgna.story.implementation.CameraMarkerImp;
import org.lgna.story.implementation.OrthographicCameraMarkerImp;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class CameraFieldAndMarkerTest {

  @Test
  public void constructor_setsFieldAndMarker() {
    UserField field = new UserField("cameraField", Object.class);
    NamedCameraMarker marker = new NamedCameraMarker();
    CameraFieldAndMarker cameraFieldAndMarker = new CameraFieldAndMarker(field, marker);

    assertSame(field, cameraFieldAndMarker.field);
    assertSame(marker, cameraFieldAndMarker.marker);
  }

  @Test
  public void isOrthographic_nullMarker_false() {
    assertFalse(new CameraFieldAndMarker(null, null).isOrthographic());
  }

  @Test
  public void isOrthographic_orthographicMarker_true() throws Exception {
    assertTrue(new CameraFieldAndMarker(null, createOrthographicMarker("ortho")).isOrthographic());
  }

  @Test
  public void isOrthographic_perspectiveMarker_false() {
    assertFalse(new CameraFieldAndMarker(null, new NamedCameraMarker()).isOrthographic());
  }

  @Test
  public void isPerspective_nullMarker_true() {
    assertTrue(new CameraFieldAndMarker(null, null).isPerspective());
  }

  @Test
  public void isPerspective_orthographicMarker_false() throws Exception {
    assertFalse(new CameraFieldAndMarker(null, createOrthographicMarker("ortho")).isPerspective());
  }

  @Test
  public void isPerspective_perspectiveMarker_true() {
    assertTrue(new CameraFieldAndMarker(null, new NamedCameraMarker()).isPerspective());
  }

  @Test
  public void toString_fieldNamePreferredOverMarkerName() {
    UserField field = new UserField("cameraField", Object.class);
    NamedCameraMarker marker = new NamedCameraMarker();
    marker.setName("markerName");

    assertEquals("cameraField", new CameraFieldAndMarker(field, marker).toString());
  }

  @Test
  public void toString_markerNameUsedWhenFieldNull() {
    NamedCameraMarker marker = new NamedCameraMarker();
    marker.setName("markerName");

    assertEquals("markerName", new CameraFieldAndMarker(null, marker).toString());
  }

  @Test
  public void toString_nullFieldNullMarker_noName() {
    assertEquals("NO_NAME", new CameraFieldAndMarker(null, null).toString());
  }

  @Test
  public void equals_sameField_true() throws Exception {
    UserField field = new UserField("cameraField", Object.class);
    CameraFieldAndMarker first = new CameraFieldAndMarker(field, new NamedCameraMarker());
    CameraFieldAndMarker second = new CameraFieldAndMarker(field, createOrthographicMarker("ortho"));

    assertTrue(first.equals(second));
  }

  @Test
  public void equals_differentField_false() {
    CameraFieldAndMarker first = new CameraFieldAndMarker(new UserField("first", Object.class), new NamedCameraMarker());
    CameraFieldAndMarker second = new CameraFieldAndMarker(new UserField("second", Object.class), new NamedCameraMarker());

    assertFalse(first.equals(second));
  }

  @Test
  public void equals_sameObject_true() {
    CameraFieldAndMarker cam = new CameraFieldAndMarker(null, null);
    assertTrue(cam.equals(cam));
  }

  @Test
  public void equals_differentType_false() {
    CameraFieldAndMarker cam = new CameraFieldAndMarker(null, null);
    assertFalse(cam.equals("not a camera"));
  }

  @Test
  public void equals_null_false() {
    CameraFieldAndMarker cam = new CameraFieldAndMarker(null, null);
    assertFalse(cam.equals(null));
  }

  private static NamedOrthographicCameraMarker createOrthographicMarker(String name) throws Exception {
    NamedOrthographicCameraMarker marker = allocate(NamedOrthographicCameraMarker.class);
    marker.setName(name);
    return marker;
  }

  private static <T> T allocate(Class<T> type) throws Exception {
    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
    return type.cast(unsafe.allocateInstance(type));
  }

  private static class NamedCameraMarker extends CameraMarker {
    private String name;

    @Override
    public CameraMarkerImp getImplementation() {
      return null;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }
  }

  private static class NamedOrthographicCameraMarker extends OrthographicCameraMarker {
    private String name;

    @Override
    public OrthographicCameraMarkerImp getImplementation() {
      return null;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }
  }
}
