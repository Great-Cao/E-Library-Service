package com.example.elibrary.service;

import com.example.elibrary.domain.Book;
import com.example.elibrary.domain.Loan;
import com.example.elibrary.domain.User;
import com.example.elibrary.dto.response.ActiveLoanResponse;
import com.example.elibrary.dto.response.LoanResponse;
import com.example.elibrary.dto.response.PageResponse;
import com.example.elibrary.exception.ApiException;
import com.example.elibrary.exception.ErrorCode;
import com.example.elibrary.repository.BookRepository;
import com.example.elibrary.repository.LoanRepository;
import com.example.elibrary.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-01T10:00:00Z");
    private static final Instant BORROWED_AT = Instant.parse("2026-01-10T09:00:00Z");
    private static final long ALICE = 1L;
    private static final long BRIAN = 2L;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanRepository loanRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        loanService = new LoanService(bookRepository, userRepository, loanRepository, clock);
    }

    // ---------------------------------------------------------------- borrow

    @Test
    void borrowDecrementsStockAndCreatesLoan() {
        when(userRepository.existsById(ALICE)).thenReturn(true);
        when(bookRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.decrementAvailableCopies(1L)).thenReturn(1);

        Book book = bookWithId(1L);
        User user = userWithId(ALICE);
        when(bookRepository.getReferenceById(1L)).thenReturn(book);
        when(userRepository.getReferenceById(ALICE)).thenReturn(user);

        Loan saved = mock(Loan.class);
        when(saved.getId()).thenReturn(100L);
        when(saved.getBook()).thenReturn(book);
        when(saved.getUser()).thenReturn(user);
        when(saved.getBorrowedAt()).thenReturn(NOW);
        when(loanRepository.saveAndFlush(any(Loan.class))).thenReturn(saved);

        LoanResponse response = loanService.borrow(1L, ALICE);

        assertEquals(100L, response.id().longValue());
        assertEquals(1L, response.bookId().longValue());
        assertEquals(ALICE, response.userId().longValue());
        assertEquals(NOW, response.borrowedAt());
        assertNull(response.returnedAt());
    }

    @Test
    void borrowRejectsUnknownUser() {
        when(userRepository.existsById(9L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> loanService.borrow(1L, 9L));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode());
        verify(bookRepository, never()).decrementAvailableCopies(anyLong());
    }

    @Test
    void borrowRejectsUnknownBook() {
        when(userRepository.existsById(ALICE)).thenReturn(true);
        when(bookRepository.existsById(9L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> loanService.borrow(9L, ALICE));

        assertEquals(ErrorCode.BOOK_NOT_FOUND, ex.errorCode());
        verify(bookRepository, never()).decrementAvailableCopies(anyLong());
    }

    @Test
    void borrowRejectsWhenNoCopyIsAvailable() {
        when(userRepository.existsById(ALICE)).thenReturn(true);
        when(bookRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.decrementAvailableCopies(1L)).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class, () -> loanService.borrow(1L, ALICE));

        assertEquals(ErrorCode.BOOK_UNAVAILABLE, ex.errorCode());
        verify(loanRepository, never()).saveAndFlush(any(Loan.class));
    }

    // ------------------------------------------------------------- returning

    @Test
    void returnMarksLoanReturnedAndRestoresStock() {
        when(userRepository.existsById(ALICE)).thenReturn(true);
        Loan loan = openLoan(1L, ALICE);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(loanRepository.markReturned(eq(100L), any(Instant.class))).thenReturn(1);
        when(bookRepository.incrementAvailableCopies(1L)).thenReturn(1);

        LoanResponse response = loanService.returnLoan(100L, ALICE);

        assertEquals(100L, response.id().longValue());
        assertEquals(BORROWED_AT, response.borrowedAt());
        assertEquals(NOW, response.returnedAt());
        verify(bookRepository).incrementAvailableCopies(1L);
    }

    @Test
    void returnRejectsUnknownUser() {
        when(userRepository.existsById(9L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> loanService.returnLoan(100L, 9L));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode());
        verify(loanRepository, never()).findById(anyLong());
    }

    @Test
    void returnRejectsUnknownLoan() {
        when(userRepository.existsById(ALICE)).thenReturn(true);
        when(loanRepository.findById(404L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> loanService.returnLoan(404L, ALICE));

        assertEquals(ErrorCode.LOAN_NOT_FOUND, ex.errorCode());
    }

    @Test
    void returnRejectsLoanOwnedByAnotherUser() {
        when(userRepository.existsById(ALICE)).thenReturn(true);
        User owner = userWithId(BRIAN);
        Loan loan = mock(Loan.class);
        when(loan.getUser()).thenReturn(owner);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

        ApiException ex = assertThrows(ApiException.class, () -> loanService.returnLoan(100L, ALICE));

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode());
        verify(loanRepository, never()).markReturned(anyLong(), any(Instant.class));
        verify(bookRepository, never()).incrementAvailableCopies(anyLong());
    }

    @Test
    void returnRejectsAlreadyReturnedLoanWithoutRestoringStockAgain() {
        when(userRepository.existsById(ALICE)).thenReturn(true);
        Loan loan = openLoan(1L, ALICE);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(loanRepository.markReturned(eq(100L), any(Instant.class))).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class, () -> loanService.returnLoan(100L, ALICE));

        assertEquals(ErrorCode.LOAN_ALREADY_RETURNED, ex.errorCode());
        verify(bookRepository, never()).incrementAvailableCopies(anyLong());
    }

    // ---------------------------------------------------------- active loans

    @Test
    void currentLoansMapsActiveLoansWithBookDetails() {
        when(userRepository.existsById(ALICE)).thenReturn(true);

        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(book.getTitle()).thenReturn("Clean Code");
        when(book.getAuthor()).thenReturn("Robert C. Martin");
        Loan loan = mock(Loan.class);
        when(loan.getId()).thenReturn(100L);
        when(loan.getBook()).thenReturn(book);
        when(loan.getBorrowedAt()).thenReturn(BORROWED_AT);

        Page<Loan> page = new PageImpl<>(List.of(loan), PageRequest.of(0, 20), 1);
        when(loanRepository.findActiveLoans(eq(ALICE), any(Pageable.class))).thenReturn(page);

        PageResponse<ActiveLoanResponse> response = loanService.currentLoans(ALICE, "active", 0, 20);

        assertEquals(1, response.items().size());
        assertEquals(1L, response.items().get(0).bookId().longValue());
        assertEquals("Clean Code", response.items().get(0).bookTitle());
        assertEquals(BORROWED_AT, response.items().get(0).borrowedAt());
    }

    @Test
    void currentLoansRejectsUnsupportedStatus() {
        ApiException ex = assertThrows(ApiException.class,
                () -> loanService.currentLoans(ALICE, "returned", 0, 20));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.errorCode());
        verify(userRepository, never()).existsById(anyLong());
    }

    @Test
    void currentLoansRejectsUnknownUser() {
        when(userRepository.existsById(9L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> loanService.currentLoans(9L, "active", 0, 20));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode());
    }

    private Book bookWithId(long id) {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(id);
        return book;
    }

    private User userWithId(long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    /** An open loan; only the fields returnLoan() reads are stubbed. */
    private Loan openLoan(long bookId, long ownerId) {
        Book book = bookWithId(bookId);
        User user = userWithId(ownerId);
        Loan loan = mock(Loan.class);
        when(loan.getBook()).thenReturn(book);
        when(loan.getUser()).thenReturn(user);
        when(loan.getBorrowedAt()).thenReturn(BORROWED_AT);
        return loan;
    }
}
