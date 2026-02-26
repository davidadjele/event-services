package com.oneevent.ticket.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.http.HttpStatus;

import com.oneevent.event.domain.Event;
import com.oneevent.event.domain.EventStatus;
import com.oneevent.event.infrastructure.EventRepository;
import com.oneevent.shared.exception.AppException;
import com.oneevent.shared.security.SecurityContext;
import com.oneevent.ticket.domain.TicketType;
import com.oneevent.ticket.infrastructure.TicketTypeRepository;

/**
 * Tests unitaires pour le service de gestion des types de tickets.
 *
 * <p>Cette classe teste les fonctionnalités de :
 *
 * <ul>
 *   <li>Création de types de tickets
 *   <li>Listage des types de tickets (organisateur et public)
 *   <li>Mise à jour de types de tickets
 *   <li>Suppression logique de types de tickets
 *   <li>Validation des prix et quantités
 *   <li>Validation de la fenêtre de vente
 *   <li>Gestion des permissions selon les rôles
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketTypeService - Tests du service de gestion des types de tickets")
class TicketTypeServiceTest {

  @Mock private TicketTypeRepository repo;

  @Mock private EventRepository eventRepo;

  @InjectMocks private TicketTypeService service;

  private static final UUID ORG_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID TICKET_TYPE_ID = UUID.randomUUID();
  private static final Instant NOW = Instant.now();
  private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS);
  private static final Instant NEXT_WEEK = NOW.plus(7, ChronoUnit.DAYS);

  @Nested
  @DisplayName("create - Création de type de ticket")
  class CreateTests {

    @Test
    @DisplayName("Devrait créer un type de ticket avec succès")
    void shouldCreateTicketTypeSuccessfully() {
      // Given
      TicketTypeService.CreateTicketTypeCommand cmd =
          new TicketTypeService.CreateTicketTypeCommand(
              EVENT_ID, "Early Bird", new BigDecimal("25.00"), 100, TOMORROW, NEXT_WEEK);

      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Conférence Tech")
              .status(EventStatus.DRAFT)
              .startDate(NEXT_WEEK)
              .build();

      TicketType savedTicketType =
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

      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));
      when(repo.save(any(TicketType.class))).thenReturn(savedTicketType);

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        TicketType result = service.create(cmd);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Early Bird");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.getQuantityAvailable()).isEqualTo(100);
        assertThat(result.getQuantitySold()).isZero();

        ArgumentCaptor<TicketType> captor = ArgumentCaptor.forClass(TicketType.class);
        verify(repo).save(captor.capture());
        TicketType captured = captor.getValue();
        assertThat(captured.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(captured.getEventId()).isEqualTo(EVENT_ID);
      }
    }

    @Test
    @DisplayName("Devrait créer un type de ticket gratuit (prix = 0)")
    void shouldCreateFreeTicketType() {
      // Given
      TicketTypeService.CreateTicketTypeCommand cmd =
          new TicketTypeService.CreateTicketTypeCommand(
              EVENT_ID, "Gratuit", BigDecimal.ZERO, 50, null, null);

      Event event = createEvent();
      TicketType savedTicketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Gratuit")
              .price(BigDecimal.ZERO)
              .quantityAvailable(50)
              .quantitySold(0)
              .build();

      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));
      when(repo.save(any(TicketType.class))).thenReturn(savedTicketType);

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        TicketType result = service.create(cmd);

        // Then
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
      }
    }

    @Test
    @DisplayName("Devrait échouer si le prix est négatif")
    void shouldFailIfPriceIsNegative() {
      // Given
      TicketTypeService.CreateTicketTypeCommand cmd =
          new TicketTypeService.CreateTicketTypeCommand(
              EVENT_ID, "Invalid", new BigDecimal("-10.00"), 100, null, null);

      Event event = createEvent();
      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.create(cmd))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("prix doit être positif");

        verify(repo, never()).save(any());
      }
    }

    @Test
    @DisplayName("Devrait échouer si la quantité est inférieure ou égale à 0")
    void shouldFailIfQuantityIsZeroOrNegative() {
      // Given
      TicketTypeService.CreateTicketTypeCommand cmd =
          new TicketTypeService.CreateTicketTypeCommand(
              EVENT_ID, "Invalid", new BigDecimal("25.00"), 0, null, null);

      Event event = createEvent();
      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.create(cmd))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("quantité doit être supérieure à 0");

        verify(repo, never()).save(any());
      }
    }

    @Test
    @DisplayName("Devrait échouer si la date de fin de vente est avant la date de début")
    void shouldFailIfSaleEndBeforeSaleStart() {
      // Given
      TicketTypeService.CreateTicketTypeCommand cmd =
          new TicketTypeService.CreateTicketTypeCommand(
              EVENT_ID,
              "Invalid",
              new BigDecimal("25.00"),
              100,
              NEXT_WEEK, // start
              TOMORROW // end before start
              );

      Event event = createEvent();
      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.create(cmd))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("fin de vente doit être après");

        verify(repo, never()).save(any());
      }
    }

    @Test
    @DisplayName("Devrait échouer si l'événement n'existe pas")
    void shouldFailIfEventNotFound() {
      // Given
      TicketTypeService.CreateTicketTypeCommand cmd =
          new TicketTypeService.CreateTicketTypeCommand(
              EVENT_ID, "Ticket", new BigDecimal("25.00"), 100, null, null);

      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.empty());

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.create(cmd))
            .isInstanceOf(AppException.class)
            .extracting(e -> ((AppException) e).getHttpStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);

        verify(repo, never()).save(any());
      }
    }
  }

  @Nested
  @DisplayName("listByEventMine - Liste des types de tickets pour organisateur")
  class ListByEventMineTests {

    @Test
    @DisplayName("Devrait retourner la liste des types de tickets de l'organisateur")
    void shouldReturnTicketTypesForOrganizer() {
      // Given
      Event event = createEvent();
      List<TicketType> ticketTypes =
          List.of(
              createTicketType("Early Bird", new BigDecimal("25.00")),
              createTicketType("VIP", new BigDecimal("100.00")));

      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));
      when(repo.findAllByOrganizationIdAndEventIdAndDeletedAtIsNull(ORG_ID, EVENT_ID))
          .thenReturn(ticketTypes);

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        List<TicketType> result = service.listByEventMine(EVENT_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TicketType::getName).containsExactly("Early Bird", "VIP");
      }
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucun type de ticket")
    void shouldReturnEmptyListWhenNoTicketTypes() {
      // Given
      Event event = createEvent();
      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.of(event));
      when(repo.findAllByOrganizationIdAndEventIdAndDeletedAtIsNull(ORG_ID, EVENT_ID))
          .thenReturn(List.of());

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        List<TicketType> result = service.listByEventMine(EVENT_ID);

        // Then
        assertThat(result).isEmpty();
      }
    }

    @Test
    @DisplayName("Devrait échouer si l'événement n'appartient pas à l'organisateur")
    void shouldFailIfEventNotOwnedByOrganizer() {
      // Given
      when(eventRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(EVENT_ID, ORG_ID))
          .thenReturn(Optional.empty());

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.listByEventMine(EVENT_ID))
            .isInstanceOf(AppException.class)
            .extracting(e -> ((AppException) e).getHttpStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);
      }
    }
  }

  @Nested
  @DisplayName("update - Mise à jour de type de ticket")
  class UpdateTests {

    @Test
    @DisplayName("Devrait mettre à jour le nom du type de ticket")
    void shouldUpdateTicketTypeName() {
      // Given
      TicketType existing = createTicketType("Old Name", new BigDecimal("25.00"));
      TicketType updated =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("New Name")
              .price(new BigDecimal("25.00"))
              .quantityAvailable(100)
              .quantitySold(10)
              .build();

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.of(existing));
      when(repo.save(any(TicketType.class))).thenReturn(updated);

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        TicketTypeService.PatchTicketTypeCommand cmd =
            new TicketTypeService.PatchTicketTypeCommand("New Name", null, null, null, null);
        TicketType result = service.update(TICKET_TYPE_ID, cmd);

        // Then
        assertThat(result.getName()).isEqualTo("New Name");
        verify(repo).save(any(TicketType.class));
      }
    }

    @Test
    @DisplayName("Devrait mettre à jour le prix du type de ticket")
    void shouldUpdateTicketTypePrice() {
      // Given
      TicketType existing = createTicketType("Ticket", new BigDecimal("25.00"));
      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.of(existing));
      when(repo.save(any(TicketType.class))).thenAnswer(inv -> inv.getArgument(0));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        TicketTypeService.PatchTicketTypeCommand cmd =
            new TicketTypeService.PatchTicketTypeCommand(
                null, new BigDecimal("50.00"), null, null, null);
        TicketType result = service.update(TICKET_TYPE_ID, cmd);

        // Then
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
      }
    }

    @Test
    @DisplayName("Devrait échouer si la quantité disponible est inférieure à la quantité vendue")
    void shouldFailIfQuantityAvailableLessThanSold() {
      // Given
      TicketType existing = createTicketType("Ticket", new BigDecimal("25.00"));
      existing.setQuantitySold(50);
      existing.setQuantityAvailable(100);

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.of(existing));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        TicketTypeService.PatchTicketTypeCommand cmd =
            new TicketTypeService.PatchTicketTypeCommand(null, null, 30, null, null);

        assertThatThrownBy(() -> service.update(TICKET_TYPE_ID, cmd))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("quantité disponible ne peut pas être inférieure");

        verify(repo, never()).save(any());
      }
    }

    @Test
    @DisplayName("Devrait échouer si la fenêtre de vente est invalide")
    void shouldFailIfSaleWindowIsInvalid() {
      // Given
      TicketType existing = createTicketType("Ticket", new BigDecimal("25.00"));
      existing.setSaleStart(TOMORROW);
      existing.setSaleEnd(NEXT_WEEK);

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.of(existing));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then - saleEnd avant saleStart existant
        TicketTypeService.PatchTicketTypeCommand cmd =
            new TicketTypeService.PatchTicketTypeCommand(null, null, null, null, NOW);

        assertThatThrownBy(() -> service.update(TICKET_TYPE_ID, cmd))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("fin de vente doit être après");
      }
    }

    @Test
    @DisplayName("Devrait échouer si le type de ticket n'existe pas")
    void shouldFailIfTicketTypeNotFound() {
      // Given
      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.empty());

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        TicketTypeService.PatchTicketTypeCommand cmd =
            new TicketTypeService.PatchTicketTypeCommand("New", null, null, null, null);

        assertThatThrownBy(() -> service.update(TICKET_TYPE_ID, cmd))
            .isInstanceOf(AppException.class)
            .extracting(e -> ((AppException) e).getHttpStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);
      }
    }

    @Test
    @DisplayName("Devrait mettre à jour plusieurs champs en une seule requête")
    void shouldUpdateMultipleFields() {
      // Given
      TicketType existing = createTicketType("Old", new BigDecimal("25.00"));
      existing.setQuantitySold(0);

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.of(existing));
      when(repo.save(any(TicketType.class))).thenAnswer(inv -> inv.getArgument(0));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        TicketTypeService.PatchTicketTypeCommand cmd =
            new TicketTypeService.PatchTicketTypeCommand(
                "New VIP", new BigDecimal("150.00"), 200, TOMORROW, NEXT_WEEK);
        TicketType result = service.update(TICKET_TYPE_ID, cmd);

        // Then
        assertThat(result.getName()).isEqualTo("New VIP");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.getQuantityAvailable()).isEqualTo(200);
        assertThat(result.getSaleStart()).isEqualTo(TOMORROW);
        assertThat(result.getSaleEnd()).isEqualTo(NEXT_WEEK);
      }
    }
  }

  @Nested
  @DisplayName("softDelete - Suppression logique de type de ticket")
  class SoftDeleteTests {

    @Test
    @DisplayName("Devrait supprimer logiquement un type de ticket non vendu")
    void shouldSoftDeleteUnsoldTicketType() {
      // Given
      TicketType existing = createTicketType("Ticket", new BigDecimal("25.00"));
      existing.setQuantitySold(0);

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.of(existing));
      when(repo.save(any(TicketType.class))).thenAnswer(inv -> inv.getArgument(0));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When
        service.softDelete(TICKET_TYPE_ID);

        // Then
        ArgumentCaptor<TicketType> captor = ArgumentCaptor.forClass(TicketType.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
      }
    }

    @Test
    @DisplayName("Devrait échouer si des tickets ont été vendus")
    void shouldFailIfTicketsAlreadySold() {
      // Given
      TicketType existing = createTicketType("Ticket", new BigDecimal("25.00"));
      existing.setQuantitySold(10);

      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.of(existing));

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.softDelete(TICKET_TYPE_ID))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Impossible de supprimer un type de ticket déjà vendu")
            .extracting(e -> ((AppException) e).getHttpStatus())
            .isEqualTo(HttpStatus.CONFLICT);

        verify(repo, never()).save(any());
      }
    }

    @Test
    @DisplayName("Devrait échouer si le type de ticket n'existe pas")
    void shouldFailIfTicketTypeNotFound() {
      // Given
      when(repo.findByIdAndOrganizationIdAndDeletedAtIsNull(TICKET_TYPE_ID, ORG_ID))
          .thenReturn(Optional.empty());

      try (MockedStatic<SecurityContext> mockedSecurity = mockStatic(SecurityContext.class)) {
        mockedSecurity.when(SecurityContext::isSuperAdmin).thenReturn(false);
        mockedSecurity.when(SecurityContext::requireOrgId).thenReturn(ORG_ID);

        // When / Then
        assertThatThrownBy(() -> service.softDelete(TICKET_TYPE_ID))
            .isInstanceOf(AppException.class)
            .extracting(e -> ((AppException) e).getHttpStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);
      }
    }
  }

  @Nested
  @DisplayName("listForPublishedEvent - Liste publique des types de tickets")
  class ListForPublishedEventTests {

    @Test
    @DisplayName("Devrait retourner les types de tickets d'un événement publié")
    void shouldReturnTicketTypesForPublishedEvent() {
      // Given
      Event publishedEvent =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Event Publié")
              .status(EventStatus.PUBLISHED)
              .startDate(NEXT_WEEK)
              .build();

      List<TicketType> ticketTypes =
          List.of(
              createTicketType("Standard", new BigDecimal("30.00")),
              createTicketType("VIP", new BigDecimal("100.00")));

      when(eventRepo.findByIdAndStatusAndDeletedAtIsNull(EVENT_ID, EventStatus.PUBLISHED))
          .thenReturn(Optional.of(publishedEvent));
      when(repo.findAllByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(ticketTypes);

      // When
      List<TicketType> result = service.listForPublishedEvent(EVENT_ID);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result).extracting(TicketType::getName).containsExactly("Standard", "VIP");
    }

    @Test
    @DisplayName("Devrait échouer si l'événement n'est pas publié")
    void shouldFailIfEventNotPublished() {
      // Given
      when(eventRepo.findByIdAndStatusAndDeletedAtIsNull(EVENT_ID, EventStatus.PUBLISHED))
          .thenReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> service.listForPublishedEvent(EVENT_ID))
          .isInstanceOf(AppException.class)
          .extracting(e -> ((AppException) e).getHttpStatus())
          .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucun type de ticket pour l'événement publié")
    void shouldReturnEmptyListWhenNoTicketTypesForPublishedEvent() {
      // Given
      Event publishedEvent =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Event Publié")
              .status(EventStatus.PUBLISHED)
              .startDate(NEXT_WEEK)
              .build();

      when(eventRepo.findByIdAndStatusAndDeletedAtIsNull(EVENT_ID, EventStatus.PUBLISHED))
          .thenReturn(Optional.of(publishedEvent));
      when(repo.findAllByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(List.of());

      // When
      List<TicketType> result = service.listForPublishedEvent(EVENT_ID);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ===== Helper methods =====

  private Event createEvent() {
    return Event.builder()
        .id(EVENT_ID)
        .organizationId(ORG_ID)
        .title("Conférence Tech")
        .status(EventStatus.DRAFT)
        .startDate(NEXT_WEEK)
        .build();
  }

  private TicketType createTicketType(String name, BigDecimal price) {
    return TicketType.builder()
        .id(TICKET_TYPE_ID)
        .organizationId(ORG_ID)
        .eventId(EVENT_ID)
        .name(name)
        .price(price)
        .quantityAvailable(100)
        .quantitySold(10)
        .saleStart(TOMORROW)
        .saleEnd(NEXT_WEEK)
        .build();
  }
}
