package com.oneevent.shared.security;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.oneevent.shared.security.principal.AuthenticatedUser;
import com.oneevent.user.domain.Role;

/**
 * Service utilitaire pour la création et la validation de JSON Web Tokens (JWT).
 *
 * <p>Responsabilités principales :
 *
 * <ul>
 *   <li>Générer des tokens signés (HMAC256) contenant les informations d'identité nécessaires
 *       (subject, email, rôle, organisation) ;
 *   <li>Valider et décoder un token JWT reçu dans une requête pour en extraire l'identité et les
 *       claims métier.
 * </ul>
 *
 * <p>Conception :
 *
 * <ul>
 *   <li>La signature est réalisée avec l'algorithme HMAC256 en utilisant la propriété de
 *       configuration {@code app.security.jwt.secret} (doit rester secrète en production).
 *   <li>Le claim {@link #ORG_ID} est optionnel et n'est ajouté que si une organisation est fournie
 *       lors de la création du token.
 *   <li>Les dates d'émission et d'expiration sont gérées en UTC via {@link Instant}.
 * </ul>
 *
 * <p>Remarques de sécurité : ne jamais committer le secret JWT dans le dépôt. Utilisez des
 * variables d'environnement ou un gestionnaire de secrets en production.
 */
@Service
public class JwtService {

  /** Nom du claim HTTP utilisé pour l'identifiant d'organisation. */
  public static final String ORG_ID = "orgId";

  private final Algorithm algorithm;
  private final String issuer;
  private final long ttlMinutes;

  public JwtService(
      @Value("${app.security.jwt.secret}") String secret,
      @Value("${app.security.jwt.issuer}") String issuer,
      @Value("${app.security.jwt.accessTokenTtlMinutes}") long ttlMinutes) {
    this.algorithm = Algorithm.HMAC256(secret);
    this.issuer = issuer;
    this.ttlMinutes = ttlMinutes;
  }

  /**
   * Crée un JWT signé contenant les informations d'authentification et les claims métier.
   *
   * <p>Claims inclus :
   *
   * <ul>
   *   <li>{@code sub} : l'identifiant utilisateur (UUID)
   *   <li>{@code email} : l'email de l'utilisateur
   *   <li>{@code role} : le rôle de l'utilisateur (ex : ADMIN, USER)
   *   <li>{@link #ORG_ID} : identifiant de l'organisation (optionnel)
   *   <li>{@code iat}, {@code exp} : dates d'émission et d'expiration
   * </ul>
   *
   * @param userId identifiant de l'utilisateur (UUID)
   * @param email adresse email de l'utilisateur
   * @param role rôle de l'utilisateur
   * @param orgId identifiant d'organisation (peut être null)
   * @return le token JWT compact (String) prêt à être retourné au client
   */
  public String createToken(UUID userId, String email, Role role, UUID orgId) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(ttlMinutes * 60);

    var builder =
        JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("role", role.name())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(exp));

    if (orgId != null) builder.withClaim(ORG_ID, orgId.toString());

    return builder.sign(algorithm);
  }

  /**
   * Valide et décode un token JWT reçu depuis le client.
   *
   * <p>Si le token est invalide (signature incorrecte, issuer différent, expiré), la bibliothèque
   * JWT lancera une exception (ex : JWTVerificationException) — ces exceptions doivent être gérées
   * par le consommateur ou par un handler global.
   *
   * @param token le token JWT compact fourni par le client
   * @return un objet {@link AuthenticatedUser} contenant les informations extraites du token
   */
  public AuthenticatedUser decodeAndValidate(String token) {
    var jwt = JWT.require(algorithm).withIssuer(issuer).build().verify(token);

    UUID userId = UUID.fromString(jwt.getSubject());
    String email = jwt.getClaim("email").asString();
    Role role = Role.valueOf(jwt.getClaim("role").asString());
    String orgIdStr = jwt.getClaim(ORG_ID).isNull() ? null : jwt.getClaim(ORG_ID).asString();
    UUID orgId = (orgIdStr == null) ? null : UUID.fromString(orgIdStr);

    return new AuthenticatedUser(userId, email, role, orgId);
  }
}
