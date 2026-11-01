package com.amiroo.shelflife.lending;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.membership.Member;
import com.amiroo.shelflife.membership.MemberStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks borrowing rules against stored loans and persists successful checkout.
 * The transaction does not protect against concurrent checkout attempts.
 */
@Service
public class CheckoutService {

    private final LoanRepository loanRepository;
    private final Clock clock;
    private final int maxActiveLoans;
    private final int loanDurationDays;

    public CheckoutService(LoanRepository loanRepository, Clock clock,
            @Value("${shelflife.lending.max-active-loans}") int maxActiveLoans,
            @Value("${shelflife.lending.loan-duration-days}") int loanDurationDays) {
        this.loanRepository = Objects.requireNonNull(loanRepository, "loanRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (maxActiveLoans <= 0) {
            throw new IllegalArgumentException("maxActiveLoans must be positive.");
        }
        if (loanDurationDays <= 0) {
            throw new IllegalArgumentException("loanDurationDays must be positive.");
        }
        this.maxActiveLoans = maxActiveLoans;
        this.loanDurationDays = loanDurationDays;
    }

    @Transactional
    public Loan checkout(Member member, Copy copy) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(copy, "copy");

        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new BorrowingException("Suspended members cannot borrow.");
        }

        long activeLoanCount = loanRepository.countByMember_IdAndReturnedOnIsNull(member.getId());
        if (activeLoanCount >= maxActiveLoans) {
            throw new BorrowingException("Member has reached the active loan limit of " + maxActiveLoans + ".");
        }

        boolean copyOnLoan = loanRepository.existsByCopy_IdAndReturnedOnIsNull(copy.getId());
        if (copyOnLoan) {
            throw new BorrowingException("Copy already has an active loan.");
        }

        LocalDate borrowedOn = LocalDate.now(clock);
        Loan loan = new Loan(UUID.randomUUID(), copy, member,
                borrowedOn, borrowedOn.plusDays(loanDurationDays), null);
        return loanRepository.save(loan);
    }
}
