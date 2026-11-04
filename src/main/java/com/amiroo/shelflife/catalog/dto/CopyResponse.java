package com.amiroo.shelflife.catalog.dto;

import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;

public record CopyResponse(UUID id, UUID itemId) {

    public static CopyResponse from(Copy copy) {
        return new CopyResponse(copy.getId(), copy.getItem().getId());
    }
}
