package com.example.elibrary.exception;

import java.util.List;

/**
 * A business failure that maps directly to an {@link ErrorCode} and HTTP status.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient List<ErrorResponse.FieldViolation> details;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ApiException(ErrorCode errorCode, String message, List<ErrorResponse.FieldViolation> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorResponse.FieldViolation> details() {
        return details;
    }
}
