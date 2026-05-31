package com.plover.plover_be.user.exception;

import com.plover.plover_be.global.exception.ErrorCode;
import com.plover.plover_be.global.exception.BusinessException;

public class AppleAuthException extends BusinessException {

    public AppleAuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
