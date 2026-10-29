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

    @Autowired
    private LoanRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReloadsActiveLoanWithItsCopyItemAndMember() {
        Item item = new Item(UUID.randomUUID(), "The Hobbit");
        entityManager.persistAndFlush(item);
        Copy copy = new Copy(UUID.randomUUID(), item);
        entityManager.persistAndFlush(copy);
        Member member = new Member(UUID.randomUUID(), "Alex");
        entityManager.persistAndFlush(member);
        UUID id = UUID.randomUUID();
        LocalDate borrowedOn = LocalDate.of(2026, 10, 22);
        LocalDate dueOn = LocalDate.of(2026, 11, 5);
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
