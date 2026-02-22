package com.oneevent.auth.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oneevent.auth.application.AuthService;
import com.oneevent.organization.domain.CountryCode;
import com.oneevent.shared.security.principal.AuthenticatedUser;
import com.oneevent.shared.validation.CurrencyCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/login")
  public AuthService.AuthToken login(@RequestBody LoginRequest req) {
    return auth.login(req.email(), req.password());
  }

  @PostMapping("/register-organizer")
  public AuthService.AuthToken registerOrganizer(@RequestBody RegisterOrganizerRequest req) {
    return auth.registerOrganizer(
        req.orgName(), req.countryCode(), req.currencyCode(), req.email(), req.password());
  }

  @GetMapping("/me")
  public MeResponse me(@AuthenticationPrincipal AuthenticatedUser p) {
    return new MeResponse(
        p.userId().toString(),
        p.email(),
        p.role().name(),
        p.orgId() == null ? null : p.orgId().toString());
  }

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  public record RegisterOrganizerRequest(
      @NotBlank String orgName,
      @NotNull CountryCode countryCode,
      @NotNull CurrencyCode currencyCode,
      @Email @NotBlank String email,
      @NotBlank String password) {}

  public record MeResponse(String userId, String email, String role, String orgId) {}
}
