package com.trinity.banking.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    public static final int MINIMUM_AGE = 18;

    private Long id;
    private IdentificationType identificationType;
    private String identificationNumber;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Regla de negocio: solo se admiten clientes mayores de edad. */
    public static boolean isAdult(LocalDate birthDate, LocalDate today) {
        return birthDate != null && Period.between(birthDate, today).getYears() >= MINIMUM_AGE;
    }
}
