package com.jikchin.jikchinbackend.global.error;

import lombok.Getter;

@Getter
public class ErrorMessage {
  private final String errorCode;
  private final String message;
  private final Object errorData;

  public ErrorMessage(ErrorType errorType, Object errorData) {
    this.errorCode = errorType.getErrorCode().name();
    this.message = errorType.getMessage();
    this.errorData = errorData;
  }
}
