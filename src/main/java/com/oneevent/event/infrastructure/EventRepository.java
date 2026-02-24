package com.oneevent.event.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.oneevent.event.domain.Event;
import com.oneevent.event.domain.EventStatus;

public interface EventRepository extends JpaRepository<Event, UUID> {

  // Organizer scope
  Page<Event> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

  Optional<Event> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

  // Public/Participant scope
  Page<Event> findAllByStatusAndDeletedAtIsNull(EventStatus status, Pageable pageable);

  Optional<Event> findByIdAndStatusAndDeletedAtIsNull(UUID id, EventStatus status);
}
