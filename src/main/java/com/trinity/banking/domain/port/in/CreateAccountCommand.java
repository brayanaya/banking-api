package com.trinity.banking.domain.port.in;

import java.math.BigDecimal;

import com.trinity.banking.domain.model.AccountType;

public record CreateAccountCommand(
        Long clientId,
        AccountType type,
        BigDecimal initialBalance,
        boolean gmfExempt) {
}
