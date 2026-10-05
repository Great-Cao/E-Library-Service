package com.example.elibrary.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Locale;

/**
 * Translates every exception into the documented error envelope. Nothing about
 * the internals (stack traces, SQL, file paths) is ever leaked to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorCode code = ex.errorCode();
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, ex.getMessage(), ex.details()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        List<ErrorResponse.FieldViolation> details = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ErrorResponse.FieldViolation(
                                String.valueOf(result.getMethodParameter().getParameterName()),
                                error.getDefaultMessage())))
                .toList();
        return validationError(details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponse.FieldViolation> details = ex.getConstraintViolations().stream()
                .map(this::toFieldViolation)
                .toList();
        return validationError(details);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldViolation> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return validationError(details);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        List<ErrorResponse.FieldViolation> details = List.of(
                new ErrorResponse.FieldViolation(field(ex), reason(ex)));
        return validationError(details);
    }

    /**
     * SQLite reports write contention as "database is locked" / SQLITE_BUSY. That
     * is a transient infrastructure problem, never a business conflict, so it maps
     * to 503 and must not be confused with an empty shelf (409 BOOK_UNAVAILABLE).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        if (isDatabaseBusy(ex)) {
            log.warn("SQLite reported write contention: {}", rootMessage(ex));
            ErrorCode code = ErrorCode.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(code.status())
                    .body(ErrorResponse.of(code, code.defaultMessage(), List.of()));
        }
        log.error("Unexpected data access failure", ex);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, code.defaultMessage(), List.of()));
    }

    /**
     * Unmatched routes and unsupported methods are ordinary client mistakes, not
     * server faults, so they must not fall through to the 500 handler.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        ErrorCode code = ErrorCode.NOT_FOUND;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, code.defaultMessage(), List.of()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorCode code = ErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, code.defaultMessage(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, code.defaultMessage(), List.of()));
    }

    private ResponseEntity<ErrorResponse> validationError(List<ErrorResponse.FieldViolation> details) {
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        String message = details.isEmpty()
                ? code.defaultMessage()
                : details.get(0).reason();
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, message, details));
    }

    private ErrorResponse.FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return new ErrorResponse.FieldViolation(field, violation.getMessage());
    }

    private String field(Exception ex) {
        if (ex instanceof MissingServletRequestParameterException missing) {
            return missing.getParameterName();
        }
        if (ex instanceof MissingPathVariableException missing) {
            return missing.getVariableName();
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return mismatch.getName();
        }
        return "request";
    }

    /**
     * Builds a client-facing reason without echoing the framework message, which
     * would leak internal Java type names and the raw input value.
     */
    private String reason(Exception ex) {
        if (ex instanceof MissingServletRequestParameterException) {
            return "is required";
        }
        if (ex instanceof MissingPathVariableException) {
            return "must not be blank";
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            Class<?> requiredType = mismatch.getRequiredType();
            return "must be a valid " + (requiredType == null ? "value" : requiredType.getSimpleName());
        }
        return "is malformed";
    }

    private boolean isDatabaseBusy(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof PessimisticLockingFailureException
                    || current instanceof CannotGetJdbcConnectionException
                    || current instanceof QueryTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            String normalized = message.toUpperCase(Locale.ROOT);
            if (normalized.contains("SQLITE_BUSY")
                    || normalized.contains("SQLITE_LOCKED")
                    || normalized.contains("DATABASE IS LOCKED")
                    || normalized.contains("DATABASE TABLE IS LOCKED")) {
                return true;
            }
        }
        return false;
    }

    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
