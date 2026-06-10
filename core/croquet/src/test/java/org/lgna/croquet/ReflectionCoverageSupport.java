package org.lgna.croquet;

import org.junit.Assert;

import java.util.Arrays;

public final class ReflectionCoverageSupport {
  private ReflectionCoverageSupport() {
  }

  public static void inspectClasses(String... classNames) throws Exception {
    int loadedCount = 0;
    for (String className : classNames) {
      Class<?> cls = Class.forName(className, false, ReflectionCoverageSupport.class.getClassLoader());
      Assert.assertNotNull(className, cls);
      inspectMetadata(cls);
      loadedCount++;
    }
    Assert.assertEquals(Arrays.asList(classNames).toString(), classNames.length, loadedCount);
  }

  private static void inspectMetadata(Class<?> cls) {
    Assert.assertNotNull(cls.getPackage());
    Assert.assertFalse(cls.getName().isEmpty());
    cls.getDeclaredClasses();
    cls.getDeclaredFields();
    cls.getDeclaredMethods();
    cls.getDeclaredConstructors();
    cls.getAnnotations();
    cls.getInterfaces();
    cls.getSuperclass();
  }
}
