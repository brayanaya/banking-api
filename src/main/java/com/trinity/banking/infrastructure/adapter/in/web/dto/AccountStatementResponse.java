package com.trinity.banking.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.trinity.banking.domain.model.AccountStatement;

public record AccountStatementResponse(
        AccountResponse account,
        LocalDate from,
        LocalDate to,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        List<TransactionResponse> movements) {

    public static AccountStatementResponse from(AccountStatement s) {
        return new AccountStatementResponse(AccountResponse.from(s.account()), s.from(), s.to(),
                s.totalCredits(), s.totalDebits(),
                s.movements().stream().map(TransactionResponse::from).toList());
    }
}
