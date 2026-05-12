#!/bin/bash

set -e

REGION="us-east-1"
ENDPOINT="http://localhost:4566"

aws configure set cli_follow_urlparam false

echo ">>> Criando parâmetros no SSM Parameter Store..."

put_parameter() {
  local name="$1"
  local value="$2"
  local type="${3:-String}"

  aws ssm put-parameter \
    --endpoint-url "$ENDPOINT" \
    --region "$REGION" \
    --name "/meeting-room/$ENVIRONMENT/$name" \
    --value "$value" \
    --type "$type" \
    --overwrite
}

# Hibernate ORM
put_parameter "quarkus.datasource.username" "ada.tech" "String"
put_parameter "quarkus.datasource.password" "turma1660" "SecureString"
put_parameter "quarkus.datasource.jdbc.url" "jdbc:postgresql://meeting_room_postgres:5432/meeting_room" "String"
put_parameter "quarkus.datasource.jdbc.url" "jdbc:postgresql://meeting_room_postgres:5432/meeting_room" "String"
put_parameter "quarkus.datasource.jdbc.telemetry" "true" "String"

put_parameter "quarkus.hibernate-orm.database.generation" "none" "String"
put_parameter "quarkus.hibernate-orm.log.sql" "false" "String"
put_parameter "quarkus.hibernate-orm.metrics.enabled" "true" "String"

# ---------------------------------------------------------------
# Observabilidade - Logs
# Inclui correlationId e traceId/spanId (MDC) no formato do log
# %X{traceId} e %X{spanId} são injetados automaticamente pelo OpenTelemetry
# ---------------------------------------------------------------
put_parameter "quarkus.log.level" "INFO" "String"
put_parameter "quarkus.log.category.\"br.com.ada.classes.meetingroom\".level" "DEBUG" "String"
put_parameter "quarkus.log.console.format" "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] [%X{correlationId}] [trace=%X{traceId} span=%X{spanId}] (%t) %s%e%n" "String"

# ---------------------------------------------------------------
# Observabilidade - Distributed Tracing (OpenTelemetry)
# Exporta spans via OTLP para Jaeger/Zipkin/Grafana Tempo
# Endpoint padrão: http://localhost:4317 (Jaeger all-in-one ou otel-collector)
# ---------------------------------------------------------------
put_parameter "quarkus.otel.enabled" "true" "String"
put_parameter "quarkus.otel.service.name" "meeting-room" "String"
put_parameter "quarkus.otel.exporter.otlp.traces.endpoint" "http://meeting_room_jaeger:4317" "String"
# Para desabilitar em testes locais sem coletor OTLP, use: quarkus.otel.sdk.disabled=true
put_parameter "quarkus.otel.traces.sampler" "always_on" "String"

# ---------------------------------------------------------------
# Observabilidade - Métricas (Micrometer / Prometheus)
# Endpoint: GET /q/metrics
# ---------------------------------------------------------------
put_parameter "quarkus.micrometer.enabled" "true" "String"
put_parameter "quarkus.micrometer.registry-enabled-default" "true" "String"
put_parameter "quarkus.micrometer.binder.jvm" "true" "String"
put_parameter "quarkus.micrometer.binder.system" "true" "String"
put_parameter "quarkus.micrometer.export.prometheus.enabled" "true" "String"
put_parameter "quarkus.micrometer.export.prometheus.path" "/q/metrics" "String"

# ---------------------------------------------------------------
# Observabilidade - Health
# Liveness:  GET /q/health/live
# Readiness: GET /q/health/ready  (inclui check de banco de dados)
# ---------------------------------------------------------------
put_parameter "quarkus.smallrye-health.ui.enable" "true" "String"

# ---------------------------------------------------------------
# Integração - BrasilAPI (Feriados)
# REST Client: GET https://brasilapi.com.br/api/feriados/v1/{year}
# ---------------------------------------------------------------
put_parameter "quarkus.rest-client.brasil-api.url" "https://brasilapi.com.br" "String"
put_parameter "quarkus.rest-client.brasil-api.connect-timeout" "3000" "String"
put_parameter "quarkus.rest-client.brasil-api.read-timeout" "5000" "String"

# ---------------------------------------------------------------
# Feature Toggles - Validações de Reserva
# Desabilite para ambientes de teste ou datas especiais
# ---------------------------------------------------------------
put_parameter "meeting-room.validation.holiday.enabled" "true" "String"
put_parameter "meeting-room.validation.weekend.enabled" "true" "String"

# Fallback da BrasilAPI:
#   true  = ignora validação de feriado quando a API está fora (fail-open)
#   false = bloqueia a reserva quando a API está fora (fail-closed)
put_parameter "meeting-room.integration.brasil-api.fallback.allow-continue" "true" "String"

# ---------------------------------------------------------------
# Fault Tolerance - Métricas do SmallRye (expostas via Micrometer)
# ---------------------------------------------------------------
put_parameter "smallrye.faulttolerance.metrics.enabled" "true" "String"

# ---------------------------------------------------------------
# Fault Tolerance - Retry da BrasilAPI
# Formato: <NomeSimplesDaClasse>/<método>/Retry/<par�metro>=<valor>
# ---------------------------------------------------------------
put_parameter "HolidayClientDelegate/fetchNationalHolidays/Retry/maxRetries" "3" "String"
put_parameter "HolidayClientDelegate/fetchNationalHolidays/Retry/delay" "100" "String"
put_parameter "HolidayClientDelegate/fetchNationalHolidays/Retry/delayUnit" "MILLIS" "String"
put_parameter "HolidayClientDelegate/fetchNationalHolidays/Retry/jitter" "25" "String"
put_parameter "HolidayClientDelegate/fetchNationalHolidays/Retry/jitterDelayUnit" "MILLIS" "String"
put_parameter "HolidayClientDelegate/fetchNationalHolidays/Retry/maxDuration" "5000" "String"
put_parameter "HolidayClientDelegate/fetchNationalHolidays/Retry/durationUnit" "MILLIS" "String"

# ---------------------------------------------------------------
# Cache - Feriados (Caffeine)
# TTL de 1 dia para evitar chamadas repetidas ? BrasilAPI
# ---------------------------------------------------------------
put_parameter "quarkus.cache.caffeine.national-holidays.expire-after-write" "P1D" "String"

# ---------------------------------------------------------------
# JWT - Configurações de segurança
# ---------------------------------------------------------------
put_parameter "mp.jwt.verify.issuer" "meeting-room-api" "String"
put_parameter "mp.jwt.verify.publickey" "$(cat /usr/share/localstack/keys/meeting-room-public-key.pem)" "String"
put_parameter "smallrye.jwt.sign.key" "$(cat /usr/share/localstack/keys/meeting-room-private-key.pem)" "SecureString"

echo ">>> Parâmetros criados com sucesso:"
aws ssm describe-parameters \
  --endpoint-url "$ENDPOINT" \
  --region "$REGION" \
  --query "Parameters[*].Name" \
  --output table


