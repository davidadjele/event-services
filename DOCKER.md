# Docker Setup for Event Services

## Démarrer PostgreSQL avec Docker

### Prérequis
- Docker et Docker Compose installés sur votre machine

### Commandes

#### Démarrer la base de données
```bash
docker-compose up -d
```

#### Vérifier que le conteneur est en cours d'exécution
```bash
docker-compose ps
```

#### Voir les logs de PostgreSQL
```bash
docker-compose logs -f postgres
```

#### Arrêter la base de données
```bash
docker-compose down
```

#### Arrêter et supprimer les volumes (⚠️ supprime les données)
```bash
docker-compose down -v
```

## Connexion à la base de données

### Informations de connexion
- **Host**: localhost
- **Port**: 5432
- **Database**: eventdb
- **Username**: eventuser
- **Password**: eventpassword

### Connexion avec psql (dans le conteneur)
```bash
docker exec -it event-services-postgres psql -U eventuser -d eventdb
```

### Connexion avec un client externe
Utilisez les informations ci-dessus pour vous connecter avec DBeaver, pgAdmin, ou tout autre client PostgreSQL.

## Exécuter l'application avec le profil local

### Avec Gradle
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Avec Java
```bash
java -jar build/libs/services-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Avec votre IDE
Ajoutez la variable d'environnement ou l'argument VM :
```
-Dspring.profiles.active=local
```

## Structure des fichiers

- `docker-compose.yml` - Configuration Docker pour PostgreSQL
- `src/main/resources/application.yaml` - Configuration par défaut
- `src/main/resources/application-local.yaml` - Configuration locale avec PostgreSQL
