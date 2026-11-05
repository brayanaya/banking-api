package com.trinity.banking.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountStatus;
import com.trinity.banking.domain.model.AccountType;

public record AccountResponse(
        Long id,
        AccountType type,
        String accountNumber,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal availableBalance,
        boolean gmfExempt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long clientId) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getType(), a.getAccountNumber(), a.getStatus(),
                a.getBalance(), a.getAvailableBalance(), a.isGmfExempt(),
                a.getCreatedAt(), a.getUpdatedAt(), a.getClientId());
    }
}
