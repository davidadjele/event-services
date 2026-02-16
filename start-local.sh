#!/bin/bash

echo "🚀 Démarrage de l'environnement de développement Event Services..."

# Démarrer Docker Compose
echo "📦 Démarrage de PostgreSQL avec Docker..."
docker-compose up -d

# Attendre que PostgreSQL soit prêt
echo "⏳ Attente de PostgreSQL..."
sleep 5

# Vérifier que PostgreSQL est prêt
MAX_TRIES=30
COUNT=0
until docker exec event-services-postgres pg_isready -U eventuser -d eventdb > /dev/null 2>&1 || [ $COUNT -eq $MAX_TRIES ]; do
  echo "   PostgreSQL n'est pas encore prêt... ($COUNT/$MAX_TRIES)"
  sleep 2
  COUNT=$((COUNT+1))
done

if [ $COUNT -eq $MAX_TRIES ]; then
  echo "❌ PostgreSQL n'a pas démarré correctement"
  exit 1
fi

echo "✅ PostgreSQL est prêt!"
echo ""
echo "📊 Informations de connexion:"
echo "   Host: localhost"
echo "   Port: 5432"
echo "   Database: eventdb"
echo "   Username: eventuser"
echo "   Password: eventpass"
echo ""
echo "🏃 Démarrage de l'application Spring Boot..."
./gradlew bootRun --args='--spring.profiles.active=local'
