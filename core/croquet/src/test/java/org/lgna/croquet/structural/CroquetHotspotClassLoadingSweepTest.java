package org.lgna.croquet.structural;

import org.junit.Test;
import org.lgna.croquet.ClassLoadingSweepSupport;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CroquetHotspotClassLoadingSweepTest {
  @Test
  public void loadsCroquetCoverageHotspots() {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepClasses(List.of(
        "org.lgna.croquet.views.DragComponent",
        "org.lgna.croquet.views.AbstractWindow",
        "org.lgna.croquet.imp.cascade.RtItem",
        "org.lgna.croquet.views.FolderTabbedPane",
        "org.lgna.croquet.views.Dialog",
        "org.lgna.croquet.Application",
        "org.lgna.croquet.history.DragStep",
        "org.lgna.croquet.Cascade$InternalMenuModel",
        "org.lgna.croquet.AbstractDialogComposite",
        "org.lgna.croquet.AbstractSplitComposite",
        "org.lgna.croquet.ModalFrameComposite",
        "org.lgna.croquet.views.ToolBarView",
        "org.lgna.croquet.importer.Importer",
        "org.lgna.croquet.SingleSelectTableRowState",
        "org.lgna.croquet.OperationWizardDialogCoreComposite",
        "org.lgna.croquet.views.ComponentManager"
    ));
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", staticFields=" + stats.getStaticFieldAccessCount()
        + ", failures=" + stats.getFailures();

    assertEquals(summary, 16, stats.getAttemptedClassCount());
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
    assertEquals(summary, 0, stats.getStaticFieldAccessCount());
  }
}
