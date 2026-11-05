package com.trinity.banking.domain.port.in;

import java.time.LocalDate;

import com.trinity.banking.domain.model.IdentificationType;

public record ClientCommand(
        IdentificationType identificationType,
        String identificationNumber,
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate) {
}
