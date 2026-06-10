package edu.cmu.cs.dennisc.render.gl.structural;

import edu.cmu.cs.dennisc.render.gl.ClassLoadingSweepSupport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GlrenderImpPackageClassLoadingSweepTest {

  @Test
  public void loadsGlrenderImpPackage() throws Exception {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepModuleSourceClassesWithPrefix("edu.cmu.cs.dennisc.render.gl.imp.");
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", staticFields=" + stats.getStaticFieldAccessCount()
        + ", failures=" + stats.getFailures();

    assertTrue(summary, stats.getAttemptedClassCount() >= 25);
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
    assertEquals(summary, 0, stats.getStaticFieldAccessCount());
  }
}
