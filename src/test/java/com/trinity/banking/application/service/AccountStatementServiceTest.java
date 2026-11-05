package com.trinity.banking.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.trinity.banking.TestData;
import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.AccountStatement;
import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.domain.port.out.TransactionRepositoryPort;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountStatementServiceTest {

    @Mock
    AccountRepositoryPort accountRepository;
    @Mock
    TransactionRepositoryPort transactionRepository;

    AccountStatementService service;

    @BeforeEach
    void setUp() {
        service = new AccountStatementService(accountRepository, transactionRepository, TestData.CLOCK);
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(TestData.savings(1L, "5300000001", "700.00")));
    }

    private Transaction tx(MovementType type, String amount) {
        return Transaction.builder().movementType(type).amount(new BigDecimal(amount)).build();
    }

    @Test
    void defaultsToCurrentMonthAndSumsTotals() {
        when(transactionRepository.findByAccountIdBetween(eq(1L), any(), any())).thenReturn(List.of(
                tx(MovementType.CREDIT, "1000.00"), tx(MovementType.DEBIT, "300.00"), tx(MovementType.CREDIT, "50.00")));

        AccountStatement statement = service.getStatement(1L, null, null);

        assertEquals(LocalDate.of(2026, 11, 1), statement.from());
        assertEquals(LocalDate.of(2026, 11, 3), statement.to());
        assertEquals(0, new BigDecimal("1050.00").compareTo(statement.totalCredits()));
        assertEquals(0, new BigDecimal("300.00").compareTo(statement.totalDebits()));
        assertEquals(3, statement.movements().size());

        verify(transactionRepository).findByAccountIdBetween(1L,
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 4, 0, 0));
    }

    @Test
    void usesExplicitRange() {
        when(transactionRepository.findByAccountIdBetween(eq(1L), any(), any())).thenReturn(List.of());

        AccountStatement statement = service.getStatement(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(LocalDate.of(2026, 10, 1), statement.from());
        assertEquals(0, BigDecimal.ZERO.compareTo(statement.totalCredits()));
    }

    @Test
    void rejectsInvertedRange() {
        assertThrows(BusinessRuleException.class,
                () -> service.getStatement(1L, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1)));
    }

    @Test
    void failsWhenAccountMissing() {
        when(accountRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getStatement(9L, null, null));
    }
}
