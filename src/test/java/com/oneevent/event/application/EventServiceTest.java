package com.oneevent.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.oneevent.event.domain.Event;
import com.oneevent.event.domain.EventStatus;
import com.oneevent.event.infrastructure.EventRepository;
import com.oneevent.shared.exception.AppException;
import com.oneevent.shared.security.SecurityContext;

/**
 * Tests unitaires pour le service de gestion des événements.
 *
 * <p>Cette classe teste les fonctionnalités de :
 *
 * <ul>
 *   <li>Création d'événements (organisateur et super admin)
 *   <li>Listage des événements (mine et publics)
 *   <li>Récupération d'événements (mine et publics)
 *   <li>Mise à jour d'événements
 *   <li>Suppression logique d'événements
 *   <li>Validation des dates
 *   <li>Gestion des permissions selon les rôles
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService - Tests du service de gestion des événements")
class EventServiceTest {

  @Mock private EventRepository repo;

  @InjectMocks private EventService service;

  private static final UUID ORG_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final Instant NOW = Instant.now();
  private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS);
  private static final Instant NEXT_WEEK = NOW.plus(7, ChronoUnit.DAYS);

  @Nested
  @DisplayName("create - Création d'événement")
  class CreateTests {

    @Test
    @DisplayName("Devrait sauvegarder un événement dans le repository")
    void shouldSaveEventInRepository() {
      // Given
      EventService.CreateEventCommand cmd =
          new EventService.CreateEventCommand(
              null, // orgId sera résolu via SecurityContext
              "Conférence Tech",
              "Description de la conférence",
              "Lomé, Togo",
              TOMORROW,
              NEXT_WEEK,
              EventStatus.DRAFT);

      Event savedEvent =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(cmd.title())
              .description(cmd.description())
              .location(cmd.location())
              .startDate(cmd.startDate())
              .endDate(cmd.endDate())
              .status(cmd.status())
              .build();

      when(repo.save(any(Event.class))).thenReturn(savedEvent);

      // Mock SecurityContext pour simuler un organisateur
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        Event result = service.create(cmd);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Conférence Tech");
        assertThat(result.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(repo).save(eventCaptor.capture());
        Event captured = eventCaptor.getValue();
        assertThat(captured.getId()).isNotNull();
        assertThat(captured.getOrganizationId()).isEqualTo(ORG_ID);
      }
    }

    @Test
    @DisplayName("Devrait échouer si la date de fin est avant la date de début")
    void shouldFailIfEndDateBeforeStartDate() {
      // Given
      EventService.CreateEventCommand cmd =
          new EventService.CreateEventCommand(
              null,
              "Event",
              "Desc",
              "Loc",
              NEXT_WEEK, // start
              TOMORROW, // end before start
              EventStatus.DRAFT);

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.create(cmd))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("date de fin doit être après")
            .extracting(e -> ((AppException) e).getHttpStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(repo, never()).save(any());
      }
    }

    @Test
    @DisplayName("Devrait accepter une date de fin nulle")
    void shouldAcceptNullEndDate() {
      // Given
      EventService.CreateEventCommand cmd =
          new EventService.CreateEventCommand(
              null, "Event", "Desc", "Loc", TOMORROW, null, EventStatus.DRAFT);

      Event savedEvent = Event.builder().id(EVENT_ID).organizationId(ORG_ID).build();
      when(repo.save(any(Event.class))).thenReturn(savedEvent);

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        Event result = service.create(cmd);

        // Then
        assertThat(result).isNotNull();
        verify(repo).save(any(Event.class));
      }
    }
  }

  @Nested
  @DisplayName("listMine - Listage des événements de mon organisation")
  class ListMineTests {

    @Test
    @DisplayName("Devrait lister les événements d'une organisation")
    void shouldListEventsForOrganization() {
      // Given
      Pageable pageable = PageRequest.of(0, 10);
      Event event1 =
          Event.builder().id(UUID.randomUUID()).organizationId(ORG_ID).title("Event 1").build();
      Event event2 =
          Event.builder().id(UUID.randomUUID()).organizationId(ORG_ID).title("Event 2").build();
      Page<Event> page = new PageImpl<>(List.of(event1, event2), pageable, 2);

      when(repo.findAllByOrganizationIdAndDeletedAtIsNull(ORG_ID, pageable)).thenReturn(page);

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        Page<Event> result = service.listMine(null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(repo).findAllByOrganizationIdAndDeletedAtIsNull(ORG_ID, pageable);
      }
    }
  }

  @Nested
  @DisplayName("getMine - Récupération d'un événement de mon organisation")
  class GetMineTests {

    @Test
    @DisplayName("Devrait récupérer un événement existant de l'organisation")
    void shouldGetEventSuccessfully() {
      // Given
      Event event = Event.builder().id(EVENT_ID).organizationId(ORG_ID).title("My Event").build();
      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        Event result = service.getMine(null, EVENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(EVENT_ID);
        assertThat(result.getTitle()).isEqualTo("My Event");
        verify(repo).findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID);
      }
    }

    @Test
    @DisplayName("Devrait échouer si l'événement n'existe pas")
    void shouldFailIfEventNotFound() {
      // Given
      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.empty());

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.getMine(null, EVENT_ID))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Événement introuvable")
            .extracting(e -> ((AppException) e).getHttpStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);
      }
    }
  }

  @Nested
  @DisplayName("update - Mise à jour d'un événement")
  class UpdateTests {

    @Test
    @DisplayName("Devrait mettre à jour tous les champs fournis")
    void shouldUpdateAllProvidedFields() {
      // Given
      Event existing =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Old Title")
              .description("Old Desc")
              .location("Old Loc")
              .startDate(TOMORROW)
              .endDate(NEXT_WEEK)
              .status(EventStatus.DRAFT)
              .build();

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(existing));
      when(repo.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

      Instant newStart = NOW.plus(2, ChronoUnit.DAYS);
      Instant newEnd = NOW.plus(9, ChronoUnit.DAYS);

      EventService.UpdateEventCommand cmd =
          new EventService.UpdateEventCommand(
              "New Title", "New Desc", "New Loc", newStart, newEnd, EventStatus.PUBLISHED);

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        Event result = service.update(EVENT_ID, null, cmd);

        // Then
        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("New Desc");
        assertThat(result.getLocation()).isEqualTo("New Loc");
        assertThat(result.getStartDate()).isEqualTo(newStart);
        assertThat(result.getEndDate()).isEqualTo(newEnd);
        assertThat(result.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(repo).save(existing);
      }
    }

    @Test
    @DisplayName("Devrait ne mettre à jour que les champs non-null")
    void shouldUpdateOnlyNonNullFields() {
      // Given
      Event existing =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Old Title")
              .description("Old Desc")
              .location("Old Loc")
              .startDate(TOMORROW)
              .endDate(NEXT_WEEK)
              .status(EventStatus.DRAFT)
              .build();

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(existing));
      when(repo.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

      EventService.UpdateEventCommand cmd =
          new EventService.UpdateEventCommand("New Title", null, null, null, null, null);

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        Event result = service.update(EVENT_ID, null, cmd);

        // Then
        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("Old Desc"); // unchanged
        assertThat(result.getLocation()).isEqualTo("Old Loc"); // unchanged
        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT); // unchanged
      }
    }

    @Test
    @DisplayName("Devrait échouer si les nouvelles dates sont invalides")
    void shouldFailIfUpdatedDatesAreInvalid() {
      // Given
      Event existing =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .startDate(TOMORROW)
              .endDate(NEXT_WEEK)
              .build();

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(existing));

      EventService.UpdateEventCommand cmd =
          new EventService.UpdateEventCommand(null, null, null, NEXT_WEEK, TOMORROW, null);

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.update(EVENT_ID, null, cmd))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("date de fin doit être après");

        verify(repo, never()).save(any());
      }
    }

    @Test
    @DisplayName("Devrait échouer si l'événement n'existe pas")
    void shouldFailIfEventNotFoundForUpdate() {
      // Given
      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.empty());

      EventService.UpdateEventCommand cmd =
          new EventService.UpdateEventCommand("Title", null, null, null, null, null);

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.update(EVENT_ID, null, cmd))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Événement introuvable");
      }
    }
  }

  @Nested
  @DisplayName("softDelete - Suppression logique d'un événement")
  class SoftDeleteTests {

    @Test
    @DisplayName("Devrait supprimer logiquement un événement existant")
    void shouldSoftDeleteEventSuccessfully() {
      // Given
      Event event = Event.builder().id(EVENT_ID).organizationId(ORG_ID).build();
      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));
      when(repo.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        service.softDelete(EVENT_ID, null);

        // Then
        assertThat(event.getDeletedAt()).isNotNull();
        verify(repo).save(event);
      }
    }

    @Test
    @DisplayName("Devrait échouer si l'événement n'existe pas")
    void shouldFailIfEventNotFoundForDelete() {
      // Given
      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.empty());

      // Mock SecurityContext
      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.softDelete(EVENT_ID, null))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Événement introuvable");

        verify(repo, never()).save(any());
      }
    }
  }

  @Nested
  @DisplayName("listPublished - Listage des événements publics")
  class ListPublishedTests {

    @Test
    @DisplayName("Devrait lister uniquement les événements publiés")
    void shouldListOnlyPublishedEvents() {
      // Given
      Pageable pageable = PageRequest.of(0, 10);
      Event event1 =
          Event.builder()
              .id(UUID.randomUUID())
              .status(EventStatus.PUBLISHED)
              .title("Public Event 1")
              .build();
      Event event2 =
          Event.builder()
              .id(UUID.randomUUID())
              .status(EventStatus.PUBLISHED)
              .title("Public Event 2")
              .build();
      Page<Event> page = new PageImpl<>(List.of(event1, event2), pageable, 2);

      when(repo.findAllByStatusAndDeletedAtIsNull(EventStatus.PUBLISHED, pageable))
          .thenReturn(page);

      // When
      Page<Event> result = service.listPublished(pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent()).allMatch(e -> e.getStatus() == EventStatus.PUBLISHED);
      verify(repo).findAllByStatusAndDeletedAtIsNull(EventStatus.PUBLISHED, pageable);
    }
  }

  @Nested
  @DisplayName("getPublished - Récupération d'un événement public")
  class GetPublishedTests {

    @Test
    @DisplayName("Devrait récupérer un événement publié existant")
    void shouldGetPublishedEventSuccessfully() {
      // Given
      Event event =
          Event.builder().id(EVENT_ID).status(EventStatus.PUBLISHED).title("Public Event").build();
      when(repo.findByIdAndStatusAndDeletedAtIsNull(EVENT_ID, EventStatus.PUBLISHED))
          .thenReturn(Optional.of(event));

      // When
      Event result = service.getPublished(EVENT_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(EVENT_ID);
      assertThat(result.getStatus()).isEqualTo(EventStatus.PUBLISHED);
      verify(repo).findByIdAndStatusAndDeletedAtIsNull(EVENT_ID, EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Devrait échouer si l'événement n'est pas publié ou n'existe pas")
    void shouldFailIfEventNotPublished() {
      // Given
      when(repo.findByIdAndStatusAndDeletedAtIsNull(EVENT_ID, EventStatus.PUBLISHED))
          .thenReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> service.getPublished(EVENT_ID))
          .isInstanceOf(AppException.class)
          .hasMessageContaining("Événement introuvable")
          .extracting(e -> ((AppException) e).getHttpStatus())
          .isEqualTo(HttpStatus.NOT_FOUND);
    }
  }
}
