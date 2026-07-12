#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  export PGHOST=127.0.0.1 PGPORT=5432 PGUSER=postgres
  export SOURCE_DB=hunttech_prod_restore_test_YYYYMMDD_HHMMSS
  export TARGET_DB=hunttech_migration_target_YYYYMMDD_HHMMSS
  export TARGET_OWNER=cuba EXPECTED_SERVER_VERSION=11.22
  export MIGRATION_MODE=local-test MIGRATION_ID=test-YYYYMMDD-HHMMSS
  ./40-run-test-prefix-migration.sh

Runs local test migration only:
  1. preflight checks;
  2. clone restored source DB;
  3. transform itpearls_* objects to hunttech_* in target copy;
  4. apply idempotent post-migration indexes.
USAGE
}

on_error() {
  echo "ERROR: 40-run-test-prefix-migration.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "${SCRIPT_DIR}/../../.." && pwd)
LOG_DIR="${LOG_DIR:-/private/tmp/hunttech-migration-logs}"
mkdir -p "${LOG_DIR}"
chmod 700 "${LOG_DIR}"

TS=$(date +%Y%m%d-%H%M%S)
RUN_LOG="${LOG_DIR}/test-prefix-migration-${TS}.log"
PSQL_BIN="${PSQL_BIN:-/usr/local/Cellar/postgresql@11/11.22/bin/psql}"

exec > "${RUN_LOG}" 2>&1

echo "timestamp=${TS}"
echo "script=${BASH_SOURCE[0]}"
echo "repo_root=${REPO_ROOT}"
echo "log=${RUN_LOG}"

"${SCRIPT_DIR}/00-preflight.sh"
"${SCRIPT_DIR}/10-clone-restored-db.sh"

"${PSQL_BIN}" -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${TARGET_DB}" \
  -v ON_ERROR_STOP=1 \
  -v migration_id="${MIGRATION_ID}" \
  -f "${SCRIPT_DIR}/20-transform-restored-copy-to-hunttech.sql"

(
  cd "${REPO_ROOT}"
  "${PSQL_BIN}" -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${TARGET_DB}" \
    -v ON_ERROR_STOP=1 \
    -f "${SCRIPT_DIR}/30-apply-post-migration-indexes.sql"
)

echo "test_prefix_migration_complete=true"
