package com.amiroo.shelflife.lending;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.membership.Member;

/**
 * A loan's recorded data. A null returnedOn means the copy has not been returned.
 */
public class Loan {

    private final UUID id;
    private final Copy copy;
    private final Member member;
    private final LocalDate borrowedOn;
    private final LocalDate dueOn;
    private LocalDate returnedOn;

    public Loan(UUID id, Copy copy, Member member, LocalDate borrowedOn, LocalDate dueOn, LocalDate returnedOn) {
        this.id = Objects.requireNonNull(id, "id");
        this.copy = Objects.requireNonNull(copy, "copy");
        this.member = Objects.requireNonNull(member, "member");
        this.borrowedOn = Objects.requireNonNull(borrowedOn, "borrowedOn");
        this.dueOn = Objects.requireNonNull(dueOn, "dueOn");
        if (dueOn.isBefore(borrowedOn)) {
            throw new IllegalArgumentException("dueOn must not be before borrowedOn.");
        }
        if (returnedOn != null && returnedOn.isBefore(borrowedOn)) {
            throw new IllegalArgumentException("returnedOn must not be before borrowedOn.");
        }
        this.returnedOn = returnedOn;
    }

    public UUID getId() {
        return id;
    }

    public Copy getCopy() {
        return copy;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getBorrowedOn() {
        return borrowedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public boolean isActive() {
        return returnedOn == null;
    }

    public void returnOn(LocalDate returnedOn) {
        Objects.requireNonNull(returnedOn, "returnedOn");
        if (!isActive()) {
            throw new BorrowingException("Loan has already been returned.");
        }
        if (returnedOn.isBefore(borrowedOn)) {
            throw new IllegalArgumentException("returnedOn must not be before borrowedOn.");
        }
        this.returnedOn = returnedOn;
    }
}
