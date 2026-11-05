package com.trinity.banking.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.trinity.banking.domain.model.Transaction;

public interface TransactionUseCase {

    Transaction deposit(String accountNumber, BigDecimal amount, String description);

    Transaction withdraw(String accountNumber, BigDecimal amount, String description);

    /** Devuelve dos movimientos: [0] débito en la cuenta origen, [1] crédito en la cuenta destino. */
    List<Transaction> transfer(String originAccountNumber, String destinationAccountNumber,
                               BigDecimal amount, String description);
}
