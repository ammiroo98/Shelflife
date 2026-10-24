package com.amiroo.shelflife.membership;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MemberTest {

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void nameMustNotBeBlank(String name) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Member(UUID.randomUUID(), name))
                .withMessage("name must not be blank.");
    }

    @Test
    void nameMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Member(UUID.randomUUID(), null))
                .withMessage("name");
    }

    @Test
    void statusMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Member(UUID.randomUUID(), "Alex", null))
                .withMessage("status");
    }

    @Test
    void nonBlankNameIsPreserved() {
        Member member = new Member(UUID.randomUUID(), " Alex ");

        assertThat(member.getName()).isEqualTo(" Alex ");
    }
}
