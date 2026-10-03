package com.example.elibrary.repository;

import com.example.elibrary.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Decrements the available copy count only when at least one copy is free.
     * Returns the number of affected rows, so callers can detect contention
     * without ever reading a stale value in Java.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Book b
               set b.availableCopies = b.availableCopies - 1
             where b.id = :bookId
               and b.availableCopies > 0
            """)
    int decrementAvailableCopies(@Param("bookId") Long bookId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Book b
               set b.availableCopies = b.availableCopies + 1
             where b.id = :bookId
            """)
    int incrementAvailableCopies(@Param("bookId") Long bookId);
}
