package com.amiroo.shelflife.catalog;

import java.util.Objects;
import java.util.UUID;

public class Item {

    private final UUID id;
    private final String title;

    public Item(UUID id, String title) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
