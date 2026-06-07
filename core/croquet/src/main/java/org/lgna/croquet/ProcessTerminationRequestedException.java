package org.lgna.croquet;

public class ProcessTerminationRequestedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final int status;

  public ProcessTerminationRequestedException(int status) {
    super("Process termination requested with status " + status);
    this.status = status;
  }

  public int getStatus() {
    return this.status;
  }
}
