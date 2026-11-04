package com.amiroo.shelflife.catalog.dto;

import java.util.UUID;

import com.amiroo.shelflife.catalog.Item;

public record ItemResponse(UUID id, String title) {

    public static ItemResponse from(Item item) {

        return new ItemResponse(item.getId(), item.getTitle());
    }
}
