package com.amiroo.shelflife.lending;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.Item;
import com.amiroo.shelflife.membership.Member;
import com.amiroo.shelflife.membership.MemberStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckoutServiceTest {

    private final LoanRepository repository = mock(LoanRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T02:00:00Z"),
            ZoneId.of("America/Toronto"));
    private final CheckoutService service = new CheckoutService(repository, clock, 2, 14);
    private final Member member = new Member(UUID.randomUUID(), "Alex");
    private final Item item = new Item(UUID.randomUUID(), "The Hobbit");
    private final Copy copy = new Copy(UUID.randomUUID(), item);

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    void eligibleMemberChecksOutAvailableCopyAndReceivesPersistedLoan(long activeLoans) {
        when(repository.countByMember_IdAndReturnedOnIsNull(member.getId())).thenReturn(activeLoans);
        when(repository.existsByCopy_IdAndReturnedOnIsNull(copy.getId())).thenReturn(false);
        // JPA save may return a different managed instance when merging an assigned ID.
        when(repository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            return new Loan(loan.getId(), loan.getCopy(), loan.getMember(),
                    loan.getBorrowedOn(), loan.getDueOn(), loan.getReturnedOn());
        });

        Loan result = service.checkout(member, copy);

        ArgumentCaptor<Loan> captured = ArgumentCaptor.forClass(Loan.class);
        verify(repository).save(captured.capture());
        Loan submitted = captured.getValue();
        assertThat(submitted.getId()).isNotNull();
        assertThat(submitted.getMember()).isSameAs(member);
        assertThat(submitted.getCopy()).isSameAs(copy);
        assertThat(submitted.isActive()).isTrue();
        assertThat(submitted.getReturnedOn()).isNull();
        assertThat(submitted.getBorrowedOn()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(submitted.getDueOn()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(result).isNotSameAs(submitted);
        assertThat(result.getId()).isEqualTo(submitted.getId());
        assertThat(result.getBorrowedOn()).isEqualTo(submitted.getBorrowedOn());
    }

    @Test
    void suspendedMemberIsRejectedBeforeConsultingStoredLoans() {
        Member suspended = new Member(UUID.randomUUID(), "Alex", MemberStatus.SUSPENDED);

        assertThatThrownBy(() -> service.checkout(suspended, copy))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Suspended members cannot borrow.");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(longs = {2, 3})
    void storedCountAtOrAboveLimitRejectsCheckoutBeforeCopyCheck(long activeLoans) {
        when(repository.countByMember_IdAndReturnedOnIsNull(member.getId())).thenReturn(activeLoans);

        assertThatThrownBy(() -> service.checkout(member, copy))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Member has reached the active loan limit of 2.");
        verify(repository, never()).existsByCopy_IdAndReturnedOnIsNull(any(UUID.class));
        verify(repository, never()).save(any(Loan.class));
    }

    @Test
    void storedActiveLoanForCopyRejectsCheckoutWithoutSaving() {
        when(repository.countByMember_IdAndReturnedOnIsNull(member.getId())).thenReturn(0L);
        when(repository.existsByCopy_IdAndReturnedOnIsNull(copy.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.checkout(member, copy))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Copy already has an active loan.");
        verify(repository, never()).save(any(Loan.class));
    }

    @Test
    void checkoutUsesConfiguredLoanDuration() {
        CheckoutService sevenDayService = new CheckoutService(repository, clock, 1, 7);
        when(repository.countByMember_IdAndReturnedOnIsNull(member.getId())).thenReturn(0L);
        when(repository.existsByCopy_IdAndReturnedOnIsNull(copy.getId())).thenReturn(false);
        when(repository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan loan = sevenDayService.checkout(member, copy);

        assertThat(loan.getDueOn()).isEqualTo(LocalDate.of(2026, 10, 29));
    }

    @ParameterizedTest
    @CsvSource({"0, 14", "-1, 14", "2, 0", "2, -1"})
    void limitsAndDurationMustBePositive(int maxActiveLoans, int loanDurationDays) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CheckoutService(repository, clock, maxActiveLoans, loanDurationDays));
    }
}
