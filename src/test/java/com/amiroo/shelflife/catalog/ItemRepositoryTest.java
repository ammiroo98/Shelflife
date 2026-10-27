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
class ItemRepositoryTest {

    @Autowired
    private ItemRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReloadsItemWithApplicationAssignedUuid() {
        UUID id = UUID.randomUUID();
        Item item = new Item(id, "The Hobbit");

        Item saved = repository.saveAndFlush(item);
        entityManager.clear();

        Item reloaded = repository.findById(id).orElseThrow();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(reloaded).isNotSameAs(saved);
        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getTitle()).isEqualTo("The Hobbit");
    }
}
