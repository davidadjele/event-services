package com.oneevent.auth.application;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.oneevent.organization.domain.CountryCode;
import com.oneevent.organization.domain.Organization;
import com.oneevent.organization.domain.OrganizationStatus;
import com.oneevent.organization.infrastructure.OrganizationRepository;
import com.oneevent.shared.exception.AppException;
import com.oneevent.shared.security.JwtService;
import com.oneevent.shared.validation.CurrencyCode;
import com.oneevent.user.domain.Role;
import com.oneevent.user.domain.User;
import com.oneevent.user.domain.UserStatus;
import com.oneevent.user.infrastructure.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  public static final String EMAIL_OU_MOT_DE_PASSE_INVALIDE = "Email ou mot de passe invalide";
  public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
  private final UserRepository userRepo;
  private final OrganizationRepository orgRepo;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthToken login(String email, String password) {
    var user =
        userRepo
            .findByEmail(email)
            .orElseThrow(
                () ->
                    AppException.builder(HttpStatus.UNAUTHORIZED)
                        .message(EMAIL_OU_MOT_DE_PASSE_INVALIDE)
                        .errorCode(AUTH_INVALID_CREDENTIALS)
                        .logMessage("Login failed: user not found email=" + email)
                        .build());

    if (!encoder.matches(password, user.getPasswordHash())) {
      throw AppException.builder(HttpStatus.UNAUTHORIZED)
          .message(EMAIL_OU_MOT_DE_PASSE_INVALIDE)
          .errorCode(AUTH_INVALID_CREDENTIALS)
          .logMessage("Login failed: bad password email=" + email)
          .build();
    }

    String token =
        jwt.createToken(user.getId(), user.getEmail(), user.getRole(), user.getOrganizationId());
    return new AuthToken(token);
  }

  public AuthToken registerOrganizer(
      String orgName,
      CountryCode countryCode,
      CurrencyCode currencyCode,
      String email,
      String password) {
    userRepo
        .findByEmail(email)
        .ifPresent(
            u -> {
              throw AppException.builder(HttpStatus.CONFLICT)
                  .message("Cet email est déjà utilisé")
                  .errorCode("AUTH_EMAIL_EXISTS")
                  .logMessage("Register organizer failed: email exists " + email)
                  .build();
            });

    UUID orgId = UUID.randomUUID();
    var org =
        Organization.builder()
            .id(orgId)
            .name(orgName)
            .countryCode(countryCode)
            .currencyCode(currencyCode)
            .status(OrganizationStatus.ACTIVE)
            .build();
    orgRepo.save(org);

    var user =
        User.builder()
            .id(UUID.randomUUID())
            .organizationId(orgId)
            .email(email)
            .passwordHash(encoder.encode(password))
            .role(Role.ORGANIZER)
            .status(UserStatus.ACTIVE)
            .build();
    userRepo.save(user);

    String token =
        jwt.createToken(user.getId(), user.getEmail(), user.getRole(), user.getOrganizationId());
    return new AuthToken(token);
  }

  public record AuthToken(String accessToken) {}
}
