#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  SOURCE_DB=... TARGET_DB=... MIGRATION_MODE=local-test MIGRATION_ID=... ./00-preflight.sh

Checks environment safety for local test migration only.
USAGE
}

on_error() {
  echo "ERROR: 00-preflight.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_var() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "ERROR: required variable ${name} is not set" >&2
    usage
    exit 2
  fi
}

for name in PGHOST PGPORT PGUSER SOURCE_DB TARGET_DB EXPECTED_SERVER_VERSION MIGRATION_MODE MIGRATION_ID; do
  require_var "${name}"
done

if [[ "${MIGRATION_MODE}" != "local-test" ]]; then
  echo "ERROR: this script is limited to MIGRATION_MODE=local-test" >&2
  exit 3
fi

HOSTNAME_ACTUAL=$(hostname -f 2>/dev/null || hostname)
if [[ "${HOSTNAME_ACTUAL}" == "hr.hunttech.ru" ]]; then
  echo "ERROR: refusing to run local test migration on production hostname" >&2
  exit 4
fi

PSQL_BIN="${PSQL_BIN:-psql}"
SERVER_VERSION=$("${PSQL_BIN}" -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -Atqc "show server_version")
if [[ "${SERVER_VERSION}" != "${EXPECTED_SERVER_VERSION}"* ]]; then
  echo "ERROR: PostgreSQL server version ${SERVER_VERSION} does not match expected ${EXPECTED_SERVER_VERSION}" >&2
  exit 5
fi

SOURCE_EXISTS=$("${PSQL_BIN}" -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -Atqc "select count(*) from pg_database where datname='${SOURCE_DB}'")
TARGET_EXISTS=$("${PSQL_BIN}" -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -Atqc "select count(*) from pg_database where datname='${TARGET_DB}'")

if [[ "${SOURCE_EXISTS}" != "1" ]]; then
  echo "ERROR: SOURCE_DB does not exist: ${SOURCE_DB}" >&2
  exit 6
fi
if [[ "${TARGET_EXISTS}" != "0" ]]; then
  echo "ERROR: TARGET_DB already exists: ${TARGET_DB}" >&2
  exit 7
fi

echo "preflight_ok=true"
echo "hostname=${HOSTNAME_ACTUAL}"
echo "server_version=${SERVER_VERSION}"
echo "source_db=${SOURCE_DB}"
echo "target_db=${TARGET_DB}"
echo "migration_id=${MIGRATION_ID}"
