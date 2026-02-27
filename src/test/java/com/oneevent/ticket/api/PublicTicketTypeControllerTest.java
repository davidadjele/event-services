package com.oneevent.ticket.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.oneevent.shared.exception.AppException;
import com.oneevent.ticket.application.TicketTypeService;
import com.oneevent.ticket.domain.TicketType;

/**
 * Tests d'intégration pour le contrôleur public des types de tickets.
 *
 * <p>Cette classe teste le seul endpoint public des types de tickets :
 *
 * <ul>
 *   <li>GET /api/v1/public/ticket-types/by-event/{eventId} — Liste les types de tickets d'un
 *       événement publié, accessible sans authentification.
 * </ul>
 *
 * <p>Les scénarios couverts :
 *
 * <ul>
 *   <li>Accès sans authentification (endpoint public)
 *   <li>Retour de la liste complète avec les champs attendus
 *   <li>Liste vide lorsqu'aucun type de ticket n'existe
 *   <li>404 si l'événement est introuvable ou non publié
 *   <li>400 si l'identifiant de l'événement n'est pas un UUID valide
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PublicTicketTypeController - Tests du contrôleur public des types de tickets")
class PublicTicketTypeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TicketTypeService service;

  /** URL de base de l'endpoint public des types de tickets. */
  private static final String BASE_URL = "/api/v1/public/ticket-types";

  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID ORG_ID = UUID.randomUUID();
  private static final Instant NOW = Instant.now();
  private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS);
  private static final Instant NEXT_WEEK = NOW.plus(7, ChronoUnit.DAYS);

  @Nested
  @DisplayName(
      "GET /public/ticket-types/by-event/{eventId} - Liste des types de tickets d'un événement publié")
  class ListForPublishedEventTests {

    @Test
    @DisplayName("Devrait lister les types de tickets sans authentification")
    void shouldListTicketTypesWithoutAuthentication() throws Exception {
      // Given
      List<TicketType> ticketTypes =
          List.of(
              buildTicketType("Early Bird", new BigDecimal("15.00"), 200, 30),
              buildTicketType("VIP", new BigDecimal("75.00"), 50, 5));

      when(service.listForPublishedEvent(EVENT_ID)).thenReturn(ticketTypes);

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/by-event/" + EVENT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].name").value("Early Bird"))
          .andExpect(jsonPath("$[0].price").value(15.00))
          .andExpect(jsonPath("$[0].quantityAvailable").value(200))
          .andExpect(jsonPath("$[0].quantitySold").value(30))
          .andExpect(jsonPath("$[1].name").value("VIP"))
          .andExpect(jsonPath("$[1].price").value(75.00));

      verify(service).listForPublishedEvent(EVENT_ID);
    }

    @Test
    @DisplayName("Devrait retourner les champs attendus dans la réponse publique")
    void shouldReturnExpectedPublicFields() throws Exception {
      // Given
      UUID ticketTypeId = UUID.randomUUID();
      TicketType ticketType =
          TicketType.builder()
              .id(ticketTypeId)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Standard")
              .price(new BigDecimal("20.00"))
              .quantityAvailable(100)
              .quantitySold(10)
              .saleStart(TOMORROW)
              .saleEnd(NEXT_WEEK)
              .build();

      when(service.listForPublishedEvent(EVENT_ID)).thenReturn(List.of(ticketType));

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/by-event/" + EVENT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].id").value(ticketTypeId.toString()))
          .andExpect(jsonPath("$[0].eventId").value(EVENT_ID.toString()))
          .andExpect(jsonPath("$[0].name").value("Standard"))
          .andExpect(jsonPath("$[0].price").value(20.00))
          .andExpect(jsonPath("$[0].quantityAvailable").value(100))
          .andExpect(jsonPath("$[0].quantitySold").value(10))
          .andExpect(jsonPath("$[0].saleStart").exists())
          .andExpect(jsonPath("$[0].saleEnd").exists());
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucun type de ticket")
    void shouldReturnEmptyListWhenNoTicketTypes() throws Exception {
      // Given
      when(service.listForPublishedEvent(EVENT_ID)).thenReturn(List.of());

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/by-event/" + EVENT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));

      verify(service).listForPublishedEvent(EVENT_ID);
    }

    @Test
    @DisplayName("Devrait retourner un ticket gratuit avec prix à zéro")
    void shouldReturnFreeTicketWithZeroPrice() throws Exception {
      // Given
      TicketType freeTicket = buildTicketType("Entrée libre", BigDecimal.ZERO, 500, 0);

      when(service.listForPublishedEvent(EVENT_ID)).thenReturn(List.of(freeTicket));

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/by-event/" + EVENT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].name").value("Entrée libre"))
          .andExpect(jsonPath("$[0].price").value(0));
    }

    @Test
    @DisplayName("Devrait retourner 404 si l'événement est introuvable ou non publié")
    void shouldReturn404WhenEventNotFoundOrNotPublished() throws Exception {
      // Given
      when(service.listForPublishedEvent(EVENT_ID))
          .thenThrow(
              AppException.builder(HttpStatus.NOT_FOUND)
                  .message("Événement introuvable")
                  .errorCode("EVENT_NOT_FOUND")
                  .build());

      // When / Then
      mockMvc.perform(get(BASE_URL + "/by-event/" + EVENT_ID)).andExpect(status().isNotFound());

      verify(service).listForPublishedEvent(EVENT_ID);
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'eventId n'est pas un UUID valide")
    void shouldReturn400WhenEventIdIsInvalidUuid() throws Exception {
      // When / Then
      mockMvc.perform(get(BASE_URL + "/by-event/invalid-uuid")).andExpect(status().isBadRequest());
    }
  }

  // ===== Méthode utilitaire =====

  /**
   * Construit un {@link TicketType} de test avec les propriétés fournies.
   *
   * @param name le nom du type de ticket
   * @param price le prix unitaire
   * @param quantityAvailable le nombre de places disponibles
   * @param quantitySold le nombre de places déjà vendues
   * @return une instance de {@link TicketType} prête à l'emploi dans les tests
   */
  private TicketType buildTicketType(
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
