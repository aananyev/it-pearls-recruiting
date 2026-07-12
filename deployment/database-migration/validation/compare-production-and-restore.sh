#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  PROD_DB=itpearls RESTORE_DB=hunttech_prod_restore_test_YYYYMMDD_HHMMSS ./compare-production-and-restore.sh

Compares metadata and aggregate counts only. Does not expose row contents.
USAGE
}

on_error() {
  echo "ERROR: compare-production-and-restore.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for name in PROD_DB RESTORE_DB; do
  if [[ -z "${!name:-}" ]]; then
    echo "ERROR: required variable ${name} is not set" >&2
    usage
    exit 2
  fi
done

SQL="select 'tables' as metric, count(*) from information_schema.tables where table_schema='public' and table_type='BASE TABLE'
union all select 'sequences', count(*) from information_schema.sequences where sequence_schema='public'
union all select 'constraints', count(*) from pg_constraint where connamespace='public'::regnamespace
union all select 'indexes', count(*) from pg_indexes where schemaname='public'
union all select 'sec_user', count(*) from sec_user
union all select 'sec_role', count(*) from sec_role
union all select 'sec_user_role', count(*) from sec_user_role
union all select 'sys_file', count(*) from sys_file
union all select 'sys_db_changelog', count(*) from sys_db_changelog
order by metric;"

echo "production=${PROD_DB}"
psql -d "${PROD_DB}" -Atc "${SQL}"
echo "restore=${RESTORE_DB}"
psql -d "${RESTORE_DB}" -Atc "${SQL}"
