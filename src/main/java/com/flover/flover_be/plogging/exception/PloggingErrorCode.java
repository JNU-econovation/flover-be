package com.flover.flover_be.plogging.exception;

import com.flover.flover_be.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PloggingErrorCode implements ErrorCode {

    PLOGGING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "플로깅 세션을 찾을 수 없습니다."),
    IMAGE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 데이터를 처리하는 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
