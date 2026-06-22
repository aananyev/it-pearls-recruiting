#!/bin/bash
# Создаёт пользователя и базу для локальной разработки IT-Pearls.
# Запускать от суперпользователя PostgreSQL (обычно postgres или текущий пользователь macOS).
set -euo pipefail

PGHOST="${POSTGRES_HOST:-localhost}"
PGPORT="${POSTGRES_PORT:-5432}"
PGUSER_ADMIN="${POSTGRES_ADMIN_USER:-postgres}"
PGUSER_APP="${POSTGRES_USER:-cuba}"
PGPASSWORD_APP="${POSTGRES_PASSWORD:-cuba}"
PGDATABASE="${POSTGRES_DB:-itpearls}"

PSQL="${PSQL:-psql}"
export PGHOST PGPORT

echo "Проверка подключения к PostgreSQL на ${PGHOST}:${PGPORT}..."
if ! command -v pg_isready >/dev/null 2>&1; then
  if [ -x "/usr/local/opt/postgresql@11/bin/pg_isready" ]; then
  export PATH="/usr/local/opt/postgresql@11/bin:$PATH"
  fi
fi
pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER_ADMIN" || {
  echo "PostgreSQL не запущен. Запустите: ./start-postgres11.sh start"
  echo "Или: docker compose up -d"
  exit 1
}

role_exists=$($PSQL -h "$PGHOST" -p "$PGPORT" -U "$PGUSER_ADMIN" -d postgres -tAc \
  "SELECT 1 FROM pg_roles WHERE rolname='${PGUSER_APP}'" || true)
if [ "$role_exists" != "1" ]; then
  echo "Создание пользователя ${PGUSER_APP}..."
  $PSQL -h "$PGHOST" -p "$PGPORT" -U "$PGUSER_ADMIN" -d postgres -c \
    "CREATE USER ${PGUSER_APP} WITH PASSWORD '${PGPASSWORD_APP}' CREATEDB;"
else
  echo "Пользователь ${PGUSER_APP} уже существует."
fi

db_exists=$($PSQL -h "$PGHOST" -p "$PGPORT" -U "$PGUSER_ADMIN" -d postgres -tAc \
  "SELECT 1 FROM pg_database WHERE datname='${PGDATABASE}'" || true)
if [ "$db_exists" != "1" ]; then
  echo "Создание базы ${PGDATABASE}..."
  $PSQL -h "$PGHOST" -p "$PGPORT" -U "$PGUSER_ADMIN" -d postgres -c \
    "CREATE DATABASE ${PGDATABASE} OWNER ${PGUSER_APP} ENCODING 'UTF8';"
else
  echo "База ${PGDATABASE} уже существует."
fi

echo "Готово. Проверка:"
$PSQL -h "$PGHOST" -p "$PGPORT" -U "$PGUSER_APP" -d "$PGDATABASE" -c "SELECT version();"
