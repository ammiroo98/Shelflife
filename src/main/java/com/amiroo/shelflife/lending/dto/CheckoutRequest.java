package com.amiroo.shelflife.lending.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull UUID memberId, @NotNull UUID copyId) {
}
