#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MANIFEST="${DEPLOYMENT_ROOT}/config/deployment-manifest.yaml"
LOG_DIR="${DEPLOYMENT_ROOT}/reports"
DRY_RUN=1

trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR ${BASH_SOURCE[0]}:${LINENO} exit=${rc}" >&2; exit "${rc}"' ERR

usage_common() {
  cat <<'USAGE'
Common options:
  --dry-run       Show what would be done. Default.
  --execute       Execute guarded action. Requires HRM_DEPLOY_APPROVED=yes.
  --help          Show help.
USAGE
}

parse_common_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --dry-run) DRY_RUN=1 ;;
      --execute) DRY_RUN=0 ;;
      --help|-h) usage; exit 0 ;;
      *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
    esac
    shift
  done
}

log() {
  printf '%s %s\n' "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$*"
}

require_file() {
  [[ -f "$1" ]] || { echo "Required file not found: $1" >&2; exit 10; }
}

require_manifest() {
  require_file "${MANIFEST}"
}

require_approved_execute() {
  if [[ "${DRY_RUN}" -eq 0 && "${HRM_DEPLOY_APPROVED:-no}" != "yes" ]]; then
    echo "Refusing execute: set HRM_DEPLOY_APPROVED=yes after human approval." >&2
    exit 20
  fi
}

run_cmd() {
  log "+ $*"
  if [[ "${DRY_RUN}" -eq 0 ]]; then
    "$@"
  fi
}

require_exact_host_if_remote() {
  local expected="${EXPECTED_HOST:-hr.hunttech.ru}"
  local actual
  actual="$(hostname -f 2>/dev/null || hostname)"
  if [[ "${RUNNING_ON_PRODUCTION:-no}" == "yes" && "${actual}" != "${expected}" ]]; then
    echo "Refusing: expected host ${expected}, actual ${actual}" >&2
    exit 30
  fi
}

refuse_wildcard_path() {
  local path="${1:-}"
  [[ -n "${path}" ]] || { echo "Empty path is forbidden" >&2; exit 40; }
  [[ "${path}" != "/" ]] || { echo "Root path is forbidden" >&2; exit 41; }
  [[ "${path}" != *"*"* ]] || { echo "Wildcard path is forbidden: ${path}" >&2; exit 42; }
}

safe_ssh() {
  local host="${1:?host required}"
  shift
  ssh -o BatchMode=yes -o ConnectTimeout=10 "${host}" "$@"
}
