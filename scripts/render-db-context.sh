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
            PROFILE="${2:-}"
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

password="${!DB_PROFILE_PASSWORD_VAR}"
xml_escape() {
    printf '%s' "$1" | sed \
        -e 's/\&/\&amp;/g' \
        -e 's/</\&lt;/g' \
        -e 's/>/\&gt;/g' \
        -e 's/"/\&quot;/g' \
        -e "s/'/\&apos;/g"
}

escaped_password="$(xml_escape "$password")"
mkdir -p "$(dirname "$OUTPUT")"
umask 077
temporary_output="${OUTPUT}.tmp.$$"
trap 'rm -f "$temporary_output"' EXIT

{
    printf '%s\n' '<Context>'
    printf '%s\n' '    <!-- Generated outside Git by render-db-context.sh. -->'
    printf '%s\n' '    <Manager pathname=""/>'
    printf '    <Resource driverClassName="org.postgresql.Driver" maxIdle="2" maxTotal="20" maxWaitMillis="5000" name="jdbc/CubaDS" type="javax.sql.DataSource" url="%s" username="%s" password="%s"/>\n' \
        "$(db_profile_jdbc_url)" "$DB_PROFILE_USER" "$escaped_password"
    printf '%s\n' '</Context>'
} > "$temporary_output"
mv "$temporary_output" "$OUTPUT"
chmod 600 "$OUTPUT"
trap - EXIT
