#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

printf '\n==> Criando Sala Java\n'
curl -sS -X POST "$BASE_URL/rooms" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sala Java","capacity":12}'
printf '\n\n==> Criando Sala Arquitetura\n'
curl -sS -X POST "$BASE_URL/rooms" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sala Arquitetura","capacity":20}'
printf '\n\n==> Listando todas as salas\n'
curl -sS "$BASE_URL/rooms"
printf '\n\n==> Filtrando salas com capacidade minima 15\n'
curl -sS "$BASE_URL/rooms?minCapacity=15"
printf '\n'

