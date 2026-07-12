#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Validate restored file checksums from a simple manifest.

Manifest format:
  <sha256>  <absolute-file-path>

Required env:
  CHECKSUM_MANIFEST
USAGE
}

log() { printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }
die() { log "ERROR: $*"; exit 1; }
trap 'die "failed at line $LINENO"' ERR

[[ "${1:-}" == "--help" ]] && { usage; exit 0; }
: "${CHECKSUM_MANIFEST:?CHECKSUM_MANIFEST is required}"
[[ -f "$CHECKSUM_MANIFEST" ]] || die "manifest not found: $CHECKSUM_MANIFEST"

sha256sum --check "$CHECKSUM_MANIFEST"
log "Checksum validation completed"
