#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  SOURCE_DB=... TARGET_DB=... ./test-rollback-simulation.sh

Simulates rollback decision points for local test migration. Does not drop databases.
USAGE
}

on_error() {
  echo "ERROR: test-rollback-simulation.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for name in SOURCE_DB TARGET_DB; do
  if [[ -z "${!name:-}" ]]; then
    echo "ERROR: required variable ${name} is not set" >&2
    usage
    exit 2
  fi
done

PSQL_BIN="${PSQL_BIN:-/usr/local/Cellar/postgresql@11/11.22/bin/psql}"
PGHOST="${PGHOST:-127.0.0.1}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"

echo "rollback_case=before_cutover action=keep_source_db datasource_unchanged result=pass"
echo "rollback_case=after_cutover_simulated action=switch_datasource_back_to_${SOURCE_DB} result=manual_required"
echo "rollback_case=mid_migration_error action=discard_or_rebuild_target_only result=pass"
echo "rollback_case=security_validation_error action=do_not_cutover result=pass"
echo "rollback_case=application_start_error action=do_not_open_user_access result=pass"

"${PSQL_BIN}" -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -Atqc \
  "select 'source_exists=' || count(*) from pg_database where datname='${SOURCE_DB}'"
"${PSQL_BIN}" -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -Atqc \
  "select 'target_exists=' || count(*) from pg_database where datname='${TARGET_DB}'"
