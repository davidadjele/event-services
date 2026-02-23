package com.oneevent.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

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

/**
 * Tests unitaires pour le service d'authentification.
 *
 * <p>Cette classe teste les fonctionnalités de :
 *
 * <ul>
 *   <li>Connexion des utilisateurs (organisateurs et participants)
 *   <li>Inscription des organisateurs avec création d'organisation
 *   <li>Inscription des participants
 *   <li>Validation des emails et mots de passe
 *   <li>Gestion des erreurs (email déjà utilisé, credentials invalides)
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests du service d'authentification")
class AuthServiceTest {

  @Mock private UserRepository userRepo;

  @Mock private OrganizationRepository orgRepo;

  @Mock private PasswordEncoder encoder;

  @Mock private JwtService jwt;

  @InjectMocks private AuthService authService;

  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_PASSWORD = "password123";
  private static final String ENCODED_PASSWORD = "$2a$10$encodedPasswordHash";
  private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

  @Nested
  @DisplayName("Login - Connexion utilisateur")
  class LoginTests {

    private User existingUser;

    @BeforeEach
    void setUp() {
      existingUser =
          User.builder()
              .id(UUID.randomUUID())
              .email(TEST_EMAIL)
              .passwordHash(ENCODED_PASSWORD)
              .role(Role.PARTICIPANT)
              .status(UserStatus.ACTIVE)
              .organizationId(null)
              .build();
    }

    @Test
    @DisplayName("Devrait retourner un token JWT valide lors d'une connexion réussie")
    void shouldReturnTokenWhenLoginSuccessful() {
      // Given
      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));
      when(encoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
      when(jwt.createToken(
              existingUser.getId(),
              existingUser.getEmail(),
              existingUser.getRole(),
              existingUser.getOrganizationId()))
          .thenReturn(JWT_TOKEN);

      // When
      AuthService.AuthToken result = authService.login(TEST_EMAIL, TEST_PASSWORD);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.accessToken()).isEqualTo(JWT_TOKEN);
      assertThat(result.role()).isEqualTo(Role.PARTICIPANT);

      verify(userRepo).findByEmail(TEST_EMAIL);
      verify(encoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
      verify(jwt)
          .createToken(
              existingUser.getId(),
              existingUser.getEmail(),
              existingUser.getRole(),
              existingUser.getOrganizationId());
    }

    @Test
    @DisplayName(
        "Devrait retourner un token avec orgId pour un organisateur lors d'une connexion réussie")
    void shouldReturnTokenWithOrgIdForOrganizer() {
      // Given
      UUID orgId = UUID.randomUUID();
      User organizer =
          User.builder()
              .id(UUID.randomUUID())
              .email(TEST_EMAIL)
              .passwordHash(ENCODED_PASSWORD)
              .role(Role.ORGANIZER)
              .status(UserStatus.ACTIVE)
              .organizationId(orgId)
              .build();

      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(organizer));
      when(encoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
      when(jwt.createToken(organizer.getId(), organizer.getEmail(), organizer.getRole(), orgId))
          .thenReturn(JWT_TOKEN);

      // When
      AuthService.AuthToken result = authService.login(TEST_EMAIL, TEST_PASSWORD);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.accessToken()).isEqualTo(JWT_TOKEN);
      assertThat(result.role()).isEqualTo(Role.ORGANIZER);

      verify(jwt).createToken(organizer.getId(), organizer.getEmail(), organizer.getRole(), orgId);
    }

    @Test
    @DisplayName("Devrait lancer une exception 401 si l'utilisateur n'existe pas")
    void shouldThrowUnauthorizedWhenUserNotFound() {
      // Given
      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> authService.login(TEST_EMAIL, TEST_PASSWORD))
          .isInstanceOf(AppException.class)
          .hasMessageContaining("Email ou mot de passe invalide")
          .extracting(e -> ((AppException) e).getHttpStatus())
          .isEqualTo(HttpStatus.UNAUTHORIZED);

      verify(userRepo).findByEmail(TEST_EMAIL);
      verify(encoder, never()).matches(anyString(), anyString());
      verify(jwt, never()).createToken(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Devrait lancer une exception 401 si le mot de passe est incorrect")
    void shouldThrowUnauthorizedWhenPasswordInvalid() {
      // Given
      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));
      when(encoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

      // When / Then
      assertThatThrownBy(() -> authService.login(TEST_EMAIL, TEST_PASSWORD))
          .isInstanceOf(AppException.class)
          .hasMessageContaining("Email ou mot de passe invalide")
          .extracting(e -> ((AppException) e).getHttpStatus())
          .isEqualTo(HttpStatus.UNAUTHORIZED);

      verify(userRepo).findByEmail(TEST_EMAIL);
      verify(encoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
      verify(jwt, never()).createToken(any(), anyString(), any(), any());
    }
  }

  @Nested
  @DisplayName("Register Organizer - Inscription organisateur")
  class RegisterOrganizerTests {

    private static final String ORG_NAME = "Events Afrique Inc.";
    private static final CountryCode COUNTRY_CODE = CountryCode.TG;
    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.EUR;

    @Test
    @DisplayName("Devrait créer une organisation et un utilisateur organisateur avec succès")
    void shouldRegisterOrganizerSuccessfully() {
      // Given
      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
      when(encoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
      when(jwt.createToken(any(UUID.class), eq(TEST_EMAIL), eq(Role.ORGANIZER), any(UUID.class)))
          .thenReturn(JWT_TOKEN);

      // When
      AuthService.AuthToken result =
          authService.registerOrganizer(
              ORG_NAME, COUNTRY_CODE, CURRENCY_CODE, TEST_EMAIL, TEST_PASSWORD);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.accessToken()).isEqualTo(JWT_TOKEN);
      assertThat(result.role()).isEqualTo(Role.ORGANIZER);

      // Vérifier que l'organisation a été créée
      ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
      verify(orgRepo).save(orgCaptor.capture());
      Organization savedOrg = orgCaptor.getValue();
      assertThat(savedOrg.getName()).isEqualTo(ORG_NAME);
      assertThat(savedOrg.getCountryCode()).isEqualTo(COUNTRY_CODE);
      assertThat(savedOrg.getCurrencyCode()).isEqualTo(CURRENCY_CODE);
      assertThat(savedOrg.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
      assertThat(savedOrg.getId()).isNotNull();

      // Vérifier que l'utilisateur a été créé
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepo).save(userCaptor.capture());
      User savedUser = userCaptor.getValue();
      assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL);
      assertThat(savedUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
      assertThat(savedUser.getRole()).isEqualTo(Role.ORGANIZER);
      assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(savedUser.getOrganizationId()).isEqualTo(savedOrg.getId());
      assertThat(savedUser.getId()).isNotNull();

      verify(userRepo).findByEmail(TEST_EMAIL);
      verify(encoder).encode(TEST_PASSWORD);
    }

    @Test
    @DisplayName("Devrait lancer une exception 409 si l'email existe déjà")
    void shouldThrowConflictWhenEmailExists() {
      // Given
      User existingUser =
          User.builder()
              .id(UUID.randomUUID())
              .email(TEST_EMAIL)
              .passwordHash(ENCODED_PASSWORD)
              .role(Role.PARTICIPANT)
              .status(UserStatus.ACTIVE)
              .build();

      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

      // When / Then
      assertThatThrownBy(
              () ->
                  authService.registerOrganizer(
                      ORG_NAME, COUNTRY_CODE, CURRENCY_CODE, TEST_EMAIL, TEST_PASSWORD))
          .isInstanceOf(AppException.class)
          .hasMessageContaining("Cet email est déjà utilisé")
          .extracting(e -> ((AppException) e).getHttpStatus())
          .isEqualTo(HttpStatus.CONFLICT);

      verify(userRepo).findByEmail(TEST_EMAIL);
      verify(orgRepo, never()).save(any());
      verify(userRepo, never()).save(any());
      verify(encoder, never()).encode(anyString());
      verify(jwt, never()).createToken(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Devrait générer un UUID différent pour l'organisation et l'utilisateur")
    void shouldGenerateDifferentUuidsForOrgAndUser() {
      // Given
      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
      when(encoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
      when(jwt.createToken(any(UUID.class), eq(TEST_EMAIL), eq(Role.ORGANIZER), any(UUID.class)))
          .thenReturn(JWT_TOKEN);

      // When
      authService.registerOrganizer(
          ORG_NAME, COUNTRY_CODE, CURRENCY_CODE, TEST_EMAIL, TEST_PASSWORD);

      // Then
      ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(orgRepo).save(orgCaptor.capture());
      verify(userRepo).save(userCaptor.capture());

      Organization savedOrg = orgCaptor.getValue();
      User savedUser = userCaptor.getValue();

      assertThat(savedOrg.getId()).isNotEqualTo(savedUser.getId());
      assertThat(savedUser.getOrganizationId()).isEqualTo(savedOrg.getId());
    }
  }

  @Nested
  @DisplayName("Register Participant - Inscription participant")
  class RegisterParticipantTests {

    @Test
    @DisplayName("Devrait créer un utilisateur participant avec succès")
    void shouldRegisterParticipantSuccessfully() {
      // Given
      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
      when(encoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
      when(jwt.createToken(any(UUID.class), eq(TEST_EMAIL), eq(Role.PARTICIPANT), eq(null)))
          .thenReturn(JWT_TOKEN);

      // When
      AuthService.AuthToken result = authService.registerParticipant(TEST_EMAIL, TEST_PASSWORD);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.accessToken()).isEqualTo(JWT_TOKEN);
      assertThat(result.role()).isEqualTo(Role.PARTICIPANT);

      // Vérifier que l'utilisateur a été créé
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepo).save(userCaptor.capture());
      User savedUser = userCaptor.getValue();
      assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL);
      assertThat(savedUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
      assertThat(savedUser.getRole()).isEqualTo(Role.PARTICIPANT);
      assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(savedUser.getOrganizationId()).isNull();
      assertThat(savedUser.getId()).isNotNull();

      verify(userRepo).findByEmail(TEST_EMAIL);
      verify(encoder).encode(TEST_PASSWORD);
      verify(orgRepo, never()).save(any());
    }

    @Test
    @DisplayName("Devrait lancer une exception 409 si l'email existe déjà")
    void shouldThrowConflictWhenEmailExists() {
      // Given
      User existingUser =
          User.builder()
              .id(UUID.randomUUID())
              .email(TEST_EMAIL)
              .passwordHash(ENCODED_PASSWORD)
              .role(Role.ORGANIZER)
              .status(UserStatus.ACTIVE)
              .build();

      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

      // When / Then
      assertThatThrownBy(() -> authService.registerParticipant(TEST_EMAIL, TEST_PASSWORD))
          .isInstanceOf(AppException.class)
          .hasMessageContaining("Cet email est déjà utilisé")
          .extracting(e -> ((AppException) e).getHttpStatus())
          .isEqualTo(HttpStatus.CONFLICT);

      verify(userRepo).findByEmail(TEST_EMAIL);
      verify(userRepo, never()).save(any());
      verify(encoder, never()).encode(anyString());
      verify(jwt, never()).createToken(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Devrait créer un participant sans organisation (organizationId null)")
    void shouldCreateParticipantWithoutOrganization() {
      // Given
      when(userRepo.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
      when(encoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
      when(jwt.createToken(any(UUID.class), eq(TEST_EMAIL), eq(Role.PARTICIPANT), eq(null)))
          .thenReturn(JWT_TOKEN);

      // When
      authService.registerParticipant(TEST_EMAIL, TEST_PASSWORD);

      // Then
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepo).save(userCaptor.capture());
      User savedUser = userCaptor.getValue();

      assertThat(savedUser.getOrganizationId()).isNull();
      verify(jwt).createToken(savedUser.getId(), TEST_EMAIL, Role.PARTICIPANT, null);
    }
  }
}
