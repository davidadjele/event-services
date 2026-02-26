package com.oneevent.ticket.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketTypeResponse(
    String id,
    String eventId,
    String name,
    BigDecimal price,
    int quantityAvailable,
    int quantitySold,
    Instant saleStart,
    Instant saleEnd,
    Instant createdAt,
    Instant updatedAt) {}
