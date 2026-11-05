package com.trinity.banking.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.trinity.banking.TestData;
import com.trinity.banking.domain.exception.BusinessRuleException;

class AccountTest {

    @Test
    void savingsAvailableBalanceEqualsBalance() {
        Account a = TestData.savings(1L, "5300000001", "1000.00");
        assertEquals(0, new BigDecimal("1000.00").compareTo(a.getAvailableBalance()));
    }

    @Test
    void checkingAvailableBalanceIncludesOverdraft() {
        Account a = TestData.checking(1L, "3300000001", "1000.00");
        assertEquals(0, new BigDecimal("1001000.00").compareTo(a.getAvailableBalance()));
    }

    @Test
    void savingsCannotGoBelowZero() {
        Account a = TestData.savings(1L, "5300000001", "100.00");
        assertThrows(BusinessRuleException.class, () -> a.debit(new BigDecimal("100.01")));
        assertEquals(0, new BigDecimal("100.00").compareTo(a.getBalance()));
    }

    @Test
    void savingsCanBeEmptiedExactly() {
        Account a = TestData.savings(1L, "5300000001", "100.00");
        a.debit(new BigDecimal("100.00"));
        assertEquals(0, BigDecimal.ZERO.compareTo(a.getBalance()));
    }

    @Test
    void checkingCanUseOverdraftButNotBeyond() {
        Account a = TestData.checking(1L, "3300000001", "0.00");
        a.debit(new BigDecimal("1000000.00"));
        assertEquals(0, new BigDecimal("-1000000.00").compareTo(a.getBalance()));
        assertThrows(BusinessRuleException.class, () -> a.debit(new BigDecimal("0.01")));
    }

    @Test
    void creditUpdatesBalanceAndAvailableBalance() {
        Account a = TestData.savings(1L, "5300000001", "100.00");
        a.credit(new BigDecimal("50.50"));
        assertEquals(0, new BigDecimal("150.50").compareTo(a.getBalance()));
        assertEquals(0, new BigDecimal("150.50").compareTo(a.getAvailableBalance()));
    }

    @Test
    void rejectsNonPositiveAmounts() {
        Account a = TestData.savings(1L, "5300000001", "100.00");
        assertThrows(BusinessRuleException.class, () -> a.credit(BigDecimal.ZERO));
        assertThrows(BusinessRuleException.class, () -> a.debit(new BigDecimal("-1")));
    }

    @Test
    void inactiveAccountCannotTransact() {
        Account a = TestData.savings(1L, "5300000001", "100.00");
        a.changeStatus(AccountStatus.INACTIVE);
        assertThrows(BusinessRuleException.class, () -> a.credit(BigDecimal.TEN));
        assertThrows(BusinessRuleException.class, () -> a.debit(BigDecimal.TEN));
    }

    @Test
    void canToggleBetweenActiveAndInactive() {
        Account a = TestData.savings(1L, "5300000001", "100.00");
        a.changeStatus(AccountStatus.INACTIVE);
        a.changeStatus(AccountStatus.ACTIVE);
        assertEquals(AccountStatus.ACTIVE, a.getStatus());
    }

    @Test
    void cannotCancelWithBalance() {
        Account a = TestData.savings(1L, "5300000001", "0.01");
        assertThrows(BusinessRuleException.class, () -> a.changeStatus(AccountStatus.CANCELLED));
    }

    @Test
    void canCancelWithZeroBalanceAndCannotReopen() {
        Account a = TestData.savings(1L, "5300000001", "0.00");
        a.changeStatus(AccountStatus.CANCELLED);
        assertEquals(AccountStatus.CANCELLED, a.getStatus());
        assertThrows(BusinessRuleException.class, () -> a.changeStatus(AccountStatus.ACTIVE));
    }

    @Test
    void clientAgeBoundary() {
        LocalDate today = LocalDate.of(2026, 11, 3);
        assertTrue(Client.isAdult(LocalDate.of(2008, 11, 3), today));   // cumple 18 hoy
        assertFalse(Client.isAdult(LocalDate.of(2008, 11, 4), today));  // cumple 18 mañana
        assertFalse(Client.isAdult(null, today));
    }
}
