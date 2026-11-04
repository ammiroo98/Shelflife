package com.amiroo.shelflife.catalog.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateCopyRequest(@NotNull UUID itemId) {
}
