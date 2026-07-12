#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Rollback files restored by restore-approved-files.sh.

Required env:
  APPROVED_RESTORE_LIST CSV with id,expected_relative_path
  STORAGE_ROOT
  ALLOW_REMOVE_RESTORED_FILES yes/no

Optional env:
  DRY_RUN=true|false    Default true
USAGE
}

log() { printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }
die() { log "ERROR: $*"; exit 1; }
trap 'die "failed at line $LINENO"' ERR

[[ "${1:-}" == "--help" ]] && { usage; exit 0; }
: "${APPROVED_RESTORE_LIST:?APPROVED_RESTORE_LIST is required}"
: "${STORAGE_ROOT:?STORAGE_ROOT is required}"
: "${ALLOW_REMOVE_RESTORED_FILES:?ALLOW_REMOVE_RESTORED_FILES is required}"
DRY_RUN="${DRY_RUN:-true}"

[[ "$ALLOW_REMOVE_RESTORED_FILES" == "yes" ]] || die "explicit rollback approval is required"
[[ -f "$APPROVED_RESTORE_LIST" ]] || die "approved list not found"
[[ -d "$STORAGE_ROOT" ]] || die "storage root not found"

tail -n +2 "$APPROVED_RESTORE_LIST" | while IFS=, read -r id expected_relative_path rest; do
  target="$STORAGE_ROOT/$expected_relative_path"
  log "rollback candidate $expected_relative_path"
  if [[ "$DRY_RUN" != "true" && -f "$target" ]]; then
    rm -- "$target"
  fi
done

log "rollback completed; DRY_RUN=$DRY_RUN"
