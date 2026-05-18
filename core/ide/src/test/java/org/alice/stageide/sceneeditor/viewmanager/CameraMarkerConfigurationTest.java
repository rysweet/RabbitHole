package org.alice.stageide.sceneeditor.viewmanager;

import edu.cmu.cs.dennisc.animation.Animation;
import edu.cmu.cs.dennisc.animation.AnimationObserver;
import edu.cmu.cs.dennisc.animation.Animator;
import edu.cmu.cs.dennisc.animation.FrameObserver;
import edu.cmu.cs.dennisc.property.InstanceProperty;
import edu.cmu.cs.dennisc.scenegraph.AbstractCamera;
import edu.cmu.cs.dennisc.scenegraph.SimpleAppearance;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import edu.cmu.cs.dennisc.scenegraph.Transformable;
import edu.cmu.cs.dennisc.scenegraph.Visual;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.math.immutable.Dimension3;
import org.alice.stageide.sceneeditor.CameraOption;
import org.junit.Test;
import org.lgna.story.CameraMarker;
import org.lgna.story.implementation.CameraMarkerImp;
import org.lgna.story.implementation.ProgramImp;
import org.lgna.story.implementation.SceneImp;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

public class CameraMarkerConfigurationTest {

  @Test
  public void constructor_setsFieldsAndCallsInitialize() throws Exception {
    RecordingAnimator animator = new RecordingAnimator();
    CameraMarkerTracker tracker = createTracker(animator);
    FakeCameraMarker marker = createMarker();
    TestCameraMarkerConfiguration configuration = new TestCameraMarkerConfiguration(tracker, marker);

    assertSame(tracker, configuration.tracker);
    assertEquals(CameraOption.FRONT, configuration.cameraOption);
    assertSame(marker.getImplementation(), configuration.markerImp);
    assertEquals(1, configuration.initializeCount);
    assertEquals(MarkerUtilities.getNameForCamera(CameraOption.FRONT), marker.getName());
  }

  @Test
  public void isActive_trueWhenTrackerMatches() throws Exception {
    CameraMarkerTracker tracker = createTracker(new RecordingAnimator());
    TestCameraMarkerConfiguration configuration = new TestCameraMarkerConfiguration(tracker, createMarker());
    setField(CameraMarkerTracker.class, tracker, "activeMarker", configuration);

    assertTrue(configuration.isActiveForTest());
  }

  @Test
  public void isActive_falseWhenTrackerDoesNotMatch() throws Exception {
    CameraMarkerTracker tracker = createTracker(new RecordingAnimator());
    TestCameraMarkerConfiguration configuration = new TestCameraMarkerConfiguration(tracker, createMarker());

    assertFalse(configuration.isActiveForTest());
  }

  @Test
  public void updateCameraToNewMarkerLocation_usesCurrentCamera() throws Exception {
    CameraMarkerTracker tracker = createTracker(new RecordingAnimator());
    UpdateRecordingCameraMarkerConfiguration configuration = new UpdateRecordingCameraMarkerConfiguration(tracker, createMarker());

    configuration.updateCameraToNewMarkerLocation();

    assertTrue(configuration.animateCalled);
    assertSame(configuration.camera, configuration.previousCamera);
  }

  @Test
  public void animateToTargetView_matchingTransformStartsTrackingImmediately() throws Exception {
    RecordingAnimator animator = new RecordingAnimator();
    CameraMarkerTracker tracker = createTracker(animator);
    TestCameraMarkerConfiguration configuration = new TestCameraMarkerConfiguration(tracker, createMarker());
    configuration.targetTransform = AffineMatrix4x4.IDENTITY;

    configuration.animateToTargetView(null);

    assertEquals(1, configuration.switchToCameraCount);
    assertEquals(1, configuration.startTrackingCount);
    assertEquals(0, animator.invokeLaterCount);
    assertNull(animator.lastAnimation);
  }

  @Test
  public void animateToTargetView_differentTransformQueuesAnimation() throws Exception {
    RecordingAnimator animator = new RecordingAnimator();
    CameraMarkerTracker tracker = createTracker(animator);
    TestCameraMarkerConfiguration configuration = new TestCameraMarkerConfiguration(tracker, createMarker());
    configuration.targetTransform = AffineMatrix4x4.createTranslation(1, 2, 3);

    configuration.animateToTargetView(null);

    assertEquals(1, configuration.switchToCameraCount);
    assertEquals(0, configuration.startTrackingCount);
    assertEquals(1, animator.invokeLaterCount);
    assertNotNull(animator.lastAnimation);
  }

  @Test
  public void queuedAnimationCompletion_startsTracking() throws Exception {
    RecordingAnimator animator = new RecordingAnimator();
    CameraMarkerTracker tracker = createTracker(animator);
    TestCameraMarkerConfiguration configuration = new TestCameraMarkerConfiguration(tracker, createMarker());
    configuration.targetTransform = AffineMatrix4x4.createTranslation(4, 5, 6);

    configuration.animateToTargetView(null);
    animator.lastAnimation.complete(null);

    assertEquals(1, configuration.startTrackingCount);
  }

  private static FakeCameraMarker createMarker() throws Exception {
    FakeCameraMarker marker = new FakeCameraMarker();
    FakeCameraMarkerImp implementation = allocate(FakeCameraMarkerImp.class);
    Transformable root = new Transformable();
    Transformable markerComposite = new Transformable();
    markerComposite.setParent(root);
    implementation.abstraction = marker;
    implementation.sgComposite = markerComposite;
    implementation.paintAppearances = new SimpleAppearance[]{new SimpleAppearance()};
    implementation.opacityAppearances = implementation.paintAppearances;
    implementation.visuals = new Visual[0];
    implementation.scaleProperties = new InstanceProperty[0];
    implementation.scale = Dimension3.UNIT_SIZE;
    marker.implementation = implementation;
    return marker;
  }

