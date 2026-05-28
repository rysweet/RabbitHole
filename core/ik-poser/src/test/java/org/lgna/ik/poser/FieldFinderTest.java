package org.lgna.ik.poser;

import org.junit.Test;

import static org.junit.Assert.*;

public class FieldFinderTest {

  @Test
  public void getInstance_returnsSingleton() {
    FieldFinder instance1 = FieldFinder.getInstance();
    FieldFinder instance2 = FieldFinder.getInstance();
    assertNotNull(instance1);
    assertSame(instance1, instance2);
  }
}
