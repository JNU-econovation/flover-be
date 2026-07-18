package com.plover.plover_be.crew.exception;

import com.plover.plover_be.global.exception.BusinessException;
import com.plover.plover_be.global.exception.ErrorCode;

public class CrewException extends BusinessException {

    public CrewException(ErrorCode errorCode) {
        super(errorCode);
    }
}
