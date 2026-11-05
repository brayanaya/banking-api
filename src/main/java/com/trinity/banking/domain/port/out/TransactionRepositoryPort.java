package com.trinity.banking.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;

import com.trinity.banking.domain.model.Transaction;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    /** Movimientos de una cuenta en el rango [from, toExclusive), ordenados cronológicamente. */
    List<Transaction> findByAccountIdBetween(Long accountId, LocalDateTime from, LocalDateTime toExclusive);
}
