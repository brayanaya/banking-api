package com.trinity.banking.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;

import com.trinity.banking.domain.model.IdentificationType;
import com.trinity.banking.domain.port.in.ClientCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotNull(message = "El tipo de identificación es obligatorio")
        IdentificationType identificationType,

        @NotBlank(message = "El número de identificación es obligatorio")
        @Size(min = 3, max = 20, message = "El número de identificación debe tener entre 3 y 20 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9]+$", message = "El número de identificación solo admite letras y números")
        String identificationNumber,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
        String lastName,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        @Email(message = "El correo electrónico no es válido")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "El correo debe tener el formato xxxx@xxxxx.xxx")
        String email,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate birthDate) {

    public ClientCommand toCommand() {
        return new ClientCommand(identificationType, identificationNumber, firstName, lastName, email, birthDate);
    }
}
