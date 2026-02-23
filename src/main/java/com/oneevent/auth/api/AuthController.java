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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "Authentification",
    description = "API de gestion de l'authentification et inscription des utilisateurs")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService auth;

  @Operation(
      summary = "Connexion utilisateur",
      description =
          "Authentifie un utilisateur (organisateur ou participant) avec email et mot de passe et retourne un token JWT")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Connexion réussie",
            content = @Content(schema = @Schema(implementation = AuthService.AuthToken.class))),
        @ApiResponse(responseCode = "401", description = "Email ou mot de passe invalide"),
        @ApiResponse(responseCode = "400", description = "Requête invalide (validation échouée)")
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Identifiants de connexion (email et mot de passe)",
      required = true)
  @PostMapping("/login")
  public AuthService.AuthToken login(@Valid @RequestBody LoginRequest req) {
    return auth.login(req.email(), req.password());
  }

  @Operation(
      summary = "Inscription participant",
      description =
          "Crée un nouveau compte participant (utilisateur sans organisation) et retourne un token JWT")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Inscription réussie",
            content = @Content(schema = @Schema(implementation = AuthService.AuthToken.class))),
        @ApiResponse(responseCode = "409", description = "Cet email est déjà utilisé"),
        @ApiResponse(responseCode = "400", description = "Requête invalide (validation échouée)")
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Informations d'inscription du participant",
      required = true)
  @PostMapping("/register-participant")
  public AuthService.AuthToken registerParticipant(
      @Valid @RequestBody RegisterParticipantRequest req) {
    return auth.registerParticipant(req.email(), req.password());
  }

  @Operation(
      summary = "Inscription organisateur",
      description =
          "Crée un nouveau compte organisateur avec une organisation et retourne un token JWT")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Inscription réussie",
            content = @Content(schema = @Schema(implementation = AuthService.AuthToken.class))),
        @ApiResponse(responseCode = "409", description = "Cet email est déjà utilisé"),
        @ApiResponse(responseCode = "400", description = "Requête invalide (validation échouée)")
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Informations d'inscription de l'organisateur et de son organisation",
      required = true)
  @PostMapping("/register-organizer")
  public AuthService.AuthToken registerOrganizer(@Valid @RequestBody RegisterOrganizerRequest req) {
    return auth.registerOrganizer(
        req.orgName(), req.countryCode(), req.currencyCode(), req.email(), req.password());
  }

  @Operation(
      summary = "Récupérer le profil de l'utilisateur connecté",
      description = "Retourne les informations de l'utilisateur actuellement authentifié",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profil récupéré avec succès",
            content = @Content(schema = @Schema(implementation = MeResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (token manquant ou invalide)")
      })
  @GetMapping("/me")
  public MeResponse me(@AuthenticationPrincipal AuthenticatedUser p) {
    return new MeResponse(
        p.userId().toString(),
        p.email(),
        p.role().name(),
        p.orgId() == null ? null : p.orgId().toString());
  }

  @Schema(description = "Requête de connexion utilisateur")
  public record LoginRequest(
      @Schema(description = "Adresse email de l'utilisateur", example = "user@example.com")
          @Email
          @NotBlank
          String email,
      @Schema(description = "Mot de passe de l'utilisateur", example = "motdepasse123") @NotBlank
          String password) {}

  @Schema(description = "Requête d'inscription participant")
  public record RegisterParticipantRequest(
      @Schema(description = "Adresse email du participant", example = "participant@example.com")
          @Email
          @NotBlank
          String email,
      @Schema(
              description = "Mot de passe du participant (minimum 6 caractères)",
              example = "password123")
          @NotBlank
          String password) {}

  @Schema(description = "Requête d'inscription organisateur")
  public record RegisterOrganizerRequest(
      @Schema(description = "Nom de l'organisation", example = "Events Afrique Inc.") @NotBlank
          String orgName,
      @Schema(
              description = "Code pays de l'organisation (ISO 3166-1 alpha-2 ou alpha-3)",
              example = "FRA")
          @NotNull
          CountryCode countryCode,
      @Schema(description = "Code devise de l'organisation (ISO 4217)", example = "EUR") @NotNull
          CurrencyCode currencyCode,
      @Schema(description = "Adresse email de l'organisateur", example = "organizer@example.com")
          @Email
          @NotBlank
          String email,
      @Schema(description = "Mot de passe de l'organisateur", example = "securepass123") @NotBlank
          String password) {}

  @Schema(description = "Informations du profil utilisateur")
  public record MeResponse(
      @Schema(
              description = "Identifiant unique de l'utilisateur (UUID)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          String userId,
      @Schema(description = "Adresse email de l'utilisateur", example = "user@example.com")
          String email,
      @Schema(
              description = "Rôle de l'utilisateur",
              example = "ORGANIZER",
              allowableValues = {"ORGANIZER", "PARTICIPANT"})
          String role,
      @Schema(
              description = "Identifiant de l'organisation (null pour les participants)",
              example = "456e7890-e89b-12d3-a456-426614174111",
              nullable = true)
          String orgId) {}
}
