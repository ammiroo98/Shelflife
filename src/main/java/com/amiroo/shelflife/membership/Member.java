package com.amiroo.shelflife.membership;

import java.util.Objects;
import java.util.UUID;

public class Member {

    private final UUID id;
    private final String name;

    public Member(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
