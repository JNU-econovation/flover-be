package com.flover.flover_be.plogging.exception;

import com.flover.flover_be.global.exception.BusinessException;
import com.flover.flover_be.global.exception.ErrorCode;

public class PloggingException extends BusinessException {

    public PloggingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
