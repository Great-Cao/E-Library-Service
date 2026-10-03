package com.example.elibrary.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void openApiDocumentIsExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("E-Library Service API"))
                .andExpect(jsonPath("$.paths['/api/v1/books']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/books/{bookId}/loans']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/loans/{loanId}/return']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/loans']").exists());
    }

    @Test
    void swaggerUiIsAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
