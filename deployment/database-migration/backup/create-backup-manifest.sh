#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  PGDATABASE=itpearls ./create-backup-manifest.sh /secure/path/db.dump [/secure/path/globals.sql]
USAGE
}

on_error() {
  echo "ERROR: create-backup-manifest.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

if [[ $# -lt 1 || $# -gt 2 || "${1:-}" == "--help" ]]; then
  usage
  exit 2
fi

DUMP_PATH="$1"
GLOBALS_PATH="${2:-}"
test -f "${DUMP_PATH}"
test -s "${DUMP_PATH}"

DIR=$(dirname "${DUMP_PATH}")
TS=$(date +%Y%m%d-%H%M%S)
MANIFEST="${DIR}/backup_manifest_${TS}.txt"

{
  echo "timestamp=${TS}"
  echo "hostname=$(hostname)"
  echo "database=${PGDATABASE:-unknown}"
  echo "pg_dump_version=$(pg_dump --version)"
  echo "pg_restore_version=$(pg_restore --version)"
  echo "format=custom"
  echo "backup_path=${DUMP_PATH}"
  echo "backup_size_bytes=$(stat -f "%z" "${DUMP_PATH}" 2>/dev/null || stat -c "%s" "${DUMP_PATH}")"
  echo "backup_permissions=$(stat -f "%Sp %Su:%Sg" "${DUMP_PATH}" 2>/dev/null || stat -c "%A %U:%G" "${DUMP_PATH}")"
  echo "sha256_dump=$(shasum -a 256 "${DUMP_PATH}" | awk '{print $1}')"
  if [[ -n "${GLOBALS_PATH}" ]]; then
    test -f "${GLOBALS_PATH}"
    echo "globals_path=${GLOBALS_PATH}"
    echo "globals_size_bytes=$(stat -f "%z" "${GLOBALS_PATH}" 2>/dev/null || stat -c "%s" "${GLOBALS_PATH}")"
    echo "globals_permissions=$(stat -f "%Sp %Su:%Sg" "${GLOBALS_PATH}" 2>/dev/null || stat -c "%A %U:%G" "${GLOBALS_PATH}")"
    echo "sha256_globals=$(shasum -a 256 "${GLOBALS_PATH}" | awk '{print $1}')"
  fi
  echo "command_pg_dump=pg_dump --format=custom --verbose --no-password --file=<BACKUP_PATH> <CONNECTION_PARAMETERS>"
} > "${MANIFEST}"

chmod 600 "${MANIFEST}"
echo "manifest=${MANIFEST}"
