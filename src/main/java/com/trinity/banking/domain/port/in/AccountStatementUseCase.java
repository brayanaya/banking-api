package com.trinity.banking.domain.port.in;

import java.time.LocalDate;

import com.trinity.banking.domain.model.AccountStatement;

public interface AccountStatementUseCase {

    /** Si {@code from}/{@code to} son nulos se usa el mes en curso. */
    AccountStatement getStatement(Long accountId, LocalDate from, LocalDate to);
}
