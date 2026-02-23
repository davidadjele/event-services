package com.oneevent.auth.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneevent.auth.application.AuthService;
import com.oneevent.organization.domain.CountryCode;
import com.oneevent.shared.security.principal.AuthenticatedUser;
import com.oneevent.shared.validation.CurrencyCode;
import com.oneevent.user.domain.Role;

/**
 * Tests d'intégration pour le contrôleur d'authentification.
 *
 * <p>Cette classe teste les endpoints HTTP de l'API d'authentification :
 *
 * <ul>
 *   <li>POST /api/v1/auth/login - Connexion
 *   <li>POST /api/v1/auth/register-participant - Inscription participant
 *   <li>POST /api/v1/auth/register-organizer - Inscription organisateur
 *   <li>GET /api/v1/auth/me - Profil utilisateur
 * </ul>
 *
 * <p>Les tests vérifient :
 *
 * <ul>
 *   <li>Les codes de statut HTTP
 *   <li>La structure des réponses JSON
 *   <li>La validation des requêtes
 *   <li>L'authentification JWT
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController - Tests du contrôleur d'authentification")
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthService authService;

  private static final String BASE_URL = "/api/v1/auth";
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_PASSWORD = "password123";
  private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

  @Nested
  @DisplayName("POST /login - Connexion utilisateur")
  class LoginTests {

    @Test
    @DisplayName("Devrait retourner 200 et un token JWT lors d'une connexion réussie")
    void shouldReturn200WithTokenWhenLoginSuccessful() throws Exception {
      // Given
      AuthService.AuthToken expectedToken = new AuthService.AuthToken(JWT_TOKEN, Role.PARTICIPANT);
      when(authService.login(TEST_EMAIL, TEST_PASSWORD)).thenReturn(expectedToken);

      AuthController.LoginRequest request =
          new AuthController.LoginRequest(TEST_EMAIL, TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value(JWT_TOKEN))
          .andExpect(jsonPath("$.role").value("PARTICIPANT"));
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'email est invalide")
    void shouldReturn400WhenEmailInvalid() throws Exception {
      // Given
      AuthController.LoginRequest request =
          new AuthController.LoginRequest("invalid-email", TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'email est vide")
    void shouldReturn400WhenEmailEmpty() throws Exception {
      // Given
      AuthController.LoginRequest request = new AuthController.LoginRequest("", TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si le mot de passe est vide")
    void shouldReturn400WhenPasswordEmpty() throws Exception {
      // Given
      AuthController.LoginRequest request = new AuthController.LoginRequest(TEST_EMAIL, "");

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si le corps de la requête est manquant")
    void shouldReturn400WhenRequestBodyMissing() throws Exception {
      // When / Then
      mockMvc
          .perform(post(BASE_URL + "/login").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /register-participant - Inscription participant")
  class RegisterParticipantTests {

    @Test
    @DisplayName("Devrait retourner 200 et un token JWT lors d'une inscription réussie")
    void shouldReturn200WithTokenWhenRegisterSuccessful() throws Exception {
      // Given
      AuthService.AuthToken expectedToken = new AuthService.AuthToken(JWT_TOKEN, Role.PARTICIPANT);
      when(authService.registerParticipant(TEST_EMAIL, TEST_PASSWORD)).thenReturn(expectedToken);

      AuthController.RegisterParticipantRequest request =
          new AuthController.RegisterParticipantRequest(TEST_EMAIL, TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/register-participant")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value(JWT_TOKEN))
          .andExpect(jsonPath("$.role").value("PARTICIPANT"));
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'email est invalide")
    void shouldReturn400WhenEmailInvalid() throws Exception {
      // Given
      AuthController.RegisterParticipantRequest request =
          new AuthController.RegisterParticipantRequest("not-an-email", TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/register-participant")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si le mot de passe est vide")
    void shouldReturn400WhenPasswordEmpty() throws Exception {
      // Given
      AuthController.RegisterParticipantRequest request =
          new AuthController.RegisterParticipantRequest(TEST_EMAIL, "");

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/register-participant")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /register-organizer - Inscription organisateur")
  class RegisterOrganizerTests {

    private static final String ORG_NAME = "Events Afrique Inc.";
    private static final CountryCode COUNTRY_CODE = CountryCode.TG;
    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.EUR;

    @Test
    @DisplayName("Devrait retourner 200 et un token JWT lors d'une inscription réussie")
    void shouldReturn200WithTokenWhenRegisterSuccessful() throws Exception {
      // Given
      AuthService.AuthToken expectedToken = new AuthService.AuthToken(JWT_TOKEN, Role.ORGANIZER);
      when(authService.registerOrganizer(
              ORG_NAME, COUNTRY_CODE, CURRENCY_CODE, TEST_EMAIL, TEST_PASSWORD))
          .thenReturn(expectedToken);

      AuthController.RegisterOrganizerRequest request =
          new AuthController.RegisterOrganizerRequest(
              ORG_NAME, COUNTRY_CODE, CURRENCY_CODE, TEST_EMAIL, TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/register-organizer")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value(JWT_TOKEN))
          .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    @DisplayName("Devrait retourner 400 si le nom de l'organisation est vide")
    void shouldReturn400WhenOrgNameEmpty() throws Exception {
      // Given
      AuthController.RegisterOrganizerRequest request =
          new AuthController.RegisterOrganizerRequest(
              "", COUNTRY_CODE, CURRENCY_CODE, TEST_EMAIL, TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/register-organizer")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si le code pays est null")
    void shouldReturn400WhenCountryCodeNull() throws Exception {
      // Given
      String requestJson =
          String.format(
              "{\"orgName\":\"%s\",\"countryCode\":null,\"currencyCode\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
              ORG_NAME, CURRENCY_CODE, TEST_EMAIL, TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/register-organizer")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si le code devise est null")
    void shouldReturn400WhenCurrencyCodeNull() throws Exception {
      // Given
      String requestJson =
          String.format(
              "{\"orgName\":\"%s\",\"countryCode\":\"%s\",\"currencyCode\":null,\"email\":\"%s\",\"password\":\"%s\"}",
              ORG_NAME, COUNTRY_CODE, TEST_EMAIL, TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/register-organizer")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 400 si l'email est invalide")
    void shouldReturn400WhenEmailInvalid() throws Exception {
      // Given
      AuthController.RegisterOrganizerRequest request =
          new AuthController.RegisterOrganizerRequest(
              ORG_NAME, COUNTRY_CODE, CURRENCY_CODE, "invalid-email", TEST_PASSWORD);

      // When / Then
      mockMvc
          .perform(
              post(BASE_URL + "/register-organizer")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /me - Profil utilisateur")
  class MeTests {

    @Test
    @DisplayName("Devrait retourner 200 avec les informations du participant authentifié")
    @WithMockUser
    void shouldReturn200WithParticipantInfo() throws Exception {
      // Given
      UUID userId = UUID.randomUUID();
      AuthenticatedUser principal =
          new AuthenticatedUser(userId, TEST_EMAIL, Role.PARTICIPANT, null);
      Authentication auth =
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/me").with(authentication(auth)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value(userId.toString()))
          .andExpect(jsonPath("$.email").value(TEST_EMAIL))
          .andExpect(jsonPath("$.role").value("PARTICIPANT"))
          .andExpect(jsonPath("$.orgId").doesNotExist());
    }

    @Test
    @DisplayName("Devrait retourner 200 avec les informations de l'organisateur authentifié")
    @WithMockUser
    void shouldReturn200WithOrganizerInfo() throws Exception {
      // Given
      UUID userId = UUID.randomUUID();
      UUID orgId = UUID.randomUUID();
      AuthenticatedUser principal =
          new AuthenticatedUser(userId, "organizer@example.com", Role.ORGANIZER, orgId);
      Authentication auth =
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

      // When / Then
      mockMvc
          .perform(get(BASE_URL + "/me").with(authentication(auth)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value(userId.toString()))
          .andExpect(jsonPath("$.email").value("organizer@example.com"))
          .andExpect(jsonPath("$.role").value("ORGANIZER"))
          .andExpect(jsonPath("$.orgId").value(orgId.toString()));
    }

    @Test
    @DisplayName("Devrait retourner 401 si l'utilisateur n'est pas authentifié")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
      // When / Then
      mockMvc.perform(get(BASE_URL + "/me")).andExpect(status().isForbidden());
    }
  }
}
