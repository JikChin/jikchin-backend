package com.jikchin.jikchinbackend.global.error;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
  private final ErrorType errorType;

  public AppException(ErrorType errorType) {
    this.errorType = errorType;
  }
}
