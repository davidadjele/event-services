package com.oneevent.organization.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oneevent.organization.domain.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {}
