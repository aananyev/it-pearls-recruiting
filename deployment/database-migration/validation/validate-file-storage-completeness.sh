#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Run full file storage completeness check for sys_file.

Required env:
  DB_NAME
  STORAGE_ROOT
  OUTPUT_DIR
USAGE
}

log() { printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }
die() { log "ERROR: $*"; exit 1; }
trap 'die "failed at line $LINENO"' ERR

[[ "${1:-}" == "--help" ]] && { usage; exit 0; }
: "${DB_NAME:?DB_NAME is required}"
: "${STORAGE_ROOT:?STORAGE_ROOT is required}"
: "${OUTPUT_DIR:?OUTPUT_DIR is required}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$SCRIPT_DIR/find-missing-files.sh"

latest_missing="$(find "$OUTPUT_DIR" -type f -name 'sys_file_missing_paths_*.txt' -print | sort | tail -1)"
missing_count="$(wc -l < "$latest_missing" | tr -d ' ')"
log "Latest missing file list: $latest_missing"
[[ "$missing_count" -eq 0 ]] || die "file storage is incomplete: $missing_count files missing"
log "file storage completeness validation passed"
