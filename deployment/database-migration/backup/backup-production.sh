#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  PGHOST=... PGPORT=5432 PGDATABASE=itpearls PGUSER=postgres BACKUP_BASE=/secure/path ./backup-production.sh [--dry-run]

Creates a PostgreSQL custom-format backup. Does not accept passwords as arguments.
Use .pgpass with 0600 permissions or a local socket / trusted operator context.
USAGE
}

on_error() {
  echo "ERROR: backup-production.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

DRY_RUN=false
if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
elif [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
fi

require_var() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "ERROR: required variable ${name} is not set" >&2
    usage
    exit 2
  fi
}

require_var PGHOST
require_var PGPORT
require_var PGDATABASE
require_var PGUSER
require_var BACKUP_BASE

if [[ -f "${HOME}/.pgpass" ]]; then
  mode=$(stat -f "%Lp" "${HOME}/.pgpass" 2>/dev/null || stat -c "%a" "${HOME}/.pgpass")
  if [[ "${mode}" != "600" ]]; then
    echo "ERROR: ${HOME}/.pgpass must have 0600 permissions" >&2
    exit 3
  fi
fi

TS=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="${BACKUP_BASE}/${TS}"
DUMP_PATH="${BACKUP_DIR}/${PGDATABASE}_${TS}.dump"
LOG_PATH="${BACKUP_DIR}/pg_dump_${TS}.log"

echo "timestamp=${TS}"
echo "backup_dir=${BACKUP_DIR}"
echo "host=${PGHOST}"
echo "port=${PGPORT}"
echo "database=${PGDATABASE}"
echo "user=${PGUSER}"

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "DRY RUN: mkdir -p ${BACKUP_DIR}"
  echo "DRY RUN: pg_dump --format=custom --verbose --no-password --file=${DUMP_PATH} <connection>"
  exit 0
fi

umask 077
mkdir -p "${BACKUP_DIR}"

start_epoch=$(date +%s)
pg_dump \
  --host="${PGHOST}" \
  --port="${PGPORT}" \
  --username="${PGUSER}" \
  --dbname="${PGDATABASE}" \
  --format=custom \
  --verbose \
  --no-password \
  --file="${DUMP_PATH}" \
  >"${LOG_PATH}" 2>&1
end_epoch=$(date +%s)

test -s "${DUMP_PATH}"
chmod 600 "${DUMP_PATH}" "${LOG_PATH}"

echo "backup_path=${DUMP_PATH}"
echo "duration_seconds=$((end_epoch - start_epoch))"
