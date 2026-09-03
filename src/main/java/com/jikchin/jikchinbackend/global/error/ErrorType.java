package com.jikchin.jikchinbackend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
  INVALID_ACCESS_PATH(HttpStatus.BAD_REQUEST, ErrorCode.E400, "잘못된 접근입니다", LogLevel.WARN),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, ErrorCode.E1001, "인증이 필요합니다.", LogLevel.WARN),
  INVALID_ACCESS_TOKEN(
      HttpStatus.UNAUTHORIZED, ErrorCode.E1002, "유효하지 않은 Access Token입니다.", LogLevel.WARN),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, ErrorCode.E1003, "접근 권한이 없습니다.", LogLevel.WARN),
  LOGIN_FAILED(HttpStatus.UNAUTHORIZED, ErrorCode.E1004, "이메일 또는 비밀번호가 일치하지 않습니다.", LogLevel.WARN),
  INVALID_REFRESH_TOKEN(
      HttpStatus.UNAUTHORIZED, ErrorCode.E1005, "유효하지 않은 Refresh Token입니다.", LogLevel.WARN),
  REFRESH_TOKEN_NOT_FOUND(
      HttpStatus.UNAUTHORIZED, ErrorCode.E1006, "이미 사용됐거나 폐기된 Refresh Token입니다.", LogLevel.WARN),
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E1007, "회원을 찾을 수 없습니다.", LogLevel.WARN),
  MEMBER_INACTIVE(HttpStatus.FORBIDDEN, ErrorCode.E1008, "활성 상태의 회원이 아닙니다.", LogLevel.WARN),
  DUPLICATE_MEMBER(HttpStatus.CONFLICT, ErrorCode.E1009, "이미 사용 중인 회원 정보입니다.", LogLevel.WARN);

  private final HttpStatus status;
  private final ErrorCode errorCode;
  private final String message;
  private final LogLevel logLevel;
}
