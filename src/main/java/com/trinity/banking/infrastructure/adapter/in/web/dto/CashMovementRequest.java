package com.trinity.banking.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de consignaciones y retiros (operan sobre una sola cuenta). */
public record CashMovementRequest(
        @NotBlank(message = "El número de cuenta es obligatorio")
        @Pattern(regexp = "^\\d{10}$", message = "El número de cuenta debe tener 10 dígitos")
        String accountNumber,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 17, fraction = 2, message = "El monto admite máximo 2 decimales")
        BigDecimal amount,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description) {
}
