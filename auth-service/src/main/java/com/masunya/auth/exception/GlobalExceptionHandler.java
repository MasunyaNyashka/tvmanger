package com.masunya.auth.exception;

import com.masunya.common.exception.BusinessException;
import com.masunya.common.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(
                        ErrorResponse.builder()
                                .message(ex.getMessage())
                                .errorCode(ex.getErrorCode())
                                .status(ex.getStatus())
                                .timestamp(Instant.now())
                                .build()
                );
    }
}
