package com.example.elibrary.dto.response;

import com.example.elibrary.domain.Loan;

import java.time.Instant;

public record LoanResponse(
        Long id,
        Long bookId,
        Long userId,
        Instant borrowedAt,
        Instant returnedAt
) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getUser().getId(),
                loan.getBorrowedAt(),
                loan.getReturnedAt());
    }
}
