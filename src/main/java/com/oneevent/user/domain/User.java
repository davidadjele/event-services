package com.oneevent.user.domain;

import java.util.UUID;

import com.oneevent.shared.auditing.AuditableEntity;

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
@Table(name = "users")
public class User extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "organization_id")
  private UUID organizationId; // null si SUPER_ADMIN

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Role role; // SUPER_ADMIN, ORGANIZER, SCANNER, PARTICIPANT

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserStatus status; // ACTIVE, SUSPENDED
}
