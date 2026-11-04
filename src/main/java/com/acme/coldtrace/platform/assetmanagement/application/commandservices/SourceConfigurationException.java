package com.acme.coldtrace.platform.assetmanagement.application.commandservices;

public class SourceConfigurationException extends RuntimeException {
  public enum Kind {
    NOT_FOUND,
    CONFLICT,
    INVALID
  }

  private final Kind kind;

  public SourceConfigurationException(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
