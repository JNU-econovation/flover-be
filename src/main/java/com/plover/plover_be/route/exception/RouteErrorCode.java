package com.plover.plover_be.route.exception;

import com.plover.plover_be.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RouteErrorCode implements ErrorCode {
    ROUTE_ENGINE_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "라우팅 엔진 서버와의 통신에 실패했습니다."),
    ROUTE_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "라우팅 경로 계산 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
