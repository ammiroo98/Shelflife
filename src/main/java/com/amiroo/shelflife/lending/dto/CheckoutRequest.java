package com.amiroo.shelflife.lending.dto;

import java.util.UUID;

public record CheckoutRequest(UUID memberId, UUID copyId) {
}
