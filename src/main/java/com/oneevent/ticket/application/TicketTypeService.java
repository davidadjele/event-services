package com.oneevent.ticket.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.oneevent.event.domain.Event;
import com.oneevent.event.domain.EventStatus;
import com.oneevent.event.infrastructure.EventRepository;
import com.oneevent.shared.constants.ErrorCodes;
import com.oneevent.shared.exception.AppException;
import com.oneevent.shared.security.SecurityContext;
import com.oneevent.ticket.domain.TicketType;
import com.oneevent.ticket.infrastructure.TicketTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketTypeService {

  private final TicketTypeRepository repo;
  private final EventRepository eventRepo;

  // ===== Organizer/Admin =====

  public TicketType create(CreateTicketTypeCommand cmd) {
    UUID orgId = requireOrgWriteScope();
    Event event = requireOwnedEvent(orgId, cmd.eventId());

    validateCreate(cmd);

    TicketType tt =
        TicketType.builder()
            .id(UUID.randomUUID())
            .organizationId(event.getOrganizationId())
            .eventId(event.getId())
            .name(cmd.name())
            .price(cmd.price())
            .quantityAvailable(cmd.quantityAvailable())
            .quantitySold(0)
            .saleStart(cmd.saleStart())
            .saleEnd(cmd.saleEnd())
            .build();

    return repo.save(tt);
  }

  public List<TicketType> listByEventMine(UUID eventId) {
    UUID orgId = requireOrgWriteScope();
    requireOwnedEvent(orgId, eventId);
    return repo.findAllByOrganizationIdAndEventIdAndDeletedAtIsNull(orgId, eventId);
  }

  public TicketType update(UUID ticketTypeId, PatchTicketTypeCommand cmd) {
    UUID orgId = requireOrgWriteScope();

    TicketType tt =
        repo.findByIdAndOrganizationIdAndDeletedAtIsNull(ticketTypeId, orgId)
            .orElseThrow(
                () ->
                    AppException.builder(HttpStatus.NOT_FOUND)
                        .message("Type de ticket introuvable")
                        .errorCode(ErrorCodes.TICKET_TYPE_NOT_FOUND)
                        .logMessage(
                            "TicketType not found orgId=" + orgId + " ticketTypeId=" + ticketTypeId)
                        .build());

    // Guard : pas possible de réduire quantityAvailable sous quantitySold
    if (cmd.quantityAvailable() != null && cmd.quantityAvailable() < tt.getQuantitySold()) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("La quantité disponible ne peut pas être inférieure aux tickets déjà vendus")
          .errorCode(ErrorCodes.INVALID_TICKET_TYPE)
          .logMessage("Invalid quantityAvailable < quantitySold ticketTypeId=" + ticketTypeId)
          .build();
    }

    Instant start = cmd.saleStart() != null ? cmd.saleStart() : tt.getSaleStart();
    Instant end = cmd.saleEnd() != null ? cmd.saleEnd() : tt.getSaleEnd();
    validateSaleWindow(start, end);

    if (cmd.name() != null) tt.setName(cmd.name());
    if (cmd.price() != null) tt.setPrice(cmd.price());
    if (cmd.quantityAvailable() != null) tt.setQuantityAvailable(cmd.quantityAvailable());
    if (cmd.saleStart() != null) tt.setSaleStart(cmd.saleStart());
    if (cmd.saleEnd() != null) tt.setSaleEnd(cmd.saleEnd());

    return repo.save(tt);
  }

  public void softDelete(UUID ticketTypeId) {
    UUID orgId = requireOrgWriteScope();

    TicketType tt =
        repo.findByIdAndOrganizationIdAndDeletedAtIsNull(ticketTypeId, orgId)
            .orElseThrow(
                () ->
                    AppException.builder(HttpStatus.NOT_FOUND)
                        .message("Type de ticket introuvable")
                        .errorCode(ErrorCodes.TICKET_TYPE_NOT_FOUND)
                        .logMessage(
                            "Delete ticketType not found orgId="
                                + orgId
                                + " ticketTypeId="
                                + ticketTypeId)
                        .build());

    // Guard : si déjà vendu, on pourrait refuser (MVP : on refuse)
    if (tt.getQuantitySold() > 0) {
      throw AppException.builder(HttpStatus.CONFLICT)
          .message("Impossible de supprimer un type de ticket déjà vendu")
          .errorCode("TICKET_TYPE_ALREADY_SOLD")
          .logMessage("Attempt to delete sold ticketTypeId=" + ticketTypeId)
          .build();
    }

    tt.setDeletedAt(Instant.now());
    repo.save(tt);
  }

  // ===== Public/Participant =====

  public List<TicketType> listForPublishedEvent(UUID eventId) {
    Event event =
        eventRepo
            .findByIdAndStatusAndDeletedAtIsNull(eventId, EventStatus.PUBLISHED)
            .orElseThrow(
                () ->
                    AppException.builder(HttpStatus.NOT_FOUND)
                        .message("Événement introuvable")
                        .errorCode(ErrorCodes.EVENT_NOT_FOUND)
                        .logMessage(
                            "Public ticket types: event not found/published eventId=" + eventId)
                        .build());

    return repo.findAllByEventIdAndDeletedAtIsNull(event.getId());
  }

  // ===== Helpers =====

  // TODO: factoriser avec EventService.requireOrgWriteScope() et requireOwnedEvent() afin d'avoir
  // une logique de sécurité centralisée
  private UUID requireOrgWriteScope() {
    if (SecurityContext.isSuperAdmin()) {
      // MVP: on évite “admin write” sans préciser le contexte org
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("Filtre organization requis pour SUPER_ADMIN")
          .errorCode(ErrorCodes.ORG_FILTER_REQUIRED)
          .logMessage("SUPER_ADMIN write without org context")
          .build();
    }
    return SecurityContext.requireOrgId();
  }

  private Event requireOwnedEvent(UUID orgId, UUID eventId) {
    return eventRepo
        .findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, orgId)
        .orElseThrow(
            () ->
                AppException.builder(HttpStatus.NOT_FOUND)
                    .message("Événement introuvable")
                    .errorCode(ErrorCodes.EVENT_NOT_FOUND)
                    .logMessage("Owned event not found orgId=" + orgId + " eventId=" + eventId)
                    .build());
  }

  private void validateCreate(CreateTicketTypeCommand cmd) {
    if (cmd.price().compareTo(BigDecimal.ZERO) < 0) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("Le prix doit être positif")
          .errorCode(ErrorCodes.INVALID_TICKET_TYPE)
          .logMessage("Invalid price < 0")
          .build();
    }
    if (cmd.quantityAvailable() <= 0) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("La quantité doit être supérieure à 0")
          .errorCode(ErrorCodes.INVALID_TICKET_TYPE)
          .logMessage("Invalid quantityAvailable <= 0")
          .build();
    }
    validateSaleWindow(cmd.saleStart(), cmd.saleEnd());
  }

  private void validateSaleWindow(Instant start, Instant end) {
    if (start != null && end != null && end.isBefore(start)) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("La fin de vente doit être après le début de vente")
          .errorCode(ErrorCodes.INVALID_TICKET_TYPE)
          .logMessage("Invalid sale window start=" + start + " end=" + end)
          .build();
    }
  }

  // ===== Commands =====

  public record CreateTicketTypeCommand(
      UUID eventId,
      String name,
      BigDecimal price,
      int quantityAvailable,
      Instant saleStart,
      Instant saleEnd) {}

  public record PatchTicketTypeCommand(
      String name,
      BigDecimal price,
      Integer quantityAvailable,
      Instant saleStart,
      Instant saleEnd) {}
}
