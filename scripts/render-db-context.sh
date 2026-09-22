#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=db-profile.sh
source "${ROOT}/scripts/db-profile.sh"

usage() {
    cat <<'USAGE'
Usage: render-db-context.sh --profile LOCAL|TEST|PRODUCTION --output PATH

The output is a local runtime file. It must not be committed.
USAGE
}

PROFILE=""
OUTPUT=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --profile)
            [[ $# -ge 2 ]] || { usage >&2; exit 2; }
            PROFILE="$2"
            shift 2
            ;;
        --output)
            OUTPUT="${2:-}"
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

[[ -n "$PROFILE" && -n "$OUTPUT" ]] || { usage >&2; exit 2; }
db_profile_resolve "$PROFILE"
db_profile_require_password
[[ -n "${HUNTTECH_DB_PASSWORD:-}" ]] || {
    printf 'HUNTTECH_DB_PASSWORD must be supplied for the rendered JNDI context\n' >&2
    exit 1
}

xml_escape() {
    printf '%s' "$1" | sed \
        -e 's/\&/\&amp;/g' \
        -e 's/</\&lt;/g' \
        -e 's/>/\&gt;/g' \
        -e 's/"/\&quot;/g' \
        -e "s/'/\&apos;/g"
}

escaped_user="$(xml_escape "$DB_PROFILE_USER")"
umask 077
mkdir -p "$(dirname "$OUTPUT")"
if [[ -L "$OUTPUT" ]]; then
    printf 'Refusing to overwrite symlink output: %s\n' "$OUTPUT" >&2
    exit 3
fi
temporary_output="$(mktemp "${OUTPUT}.tmp.XXXXXX")"
trap 'rm -f "$temporary_output"' EXIT

{
    printf '%s\n' '<Context>'
    printf '%s\n' '    <!-- Generated outside Git by render-db-context.sh. -->'
    printf '%s\n' '    <Manager pathname=""/>'
    printf '    <Resource driverClassName="org.postgresql.Driver" maxIdle="2" maxTotal="20" maxWaitMillis="5000" name="jdbc/CubaDS" type="javax.sql.DataSource" url="%s" username="%s" password="${env.HUNTTECH_DB_PASSWORD}"/>\n' \
        "$(db_profile_jdbc_url)" "$escaped_user"
    printf '%s\n' '</Context>'
} > "$temporary_output"
mv "$temporary_output" "$OUTPUT"
chmod 600 "$OUTPUT"
trap - EXIT
