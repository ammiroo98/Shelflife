package com.amiroo.shelflife.lending;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {

    private final LoanRepository loanRepository;
    private final Clock clock;

    public ReturnService(LoanRepository loanRepository, Clock clock) {
        this.loanRepository = Objects.requireNonNull(loanRepository, "loanRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public Loan returnLoan(UUID loanId) {
        Objects.requireNonNull(loanId, "loanId");
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        loan.returnOn(LocalDate.now(clock));
        return loan;
    }
}
