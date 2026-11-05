package com.trinity.banking.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Estado de cuenta: movimientos de un periodo con sus totales. */
public record AccountStatement(
        Account account,
        LocalDate from,
        LocalDate to,
        List<Transaction> movements,
        BigDecimal totalCredits,
        BigDecimal totalDebits) {
}
