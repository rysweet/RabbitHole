package edu.cmu.cs.dennisc.render.gl;

import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ClassLoadingSweepSupport {
  private ClassLoadingSweepSupport() {
  }

  public static SweepStats sweepModuleSourceTree() throws IOException {
    return sweepClasses(discoverClassNames(Paths.get(System.getProperty("basedir", "."), "src", "main", "java")));
  }

  public static SweepStats sweepModuleSourceClassesWithPrefix(String prefix) throws IOException {
    List<String> classNames = discoverClassNames(Paths.get(System.getProperty("basedir", "."), "src", "main", "java"))
        .stream()
        .filter(className -> className.startsWith(prefix))
        .toList();
    return sweepClasses(classNames);
  }

  public static SweepStats sweepClasses(List<String> classNames) {
    int loadedClassCount = 0;
    List<String> failures = new ArrayList<>();

    for (String className : classNames) {
      try {
        Class<?> cls = Class.forName(className, false, ClassLoadingSweepSupport.class.getClassLoader());
        inspectMetadata(cls);
        loadedClassCount++;
      } catch (Throwable throwable) {
        failures.add(className + " -> " + throwable.getClass().getSimpleName());
      }
    }

    return new SweepStats(classNames.size(), loadedClassCount, 0, 0, 0, 0, failures);
  }

  private static List<String> discoverClassNames(Path sourceRoot) throws IOException {
    try (Stream<Path> stream = Files.walk(sourceRoot)) {
      return stream
          .filter(path -> path.toString().endsWith(".java"))
          .filter(path -> !path.getFileName().toString().equals("package-info.java"))
          .filter(path -> !path.getFileName().toString().equals("module-info.java"))
          .filter(ClassLoadingSweepSupport::declaresLoadableTopLevelType)
          .map(path -> sourceRoot.relativize(path).toString())
          .map(relative -> relative.substring(0, relative.length() - ".java".length()))
          .map(relative -> relative.replace(File.separatorChar, '.'))
          .sorted()
          .toList();
    }
  }

  private static boolean declaresLoadableTopLevelType(Path sourceFile) {
    String simpleName = sourceFile.getFileName().toString().replaceFirst("\\.java$", "");
    Pattern declaration = Pattern.compile("(?m)^\\s*(?:public\\s+|protected\\s+|private\\s+|abstract\\s+|final\\s+|static\\s+|strictfp\\s+|sealed\\s+|non-sealed\\s+)*(@interface|class|enum|interface|record)\\s+" + Pattern.quote(simpleName) + "\\b");
    try {
      return declaration.matcher(Files.readString(sourceFile)).find();
    } catch (IOException ioe) {
      throw new IllegalStateException("Unable to inspect source file " + sourceFile, ioe);
    }
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

  public static final class SweepStats {
    private final int attemptedClassCount;
    private final int loadedClassCount;
    private final int instantiatedClassCount;
    private final int enumExerciseCount;
    private final int staticFieldAccessCount;
    private final int staticMethodCallCount;
    private final List<String> failures;

    private SweepStats(int attemptedClassCount, int loadedClassCount, int instantiatedClassCount,
        int enumExerciseCount, int staticFieldAccessCount, int staticMethodCallCount,
        List<String> failures) {
      this.attemptedClassCount = attemptedClassCount;
      this.loadedClassCount = loadedClassCount;
      this.instantiatedClassCount = instantiatedClassCount;
      this.enumExerciseCount = enumExerciseCount;
      this.staticFieldAccessCount = staticFieldAccessCount;
      this.staticMethodCallCount = staticMethodCallCount;
      this.failures = List.copyOf(failures);
    }

    public int getAttemptedClassCount() {
      return attemptedClassCount;
    }

    public int getLoadedClassCount() {
      return loadedClassCount;
    }

    public int getInstantiatedClassCount() {
      return instantiatedClassCount;
    }

    public int getEnumExerciseCount() {
      return enumExerciseCount;
    }

    public int getStaticFieldAccessCount() {
      return staticFieldAccessCount;
    }

    public int getStaticMethodCallCount() {
      return staticMethodCallCount;
    }

    public List<String> getFailures() {
      return failures;
    }
  }
}
