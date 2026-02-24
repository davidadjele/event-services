package com.oneevent.event.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.oneevent.event.application.EventService;
import com.oneevent.event.domain.Event;
import com.oneevent.event.domain.EventStatus;
import com.oneevent.shared.exception.AppException;

/**
 * Tests d'intégration pour le contrôleur public des ��vénements.
 *
 * <p>Cette classe teste les endpoints HTTP de l'API publique des événements :
 *
 * <ul>
 *   <li>GET /api/v1/public/events - Liste des événements publics (publiés)
 *   <li>GET /api/v1/public/events/{id} - Récupération d'un événement public
 * </ul>
 *
 * <p>Ces endpoints sont accessibles sans authentification et ne retournent que les événements avec
 * le statut PUBLISHED.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PublicEventController - Tests du contrôleur public des événements")
class PublicEventControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EventService service;

  private static final String BASE_URL = "/api/v1/public/events";
  private static final UUID ORG_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final Instant NOW = Instant.now();
  private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS);
  private static final Instant NEXT_WEEK = NOW.plus(7, ChronoUnit.DAYS);

  @Nested
  @DisplayName("GET /public/events - Liste des événements publics")
  class ListPublishedTests {

    @Test
    @DisplayName("Devrait lister les événements publiés sans authentification")
    void shouldListPublishedEventsWithoutAuth() throws Exception {
      // Given
      Event event1 =
          Event.builder()
              .id(UUID.randomUUID())
              .organizationId(ORG_ID)
              .title("Conférence Tech 2026")
              .description("Une grande conférence technologique")
              .location("Lomé, Togo")
              .startDate(TOMORROW)
              .endDate(NEXT_WEEK)
              .status(EventStatus.PUBLISHED)
              .build();

      Event event2 =
          Event.builder()
              .id(UUID.randomUUID())
              .organizationId(ORG_ID)
              .title("Workshop IA")
              .description("Atelier sur l'intelligence artificielle")
              .location("Cotonou, Bénin")
              .startDate(TOMORROW.plus(2, ChronoUnit.DAYS))
              .endDate(NEXT_WEEK.plus(1, ChronoUnit.DAYS))
              .status(EventStatus.PUBLISHED)
              .build();

      Page<Event> page = new PageImpl<>(List.of(event1, event2));
      when(service.listPublished(any(Pageable.class))).thenReturn(page);

      // When / Then
      mockMvc
          .perform(get(BASE_URL).param("page", "0").param("size", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(2))
          .andExpect(jsonPath("$.content[0].title").value("Conférence Tech 2026"))
          .andExpect(jsonPath("$.content[0].location").value("Lomé, Togo"))
          .andExpect(jsonPath("$.content[1].title").value("Workshop IA"))
          .andExpect(jsonPath("$.totalElements").value(2));

      verify(service).listPublished(any(Pageable.class));
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucun événement publié")
    void shouldReturnEmptyListWhenNoPublishedEvents() throws Exception {
      // Given
      Page<Event> emptyPage = new PageImpl<>(List.of());
      when(service.listPublished(any(Pageable.class))).thenReturn(emptyPage);

      // When / Then
      mockMvc
          .perform(get(BASE_URL).param("page", "0").param("size", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(0))
          .andExpect(jsonPath("$.totalElements").value(0));

      verify(service).listPublished(any(Pageable.class));
    }

    @Test
    @DisplayName("Devrait supporter la pagination avec différentes tailles de page")
    void shouldSupportPaginationWithDifferentPageSizes() throws Exception {
      // Given
      Event event =
          Event.builder().id(EVENT_ID).title("Event").status(EventStatus.PUBLISHED).build();
      Page<Event> page = new PageImpl<>(List.of(event));
      when(service.listPublished(any(Pageable.class))).thenReturn(page);

      // When / Then - page 0, size 5
      mockMvc
          .perform(get(BASE_URL).param("page", "0").param("size", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray());

      // When / Then - page 1, size 20
      mockMvc
          .perform(get(BASE_URL).param("page", "1").param("size", "20"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName(
        "Devrait utiliser des valeurs par défaut si les paramètres de pagination ne sont pas fournis")
    void shouldUseDefaultPaginationParams() throws Exception {
      // Given
      Page<Event> page = new PageImpl<>(List.of());
      when(service.listPublished(any(Pageable.class))).thenReturn(page);

      // When / Then
      mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());

      verify(service).listPublished(any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("GET /public/events/{id} - Récupération d'un événement public")
  class GetPublishedTests {

    @Test
    @DisplayName("Devrait récupérer un événement publié sans authentification")
    void shouldGetPublishedEventWithoutAuth() throws Exception {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Festival de Musique")
              .description("Un festival de musique africaine")
              .location("Accra, Ghana")
              .startDate(TOMORROW)
              .endDate(NEXT_WEEK)
              .status(EventStatus.PUBLISHED)
              .build();

      when(service.getPublished(EVENT_ID)).thenReturn(event);

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/{id}", EVENT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
          .andExpect(jsonPath("$.title").value("Festival de Musique"))
          .andExpect(jsonPath("$.description").value("Un festival de musique africaine"))
          .andExpect(jsonPath("$.location").value("Accra, Ghana"))
          .andExpect(jsonPath("$.status").value("PUBLISHED"));

      verify(service).getPublished(EVENT_ID);
    }

    @Test
    @DisplayName("Devrait retourner 404 si l'événement n'est pas publié")
    void shouldReturn404IfEventNotPublished() throws Exception {
      // Given
      when(service.getPublished(EVENT_ID))
          .thenThrow(
              AppException.builder(HttpStatus.NOT_FOUND)
                  .logMessage("Public event not found eventId=" + EVENT_ID)
                  .message("Événement introuvable")
                  .errorCode("EVENT_NOT_FOUND")
                  .build());

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/{id}", EVENT_ID))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Événement introuvable"))
          .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));

      verify(service).getPublished(EVENT_ID);
    }

    @Test
    @DisplayName("Devrait retourner 404 si l'événement n'existe pas")
    void shouldReturn404IfEventDoesNotExist() throws Exception {
      // Given
      UUID nonExistentId = UUID.randomUUID();
      when(service.getPublished(nonExistentId))
          .thenThrow(
              AppException.builder(HttpStatus.NOT_FOUND)
                  .logMessage("Public event not found eventId=" + nonExistentId)
                  .message("Événement introuvable")
                  .errorCode("EVENT_NOT_FOUND")
                  .build());

      // When / Then
      mockMvc.perform(get(BASE_URL + "/{id}", nonExistentId)).andExpect(status().isNotFound());

      verify(service).getPublished(nonExistentId);
    }

    @Test
    @DisplayName("Devrait retourner 400 si le format UUID de l'ID est invalide")
    void shouldReturn400WhenUuidFormatIsInvalid() throws Exception {
      // When / Then - ID invalide
      // Spring MVC lance MethodArgumentTypeMismatchException lors de la conversion UUID
      // qui doit être interceptée par le GlobalExceptionHandler et retourner 400
      mockMvc.perform(get(BASE_URL + "/{id}", "invalid-uuid")).andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Tests de sécurité")
  class SecurityTests {

    @Test
    @DisplayName("Les endpoints publics ne doivent pas nécessiter d'authentification")
    void publicEndpointsShouldNotRequireAuth() throws Exception {
      // Given
      Page<Event> page = new PageImpl<>(List.of());
      when(service.listPublished(any(Pageable.class))).thenReturn(page);

      Event event = Event.builder().id(EVENT_ID).status(EventStatus.PUBLISHED).build();
      when(service.getPublished(EVENT_ID)).thenReturn(event);

      // When / Then - liste sans auth
      mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());

      // When / Then - détail sans auth
      mockMvc.perform(get(BASE_URL + "/{id}", EVENT_ID)).andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("Tests des champs de réponse")
  class ResponseFieldsTests {

    @Test
    @DisplayName("La réponse publique ne devrait pas exposer de champs sensibles")
    void publicResponseShouldNotExposeSensitiveFields() throws Exception {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID) // champ potentiellement sensible
              .title("Public Event")
              .description("Description")
              .location("Location")
              .startDate(TOMORROW)
              .endDate(NEXT_WEEK)
              .status(EventStatus.PUBLISHED)
              .build();

      when(service.getPublished(EVENT_ID)).thenReturn(event);

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/{id}", EVENT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.title").exists())
          .andExpect(jsonPath("$.description").exists())
          .andExpect(jsonPath("$.location").exists())
          .andExpect(jsonPath("$.startDate").exists())
          .andExpect(jsonPath("$.endDate").exists())
          .andExpect(jsonPath("$.status").exists());
      // Note: le mapper devrait gérer les champs à exposer/masquer
    }
  }
}
