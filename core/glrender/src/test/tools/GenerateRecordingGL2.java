import com.jogamp.opengl.GL2;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Regenerates the GL2 method stubs while preserving RecordingGL2's handwritten
 * recording helpers.
 *
 * Run from the repository root:
 * java -cp $HOME/.m2/repository/org/jogamp/jogl/jogl-all/2.6.0/jogl-all-2.6.0.jar:$HOME/.m2/repository/org/jogamp/gluegen/gluegen-rt/2.6.0/gluegen-rt-2.6.0.jar \
 *   core/glrender/src/test/tools/GenerateRecordingGL2.java \
 *   core/glrender/src/test/java/edu/cmu/cs/dennisc/render/gl/imp/testing/RecordingGL2.java
 */
public final class GenerateRecordingGL2 {
  private static final String GENERATED_MARKER = "  // Generated from com.jogamp.opengl.GL2; run GenerateRecordingGL2 after JOGL upgrades.\n";
  private static final String FIRST_OLD_METHOD = "  @Override public int getBoundBuffer";

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expected the RecordingGL2.java path");
    }
    Path output = Path.of(args[0]);
    String current = Files.readString(output);
    int generatedStart = current.indexOf(GENERATED_MARKER);
    if (generatedStart < 0) {
      generatedStart = current.indexOf(FIRST_OLD_METHOD);
    }
    if (generatedStart < 0) {
      throw new IllegalStateException("Could not find the generated method section");
    }

    StringBuilder source = new StringBuilder(current.substring(0, generatedStart));
    source.append(GENERATED_MARKER);
    for (Method method : abstractMethods().values()) {
      appendMethod(source, method);
    }
    source.append("}\n");
    Files.writeString(output, source);
  }

  private static Map<String, Method> abstractMethods() {
    Method[] methods = GL2.class.getMethods();
    Arrays.sort(methods, Comparator
        .comparing(Method::getName)
        .thenComparing(method -> parameterKey(method.getParameterTypes()))
        .thenComparing(method -> method.getReturnType().getCanonicalName()));

    Map<String, Method> unique = new LinkedHashMap<>();
    for (Method method : methods) {
      if (!Modifier.isAbstract(method.getModifiers())) {
        continue;
      }
      String key = method.getName() + "(" + parameterKey(method.getParameterTypes()) + ")";
      Method previous = unique.get(key);
      if (previous == null || previous.getReturnType().isAssignableFrom(method.getReturnType())) {
        unique.put(key, method);
      }
    }
    return unique;
  }

  private static String parameterKey(Class<?>[] parameterTypes) {
    return Arrays.stream(parameterTypes)
        .map(Class::getCanonicalName)
        .reduce((left, right) -> left + "," + right)
        .orElse("");
  }

  private static void appendMethod(StringBuilder source, Method method) {
    source.append("  @Override public ")
        .append(method.getReturnType().getCanonicalName())
        .append(' ')
        .append(method.getName())
        .append('(');
    Class<?>[] parameterTypes = method.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i > 0) {
        source.append(", ");
      }
      source.append(parameterTypes[i].getCanonicalName()).append(" p").append(i);
    }
    source.append(") {\n    record(\"").append(method.getName()).append("\", new Object[] {");
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i > 0) {
        source.append(", ");
      }
      source.append('p').append(i);
    }
    source.append("});\n");
    appendReturn(source, method);
    source.append("  }\n\n");
  }

  private static void appendReturn(StringBuilder source, Method method) {
    if (method.getName().equals("glGetError") && method.getParameterCount() == 0) {
      source.append("    return this.errors.isEmpty() ? com.jogamp.opengl.GL.GL_NO_ERROR : this.errors.removeFirst();\n");
      return;
    }
    Class<?> type = method.getReturnType();
    if (type == void.class) {
      return;
    }
    if (type == boolean.class) {
      source.append("    return false;\n");
    } else if (type == char.class) {
      source.append("    return '\\0';\n");
    } else if (type == byte.class) {
      source.append("    return (byte) 0;\n");
    } else if (type == short.class) {
      source.append("    return (short) 0;\n");
    } else if (type == int.class) {
      source.append("    return 0;\n");
    } else if (type == long.class) {
      source.append("    return 0L;\n");
    } else if (type == float.class) {
      source.append("    return 0.0f;\n");
    } else if (type == double.class) {
      source.append("    return 0.0;\n");
    } else {
      source.append("    return null;\n");
    }
  }
}
