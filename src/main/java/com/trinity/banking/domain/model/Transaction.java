package com.trinity.banking.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un movimiento financiero sobre una cuenta. Una transferencia genera dos movimientos
 * (débito en la cuenta origen y crédito en la destino) que comparten la misma {@code reference}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private Long id;
    private String reference;
    private TransactionType transactionType;
    private MovementType movementType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private BigDecimal availableBalanceAfter;
    private Long accountId;
    private String accountNumber;
    private String relatedAccountNumber;
    private String description;
    private LocalDateTime createdAt;
}
