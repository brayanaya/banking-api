package com.trinity.banking.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.model.IdentificationType;

public record ClientResponse(
        Long id,
        IdentificationType identificationType,
        String identificationNumber,
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ClientResponse from(Client c) {
        return new ClientResponse(c.getId(), c.getIdentificationType(), c.getIdentificationNumber(),
                c.getFirstName(), c.getLastName(), c.getEmail(), c.getBirthDate(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
