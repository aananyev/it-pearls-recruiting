#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  ./verify-backup.sh /secure/path/db_timestamp.dump

Runs pg_restore --list, checks size and SHA-256. Does not restore data.
USAGE
}

on_error() {
  echo "ERROR: verify-backup.sh failed at line ${BASH_LINENO[0]}" >&2
}
trap on_error ERR

if [[ "${1:-}" == "--help" || $# -ne 1 ]]; then
  usage
  exit 2
fi

DUMP_PATH="$1"
test -f "${DUMP_PATH}"
test -s "${DUMP_PATH}"

DIR=$(dirname "${DUMP_PATH}")
BASE=$(basename "${DUMP_PATH}")
LIST_PATH="${DIR}/pg_restore_list_${BASE%.dump}.txt"
SHA_PATH="${DIR}/SHA256SUMS_${BASE%.dump}.txt"

pg_restore --list "${DUMP_PATH}" > "${LIST_PATH}"
shasum -a 256 "${DUMP_PATH}" > "${SHA_PATH}"

chmod 600 "${LIST_PATH}" "${SHA_PATH}"
echo "dump_path=${DUMP_PATH}"
echo "dump_size_bytes=$(stat -f "%z" "${DUMP_PATH}" 2>/dev/null || stat -c "%s" "${DUMP_PATH}")"
echo "restore_list=${LIST_PATH}"
echo "sha256_file=${SHA_PATH}"
echo "restore_list_objects=$(wc -l < "${LIST_PATH}")"
