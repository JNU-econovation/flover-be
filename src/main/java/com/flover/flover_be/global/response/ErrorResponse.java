package com.flover.flover_be.global.response;

public record ErrorResponse(int status, String message) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message);
    }
}
