#!/bin/bash
# Надёжный запуск CUBA/Tomcat для it-pearls-recruiting (без голого gradlew restart).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APP_URL="${APP_URL:-http://localhost:8080/app/}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-300}"
POLL_INTERVAL="${POLL_INTERVAL:-5}"
PROJECT_MARKER="it-pearls-recruiting/deploy/tomcat"

log() { printf '%s\n' "$*"; }

is_project_java() {
  local pid="$1"
  ps -p "$pid" -o command= 2>/dev/null | grep -q "$PROJECT_MARKER"
}

kill_stale_project_tomcat() {
  local port pid pids
  for port in 8080 8787; do
    pids="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null || true)"
    for pid in $pids; do
      if is_project_java "$pid"; then
        log "Останавливаю зависший Tomcat (PID $pid, порт $port)..."
        kill "$pid" 2>/dev/null || true
      fi
    done
  done

  local orphans
  orphans="$(pgrep -f "$PROJECT_MARKER" 2>/dev/null || true)"
  for pid in $orphans; do
    if is_project_java "$pid"; then
      log "Останавливаю процесс Tomcat проекта (PID $pid)..."
      kill "$pid" 2>/dev/null || true
    fi
  done

  sleep 2
  for port in 8080 8787; do
    pids="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null || true)"
    for pid in $pids; do
      if is_project_java "$pid"; then
        log "Принудительно: kill -9 PID $pid (порт $port)"
        kill -9 "$pid" 2>/dev/null || true
      fi
    done
  done
}

ensure_postgres() {
  if command -v pg_isready >/dev/null 2>&1 && pg_isready -q 2>/dev/null; then
    log "PostgreSQL: готов (pg_isready)."
    return 0
  fi
  log "PostgreSQL не отвечает — запуск ./start-postgres11.sh start ..."
  ./start-postgres11.sh start
  for _ in $(seq 1 30); do
    if pg_isready -q 2>/dev/null; then
      log "PostgreSQL: готов."
      return 0
    fi
    sleep 1
  done
  log "Ошибка: PostgreSQL не поднялся за 30 с. Проверьте ./start-postgres11.sh status"
  exit 1
}

wait_for_http() {
  local elapsed=0 code
  log "Ожидание ответа $APP_URL (таймаут ${WAIT_TIMEOUT} с, warmup CUBA 1–5 мин)..."
  while [ "$elapsed" -lt "$WAIT_TIMEOUT" ]; do
    code="$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 "$APP_URL" 2>/dev/null || echo '000')"
    if [ "$code" = "200" ]; then
      log "Приложение доступно: HTTP 200 — $APP_URL"
      return 0
    fi
    sleep "$POLL_INTERVAL"
    elapsed=$((elapsed + POLL_INTERVAL))
    log "  ... ещё не готово (HTTP $code), прошло ${elapsed} с"
  done
  log "Таймаут: приложение не ответило HTTP 200 за ${WAIT_TIMEOUT} с."
  log "Проверьте логи: deploy/tomcat/logs/catalina.out"
  log "Порты: nc -z localhost 8080; JDWP: nc -z localhost 8787"
  exit 1
}

ensure_postgres
kill_stale_project_tomcat

log "Gradle stop (ошибки игнорируются)..."
./gradlew stop >/dev/null 2>&1 || true

log "Gradle deploy start -x test ..."
./gradlew deploy start -x test

log "URL: $APP_URL"
wait_for_http