  private static CameraMarkerTracker createTracker(Animator animator) throws Exception {
    CameraMarkerTracker tracker = allocate(CameraMarkerTracker.class);
    setField(CameraMarkerTracker.class, tracker, "animator", animator);
    return tracker;
  }

  private static <T> T allocate(Class<T> type) throws Exception {
    return type.cast(getUnsafe().allocateInstance(type));
  }

  private static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static sun.misc.Unsafe getUnsafe() throws Exception {
    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    return (sun.misc.Unsafe) field.get(null);
  }

  private static class TestCameraMarkerConfiguration extends CameraMarkerConfiguration<CameraMarkerImp> {
    private final CameraMarker marker;
    protected AbstractCamera camera;
    private AffineMatrix4x4 targetTransform = AffineMatrix4x4.IDENTITY;
    private int initializeCount;
    private int switchToCameraCount;
    private int startTrackingCount;

    private TestCameraMarkerConfiguration(CameraMarkerTracker tracker, CameraMarker marker) {
      super(tracker, marker, CameraOption.FRONT, "front");
      this.marker = marker;
      this.camera = new SymmetricPerspectiveCamera();
      this.camera.setParent(new Transformable());
    }

    private boolean isActiveForTest() {
      return isActive();
    }

    @Override
    protected void initialize() {
      initializeCount++;
    }

    @Override
    protected void centerOn(AxisAlignedBox box) {
    }

    @Override
    void resetForScene(SceneImp sceneImp, AffineMatrix4x4 startingView) {
    }

    @Override
    protected void stopTrackingCamera() {
    }

    @Override
    protected void switchToCamera() {
      switchToCameraCount++;
    }

    @Override
    protected AbstractCamera getCamera() {
      return camera;
    }

    @Override
    protected AffineMatrix4x4 getTargetTransform() {
      return targetTransform;
    }

    @Override
    protected void startTrackingCamera() {
      startTrackingCount++;
    }

    @Override
    void updatePicturePlaneFromCamera() {
    }
  }

  private static final class UpdateRecordingCameraMarkerConfiguration extends TestCameraMarkerConfiguration {
    private boolean animateCalled;
    private AbstractCamera previousCamera;

    private UpdateRecordingCameraMarkerConfiguration(CameraMarkerTracker tracker, CameraMarker marker) {
      super(tracker, marker);
    }

    @Override
    void animateToTargetView(AbstractCamera previousCamera) {
      animateCalled = true;
      this.previousCamera = previousCamera;
    }
  }

  private static final class FakeCameraMarker extends CameraMarker {
    private FakeCameraMarkerImp implementation;
    private String name;

    @Override
    public FakeCameraMarkerImp getImplementation() {
      return implementation;
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

  private static final class FakeCameraMarkerImp extends CameraMarkerImp {
    private FakeCameraMarker abstraction;
    private Transformable sgComposite;
    private SimpleAppearance[] paintAppearances;
    private SimpleAppearance[] opacityAppearances;
    private Visual[] visuals;
    private InstanceProperty[] scaleProperties;
    private Dimension3 scale;

    private FakeCameraMarkerImp() {
      super(null);
    }

    @Override
    public ProgramImp getProgram() {
      return null;
    }

    @Override
    protected void createVisuals() {
    }

    @Override
    public FakeCameraMarker getAbstraction() {
      return abstraction;
    }

    @Override
    public Transformable getSgComposite() {
      return sgComposite;
    }

    @Override
    protected SimpleAppearance[] getSgPaintAppearances() {
      return paintAppearances;
    }

    @Override
    protected SimpleAppearance[] getSgOpacityAppearances() {
      return opacityAppearances;
    }

    @Override
    public Visual[] getSgVisuals() {
      return visuals;
    }

    @Override
    protected InstanceProperty[] getScaleProperties() {
      return scaleProperties;
    }

    @Override
    public Dimension3 getScale() {
      return scale;
    }

    @Override
    public void setScale(Dimension3 scale) {
      this.scale = scale;
    }

    @Override
    public void setSize(Dimension3 size) {
    }
  }

  private static final class RecordingAnimator implements Animator {
    private Animation lastAnimation;
    private int invokeLaterCount;

    @Override
    public double getCurrentTime() {
      return 0;
    }

    @Override
    public double getSpeedFactor() {
      return 1;
    }

    @Override
    public void setSpeedFactor(double speedFactor) {
    }

    @Override
    public void update() {
    }

    @Override
    public void invokeLater(Animation animation, AnimationObserver animationObserver) {
      invokeLaterCount++;
      lastAnimation = animation;
    }

    @Override
    public void invokeAndWait(Animation animation, AnimationObserver animationObserver) throws InterruptedException, InvocationTargetException {
    }

    @Override
    public void invokeAndWait_ThrowRuntimeExceptionsIfNecessary(Animation animation, AnimationObserver animationObserver) {
    }

    @Override
    public void addFrameObserver(FrameObserver runnable) {
    }

    @Override
    public void removeFrameObserver(FrameObserver runnable) {
    }

    @Override
    public void completeAll() {
    }

    @Override
    public void cancelAnimation() {
    }
  }
}
