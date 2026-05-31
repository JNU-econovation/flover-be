package com.plover.plover_be.plogging.exception;

import com.plover.plover_be.global.exception.BusinessException;
import com.plover.plover_be.global.exception.ErrorCode;

public class PloggingException extends BusinessException {

    public PloggingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
