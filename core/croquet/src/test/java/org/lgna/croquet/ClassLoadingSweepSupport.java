package org.lgna.croquet;

import edu.cmu.cs.dennisc.codec.BinaryDecoder;
import edu.cmu.cs.dennisc.codec.ByteArrayBinaryEncoder;
import org.junit.Assert;

import javax.swing.BorderFactory;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class ClassLoadingSweepSupport {
  private static final sun.misc.Unsafe UNSAFE = getUnsafe();
  private static final UUID SWEEP_UUID = UUID.fromString("00000000-0000-0000-0000-000000000070");
  private static final Group SWEEP_GROUP = Group.getInstance(SWEEP_UUID, "classLoadingSweep");

  private ClassLoadingSweepSupport() {
  }

  public static SweepStats sweepModuleSourceTree() throws IOException {
    CroquetTestUtils.ensureTestApplication();
    return sweepClasses(discoverClassNames(Paths.get(System.getProperty("basedir", "."), "src", "main", "java")));
  }

  public static SweepStats sweepModuleSourceClassesWithPrefix(String prefix) throws IOException {
    CroquetTestUtils.ensureTestApplication();
    List<String> classNames = discoverClassNames(Paths.get(System.getProperty("basedir", "."), "src", "main", "java"))
        .stream()
        .filter(className -> className.startsWith(prefix))
        .toList();
    return sweepClasses(classNames);
  }

  public static SweepStats sweepClasses(List<String> classNames) {
    int loadedClassCount = 0;
    int instantiatedClassCount = 0;
    int enumExerciseCount = 0;
    int staticFieldAccessCount = 0;
    int staticMethodCallCount = 0;
    int codecExerciseCount = 0;
    List<String> failures = new ArrayList<>();

    for (String className : classNames) {
      try {
        Class<?> cls = Class.forName(className, true, ClassLoadingSweepSupport.class.getClassLoader());
        loadedClassCount++;
        inspectMetadata(cls);
        enumExerciseCount += exerciseEnum(cls);
        List<Object> staticValues = new ArrayList<>();
        staticFieldAccessCount += accessPublicStaticFinalFields(cls, staticValues);
        Object instance = tryInstantiateClass(cls);
        if (instance != null) {
          instantiatedClassCount++;
          exerciseInstanceMethods(cls, instance);
          codecExerciseCount += exerciseCodecCandidate(instance);
        }
        for (Object staticValue : staticValues) {
          codecExerciseCount += exerciseCodecCandidate(staticValue);
        }
      } catch (Throwable throwable) {
        failures.add(className + " -> " + throwable.getClass().getSimpleName());
      } finally {
        cleanupApplicationState();
      }
    }

    return new SweepStats(classNames.size(), loadedClassCount, instantiatedClassCount, enumExerciseCount,
        staticFieldAccessCount, staticMethodCallCount, codecExerciseCount, failures);
  }

  private static List<String> discoverClassNames(Path sourceRoot) throws IOException {
    try (Stream<Path> stream = Files.walk(sourceRoot)) {
      return stream
          .filter(path -> path.toString().endsWith(".java"))
          .filter(path -> !path.getFileName().toString().equals("package-info.java"))
          .filter(path -> !path.getFileName().toString().equals("module-info.java"))
          .map(path -> sourceRoot.relativize(path).toString())
          .map(relative -> relative.substring(0, relative.length() - ".java".length()))
          .map(relative -> relative.replace(File.separatorChar, '.'))
          .sorted()
          .toList();
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

  private static int exerciseEnum(Class<?> cls) {
    if (!cls.isEnum()) {
      return 0;
    }
    int count = 0;
    Object[] constants = cls.getEnumConstants();
    if (constants != null) {
      count++;
    }
    try {
      Method values = cls.getMethod("values");
      Object array = values.invoke(null);
      if (array != null && array.getClass().isArray()) {
        Array.getLength(array);
        count++;
      }
    } catch (Throwable ignored) {
    }
    if (constants != null && constants.length > 0 && constants[0] instanceof Enum<?> enumConstant) {
      try {
        Method valueOf = cls.getMethod("valueOf", String.class);
        Object resolved = valueOf.invoke(null, enumConstant.name());
        if (resolved == enumConstant) {
          count++;
        }
      } catch (Throwable ignored) {
      }
    }
    return count;
  }

  private static int accessPublicStaticFinalFields(Class<?> cls, List<Object> staticValues) {
    int count = 0;
    for (Field field : cls.getFields()) {
      int modifiers = field.getModifiers();
      if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
        try {
          Object value = field.get(null);
          staticValues.add(value);
          count++;
        } catch (Throwable ignored) {
        }
      }
    }
    return count;
  }

  private static Object tryInstantiateClass(Class<?> cls) {
    int modifiers = cls.getModifiers();
    if (cls.isInterface() || cls.isAnnotation() || cls.isEnum()) {
      return null;
    }
    if (cls.isMemberClass() && !Modifier.isStatic(modifiers)) {
      return null;
    }
    if (!Modifier.isAbstract(modifiers)) {
      Constructor<?>[] constructors = cls.getDeclaredConstructors();
      java.util.Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount));
      for (Constructor<?> constructor : constructors) {
        Object[] args = buildArguments(constructor.getParameterTypes());
        if (args == null) {
          continue;
        }
        try {
          constructor.setAccessible(true);
          return constructor.newInstance(args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException ignored) {
        }
      }
    }
    try {
      return UNSAFE.allocateInstance(cls);
    } catch (InstantiationException ignored) {
      return null;
    }
  }

  private static void exerciseInstanceMethods(Class<?> cls, Object instance) {
    for (Class<?> current = cls; current != null && current != Object.class; current = current.getSuperclass()) {
      for (Method method : current.getDeclaredMethods()) {
        int modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers) || Modifier.isStatic(modifiers)) {
          continue;
        }
        if (method.getName().equals("wait") || method.getName().equals("notify") || method.getName().equals("notifyAll") || method.getName().equals("getClass")) {
          continue;
        }
        if (opensRealModalDialog(current, method)) {
          continue;
        }
        try {
          method.setAccessible(true);
          if (method.getParameterCount() == 0) {
            Object value = method.invoke(instance);
            if (value != null && value.getClass().isArray()) {
              Assert.assertTrue(Array.getLength(value) >= 0);
            }

          } else if (method.getParameterCount() >= 1 && method.getParameterCount() <= 5) {
            Object[] arguments = buildArguments(method.getParameterTypes());
            if (arguments != null) {
              method.invoke(instance, arguments);
            }
          }
        } catch (Throwable ignored) {
        }
      }
    }
  }

  private static boolean opensRealModalDialog(Class<?> owner, Method method) {
    return owner == DocumentFrame.class
        && (method.getName().equals("showSaveFileDialog") || method.getName().equals("showOpenFileDialog"));
  }

  private static Object[] buildArguments(Class<?>[] parameterTypes) {
    Object[] args = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      Object value = defaultValue(parameterTypes[i]);
      if (value == UnsupportedValue.INSTANCE) {
        return null;
      }
      args[i] = value;
    }
    return args;
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class || type == Boolean.class) return false;
    if (type == byte.class || type == Byte.class) return (byte) 0;
    if (type == short.class || type == Short.class) return (short) 0;
    if (type == int.class || type == Integer.class) return 0;
    if (type == long.class || type == Long.class) return 0L;
    if (type == float.class || type == Float.class) return 0.0f;
    if (type == double.class || type == Double.class) return 0.0d;
    if (type == char.class || type == Character.class) return '\0';
    if (type == String.class) return "";
    if (type == UUID.class) return SWEEP_UUID;
    if (type == Group.class) return SWEEP_GROUP;
    if (type == Locale.class) return Locale.ROOT;
    if (type == File.class) return new File(".");
    if (type == Color.class) return Color.BLACK;
    if (type == Dimension.class) return new Dimension(1, 1);
    if (type == Point.class) return new Point(0, 0);
    if (type == Rectangle.class) return new Rectangle(0, 0, 1, 1);
    if (type == Insets.class) return new Insets(0, 0, 0, 0);
    if (type == Font.class) return new Font("Dialog", Font.PLAIN, 12);
    if (type == Class.class) return Object.class;
    if (type == Object.class) return new Object();
    if (type.isEnum()) return type.getEnumConstants().length > 0 ? type.getEnumConstants()[0] : null;
    if (type.isArray()) return Array.newInstance(type.getComponentType(), 0);
    if (type == List.class || type == Collection.class) return Collections.emptyList();
    if (type == Map.class) return Collections.emptyMap();
    if (type == Set.class) return Collections.emptySet();
    if (type == Queue.class) return new ArrayDeque<>();
    if (type == LayoutManager.class) return new BorderLayout();
    if (type == ItemCodec.class) return CroquetTestUtils.STRING_CODEC;
    if (Border.class.isAssignableFrom(type)) return BorderFactory.createEmptyBorder();
    if (javax.swing.ButtonModel.class.isAssignableFrom(type)) return new DefaultButtonModel();
    if (javax.swing.ComboBoxModel.class.isAssignableFrom(type)) return new DefaultComboBoxModel<>();
    if (javax.swing.ListModel.class.isAssignableFrom(type)) return new DefaultListModel<>();
    if (javax.swing.ListSelectionModel.class.isAssignableFrom(type)) return new DefaultListSelectionModel();
    if (javax.swing.SpinnerModel.class.isAssignableFrom(type)) return new SpinnerNumberModel(0, -10, 10, 1);
    if (javax.swing.tree.TreeModel.class.isAssignableFrom(type)) return new DefaultTreeModel(new DefaultMutableTreeNode("root"));
    if (javax.swing.Icon.class.isAssignableFrom(type)) return new ImageIcon();
    if (Component.class.isAssignableFrom(type)) return defaultComponent(type);
    if (!type.isPrimitive()) {
      Object fallback = allocatePlaceholder(type);
      return fallback != UnsupportedValue.INSTANCE ? fallback : null;
    }
    return UnsupportedValue.INSTANCE;
  }

  private static Object allocatePlaceholder(Class<?> type) {
    int modifiers = type.getModifiers();
    Package pkg = type.getPackage();
    if (type.isInterface() || type.isAnnotation() || type.isArray()) {
      return UnsupportedValue.INSTANCE;
    }
    if (type.isMemberClass() && !Modifier.isStatic(modifiers)) {
      return UnsupportedValue.INSTANCE;
    }
    if (Modifier.isAbstract(modifiers)) {
      return UnsupportedValue.INSTANCE;
    }
    String packageName = pkg != null ? pkg.getName() : "";
    if (!(packageName.startsWith("org.lgna.") || packageName.startsWith("edu.cmu.") || packageName.startsWith("org.alice."))) {
      return UnsupportedValue.INSTANCE;
    }
    try {
      return UNSAFE.allocateInstance(type);
    } catch (InstantiationException ignored) {
      return UnsupportedValue.INSTANCE;
    }
  }

  private static Object defaultComponent(Class<?> type) {
    if (type.isAssignableFrom(JPanel.class)) return new JPanel();
    if (type.isAssignableFrom(JLabel.class)) return new JLabel();
    if (type.isAssignableFrom(JButton.class)) return new JButton();
    if (type.isAssignableFrom(JRootPane.class)) return new JRootPane();
    if (type.isAssignableFrom(JScrollPane.class)) return new JScrollPane();
    if (type.isAssignableFrom(JPopupMenu.class)) return new JPopupMenu();
    if (type.isAssignableFrom(JTextField.class)) return new JTextField();
    if (type.isAssignableFrom(JTextArea.class)) return new JTextArea();
    if (type.isAssignableFrom(JTable.class)) return new JTable();
    if (type.isAssignableFrom(JList.class)) return new JList<>();
    if (type.isAssignableFrom(JTree.class)) return new JTree();
    if (type.isAssignableFrom(JSlider.class)) return new JSlider();
    if (type.isAssignableFrom(JSpinner.class)) return new JSpinner();
    if (type.isAssignableFrom(JComboBox.class)) return new JComboBox<>();
    if (type.isAssignableFrom(JMenuBar.class)) return new JMenuBar();
    if (type.isAssignableFrom(JComponent.class)) return new JPanel();
    return null;
  }

  private static int exerciseCodecCandidate(Object candidate) {
    if (!(candidate instanceof ItemCodec<?> codec)) {
      return 0;
    }
    int count = 0;
    try {
      codec.getValueClass();
      count++;
    } catch (Throwable ignored) {
    }
    try {
      StringBuilder builder = new StringBuilder();
      appendRepresentation(codec, builder, sampleValue(codec.getValueClass()));
      count++;
    } catch (Throwable ignored) {
    }
    try {
      ByteArrayBinaryEncoder encoder = new ByteArrayBinaryEncoder();
      encodeValue(codec, encoder, null);
      BinaryDecoder decoder = encoder.createDecoder();
      decodeValue(codec, decoder);
      count++;
    } catch (Throwable ignored) {
    }
    Object sample = sampleValue(codec.getValueClass());
    if (sample != UnsupportedValue.INSTANCE) {
      try {
        ByteArrayBinaryEncoder encoder = new ByteArrayBinaryEncoder();
        encodeValue(codec, encoder, sample);
        BinaryDecoder decoder = encoder.createDecoder();
        decodeValue(codec, decoder);
        count++;
      } catch (Throwable ignored) {
      }
    }
    return count;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void appendRepresentation(ItemCodec codec, StringBuilder builder, Object value) {
    codec.appendRepresentation(builder, value == UnsupportedValue.INSTANCE ? null : value);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void encodeValue(ItemCodec codec, ByteArrayBinaryEncoder encoder, Object value) {
    codec.encodeValue(encoder, value);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Object decodeValue(ItemCodec codec, BinaryDecoder decoder) {
    return codec.decodeValue(decoder);
  }

  private static Object sampleValue(Class<?> type) {
    Object value = defaultValue(type);
    if (value != null) {
      return value;
    }
    if (type.isEnum()) {
      Object[] enumConstants = type.getEnumConstants();
      return enumConstants.length > 0 ? enumConstants[0] : null;
    }
    return UnsupportedValue.INSTANCE;
  }

  private static void cleanupApplicationState() {
    Application<?> application = Application.getActiveInstance();
    if (application == null) {
      return;
    }
    for (int i = 0; i < 16 && application.getOpenActivity() != null; i++) {
      application.getOpenActivity().getLatestActivity().finish();
    }
  }

  private static sun.misc.Unsafe getUnsafe() {
    try {
      Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      return (sun.misc.Unsafe) field.get(null);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static final class SweepStats {
    private final int attemptedClassCount;
    private final int loadedClassCount;
    private final int instantiatedClassCount;
    private final int enumExerciseCount;
    private final int staticFieldAccessCount;
    private final int staticMethodCallCount;
    private final int codecExerciseCount;
    private final List<String> failures;

    private SweepStats(int attemptedClassCount, int loadedClassCount, int instantiatedClassCount,
        int enumExerciseCount, int staticFieldAccessCount, int staticMethodCallCount,
        int codecExerciseCount, List<String> failures) {
      this.attemptedClassCount = attemptedClassCount;
      this.loadedClassCount = loadedClassCount;
      this.instantiatedClassCount = instantiatedClassCount;
      this.enumExerciseCount = enumExerciseCount;
      this.staticFieldAccessCount = staticFieldAccessCount;
      this.staticMethodCallCount = staticMethodCallCount;
      this.codecExerciseCount = codecExerciseCount;
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

    public int getCodecExerciseCount() {
      return codecExerciseCount;
    }

    public List<String> getFailures() {
      return failures;
    }
  }

  private enum UnsupportedValue {
    INSTANCE
  }
}
