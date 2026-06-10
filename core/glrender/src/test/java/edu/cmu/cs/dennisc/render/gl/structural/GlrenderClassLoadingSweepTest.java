package edu.cmu.cs.dennisc.render.gl.structural;

import edu.cmu.cs.dennisc.render.gl.ClassLoadingSweepSupport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GlrenderClassLoadingSweepTest {

  @Test
  public void smokeLoadsEntireGlrenderSourceTree() throws Exception {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepModuleSourceTree();
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", enums=" + stats.getEnumExerciseCount()
        + ", staticFields=" + stats.getStaticFieldAccessCount()
        + ", staticMethods=" + stats.getStaticMethodCallCount()
        + ", failures=" + stats.getFailures();

    assertTrue(summary, stats.getAttemptedClassCount() >= 125);
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
    assertEquals(summary, 0, stats.getEnumExerciseCount());
    assertEquals(summary, 0, stats.getStaticFieldAccessCount());
    assertEquals(summary, 0, stats.getStaticMethodCallCount());
  }
}
