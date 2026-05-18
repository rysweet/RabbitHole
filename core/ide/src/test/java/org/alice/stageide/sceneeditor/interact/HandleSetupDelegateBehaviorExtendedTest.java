package org.alice.stageide.sceneeditor.interact;

import org.alice.interact.DragAdapter;
import org.alice.interact.handle.HandleManager;
import org.alice.interact.handle.HandleSet;
import org.alice.interact.handle.ManipulationHandle;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

public class HandleSetupDelegateBehaviorExtendedTest {
  private TestDragAdapter adapter;

  @Before
  public void setUp() {
    adapter = new TestDragAdapter();
    HandleSetupDelegate.setupHandles(adapter);
  }

  @Test
  public void rotateAboutYAxisBelongsToExpectedGroups() {
    ManipulationHandle handle = findHandle("rotateAboutYAxis");

    assertTrue(handle.isMemberOf(HandleSet.HandleGroup.Y_AXIS));
    assertTrue(handle.isMemberOf(HandleSet.HandleGroup.VISUALIZATION));
    assertTrue(handle.isMemberOf(HandleSet.ROTATION_INTERACTION));
  }

  @Test
  public void translateForwardBelongsToAbsoluteTranslationAndInteractionGroups() {
    ManipulationHandle handle = findHandle("translateForward");

    assertTrue(handle.isMemberOf(HandleSet.HandleGroup.ABSOLUTE_TRANSLATION));
    assertTrue(handle.isMemberOf(HandleSet.HandleGroup.Z_AXIS));
    assertTrue(handle.isMemberOf(HandleSet.HandleGroup.X_AND_Z_AXIS));
    assertTrue(handle.isMemberOf(HandleSet.HandleGroup.INTERACTION));
  }

  @Test
  public void scaleAxisUniformBelongsToResizeGroups() {
    ManipulationHandle handle = findHandle("scaleAxisUniform");

    assertTrue(handle.isMemberOf(HandleSet.HandleGroup.RESIZE_AXIS));
    assertTrue(handle.isMemberOf(HandleSet.HandleGroup.VISUALIZATION));
    assertTrue(handle.isMemberOf(HandleSet.RESIZE_INTERACTION));
  }

  @Test
  public void jointHandlesRemainInJointGroup() {
    long jointCount = getHandles(adapter).stream()
        .filter(handle -> handle.isMemberOf(HandleSet.HandleGroup.JOINT))
        .count();

    assertEquals(6, jointCount);
  }

  @Test
  public void representativeHandlesUseExpectedConcreteTypes() {
    assertEquals("ManipulationAxes", findHandle("handleAxis").getClass().getSimpleName());
    assertEquals("StoodUpRotationRingHandle", findHandle("rotateAboutYAxisStoodUp").getClass().getSimpleName());
    assertEquals("LinearTranslateHandle", findHandle("translateJointYAxis").getClass().getSimpleName());
    assertEquals("LinearScaleHandle", findHandle("scaleAxisYZ").getClass().getSimpleName());
  }

  private ManipulationHandle findHandle(String name) {
    return getHandles(adapter).stream()
        .filter(handle -> name.equals(handle.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing handle: " + name));
  }

  @SuppressWarnings("unchecked")
  private static List<ManipulationHandle> getHandles(DragAdapter adapter) {
    try {
      Method getHandleManager = DragAdapter.class.getDeclaredMethod("getHandleManager");
      getHandleManager.setAccessible(true);
      HandleManager handleManager = (HandleManager) getHandleManager.invoke(adapter);
      Field handlesField = HandleManager.class.getDeclaredField("handles");
      handlesField.setAccessible(true);
      return (List<ManipulationHandle>) handlesField.get(handleManager);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  static class TestDragAdapter extends DragAdapter {
  }
}
