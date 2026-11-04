package com.amiroo.shelflife.membership.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(@NotBlank String name) {
}
