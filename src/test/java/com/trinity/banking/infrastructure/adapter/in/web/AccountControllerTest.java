package com.trinity.banking.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.trinity.banking.TestData;
import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountStatement;
import com.trinity.banking.domain.model.AccountStatus;
import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.model.TransactionType;
import com.trinity.banking.domain.port.in.AccountStatementUseCase;
import com.trinity.banking.domain.port.in.AccountUseCase;
import com.trinity.banking.domain.port.in.CreateAccountCommand;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AccountUseCase accountUseCase;

    @MockBean
    AccountStatementUseCase statementUseCase;

    @Test
    void createReturns201() throws Exception {
        when(accountUseCase.create(any(CreateAccountCommand.class)))
                .thenReturn(TestData.savings(10L, "5312345678", "0.00"));

        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"type\":\"SAVINGS\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/accounts/10")))
                .andExpect(jsonPath("$.accountNumber").value("5312345678"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.type").value("SAVINGS"));
    }

    @Test
    void createRejectsInvalidType() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"type\":\"CRYPTO\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountUseCase);
    }

    @Test
    void createRejectsMissingClient() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SAVINGS\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountUseCase);
    }

    @Test
    void createRejectsNegativeInitialBalance() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"type\":\"SAVINGS\",\"initialBalance\":-5}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountUseCase);
    }

    @Test
    void createForUnknownClientReturns404() throws Exception {
        when(accountUseCase.create(any(CreateAccountCommand.class)))
                .thenThrow(new ResourceNotFoundException("Cliente con id 99 no encontrado"));

        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":99,\"type\":\"CHECKING\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listCanFilterByClient() throws Exception {
        when(accountUseCase.getAll(1L)).thenReturn(List.of(TestData.savings(1L, "5300000001", "10.00")));

        mockMvc.perform(get("/api/v1/accounts").param("clientId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        verify(accountUseCase).getAll(1L);
    }

    @Test
    void listWithoutFilterPassesNull() throws Exception {
        when(accountUseCase.getAll(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isOk());
        verify(accountUseCase).getAll(null);
    }

    @Test
    void getByIdReturnsAccount() throws Exception {
        when(accountUseCase.getById(1L)).thenReturn(TestData.checking(1L, "3300000001", "10.00"));

        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CHECKING"));
    }

    @Test
    void changeStatusReturnsUpdatedAccount() throws Exception {
        Account inactive = TestData.savings(1L, "5300000001", "10.00");
        inactive.setStatus(AccountStatus.INACTIVE);
        when(accountUseCase.changeStatus(1L, AccountStatus.INACTIVE)).thenReturn(inactive);

        mockMvc.perform(patch("/api/v1/accounts/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void cancelWithBalanceReturns422() throws Exception {
        when(accountUseCase.changeStatus(1L, AccountStatus.CANCELLED))
                .thenThrow(new BusinessRuleException("Solo se pueden cancelar cuentas con saldo igual a $0"));

        mockMvc.perform(patch("/api/v1/accounts/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changeStatusRejectsMissingStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/accounts/1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statementReturnsMovementsAndTotals() throws Exception {
        Transaction credit = Transaction.builder().id(1L).reference("ref").transactionType(TransactionType.DEPOSIT)
                .movementType(MovementType.CREDIT).amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("100.00")).availableBalanceAfter(new BigDecimal("100.00"))
                .accountNumber("5300000001").createdAt(TestData.NOW).build();
        AccountStatement statement = new AccountStatement(TestData.savings(1L, "5300000001", "100.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), List.of(credit),
                new BigDecimal("100.00"), BigDecimal.ZERO);
        when(statementUseCase.getStatement(eq(1L), eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 3))))
                .thenReturn(statement);

        mockMvc.perform(get("/api/v1/accounts/1/statement").param("from", "2026-11-01").param("to", "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.accountNumber").value("5300000001"))
                .andExpect(jsonPath("$.totalCredits").value(100.00))
                .andExpect(jsonPath("$.movements", hasSize(1)))
                .andExpect(jsonPath("$.movements[0].movementType").value("CREDIT"));
    }

    @Test
    void statementRejectsInvalidDate() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/statement").param("from", "03-11-2026"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(statementUseCase);
    }
}
