package com.jikchin.jikchinbackend.global.response;

import com.jikchin.jikchinbackend.global.error.ErrorMessage;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import lombok.Getter;

@Getter
public class ApiResponse<T> {
  private final ResultType resultType;
  private final T data;
  private ErrorMessage error;

  private ApiResponse(ResultType resultType, T data, ErrorMessage error) {
    this.resultType = resultType;
    this.data = data;
    this.error = error;
  }

  public static <S> ApiResponse<S> success(S data) {
    return new ApiResponse<>(ResultType.SUCCESS, data, null);
  }

  public static <S> ApiResponse<S> success() {
    return new ApiResponse<>(ResultType.SUCCESS, null, null);
  }

  public static ApiResponse<Void> error(ErrorType errorType, Object errorData) {
    return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(errorType, errorData));
  }

  public static ApiResponse<Void> error(ErrorType errorType) {
    return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(errorType, null));
  }
}
