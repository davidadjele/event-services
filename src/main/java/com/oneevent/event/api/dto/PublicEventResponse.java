package com.oneevent.event.api.dto;

import java.time.Instant;

import com.oneevent.event.domain.EventStatus;

/** Public response (sans orgId) */
public record PublicEventResponse(
    String id,
    String title,
    String description,
    String location,
    Instant startDate,
    Instant endDate,
    EventStatus status) {}
