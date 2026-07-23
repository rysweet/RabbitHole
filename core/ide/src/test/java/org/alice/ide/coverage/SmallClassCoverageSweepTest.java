package org.alice.ide.coverage;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmallClassCoverageSweepTest {
  @Test
  public void exercisesLongTailClassesHeadlessly() {
    HeadlessClassExerciseSupport.SmokeStats stats = HeadlessClassExerciseSupport.exerciseSweepTargets();

    assertTrue("expected many discovered target classes", stats.getDiscoveredTargetCount() >= 110);
    assertTrue("expected many classes to load", stats.getLoadedCount() >= 90);
    assertTrue("expected many classes to instantiate", stats.getInstantiatedCount() >= 40);
    assertTrue("expected few headless exercise failures", stats.getFailureCount() <= 20);
  }

  /**
   * Regression guard: the sweep force-initializes every target via
   * {@code Class.forName(name, true, ...)}. Classes whose static initializer
   * requires a live IDE singleton (e.g. DeclarationMeta) NPE when swept
   * headlessly, which poisons the class JVM-wide (ExceptionInInitializerError ->
   * NoClassDefFoundError) and breaks unrelated tests in the same reused fork.
   * Such classes must never be listed as sweep targets.
   */
  @Test
  public void doesNotSweepIdeSingletonDependentClasses() throws Exception {
    String targets;
    try (InputStream in = Objects.requireNonNull(
        getClass().getResourceAsStream("/org/alice/ide/coverage/small-class-targets.txt"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      targets = reader.lines()
          .filter(line -> !line.isBlank() && !line.startsWith("#"))
          .collect(Collectors.joining("\n"));
    }

    assertFalse("DeclarationMeta must not be a sweep target: its static initializer "
            + "needs an active IDE and poisons the class when force-loaded headlessly",
        targets.contains("org.alice.ide.meta.DeclarationMeta"));
  }
}
