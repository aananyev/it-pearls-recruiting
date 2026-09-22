#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=db-profile.sh
source "${ROOT}/scripts/db-profile.sh"

usage() {
    cat <<'USAGE'
Usage: validate-db-profile.sh --profile LOCAL|TEST|PRODUCTION

The command prints only non-secret effective connection metadata.
USAGE
}

PROFILE=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --profile)
            PROFILE="${2:-}"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            usage >&2
            exit 2
            ;;
    esac
done

[[ -n "$PROFILE" ]] || { usage >&2; exit 2; }
db_profile_resolve "$PROFILE"

if [[ "$PROFILE" == "PRODUCTION" ]]; then
    [[ "$DB_PROFILE_HOST" == "127.0.0.1" ]] || {
        printf 'Refusing PRODUCTION: effective DB host is not 127.0.0.1\n' >&2
        exit 20
    }

    command -v rg >/dev/null 2>&1 || {
        printf 'Refusing PRODUCTION: ripgrep (rg) is required for artifact scan\n' >&2
        exit 23
    }

    # Production artifacts must never carry the remote work DB or an ambiguous
    # localhost fallback in a JNDI JDBC URL.
    while IFS= read -r config_file; do
        [[ -f "$config_file" ]] || {
            printf 'Refusing PRODUCTION: mandatory config is missing: %s\n' "$config_file" >&2
            exit 24
        }
        if rg -n -i -P 'jdbc:postgresql://(192\.168\.1\.135|localhost|0\.0\.0\.0|\[::1\]|127\.(?!0\.0\.1(?:[:/"[:space:]]|$)))([:/"[:space:]]|$)' "$config_file" >/dev/null; then
            printf 'Refusing PRODUCTION: unsafe JDBC host found in %s\n' "$config_file" >&2
            exit 21
        fi
        if rg -n -P 'password="(?:[^"$]|\$(?!\{))[^\"]*"|^cuba\.dataSource\.password=(?:[^$]|\$(?!\{)).*$' "$config_file" >/dev/null; then
            printf 'Refusing PRODUCTION: hardcoded datasource password found in %s\n' "$config_file" >&2
            exit 22
        fi
    done <<EOF
${ROOT}/modules/core/web/META-INF/context.xml
${ROOT}/modules/core/web/META-INF/jetty-env.xml
${ROOT}/modules/core/web/META-INF/war-context.xml
${ROOT}/modules/core/src/app.properties
${ROOT}/modules/core/src/com/company/hunttech/app.properties
EOF
fi

printf 'profile=%s host=%s port=%s database=%s user=%s\n' \
    "$DB_PROFILE_NAME" "$DB_PROFILE_HOST" "$DB_PROFILE_PORT" \
    "$DB_PROFILE_DATABASE" "$DB_PROFILE_USER"
