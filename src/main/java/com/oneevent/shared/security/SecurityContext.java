package com.oneevent.shared.security;

import static lombok.AccessLevel.PRIVATE;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import com.oneevent.shared.exception.AppException;
import com.oneevent.shared.security.principal.AuthenticatedUser;

import lombok.NoArgsConstructor;

/**
 * Utilitaire d'accès au contexte de sécurité (statique) pour l'application.
 *
 * <p>Cette classe fournit des méthodes pratiques pour :
 *
 * <ul>
 *   <li>Récupérer le principal d'authentification actuellement présent dans le {@link
 *       SecurityContextHolder} ;
 *   <li>Vérifier le rôle de super‑administrateur ;
 *   <li>Récupérer l'identifiant d'organisation associé au principal, en levant une exception
 *       applicative si l'accès n'est pas autorisé.
 * </ul>
 *
 * <p>Conventions :
 *
 * <ul>
 *   <li>Le principal attendu est un objet de type {@link AuthenticatedUser} ; si le principal est
 *       absent ou d'un type différent, une {@link AppException} avec statut {@link
 *       HttpStatus#UNAUTHORIZED} est levée ;
 *   <li>Pour les contrôles d'accès relatifs à l'organisation, la méthode {@link #requireOrgId()}
 *       renvoie UUID de l'organisation ou lève une {@link AppException} avec statut {@link
 *       HttpStatus#FORBIDDEN} si UUID est manquant (sauf pour le rôle SUPER_ADMIN qui a un accès
 *       global).
 * </ul>
 *
 * <p>Exemple d'utilisation :
 *
 * <pre>
 *   // Récupérer le principal et son email
 *   var principal = SecurityContext.principal();
 *   String email = principal.email();
 *
 *   // Obtenir l'orgId requis pour une opération (lance AppException si absent)
 *   UUID orgId = SecurityContext.requireOrgId();
 * </pre>
 *
 * <p>Notes :
 *
 * <ul>
 *   <li>Classe finale, utilitaire statique — ne pas instancier.
 *   <li>Les exceptions levées par cette classe sont conçues pour être interceptées et normalisées
 *       par le gestionnaire d'exceptions global de l'application.
 * </ul>
 */
@NoArgsConstructor(access = PRIVATE)
public final class SecurityContext {
  /**
   * Retourne le principal décodé présent dans le contexte de sécurité.
   *
   * <p>Comportement :
   *
   * <ul>
   *   <li>Si authentication ou le principal est absent, lance une {@link AppException} avec {@link
   *       HttpStatus#UNAUTHORIZED} ;
   *   <li>Si le principal n'est pas de type {@link AuthenticatedUser}, lance également une {@link
   *       AppException} d'authentification requise.
   * </ul>
   *
   * @return le principal sous forme de {@link AuthenticatedUser}
   * @throws AppException avec statut 401 si l'utilisateur n'est pas authentifié
   */
  public static AuthenticatedUser principal() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null
        || auth.getPrincipal() == null
        || !(auth.getPrincipal() instanceof AuthenticatedUser p)) {
      throw AppException.builder(HttpStatus.UNAUTHORIZED)
          .message("Non authentifié")
          .errorCode("AUTH_REQUIRED")
          .logMessage("Missing authentication principal")
          .build();
    }
    return p;
  }

  /**
   * Indique si le principal courant possède le rôle de super administrateur.
   *
   * @return {@code true} si le rôle du principal est exactement {@code SUPER_ADMIN} (comparaison
   *     littérale), sinon {@code false}
   */
  public static boolean isSuperAdmin() {
    return "SUPER_ADMIN".equals(principal().role());
  }

  /**
   * Retourne l'identifiant d'organisation du principal courant.
   *
   * <p>Comportement :
   *
   * <ul>
   *   <li>Si le principal est {@code SUPER_ADMIN}, la méthode retourne {@code null} (signifie accès
   *       global) ;
   *   <li>Si le principal n'a pas d'orgId et n'est pas super admin, on lance une {@link
   *       AppException} avec statut {@link HttpStatus#FORBIDDEN} pour signaler l'accès interdit.
   * </ul>
   *
   * @return UUID de l'organisation associée au principal, ou {@code null} si le principal est super
   *     admin
   * @throws AppException avec statut 403 si l'orgId est requis mais absent
   */
  public static UUID requireOrgId() {
    if (isSuperAdmin()) return null; // super admin peut agir globalement
    UUID orgId = principal().orgId();
    if (orgId == null) {
      throw AppException.builder(HttpStatus.FORBIDDEN)
          .message("Accès interdit")
          .errorCode("ORG_REQUIRED")
          .logMessage("OrgId missing for non-superadmin")
          .build();
    }
    return orgId;
  }
}
