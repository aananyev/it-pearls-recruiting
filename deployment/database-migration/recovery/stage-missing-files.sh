#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Stage recovered files from an external archive directory.

Required env:
  RECOVERY_SOURCE_DIR   Directory containing recovered files by relative path
  APPROVED_RESTORE_LIST CSV with id,expected_relative_path
  STAGING_DIR           Empty staging directory

This script copies to staging only. It does not touch production file storage.
USAGE
}

log() { printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }
die() { log "ERROR: $*"; exit 1; }
trap 'die "failed at line $LINENO"' ERR

[[ "${1:-}" == "--help" ]] && { usage; exit 0; }
: "${RECOVERY_SOURCE_DIR:?RECOVERY_SOURCE_DIR is required}"
: "${APPROVED_RESTORE_LIST:?APPROVED_RESTORE_LIST is required}"
: "${STAGING_DIR:?STAGING_DIR is required}"
DRY_RUN="${DRY_RUN:-true}"

[[ -d "$RECOVERY_SOURCE_DIR" ]] || die "source dir not found"
[[ -f "$APPROVED_RESTORE_LIST" ]] || die "approved restore list not found"
mkdir -p "$STAGING_DIR"

tail -n +2 "$APPROVED_RESTORE_LIST" | while IFS=, read -r id expected_relative_path rest; do
  src="$RECOVERY_SOURCE_DIR/$expected_relative_path"
  dst="$STAGING_DIR/$expected_relative_path"
  [[ -f "$src" ]] || die "approved source file missing: $expected_relative_path"
  log "stage $expected_relative_path"
  if [[ "$DRY_RUN" != "true" ]]; then
    mkdir -p "$(dirname "$dst")"
    cp -p "$src" "$dst"
  fi
done

log "staging completed; DRY_RUN=$DRY_RUN"
