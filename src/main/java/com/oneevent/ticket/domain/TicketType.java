package com.oneevent.ticket.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.oneevent.shared.auditing.AuditableEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ticket_types")
public class TicketType extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(name = "quantity_available", nullable = false)
  private int quantityAvailable;

  @Column(name = "quantity_sold", nullable = false)
  private int quantitySold;

  @Column(name = "sale_start")
  private Instant saleStart;

  @Column(name = "sale_end")
  private Instant saleEnd;
}
