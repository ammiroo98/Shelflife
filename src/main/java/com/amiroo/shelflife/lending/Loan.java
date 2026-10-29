package com.amiroo.shelflife.lending;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.membership.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A loan's recorded data. A null returnedOn means the copy has not been returned.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private Copy copy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "borrowed_on", nullable = false)
    private LocalDate borrowedOn;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Column(name = "returned_on", nullable = true)
    private LocalDate returnedOn;

    // Required by JPA for loading entities; normal callers use the validated constructor.
    protected Loan() {
    }

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
