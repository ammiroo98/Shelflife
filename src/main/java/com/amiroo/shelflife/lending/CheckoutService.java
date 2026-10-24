package com.amiroo.shelflife.lending;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.membership.Member;
import com.amiroo.shelflife.membership.MemberStatus;

/**
 * Checks borrowing rules against the caller's complete collection of known loans.
 * The caller must retain successful loans and serialize checkout attempts;
 * this service does not store loans or provide concurrency protection.
 */
public class CheckoutService {

    private final Clock clock;
    private final int maxActiveLoans;
    private final int loanDurationDays;

    public CheckoutService(Clock clock, int maxActiveLoans, int loanDurationDays) {
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

    public Loan checkout(Member member, Copy copy, Collection<Loan> loans) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(copy, "copy");
        Objects.requireNonNull(loans, "loans");

        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new BorrowingException("Suspended members cannot borrow.");
        }

        long activeLoanCount = loans.stream()
                .filter(Loan::isActive)
                .filter(loan -> loan.getMember().getId().equals(member.getId()))
                .count();
        if (activeLoanCount >= maxActiveLoans) {
            throw new BorrowingException("Member has reached the active loan limit of " + maxActiveLoans + ".");
        }

        boolean copyOnLoan = loans.stream()
                .filter(Loan::isActive)
                .anyMatch(loan -> loan.getCopy().getId().equals(copy.getId()));
        if (copyOnLoan) {
            throw new BorrowingException("Copy already has an active loan.");
        }

        LocalDate borrowedOn = LocalDate.now(clock);
        return new Loan(UUID.randomUUID(), copy, member,
                borrowedOn, borrowedOn.plusDays(loanDurationDays), null);
    }
}
