package edu.cmu.cs.dennisc.render.gl.structural;

import edu.cmu.cs.dennisc.render.gl.ClassLoadingSweepSupport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GlrenderAdaptersPackageClassLoadingSweepTest {

  @Test
  public void loadsGlrenderAdapterPackages() throws Exception {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepModuleSourceClassesWithPrefix("edu.cmu.cs.dennisc.render.gl.imp.adapters");
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", failures=" + stats.getFailures();

    assertTrue(summary, stats.getAttemptedClassCount() >= 70);
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
  }
}
