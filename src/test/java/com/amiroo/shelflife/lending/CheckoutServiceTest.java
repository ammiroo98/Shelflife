package com.amiroo.shelflife.lending;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.Item;
import com.amiroo.shelflife.membership.Member;
import com.amiroo.shelflife.membership.MemberStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T02:00:00Z"),
            ZoneId.of("America/Toronto"));
    private final CheckoutService service = new CheckoutService(clock, 2, 14);
    private final Member member = new Member(UUID.randomUUID(), "Alex");
    private final Item item = new Item(UUID.randomUUID(), "The Hobbit");
    private final Copy copy = new Copy(UUID.randomUUID(), item);

    @Test
    void activeMemberCanCheckoutAvailableCopyUsingClockDateAndConfiguredDuration() {
        Loan loan = service.checkout(member, copy, List.of());

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(loan.getId()).isNotNull();
        assertThat(loan.getMember()).isSameAs(member);
        assertThat(loan.getCopy()).isSameAs(copy);
        assertThat(loan.isActive()).isTrue();
        assertThat(loan.getReturnedOn()).isNull();
        assertThat(loan.getBorrowedOn()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(loan.getDueOn()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    void suspendedMemberCannotCheckout() {
        Member suspended = new Member(UUID.randomUUID(), "Alex", MemberStatus.SUSPENDED);

        assertThatThrownBy(() -> service.checkout(suspended, copy, List.of()))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Suspended members cannot borrow.");
    }

    @Test
    void memberAtLimitCannotCheckoutEvenThroughAnotherObjectWithSameId() {
        Member sameMember = new Member(member.getId(), member.getName());
        List<Loan> loans = List.of(
                service.checkout(member, new Copy(UUID.randomUUID(), item), List.of()),
                service.checkout(member, new Copy(UUID.randomUUID(), item), List.of()));

        assertThatThrownBy(() -> service.checkout(sameMember, copy, loans))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Member has reached the active loan limit of 2.");
    }

    @Test
    void memberBelowLimitCanCheckoutAndOtherMembersLoansDoNotCount() {
        Member otherMember = new Member(UUID.randomUUID(), "Sam");
        List<Loan> loans = List.of(
                service.checkout(member, new Copy(UUID.randomUUID(), item), List.of()),
                service.checkout(otherMember, new Copy(UUID.randomUUID(), item), List.of()));

        assertThat(service.checkout(member, copy, loans).isActive()).isTrue();
    }

    @Test
    void copyWithActiveLoanCannotBeCheckedOutByAnotherMember() {
        Loan existing = service.checkout(member, copy, List.of());
        Member otherMember = new Member(UUID.randomUUID(), "Sam");
        Copy sameCopy = new Copy(copy.getId(), item);

        assertThatThrownBy(() -> service.checkout(otherMember, sameCopy, List.of(existing)))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Copy already has an active loan.");
    }

    @Test
    void returningLoanFreesBothCopyAndMemberCapacity() {
        CheckoutService oneLoanService = new CheckoutService(clock, 1, 7);
        List<Loan> loans = new ArrayList<>();
        Loan first = oneLoanService.checkout(member, copy, loans);
        loans.add(first);
        first.returnOn(LocalDate.now(clock));

        Loan second = oneLoanService.checkout(member, copy, loans);
        loans.add(second);

        assertThat(first.isActive()).isFalse();
        assertThat(second.isActive()).isTrue();
        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(second.getDueOn()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(loans).filteredOn(Loan::isActive).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({"0, 14", "-1, 14", "2, 0", "2, -1"})
    void limitsAndDurationMustBePositive(int maxActiveLoans, int loanDurationDays) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CheckoutService(clock, maxActiveLoans, loanDurationDays));
    }
}
