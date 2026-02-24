package com.masunya.common.exception;

import lombok.Getter;
@Getter
public class BusinessException extends RuntimeException {
    private final int status;
    private final String errorCode;
    public BusinessException(String message, int status) {
        super(message);
        this.status = status;
        this.errorCode = null;
    }
    public BusinessException(String message, String errorCode, int status) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
