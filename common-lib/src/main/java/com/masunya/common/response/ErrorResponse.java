package com.masunya.common.response;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
@Getter
@Builder
public class ErrorResponse {
    private String message;
    private String errorCode;
    private int status;
    private Instant timestamp;
}
