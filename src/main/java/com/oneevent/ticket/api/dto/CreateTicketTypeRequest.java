package com.oneevent.ticket.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketTypeRequest(
    @NotNull UUID eventId,
    @NotBlank String name,
    @NotNull @Min(0) BigDecimal price,
    @Min(1) int quantityAvailable,
    Instant saleStart,
    Instant saleEnd) {}
