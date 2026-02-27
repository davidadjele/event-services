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

  public static final String TICKET_TYPE_ID = " ticketTypeId=";
  private final TicketTypeRepository repo;
  private final EventRepository eventRepo;

  // ===== Organizer/Admin =====

  public TicketType create(CreateTicketTypeCommand cmd) {
    UUID orgId = SecurityContext.resolveOrgIdForWrite(cmd.organizationId());
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

  public List<TicketType> listByEventMine(UUID orgId, UUID eventId) {
    UUID resolvedOrgId = SecurityContext.resolveOrgIdForList(orgId);
    requireOwnedEvent(resolvedOrgId, eventId);
    return repo.findAllByOrganizationIdAndEventIdAndDeletedAtIsNull(resolvedOrgId, eventId);
  }

  public TicketType update(UUID ticketTypeId, UUID orgId, PatchTicketTypeCommand cmd) {
    UUID resolvedOrgId = SecurityContext.resolveOrgIdForList(orgId);

    TicketType tt =
        repo.findByIdAndOrganizationIdAndDeletedAtIsNull(ticketTypeId, resolvedOrgId)
            .orElseThrow(
                () ->
                    AppException.builder(HttpStatus.NOT_FOUND)
                        .message("Type de ticket introuvable")
                        .errorCode(ErrorCodes.TICKET_TYPE_NOT_FOUND)
                        .logMessage(
                            "[TicketType][update] Introuvable : orgId="
                                + resolvedOrgId
                                + TICKET_TYPE_ID
                                + ticketTypeId)
                        .build());

    // Guard : pas possible de réduire quantityAvailable sous quantitySold
    if (cmd.quantityAvailable() != null && cmd.quantityAvailable() < tt.getQuantitySold()) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("La quantité disponible ne peut pas être inférieure aux tickets déjà vendus")
          .errorCode(ErrorCodes.INVALID_TICKET_TYPE)
          .logMessage(
              "[TicketType][update] quantityAvailable="
                  + cmd.quantityAvailable()
                  + " < quantitySold="
                  + tt.getQuantitySold()
                  + TICKET_TYPE_ID
                  + ticketTypeId)
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

  public void softDelete(UUID ticketTypeId, UUID orgId) {
    UUID resolvedOrgId = SecurityContext.resolveOrgIdForList(orgId);

    TicketType tt =
        repo.findByIdAndOrganizationIdAndDeletedAtIsNull(ticketTypeId, resolvedOrgId)
            .orElseThrow(
                () ->
                    AppException.builder(HttpStatus.NOT_FOUND)
                        .message("Type de ticket introuvable")
                        .errorCode(ErrorCodes.TICKET_TYPE_NOT_FOUND)
                        .logMessage(
                            "[TicketType][softDelete] Introuvable : orgId="
                                + resolvedOrgId
                                + TICKET_TYPE_ID
                                + ticketTypeId)
                        .build());

    // Guard : si déjà vendu, on pourrait refuser (MVP : on refuse)
    if (tt.getQuantitySold() > 0) {
      throw AppException.builder(HttpStatus.CONFLICT)
          .message("Impossible de supprimer un type de ticket déjà vendu")
          .errorCode("TICKET_TYPE_ALREADY_SOLD")
          .logMessage(
              "[TicketType][softDelete] Tentative de suppression d'un type déjà vendu : ticketTypeId="
                  + ticketTypeId
                  + " quantitySold="
                  + tt.getQuantitySold())
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
                            "[TicketType][listPublic] Événement introuvable ou non publié : eventId="
                                + eventId)
                        .build());

    return repo.findAllByEventIdAndDeletedAtIsNull(event.getId());
  }

  // ===== Helpers =====

  private Event requireOwnedEvent(UUID orgId, UUID eventId) {
    return eventRepo
        .findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, orgId)
        .orElseThrow(
            () ->
                AppException.builder(HttpStatus.NOT_FOUND)
                    .message("Événement introuvable")
                    .errorCode(ErrorCodes.EVENT_NOT_FOUND)
                    .logMessage(
                        "[TicketType] Événement introuvable ou non possédé : orgId="
                            + orgId
                            + " eventId="
                            + eventId)
                    .build());
  }

  private void validateCreate(CreateTicketTypeCommand cmd) {
    if (cmd.price().compareTo(BigDecimal.ZERO) < 0) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("Le prix doit être positif")
          .errorCode(ErrorCodes.INVALID_TICKET_TYPE)
          .logMessage("[TicketType][create] Prix négatif : price=" + cmd.price())
          .build();
    }
    if (cmd.quantityAvailable() <= 0) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("La quantité doit être supérieure à 0")
          .errorCode(ErrorCodes.INVALID_TICKET_TYPE)
          .logMessage(
              "[TicketType][create] Quantité invalide : quantityAvailable="
                  + cmd.quantityAvailable())
          .build();
    }
    validateSaleWindow(cmd.saleStart(), cmd.saleEnd());
  }

  private void validateSaleWindow(Instant start, Instant end) {
    if (start != null && end != null && end.isBefore(start)) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("La fin de vente doit être après le début de vente")
          .errorCode(ErrorCodes.INVALID_TICKET_TYPE)
          .logMessage(
              "[TicketType] Fenêtre de vente invalide : saleStart=" + start + " saleEnd=" + end)
          .build();
    }
  }

  // ===== Commands =====

  public record CreateTicketTypeCommand(
      UUID organizationId, // requis si SUPER_ADMIN, ignoré pour ORGANIZER
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
