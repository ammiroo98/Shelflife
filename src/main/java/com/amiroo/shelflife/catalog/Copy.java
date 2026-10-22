package com.amiroo.shelflife.catalog;

import java.util.Objects;
import java.util.UUID;

public class Copy {

    private final UUID id;
    private final Item item;

    public Copy(UUID id, Item item) {
        this.id = Objects.requireNonNull(id, "id");
        this.item = Objects.requireNonNull(item, "item");
    }

    public UUID getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }
}
