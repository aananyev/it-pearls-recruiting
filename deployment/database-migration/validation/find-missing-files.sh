#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Find sys_file records whose physical file is missing.

Required env:
  DB_NAME        PostgreSQL database name
  STORAGE_ROOT   CUBA file storage root
  OUTPUT_DIR     Directory for generated reports

Optional env:
  PGHOST, PGPORT, PGUSER, PGDATABASE, PGPASSFILE

Example:
  DB_NAME=hunttech STORAGE_ROOT=/opt/app_home/fileStorage OUTPUT_DIR=/secure/out ./find-missing-files.sh
USAGE
}

log() { printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }
die() { log "ERROR: $*"; exit 1; }
trap 'die "failed at line $LINENO"' ERR

[[ "${1:-}" == "--help" ]] && { usage; exit 0; }
: "${DB_NAME:?DB_NAME is required}"
: "${STORAGE_ROOT:?STORAGE_ROOT is required}"
: "${OUTPUT_DIR:?OUTPUT_DIR is required}"

[[ -d "$STORAGE_ROOT" ]] || die "STORAGE_ROOT does not exist: $STORAGE_ROOT"
mkdir -p "$OUTPUT_DIR"
chmod 700 "$OUTPUT_DIR"

TS="$(date -u '+%Y%m%d-%H%M%S')"
EXPECTED="$OUTPUT_DIR/sys_file_expected_paths_$TS.tsv"
MISSING="$OUTPUT_DIR/sys_file_missing_paths_$TS.txt"

log "Exporting sys_file expected paths from read-only session"
psql -q -X -v ON_ERROR_STOP=1 -d "$DB_NAME" -At -F $'\t' <<'SQL' > "$EXPECTED"
SET default_transaction_read_only = on;
SET statement_timeout = '5min';
SET lock_timeout = '5s';
SET idle_in_transaction_session_timeout = '5min';
BEGIN READ ONLY;
SELECT
  id,
  to_char(create_date, 'YYYY/MM/DD') || '/' || id::text || '.' || COALESCE(ext, 'null') AS relative_path
FROM sys_file
ORDER BY create_date, id;
ROLLBACK;
SQL

log "Checking physical files without modifying storage"
: > "$MISSING"
while IFS=$'\t' read -r id rel_path; do
  [[ -n "$id" && -n "$rel_path" ]] || continue
  [[ -f "$STORAGE_ROOT/$rel_path" ]] || printf '%s\n' "$rel_path" >> "$MISSING"
done < "$EXPECTED"

log "Expected paths: $(wc -l < "$EXPECTED" | tr -d ' ')"
log "Missing paths: $(wc -l < "$MISSING" | tr -d ' ')"
log "Output: $MISSING"
