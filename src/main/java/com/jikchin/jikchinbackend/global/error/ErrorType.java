package com.jikchin.jikchinbackend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    INVALID_ACCESS_PATH(HttpStatus.BAD_REQUEST, ErrorCode.E400, "잘못된 접근입니다", LogLevel.WARN);

    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final String message;
    private final LogLevel logLevel;

}
