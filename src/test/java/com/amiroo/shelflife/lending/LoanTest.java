package com.amiroo.shelflife.lending;

import java.time.LocalDate;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.Item;
import com.amiroo.shelflife.membership.Member;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    private final Item item = new Item(UUID.randomUUID(), "The Hobbit");
    private final Copy copy = new Copy(UUID.randomUUID(), item);
    private final Member member = new Member(UUID.randomUUID(), "Alex");
    private final LocalDate borrowedOn = LocalDate.of(2026, 10, 22);
    private final LocalDate dueOn = LocalDate.of(2026, 11, 5);

    @Test
    void dueDateBeforeBorrowedDateIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Loan(UUID.randomUUID(), copy, member,
                        borrowedOn, borrowedOn.minusDays(1), null))
                .withMessage("dueOn must not be before borrowedOn.");
    }

    @Test
    void recordedReturnBeforeBorrowedDateIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Loan(UUID.randomUUID(), copy, member,
                        borrowedOn, dueOn, borrowedOn.minusDays(1)))
                .withMessage("returnedOn must not be before borrowedOn.");
    }

    @Test
    void dueDateAndRecordedReturnCanEqualBorrowedDate() {
        Loan loan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, borrowedOn, borrowedOn);

        assertThat(loan.getDueOn()).isEqualTo(borrowedOn);
        assertThat(loan.getReturnedOn()).isEqualTo(borrowedOn);
        assertThat(loan.isActive()).isFalse();
        assertThatThrownBy(() -> loan.returnOn(borrowedOn))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Loan has already been returned.");
    }

    @Test
    void returnBeforeBorrowedDateLeavesLoanActiveAndAllowsLaterValidReturn() {
        Loan loan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> loan.returnOn(borrowedOn.minusDays(1)))
                .withMessage("returnedOn must not be before borrowedOn.");
        assertThat(loan.isActive()).isTrue();
        assertThat(loan.getReturnedOn()).isNull();

        loan.returnOn(borrowedOn);

        assertThat(loan.isActive()).isFalse();
        assertThat(loan.getReturnedOn()).isEqualTo(borrowedOn);
    }

    @Test
    void returnAfterDueDateRemainsAllowed() {
        Loan recordedLoan = new Loan(UUID.randomUUID(), copy, member,
                borrowedOn, dueOn, dueOn.plusDays(1));
        Loan activeLoan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, null);

        activeLoan.returnOn(dueOn.plusDays(1));

        assertThat(recordedLoan.getReturnedOn()).isEqualTo(dueOn.plusDays(1));
        assertThat(activeLoan.getReturnedOn()).isEqualTo(dueOn.plusDays(1));
    }

    @Test
    void activeLoanCanBeReturnedOnlyOnce() {
        LocalDate returnedOn = LocalDate.of(2026, 11, 2);
        Loan loan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, null);

        assertThat(loan.isActive()).isTrue();
        loan.returnOn(returnedOn);

        assertThat(loan.isActive()).isFalse();
        assertThat(loan.getReturnedOn()).isEqualTo(returnedOn);
        assertThatThrownBy(() -> loan.returnOn(returnedOn.plusDays(1)))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Loan has already been returned.");
        assertThat(loan.getReturnedOn()).isEqualTo(returnedOn);
        assertThat(loan.getBorrowedOn()).isEqualTo(borrowedOn);
        assertThat(loan.getDueOn()).isEqualTo(dueOn);
    }

    @Test
    void nullReturnDateCannotChangeAnActiveLoan() {
        Loan loan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, null);

        assertThatNullPointerException().isThrownBy(() -> loan.returnOn(null))
                .withMessage("returnedOn");
        assertThat(loan.isActive()).isTrue();
    }

    @Test
    void loanLinksMemberToOneSpecificCopyOfASharedItem() {
        Copy borrowedCopy = new Copy(UUID.randomUUID(), item);
        Copy otherCopy = new Copy(UUID.randomUUID(), item);

        Loan loan = new Loan(UUID.randomUUID(), borrowedCopy, member, borrowedOn, dueOn, null);

        assertThat(loan.getCopy()).isSameAs(borrowedCopy).isNotSameAs(otherCopy);
        assertThat(borrowedCopy.getId()).isNotEqualTo(otherCopy.getId());
        assertThat(loan.getCopy().getItem()).isSameAs(otherCopy.getItem());
        assertThat(loan.getMember()).isSameAs(member);
        assertThat(loan.getReturnedOn()).isNull();
    }

    @Test
    void loanCanRepresentRecordedBorrowingAndReturnDates() {
        LocalDate returnedOn = LocalDate.of(2026, 11, 2);

        Loan loan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, returnedOn);

        assertThat(loan.getBorrowedOn()).isEqualTo(borrowedOn);
        assertThat(loan.getDueOn()).isEqualTo(dueOn);
        assertThat(loan.getReturnedOn()).isEqualTo(returnedOn);
    }

    @Test
    void copyAndLoanRequireTheirRelationships() {
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
