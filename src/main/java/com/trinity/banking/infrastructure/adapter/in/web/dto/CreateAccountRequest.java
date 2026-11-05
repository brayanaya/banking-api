package com.trinity.banking.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;

import com.trinity.banking.domain.model.AccountType;
import com.trinity.banking.domain.port.in.CreateAccountCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clientId,

        @NotNull(message = "El tipo de cuenta es obligatorio (CHECKING o SAVINGS)")
        AccountType type,

        @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser menor a 0")
        @Digits(integer = 17, fraction = 2, message = "El saldo inicial admite máximo 2 decimales")
        BigDecimal initialBalance,

        Boolean gmfExempt) {

    public CreateAccountCommand toCommand() {
        return new CreateAccountCommand(clientId, type, initialBalance, Boolean.TRUE.equals(gmfExempt));
    }
}
