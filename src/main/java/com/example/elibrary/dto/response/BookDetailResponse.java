package com.example.elibrary.dto.response;

import com.example.elibrary.domain.Book;

import java.time.Instant;

public record BookDetailResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String description,
        int totalCopies,
        int availableCopies,
        Instant createdAt
) {

    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getDescription(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.getCreatedAt());
    }
}
