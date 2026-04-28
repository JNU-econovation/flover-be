package com.flover.flover_be.user.exception;

import com.flover.flover_be.global.exception.BusinessException;
import com.flover.flover_be.global.exception.ErrorCode;

public class KakaoAuthException extends BusinessException {

    public KakaoAuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
