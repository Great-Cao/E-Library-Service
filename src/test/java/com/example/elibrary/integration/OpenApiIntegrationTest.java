package com.example.elibrary.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void identityIsDocumentedAsAHeaderAndNotAsAQueryParameter() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode document = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String path : List.of(
                "/api/v1/books/{bookId}/loans",
                "/api/v1/loans/{loanId}/return",
                "/api/v1/users/me/loans")) {
            JsonNode pathItem = document.path("paths").path(path);
            JsonNode operation = pathItem.has("post") ? pathItem.path("post") : pathItem.path("get");

            List<String> parameterNames = new ArrayList<>();
            operation.path("parameters").forEach(
                    parameter -> parameterNames.add(parameter.path("name").asText()));

            assertFalse(parameterNames.contains("userId"),
                    path + " must not document a userId parameter, but had: " + parameterNames);
            assertTrue(parameterNames.contains("X-User-Id"),
                    path + " should document the X-User-Id header, but had: " + parameterNames);
        }
    }

    @Test
    void sharedErrorModelIsDocumented() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode document = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode responses = document.path("paths")
                .path("/api/v1/books/{bookId}/loans").path("post").path("responses");

        assertTrue(responses.has("400"), "expected a documented 400 response");
        assertTrue(responses.has("409"), "expected a documented 409 response");
        assertTrue(document.path("components").path("schemas").has("ErrorResponse"),
                "expected the shared ErrorResponse schema in components");
    }

    @Test
    void everyEndpointDocumentsItsSuccessResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode document = objectMapper.readTree(result.getResponse().getContentAsString());

        // Declaring error responses must not suppress the auto-generated success response.
        assertSuccessResponse(document, "/api/v1/books", "get", "200");
        assertSuccessResponse(document, "/api/v1/books/{bookId}", "get", "200");
        assertSuccessResponse(document, "/api/v1/books/{bookId}/loans", "post", "201");
        assertSuccessResponse(document, "/api/v1/loans/{loanId}/return", "post", "200");
        assertSuccessResponse(document, "/api/v1/users/me/loans", "get", "200");

        JsonNode schemas = document.path("components").path("schemas");
        for (String schema : List.of("BookDetailResponse", "LoanResponse")) {
            assertTrue(schemas.has(schema),
                    "expected response schema " + schema + " in components, but had: "
                            + schemas.fieldNames().next());
        }
    }

    private void assertSuccessResponse(JsonNode document, String path, String method, String code) {
        JsonNode responses = document.path("paths").path(path).path(method).path("responses");
        assertTrue(responses.has(code),
                path + " should document " + code + ", but documented: "
                        + String.join(", ", iterable(responses.fieldNames())));
        assertTrue(responses.path(code).path("content").has("application/json"),
                path + " " + code + " should describe a JSON response body");
    }

    private List<String> iterable(java.util.Iterator<String> names) {
        List<String> values = new ArrayList<>();
        names.forEachRemaining(values::add);
        return values;
    }
}
