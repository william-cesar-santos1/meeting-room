#!/bin/bash
# ============================================================
# Meeting Room API — Runner do Teste de Carga (Gatling)
# ============================================================
# Uso:
#   ./run-load-test.sh                  # padrão: http://localhost:8080
#   ./run-load-test.sh http://meu-host  # host customizado
#
# Pré-requisitos:
#   - Java instalado (já disponível na máquina)
#   - Aplicação rodando (docker compose up -d + app na porta 8080)
#
# O relatório HTML é gerado em:
#   target/gatling/<simulacao>-<timestamp>/index.html
# ============================================================

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BASE_URL="${1:-http://localhost:8080}"

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════╗"
echo "║       Meeting Room API — Teste de Carga (Gatling)    ║"
echo "║  3 VUs  |  1 req/s → 5 req/s  |  throttle global     ║"
echo "╚══════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ---------------------------------------------------------------
# 1. Verifica se a aplicação está respondendo
# ---------------------------------------------------------------
echo -e "${YELLOW}[1/3] Verificando disponibilidade da aplicação em ${BASE_URL}...${NC}"

MAX_RETRIES=10
RETRY=0
until curl -sf "${BASE_URL}/q/health/live" > /dev/null 2>&1; do
  RETRY=$((RETRY + 1))
  if [ $RETRY -ge $MAX_RETRIES ]; then
    echo -e "${RED}  Erro: aplicação não respondeu após ${MAX_RETRIES} tentativas.${NC}"
    echo -e "${RED}  Verifique se o docker-compose está rodando e a app subiu na porta 8080.${NC}"
    exit 1
  fi
  echo "  Aguardando aplicação... tentativa ${RETRY}/${MAX_RETRIES}"
  sleep 3
done

echo -e "${GREEN}  Aplicação disponível!${NC}"

# ---------------------------------------------------------------
# 2. Exibe informações do teste
# ---------------------------------------------------------------
echo -e "\n${YELLOW}[2/3] Configuração do teste...${NC}"
echo -e "  Target    : ${CYAN}${BASE_URL}${NC}"
echo -e "  VUs       : ${CYAN}3 simultâneos (admin + joao + maria)${NC}"
echo -e "  Ramp-up   : ${CYAN}1 req/s por VU  →  5 req/s por VU${NC}"
echo -e "  Total     : ${CYAN}3 req/s inicial → 15 req/s no pico${NC}"
echo -e "  Duração   : ${CYAN}~7 minutos${NC}"
echo -e "  Relatório : ${CYAN}${PROJECT_ROOT}/target/gatling/*/index.html${NC}"

# ---------------------------------------------------------------
# 3. Executa o teste via Maven
# ---------------------------------------------------------------
echo -e "\n${YELLOW}[3/3] Iniciando teste de carga...${NC}\n"

cd "$PROJECT_ROOT"

./mvnw gatling:test \
  -DbaseUrl="${BASE_URL}" \
  -Dgatling.simulationClass=br.com.ada.classes.meetingroom.simulation.LoadSimulation \
  --no-transfer-progress

EXIT_CODE=$?

# ---------------------------------------------------------------
# Resultado
# ---------------------------------------------------------------
echo ""
REPORT_DIR=$(find "${PROJECT_ROOT}/target/gatling" -name "index.html" -maxdepth 3 2>/dev/null | sort | tail -1)

if [ $EXIT_CODE -eq 0 ]; then
  echo -e "${GREEN}╔══════════════════════════════════════════════════════╗"
  echo -e "║         ✅  Teste concluído com SUCESSO              ║"
  echo -e "╚══════════════════════════════════════════════════════╝${NC}"
else
  echo -e "${RED}╔══════════════════════════════════════════════════════╗"
  echo -e "║     ❌  Teste concluído com FALHAS nos thresholds     ║"
  echo -e "╚══════════════════════════════════════════════════════╝${NC}"
fi

if [ -n "$REPORT_DIR" ]; then
  echo ""
  echo -e "  📊 Relatório HTML: ${CYAN}${REPORT_DIR}${NC}"
  echo ""
fi

exit $EXIT_CODE

