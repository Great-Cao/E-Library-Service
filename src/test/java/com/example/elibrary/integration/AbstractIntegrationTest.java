package com.example.elibrary.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Shared setup for HTTP-level tests. Every test runs against a real SQLite file
 * that is rebuilt from the Flyway migrations, so results never depend on a
 * developer's local database or on leftovers from a previous run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    private static final Path TEST_DB = Path.of("target", "e-library-it.db");
    private static final String SEED_SCRIPT = "db/migration/R__seed_demo_data.sql";
    private static final String BORROWED_AT = "2026-01-01T00:00:00Z";

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        try {
            Files.deleteIfExists(TEST_DB);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String url = "jdbc:sqlite:" + TEST_DB.toAbsolutePath()
                + "?foreign_keys=true&busy_timeout=5000";
        registry.add("spring.datasource.url", () -> url);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetDatabase() {
        // Clear the data and replay the real seed script, so every test starts from
        // exactly the documented demo state regardless of what ran before it.
        jdbcTemplate.execute("DELETE FROM loans");
        jdbcTemplate.execute("DELETE FROM books");
        jdbcTemplate.execute("DELETE FROM users");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(SEED_SCRIPT));
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset the test database", e);
        }
    }

    protected void insertUser(long id, String name) {
        jdbcTemplate.update(
                "INSERT INTO users (id, name, created_at) VALUES (?, ?, ?)", id, name, BORROWED_AT);
    }

    protected long insertBook(String title, String isbn, int totalCopies, int availableCopies) {
        jdbcTemplate.update("""
                INSERT INTO books (title, author, isbn, description, total_copies, available_copies, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, title, "Test Author", isbn, "Test description", totalCopies, availableCopies, BORROWED_AT);
        Long id = jdbcTemplate.queryForObject("SELECT id FROM books WHERE isbn = ?", Long.class, isbn);
        return id == null ? -1L : id;
    }

    protected int availableCopies(long bookId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT available_copies FROM books WHERE id = ?", Integer.class, bookId);
        return value == null ? -1 : value;
    }

    protected int openLoanCount(long bookId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loans WHERE book_id = ? AND returned_at IS NULL",
                Integer.class, bookId);
        return value == null ? -1 : value;
    }
}
