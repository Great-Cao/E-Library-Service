package com.example.elibrary.exception;

import org.springframework.http.HttpStatus;

/**
 * The single source of truth for API error codes, their HTTP status and the
 * default message. Responses always use this code + status pairing.
 */
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "The request parameter or header is invalid."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "Book not found."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found."),
    LOAN_NOT_FOUND(HttpStatus.NOT_FOUND, "Loan not found."),
    BOOK_UNAVAILABLE(HttpStatus.CONFLICT, "No copies are currently available."),
    LOAN_ALREADY_RETURNED(HttpStatus.CONFLICT, "The loan has already been returned."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "The loan belongs to another user."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource does not exist."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "The HTTP method is not supported for this resource."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The service is temporarily unavailable. Please retry later."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
