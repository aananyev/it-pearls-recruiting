#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR validate-deployment exit=${rc}" >&2; exit "${rc}"' ERR

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "$(date '+%Y-%m-%dT%H:%M:%S%z') Validating production-deployment artifact set"
test -f "${ROOT}/config/deployment-manifest.yaml"
test -f "${ROOT}/runbooks/production-deployment-runbook.md"
test -f "${ROOT}/runbooks/production-deployment-rollback.md"
test -f "${ROOT}/runbooks/tomcat-context-path-migration.md"
find "${ROOT}/scripts" -name '*.sh' -print0 | xargs -0 -n1 bash -n
find "${ROOT}/validation" -name '*.sh' -print0 | xargs -0 -n1 bash -n
echo "$(date '+%Y-%m-%dT%H:%M:%S%z') Deployment artifact set validation completed"
