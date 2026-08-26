package com.fya.credits.exception;

public class DependencyUnavailableException extends RuntimeException {
  public DependencyUnavailableException(String message) {
    super(message);
  }
}
