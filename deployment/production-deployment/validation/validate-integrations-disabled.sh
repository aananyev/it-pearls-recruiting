#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR validate-integrations-disabled exit=${rc}" >&2; exit "${rc}"' ERR

CONFIG_FILE="${1:?Usage: $0 /path/to/local.app.properties}"
echo "$(date '+%Y-%m-%dT%H:%M:%S%z') Validating integrations disabled in ${CONFIG_FILE}"
grep -Eq '^cuba\.automaticDatabaseUpdate=false$' "${CONFIG_FILE}"
grep -Eq '^cuba\.schedulingActive=false$' "${CONFIG_FILE}"
if grep -Eq '^cuba\.email\.smtpHost=.+$' "${CONFIG_FILE}"; then
  echo "SMTP host must be empty before cutover" >&2
  exit 2
fi
if grep -Eq '^hunttech\.telegram\.botToken=.+$' "${CONFIG_FILE}"; then
  echo "Telegram token must be empty before cutover" >&2
  exit 3
fi
echo "$(date '+%Y-%m-%dT%H:%M:%S%z') Integrations-disabled validation completed"
