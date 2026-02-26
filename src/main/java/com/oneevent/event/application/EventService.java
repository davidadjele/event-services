package com.oneevent.event.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.oneevent.event.domain.Event;
import com.oneevent.event.domain.EventStatus;
import com.oneevent.event.infrastructure.EventRepository;
import com.oneevent.shared.exception.AppException;
import com.oneevent.shared.security.SecurityContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

  private static final String EVENEMENT_INTROUVABLE = "Événement introuvable";
  private static final String NOT_FOUND = "EVENT_NOT_FOUND";
  public static final String EVENT_ID = " eventId=";

  private final EventRepository repo;

  // ===== Organizer/Admin (auth) =====

  public Event create(CreateEventCommand cmd) {
    UUID orgId = SecurityContext.resolveOrgIdForWrite(cmd.organizationId());

    validateDates(cmd.startDate(), cmd.endDate());

    Event e =
        Event.builder()
            .id(UUID.randomUUID())
            .organizationId(orgId)
            .title(cmd.title())
            .description(cmd.description())
            .location(cmd.location())
            .startDate(cmd.startDate())
            .endDate(cmd.endDate())
            .status(cmd.status())
            .build();

    return repo.save(e);
  }

  public Page<Event> listMine(UUID orgIdFilterForAdmin, Pageable pageable) {
    UUID orgId = SecurityContext.resolveOrgIdForList(orgIdFilterForAdmin);
    return repo.findAllByOrganizationIdAndDeletedAtIsNull(orgId, pageable);
  }

  public Event getMine(UUID orgIdFilterForAdmin, UUID eventId) {
    UUID orgId = SecurityContext.resolveOrgIdForList(orgIdFilterForAdmin);
    return repo.findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, orgId)
        .orElseThrow(() -> notFound(orgId, eventId));
  }

  public Event update(UUID eventId, UUID orgIdFilterForAdmin, UpdateEventCommand cmd) {
    UUID orgId = SecurityContext.resolveOrgIdForList(orgIdFilterForAdmin);
    Event e =
        repo.findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, orgId)
            .orElseThrow(() -> notFound(orgId, eventId));

    Instant start = cmd.startDate() != null ? cmd.startDate() : e.getStartDate();
    Instant end = cmd.endDate() != null ? cmd.endDate() : e.getEndDate();
    validateDates(start, end);

    if (cmd.title() != null) e.setTitle(cmd.title());
    if (cmd.description() != null) e.setDescription(cmd.description());
    if (cmd.location() != null) e.setLocation(cmd.location());
    if (cmd.startDate() != null) e.setStartDate(cmd.startDate());
    if (cmd.endDate() != null) e.setEndDate(cmd.endDate());
    if (cmd.status() != null) e.setStatus(cmd.status());

    return repo.save(e);
  }

  public void softDelete(UUID eventId, UUID orgIdFilterForAdmin) {
    UUID orgId = SecurityContext.resolveOrgIdForList(orgIdFilterForAdmin);
    Event e =
        repo.findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, orgId)
            .orElseThrow(() -> notFound(orgId, eventId));

    e.setDeletedAt(Instant.now());
    repo.save(e);
  }

  // ===== Public/Participant =====

  public Page<Event> listPublished(Pageable pageable) {
    return repo.findAllByStatusAndDeletedAtIsNull(EventStatus.PUBLISHED, pageable);
  }

  public Event getPublished(UUID eventId) {
    return repo.findByIdAndStatusAndDeletedAtIsNull(eventId, EventStatus.PUBLISHED)
        .orElseThrow(
            () ->
                AppException.builder(HttpStatus.NOT_FOUND)
                    .message(EVENEMENT_INTROUVABLE)
                    .errorCode(NOT_FOUND)
                    .logMessage("[Event][getPublished] Introuvable : eventId=" + eventId)
                    .build());
  }

  // ===== helpers =====

  private void validateDates(Instant start, Instant end) {
    if (end != null && end.isBefore(start)) {
      throw AppException.builder(HttpStatus.BAD_REQUEST)
          .message("La date de fin doit être après la date de début")
          .errorCode("INVALID_DATES")
          .logMessage("[Event] Dates invalides : start=" + start + " end=" + end)
          .build();
    }
  }

  private AppException notFound(UUID orgId, UUID eventId) {
    return AppException.builder(HttpStatus.NOT_FOUND)
        .message(EVENEMENT_INTROUVABLE)
        .errorCode(NOT_FOUND)
        .logMessage("[Event] Introuvable : orgId=" + orgId + EVENT_ID + eventId)
        .build();
  }

  // ===== Commands =====
  public record CreateEventCommand(
      UUID organizationId, // requis si SUPER_ADMIN
      String title,
      String description,
      String location,
      Instant startDate,
      Instant endDate,
      EventStatus status) {}

  public record UpdateEventCommand(
      String title,
      String description,
      String location,
      Instant startDate,
      Instant endDate,
      EventStatus status) {}
}
