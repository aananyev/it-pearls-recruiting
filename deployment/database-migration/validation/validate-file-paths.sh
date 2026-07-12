#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Validate paths from the anonymized missing-files register.

Required env:
  REGISTER_CSV   Path to reports/missing-files-register.csv
  STORAGE_ROOT   CUBA file storage root
  OUTPUT_DIR     Directory for validation output
USAGE
}

log() { printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }
die() { log "ERROR: $*"; exit 1; }
trap 'die "failed at line $LINENO"' ERR

[[ "${1:-}" == "--help" ]] && { usage; exit 0; }
: "${REGISTER_CSV:?REGISTER_CSV is required}"
: "${STORAGE_ROOT:?STORAGE_ROOT is required}"
: "${OUTPUT_DIR:?OUTPUT_DIR is required}"

[[ -f "$REGISTER_CSV" ]] || die "REGISTER_CSV not found: $REGISTER_CSV"
[[ -d "$STORAGE_ROOT" ]] || die "STORAGE_ROOT not found: $STORAGE_ROOT"
mkdir -p "$OUTPUT_DIR"

OUT="$OUTPUT_DIR/file_path_validation_$(date -u '+%Y%m%d-%H%M%S').csv"
printf 'id,expected_relative_path,exists\n' > "$OUT"

tail -n +2 "$REGISTER_CSV" | while IFS=, read -r id expected_relative_path rest; do
  [[ -n "$id" && -n "$expected_relative_path" ]] || continue
  if [[ -f "$STORAGE_ROOT/$expected_relative_path" ]]; then
    printf '%s,%s,true\n' "$id" "$expected_relative_path" >> "$OUT"
  else
    printf '%s,%s,false\n' "$id" "$expected_relative_path" >> "$OUT"
  fi
done

log "Validation output: $OUT"
log "Missing after validation: $(awk -F, 'NR>1 && $3=="false"{c++} END{print c+0}' "$OUT")"
