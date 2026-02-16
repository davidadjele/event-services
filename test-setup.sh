#!/bin/bash

echo "🧪 Test de connexion à PostgreSQL..."

# Vérifier que PostgreSQL est prêt
if ! docker exec event-services-postgres pg_isready -U eventuser -d eventdb > /dev/null 2>&1; then
  echo "❌ PostgreSQL n'est pas en cours d'exécution"
  echo "💡 Démarrez PostgreSQL avec: docker-compose up -d"
  exit 1
fi

echo "✅ PostgreSQL est prêt"

# Tester la connexion depuis Java
cd "$(dirname "$0")"
echo "🔧 Compilation du projet..."
./gradlew compileJava > /dev/null 2>&1

if [ $? -eq 0 ]; then
  echo "✅ Compilation réussie"
else
  echo "❌ Erreur de compilation"
  exit 1
fi

echo "🧪 Exécution des tests..."
./gradlew test > /dev/null 2>&1

if [ $? -eq 0 ]; then
  echo "✅ Tous les tests passent"
else
  echo "❌ Des tests ont échoué"
  exit 1
fi

echo ""
echo "✅ Tout fonctionne correctement!"
echo ""
echo "Pour démarrer l'application avec PostgreSQL:"
echo "  ./gradlew bootRun --args='--spring.profiles.active=local'"
echo ""
echo "Ou utilisez le script de démarrage:"
echo "  ./start-local.sh"
