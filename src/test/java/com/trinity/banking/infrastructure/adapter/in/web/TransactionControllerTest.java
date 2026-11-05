package com.trinity.banking.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.model.TransactionType;
import com.trinity.banking.domain.port.in.TransactionUseCase;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TransactionUseCase transactionUseCase;

    private Transaction tx(TransactionType type, MovementType movement, String account, String related) {
        return Transaction.builder().id(1L).reference("ref-1").transactionType(type).movementType(movement)
                .amount(new BigDecimal("100.00")).balanceAfter(new BigDecimal("900.00"))
                .availableBalanceAfter(new BigDecimal("900.00")).accountNumber(account)
                .relatedAccountNumber(related).createdAt(TestData.NOW).build();
    }

    @Test
    void depositReturns201() throws Exception {
        when(transactionUseCase.deposit(eq("5300000001"), eq(new BigDecimal("100.00")), any()))
                .thenReturn(tx(TransactionType.DEPOSIT, MovementType.CREDIT, "5300000001", null));

        mockMvc.perform(post("/api/v1/transactions/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"5300000001\",\"amount\":100.00,\"description\":\"Pago\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.movementType").value("CREDIT"))
                .andExpect(jsonPath("$.balanceAfter").value(900.00));
    }

    @Test
    void withdrawalReturns201() throws Exception {
        when(transactionUseCase.withdraw(eq("5300000001"), eq(new BigDecimal("100.00")), any()))
                .thenReturn(tx(TransactionType.WITHDRAWAL, MovementType.DEBIT, "5300000001", null));

        mockMvc.perform(post("/api/v1/transactions/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"5300000001\",\"amount\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("DEBIT"));
    }

    @Test
    void withdrawalWithInsufficientFundsReturns422() throws Exception {
        when(transactionUseCase.withdraw(any(), any(), any()))
                .thenThrow(new BusinessRuleException("Fondos insuficientes en la cuenta 5300000001"));

        mockMvc.perform(post("/api/v1/transactions/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"5300000001\",\"amount\":999999.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Fondos insuficientes en la cuenta 5300000001"));
    }

    @Test
    void depositToUnknownAccountReturns404() throws Exception {
        when(transactionUseCase.deposit(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Cuenta 9999999999 no encontrada"));

        mockMvc.perform(post("/api/v1/transactions/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"9999999999\",\"amount\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidAmountsAndAccountNumbers() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"5300000001\",\"amount\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/transactions/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"123\",\"amount\":10}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/transactions/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"5300000001\",\"amount\":10.123}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(transactionUseCase);
    }

    @Test
    void transferReturnsDebitAndCredit() throws Exception {
        when(transactionUseCase.transfer(eq("5300000001"), eq("3300000001"), eq(new BigDecimal("100.00")), any()))
                .thenReturn(List.of(
                        tx(TransactionType.TRANSFER, MovementType.DEBIT, "5300000001", "3300000001"),
                        tx(TransactionType.TRANSFER, MovementType.CREDIT, "3300000001", "5300000001")));

        mockMvc.perform(post("/api/v1/transactions/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originAccountNumber":"5300000001",
                                 "destinationAccountNumber":"3300000001",
                                 "amount":100.00,
                                 "description":"Pago"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].movementType").value("DEBIT"))
                .andExpect(jsonPath("$[1].movementType").value("CREDIT"))
                .andExpect(jsonPath("$[0].reference").value("ref-1"));
    }

    @Test
    void transferRejectsMissingDestination() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originAccountNumber\":\"5300000001\",\"amount\":10}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(transactionUseCase);
    }

    @Test
    void transferToSameAccountReturns422() throws Exception {
        when(transactionUseCase.transfer(any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("La cuenta origen y la cuenta destino no pueden ser la misma"));

        mockMvc.perform(post("/api/v1/transactions/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originAccountNumber\":\"5300000001\",\"destinationAccountNumber\":\"5300000001\",\"amount\":10}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
