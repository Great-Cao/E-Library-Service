package com.example.elibrary.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void browseReturnsFirstPageSortedById() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.items[0].availableCopies").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void browseLastPageReturnsRemainingBook() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(6));
    }

    @Test
    void detailReturnsBookWithDescriptionAndTimestamp() throws Exception {
        mockMvc.perform(get("/api/v1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert C. Martin"))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.totalCopies").value(3))
                .andExpect(jsonPath("$.availableCopies").value(3))
                .andExpect(jsonPath("$.createdAt").value("2026-01-05T08:05:00Z"));
    }

    @Test
    void detailOfUnknownBookReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void pageSizeAboveMaximumReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("size"));
    }

    @Test
    void negativePageReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void nonNumericBookIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/books/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownRouteReturns404InsteadOf500() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(delete("/api/v1/books/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void oversizedPageNumberReturns400InsteadOf500() throws Exception {
        // page * size must stay inside the int range used for the SQL offset.
        mockMvc.perform(get("/api/v1/books").param("page", "21474837").param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void blankBookIdReturns400InsteadOf500() throws Exception {
        // Tomcat decodes %20 to a space before Spring matches the route, so the path
        // variable ends up blank. MockMvc performs no such decoding, so the decoded
        // URI is supplied explicitly to reproduce what a real request hits.
        mockMvc.perform(get("/api/v1/books").with(request -> {
                    request.setRequestURI("/api/v1/books/ ");
                    request.setServletPath("/api/v1/books/ ");
                    return request;
                }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidBookIdDoesNotLeakInternalTypeNames() throws Exception {
        mockMvc.perform(get("/api/v1/books/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message", not(containsString("java."))))
                .andExpect(jsonPath("$.error.details[0].reason", not(containsString("java."))));
    }
}
