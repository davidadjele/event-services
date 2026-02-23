package com.oneevent.event.api.dto;

import java.time.Instant;

import com.oneevent.event.domain.EventStatus;

public record EventResponse(
    String id,
    String organizationId,
    String title,
    String description,
    String location,
    Instant startDate,
    Instant endDate,
    EventStatus status,
    Instant createdAt,
    Instant updatedAt) {}
