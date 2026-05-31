package com.plover.plover_be.user.exception;

import com.plover.plover_be.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    KAKAO_TOKEN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 토큰 발급에 실패했습니다."),
    KAKAO_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "카카오 액세스 토큰이 유효하지 않거나 만료되었습니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.UNAUTHORIZED, "카카오 사용자 정보 조회에 실패했습니다."),

    APPLE_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Apple 토큰 검증에 실패했습니다."),
    APPLE_CLAIMS_INVALID(HttpStatus.UNAUTHORIZED, "Apple 토큰의 정보가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
