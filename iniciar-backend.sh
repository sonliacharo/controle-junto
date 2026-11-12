#!/usr/bin/env bash
# Sobe a API Spring Boot na porta 8080.
# Se existir um arquivo .env na raiz, carrega as variáveis (Supabase, Gemini, JWT).
set -e
cd "$(dirname "$0")"
if [ -f .env ]; then
  set -a; source .env; set +a
  echo "Variáveis do .env carregadas."
else
  echo "Sem .env: usando banco H2 em memória e classificação local da IA."
fi
cd backend
sh mvnw spring-boot:run
