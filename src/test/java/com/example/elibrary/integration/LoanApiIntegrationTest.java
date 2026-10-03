package com.example.elibrary.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanApiIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_HEADER = "X-User-Id";

    @Test
    void borrowCreatesLoanAndDecrementsStock() throws Exception {
        mockMvc.perform(post("/api/v1/books/1/loans").header(USER_HEADER, "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.borrowedAt").isNotEmpty())
                .andExpect(jsonPath("$.returnedAt").value(nullValue()));

        assertEquals(2, availableCopies(1));
        assertEquals(1, openLoanCount(1));
    }

    @Test
    void missingIdentityHeaderReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/books/1/loans"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value(USER_HEADER));
    }

    @Test
    void nonNumericIdentityHeaderReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/books/1/loans").header(USER_HEADER, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void nonPositiveIdentityHeaderReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/books/1/loans").header(USER_HEADER, "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void borrowingUnknownBookReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/books/999/loans").header(USER_HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void borrowingAsUnknownUserReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/books/1/loans").header(USER_HEADER, "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void borrowingUnavailableBookReturns409() throws Exception {
        mockMvc.perform(post("/api/v1/books/6/loans").header(USER_HEADER, "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BOOK_UNAVAILABLE"));
        assertEquals(0, availableCopies(6));
    }

    @Test
    void currentLoansListsActiveLoansWithBookDetails() throws Exception {
        borrow(1, 1);
        borrow(1, 1); // the same user may hold several copies of one title
        borrow(2, 1);

        mockMvc.perform(get("/api/v1/users/me/loans").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].bookId").value(1))
                .andExpect(jsonPath("$.items[0].bookTitle").value("Clean Code"))
                .andExpect(jsonPath("$.items[0].bookAuthor").value("Robert C. Martin"))
                .andExpect(jsonPath("$.items[2].bookId").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void currentLoansOnlyReturnsTheCallersLoans() throws Exception {
        borrow(1, 1);
        mockMvc.perform(get("/api/v1/users/me/loans").header(USER_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void unsupportedStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/loans").param("status", "returned").header(USER_HEADER, "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("status"));
    }

    @Test
    void currentLoansForUnknownUserReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/loans").header(USER_HEADER, "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void returningLoanRestoresStockAndClearsItFromActiveLoans() throws Exception {
        long loanId = borrow(1, 1);
        assertEquals(2, availableCopies(1));

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());

        assertEquals(3, availableCopies(1));
        assertEquals(0, openLoanCount(1));

        // The record is kept for history, it is just no longer "active".
        Integer allLoans = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM loans WHERE id = ?",
                Integer.class, loanId);
        assertEquals(1, allLoans);

        mockMvc.perform(get("/api/v1/users/me/loans").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void returningTheSameLoanTwiceReturns409AndDoesNotDoubleRestoreStock() throws Exception {
        long loanId = borrow(1, 1);
        returnLoan(loanId, 1);

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return").header(USER_HEADER, "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("LOAN_ALREADY_RETURNED"));

        assertEquals(3, availableCopies(1));
    }

    @Test
    void returningAnotherUsersLoanReturns403() throws Exception {
        long loanId = borrow(1, 1);

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return").header(USER_HEADER, "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        // The failed attempt must not change anything.
        assertEquals(2, availableCopies(1));
        assertEquals(1, openLoanCount(1));
    }

    @Test
    void returningUnknownLoanReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/loans/424242/return").header(USER_HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LOAN_NOT_FOUND"));
    }

    @Test
    void foreignKeyConstraintIsEnforcedBySqlite() {
        // SQLite reports constraint violations with a null SQL state, so Spring
        // surfaces them as a generic DataAccessException rather than a subclass.
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO loans (book_id, user_id, borrowed_at) VALUES (?, ?, ?)",
                99999, 1, "2026-01-01T00:00:00Z"));
    }

    @Test
    void stockCheckConstraintIsEnforcedBySqlite() {
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "UPDATE books SET available_copies = total_copies + 1 WHERE id = 1"));
    }

    @Test
    void uniqueIsbnConstraintIsEnforcedBySqlite() {
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update("""
                INSERT INTO books (title, author, isbn, description, total_copies, available_copies, created_at)
                VALUES ('Duplicate', 'Author', '9780132350884', NULL, 1, 1, '2026-01-01T00:00:00Z')
                """));
    }

    private long borrow(long bookId, long userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books/" + bookId + "/loans")
                        .header(USER_HEADER, String.valueOf(userId)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void returnLoan(long loanId, long userId) throws Exception {
        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return")
                        .header(USER_HEADER, String.valueOf(userId)))
                .andExpect(status().isOk());
    }
}
