package com.amiroo.shelflife.lending.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.amiroo.shelflife.lending.Loan;

public record LoanResponse(UUID id, UUID copyId, UUID memberId, LocalDate borrowedOn, LocalDate dueOn,
        LocalDate returnedOn, boolean active) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getCopy().getId(), loan.getMember().getId(),
                loan.getBorrowedOn(), loan.getDueOn(), loan.getReturnedOn(), loan.isActive());
    }
}
