package com.oneevent.ticket.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateTicketTypeRequest(
    String name, BigDecimal price, Integer quantityAvailable, Instant saleStart, Instant saleEnd) {}
