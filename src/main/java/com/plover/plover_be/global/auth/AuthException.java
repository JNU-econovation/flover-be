package com.plover.plover_be.global.auth;

import com.plover.plover_be.global.exception.BusinessException;
import com.plover.plover_be.global.exception.ErrorCode;

public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
