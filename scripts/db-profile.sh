#!/usr/bin/env bash

# Безопасный контракт профилей БД HRM HuntTech.
# Скрипт намеренно не содержит паролей и не печатает их.

db_profile_error() {
    printf 'DB profile error: %s\n' "$*" >&2
    return 1
}

db_profile_require_value() {
    local variable="$1"
    local value="${!variable-}"
    if [[ -z "$value" ]]; then
        db_profile_error "required variable ${variable} is not set"
        return 1
    fi
}

db_profile_validate_token() {
    local name="$1"
    local value="$2"
    if [[ -z "$value" || "$value" == *[!A-Za-z0-9._-]* ]]; then
        db_profile_error "${name} contains unsupported characters"
        return 1
    fi
}

db_profile_validate_port() {
    local name="$1"
    local value="$2"
    if [[ ! "$value" =~ ^[0-9]+$ ]]; then
        db_profile_error "${name} must be a TCP port"
        return 1
    fi
    if (( ${#value} > 5 )); then
        db_profile_error "${name} must be a TCP port"
        return 1
    fi
    local decimal_value=$((10#$value))
    if (( decimal_value < 1 || decimal_value > 65535 )); then
        db_profile_error "${name} must be a TCP port"
        return 1
    fi
}

db_profile_validate_host() {
    local name="$1"
    local value="$2"
    if [[ -z "$value" || "$value" == *[!A-Za-z0-9._-]* ]]; then
        db_profile_error "${name} contains unsupported characters"
        return 1
    fi
}

db_profile_normalize_host() {
    local value="$1"
    value="$(printf '%s' "$value" | tr '[:upper:]' '[:lower:]')"
    value="${value%.}"
    printf '%s' "$value"
}

db_profile_test_host_is_forbidden() {
    local host
    host="$(db_profile_normalize_host "$1")"
    [[ "$host" == 0.* || "$host" =~ (^|\.)0[0-9] ]] && return 0
    [[ "$host" == "192.168.1.135" ||
       "$host" == "127.0.0.1" || "$host" == 127.* ||
       "$host" == "localhost" || "$host" == localhost.* ||
       "$host" == "[::1]" || "$host" == "::1" ||
       "$host" == "0:0:0:0:0:0:0:1" || "$host" == "0.0.0.0" ||
       "$host" =~ ^[0-9]+$ ]]
}

db_profile_resolve() {
    local profile="${1:-${HUNTTECH_DB_PROFILE:-}}"

    DB_PROFILE_NAME=""
    DB_PROFILE_HOST=""
    DB_PROFILE_PORT=""
    DB_PROFILE_DATABASE=""
    DB_PROFILE_USER=""
    DB_PROFILE_PASSWORD_VAR=""

    case "$profile" in
        LOCAL)
            DB_PROFILE_NAME=LOCAL
            DB_PROFILE_HOST="${HUNTTECH_LOCAL_DB_HOST:-127.0.0.1}"
            DB_PROFILE_PORT="${HUNTTECH_LOCAL_DB_PORT:-5432}"
            DB_PROFILE_DATABASE="${HUNTTECH_LOCAL_DB_NAME:-hunttech}"
            DB_PROFILE_USER="${HUNTTECH_LOCAL_DB_USER:-cuba}"
            DB_PROFILE_PASSWORD_VAR=HUNTTECH_LOCAL_DB_PASSWORD
            ;;
        TEST)
            DB_PROFILE_NAME=TEST
            DB_PROFILE_PASSWORD_VAR=HUNTTECH_TEST_DB_PASSWORD
            db_profile_require_value HUNTTECH_TEST_DB_HOST || return 1
            db_profile_require_value HUNTTECH_TEST_DB_PORT || return 1
            db_profile_require_value HUNTTECH_TEST_DB_NAME || return 1
            db_profile_require_value HUNTTECH_TEST_DB_USER || return 1
            DB_PROFILE_HOST="$HUNTTECH_TEST_DB_HOST"
            DB_PROFILE_PORT="$HUNTTECH_TEST_DB_PORT"
            DB_PROFILE_DATABASE="$HUNTTECH_TEST_DB_NAME"
            DB_PROFILE_USER="$HUNTTECH_TEST_DB_USER"
            if db_profile_test_host_is_forbidden "$DB_PROFILE_HOST"; then
                db_profile_error "TEST must use an explicitly separate database host"
                return 1
            fi
            ;;
        PRODUCTION)
            # Production is fail-closed. localhost is rejected even though it
            # often resolves to loopback, because it permits an ambiguous fallback.
            if [[ -n "${HUNTTECH_DB_HOST:-}" ]]; then
                db_profile_error "generic HUNTTECH_DB_HOST is forbidden for PRODUCTION"
                return 1
            fi
            if [[ "${HUNTTECH_PRODUCTION_DB_HOST:-127.0.0.1}" != "127.0.0.1" ]]; then
                db_profile_error "PRODUCTION host must be exactly 127.0.0.1"
                return 1
            fi
            DB_PROFILE_NAME=PRODUCTION
            DB_PROFILE_HOST=127.0.0.1
            DB_PROFILE_PORT="${HUNTTECH_PRODUCTION_DB_PORT:-5432}"
            DB_PROFILE_DATABASE="${HUNTTECH_PRODUCTION_DB_NAME:-hunttech}"
            DB_PROFILE_USER="${HUNTTECH_PRODUCTION_DB_USER:-cuba}"
            DB_PROFILE_PASSWORD_VAR=HUNTTECH_PRODUCTION_DB_PASSWORD
            ;;
        *)
            db_profile_error "unknown profile '${profile:-<empty>}'"
            return 1
            ;;
    esac

    db_profile_validate_host "${DB_PROFILE_NAME} host" "$DB_PROFILE_HOST" || return 1
    db_profile_validate_port "${DB_PROFILE_NAME} port" "$DB_PROFILE_PORT" || return 1
    db_profile_validate_token "${DB_PROFILE_NAME} database" "$DB_PROFILE_DATABASE" || return 1
    db_profile_validate_token "${DB_PROFILE_NAME} user" "$DB_PROFILE_USER" || return 1
}

db_profile_require_password() {
    [[ -n "${DB_PROFILE_PASSWORD_VAR:-}" ]] || {
        db_profile_error 'profile is not resolved'
        return 1
    }
    local password="${!DB_PROFILE_PASSWORD_VAR-}"
    if [[ -z "$password" ]]; then
        db_profile_error "${DB_PROFILE_PASSWORD_VAR} must be supplied outside Git"
        return 1
    fi
    if [[ "$password" == *$'\n'* || "$password" == *$'\r'* ]]; then
        db_profile_error "${DB_PROFILE_PASSWORD_VAR} must not contain newlines"
        return 1
    fi
}

db_profile_jdbc_url() {
    [[ -n "${DB_PROFILE_HOST:-}" ]] || {
        db_profile_error 'profile is not resolved'
        return 1
    }
    printf 'jdbc:postgresql://%s:%s/%s\n' \
        "$DB_PROFILE_HOST" "$DB_PROFILE_PORT" "$DB_PROFILE_DATABASE"
}
