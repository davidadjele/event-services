package com.oneevent.event.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.oneevent.event.domain.EventStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEventRequest(
    UUID organizationId, // requis si SUPER_ADMIN
    @NotBlank String title,
    String description,
    String location,
    @NotNull Instant startDate,
    Instant endDate,
    @NotNull EventStatus status) {}
