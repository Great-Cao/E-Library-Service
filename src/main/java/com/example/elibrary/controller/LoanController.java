package com.example.elibrary.controller;

import com.example.elibrary.config.CurrentUserId;
import com.example.elibrary.dto.response.ActiveLoanResponse;
import com.example.elibrary.dto.response.LoanResponse;
import com.example.elibrary.dto.response.PageResponse;
import com.example.elibrary.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Loans", description = "Return copies and list the current user's loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/loans/{loanId}/return")
    @Operation(summary = "Return a borrowed copy",
            description = "Closes the loan and restores one copy of stock. Only the owning "
                    + "user may return a loan; a repeated return returns 409.")
    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true,
            description = "Simulated current user id", example = "1")
    public LoanResponse returnLoan(@PathVariable Long loanId, @CurrentUserId Long userId) {
        return loanService.returnLoan(loanId, userId);
    }

    @GetMapping("/users/me/loans")
    @Operation(summary = "List the current user's active loans",
            description = "Returns not-yet-returned loans of the user identified by X-User-Id, "
                    + "each enriched with the book title and author. Only status=active is supported.")
    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true,
            description = "Simulated current user id", example = "1")
    public PageResponse<ActiveLoanResponse> currentLoans(
            @Parameter(description = "Loan status filter; only 'active' is supported", example = "active")
            @RequestParam(defaultValue = "active") String status,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, 1-100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @CurrentUserId Long userId) {
        return loanService.currentLoans(userId, status, page, size);
    }
}
