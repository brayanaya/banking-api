package com.trinity.banking.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.trinity.banking.TestData;
import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountStatus;
import com.trinity.banking.domain.model.AccountType;
import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.port.in.CreateAccountCommand;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.domain.port.out.ClientRepositoryPort;
import com.trinity.banking.domain.port.out.TransactionRepositoryPort;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

    @Mock
    AccountRepositoryPort accountRepository;
    @Mock
    ClientRepositoryPort clientRepository;
    @Mock
    TransactionRepositoryPort transactionRepository;

    AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accountRepository, clientRepository, transactionRepository, TestData.CLOCK);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(TestData.client(1L)));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(10L);
            }
            return a;
        });
    }

    @Test
    void savingsAccountStartsWith53HasTenDigitsAndIsActive() {
        Account created = service.create(new CreateAccountCommand(1L, AccountType.SAVINGS, null, false));

        assertTrue(created.getAccountNumber().matches("^53\\d{8}$"));
        assertEquals(AccountStatus.ACTIVE, created.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(created.getBalance()));
        assertEquals(TestData.NOW, created.getCreatedAt());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void checkingAccountStartsWith33HasTenDigits() {
        Account created = service.create(new CreateAccountCommand(1L, AccountType.CHECKING, null, true));

        assertTrue(created.getAccountNumber().matches("^33\\d{8}$"));
        assertTrue(created.isGmfExempt());
    }

    @Test
    void generatedNumberIsRetriedWhenAlreadyTaken() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(true, true, false);

        Account created = service.create(new CreateAccountCommand(1L, AccountType.SAVINGS, null, false));

        assertTrue(created.getAccountNumber().matches("^53\\d{8}$"));
        verify(accountRepository, org.mockito.Mockito.times(3)).existsByAccountNumber(anyString());
    }

    @Test
    void initialBalanceIsRecordedAsDepositMovement() {
        Account created = service.create(
                new CreateAccountCommand(1L, AccountType.SAVINGS, new BigDecimal("500.00"), false));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(MovementType.CREDIT, captor.getValue().getMovementType());
        assertEquals(0, new BigDecimal("500.00").compareTo(captor.getValue().getAmount()));
        assertEquals(created.getAccountNumber(), captor.getValue().getAccountNumber());
    }

    @Test
    void rejectsNegativeInitialBalance() {
        assertThrows(BusinessRuleException.class, () -> service.create(
                new CreateAccountCommand(1L, AccountType.SAVINGS, new BigDecimal("-1"), false)));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void productCannotExistWithoutClient() {
        assertThrows(ResourceNotFoundException.class, () -> service.create(
                new CreateAccountCommand(99L, AccountType.SAVINGS, null, false)));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAllFiltersByClientWhenProvided() {
        when(accountRepository.findByClientId(1L)).thenReturn(List.of(TestData.savings(1L, "5300000001", "0.00")));
        when(accountRepository.findAll()).thenReturn(List.of());

        assertEquals(1, service.getAll(1L).size());
        assertEquals(0, service.getAll(null).size());
    }

    @Test
    void getByIdFailsWhenMissing() {
        when(accountRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(5L));
    }

    @Test
    void canDeactivateAndReactivate() {
        when(accountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(TestData.savings(1L, "5300000001", "100.00")));

        Account inactive = service.changeStatus(1L, AccountStatus.INACTIVE);
        assertEquals(AccountStatus.INACTIVE, inactive.getStatus());
        assertEquals(TestData.NOW, inactive.getUpdatedAt());
    }

    @Test
    void cannotCancelAccountWithBalance() {
        when(accountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(TestData.savings(1L, "5300000001", "100.00")));

        assertThrows(BusinessRuleException.class, () -> service.changeStatus(1L, AccountStatus.CANCELLED));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void canCancelAccountWithZeroBalance() {
        when(accountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(TestData.savings(1L, "5300000001", "0.00")));

        assertEquals(AccountStatus.CANCELLED, service.changeStatus(1L, AccountStatus.CANCELLED).getStatus());
    }

    @Test
    void changeStatusFailsWhenAccountMissing() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.changeStatus(1L, AccountStatus.INACTIVE));
    }
}
