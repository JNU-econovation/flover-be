package com.flover.flover_be.global.auth;

import com.flover.flover_be.global.exception.BusinessException;
import com.flover.flover_be.global.exception.ErrorCode;

public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
