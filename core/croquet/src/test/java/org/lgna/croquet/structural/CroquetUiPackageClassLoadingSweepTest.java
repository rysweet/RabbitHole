package org.lgna.croquet.structural;

import org.junit.Test;
import org.lgna.croquet.ClassLoadingSweepSupport;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CroquetUiPackageClassLoadingSweepTest {
  @Test
  public void loadsAdditionalUiAndSupportClasses() {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepClasses(List.of(
        "org.lgna.croquet.color.ColorChooserDialogCoreComposite",
        "org.lgna.croquet.color.ColorChooserTabComposite",
        "org.lgna.croquet.color.views.ColorChooserDialogCoreView",
        "org.lgna.croquet.color.views.ColorChooserTabView",
        "org.lgna.croquet.importer.Importer",
        "org.lgna.croquet.tools.UUIDGenerator",
        "org.lgna.croquet.views.renderers.ItemCodecListCellRenderer",
        "org.lgna.croquet.imp.dialog.InputDialogContentComposite",
        "org.lgna.croquet.imp.dialog.views.InputDialogContentPane",
        "org.lgna.croquet.imp.frame.IsFrameShowingState",
        "org.lgna.croquet.imp.frame.LazyIsFrameShowingState",
        "org.lgna.croquet.imp.launch.LazyLaunchOperation",
        "org.lgna.croquet.imp.launch.LazySimpleLaunchOperationFactory",
        "org.lgna.croquet.simple.SimpleDocumentFrame",
        "org.lgna.croquet.views.ContentPane",
        "org.lgna.croquet.views.Dialog",
        "org.lgna.croquet.views.Frame",
        "org.lgna.croquet.views.Layer",
        "org.lgna.croquet.views.LayeredPane",
        "org.lgna.croquet.views.Window"
    ));
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", staticFields=" + stats.getStaticFieldAccessCount()
        + ", failures=" + stats.getFailures();

    assertEquals(summary, 20, stats.getAttemptedClassCount());
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
    assertEquals(summary, 0, stats.getStaticFieldAccessCount());
  }
}
