package com.amiroo.shelflife.membership;

import java.util.UUID;

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
class MemberRepositoryTest {

    @Autowired
    private MemberRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReloadsSuspendedMemberWithApplicationAssignedUuid() {
        UUID id = UUID.randomUUID();
        Member member = new Member(id, "Alex", MemberStatus.SUSPENDED);

        Member saved = repository.saveAndFlush(member);
        entityManager.clear();

        Member reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded).isNotSameAs(saved).isNotSameAs(member);
        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getName()).isEqualTo("Alex");
        assertThat(reloaded.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }
}
