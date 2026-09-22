#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=db-profile.sh
source "${ROOT}/scripts/db-profile.sh"

failures=0
pass() { printf 'PASS %s\n' "$1"; }
fail() { printf 'FAIL %s\n' "$1" >&2; failures=$((failures + 1)); }

if command -v rg >/dev/null 2>&1; then
    pass 'ripgrep is available for security scans'
else
    fail 'ripgrep is required for security scans'
fi

if db_profile_resolve LOCAL && [[ "$DB_PROFILE_HOST" == "192.168.1.135" ]]; then
    pass 'LOCAL resolves to 192.168.1.135'
else
    fail 'LOCAL must resolve to 192.168.1.135'
fi

if HUNTTECH_DB_PROFILE=LOCAL db_profile_resolve && [[ "$DB_PROFILE_NAME" == "LOCAL" ]]; then
    pass 'environment profile fallback resolves explicitly'
else
    fail 'environment profile fallback did not resolve'
fi

if db_profile_resolve PRODUCTION && [[ "$DB_PROFILE_HOST" == "127.0.0.1" ]]; then
    pass 'PRODUCTION resolves to 127.0.0.1'
else
    fail 'PRODUCTION must resolve to 127.0.0.1'
fi

if HUNTTECH_PRODUCTION_DB_HOST=192.168.1.135 db_profile_resolve PRODUCTION >/dev/null 2>&1; then
    fail 'PRODUCTION accepted 192.168.1.135'
else
    pass 'PRODUCTION rejects 192.168.1.135'
fi

if HUNTTECH_PRODUCTION_DB_HOST=localhost db_profile_resolve PRODUCTION >/dev/null 2>&1; then
    fail 'PRODUCTION accepted localhost'
else
    pass 'PRODUCTION rejects localhost'
fi

if db_profile_resolve UNKNOWN >/dev/null 2>&1; then
    fail 'unknown profile was accepted'
else
    pass 'unknown profile is rejected'
fi

if HUNTTECH_TEST_DB_HOST=10.20.30.40 \
   HUNTTECH_TEST_DB_PORT=5544 \
   HUNTTECH_TEST_DB_NAME=hrm_test \
   HUNTTECH_TEST_DB_USER=hrm_test_user \
   db_profile_resolve TEST &&
   [[ "$DB_PROFILE_HOST" == "10.20.30.40" &&
      "$DB_PROFILE_PORT" == "5544" &&
      "$DB_PROFILE_DATABASE" == "hrm_test" &&
      "$DB_PROFILE_USER" == "hrm_test_user" ]]; then
    pass 'TEST uses explicit independent settings'
else
    fail 'TEST did not use explicit independent settings'
fi

if HUNTTECH_TEST_DB_HOST=192.168.1.135 \
   HUNTTECH_TEST_DB_PORT=5432 \
   HUNTTECH_TEST_DB_NAME=hrm_test \
   HUNTTECH_TEST_DB_USER=hrm_test_user \
   db_profile_resolve TEST >/dev/null 2>&1; then
    fail 'TEST inherited the LOCAL host'
else
    pass 'TEST rejects the LOCAL host'
fi

if HUNTTECH_TEST_DB_HOST=::1 \
   HUNTTECH_TEST_DB_PORT=5432 \
   HUNTTECH_TEST_DB_NAME=hrm_test \
   HUNTTECH_TEST_DB_USER=hrm_test_user \
   db_profile_resolve TEST >/dev/null 2>&1; then
    fail 'TEST accepted IPv6 loopback'
else
    pass 'TEST rejects IPv6 loopback'
fi

if HUNTTECH_LOCAL_DB_PORT=00000 db_profile_resolve LOCAL >/dev/null 2>&1; then
    fail 'out-of-range zero port was accepted'
else
    pass 'out-of-range zero port is rejected'
fi

if (db_profile_resolve LOCAL && unset HUNTTECH_LOCAL_DB_PASSWORD && db_profile_require_password) >/dev/null 2>&1; then
    fail 'missing LOCAL password was accepted'
else
    pass 'missing LOCAL password is rejected'
fi

render_dir="$(mktemp -d)"
trap 'rm -rf "$render_dir"' EXIT
if HUNTTECH_LOCAL_DB_PASSWORD='p<&"' \
   bash "$ROOT/scripts/render-db-context.sh" --profile LOCAL --output "$render_dir/context.xml" >/dev/null 2>&1 &&
   [[ "$(stat -f '%OLp' "$render_dir/context.xml" 2>/dev/null || stat -c '%a' "$render_dir/context.xml")" == "600" ]] &&
   rg -q 'jdbc:postgresql://192\.168\.1\.135:5432/hunttech' "$render_dir/context.xml" &&
   rg -q 'p&lt;&amp;&quot;' "$render_dir/context.xml"; then
    pass 'rendered context uses profile host, escapes password, and is mode 600'
else
    fail 'rendered context contract failed'
fi

if rg -n 'password="cuba"|^cuba\.dataSource\.password=cuba$' \
    "$ROOT/modules/core/web/META-INF" "$ROOT/modules/core/src" -g '*.properties' >/dev/null; then
    fail 'tracked datasource password remains in application configuration'
else
    pass 'tracked datasource password is absent'
fi

if HUNTTECH_DB_PROFILE=UNKNOWN "$ROOT/scripts/validate-db-profile.sh" --profile UNKNOWN >/dev/null 2>&1; then
    fail 'validation command accepted an unknown profile'
else
    pass 'validation command rejects an unknown profile'
fi

if [[ "$failures" -ne 0 ]]; then
    printf '%s\n' "${failures} db-profile test(s) failed" >&2
    exit 1
fi
printf '%s\n' 'All db-profile tests passed.'
