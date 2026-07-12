#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Restore approved staged files into CUBA file storage.

Required env:
  STAGING_DIR
  APPROVED_RESTORE_LIST CSV with id,expected_relative_path
  STORAGE_ROOT
  APPROVAL_TOKEN        Must be RESTORE_APPROVED

Optional env:
  DRY_RUN=true|false    Default true
USAGE
}

log() { printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }
die() { log "ERROR: $*"; exit 1; }
trap 'die "failed at line $LINENO"' ERR

[[ "${1:-}" == "--help" ]] && { usage; exit 0; }
: "${STAGING_DIR:?STAGING_DIR is required}"
: "${APPROVED_RESTORE_LIST:?APPROVED_RESTORE_LIST is required}"
: "${STORAGE_ROOT:?STORAGE_ROOT is required}"
: "${APPROVAL_TOKEN:?APPROVAL_TOKEN is required}"
DRY_RUN="${DRY_RUN:-true}"

[[ "$APPROVAL_TOKEN" == "RESTORE_APPROVED" ]] || die "APPROVAL_TOKEN must be RESTORE_APPROVED"
[[ -d "$STAGING_DIR" ]] || die "staging dir not found"
[[ -d "$STORAGE_ROOT" ]] || die "storage root not found"

tail -n +2 "$APPROVED_RESTORE_LIST" | while IFS=, read -r id expected_relative_path rest; do
  src="$STAGING_DIR/$expected_relative_path"
  dst="$STORAGE_ROOT/$expected_relative_path"
  [[ -f "$src" ]] || die "staged file missing: $expected_relative_path"
  [[ ! -e "$dst" ]] || die "destination already exists: $expected_relative_path"
  log "restore $expected_relative_path"
  if [[ "$DRY_RUN" != "true" ]]; then
    mkdir -p "$(dirname "$dst")"
    cp -p "$src" "$dst"
    chown tomcat:tomcat "$dst"
  fi
done

log "restore completed; DRY_RUN=$DRY_RUN"
