package com.example.elibrary.controller;

import com.example.elibrary.config.CurrentUserId;
import com.example.elibrary.dto.response.BookDetailResponse;
import com.example.elibrary.dto.response.BookSummaryResponse;
import com.example.elibrary.dto.response.LoanResponse;
import com.example.elibrary.dto.response.PageResponse;
import com.example.elibrary.service.BookService;
import com.example.elibrary.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Books", description = "Browse books and borrow copies")
public class BookController {

    private final BookService bookService;
    private final LoanService loanService;

    public BookController(BookService bookService, LoanService loanService) {
        this.bookService = bookService;
        this.loanService = loanService;
    }

    @GetMapping("/books")
    @Operation(summary = "Browse books",
            description = "Returns books ordered by id. Pagination only; no search filters in this scope.")
    public PageResponse<BookSummaryResponse> listBooks(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, 1-100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return bookService.listBooks(page, size);
    }

    @GetMapping("/books/{bookId}")
    @Operation(summary = "Get book details")
    public BookDetailResponse getBook(@PathVariable Long bookId) {
        return bookService.getBook(bookId);
    }

    @PostMapping("/books/{bookId}/loans")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Borrow a book",
            description = "Borrows one copy for the user identified by X-User-Id. "
                    + "A user may hold several copies of the same title.")
    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true,
            description = "Simulated current user id", example = "1")
    public LoanResponse borrowBook(@PathVariable Long bookId, @CurrentUserId Long userId) {
        return loanService.borrow(bookId, userId);
    }
}
