package com.flover.flover_be.plogging.exception;

import com.flover.flover_be.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PloggingErrorCode implements ErrorCode {

    PLOGGING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "플로깅 세션을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
