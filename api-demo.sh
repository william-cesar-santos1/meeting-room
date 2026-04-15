#!/usr/bin/env bash
# =============================================================================
# api-demo.sh — Aula 3: Segurança JWT
# =============================================================================
# Fluxo demonstrado:
#   1. Login como ADMIN  → obtém TOKEN_ADMIN
#   2. Login como USER   → obtém TOKEN_USER
#   3. ADMIN cria sala   → 201 Created
#   4. USER tenta criar sala → 403 Forbidden  (exercício!)
#   5. USER cria reserva → 201 Created
#   6. USER tenta cancelar reserva de outro usuário → 403 Forbidden  (exercício!)
#   7. ADMIN cancela qualquer reserva → 204 No Content
#   8. GET sem token → 200 OK (endpoints públicos)
# =============================================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

separator() { echo -e "\n\033[1;34m──────────────────────────────────────\033[0m"; }

# =============================================================================
# 1. LOGIN — obter tokens JWT
# =============================================================================
separator
echo "==> [1] Login como ADMIN"
RESPONSE_ADMIN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')
echo "$RESPONSE_ADMIN" | jq .
TOKEN_ADMIN=$(echo "$RESPONSE_ADMIN" | jq -r '.token')

separator
echo "==> [2] Login como USER (joao)"
RESPONSE_USER=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"joao","password":"user123"}')
echo "$RESPONSE_USER" | jq .
TOKEN_USER=$(echo "$RESPONSE_USER" | jq -r '.token')

separator
echo "==> [3] Login como USER (maria)"
RESPONSE_MARIA=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"maria","password":"user123"}')
echo "$RESPONSE_MARIA" | jq .
TOKEN_MARIA=$(echo "$RESPONSE_MARIA" | jq -r '.token')

# =============================================================================
# 2. GERENCIAMENTO DE SALAS — apenas ADMIN
# =============================================================================
separator
echo "==> [4] ADMIN cria uma sala (deve retornar 201)"
curl -s -X POST "$BASE_URL/rooms" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"name":"Sala JWT","capacity":10}' | jq .

separator
echo "==> [5] USER tenta criar sala — deve retornar 403 Forbidden"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/rooms" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{"name":"Sala Proibida","capacity":5}'

separator
echo "==> [6] Sem token tenta criar sala — deve retornar 401 Unauthorized"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/rooms" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sala SemToken","capacity":5}'

separator
echo "==> [7] GET de salas sem token — deve retornar 200 (endpoint público)"
curl -s "$BASE_URL/rooms?size=3" | jq '{total: .total, items: [.items[] | {id, name}]}'

# =============================================================================
# 3. RESERVAS — USER cria, verifica propriedade no cancelamento
# =============================================================================
separator
echo "==> [8] JOAO cria uma reserva (deve retornar 201)"
RESERVA_JOAO=$(curl -s -X POST "$BASE_URL/reservations" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{
    "roomId": 1,
    "guestName": "Joao Silva",
    "startAt": "2026-05-10T09:00:00",
    "endAt":   "2026-05-10T11:00:00"
  }')
echo "$RESERVA_JOAO" | jq .
RESERVA_JOAO_ID=$(echo "$RESERVA_JOAO" | jq -r '.id')

separator
echo "==> [9] MARIA cria uma reserva (deve retornar 201)"
RESERVA_MARIA=$(curl -s -X POST "$BASE_URL/reservations" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_MARIA" \
  -d '{
    "roomId": 2,
    "guestName": "Maria Santos",
    "startAt": "2026-05-11T14:00:00",
    "endAt":   "2026-05-11T16:00:00"
  }')
echo "$RESERVA_MARIA" | jq .
RESERVA_MARIA_ID=$(echo "$RESERVA_MARIA" | jq -r '.id')

separator
echo "==> [10] JOAO tenta cancelar a reserva de MARIA — deve retornar 403 Forbidden"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X DELETE "$BASE_URL/reservations/$RESERVA_MARIA_ID" \
  -H "Authorization: Bearer $TOKEN_USER"

separator
echo "==> [11] JOAO cancela a PROPRIA reserva — deve retornar 204 No Content"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X DELETE "$BASE_URL/reservations/$RESERVA_JOAO_ID" \
  -H "Authorization: Bearer $TOKEN_USER"

separator
echo "==> [12] ADMIN cancela a reserva de MARIA (qualquer reserva) — deve retornar 204"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X DELETE "$BASE_URL/reservations/$RESERVA_MARIA_ID" \
  -H "Authorization: Bearer $TOKEN_ADMIN"

separator
echo "==> [13] ADMIN deleta a sala criada — deve retornar 204"
SALA_JWT_ID=$(curl -s "$BASE_URL/rooms?name=Sala+JWT" | jq -r '.items[0].id')
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X DELETE "$BASE_URL/rooms/$SALA_JWT_ID" \
  -H "Authorization: Bearer $TOKEN_ADMIN"

separator
echo -e "\n\033[1;32mDemo concluída!\033[0m"
