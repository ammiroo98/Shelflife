package com.amiroo.shelflife.lending;

import java.time.LocalDate;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.Item;
import com.amiroo.shelflife.membership.Member;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LoanTest {

    @Test
    void loanLinksMemberToOneSpecificCopyOfASharedItem() {
        Item item = new Item(UUID.randomUUID(), "The Hobbit");
        Copy borrowedCopy = new Copy(UUID.randomUUID(), item);
        Copy otherCopy = new Copy(UUID.randomUUID(), item);
        Member member = new Member(UUID.randomUUID(), "Alex");

        Loan loan = new Loan(UUID.randomUUID(), borrowedCopy, member,
                LocalDate.of(2026, 10, 22), LocalDate.of(2026, 11, 5), null);

        assertThat(loan.getCopy()).isSameAs(borrowedCopy).isNotSameAs(otherCopy);
        assertThat(borrowedCopy.getId()).isNotEqualTo(otherCopy.getId());
        assertThat(loan.getCopy().getItem()).isSameAs(otherCopy.getItem());
        assertThat(loan.getMember()).isSameAs(member);
        assertThat(loan.getReturnedOn()).isNull();
    }

    @Test
    void loanCanRepresentRecordedBorrowingAndReturnDates() {
        Item item = new Item(UUID.randomUUID(), "The Hobbit");
        Copy copy = new Copy(UUID.randomUUID(), item);
        Member member = new Member(UUID.randomUUID(), "Alex");
        LocalDate borrowedOn = LocalDate.of(2026, 10, 22);
        LocalDate dueOn = LocalDate.of(2026, 11, 5);
        LocalDate returnedOn = LocalDate.of(2026, 11, 2);

        Loan loan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, returnedOn);

        assertThat(loan.getBorrowedOn()).isEqualTo(borrowedOn);
        assertThat(loan.getDueOn()).isEqualTo(dueOn);
        assertThat(loan.getReturnedOn()).isEqualTo(returnedOn);
    }

    @Test
    void copyAndLoanRequireTheirRelationships() {
        Item item = new Item(UUID.randomUUID(), "The Hobbit");
        Copy copy = new Copy(UUID.randomUUID(), item);
        Member member = new Member(UUID.randomUUID(), "Alex");
        LocalDate borrowedOn = LocalDate.of(2026, 10, 22);
        LocalDate dueOn = LocalDate.of(2026, 11, 5);

        assertThatNullPointerException()
                .isThrownBy(() -> new Copy(UUID.randomUUID(), null))
                .withMessage("item");
        assertThatNullPointerException()
                .isThrownBy(() -> new Loan(UUID.randomUUID(), null, member, borrowedOn, dueOn, null))
                .withMessage("copy");
        assertThatNullPointerException()
                .isThrownBy(() -> new Loan(UUID.randomUUID(), copy, null, borrowedOn, dueOn, null))
                .withMessage("member");
    }
}
