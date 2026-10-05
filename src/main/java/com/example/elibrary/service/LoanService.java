package com.example.elibrary.service;

import com.example.elibrary.domain.Loan;
import com.example.elibrary.dto.response.ActiveLoanResponse;
import com.example.elibrary.dto.response.LoanResponse;
import com.example.elibrary.dto.response.PageResponse;
import com.example.elibrary.exception.ApiException;
import com.example.elibrary.exception.ErrorCode;
import com.example.elibrary.exception.ErrorResponse.FieldViolation;
import com.example.elibrary.repository.BookRepository;
import com.example.elibrary.repository.LoanRepository;
import com.example.elibrary.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanService {

    private static final String ACTIVE_STATUS = "active";

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;

    public LoanService(BookRepository bookRepository,
                       UserRepository userRepository,
                       LoanRepository loanRepository,
                       Clock clock) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    /**
     * Borrows one copy of a book. The stock decrement is a single conditional
     * SQL update, so two concurrent requests can never both take the last copy.
     */
    @Transactional
    public LoanResponse borrow(Long bookId, Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND, "User " + userId + " does not exist.");
        }
        if (!bookRepository.existsById(bookId)) {
            throw new ApiException(ErrorCode.BOOK_NOT_FOUND, "Book " + bookId + " does not exist.");
        }

        int updated = bookRepository.decrementAvailableCopies(bookId);
        if (updated == 0) {
            throw new ApiException(ErrorCode.BOOK_UNAVAILABLE,
                    "Book " + bookId + " has no copies available.");
        }

        Loan loan = new Loan(
                bookRepository.getReferenceById(bookId),
                userRepository.getReferenceById(userId),
                now());
        Loan saved = loanRepository.saveAndFlush(loan);
        return LoanResponse.from(saved);
    }

    /**
     * Returns a loan. Ownership is checked first, then the loan is closed with a
     * conditional update so a duplicate return cannot restore stock twice.
     */
    @Transactional
    public LoanResponse returnLoan(Long loanId, Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND, "User " + userId + " does not exist.");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException(ErrorCode.LOAN_NOT_FOUND,
                        "Loan " + loanId + " does not exist."));

        if (!loan.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN,
                    "Loan " + loanId + " belongs to another user.");
        }

        Long bookId = loan.getBook().getId();
        Long ownerId = loan.getUser().getId();
        Instant borrowedAt = loan.getBorrowedAt();
        Instant returnedAt = now();

        int updated = loanRepository.markReturned(loanId, returnedAt);
        if (updated == 0) {
            throw new ApiException(ErrorCode.LOAN_ALREADY_RETURNED,
                    "Loan " + loanId + " has already been returned.");
        }

        bookRepository.incrementAvailableCopies(bookId);
        return new LoanResponse(loanId, bookId, ownerId, borrowedAt, returnedAt);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActiveLoanResponse> currentLoans(Long userId, String status, int page, int size) {
        validateStatus(status);
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND, "User " + userId + " does not exist.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Loan> loans = loanRepository.findActiveLoans(userId, pageable);
        return PageResponse.of(loans, ActiveLoanResponse::from);
    }

    private void validateStatus(String status) {
        if (status != null && ACTIVE_STATUS.equalsIgnoreCase(status)) {
            return;
        }
        throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "Only status=active is supported.",
                List.of(new FieldViolation("status", "only 'active' is supported")));
    }

    /** Millisecond precision keeps timestamps readable and stable in API output. */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
