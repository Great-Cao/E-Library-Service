package com.example.elibrary.integration;

import com.example.elibrary.exception.ApiException;
import com.example.elibrary.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that the conditional stock update really prevents overselling: many
 * borrowers race for a small number of copies and never take more than exist.
 */
class ConcurrentBorrowIntegrationTest extends AbstractIntegrationTest {

    private static final long USER_ID = 1L;

    @Autowired
    private LoanService loanService;

    @Test
    void concurrentBorrowersNeverOversell() throws Exception {
        long bookId = insertBook("Scarce Title", "isbn-concurrency-test", 2, 2);
        int borrowers = 8;

        ExecutorService pool = Executors.newFixedThreadPool(borrowers);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < borrowers; i++) {
                results.add(pool.submit(() -> {
                    startTogether.await();
                    try {
                        loanService.borrow(bookId, USER_ID);
                        return true;
                    } catch (ApiException ex) {
                        return false;
                    }
                }));
            }
            startTogether.countDown();

            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (Boolean.TRUE.equals(result.get(30, TimeUnit.SECONDS))) {
                    succeeded++;
                }
            }

            assertEquals(2, succeeded, "exactly the available copies should be borrowed");
            assertEquals(2, openLoanCount(bookId));
            assertTrue(availableCopies(bookId) >= 0, "stock must never go negative");
            assertEquals(0, availableCopies(bookId));
        } finally {
            pool.shutdownNow();
        }
    }
}
