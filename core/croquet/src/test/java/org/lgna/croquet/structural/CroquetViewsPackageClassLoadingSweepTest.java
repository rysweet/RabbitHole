package org.lgna.croquet.structural;

import org.junit.Test;
import org.lgna.croquet.ClassLoadingSweepSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CroquetViewsPackageClassLoadingSweepTest {
  @Test
  public void loadsCroquetViewsPackage() throws Exception {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepModuleSourceClassesWithPrefix("org.lgna.croquet.views.");
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", staticFields=" + stats.getStaticFieldAccessCount()
        + ", failures=" + stats.getFailures();

    assertTrue(summary, stats.getAttemptedClassCount() >= 60);
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
    assertEquals(summary, 0, stats.getStaticFieldAccessCount());
  }
}
