package com.oneevent.ticket.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oneevent.ticket.domain.TicketType;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

  List<TicketType> findAllByOrganizationIdAndEventIdAndDeletedAtIsNull(
      UUID organizationId, UUID eventId);

  Optional<TicketType> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

  Optional<TicketType> findByIdAndDeletedAtIsNull(UUID id);

  // public: ticket types d’un event (on filtrera l’event PUBLISHED côté service)
  List<TicketType> findAllByEventIdAndDeletedAtIsNull(UUID eventId);
}
