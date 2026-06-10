package org.alice.ide.coverage;

import org.alice.ide.testing.TestIdeBootstrap;
import org.alice.math.immutable.Point3;
import org.alice.stageide.modelresource.ClassResourceKey;
import org.alice.stageide.modelresource.ResourceKey;
import org.lgna.common.resources.AudioResource;
import org.lgna.croquet.DropSite;
import org.lgna.croquet.SingleSelectTreeState;
import org.lgna.croquet.Triggerable;
import org.lgna.croquet.history.DragStep;
import org.lgna.croquet.icon.IconFactory;
import org.lgna.project.Project;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractParameter;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.InstanceCreation;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserField;
import org.lgna.story.SGround;
import org.lgna.story.SThingMarker;
import org.lgna.story.SMovableTurnable;
import org.lgna.story.implementation.GroundImp;
import org.lgna.story.implementation.ModelImp;
import org.lgna.story.implementation.SceneImp;
import org.lgna.story.resources.FishResource;
import org.junit.Assert;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class HeadlessClassExerciseSupport {
  private static final Object UNSUPPORTED = new Object();
  private static final int MAX_RECURSION_DEPTH = 2;

  private HeadlessClassExerciseSupport() {
  }

  static SmokeStats exercise(String... classNames) {
    return exercise(Arrays.asList(classNames));
  }

  static SmokeStats exercise(Collection<String> classNames) {
    SmokeStats stats = new SmokeStats();
    for (String className : new LinkedHashSet<>(classNames)) {
      exerciseClass(className, stats);
    }
    return stats;
  }

  static SmokeStats exerciseSweepTargets() {
    List<String> targets = new ArrayList<>(loadTargetTopLevels());
    Collections.sort(targets);
    SmokeStats stats = exercise(targets);
    stats.setDiscoveredTargetCount(targets.size());
    return stats;
  }

  private static volatile List<String> cachedTargets;

  private static Set<String> loadTargetTopLevels() {
    List<String> targets = cachedTargets;
    if (targets == null) {
      try {
        Path path = Paths.get(Objects.requireNonNull(
            HeadlessClassExerciseSupport.class.getResource("/org/alice/ide/coverage/small-class-targets.txt")).toURI());
        targets = Files.readAllLines(path).stream()
            .filter(line -> !line.isBlank() && !line.startsWith("#"))
            .collect(Collectors.toList());
        cachedTargets = targets;
      } catch (Exception exception) {
        throw new AssertionError(exception);
      }
    }
    return new LinkedHashSet<>(targets);
  }

  private static void exerciseClass(String className, SmokeStats stats) {
    if (!isHeadlessFriendlyName(className)) {
      return;
    }
    try {
      Class<?> clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
      stats.loaded.add(className);
      if (!isHeadlessFriendly(clazz)) {
        return;
      }
      invokeStaticMethods(clazz);
      Object instance = instantiate(clazz, 0);
      if (instance != null) {
        stats.instantiated.add(className);
        exerciseInstance(clazz, instance);
      }
    } catch (Throwable throwable) {
      stats.failures.put(className, unwrap(throwable));
    }
  }

  private static boolean isHeadlessFriendlyName(String name) {
    return !(name.contains(".views.")
        || name.contains(".swing.")
        || name.contains(".capture.")
        || name.contains(".sceneeditor.interact.")
        || name.endsWith("View")
        || name.endsWith("Pane")
        || name.endsWith("Panel")
        || name.endsWith("Frame")
        || name.endsWith("Dialog")
        || name.endsWith("Renderer")
        || name.endsWith("Manipulator")
        || name.endsWith("StencilView")
        || name.endsWith("Composite")
        || name.endsWith("Operation")
        || name.endsWith("Menu")
        || name.endsWith("Cascade")
        || name.endsWith("FillIn")
        || name.endsWith("Wizard")
        || name.contains("MemoryUsage")
        || name.contains("WindowDetector"));
  }

  private static boolean isHeadlessFriendly(Class<?> clazz) {
    return isHeadlessFriendlyName(clazz.getName());
  }

  private static void invokeStaticMethods(Class<?> clazz) {
    Method[] methods = clazz.getDeclaredMethods();
    Arrays.sort(methods, Comparator.comparingInt(Method::getParameterCount));
    for (Method method : methods) {
      if (!Modifier.isStatic(method.getModifiers()) || !isExercisable(method)) {
        continue;
      }
      Object[] args = buildArguments(clazz, method.getParameterTypes(), 0);
      if (args == null) {
        continue;
      }
      try {
        method.setAccessible(true);
        Object value = method.invoke(null, args);
        touch(value);
      } catch (Throwable ignored) {
      }
    }
  }

  // Best-effort denylist for methods that show modal dialogs or have
  // disruptive side effects. This is NOT exhaustive — new dialog-showing
  // methods (e.g. alertUser(), confirmAction()) will bypass it. If sweep
  // tests hang again, take a thread dump, identify the method, and add it
  // here. A structural fix would be a per-method timeout with thread-kill.
  private static final Set<String> BLOCKED_METHOD_NAMES = Set.of(
      "setVisible", "hide", "pack", "toFront", "toBack",
      "dispose", "close", "requestFocus", "requestFocusInWindow",
      "browse", "openInSystemEditor", "launch",
      "getGalleryLocationFromUser");

  private static final String[] BLOCKED_METHOD_PREFIXES = {
      "show", "open", "print", "display"
  };

  private static boolean isExercisable(Method method) {
    if (method.isSynthetic() || Modifier.isNative(method.getModifiers())) {
      return false;
    }
    String name = method.getName();
    if (name.equals("$jacocoInit") || name.equals("main")) {
      return false;
    }
    if (BLOCKED_METHOD_NAMES.contains(name)) {
      return false;
    }
    for (String prefix : BLOCKED_METHOD_PREFIXES) {
      if (name.startsWith(prefix)) {
        return false;
      }
    }
    return true;
  }

  private static Object instantiate(Class<?> clazz, int depth) {
    if (depth > MAX_RECURSION_DEPTH) {
      return null;
    }
    if (clazz.isEnum()) {
      Object[] constants = clazz.getEnumConstants();
      return constants.length > 0 ? constants[0] : null;
    }
    if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isAnnotation()) {
      return null;
    }
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount));
    for (Constructor<?> constructor : constructors) {
      Object[] args = buildArguments(clazz, constructor.getParameterTypes(), depth + 1);
      if (args == null) {
        continue;
      }
      try {
        constructor.setAccessible(true);
        return constructor.newInstance(args);
      } catch (Throwable ignored) {
      }
    }
    return null;
  }

  private static Object[] buildArguments(Class<?> owner, Class<?>[] parameterTypes, int depth) {
    Object[] args = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      Object value = createArgument(owner, parameterTypes[i], depth);
      if (value == UNSUPPORTED) {
        return null;
      }
      args[i] = value;
    }
    return args;
  }

  private static final Map<Class<?>, Object> ARGUMENT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

  private static Object createArgument(Class<?> owner, Class<?> type, int depth) {
    if (type.isPrimitive()) {
      if (type == boolean.class) {
        return false;
      } else if (type == char.class) {
        return '\0';
      } else if (type == byte.class) {
        return (byte) 0;
      } else if (type == short.class) {
        return (short) 0;
      } else if (type == int.class) {
        return 0;
      } else if (type == long.class) {
        return 0L;
      } else if (type == float.class) {
        return 0.0f;
      } else {
        return 0.0d;
      }
    }
    if (type == String.class) {
      return "";
    }
    if (type == Object.class) {
      return new Object();
    }
    if (type == Class.class) {
      return Object.class;
    }
    if (type == File.class) {
      return new File(".");
    }
    if (type == Path.class) {
      return Path.of(".");
    }
    if (type == URI.class) {
      return URI.create("file:.");
    }
    if (type == URL.class) {
      try {
        return URI.create("file:.").toURL();
      } catch (Exception exception) {
        return UNSUPPORTED;
      }
    }
    if (type == UUID.class) {
      return UUID.randomUUID();
    }
    if (type == Locale.class) {
      return Locale.ENGLISH;
    }
    if (type == Optional.class) {
      return Optional.empty();
    }
    if (type.isArray()) {
      return Array.newInstance(type.getComponentType(), 0);
    }
    if (type.isEnum()) {
      Object[] constants = type.getEnumConstants();
      return constants.length > 0 ? constants[0] : null;
    }
    if (Collection.class.isAssignableFrom(type)) {
      return List.of();
    }
    if (Map.class.isAssignableFrom(type)) {
      return Map.of();
    }
    if (type == Project.class) {
      return ARGUMENT_CACHE.computeIfAbsent(type, k -> TestIdeBootstrap.createMinimalProject());
    }
    if (type == SceneImp.class) {
      return ARGUMENT_CACHE.computeIfAbsent(type, k -> new SceneImp(null));
    }
    if (type == GroundImp.class || type == ModelImp.class) {
      return ARGUMENT_CACHE.computeIfAbsent(type, k -> new SGround().getImplementation());
    }
    if (type == SMovableTurnable.class) {
      return ARGUMENT_CACHE.computeIfAbsent(type, k -> new SThingMarker());
    }
    if (type == AudioResource.class) {
      return ARGUMENT_CACHE.computeIfAbsent(type, k -> new AudioResource(UUID.randomUUID()));
    }
    if (type == ResourceKey.class) {
      return ARGUMENT_CACHE.computeIfAbsent(type, k -> new DummyResourceKey());
    }
    if (type == ClassResourceKey.class) {
      return ARGUMENT_CACHE.computeIfAbsent(type, k -> new ClassResourceKey(FishResource.class));
    }
    if (type == BlockStatement.class) {
      return new BlockStatement();
    }
    if (type == Statement.class) {
      return AstUtilities.createMethodInvocationStatement(new NullLiteral(), null);
    }
    if (type == Expression.class) {
      return new NullLiteral();
    }
    if (type == AbstractDeclaration.class) {
      return new UserField("target", JavaType.getInstance(SThingMarker.class));
    }
    if (type == AbstractType.class) {
      return JavaType.getInstance(SThingMarker.class);
    }
    if (type == NamedUserType.class) {
      return AstUtilities.createType("CoverageType", JavaType.getInstance(SThingMarker.class));
    }
    if (type == AbstractParameter.class) {
      return new org.lgna.project.ast.UserParameter("value", JavaType.getInstance(String.class));
    }
    if (type == Point3.class) {
      return Point3.ORIGIN;
    }
    if (type == DropSite.class || type == DragStep.class || type.getName().contains("SearchTabView") || type.getName().contains("StandardExpressionState")) {
      return null;
    }
    if (type.isInterface()) {
      InvocationHandler handler = (proxy, method, args) -> defaultReturnValue(method.getReturnType());
      return java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }
    Object nested = instantiate(type, depth + 1);
    return nested != null ? nested : UNSUPPORTED;
  }

  private static Object defaultReturnValue(Class<?> returnType) {
    if (returnType == void.class) {
      return null;
    }
    if (returnType.isPrimitive()) {
      if (returnType == boolean.class) {
        return false;
      } else if (returnType == char.class) {
        return '\0';
      } else if (returnType == byte.class) {
        return (byte) 0;
      } else if (returnType == short.class) {
        return (short) 0;
      } else if (returnType == int.class) {
        return 0;
      } else if (returnType == long.class) {
        return 0L;
      } else if (returnType == float.class) {
        return 0.0f;
      } else {
        return 0.0d;
      }
    }
    return null;
  }

  private static void exerciseInstance(Class<?> clazz, Object instance) {
    touch(instance.toString());
    touch(instance.hashCode());
    touch(instance.equals(instance));
    Method[] methods = clazz.getDeclaredMethods();
    Arrays.sort(methods, Comparator.comparingInt(Method::getParameterCount));
    for (Method method : methods) {
      if (Modifier.isStatic(method.getModifiers()) || !isExercisable(method)) {
        continue;
      }
      Object[] args = buildArguments(clazz, method.getParameterTypes(), 0);
      if (args == null) {
        continue;
      }
      try {
        method.setAccessible(true);
        touch(method.invoke(instance, args));
      } catch (Throwable ignored) {
      }
    }
  }

  private static void touch(Object value) {
    if (value == null) {
      return;
    }
    value.toString();
    value.hashCode();
  }

  private static Throwable unwrap(Throwable throwable) {
    if (throwable instanceof InvocationTargetException ite && ite.getTargetException() != null) {
      return unwrap(ite.getTargetException());
    }
    return throwable;
  }

  static final class SmokeStats {
    private final Set<String> loaded = new LinkedHashSet<>();
    private final Set<String> instantiated = new LinkedHashSet<>();
    private final Map<String, Throwable> failures = new LinkedHashMap<>();
    private int discoveredTargetCount;

    void assertLoaded(String... classNames) {
      for (String className : classNames) {
        Assert.assertTrue("Expected to load " + className + " but failures were " + failures.keySet(), loaded.contains(className));
      }
    }

    int getLoadedCount() {
      return loaded.size();
    }

    int getInstantiatedCount() {
      return instantiated.size();
    }

    int getDiscoveredTargetCount() {
      return discoveredTargetCount;
    }

    int getFailureCount() {
      return failures.size();
    }

    void setDiscoveredTargetCount(int discoveredTargetCount) {
      this.discoveredTargetCount = discoveredTargetCount;
    }
  }

  private static final class DummyResourceKey extends ResourceKey {
    @Override
    public String getSearchText() {
      return "dummy search";
    }

    @Override
    public String getInternalName() {
      return "Dummy";
    }

    @Override
    public String getLocalizedName() {
      return "Dummy";
    }

    @Override
    public String getLocalizedCreationText() {
      return "new Dummy";
    }

    @Override
    public IconFactory getIconFactory() {
      return null;
    }

    @Override
    public boolean isLeaf() {
      return true;
    }

    @Override
    protected void appendRep(StringBuilder sb) {
      sb.append("dummy");
    }

    @Override
    public InstanceCreation createInstanceCreation(Set<NamedUserType> typeCache) {
      return null;
    }

    @Override
    public String[] getTags() {
      return new String[] {"dummy"};
    }

    @Override
    public String[] getGroupTags() {
      return new String[] {"group"};
    }

    @Override
    public String[] getThemeTags() {
      return new String[] {"theme"};
    }

    @Override
    public boolean isInstanceCreator() {
      return false;
    }

    @Override
    public Triggerable getLeftClickOperation(org.alice.stageide.modelresource.ResourceNode node, SingleSelectTreeState<org.alice.stageide.modelresource.ResourceNode> controller) {
      return null;
    }

    @Override
    public Triggerable getDropOperation(org.alice.stageide.modelresource.ResourceNode node, DragStep step, DropSite dropSite) {
      return null;
    }
  }
}
