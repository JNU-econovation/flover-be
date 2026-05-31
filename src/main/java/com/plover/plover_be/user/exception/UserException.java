package com.plover.plover_be.user.exception;

import com.plover.plover_be.global.exception.BusinessException;
import com.plover.plover_be.global.exception.ErrorCode;

public class UserException extends BusinessException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
