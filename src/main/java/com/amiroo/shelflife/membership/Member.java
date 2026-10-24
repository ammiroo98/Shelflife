package com.amiroo.shelflife.membership;

import java.util.Objects;
import java.util.UUID;

public class Member {

    private final UUID id;
    private final String name;
    private final MemberStatus status;

    public Member(UUID id, String name) {
        this(id, name, MemberStatus.ACTIVE);
    }

    public Member(UUID id, String name, MemberStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank.");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MemberStatus getStatus() {
        return status;
    }
}
