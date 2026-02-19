package com.oneevent.organization.domain;

import java.util.UUID;

import com.oneevent.shared.auditing.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "organizations")
public class Organization extends AuditableEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(name = "country_code", nullable = false, length = 2)
  private String countryCode;

  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode;

  @Column(nullable = false, length = 30)
  private String status; // ACTIVE, SUSPENDED
}
