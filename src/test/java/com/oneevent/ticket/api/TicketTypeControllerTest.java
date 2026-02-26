package com.oneevent.ticket.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneevent.shared.exception.AppException;
import com.oneevent.shared.security.principal.AuthenticatedUser;
import com.oneevent.ticket.api.dto.CreateTicketTypeRequest;
import com.oneevent.ticket.api.dto.UpdateTicketTypeRequest;
import com.oneevent.ticket.application.TicketTypeService;
import com.oneevent.ticket.domain.TicketType;
import com.oneevent.user.domain.Role;

/**
 * Tests d'intégration pour le contrôleur des types de tickets (admin/organisateur).
 *
 * <p>Cette classe teste les endpoints HTTP de l'API de gestion des types de tickets :
 *
 * <ul>
 *   <li>POST /api/v1/ticket-types - Création de type de ticket
 *   <li>GET /api/v1/ticket-types/by-event/{eventId} - Liste des types de tickets par événement
 *   <li>PATCH /api/v1/ticket-types/{id} - Mise à jour d'un type de ticket
 *   <li>DELETE /api/v1/ticket-types/{id} - Suppression d'un type de ticket
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TicketTypeController - Tests du contrôleur de gestion des types de tickets")
class TicketTypeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TicketTypeService service;

  private static final String BASE_URL = "/api/v1/ticket-types";
  private static final UUID ORG_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID TICKET_TYPE_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String TEST_EMAIL = "organizer@example.com";
  private static final Instant NOW = Instant.now();
  private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS);
  private static final Instant NEXT_WEEK = NOW.plus(7, ChronoUnit.DAYS);

  private Authentication createAuth() {
    AuthenticatedUser principal =
        new AuthenticatedUser(USER_ID, TEST_EMAIL, Role.ORGANIZER, TicketTypeControllerTest.ORG_ID);
    return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
  }

  @Nested
  @DisplayName("POST /ticket-types - Création de type de ticket")
  class CreateTests {

    @Test
    @DisplayName("Devrait créer un type de ticket avec succès")
    @WithMockUser
    void shouldCreateTicketTypeSuccessfully() throws Exception {
      // Given
      CreateTicketTypeRequest request =
          new CreateTicketTypeRequest(
              EVENT_ID, "Early Bird", new BigDecimal("25.00"), 100, TOMORROW, NEXT_WEEK);

      TicketType createdTicketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Early Bird")
              .price(new BigDecimal("25.00"))
              .quantityAvailable(100)
              .quantitySold(0)
              .saleStart(TOMORROW)
              .saleEnd(NEXT_WEEK)
              .build();

      when(service.create(any(TicketTypeService.CreateTicketTypeCommand.class)))
          .thenReturn(createdTicketType);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(TICKET_TYPE_ID.toString()))
          .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
          .andExpect(jsonPath("$.name").value("Early Bird"))
          .andExpect(jsonPath("$.price").value(25.00))
          .andExpect(jsonPath("$.quantityAvailable").value(100))
          .andExpect(jsonPath("$.quantitySold").value(0));

      verify(service).create(any(TicketTypeService.CreateTicketTypeCommand.class));
    }

    @Test
    @DisplayName("Devrait créer un type de ticket gratuit")
    @WithMockUser
    void shouldCreateFreeTicketType() throws Exception {
      // Given
      CreateTicketTypeRequest request =
          new CreateTicketTypeRequest(EVENT_ID, "Gratuit", BigDecimal.ZERO, 50, null, null);

      TicketType createdTicketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Gratuit")
              .price(BigDecimal.ZERO)
              .quantityAvailable(50)
              .quantitySold(0)
              .build();

      when(service.create(any(TicketTypeService.CreateTicketTypeCommand.class)))
          .thenReturn(createdTicketType);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("Gratuit"))
          .andExpect(jsonPath("$.price").value(0));
    }

    @Test
    @DisplayName("Devrait retourner 400 si le nom est vide")
    @WithMockUser
    void shouldReturn400WhenNameIsBlank() throws Exception {
      // Given
      CreateTicketTypeRequest request =
          new CreateTicketTypeRequest(
              EVENT_ID, "", new BigDecimal("25.00"), 100, TOMORROW, NEXT_WEEK);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'eventId est null")
    @WithMockUser
    void shouldReturn400WhenEventIdIsNull() throws Exception {
      // Given
      CreateTicketTypeRequest request =
          new CreateTicketTypeRequest(null, "Ticket", new BigDecimal("25.00"), 100, null, null);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si le prix est null")
    @WithMockUser
    void shouldReturn400WhenPriceIsNull() throws Exception {
      // Given
      CreateTicketTypeRequest request =
          new CreateTicketTypeRequest(EVENT_ID, "Ticket", null, 100, null, null);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si la quantité est inférieure à 1")
    @WithMockUser
    void shouldReturn400WhenQuantityLessThan1() throws Exception {
      // Given
      CreateTicketTypeRequest request =
          new CreateTicketTypeRequest(EVENT_ID, "Ticket", new BigDecimal("25.00"), 0, null, null);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 403 sans authentification")
    void shouldReturn403WhenNotAuthenticated() throws Exception {
      // Given
      CreateTicketTypeRequest request =
          new CreateTicketTypeRequest(EVENT_ID, "Ticket", new BigDecimal("25.00"), 100, null, null);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /ticket-types/by-event/{eventId} - Liste des types de tickets")
  class ListByEventTests {

    @Test
    @DisplayName("Devrait retourner la liste des types de tickets pour un événement")
    @WithMockUser
    void shouldReturnTicketTypesListForEvent() throws Exception {
      // Given
      List<TicketType> ticketTypes =
          List.of(
              createTicketType("Early Bird", new BigDecimal("25.00"), 100, 10),
              createTicketType("VIP", new BigDecimal("100.00"), 50, 5));

      when(service.listByEventMine(EVENT_ID)).thenReturn(ticketTypes);

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/by-event/" + EVENT_ID).with(authentication(createAuth())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].name").value("Early Bird"))
          .andExpect(jsonPath("$[1].name").value("VIP"));

      verify(service).listByEventMine(EVENT_ID);
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucun type de ticket")
    @WithMockUser
    void shouldReturnEmptyListWhenNoTicketTypes() throws Exception {
      // Given
      when(service.listByEventMine(EVENT_ID)).thenReturn(List.of());

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/by-event/" + EVENT_ID).with(authentication(createAuth())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Devrait retourner 404 si l'événement n'existe pas")
    @WithMockUser
    void shouldReturn404WhenEventNotFound() throws Exception {
      // Given
      when(service.listByEventMine(EVENT_ID))
          .thenThrow(
              AppException.builder(HttpStatus.NOT_FOUND)
                  .message("Événement introuvable")
                  .errorCode("EVENT_NOT_FOUND")
                  .build());

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/by-event/" + EVENT_ID).with(authentication(createAuth())))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'eventId est invalide")
    @WithMockUser
    void shouldReturn400WhenEventIdInvalid() throws Exception {
      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/by-event/invalid-uuid").with(authentication(createAuth())))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 403 sans authentification")
    void shouldReturn403WhenNotAuthenticated() throws Exception {
      // When / Then
      mockMvc.perform(get(BASE_URL + "/by-event/" + EVENT_ID)).andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PATCH /ticket-types/{id} - Mise à jour de type de ticket")
  class UpdateTests {

    @Test
    @DisplayName("Devrait mettre à jour un type de ticket avec succès")
    @WithMockUser
    void shouldUpdateTicketTypeSuccessfully() throws Exception {
      // Given
      UpdateTicketTypeRequest request =
          new UpdateTicketTypeRequest("VIP Updated", new BigDecimal("150.00"), 200, null, null);

      TicketType updatedTicketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("VIP Updated")
              .price(new BigDecimal("150.00"))
              .quantityAvailable(200)
              .quantitySold(10)
              .build();

      when(service.update(eq(TICKET_TYPE_ID), any(TicketTypeService.PatchTicketTypeCommand.class)))
          .thenReturn(updatedTicketType);

      // When / Then
      mockMvc
          .perform(
              patch(BASE_URL + "/" + TICKET_TYPE_ID)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(TICKET_TYPE_ID.toString()))
          .andExpect(jsonPath("$.name").value("VIP Updated"))
          .andExpect(jsonPath("$.price").value(150.00))
          .andExpect(jsonPath("$.quantityAvailable").value(200));

      verify(service)
          .update(eq(TICKET_TYPE_ID), any(TicketTypeService.PatchTicketTypeCommand.class));
    }

    @Test
    @DisplayName("Devrait mettre à jour uniquement le nom")
    @WithMockUser
    void shouldUpdateOnlyName() throws Exception {
      // Given
      UpdateTicketTypeRequest request =
          new UpdateTicketTypeRequest("New Name", null, null, null, null);

      TicketType updatedTicketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("New Name")
              .price(new BigDecimal("25.00"))
              .quantityAvailable(100)
              .quantitySold(10)
              .build();

      when(service.update(eq(TICKET_TYPE_ID), any(TicketTypeService.PatchTicketTypeCommand.class)))
          .thenReturn(updatedTicketType);

      // When / Then
      mockMvc
          .perform(
              patch(BASE_URL + "/" + TICKET_TYPE_ID)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    @DisplayName("Devrait retourner 404 si le type de ticket n'existe pas")
    @WithMockUser
    void shouldReturn404WhenTicketTypeNotFound() throws Exception {
      // Given
      UpdateTicketTypeRequest request =
          new UpdateTicketTypeRequest("Updated", null, null, null, null);

      when(service.update(eq(TICKET_TYPE_ID), any(TicketTypeService.PatchTicketTypeCommand.class)))
          .thenThrow(
              AppException.builder(HttpStatus.NOT_FOUND)
                  .message("Type de ticket introuvable")
                  .errorCode("TICKET_TYPE_NOT_FOUND")
                  .build());

      // When / Then
      mockMvc
          .perform(
              patch(BASE_URL + "/" + TICKET_TYPE_ID)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'ID est invalide")
    @WithMockUser
    void shouldReturn400WhenIdInvalid() throws Exception {
      // Given
      UpdateTicketTypeRequest request =
          new UpdateTicketTypeRequest("Updated", null, null, null, null);

      // When / Then
      mockMvc
          .perform(
              patch(BASE_URL + "/invalid-uuid")
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si la quantité est inférieure aux ventes")
    @WithMockUser
    void shouldReturn400WhenQuantityLessThanSold() throws Exception {
      // Given
      UpdateTicketTypeRequest request = new UpdateTicketTypeRequest(null, null, 5, null, null);

      when(service.update(eq(TICKET_TYPE_ID), any(TicketTypeService.PatchTicketTypeCommand.class)))
          .thenThrow(
              AppException.builder(HttpStatus.BAD_REQUEST)
                  .message(
                      "La quantité disponible ne peut pas être inférieure aux tickets déjà vendus")
                  .errorCode("INVALID_TICKET_TYPE")
                  .build());

      // When / Then
      mockMvc
          .perform(
              patch(BASE_URL + "/" + TICKET_TYPE_ID)
                  .with(authentication(createAuth()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 403 sans authentification")
    void shouldReturn403WhenNotAuthenticated() throws Exception {
      // Given
      UpdateTicketTypeRequest request =
          new UpdateTicketTypeRequest("Updated", null, null, null, null);

      // When / Then
      mockMvc
          .perform(
              patch(BASE_URL + "/" + TICKET_TYPE_ID)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("DELETE /ticket-types/{id} - Suppression de type de ticket")
  class DeleteTests {

    @Test
    @DisplayName("Devrait supprimer un type de ticket avec succès")
    @WithMockUser
    void shouldDeleteTicketTypeSuccessfully() throws Exception {
      // Given
      doNothing().when(service).softDelete(TICKET_TYPE_ID);

      // When / Then
      mockMvc
          .perform(delete(BASE_URL + "/" + TICKET_TYPE_ID).with(authentication(createAuth())))
          .andExpect(status().isOk());

      verify(service).softDelete(TICKET_TYPE_ID);
    }

    @Test
    @DisplayName("Devrait retourner 404 si le type de ticket n'existe pas")
    @WithMockUser
    void shouldReturn404WhenTicketTypeNotFound() throws Exception {
      // Given
      doThrow(
              AppException.builder(HttpStatus.NOT_FOUND)
                  .message("Type de ticket introuvable")
                  .errorCode("TICKET_TYPE_NOT_FOUND")
                  .build())
          .when(service)
          .softDelete(TICKET_TYPE_ID);

      // When / Then
      mockMvc
          .perform(delete(BASE_URL + "/" + TICKET_TYPE_ID).with(authentication(createAuth())))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait retourner 409 si des tickets ont été vendus")
    @WithMockUser
    void shouldReturn409WhenTicketsAlreadySold() throws Exception {
      // Given
      doThrow(
              AppException.builder(HttpStatus.CONFLICT)
                  .message("Impossible de supprimer un type de ticket déjà vendu")
                  .errorCode("TICKET_TYPE_ALREADY_SOLD")
                  .build())
          .when(service)
          .softDelete(TICKET_TYPE_ID);

      // When / Then
      mockMvc
          .perform(delete(BASE_URL + "/" + TICKET_TYPE_ID).with(authentication(createAuth())))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'ID est invalide")
    @WithMockUser
    void shouldReturn400WhenIdInvalid() throws Exception {
      // When / Then
      mockMvc
          .perform(delete(BASE_URL + "/invalid-uuid").with(authentication(createAuth())))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 403 sans authentification")
    void shouldReturn403WhenNotAuthenticated() throws Exception {
      // When / Then
      mockMvc.perform(delete(BASE_URL + "/" + TICKET_TYPE_ID)).andExpect(status().isForbidden());
    }
  }

  // ===== Helper methods =====

  private TicketType createTicketType(
      String name, BigDecimal price, int quantityAvailable, int quantitySold) {
    return TicketType.builder()
        .id(UUID.randomUUID())
        .organizationId(ORG_ID)
        .eventId(EVENT_ID)
        .name(name)
        .price(price)
        .quantityAvailable(quantityAvailable)
        .quantitySold(quantitySold)
        .saleStart(TOMORROW)
        .saleEnd(NEXT_WEEK)
        .build();
  }
}
