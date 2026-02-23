# OneEvent Services

Plateforme de gestion des événements en Afrique

## 🚀 Démarrage rapide

### Prérequis
- Java 21
- Docker et Docker Compose
- Gradle (ou utiliser le wrapper `./gradlew`)

### Démarrage automatique (recommandé)

```bash
./start-local.sh
```

Ce script va :
1. Démarrer PostgreSQL dans Docker
2. Attendre que la base de données soit prête
3. Démarrer l'application Spring Boot avec le profil `local`

### Démarrage manuel

1. **Démarrer PostgreSQL**
```bash
docker-compose up -d
```

2. **Démarrer l'application**
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Arrêt de l'environnement

```bash
./stop-local.sh
```

## 📦 Configuration

### Profils disponibles

- **default** : Configuration minimale (sans datasource configurée)
- **local** : PostgreSQL dans Docker (voir `application-local.yaml`)

### Base de données PostgreSQL

**Informations de connexion :**
- Host: `localhost`
- Port: `5432`
- Database: `eventdb`
- Username: `eventuser`
- Password: `eventpass`

**Connexion directe avec psql :**
```bash
docker exec -it event-services-postgres psql -U eventuser -d eventdb
```

## 🛠️ Commandes utiles

### Docker

```bash
# Voir les logs de PostgreSQL
docker-compose logs -f postgres

# Vérifier le statut
docker-compose ps

# Arrêter et supprimer les données
docker-compose down -v

# Reconstruire l'image
docker-compose up -d --build
```

### Gradle

```bash
# Build sans tests
./gradlew clean build -x test

# Exécuter les tests
./gradlew test

# Nettoyer le build
./gradlew clean
```

## 🧹 Formatage du code (Spotless)

Ce projet utilise le plugin Gradle Spotless pour uniformiser le style du code Java.

Commandes utiles :

```bash
# Vérifie que les fichiers respectent le format configuré
./gradlew spotlessCheck

# Applique le formatage automatiquement (modifie les fichiers)
./gradlew spotlessApply
```

## 📁 Structure du projet

```
event-services/
├── docker-compose.yml          # Configuration Docker pour PostgreSQL
├── start-local.sh              # Script de démarrage automatique
├── stop-local.sh               # Script d'arrêt
├── DOCKER.md                   # Documentation Docker détaillée
├── src/
│   └── main/
│       ├── java/
│       │   └── com/event/services/
│       └── resources/
│           ├── application.yaml        # Configuration par défaut
│           └── application-local.yaml  # Configuration PostgreSQL locale
└── build.gradle                # Configuration Gradle
```

## 📖 Documentation

- [Documentation Docker](DOCKER.md) - Guide complet pour Docker et PostgreSQL

## 🔧 Technologies

- Spring Boot 4.0.2
- Java 21
- PostgreSQL 16
- Gradle
- Docker & Docker Compose
