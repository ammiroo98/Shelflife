package com.amiroo.shelflife.lending;

import java.time.LocalDate;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.Item;
import com.amiroo.shelflife.membership.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "SHELFLIFE_POSTGRES_TESTS", matches = "true")
class LoanRepositoryTest {

    private final LocalDate borrowedOn = LocalDate.of(2026, 10, 22);
    private final LocalDate dueOn = LocalDate.of(2026, 11, 5);

    @Autowired
    private LoanRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void countsOnlyActiveLoansForTheSpecifiedMemberId() {
        Item item = new Item(UUID.randomUUID(), "The Hobbit");
        entityManager.persist(item);
        Member member = new Member(UUID.randomUUID(), "Alex");
        Member otherMember = new Member(UUID.randomUUID(), "Sam");
        Member returnedOnlyMember = new Member(UUID.randomUUID(), "Taylor");
        Member memberWithoutLoans = new Member(UUID.randomUUID(), "Robin");
        entityManager.persist(member);
        entityManager.persist(otherMember);
        entityManager.persist(returnedOnlyMember);
        entityManager.persist(memberWithoutLoans);
        persistLoan(item, member, null);
        persistLoan(item, member, null);
        persistLoan(item, member, borrowedOn.plusDays(1));
        persistLoan(item, otherMember, null);
        persistLoan(item, returnedOnlyMember, borrowedOn.plusDays(1));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.countByMember_IdAndReturnedOnIsNull(member.getId())).isEqualTo(2);
        assertThat(repository.countByMember_IdAndReturnedOnIsNull(otherMember.getId())).isEqualTo(1);
        assertThat(repository.countByMember_IdAndReturnedOnIsNull(returnedOnlyMember.getId())).isZero();
        assertThat(repository.countByMember_IdAndReturnedOnIsNull(memberWithoutLoans.getId())).isZero();
    }

    @Test
    void detectsActiveLoanOnlyForTheSpecifiedCopyId() {
        Item item = new Item(UUID.randomUUID(), "The Hobbit");
        entityManager.persist(item);
        Member member = new Member(UUID.randomUUID(), "Alex");
        entityManager.persist(member);
        Copy borrowedCopy = persistLoan(item, member, null);
        entityManager.persist(new Loan(UUID.randomUUID(), borrowedCopy, member,
                borrowedOn.minusDays(14), borrowedOn, borrowedOn.minusDays(1)));
        Copy returnedCopy = persistLoan(item, member, borrowedOn.plusDays(1));
        Copy copyWithoutLoans = new Copy(UUID.randomUUID(), item);
        entityManager.persist(copyWithoutLoans);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.existsByCopy_IdAndReturnedOnIsNull(borrowedCopy.getId())).isTrue();
        assertThat(repository.existsByCopy_IdAndReturnedOnIsNull(returnedCopy.getId())).isFalse();
        assertThat(repository.existsByCopy_IdAndReturnedOnIsNull(copyWithoutLoans.getId())).isFalse();
    }

    private Copy persistLoan(Item item, Member member, LocalDate returnedOn) {
        Copy copy = new Copy(UUID.randomUUID(), item);
        entityManager.persist(copy);
        entityManager.persist(new Loan(UUID.randomUUID(), copy, member, borrowedOn, dueOn, returnedOn));
        return copy;
    }

    @Test
    void savesAndReloadsActiveLoanWithItsCopyItemAndMember() {
        Item item = new Item(UUID.randomUUID(), "The Hobbit");
        entityManager.persistAndFlush(item);
        Copy copy = new Copy(UUID.randomUUID(), item);
        entityManager.persistAndFlush(copy);
        Member member = new Member(UUID.randomUUID(), "Alex");
        entityManager.persistAndFlush(member);
        UUID id = UUID.randomUUID();
        Loan loan = new Loan(id, copy, member, borrowedOn, dueOn, null);

        Loan saved = repository.saveAndFlush(loan);
        entityManager.clear();

        Loan reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded).isNotSameAs(saved).isNotSameAs(loan);
        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getCopy()).isNotSameAs(copy);
        assertThat(reloaded.getCopy().getId()).isEqualTo(copy.getId());
        assertThat(reloaded.getCopy().getItem()).isNotSameAs(item);
        assertThat(reloaded.getCopy().getItem().getId()).isEqualTo(item.getId());
        assertThat(reloaded.getCopy().getItem().getTitle()).isEqualTo("The Hobbit");
        assertThat(reloaded.getMember()).isNotSameAs(member);
        assertThat(reloaded.getMember().getId()).isEqualTo(member.getId());
        assertThat(reloaded.getMember().getName()).isEqualTo("Alex");
        assertThat(reloaded.getBorrowedOn()).isEqualTo(borrowedOn);
        assertThat(reloaded.getDueOn()).isEqualTo(dueOn);
        assertThat(reloaded.getReturnedOn()).isNull();
        assertThat(reloaded.isActive()).isTrue();
    }
}
