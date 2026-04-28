package com.flover.flover_be.user.exception;

import com.flover.flover_be.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    KAKAO_TOKEN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 토큰 발급에 실패했습니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.UNAUTHORIZED, "카카오 사용자 정보 조회에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
