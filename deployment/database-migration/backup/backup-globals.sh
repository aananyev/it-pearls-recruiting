#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  PGHOST=... PGPORT=5432 PGUSER=postgres BACKUP_BASE=/secure/path ./backup-globals.sh [--dry-run]

Creates pg_dumpall --globals-only output. Treat the result as sensitive.
USAGE
}

on_error() {
  echo "ERROR: backup-globals.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

DRY_RUN=false
if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
elif [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
fi

for name in PGHOST PGPORT PGUSER BACKUP_BASE; do
  if [[ -z "${!name:-}" ]]; then
    echo "ERROR: required variable ${name} is not set" >&2
    usage
    exit 2
  fi
done

TS=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="${BACKUP_BASE}/${TS}"
GLOBALS_PATH="${BACKUP_DIR}/globals_${TS}.sql"
LOG_PATH="${BACKUP_DIR}/pg_dumpall_globals_${TS}.log"

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "DRY RUN: pg_dumpall --globals-only --no-password --file=${GLOBALS_PATH}"
  exit 0
fi

umask 077
mkdir -p "${BACKUP_DIR}"

pg_dumpall \
  --host="${PGHOST}" \
  --port="${PGPORT}" \
  --username="${PGUSER}" \
  --globals-only \
  --no-password \
  --file="${GLOBALS_PATH}" \
  >"${LOG_PATH}" 2>&1

test -s "${GLOBALS_PATH}"
chmod 600 "${GLOBALS_PATH}" "${LOG_PATH}"
echo "globals_path=${GLOBALS_PATH}"
