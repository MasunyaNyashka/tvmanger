package com.masunya.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final int status;

    // Пустой конструктор — дефолтные значения
    public BusinessException() {
        super("Внутренняя ошибка сервера");
        this.errorCode = "INTERNAL_ERROR";
        this.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    // Только сообщение
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.status = HttpStatus.BAD_REQUEST.value();
    }

    // Сообщение + статус
    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.errorCode = status.name();
        this.status = status.value();
    }

    // Сообщение + код ошибки + статус
    public BusinessException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status.value();
    }
}
