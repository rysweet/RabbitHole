package org.alice.stageide.sceneeditor.viewmanager;

import edu.cmu.cs.dennisc.property.event.PropertyEvent;
import edu.cmu.cs.dennisc.scenegraph.AbstractCamera;
import edu.cmu.cs.dennisc.scenegraph.OrthographicCamera;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.stageide.sceneeditor.CameraOption;
import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.event.ValueEvent;
import org.lgna.story.implementation.CameraMarkerImp;
import org.lgna.story.implementation.SceneImp;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CameraMarkerTrackerExtendedTest {
  private CameraMarkerTracker tracker;
  private Map<CameraOption, CameraMarkerConfiguration<?>> markerMap;

  @Before
  public void setUp() throws Exception {
    tracker = (CameraMarkerTracker) getUnsafe().allocateInstance(CameraMarkerTracker.class);
    markerMap = new EnumMap<>(CameraOption.class);
    setField(tracker, "mapViewToMarker", markerMap);
  }

  @Test
  public void valueChangedIgnoresSelectionsUntilAllCamerasAreSet() throws Exception {
    RecordingMarkerConfiguration marker = createMarker(new OrthographicCamera());
    markerMap.put(CameraOption.TOP, marker);

    tracker.valueChanged(ValueEvent.createInstance(CameraOption.TOP));

    assertEquals(0, marker.animateCount);
    assertNull(getField(tracker, "activeMarker"));
  }

  @Test
  public void valueChangedStopsPreviousMarkerAndAnimatesToNextMarker() throws Exception {
    SymmetricPerspectiveCamera main = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera layout = new SymmetricPerspectiveCamera();
    OrthographicCamera orthographic = new OrthographicCamera();
    tracker.setCameras(main, layout, orthographic);

    RecordingMarkerConfiguration previous = createMarker(layout);
    RecordingMarkerConfiguration next = createMarker(orthographic);
    markerMap.put(CameraOption.FRONT, previous);
    markerMap.put(CameraOption.TOP, next);
    setField(tracker, "activeMarker", previous);

    tracker.valueChanged(ValueEvent.createInstance(CameraOption.TOP));

    assertEquals(1, previous.stopTrackingCount);
    assertEquals(1, next.animateCount);
    assertSame(layout, next.previousCamera);
    assertSame(next, getField(tracker, "activeMarker"));
  }

  @Test
  public void valueChangedIgnoresAlreadyActiveMarker() throws Exception {
    SymmetricPerspectiveCamera main = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera layout = new SymmetricPerspectiveCamera();
    OrthographicCamera orthographic = new OrthographicCamera();
    tracker.setCameras(main, layout, orthographic);

    RecordingMarkerConfiguration marker = createMarker(orthographic);
    markerMap.put(CameraOption.TOP, marker);
    setField(tracker, "activeMarker", marker);

    tracker.valueChanged(ValueEvent.createInstance(CameraOption.TOP));

    assertEquals(0, marker.animateCount);
    assertEquals(0, marker.stopTrackingCount);
  }

  @Test
  public void propertyChangedIgnoresEventsForOtherOrthographicCameras() throws Exception {
    tracker.setCameras(new SymmetricPerspectiveCamera(), new SymmetricPerspectiveCamera(), new OrthographicCamera());
    RecordingMarkerConfiguration marker = createMarker(new OrthographicCamera());
    setField(tracker, "activeMarker", marker);

    OrthographicCamera otherCamera = new OrthographicCamera();
    tracker.propertyChanged(new PropertyEvent(otherCamera.picturePlane, otherCamera, otherCamera.picturePlane.getValue()));

    assertEquals(0, marker.picturePlaneUpdateCount);
  }

  @Test
  public void propertyChangedUpdatesActiveMarkerForTrackedPicturePlane() throws Exception {
    OrthographicCamera orthographic = new OrthographicCamera();
    tracker.setCameras(new SymmetricPerspectiveCamera(), new SymmetricPerspectiveCamera(), orthographic);
    RecordingMarkerConfiguration marker = createMarker(orthographic);
    setField(tracker, "activeMarker", marker);

    tracker.propertyChanged(new PropertyEvent(orthographic.picturePlane, orthographic, orthographic.picturePlane.getValue()));

    assertEquals(1, marker.picturePlaneUpdateCount);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = CameraMarkerTracker.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Object getField(Object target, String name) throws Exception {
    Field field = CameraMarkerTracker.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static sun.misc.Unsafe getUnsafe() throws Exception {
    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    return (sun.misc.Unsafe) field.get(null);
  }

  private static RecordingMarkerConfiguration createMarker(AbstractCamera camera) throws Exception {
    RecordingMarkerConfiguration marker = (RecordingMarkerConfiguration) getUnsafe().allocateInstance(RecordingMarkerConfiguration.class);
    marker.camera = camera;
    return marker;
  }

  private static final class RecordingMarkerConfiguration extends CameraMarkerConfiguration<CameraMarkerImp> {
    private AbstractCamera camera;
    private int animateCount;
    private int stopTrackingCount;
    private int picturePlaneUpdateCount;
    private AbstractCamera previousCamera;

    private RecordingMarkerConfiguration() {
      super(null, null, null, null);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void centerOn(AxisAlignedBox box) {
    }

    @Override
    void resetForScene(SceneImp sceneImp, AffineMatrix4x4 startingView) {
    }

    @Override
    protected void stopTrackingCamera() {
      stopTrackingCount++;
    }

    @Override
    protected void switchToCamera() {
    }

    @Override
    protected AbstractCamera getCamera() {
      return camera;
    }

    @Override
    void animateToTargetView(AbstractCamera previousCamera) {
      animateCount++;
      this.previousCamera = previousCamera;
    }

    @Override
    protected AffineMatrix4x4 getTargetTransform() {
      return AffineMatrix4x4.IDENTITY;
    }

    @Override
    protected void startTrackingCamera() {
    }

    @Override
    void updatePicturePlaneFromCamera() {
      picturePlaneUpdateCount++;
    }
  }
}
