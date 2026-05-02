package com.flover.flover_be.user.exception;

import com.flover.flover_be.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_PLOGGING_TIME(HttpStatus.BAD_REQUEST, "플로깅 시간은 양수여야 합니다.");

    private final HttpStatus status;
    private final String message;
}
