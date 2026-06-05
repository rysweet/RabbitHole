package org.lgna.croquet;

public class ProcessTerminationRequestedException extends RuntimeException {
  private final int status;

  public ProcessTerminationRequestedException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
