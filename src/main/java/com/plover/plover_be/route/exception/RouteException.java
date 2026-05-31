package com.plover.plover_be.route.exception;

import com.plover.plover_be.global.exception.BusinessException;
import com.plover.plover_be.global.exception.ErrorCode;

public class RouteException extends BusinessException {
    public RouteException(ErrorCode errorCode) {
        super(errorCode);
    }
}
