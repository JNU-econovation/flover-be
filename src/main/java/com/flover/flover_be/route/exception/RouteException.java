package com.flover.flover_be.route.exception;

import com.flover.flover_be.global.exception.BusinessException;
import com.flover.flover_be.global.exception.ErrorCode;

public class RouteException extends BusinessException {
    public RouteException(ErrorCode errorCode) {
        super(errorCode);
    }
}
