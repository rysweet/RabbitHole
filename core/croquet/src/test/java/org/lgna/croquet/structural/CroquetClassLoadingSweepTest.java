package org.lgna.croquet.structural;

import org.junit.Test;
import org.lgna.croquet.ClassLoadingSweepSupport;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CroquetClassLoadingSweepTest {
  @Test
  public void smokeLoadsAdditionalCroquetClasses() {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepClasses(List.of(
        "org.lgna.croquet.CompositeLocalizationDelegate",
        "org.lgna.croquet.CompositeResourceManager",
        "org.lgna.croquet.CompositeTabManager",
        "org.lgna.croquet.CompositeViewLifecycle",
        "org.lgna.croquet.DataIndexPair",
        "org.lgna.croquet.EmptyConditionText",
        "org.lgna.croquet.Group",
        "org.lgna.croquet.InternalStateTypes",
        "org.lgna.croquet.ListSelectionListenerAdapter",
        "org.lgna.croquet.OwnedByCompositeOperation",
        "org.lgna.croquet.OwnedByCompositeOperationSubKey",
        "org.lgna.croquet.OwnedByCompositeValueCreator",
        "org.lgna.croquet.SingleSelectListStateComboBoxPrepModel",
        "org.lgna.croquet.ValueHolder",
        "org.lgna.croquet.data.ImmutableListData",
        "org.lgna.croquet.data.MutableListData",
        "org.lgna.croquet.edits.StateEdit",
        "org.lgna.croquet.event.ValueEvent",
        "org.lgna.croquet.history.PopupPrepStep",
        "org.lgna.croquet.imp.booleanstate.BooleanStateMenuModel",
        "org.lgna.croquet.imp.booleanstate.BooleanStateSetToValueOperation",
        "org.lgna.croquet.imp.cascade.RtBlank",
        "org.lgna.croquet.imp.cascade.RtNode",
        "org.lgna.croquet.imp.dialog.LaunchOperationOwningCompositeImp",
        "org.lgna.croquet.imp.operation.OperationMenuItemPrepModel",
        "org.lgna.croquet.views.AwtHierarchyHandler",
        "org.lgna.croquet.views.FolderTitlesPanel",
        "org.lgna.croquet.views.Separator",
        "org.lgna.croquet.views.ToolPaletteView",
        "org.lgna.croquet.views.imp.JScrollMenuItem",
        "org.lgna.croquet.views.imp.ScrollDirection",
        "org.lgna.croquet.views.imp.ScrollingPopupMenuLayout"
    ));
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", enums=" + stats.getEnumExerciseCount()
        + ", staticFields=" + stats.getStaticFieldAccessCount()
        + ", codecs=" + stats.getCodecExerciseCount()
        + ", failures=" + stats.getFailures();

    assertEquals(summary, 32, stats.getAttemptedClassCount());
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
    assertEquals(summary, 0, stats.getEnumExerciseCount());
    assertEquals(summary, 0, stats.getStaticFieldAccessCount());
    assertEquals(summary, 0, stats.getCodecExerciseCount());
  }
}
