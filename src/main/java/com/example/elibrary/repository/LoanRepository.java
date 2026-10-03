package com.example.elibrary.repository;

import com.example.elibrary.domain.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query("""
            select l
              from Loan l
              join fetch l.book
             where l.user.id = :userId
               and l.returnedAt is null
             order by l.id asc
            """)
    Page<Loan> findActiveLoans(@Param("userId") Long userId, Pageable pageable);

    /**
     * Marks a loan as returned only if it is still open. Returns the number of
     * affected rows so a duplicate return is detected rather than silently
     * restoring stock twice.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Loan l
               set l.returnedAt = :returnedAt
             where l.id = :loanId
               and l.returnedAt is null
            """)
    int markReturned(@Param("loanId") Long loanId, @Param("returnedAt") Instant returnedAt);
}
