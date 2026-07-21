package com.plover.plover_be.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. (jpeg, png, webp, heic, heif, avif)"),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "허용되지 않은 이미지 URL입니다."),
    IMAGE_OBJECT_NOT_FOUND(HttpStatus.BAD_REQUEST, "업로드된 이미지 객체를 찾을 수 없습니다."),
    IMAGE_OBJECT_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지 파일 크기는 10MB를 초과할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
