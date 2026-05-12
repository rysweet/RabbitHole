package org.alice.serialization.tweedle;

import org.lgna.project.ast.Statement;

/**
 * Companion class that encapsulates statement-encoding logic extracted from
 * {@link TweedleEncoder}. Delegates back to the owning encoder for
 * super-class calls and protected formatting methods via package-private
 * bridge methods.
 */
class StatementEncoder {

  private final TweedleEncoder encoder;

  StatementEncoder(TweedleEncoder encoder) {
    this.encoder = encoder;
  }

  void appendStatementCompletion(Statement stmt) {
    encoder.superAppendStatementCompletion(stmt);
    appendStatementEnd(stmt);
  }

  void appendStatementCompletion() {
    encoder.superAppendStatementCompletion();
    encoder.forwardAppendNewLine();
  }

  void pushStatementDisabled() {
    encoder.forwardAppendString(TweedleEncoder.NODE_DISABLE);
    encoder.superPushStatementDisabled();
  }

  void appendCodeFlowStatement(Statement stmt, Runnable appender) {
    appender.run();
    appendStatementEnd(stmt);
  }

  private void appendStatementEnd(Statement stmt) {
    if (!stmt.isEnabled.getValue()) {
      encoder.forwardAppendSpace();
      encoder.forwardAppendString(TweedleEncoder.NODE_ENABLE);
    }
    encoder.forwardAppendNewLine();
  }
}
