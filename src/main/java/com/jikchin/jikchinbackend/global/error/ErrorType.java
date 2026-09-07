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
  DUPLICATE_MEMBER(HttpStatus.CONFLICT, ErrorCode.E1009, "이미 사용 중인 회원 정보입니다.", LogLevel.WARN),
  REVIEW_SELF_NOT_ALLOWED(
      HttpStatus.BAD_REQUEST, ErrorCode.E2001, "자기 자신은 평가할 수 없습니다.", LogLevel.WARN),
  REVIEW_NOT_MATE_MEMBER(
      HttpStatus.FORBIDDEN, ErrorCode.E2002, "해당 모집글의 확정 멤버만 평가할 수 있습니다.", LogLevel.WARN),
  REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, ErrorCode.E2003, "이미 평가한 멤버입니다.", LogLevel.WARN),
  REVIEW_MATE_POST_NOT_FOUND(
      HttpStatus.NOT_FOUND, ErrorCode.E2004, "모집글을 찾을 수 없습니다.", LogLevel.WARN),
  REPORT_SELF_NOT_ALLOWED(
      HttpStatus.BAD_REQUEST, ErrorCode.E3001, "자기 자신은 신고할 수 없습니다.", LogLevel.WARN),
  REPORT_NOT_MATE_MEMBER(
      HttpStatus.FORBIDDEN, ErrorCode.E3002, "해당 모집글의 확정 멤버만 신고할 수 있습니다.", LogLevel.WARN),
  REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, ErrorCode.E3003, "이미 신고한 건입니다.", LogLevel.WARN),
  REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E3004, "신고 내역을 찾을 수 없습니다.", LogLevel.WARN),
  REPORT_ALREADY_PROCESSED(HttpStatus.CONFLICT, ErrorCode.E3005, "이미 처리된 신고입니다.", LogLevel.WARN),
  REPORT_MATE_POST_NOT_FOUND(
      HttpStatus.NOT_FOUND, ErrorCode.E3006, "모집글을 찾을 수 없습니다.", LogLevel.WARN);

  private final HttpStatus status;
  private final ErrorCode errorCode;
  private final String message;
  private final LogLevel logLevel;
}
