#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=db-profile.sh
source "${ROOT}/scripts/db-profile.sh"

usage() {
    cat <<'USAGE'
Usage: validate-db-profile.sh --profile LOCAL|TEST|PRODUCTION [--require-password]

The command prints only non-secret effective connection metadata.
USAGE
}

PROFILE=""
REQUIRE_PASSWORD=false
while [[ $# -gt 0 ]]; do
    case "$1" in
        --profile)
            [[ $# -ge 2 ]] || { usage >&2; exit 2; }
            PROFILE="$2"
            shift 2
            ;;
        --require-password)
            REQUIRE_PASSWORD=true
            shift
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
if [[ "$REQUIRE_PASSWORD" == true ]]; then
    if [[ -z "${HUNTTECH_PRODUCTION_DB_PASSWORD:-}" && "${HUNTTECH_DB_PASSWORD_PRESENT:-}" != 1 ]]; then
        db_profile_error 'PRODUCTION password must be supplied outside Git' >&2
        exit 20
    fi
fi

if [[ "$PROFILE" == "PRODUCTION" ]]; then
    [[ "$DB_PROFILE_HOST" == "127.0.0.1" ]] || {
        printf 'Refusing PRODUCTION: effective DB host is not 127.0.0.1\n' >&2
        exit 20
    }
    if [[ -n "${HUNTTECH_DB_JDBC_URL:-}" &&
          ! "$HUNTTECH_DB_JDBC_URL" =~ ^jdbc:postgresql://127\.0\.0\.1:[0-9]+/[A-Za-z0-9._-]+$ ]]; then
        printf 'Refusing PRODUCTION: HUNTTECH_DB_JDBC_URL must target 127.0.0.1\n' >&2
        exit 26
    fi

    command -v rg >/dev/null 2>&1 || {
        printf 'Refusing PRODUCTION: ripgrep (rg) is required for artifact scan\n' >&2
        exit 23
    }
    if ! printf 'pcre2-probe\n' | rg -P 'pcre2-probe' >/dev/null 2>&1; then
        printf 'Refusing PRODUCTION: ripgrep with PCRE2 support is required for artifact scan\n' >&2
        exit 23
    fi

    # Production artifacts must never carry the remote work DB or an ambiguous
    # localhost fallback in a JNDI JDBC URL.
    config_files=(
        "${ROOT}/modules/core/web/META-INF/context.xml"
        "${ROOT}/modules/core/web/META-INF/jetty-env.xml"
        "${ROOT}/modules/core/web/META-INF/war-context.xml"
        "${ROOT}/modules/core/src/app.properties"
        "${ROOT}/modules/core/src/com/company/hunttech/app.properties"
    )
    runtime_config="${ROOT}/deploy/tomcat/webapps/hrm-core/META-INF/context.xml"
    [[ -f "$runtime_config" ]] && config_files+=("$runtime_config")
    for config_file in "${config_files[@]}"; do
        [[ -f "$config_file" ]] || {
            printf 'Refusing PRODUCTION: mandatory config is missing: %s\n' "$config_file" >&2
            exit 24
        }
        scan_status=0
        rg -n -i -P 'jdbc:postgresql://(192\.168\.1\.135|localhost|0\.0\.0\.0|\[::1\]|127\.(?!0\.0\.1(?:[:/"\x09\x20]|$))[0-9.]+)(?:[:/"\x09\x20]|$)' "$config_file" >/dev/null || scan_status=$?
        if ((scan_status >= 2)); then
            printf 'Refusing PRODUCTION: unable to scan %s for unsafe JDBC hosts\n' "$config_file" >&2
            exit 25
        fi
        if ((scan_status == 0)); then
            printf 'Refusing PRODUCTION: unsafe JDBC host found in %s\n' "$config_file" >&2
            exit 21
        fi
        jdbc_status=0
        jdbc_urls="$(rg -o -i -P 'jdbc:postgresql://[^[:space:]"<]+' "$config_file" 2>/dev/null || jdbc_status=$?)"
        if ((jdbc_status >= 2)); then
            printf 'Refusing PRODUCTION: unable to inspect JDBC URLs in %s\n' "$config_file" >&2
            exit 25
        fi
        if ((jdbc_status == 0)) && [[ -n "$jdbc_urls" ]]; then
            while IFS= read -r jdbc_url; do
                [[ "$jdbc_url" =~ ^jdbc:postgresql://127\.0\.0\.1:[0-9]+/[A-Za-z0-9._-]+$ ]] || {
                    printf 'Refusing PRODUCTION: JDBC URL must target 127.0.0.1 in %s\n' "$config_file" >&2
                    exit 21
                }
            done <<< "$jdbc_urls"
        fi
        scan_status=0
        rg -n -P 'password="(?:[^"$]|\$(?!\{))[^\"]*"|^cuba\.dataSource\.password=(?:[^$]|\$(?!\{)).*$|<Set name="password">[^$<][^<]*</Set>' "$config_file" >/dev/null || scan_status=$?
        if ((scan_status >= 2)); then
            printf 'Refusing PRODUCTION: unable to scan %s for hardcoded passwords\n' "$config_file" >&2
            exit 25
        fi
        if ((scan_status == 0)); then
            printf 'Refusing PRODUCTION: hardcoded datasource password found in %s\n' "$config_file" >&2
            exit 22
        fi
    done
fi

printf 'profile=%s host=%s port=%s database=%s user=%s\n' \
    "$DB_PROFILE_NAME" "$DB_PROFILE_HOST" "$DB_PROFILE_PORT" \
    "$DB_PROFILE_DATABASE" "$DB_PROFILE_USER"
