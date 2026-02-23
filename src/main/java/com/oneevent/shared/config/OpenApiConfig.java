package com.oneevent.shared.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuration OpenAPI/Swagger pour la documentation interactive de l'API.
 *
 * <p>Cette classe configure Swagger UI pour générer automatiquement la documentation de l'API REST
 * avec :
 *
 * <ul>
 *   <li>Les informations générales de l'API (titre, description, version)
 *   <li>L'authentification JWT via Bearer Token
 *   <li>Les serveurs disponibles (local, staging, production)
 *   <li>Les contacts et la licence
 * </ul>
 *
 * <p><b>Accès à la documentation :</b>
 *
 * <ul>
 *   <li>Swagger UI : <a
 *       href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a>
 *   <li>API Docs JSON : <a
 *       href="http://localhost:8080/v3/api-docs">http://localhost:8080/v1/one-event-api-docs</a>
 * </ul>
 *
 * <p><b>Utilisation de l'authentification dans Swagger UI :</b>
 *
 * <ol>
 *   <li>Cliquez sur le bouton "Authorize" en haut à droite
 *   <li>Entrez votre token JWT au format : <code>Bearer {votre_token}</code>
 *   <li>Cliquez sur "Authorize" pour appliquer le token à toutes les requêtes
 * </ol>
 *
 * <p><b>Annotations à utiliser dans vos contrôleurs :</b>
 *
 * <pre>{@code
 * @Tag(name = "Authentification", description = "API de gestion de l'authentification")
 * @RestController
 * @RequestMapping("/api/v1/auth")
 * public class AuthController {
 *
 *   @Operation(
 *     summary = "Connexion utilisateur",
 *     description = "Authentifie un utilisateur et retourne un token JWT"
 *   )
 *   @ApiResponses(value = {
 *     @ApiResponse(responseCode = "200", description = "Connexion réussie"),
 *     @ApiResponse(responseCode = "401", description = "Identifiants invalides")
 *   })
 *   @PostMapping("/login")
 *   public AuthToken login(@RequestBody LoginRequest request) {
 *     // ...
 *   }
 * }
 * }</pre>
 *
 * @see OpenAPI
 * @see SecurityScheme
 */
@Configuration
public class OpenApiConfig {

  @Value("${spring.application.name:Event Services}")
  private String applicationName;

  @Value("${app.version:0.0.1}")
  private String appVersion;

  /**
   * Configure la documentation OpenAPI 3.0.1 de l'application.
   *
   * <p>Cette méthode définit :
   *
   * <ul>
   *   <li><b>Informations générales :</b> Titre, description, version, contact, licence
   *   <li><b>Sécurité JWT :</b> Schéma d'authentification Bearer Token
   *   <li><b>Serveurs :</b> URLs des environnements (local, staging, production)
   * </ul>
   *
   * <p><b>Configuration de sécurité :</b> Tous les endpoints nécessitent par défaut un token JWT,
   * sauf ceux marqués avec {@code @SecurityRequirement(name = "")}.
   *
   * @return la configuration OpenAPI complète
   */
  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
        .info(
            new Info()
                .title(applicationName + " - API REST")
                .description(
                    """
                    # API de gestion d'événements

                    Cette API permet de gérer une plateforme d'événements avec :

                    - **Authentification** : Inscription et connexion (organisateurs et participants)
                    - **Organisations** : Gestion des organisateurs d'événements
                    - **Utilisateurs** : Gestion des profils utilisateurs
                    - **Événements** : Création, modification, consultation d'événements (à venir)

                    ## Authentification

                    L'API utilise **JWT (JSON Web Tokens)** pour sécuriser les endpoints.

                    ### Obtenir un token :

                    1. **Inscription** : `POST /api/v1/auth/register/organizer` ou `/api/v1/auth/register/participant`
                    2. **Connexion** : `POST /api/v1/auth/login`
                    3. Récupérez le `accessToken` dans la réponse

                    ### Utiliser le token :

                    Ajoutez le header suivant à vos requêtes :
                    ```
                    Authorization: Bearer {votre_token}
                    ```

                    ## Codes d'erreur

                    - `400` : Requête invalide (validation échouée)
                    - `401` : Non authentifié (token manquant ou invalide)
                    - `403` : Non autorisé (permissions insuffisantes)
                    - `404` : Ressource non trouvée
                    - `409` : Conflit (ex: email déjà utilisé)
                    - `500` : Erreur serveur
                    """)
                .version(appVersion)
                .contact(
                    new Contact()
                        .name("Équipe Event Services")
                        .email("support@event-services.com") // À mettre à jour
                        .url("https://event-services.com")) // À mettre à jour
                .license(
                    new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .servers(
            List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Serveur local (développement)"),
                new Server()
                    .url("https://staging-api.event-services.com") // À mettre à jour
                    .description("Serveur de staging"),
                new Server()
                    .url("https://api.event-services.com") // À mettre à jour
                    .description("Serveur de production")))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Entrez votre token JWT obtenu lors de la connexion. Format: Bearer {token}")));
  }
}
