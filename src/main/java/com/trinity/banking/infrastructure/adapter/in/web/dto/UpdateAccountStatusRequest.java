package com.trinity.banking.infrastructure.adapter.in.web.dto;

import com.trinity.banking.domain.model.AccountStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "El estado es obligatorio (ACTIVE, INACTIVE o CANCELLED)")
        AccountStatus status) {
}
