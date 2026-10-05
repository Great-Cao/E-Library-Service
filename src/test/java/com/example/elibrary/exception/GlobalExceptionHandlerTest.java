package com.example.elibrary.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void busyDatabaseIsReportedAsServiceUnavailable() {
        ResponseEntity<ErrorResponse> response = handler.handleDataAccess(
                new CannotAcquireLockException("SQLITE_BUSY: database is locked"));

        assertServiceUnavailable(response);
    }

    @Test
    void lockedDatabaseIsReportedAsServiceUnavailable() {
        // SQLITE_LOCKED reports "database table is locked", which the BUSY pattern misses.
        ResponseEntity<ErrorResponse> response = handler.handleDataAccess(
                new CannotAcquireLockException("SQLITE_LOCKED: database table is locked"));

        assertServiceUnavailable(response);
    }

    @Test
    void unrelatedDataAccessFailureIsReportedAsInternalError() {
        ResponseEntity<ErrorResponse> response = handler.handleDataAccess(
                new DataAccessResourceFailureException("syntax error in generated SQL"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().error().code());
    }

    private void assertServiceUnavailable(ResponseEntity<ErrorResponse> response) {
        assertEquals(503, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().error().code());
    }
}
