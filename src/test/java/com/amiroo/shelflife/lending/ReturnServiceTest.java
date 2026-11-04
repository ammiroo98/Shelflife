package com.amiroo.shelflife.lending;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.Item;
import com.amiroo.shelflife.membership.Member;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReturnServiceTest {

    private final LoanRepository repository = mock(LoanRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T02:00:00Z"),
            ZoneId.of("America/Toronto"));
    private final ReturnService service = new ReturnService(repository, clock);
    private final Copy copy = new Copy(UUID.randomUUID(), new Item(UUID.randomUUID(), "The Hobbit"));
    private final Member member = new Member(UUID.randomUUID(), "Alex");
    private final LocalDate borrowedOn = LocalDate.of(2026, 10, 22);
    private final LocalDate dueOn = LocalDate.of(2026, 11, 5);

    @Test
    void returnsLoadedLoanUsingClockDateWithoutSaving() {
        Loan loan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, null);
        when(repository.findById(loan.getId())).thenReturn(Optional.of(loan));

        Loan result = service.returnLoan(loan.getId());

        assertThat(result).isSameAs(loan);
        assertThat(result.getReturnedOn()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(result.isActive()).isFalse();
        verify(repository, never()).save(any(Loan.class));
    }

    @Test
    void missingLoanProducesLoanNotFoundException() {
        UUID loanId = UUID.randomUUID();
        when(repository.findById(loanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnLoan(loanId))
                .isInstanceOf(LoanNotFoundException.class)
                .hasMessage("Loan not found: " + loanId);
    }

    @Test
    void alreadyReturnedLoanIsRejectedWithoutOverwritingReturnDate() {
        LocalDate originalReturn = LocalDate.of(2026, 11, 1);
        Loan loan = new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, originalReturn);
        when(repository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.returnLoan(loan.getId()))
                .isInstanceOf(BorrowingException.class)
                .hasMessage("Loan has already been returned.");
        assertThat(loan.getReturnedOn()).isEqualTo(originalReturn);
    }

    @Test
    void nullLoanIdIsRejectedBeforeRepositoryAccess() {
        assertThatNullPointerException().isThrownBy(() -> service.returnLoan(null))
                .withMessage("loanId");
        verifyNoInteractions(repository);
    }

    @Test
    void clockDateBeforeBorrowingIsRejectedByLoanAndLeavesItActive() {
        LocalDate futureBorrowing = LocalDate.of(2026, 11, 3);
        Loan loan = new Loan(UUID.randomUUID(), copy, member,
                futureBorrowing, futureBorrowing.plusDays(14), null);
        when(repository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatIllegalArgumentException().isThrownBy(() -> service.returnLoan(loan.getId()))
                .withMessage("returnedOn must not be before borrowedOn.");
        assertThat(loan.isActive()).isTrue();
        assertThat(loan.getReturnedOn()).isNull();
    }
}
