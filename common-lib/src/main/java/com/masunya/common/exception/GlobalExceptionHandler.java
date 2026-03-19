package com.masunya.common.exception;

import com.masunya.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        // Возвращаем бизнес-ошибки в предсказуемом формате для UI.
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Берем первую валидационную ошибку как основное сообщение для клиента.
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Validation error"
                : ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.builder()
                                .message(message)
                                .errorCode("VALIDATION_ERROR")
                                .status(HttpStatus.BAD_REQUEST.value())
                                .timestamp(Instant.now())
                                .build()
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        // Фолбэк на неожиданные ошибки, чтобы не отдавать stacktrace наружу.
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.builder()
                                .message("Внутренняя ошибка сервера")
                                .errorCode("INTERNAL_ERROR")
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .timestamp(Instant.now())
                                .build()
                );
    }
}
