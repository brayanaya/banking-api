package com.trinity.banking.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountStatus;
import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.model.TransactionType;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.domain.port.out.TransactionRepositoryPort;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock
    AccountRepositoryPort accountRepository;
    @Mock
    TransactionRepositoryPort transactionRepository;

    TransactionService service;

    Account savings;
    Account checking;

    @BeforeEach
    void setUp() {
        service = new TransactionService(accountRepository, transactionRepository, TestData.CLOCK);
        savings = TestData.savings(1L, "5300000001", "1000.00");
        checking = TestData.checking(2L, "3300000001", "500.00");

        when(accountRepository.findByAccountNumberForUpdate("5300000001")).thenReturn(Optional.of(savings));
        when(accountRepository.findByAccountNumberForUpdate("3300000001")).thenReturn(Optional.of(checking));
        when(accountRepository.findByAccountNumberForUpdate("9999999999")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void depositIncreasesBalanceAndAvailableBalance() {
        Transaction tx = service.deposit("5300000001", new BigDecimal("250.00"), "Pago");

        assertEquals(0, new BigDecimal("1250.00").compareTo(savings.getBalance()));
        assertEquals(0, new BigDecimal("1250.00").compareTo(savings.getAvailableBalance()));
        assertEquals(TransactionType.DEPOSIT, tx.getTransactionType());
        assertEquals(MovementType.CREDIT, tx.getMovementType());
        assertEquals(0, new BigDecimal("1250.00").compareTo(tx.getBalanceAfter()));
        assertEquals(TestData.NOW, savings.getUpdatedAt());
    }

    @Test
    void withdrawalDecreasesBalance() {
        Transaction tx = service.withdraw("5300000001", new BigDecimal("400.00"), null);

        assertEquals(0, new BigDecimal("600.00").compareTo(savings.getBalance()));
        assertEquals(TransactionType.WITHDRAWAL, tx.getTransactionType());
        assertEquals(MovementType.DEBIT, tx.getMovementType());
    }

    @Test
    void withdrawalFailsWithInsufficientFundsAndChangesNothing() {
        assertThrows(BusinessRuleException.class, () -> service.withdraw("5300000001", new BigDecimal("1000.01"), null));

        assertEquals(0, new BigDecimal("1000.00").compareTo(savings.getBalance()));
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void operationsFailOnInactiveAccounts() {
        savings.setStatus(AccountStatus.INACTIVE);
        assertThrows(BusinessRuleException.class, () -> service.deposit("5300000001", BigDecimal.TEN, null));
        assertThrows(BusinessRuleException.class, () -> service.withdraw("5300000001", BigDecimal.TEN, null));
    }

    @Test
    void rejectsZeroOrNegativeAmounts() {
        assertThrows(BusinessRuleException.class, () -> service.deposit("5300000001", BigDecimal.ZERO, null));
        assertThrows(BusinessRuleException.class, () -> service.withdraw("5300000001", new BigDecimal("-5"), null));
        assertThrows(BusinessRuleException.class,
                () -> service.transfer("5300000001", "3300000001", BigDecimal.ZERO, null));
    }

    @Test
    void failsWhenAccountDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> service.deposit("9999999999", BigDecimal.TEN, null));
    }

    @Test
    void transferGeneratesDebitAndCreditSharingReference() {
        List<Transaction> result = service.transfer("5300000001", "3300000001", new BigDecimal("300.00"), "Pago deuda");

        assertEquals(2, result.size());
        Transaction debit = result.get(0);
        Transaction credit = result.get(1);

        assertEquals(MovementType.DEBIT, debit.getMovementType());
        assertEquals("5300000001", debit.getAccountNumber());
        assertEquals("3300000001", debit.getRelatedAccountNumber());

        assertEquals(MovementType.CREDIT, credit.getMovementType());
        assertEquals("3300000001", credit.getAccountNumber());
        assertEquals("5300000001", credit.getRelatedAccountNumber());

        assertEquals(debit.getReference(), credit.getReference());
        assertEquals(TransactionType.TRANSFER, debit.getTransactionType());
        assertEquals(TransactionType.TRANSFER, credit.getTransactionType());

        assertEquals(0, new BigDecimal("700.00").compareTo(savings.getBalance()));
        assertEquals(0, new BigDecimal("800.00").compareTo(checking.getBalance()));
    }

    @Test
    void transferWorksInTheOppositeLockingOrderToo() {
        // origen con número "mayor" que el destino: el servicio igual debe operar correctamente
        List<Transaction> result = service.transfer("5300000001", "3300000001", new BigDecimal("10.00"), null);
        List<Transaction> reverse = service.transfer("3300000001", "5300000001", new BigDecimal("10.00"), null);

        assertEquals("5300000001", result.get(0).getAccountNumber());
        assertEquals("3300000001", reverse.get(0).getAccountNumber());
        assertEquals(0, new BigDecimal("1000.00").compareTo(savings.getBalance()));
        assertEquals(0, new BigDecimal("500.00").compareTo(checking.getBalance()));
    }

    @Test
    void transferFailsWithInsufficientFundsAndPersistsNothing() {
        assertThrows(BusinessRuleException.class,
                () -> service.transfer("5300000001", "3300000001", new BigDecimal("5000.00"), null));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        assertEquals(0, new BigDecimal("500.00").compareTo(checking.getBalance()));
    }

    @Test
    void transferFailsWhenDestinationDoesNotExist() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.transfer("5300000001", "9999999999", BigDecimal.TEN, null));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferFailsWhenOriginAndDestinationAreTheSame() {
        assertThrows(BusinessRuleException.class,
                () -> service.transfer("5300000001", "5300000001", BigDecimal.TEN, null));
    }

    @Test
    void transferFailsWhenDestinationIsInactive() {
        checking.setStatus(AccountStatus.INACTIVE);
        assertThrows(BusinessRuleException.class,
                () -> service.transfer("5300000001", "3300000001", BigDecimal.TEN, null));
    }
}
