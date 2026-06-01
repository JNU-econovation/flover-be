package com.plover.plover_be.plogging.exception;

import com.plover.plover_be.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PloggingErrorCode implements ErrorCode {

    PLOGGING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "플로깅 세션을 찾을 수 없습니다."),
    IMAGE_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "이미지 파일은 필수입니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지 파일 크기는 5MB를 초과할 수 없습니다."),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않거나 손상된 이미지 형식입니다."),
    IMAGE_PIXEL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지 해상도가 허용 범위를 초과했습니다."),
    IMAGE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 데이터를 처리하는 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
