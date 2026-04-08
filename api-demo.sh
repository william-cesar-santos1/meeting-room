#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "==> Criando Salas de Exemplo"
curl -s -X POST "$BASE_URL/rooms" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sala Java","capacity":12}' | jq .

curl -s -X POST "$BASE_URL/rooms" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sala Python","capacity":8}' | jq .

curl -s -X POST "$BASE_URL/rooms" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Auditorio","capacity":30}' | jq .

echo -e "\n==> Listando todas as salas"
curl -s "$BASE_URL/rooms" | jq .

echo -e "\n==> Buscando salas por NOME (query param: name=java)"
curl -s "$BASE_URL/rooms?name=java" | jq .

echo -e "\n==> Filtrando salas por CAPACIDADE MÍNIMA (query param: minCapacity=10)"
curl -s "$BASE_URL/rooms?minCapacity=10" | jq .

echo -e "\n==> Criando Reservas de Exemplo"
curl -s -X POST "$BASE_URL/reservations" \
  -H 'Content-Type: application/json' \
  -d '{
    "roomId":1,
    "guestName":"João Silva",
    "startAt":"2026-04-10T09:00:00",
    "endAt":"2026-04-10T11:00:00"
  }' | jq .

curl -s -X POST "$BASE_URL/reservations" \
  -H 'Content-Type: application/json' \
  -d '{
    "roomId":2,
    "guestName":"Maria Santos",
    "startAt":"2026-04-10T14:00:00",
    "endAt":"2026-04-10T16:00:00"
  }' | jq .

echo -e "\n==> Listando todas as reservas"
curl -s "$BASE_URL/reservations" | jq .

echo -e "\n==> Filtrando reservas por SALA (query param: roomId=1)"
curl -s "$BASE_URL/reservations?roomId=1" | jq .

echo -e "\n==> Buscando reservas por DATA (query param: date=2026-04-10T10:30:00)"
echo "==> Busca retorna reservas que se sobrepõem ao horário fornecido"
curl -s "$BASE_URL/reservations?date=2026-04-10T10:30:00" | jq .

echo -e "\n==> Buscando reservas em outra data (query param: date=2026-04-10T15:00:00)"
curl -s "$BASE_URL/reservations?date=2026-04-10T15:00:00" | jq .

