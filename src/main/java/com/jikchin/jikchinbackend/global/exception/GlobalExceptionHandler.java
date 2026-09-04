package com.jikchin.jikchinbackend.global.exception;

import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
    return ResponseEntity.status(exception.getErrorType().getStatus())
        .body(ApiResponse.error(exception.getErrorType()));
  }
}
