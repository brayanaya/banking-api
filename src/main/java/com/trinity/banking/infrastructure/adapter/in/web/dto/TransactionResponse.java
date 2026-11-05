package com.trinity.banking.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.model.TransactionType;

public record TransactionResponse(
        Long id,
        String reference,
        TransactionType transactionType,
        MovementType movementType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        BigDecimal availableBalanceAfter,
        String accountNumber,
        String relatedAccountNumber,
        String description,
        LocalDateTime createdAt) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getReference(), t.getTransactionType(), t.getMovementType(),
                t.getAmount(), t.getBalanceAfter(), t.getAvailableBalanceAfter(), t.getAccountNumber(),
                t.getRelatedAccountNumber(), t.getDescription(), t.getCreatedAt());
    }
}
