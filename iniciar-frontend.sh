#!/usr/bin/env bash
# Sobe o front-end React na porta 5173 (a API precisa estar rodando na 8080).
set -e
cd "$(dirname "$0")/frontend"
[ -d node_modules ] || npm install
npm run dev
