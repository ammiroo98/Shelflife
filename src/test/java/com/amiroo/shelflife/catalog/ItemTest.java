package com.amiroo.shelflife.catalog;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ItemTest {

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void titleMustNotBeBlank(String title) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Item(UUID.randomUUID(), title))
                .withMessage("title must not be blank.");
    }

    @Test
    void titleMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Item(UUID.randomUUID(), null))
                .withMessage("title");
    }

    @Test
    void nonBlankTitleIsPreserved() {
        Item item = new Item(UUID.randomUUID(), " The Hobbit ");

        assertThat(item.getTitle()).isEqualTo(" The Hobbit ");
    }
}
