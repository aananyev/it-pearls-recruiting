#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  PGHOST=127.0.0.1 PGPORT=5432 PGUSER=postgres SOURCE_DB=... TARGET_DB=... TARGET_OWNER=cuba ./10-clone-restored-db.sh

Creates a local target database from restored source DB using CREATE DATABASE ... TEMPLATE.
Local test only. Does not run on production.
USAGE
}

on_error() {
  echo "ERROR: 10-clone-restored-db.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for name in PGHOST PGPORT PGUSER SOURCE_DB TARGET_DB TARGET_OWNER; do
  if [[ -z "${!name:-}" ]]; then
    echo "ERROR: required variable ${name} is not set" >&2
    usage
    exit 2
  fi
done

PSQL_BIN="${PSQL_BIN:-psql}"

HOSTNAME_ACTUAL=$(hostname -f 2>/dev/null || hostname)
if [[ "${HOSTNAME_ACTUAL}" == "hr.hunttech.ru" ]]; then
  echo "ERROR: refusing to clone database on production hostname" >&2
  exit 3
fi

"${PSQL_BIN}" -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -v ON_ERROR_STOP=1 <<SQL
select pg_terminate_backend(pid)
from pg_stat_activity
where datname = '${SOURCE_DB}'
  and pid <> pg_backend_pid()
  and application_name like 'hunttech_migration_test%';

create database ${TARGET_DB}
  with template ${SOURCE_DB}
  owner ${TARGET_OWNER};
SQL

echo "target_db_created=${TARGET_DB}"
