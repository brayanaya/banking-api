package com.trinity.banking.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.trinity.banking.TestData;
import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.DuplicateResourceException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.model.IdentificationType;
import com.trinity.banking.domain.port.in.ClientCommand;
import com.trinity.banking.domain.port.in.ClientUseCase;

import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    private static final String VALID_BODY = """
            {
              "identificationType": "CC",
              "identificationNumber": "1075000001",
              "firstName": "Laura",
              "lastName": "Gomez",
              "email": "laura.gomez@example.com",
              "birthDate": "1992-03-14"
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientUseCase clientUseCase;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(clientUseCase.create(any(ClientCommand.class))).thenReturn(TestData.client(1L));

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/clients/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("laura.gomez@example.com"))
                .andExpect(jsonPath("$.birthDate").value("1992-03-14"));
    }

    @Test
    void createRejectsInvalidEmail() throws Exception {
        String body = VALID_BODY.replace("laura.gomez@example.com", "no-es-un-correo");

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isNotEmpty());
        verifyNoInteractions(clientUseCase);
    }

    @Test
    void createRejectsEmailWithOneCharTld() throws Exception {
        String body = VALID_BODY.replace("laura.gomez@example.com", "test@dominio.c");

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isNotEmpty());
        verifyNoInteractions(clientUseCase);
    }

    @Test
    void createAcceptsEmailWithTwoCharTld() throws Exception {
        String body = VALID_BODY.replace("laura.gomez@example.com", "test@dominio.co");
        Client clientWithCustomEmail = Client.builder()
                .id(1L)
                .identificationType(IdentificationType.CC)
                .identificationNumber("1075000001")
                .firstName("Laura")
                .lastName("Gomez")
                .email("test@dominio.co")
                .birthDate(LocalDate.of(1992, 3, 14))
                .createdAt(TestData.NOW.minusDays(10))
                .build();
        when(clientUseCase.create(any(ClientCommand.class))).thenReturn(clientWithCustomEmail);

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@dominio.co"));
    }

    @Test
    void createRejectsShortNames() throws Exception {
        String body = VALID_BODY.replace("\"Laura\"", "\"L\"");

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(clientUseCase);
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(clientUseCase);
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content("{ no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMinorReturns422() throws Exception {
        when(clientUseCase.create(any(ClientCommand.class)))
                .thenThrow(new BusinessRuleException("El cliente debe ser mayor de edad (18 años o más)"));

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El cliente debe ser mayor de edad (18 años o más)"));
    }

    @Test
    void createDuplicateReturns409() throws Exception {
        when(clientUseCase.create(any(ClientCommand.class)))
                .thenThrow(new DuplicateResourceException("Ya existe un cliente"));

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllReturnsList() throws Exception {
        when(clientUseCase.getAll()).thenReturn(List.of(TestData.client(1L), TestData.client(2L)));

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getByIdReturnsClient() throws Exception {
        when(clientUseCase.getById(1L)).thenReturn(TestData.client(1L));

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Laura"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(clientUseCase.getById(9L)).thenThrow(new ResourceNotFoundException("Cliente con id 9 no encontrado"));

        mockMvc.perform(get("/api/v1/clients/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/clients/9"));
    }

    @Test
    void getByIdWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/clients/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedClient() throws Exception {
        when(clientUseCase.update(eq(1L), any(ClientCommand.class))).thenReturn(TestData.client(1L));

        mockMvc.perform(put("/api/v1/clients/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/1")).andExpect(status().isNoContent());
        verify(clientUseCase).delete(1L);
    }

    @Test
    void deleteWithProductsReturns422() throws Exception {
        doThrow(new BusinessRuleException("No se puede eliminar un cliente que tiene productos vinculados"))
                .when(clientUseCase).delete(1L);

        mockMvc.perform(delete("/api/v1/clients/1")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        when(clientUseCase.getById(1L)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno del servidor"));
    }

    @Test
    void dataIntegrityViolationReturns409() throws Exception {
        when(clientUseCase.create(any(ClientCommand.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La operación viola una restricción de integridad de datos"));
    }

    @Test
    void unsupportedHttpMethodReturns405() throws Exception {
        // PUT is not supported on /api/v1/clients (only POST, GET)
        mockMvc.perform(put("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void nonExistentEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/non-existent-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recurso no encontrado"));
    }
}
