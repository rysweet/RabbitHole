package org.lgna.ik.poser;

public class CannotCreateExpressionException extends Exception {
  private final Object value;

  public CannotCreateExpressionException(Object value) {
    this.value = value;
  }

  public CannotCreateExpressionException(Object value, Throwable cause) {
    super(cause);
    this.value = value;
  }

  public Object getValue() {
    return this.value;
  }
}
