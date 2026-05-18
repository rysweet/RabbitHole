package org.alice.stageide.sceneeditor;

import org.alice.ide.instancefactory.InstanceFactory;
import org.alice.interact.manipulator.scenegraph.SnapGrid;
import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.event.ValueEvent;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.JavaType;
import org.lgna.croquet.icon.IconFactory;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class SceneEditorListenersExtendedTest {
  private StorytellingSceneEditor editor;
  private RecordingFieldManager fieldManager;
  private SceneEditorListeners listeners;

  @Before
  public void setUp() throws Exception {
    editor = (StorytellingSceneEditor) getUnsafe().allocateInstance(StorytellingSceneEditor.class);
    editor.snapGrid = new SnapGrid();
    fieldManager = new RecordingFieldManager(editor);
    setField(editor, StorytellingSceneEditor.class, "fieldManager", fieldManager);
    listeners = new SceneEditorListeners(editor);
  }

  @Test
  public void showSnapGridListenerUpdatesSnapGridShowing() {
    listeners.showSnapGridListener.valueChanged(ValueEvent.createInstance(false, true));
    assertTrue(editor.snapGrid.getShowing());
  }

  @Test
  public void snapGridSpacingListenerUpdatesSnapGridSpacing() throws Exception {
    listeners.snapGridSpacingListener.valueChanged(ValueEvent.createInstance(0.5, 2.5));
    assertEquals(2.5, getGridSpacing(editor.snapGrid), 0.0);
  }

  @Test
  public void cameraMarkerFieldSelectionListenerForwardsField() {
    UserField field = createField("cameraMarker");
    listeners.cameraMarkerFieldSelectionListener.valueChanged(ValueEvent.createInstance(field));
    assertSame(field, fieldManager.cameraMarkerField);
  }

  @Test
  public void objectMarkerFieldSelectionListenerForwardsField() {
    UserField field = createField("objectMarker");
    listeners.objectMarkerFieldSelectionListener.valueChanged(ValueEvent.createInstance(field));
    assertSame(field, fieldManager.objectMarkerField);
  }

  @Test
  public void instanceFactorySelectionListenerTogglesSelectionFlagAroundDelegation() {
    InstanceFactory instanceFactory = new StubInstanceFactory();

    listeners.instanceFactorySelectionListener.valueChanged(ValueEvent.createInstance(instanceFactory));

    assertSame(instanceFactory, fieldManager.instanceFactory);
    assertTrue(fieldManager.selectionFlagDuringSetSelectedInstance);
    assertFalse(editor.selectionIsFromInstanceSelector);
  }

  @Test
  public void mainCameraViewSelectionObserverDelegatesToFieldManager() {
    listeners.mainCameraViewSelectionObserver.valueChanged(ValueEvent.createInstance(CameraOption.TOP));
    assertEquals(1, fieldManager.mainCameraViewSelectionCount);
  }

  private static UserField createField(String name) {
    UserField field = new UserField();
    field.name.setValue(name);
    field.valueType.setValue(JavaType.getInstance(Object.class));
    return field;
  }

  private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static double getGridSpacing(SnapGrid snapGrid) throws Exception {
    Field gridSpacing = SnapGrid.class.getDeclaredField("gridSpacing");
    gridSpacing.setAccessible(true);
    return gridSpacing.getDouble(snapGrid);
  }

  private static sun.misc.Unsafe getUnsafe() throws Exception {
    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    return (sun.misc.Unsafe) field.get(null);
  }

  private static final class RecordingFieldManager extends SceneEditorFieldManager {
    private UserField cameraMarkerField;
    private UserField objectMarkerField;
    private InstanceFactory instanceFactory;
    private boolean selectionFlagDuringSetSelectedInstance;
    private int mainCameraViewSelectionCount;

    private RecordingFieldManager(StorytellingSceneEditor editor) {
      super(editor);
    }

    @Override
    void handleCameraMarkerFieldSelection(UserField cameraMarkerField) {
      this.cameraMarkerField = cameraMarkerField;
    }

    @Override
    void handleObjectMarkerFieldSelection(UserField objectMarkerField) {
      this.objectMarkerField = objectMarkerField;
    }

    @Override
    void setSelectedInstance(InstanceFactory instanceFactory) {
      this.instanceFactory = instanceFactory;
      this.selectionFlagDuringSetSelectedInstance = this.editor.selectionIsFromInstanceSelector;
    }

    @Override
    void handleMainCameraViewSelection() {
      this.mainCameraViewSelectionCount++;
    }
  }

  private static final class StubInstanceFactory implements InstanceFactory {
    @Override public boolean isValid() { return true; }
    @Override public AbstractType<?, ?, ?> getValueType() { return null; }
    @Override public Expression createTransientExpression() { return null; }
    @Override public Expression createExpression() { return null; }
    @Override public String getRepr() { return "stub"; }
    @Override public IconFactory getIconFactory() { return null; }
    @Override public edu.cmu.cs.dennisc.property.InstanceProperty<?>[] getMutablePropertiesOfInterest() { return new edu.cmu.cs.dennisc.property.InstanceProperty<?>[0]; }
  }
}
