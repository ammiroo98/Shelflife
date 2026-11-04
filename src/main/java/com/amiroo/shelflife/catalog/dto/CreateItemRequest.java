package com.amiroo.shelflife.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateItemRequest(@NotBlank String title) {
}
