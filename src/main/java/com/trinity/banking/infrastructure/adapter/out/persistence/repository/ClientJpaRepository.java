package com.trinity.banking.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trinity.banking.domain.model.IdentificationType;
import com.trinity.banking.infrastructure.adapter.out.persistence.entity.ClientEntity;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, Long> {

    Optional<ClientEntity> findByIdentificationTypeAndIdentificationNumber(IdentificationType type, String number);

    Optional<ClientEntity> findByEmail(String email);
}
