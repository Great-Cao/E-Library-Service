package com.example.elibrary.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The one and only error envelope returned by the API.
 */
public record ErrorResponse(ApiError error) {

    public static ErrorResponse of(ErrorCode code, String message, List<FieldViolation> details) {
        return new ErrorResponse(new ApiError(code.name(), message, details));
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ApiError(String code, String message, List<FieldViolation> details) {
    }

    public record FieldViolation(String field, String reason) {
    }
}
