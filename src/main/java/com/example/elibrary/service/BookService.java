package com.example.elibrary.service;

import com.example.elibrary.domain.Book;
import com.example.elibrary.dto.response.BookDetailResponse;
import com.example.elibrary.dto.response.BookSummaryResponse;
import com.example.elibrary.dto.response.PageResponse;
import com.example.elibrary.exception.ApiException;
import com.example.elibrary.exception.ErrorCode;
import com.example.elibrary.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookSummaryResponse> listBooks(int page, int size) {
        // A deterministic sort keeps pagination stable across requests.
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Book> books = bookRepository.findAll(pageable);
        return PageResponse.of(books, BookSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public BookDetailResponse getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ApiException(ErrorCode.BOOK_NOT_FOUND,
                        "Book " + bookId + " does not exist."));
        return BookDetailResponse.from(book);
    }
}
