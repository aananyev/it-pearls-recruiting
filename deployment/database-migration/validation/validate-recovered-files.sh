#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Validate that approved recovered files exist in file storage.

Required env:
  APPROVED_RESTORE_LIST  CSV with columns: id,expected_relative_path
  STORAGE_ROOT
  OUTPUT_DIR
USAGE
}

log() { printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }
die() { log "ERROR: $*"; exit 1; }
trap 'die "failed at line $LINENO"' ERR

[[ "${1:-}" == "--help" ]] && { usage; exit 0; }
: "${APPROVED_RESTORE_LIST:?APPROVED_RESTORE_LIST is required}"
: "${STORAGE_ROOT:?STORAGE_ROOT is required}"
: "${OUTPUT_DIR:?OUTPUT_DIR is required}"

[[ -f "$APPROVED_RESTORE_LIST" ]] || die "approved list not found"
[[ -d "$STORAGE_ROOT" ]] || die "storage root not found"
mkdir -p "$OUTPUT_DIR"

OUT="$OUTPUT_DIR/recovered_files_validation_$(date -u '+%Y%m%d-%H%M%S').csv"
printf 'id,expected_relative_path,exists\n' > "$OUT"
tail -n +2 "$APPROVED_RESTORE_LIST" | while IFS=, read -r id expected_relative_path rest; do
  [[ -f "$STORAGE_ROOT/$expected_relative_path" ]] && exists=true || exists=false
  printf '%s,%s,%s\n' "$id" "$expected_relative_path" "$exists" >> "$OUT"
done

missing="$(awk -F, 'NR>1 && $3!="true"{c++} END{print c+0}' "$OUT")"
log "Validation output: $OUT"
[[ "$missing" -eq 0 ]] || die "$missing approved files are still missing"
