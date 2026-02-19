package com.oneevent.shared.exception;

import java.io.IOException;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre servlet responsable de générer et/ou propager un identifiant de trace (TraceId) pour
 * chaque requête HTTP et de le rendre disponible dans la journalisation via le MDC.
 *
 * <p>But : fournir un identifiant unique par requête afin de faciliter le traçage des logs et le
 * diagnostic distribué (corrélation des événements liés à la même requête).
 *
 * <p>Comportement principal :
 *
 * <ul>
 *   <li>Lit l'en-tête HTTP {@code X-Trace-Id} si le client l'envoie ;
 *   <li>Si l'en-tête est absent ou vide, génère un identifiant unique {@link UUID#randomUUID()};
 *   <li>Place la valeur dans le MDC sous la clé {@code traceId} pour qu'elle soit utilisée
 *       automatiquement par les appenders/loggers configurés (ex : pattern de log) ;
 *   <li>Ajoute l'en-tête {@code X-Trace-Id} à la réponse pour propager l'identifiant côté client ;
 *   <li>nettoie le MDC dans un bloc finally pour éviter les fuites entre requêtes.
 * </ul>
 *
 * <p>Utilisation :
 *
 * <ul>
 *   <li>Ce filtre est annoté {@link Component} pour être enregistré automatiquement dans la chaîne
 *       de filtres Spring Boot ;
 *   <li>Les systèmes clients (API Gateway, clients HTTP) peuvent fournir leur propre {@code
 *       X-Trace-Id} pour corréler des traces multi-niveaux ;
 *   <li>Si vous utilisez un système de traçage distribué (Zipkin, Jaeger), préférez l'intégration
 *       fournie par ces bibliothèques (tracing headers spécialisés).
 * </ul>
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

  /** Nom de l'en-tête HTTP utilisé pour la propagation du trace id. */
  private static final String X_TRACE_ID = "X-Trace-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
      throws ServletException, IOException {

    String traceId = request.getHeader(X_TRACE_ID);
    if (traceId == null || traceId.isBlank()) traceId = UUID.randomUUID().toString();

    MDC.put("traceId", traceId);
    response.setHeader(X_TRACE_ID, traceId);

    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove("traceId");
    }
  }
}
