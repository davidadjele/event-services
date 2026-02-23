package com.oneevent.shared.security.principal;

import java.util.UUID;

import com.oneevent.user.domain.Role;

public record AuthenticatedUser(UUID userId, String email, Role role, UUID orgId) {}
