package com.oneevent.event.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneevent.event.api.dto.CreateEventRequest;
import com.oneevent.event.api.dto.UpdateEventRequest;
import com.oneevent.event.application.EventService;
import com.oneevent.event.domain.Event;
import com.oneevent.event.domain.EventStatus;
import com.oneevent.shared.security.principal.AuthenticatedUser;
import com.oneevent.user.domain.Role;

/**
 * Tests d'intégration pour le contrôleur des événements (admin/organisateur).
 *
 * <p>Cette classe teste les endpoints HTTP de l'API de gestion des événements :
 *
 * <ul>
 *   <li>POST /api/v1/events - Création d'événement
 *   <li>GET /api/v1/events - Liste des événements
 *   <li>GET /api/v1/events/{id} - Récupération d'un événement
 *   <li>PATCH /api/v1/events/{id} - Mise à jour d'un événement
 *   <li>DELETE /api/v1/events/{id} - Suppression d'un événement
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("EventController - Tests du contrôleur de gestion des événements")
class EventControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private EventService service;

  private static final String BASE_URL = "/api/v1/events";
  private static final UUID ORG_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String TEST_EMAIL = "organizer@example.com";
  private static final Instant NOW = Instant.now();
  private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS);
  private static final Instant NEXT_WEEK = NOW.plus(7, ChronoUnit.DAYS);

  private Authentication createAuth(Role role, UUID orgId) {
    AuthenticatedUser principal = new AuthenticatedUser(USER_ID, TEST_EMAIL, role, orgId);
    return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
  }

  @Nested
  @DisplayName("POST /events - Création d'événement")
  class CreateTests {

    @Test
    @DisplayName("Devrait créer un événement avec succès")
    @WithMockUser
    void shouldCreateEventSuccessfully() throws Exception {
      // Given
      CreateEventRequest request =
          new CreateEventRequest(
              null,
              "Conférence Tech Africa",
              "Une conférence sur la tech en Afrique",
              "Lomé, Togo",
              TOMORROW,
              NEXT_WEEK,
              EventStatus.DRAFT);

      Event createdEvent =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(request.title())
              .description(request.description())
              .location(request.location())
              .startDate(request.startDate())
              .endDate(request.endDate())
              .status(request.status())
              .build();

      when(service.create(any(EventService.CreateEventCommand.class))).thenReturn(createdEvent);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
          .andExpect(jsonPath("$.title").value("Conférence Tech Africa"))
          .andExpect(jsonPath("$.status").value("DRAFT"));

      verify(service).create(any(EventService.CreateEventCommand.class));
    }

    @Test
    @DisplayName("Devrait retourner 400 si le titre est vide")
    @WithMockUser
    void shouldReturn400WhenTitleIsBlank() throws Exception {
      // Given
      CreateEventRequest request =
          new CreateEventRequest(
              null, "", "Description", "Location", TOMORROW, NEXT_WEEK, EventStatus.DRAFT);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si la date de début est nulle")
    @WithMockUser
    void shouldReturn400WhenStartDateIsNull() throws Exception {
      // Given
      CreateEventRequest request =
          new CreateEventRequest(
              null, "Title", "Description", "Location", null, NEXT_WEEK, EventStatus.DRAFT);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si le statut est null")
    @WithMockUser
    void shouldReturn400WhenStatusIsNull() throws Exception {
      // Given
      CreateEventRequest request =
          new CreateEventRequest(
              null, "Title", "Description", "Location", TOMORROW, NEXT_WEEK, null);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /events - Liste des événements")
  class ListTests {

    @Test
    @DisplayName("Devrait lister les événements avec pagination")
    @WithMockUser
    void shouldListEventsWithPagination() throws Exception {
      // Given
      Event event1 =
          Event.builder()
              .id(UUID.randomUUID())
              .organizationId(ORG_ID)
              .title("Event 1")
              .status(EventStatus.PUBLISHED)
              .build();
      Event event2 =
          Event.builder()
              .id(UUID.randomUUID())
              .organizationId(ORG_ID)
              .title("Event 2")
              .status(EventStatus.DRAFT)
              .build();

      Page<Event> page = new PageImpl<>(List.of(event1, event2));
      when(service.listMine(any(), any(Pageable.class))).thenReturn(page);

      // When / Then
      mockMvc
          .perform(
              get(BASE_URL)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID)))
                  .param("page", "0")
                  .param("size", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(2))
          .andExpect(jsonPath("$.totalElements").value(2));

      verify(service).listMine(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Devrait accepter un filtre orgId pour super admin")
    @WithMockUser
    void shouldAcceptOrgIdFilterForSuperAdmin() throws Exception {
      // Given
      UUID filterOrgId = UUID.randomUUID();
      Page<Event> page = new PageImpl<>(List.of());
      when(service.listMine(eq(filterOrgId), any(Pageable.class))).thenReturn(page);

      // When / Then
      mockMvc
          .perform(
              get(BASE_URL)
                  .with(authentication(createAuth(Role.SUPER_ADMIN, null)))
                  .param("orgId", filterOrgId.toString())
                  .param("page", "0")
                  .param("size", "10"))
          .andExpect(status().isOk());

      verify(service).listMine(eq(filterOrgId), any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("GET /events/{id} - Récupération d'un événement")
  class GetTests {

    @Test
    @DisplayName("Devrait récupérer un événement existant")
    @WithMockUser
    void shouldGetEventSuccessfully() throws Exception {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Mon Événement")
              .description("Description détaillée")
              .location("Lomé")
              .startDate(TOMORROW)
              .endDate(NEXT_WEEK)
              .status(EventStatus.PUBLISHED)
              .build();

      when(service.getMine(null, EVENT_ID)).thenReturn(event);

      // When / Then
      mockMvc
          .perform(
              get(BASE_URL + "/{id}", EVENT_ID)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
          .andExpect(jsonPath("$.title").value("Mon Événement"))
          .andExpect(jsonPath("$.location").value("Lomé"))
          .andExpect(jsonPath("$.status").value("PUBLISHED"));

      verify(service).getMine(null, EVENT_ID);
    }

    @Test
    @DisplayName("Devrait passer le filtre orgId pour super admin")
    @WithMockUser
    void shouldPassOrgIdFilterForSuperAdmin() throws Exception {
      // Given
      UUID filterOrgId = UUID.randomUUID();
      Event event = Event.builder().id(EVENT_ID).organizationId(filterOrgId).build();
      when(service.getMine(EVENT_ID, filterOrgId)).thenReturn(event);

      // When / Then
      mockMvc
          .perform(
              get(BASE_URL + "/{id}", EVENT_ID)
                  .with(authentication(createAuth(Role.SUPER_ADMIN, null)))
                  .param("orgId", filterOrgId.toString()))
          .andExpect(status().isOk());

      verify(service).getMine(filterOrgId, EVENT_ID);
    }
  }

  @Nested
  @DisplayName("PATCH /events/{id} - Mise à jour d'un événement")
  class UpdateTests {

    @Test
    @DisplayName("Devrait mettre à jour un événement avec succès")
    @WithMockUser
    void shouldUpdateEventSuccessfully() throws Exception {
      // Given
      UpdateEventRequest request =
          new UpdateEventRequest(
              "Titre Mis à Jour",
              "Nouvelle description",
              "Nouvelle localisation",
              null,
              null,
              EventStatus.PUBLISHED);

      Event updatedEvent =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(request.title())
              .description(request.description())
              .location(request.location())
              .startDate(TOMORROW)
              .endDate(NEXT_WEEK)
              .status(request.status())
              .build();

      when(service.update(eq(EVENT_ID), any(), any(EventService.UpdateEventCommand.class)))
          .thenReturn(updatedEvent);

      // When / Then
      mockMvc
          .perform(
              patch(BASE_URL + "/{id}", EVENT_ID)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("Titre Mis à Jour"))
          .andExpect(jsonPath("$.description").value("Nouvelle description"))
          .andExpect(jsonPath("$.status").value("PUBLISHED"));

      verify(service).update(eq(EVENT_ID), any(), any(EventService.UpdateEventCommand.class));
    }

    @Test
    @DisplayName("Devrait accepter une mise à jour partielle")
    @WithMockUser
    void shouldAcceptPartialUpdate() throws Exception {
      // Given - only updating title
      UpdateEventRequest request =
          new UpdateEventRequest("Nouveau Titre", null, null, null, null, null);

      Event updatedEvent =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(request.title())
              .description("Old Description")
              .location("Old Location")
              .status(EventStatus.DRAFT)
              .build();

      when(service.update(eq(EVENT_ID), any(), any(EventService.UpdateEventCommand.class)))
          .thenReturn(updatedEvent);

      // When / Then
      mockMvc
          .perform(
              patch(BASE_URL + "/{id}", EVENT_ID)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("Nouveau Titre"));

      verify(service).update(eq(EVENT_ID), any(), any(EventService.UpdateEventCommand.class));
    }
  }

  @Nested
  @DisplayName("DELETE /events/{id} - Suppression d'un événement")
  class DeleteTests {

    @Test
    @DisplayName("Devrait supprimer un événement avec succès")
    @WithMockUser
    void shouldDeleteEventSuccessfully() throws Exception {
      // When / Then
      mockMvc
          .perform(
              delete(BASE_URL + "/{id}", EVENT_ID)
                  .with(authentication(createAuth(Role.ORGANIZER, ORG_ID))))
          .andExpect(status().isOk());

      verify(service).softDelete(EVENT_ID, null);
    }

    @Test
    @DisplayName("Devrait passer le filtre orgId pour super admin lors de la suppression")
    @WithMockUser
    void shouldPassOrgIdFilterForSuperAdminOnDelete() throws Exception {
      // Given
      UUID filterOrgId = UUID.randomUUID();

      // When / Then
      mockMvc
          .perform(
              delete(BASE_URL + "/{id}", EVENT_ID)
                  .with(authentication(createAuth(Role.SUPER_ADMIN, null)))
                  .param("orgId", filterOrgId.toString()))
          .andExpect(status().isOk());

      verify(service).softDelete(EVENT_ID, filterOrgId);
    }
  }
}
