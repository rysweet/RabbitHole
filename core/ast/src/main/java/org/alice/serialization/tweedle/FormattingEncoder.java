package org.alice.serialization.tweedle;

import org.lgna.project.annotations.FieldTemplate;
import org.lgna.project.ast.Statement;

import java.util.function.Consumer;

/**
 * Companion class that encapsulates formatting and utility helper logic
 * extracted from {@link TweedleEncoder}. Owns the indent state and provides
 * methods for structured argument formatting, list encoding, and indentation.
 * Delegates back to the owning encoder for inherited formatting methods
 * via package-private bridge methods.
 */
class FormattingEncoder {

  private static final String INDENTION = "  ";
  private static final int MAX_CACHED_INDENT = 16;
  private static final String[] INDENT_CACHE = new String[MAX_CACHED_INDENT];
  static {
    INDENT_CACHE[0] = "";
    for (int i = 1; i < MAX_CACHED_INDENT; i++) {
      INDENT_CACHE[i] = INDENT_CACHE[i - 1] + INDENTION;
    }
  }

  private final TweedleEncoder encoder;
  private int indent = 0;

  FormattingEncoder(TweedleEncoder encoder) {
    this.encoder = encoder;
  }

  void appendVisibilityTag(FieldTemplate fieldAnnotation) {
    if (fieldAnnotation == null) {
      return;
    }
    switch (fieldAnnotation.visibility()) {
    case COMPLETELY_HIDDEN:
      encoder.forwardAppendString("@CompletelyHidden ");
      break;
    case PRIME_TIME:
      encoder.forwardAppendString("@PrimeTime ");
      break;
    case TUCKED_AWAY:
      encoder.forwardAppendString("@TuckedAway ");
      break;
    default:
    }
  }

  void appendInstantiation(String className, Runnable args) {
    encoder.forwardAppendString("new ");
    encoder.forwardAppendString(className);
    encoder.forwardParenthesize(args);
  }

  void appendArg(String label, String value) {
    encoder.forwardAppendString(label);
    encoder.forwardAppendString(": ");
    encoder.forwardAppendString(value);
  }

  void appendArg(String label, Runnable value) {
    encoder.forwardAppendString(label);
    encoder.forwardAppendString(": ");
    value.run();
  }

  void appendAnotherArg(String label, String value) {
    encoder.forwardAppendString(encoder.forwardGetListSeparator());
    appendArg(label, value);
  }

  void appendAnotherArg(String label, Runnable value) {
    encoder.forwardAppendString(encoder.forwardGetListSeparator());
    appendArg(label, value);
  }

  <T> void appendList(T[] values, Consumer<T> appendValue, String separator) {
    encoder.forwardAppendChar('{');
    int i = 0;
    while (i < values.length) {
      appendValue.accept(values[i]);
      i++;
      if (i < values.length) {
        encoder.forwardAppendString(separator);
      }
    }
    encoder.forwardAppendChar('}');
  }

  void quoteString(String aString) {
    encoder.forwardAppendChar('\"');
    encoder.forwardAppendString(aString);
    encoder.forwardAppendChar('\"');
  }

  void pushIndent() {
    indent++;
  }

  void popIndent() {
    indent--;
  }

  private static String indentString(int level) {
    if (level <= 0) {
      return "";
    }
    return level < MAX_CACHED_INDENT ? INDENT_CACHE[level] : INDENTION.repeat(level);
  }

  void appendIndent() {
    encoder.forwardAppendString(indentString(indent));
  }

  void appendIndent(Statement stmt) {
    final int level = stmt.isEnabled.getValue() ? this.indent : this.indent - 1;
    encoder.forwardAppendString(indentString(level));
  }
}
