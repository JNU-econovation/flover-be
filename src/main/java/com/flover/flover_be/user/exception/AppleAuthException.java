package com.flover.flover_be.user.exception;

import com.flover.flover_be.global.exception.ErrorCode;
import com.flover.flover_be.global.exception.BusinessException;

public class AppleAuthException extends BusinessException {

    public AppleAuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
