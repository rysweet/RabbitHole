package org.alice.stageide.sceneeditor;

import org.alice.ide.croquet.models.gallerybrowser.GalleryDragModel;
import org.alice.stageide.modelresource.ResourceNode;
import org.alice.stageide.sceneeditor.interact.GlobalDragAdapter;
import org.alice.math.immutable.AxisAlignedBox;
import org.junit.Test;
import org.lgna.croquet.*;
import org.lgna.croquet.history.DragStep;
import org.lgna.croquet.icon.IconFactory;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class SceneEditorDropReceptorExtendedTest {
  @Test
  public void isPotentiallyAcceptingOfDistinguishesGalleryModels() throws Exception {
    SceneEditorDropReceptor receptor = new SceneEditorDropReceptor(newEditor());

    assertTrue(receptor.isPotentiallyAcceptingOf(new StubGalleryDragModel()));
    assertFalse(receptor.isPotentiallyAcceptingOf(new StubDragModel()));
  }

  @Test
  public void getTrackableShapeAndViewControllerReturnEditor() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    SceneEditorDropReceptor receptor = new SceneEditorDropReceptor(editor);

    assertSame(editor, receptor.getTrackableShape(null));
    assertSame(editor, receptor.getViewController());
  }

  @Test
  public void dragStoppedForwardsToGlobalDragAdapter() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    RecordingGlobalDragAdapter adapter = (RecordingGlobalDragAdapter) getUnsafe().allocateInstance(RecordingGlobalDragAdapter.class);
    editor.globalDragAdapter = adapter;
    SceneEditorDropReceptor receptor = new SceneEditorDropReceptor(editor);
    DragStep dragStep = (DragStep) getUnsafe().allocateInstance(DragStep.class);

    receptor.dragStopped(dragStep);

    assertSame(dragStep, adapter.exitedStep);
  }

  @Test
  public void overLookingGlassStartsFalseAndDragExitedLeavesItUntouched() throws Exception {
    SceneEditorDropReceptor receptor = new SceneEditorDropReceptor(newEditor());
    Field field = SceneEditorDropReceptor.class.getDeclaredField("overLookingGlass");
    field.setAccessible(true);

    assertFalse(field.getBoolean(receptor));
    field.setBoolean(receptor, true);
    receptor.dragExited(null, false);
    assertTrue(field.getBoolean(receptor));
  }

  private static StorytellingSceneEditor newEditor() throws Exception {
    return (StorytellingSceneEditor) getUnsafe().allocateInstance(StorytellingSceneEditor.class);
  }

  private static sun.misc.Unsafe getUnsafe() throws Exception {
    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    return (sun.misc.Unsafe) field.get(null);
  }

  private static final class RecordingGlobalDragAdapter extends GlobalDragAdapter {
    private DragStep exitedStep;

    private RecordingGlobalDragAdapter() {
      super(null);
    }

    @Override
    public void dragExited(DragStep dragStep) {
      this.exitedStep = dragStep;
    }
  }

  private static final class StubDragModel extends org.lgna.croquet.AbstractModel implements DragModel {
    private StubDragModel() {
      super(UUID.randomUUID());
    }

    @Override public List<? extends DropReceptor> createListOfPotentialDropReceptors() { return Collections.emptyList(); }
    @Override public void handleDragStarted(DragStep step) { }
    @Override public void handleDragEnteredDropReceptor(DragStep step) { }
    @Override public void handleDragExitedDropReceptor(DragStep step) { }
    @Override public void handleDragStopped(DragStep step) { }
    @Override public Triggerable getDropOperation(DragStep step, DropSite dropSite) { return null; }
    @Override protected Class<? extends org.lgna.croquet.Element> getClassUsedForLocalization() { return StubDragModel.class; }
    @Override protected void localize() { }
  }

  private static final class StubGalleryDragModel extends GalleryDragModel {
    private StubGalleryDragModel() {
      super(UUID.randomUUID());
    }

    @Override public String getText() { return "stub"; }
    @Override public IconFactory getIconFactory() { return null; }
    @Override public Triggerable getLeftButtonClickOperation(org.lgna.croquet.SingleSelectTreeState<ResourceNode> controller) { return null; }
    @Override public AxisAlignedBox getBoundingBox() { return null; }
    @Override public boolean placeOnGround() { return false; }
    @Override public Triggerable getDropOperation(DragStep step, DropSite dropSite) { return null; }
    @Override protected void localize() { }
  }
}
