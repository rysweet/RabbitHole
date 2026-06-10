package edu.cmu.cs.dennisc.render.gl;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReflectionCoverageSupportSmokeTest {
  private static final AtomicInteger INSPECT_STATIC_INITIALIZER_CALLS = new AtomicInteger();
  private static final AtomicInteger INSPECT_CONSTRUCTOR_CALLS = new AtomicInteger();
  private static final AtomicInteger INSPECT_METHOD_CALLS = new AtomicInteger();
  private static final AtomicInteger SWEEP_STATIC_INITIALIZER_CALLS = new AtomicInteger();
  private static final AtomicInteger SWEEP_CONSTRUCTOR_CALLS = new AtomicInteger();
  private static final AtomicInteger SWEEP_METHOD_CALLS = new AtomicInteger();
  private static final AtomicInteger SWEEP_STATIC_METHOD_CALLS = new AtomicInteger();

  @Test
  public void inspectClassesResolvesNamedClassesHeadlessly() throws Exception {
    ReflectionCoverageSupport.inspectClasses(
        "edu.cmu.cs.dennisc.render.gl.TextureBinding",
        "edu.cmu.cs.dennisc.render.gl.imp.RenderContext",
        "edu.cmu.cs.dennisc.render.gl.imp.GlResourceCache");
  }

  @Test(expected = ClassNotFoundException.class)
  public void inspectClassesFailsForInvalidNames() throws Exception {
    ReflectionCoverageSupport.inspectClasses("edu.cmu.cs.dennisc.render.gl.DoesNotExist");
  }

  @Test
  public void inspectClassesDoesNotInitializeClasses() throws Exception {
    INSPECT_STATIC_INITIALIZER_CALLS.set(0);

    ReflectionCoverageSupport.inspectClasses(InspectStaticInitializerTrap.class.getName());

    assertEquals(0, INSPECT_STATIC_INITIALIZER_CALLS.get());
  }

  @Test
  public void inspectClassesDoesNotInvokeConstructorsOrMethods() throws Exception {
    INSPECT_CONSTRUCTOR_CALLS.set(0);
    INSPECT_METHOD_CALLS.set(0);

    ReflectionCoverageSupport.inspectClasses(InspectBehaviorTrap.class.getName());

    assertEquals(0, INSPECT_CONSTRUCTOR_CALLS.get());
    assertEquals(0, INSPECT_METHOD_CALLS.get());
  }

  @Test
  public void classLoadingSweepIsSmokeOnly() {
    SWEEP_STATIC_INITIALIZER_CALLS.set(0);
    SWEEP_CONSTRUCTOR_CALLS.set(0);
    SWEEP_METHOD_CALLS.set(0);
    SWEEP_STATIC_METHOD_CALLS.set(0);

    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepClasses(List.of(
        SweepStaticInitializerTrap.class.getName(),
        SweepBehaviorTrap.class.getName(),
        SweepStaticMethodTrap.class.getName(),
        SweepEnumTrap.class.getName()));
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", enums=" + stats.getEnumExerciseCount()
        + ", staticFields=" + stats.getStaticFieldAccessCount()
        + ", staticMethods=" + stats.getStaticMethodCallCount()
        + ", failures=" + stats.getFailures();

    assertEquals(summary, 4, stats.getAttemptedClassCount());
    assertEquals(summary, 4, stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
    assertEquals(summary, 0, stats.getEnumExerciseCount());
    assertEquals(summary, 0, stats.getStaticFieldAccessCount());
    assertEquals(summary, 0, stats.getStaticMethodCallCount());
    assertEquals(0, SWEEP_STATIC_INITIALIZER_CALLS.get());
    assertEquals(0, SWEEP_CONSTRUCTOR_CALLS.get());
    assertEquals(0, SWEEP_METHOD_CALLS.get());
    assertEquals(0, SWEEP_STATIC_METHOD_CALLS.get());
  }

  public static final class InspectStaticInitializerTrap {
    static {
      INSPECT_STATIC_INITIALIZER_CALLS.incrementAndGet();
    }
  }

  public static final class InspectBehaviorTrap {
    public InspectBehaviorTrap() {
      INSPECT_CONSTRUCTOR_CALLS.incrementAndGet();
    }

    public void recordInvocation() {
      INSPECT_METHOD_CALLS.incrementAndGet();
    }
  }

  public static final class SweepStaticInitializerTrap {
    static {
      SWEEP_STATIC_INITIALIZER_CALLS.incrementAndGet();
    }
  }

  public static final class SweepBehaviorTrap {
    public SweepBehaviorTrap() {
      SWEEP_CONSTRUCTOR_CALLS.incrementAndGet();
    }

    public void recordInvocation() {
      SWEEP_METHOD_CALLS.incrementAndGet();
    }
  }

  public static final class SweepStaticMethodTrap {
    public static void recordInvocation() {
      SWEEP_STATIC_METHOD_CALLS.incrementAndGet();
    }
  }

  public enum SweepEnumTrap {
    VALUE
  }
}
