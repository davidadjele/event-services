#!/bin/bash

echo "🛑 Arrêt de l'environnement de développement Event Services..."

# Arrêter Docker Compose
docker-compose down

echo "✅ Environnement arrêté!"
echo ""
echo "💡 Pour supprimer les données de la base de données, utilisez:"
echo "   docker-compose down -v"
