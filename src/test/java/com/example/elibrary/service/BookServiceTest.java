package com.example.elibrary.service;

import com.example.elibrary.domain.Book;
import com.example.elibrary.dto.response.BookDetailResponse;
import com.example.elibrary.dto.response.BookSummaryResponse;
import com.example.elibrary.dto.response.PageResponse;
import com.example.elibrary.exception.ApiException;
import com.example.elibrary.exception.ErrorCode;
import com.example.elibrary.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void listBooksMapsPageIntoEnvelope() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(book.getTitle()).thenReturn("Clean Code");
        when(book.getAuthor()).thenReturn("Robert C. Martin");
        when(book.getIsbn()).thenReturn("9780132350884");
        when(book.getTotalCopies()).thenReturn(3);
        when(book.getAvailableCopies()).thenReturn(2);

        Page<Book> page = new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1);
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<BookSummaryResponse> response = bookService.listBooks(0, 20);

        assertEquals(1, response.items().size());
        assertEquals("Clean Code", response.items().get(0).title());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    @Test
    void getBookReturnsDetailWhenBookExists() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(7L);
        when(book.getTitle()).thenReturn("Refactoring");
        when(book.getAuthor()).thenReturn("Martin Fowler");
        when(book.getIsbn()).thenReturn("9780134757599");
        when(book.getDescription()).thenReturn("Improving the design of existing code.");
        when(book.getTotalCopies()).thenReturn(1);
        when(book.getAvailableCopies()).thenReturn(1);
        when(book.getCreatedAt()).thenReturn(Instant.parse("2026-01-05T08:08:00Z"));
        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));

        BookDetailResponse response = bookService.getBook(7L);

        assertEquals("Refactoring", response.title());
        assertEquals("Improving the design of existing code.", response.description());
        assertEquals(Instant.parse("2026-01-05T08:08:00Z"), response.createdAt());
    }

    @Test
    void getBookThrowsBookNotFoundWhenMissing() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> bookService.getBook(99L));

        assertEquals(ErrorCode.BOOK_NOT_FOUND, ex.errorCode());
    }
}
