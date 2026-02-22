package com.oneevent.organization.domain;

import java.util.UUID;

import com.oneevent.shared.auditing.AuditableEntity;
import com.oneevent.shared.validation.CurrencyCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Enumerated(EnumType.STRING)
  @Column(name = "country_code", nullable = false, length = 3)
  private CountryCode countryCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency_code", nullable = false, length = 3)
  private CurrencyCode currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private OrganizationStatus status; // ACTIVE, SUSPENDED
}
