# Backend Architecture — Modulith (Feature-First)

## Projet : Event

---

# 1. Vision

Le backend adopte une architecture **Modulith (Monolithe Modulaire)** organisée par **feature (feature-first)**.

Inspirations :

- Clean Architecture (Robert C. Martin)
- Hexagonal Architecture (Alistair Cockburn)
- Domain-Driven Design (Eric Evans – approche pragmatique)
- Ports & Adapters

## Objectifs

- Scalabilité long terme
- Isolation claire des modules métier
- Extraction future vers microservices possible
- Testabilité élevée
- Réduction du couplage
- Maintenabilité durable

---

# 2. Organisation générale des packages

```
com.ventafri
├── Application.java
├── shared
├── auth
├── events
├── ticketing
├── payments
├── checkin
└── reporting
```

Chaque module métier contient :

```
module-name
├── api
├── application
├── domain
└── infrastructure
```

---

# 3. Description des couches

## 3.1 Couche API

Responsabilités :

- Controllers REST
- DTO
- Validation (@Valid)
- Mapping DTO → Use Case

Contraintes :

- Aucune logique métier
- Aucun accès direct aux repositories
- Aucun accès à l'infrastructure d'autres modules

---

## 3.2 Couche Application

Responsabilités :

- Use Cases
- Orchestration métier
- Gestion des transactions (@Transactional)
- Dépendances vers d’autres modules via Ports (interfaces)

C’est ici que vit la logique d’application.

---

## 3.3 Couche Domain

Responsabilités :

- Entités métier
- Value Objects
- Enums
- Règles métier
- Services métier purs

Contraintes :

- Aucune dépendance Spring
- Aucune dépendance JPA
- Aucune dépendance infrastructure

Le domaine doit rester pur Java.

---

## 3.4 Couche Infrastructure

Responsabilités :

- Repositories JPA
- Implémentations des ports
- Clients externes (Paiement, S3, Redis)
- Adaptateurs techniques

---

# 4. Communication entre modules

## Règle fondamentale

Un module peut communiquer avec un autre uniquement via :

- Ports (interfaces exposées dans application)
- Events internes

### Interdit

- Accéder aux repositories d’un autre module
- Accéder aux entités JPA d’un autre module
- Accéder aux classes infrastructure d’un autre module

---

# 5. Exemple : Payment → Ticketing

## 5.1 Port exposé par Ticketing

```java
package com.ventafri.ticketing.application.ports;

import java.util.UUID;

public interface TicketActivationPort {
    void activateTicketsForOrder(UUID orderId);
}
```

## 5.2 Implémentation côté Ticketing

```java
package com.ventafri.ticketing.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketActivationService implements TicketActivationPort {

    @Transactional
    public void activateTicketsForOrder(UUID orderId) {
        // logique activation tickets
    }
}
```

## 5.3 Utilisation côté Payments

```java
package com.ventafri.payments.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmPaymentUseCase {

    private final TicketActivationPort ticketActivationPort;

    @Transactional
    public void confirmPayment(UUID orderId) {
        // vérifier idempotence
        // changer statut paiement
        ticketActivationPort.activateTicketsForOrder(orderId);
    }
}
```

---

# 6. Dépendances autorisées

```
api → application
application → domain
application → ports
infrastructure → domain
infrastructure → ports
```

Inter-modules :

```
Module A → Module B uniquement via ports
```

Interdit :

```
Module A → Module B.infrastructure
Module A → Module B.repository
Module A → Module B.domain interne
```

---

# 7. Gestion des transactions

Les transactions doivent être placées :

- Dans les Use Cases (application layer)
- Jamais dans domain
- Jamais dans controller

---

# 8. Sécurité

Implémentation centralisée dans :

```
shared/security
```

Contient :

- JWT filter
- RBAC configuration
- Permission evaluator
- SecurityConfig

---

# 9. Gestion des erreurs

Centralisée dans :

```
shared/exception
```

Utilisation :

- @ControllerAdvice
- ErrorResponse standardisé
- Codes métier explicites

---

# 10. Évolution vers microservices

Cette architecture permet :

- Extraction du module payments
- Extraction du module ticketing
- Ajout Kafka
- Passage à une architecture event-driven

Sans refactor massif.

---

# 11. Bonnes pratiques obligatoires

- Pas de logique métier dans controller
- Pas de logique métier dans repository
- DTO jamais exposé dans domain
- Tests unitaires par module
- Testcontainers pour tests d’intégration
- Idempotence obligatoire pour webhooks paiement
- Logging structuré
- Audit trail pour actions sensibles

---

# 12. Références

Clean Architecture — Robert C. Martin  
https://www.oreilly.com/library/view/clean-architecture/9780134494272/

Hexagonal Architecture — Alistair Cockburn  
https://alistair.cockburn.us/hexagonal-architecture/

Domain-Driven Design — Eric Evans  
https://domainlanguage.com/ddd/

Spring Modulith  
https://spring.io/projects/spring-modulith

Spring Boot Reference Documentation  
https://docs.spring.io/spring-boot/docs/current/reference/html/

Baeldung — Hexagonal Architecture with Spring  
https://www.baeldung.com/hexagonal-architecture-ddd-spring
