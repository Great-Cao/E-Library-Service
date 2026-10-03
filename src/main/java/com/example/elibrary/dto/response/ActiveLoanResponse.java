package com.example.elibrary.dto.response;

import com.example.elibrary.domain.Book;
import com.example.elibrary.domain.Loan;

import java.time.Instant;

/**
 * An open loan enriched with the book details a client needs to render the list.
 */
public record ActiveLoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        Instant borrowedAt
) {

    public static ActiveLoanResponse from(Loan loan) {
        Book book = loan.getBook();
        return new ActiveLoanResponse(
                loan.getId(),
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                loan.getBorrowedAt());
    }
}
