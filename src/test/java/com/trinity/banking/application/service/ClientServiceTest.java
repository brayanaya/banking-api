package com.trinity.banking.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.trinity.banking.TestData;
import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.DuplicateResourceException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.model.IdentificationType;
import com.trinity.banking.domain.port.in.ClientCommand;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.domain.port.out.ClientRepositoryPort;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientServiceTest {

    @Mock
    ClientRepositoryPort clientRepository;
    @Mock
    AccountRepositoryPort accountRepository;

    ClientService service;

    @BeforeEach
    void setUp() {
        service = new ClientService(clientRepository, accountRepository, TestData.CLOCK);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
            }
            return c;
        });
        when(clientRepository.findByIdentification(any(), any())).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(any())).thenReturn(Optional.empty());
    }

    private ClientCommand command(LocalDate birthDate) {
        return new ClientCommand(IdentificationType.CC, "1075000001", " Laura ", "Gomez",
                " Laura.Gomez@Example.com ", birthDate);
    }

    @Test
    void createsAdultClientWithAutomaticCreationDateAndNormalizedFields() {
        Client created = service.create(command(LocalDate.of(1992, 3, 14)));

        assertEquals(1L, created.getId());
        assertEquals(TestData.NOW, created.getCreatedAt());
        assertNull(created.getUpdatedAt());
        assertEquals("Laura", created.getFirstName());
        assertEquals("laura.gomez@example.com", created.getEmail());
    }

    @Test
    void rejectsMinor() {
        assertThrows(BusinessRuleException.class, () -> service.create(command(LocalDate.of(2010, 1, 1))));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void rejectsClientTurning18Tomorrow() {
        LocalDate turns18Tomorrow = TestData.TODAY.minusYears(18).plusDays(1);
        assertThrows(BusinessRuleException.class, () -> service.create(command(turns18Tomorrow)));
    }

    @Test
    void acceptsClientTurning18Today() {
        Client created = service.create(command(TestData.TODAY.minusYears(18)));
        assertNotNull(created.getId());
    }

    @Test
    void rejectsNamesShorterThanTwoCharacters() {
        ClientCommand shortName = new ClientCommand(IdentificationType.CC, "1075000001", "A", "Gomez",
                "a@b.co", LocalDate.of(1990, 1, 1));
        assertThrows(BusinessRuleException.class, () -> service.create(shortName));

        ClientCommand shortLastName = new ClientCommand(IdentificationType.CC, "1075000001", "Laura", " G ",
                "a@b.co", LocalDate.of(1990, 1, 1));
        assertThrows(BusinessRuleException.class, () -> service.create(shortLastName));
    }

    @Test
    void rejectsDuplicateIdentification() {
        when(clientRepository.findByIdentification(IdentificationType.CC, "1075000001"))
                .thenReturn(Optional.of(TestData.client(99L)));
        assertThrows(DuplicateResourceException.class, () -> service.create(command(LocalDate.of(1992, 3, 14))));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(clientRepository.findByEmail("laura.gomez@example.com")).thenReturn(Optional.of(TestData.client(99L)));
        assertThrows(DuplicateResourceException.class, () -> service.create(command(LocalDate.of(1992, 3, 14))));
    }

    @Test
    void updateSetsModificationDateAutomatically() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(TestData.client(1L)));

        Client updated = service.update(1L, command(LocalDate.of(1992, 3, 14)));

        assertEquals(TestData.NOW, updated.getUpdatedAt());
        assertEquals(TestData.NOW.minusDays(10), updated.getCreatedAt());
    }

    @Test
    void updateAllowsKeepingOwnIdentificationAndEmail() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(TestData.client(1L)));
        when(clientRepository.findByIdentification(any(), any())).thenReturn(Optional.of(TestData.client(1L)));
        when(clientRepository.findByEmail(any())).thenReturn(Optional.of(TestData.client(1L)));

        Client updated = service.update(1L, command(LocalDate.of(1992, 3, 14)));
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void updateRejectsMinor() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(TestData.client(1L)));
        assertThrows(BusinessRuleException.class, () -> service.update(1L, command(LocalDate.of(2015, 1, 1))));
    }

    @Test
    void updateFailsWhenClientDoesNotExist() {
        when(clientRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.update(7L, command(LocalDate.of(1992, 3, 14))));
    }

    @Test
    void getByIdFailsWhenMissing() {
        when(clientRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(5L));
    }

    @Test
    void deleteIsBlockedWhenClientHasProducts() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(TestData.client(1L)));
        when(accountRepository.existsByClientId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.delete(1L));
        verify(clientRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteWorksWhenClientHasNoProducts() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(TestData.client(1L)));
        when(accountRepository.existsByClientId(1L)).thenReturn(false);

        service.delete(1L);
        verify(clientRepository).deleteById(1L);
    }

    @Test
    void deleteFailsWhenClientDoesNotExist() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L));
    }
}
