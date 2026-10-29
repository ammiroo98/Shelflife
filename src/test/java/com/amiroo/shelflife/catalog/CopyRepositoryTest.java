package com.amiroo.shelflife.catalog;

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
class CopyRepositoryTest {

    @Autowired
    private CopyRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReloadsCopyWithItsItemRelationship() {
        UUID itemId = UUID.randomUUID();
        Item item = new Item(itemId, "The Hobbit");
        entityManager.persistAndFlush(item);
        UUID copyId = UUID.randomUUID();

        Copy saved = repository.saveAndFlush(new Copy(copyId, item));
        entityManager.clear();

        Copy reloaded = repository.findById(copyId).orElseThrow();
        assertThat(reloaded).isNotSameAs(saved);
        assertThat(reloaded.getId()).isEqualTo(copyId);
        assertThat(reloaded.getItem().getId()).isEqualTo(itemId);
        assertThat(reloaded.getItem().getTitle()).isEqualTo("The Hobbit");
    }
}
