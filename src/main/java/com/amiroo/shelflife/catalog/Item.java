package com.amiroo.shelflife.catalog;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    // Required by JPA for loading entities; normal callers use the validated constructor.
    protected Item() {
    }

    public Item(UUID id, String title) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank.");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
