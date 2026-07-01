#!/bin/bash
#
# deploy-prod.sh — безопасный деплой WAR HuntTech на production-сервер
#
# Что делает:
#   1. Предварительные проверки (SSH, локальные WAR)
#   2. Интерактивный бэкап на сервере (pg_dump + копия WAR)
#   3. Остановка Tomcat, загрузка WAR (rsync + pv)
#   4. Проверка и обновление структуры БД (CUBA updateDb через SSH-туннель)
#   5. Запуск Tomcat (если не --no-start)
#   6. При сбое — предложение отката из бэкапа
#
# Использование:
#   cd /path/to/it-pearls-recruiting
#   ./deploy-prod.sh [опции]
#   ./deploy-prod.sh --help
#
# Конфигурация: ./hunttech.conf (общий с pull-prod.sh)
#
# Локальные WAR: ../tomcat_wars/*.war (как в pull-prod.sh)
# Лог: ./deploy-prod.log
#
set -euo pipefail

export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
export PGSSLMODE="${PGSSLMODE:-disable}"

# --- настраиваемые переменные (значения по умолчанию) ---
CWD=$(pwd)
current_catalog="$CWD"
ARCHIVE_DIR=""
BACKUPLOG=deploy-prod.log
LOG="${CWD}/${BACKUPLOG}"
CONFIG_FILE="${current_catalog}/hunttech.conf"

DEFAULT_DB_SERVER="hr.hunttech.ru"
DB_SERVER="$DEFAULT_DB_SERVER"
DB_USER="replica"
SSH_USER="root"
TOMCAT_WARS_DIR="/opt/tomcat/webapps"
TOMCAT_SERVICE_NAME="tomcat"
TOMCAT_WARS_LOCAL="${current_catalog}/../tomcat_wars"
FILE_STORAGE_REMOTE="/opt/app_home/fileStorage"
FILE_STORAGE_LOCAL="/opt/app_home"
DB_NAME="itpearls"
DB_DUMP_USER="postgres"
REMOTE_DB_PORT="5432"
DB_UPDATE_USER="cuba"
DB_UPDATE_PASSWORD=""

TOMCAT_UNPACK_DIRS="app app-core"

# Флаги CLI
QUIET_MODE=0
CHECK_CONFIG=0
NO_START=0
AUTO_YES=0
SKIP_DB_UPDATE=0
CONFIG_SOURCED=0

BACKUP_CREATED=0
BACKUP_REMOTE_DIR=""
DEPLOY_FAILED=0
TOMCAT_STOPPED=0
ROLLBACK_OFFERED=0
SSH_TUNNEL_PID=""
LOCAL_TUNNEL_PORT=15432
UPDATE_DB_INIT_GRADLE=""

declare -a CONFIG_TEMP_VARS=()
declare -a CONFIG_TEMP_VALUES=()

RED='\033[31m'
GREEN='\033[32m'
WHITE='\033[37m'
NC='\033[0m'

log_action() {
    local level="${1:-INFO}"
    local msg="${2:-}"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $msg" >> "$LOG"
}

info_stage() {
    echo -e "${WHITE}$*${NC}"
}

info() {
    if [ "$QUIET_MODE" -eq 0 ]; then
        echo -e "${WHITE}$*${NC}"
    fi
}

info_n() {
    if [ "$QUIET_MODE" -eq 0 ]; then
        printf "${WHITE}%s${NC}" "$*"
    fi
}

ok() {
    if [ "$QUIET_MODE" -eq 0 ]; then
        echo -e "${GREEN}OK${NC}"
    fi
}

fail() {
    echo -e "${RED}$*${NC}"
}

pv_file() {
    local size="$1"
    local file="$2"
    if [ "$QUIET_MODE" -eq 1 ]; then
        pv -q -s "$size" "$file" 2>/dev/null
    else
        pv -p --timer --rate --bytes -s "$size" "$file" 2>>"$LOG"
    fi
}

du_bytes() {
    local dir="${1:-}"
    if [ -z "$dir" ] || [ ! -d "$dir" ]; then
        echo 0
        return 0
    fi
    local kb
    kb=$(du -sk "$dir" 2>/dev/null | awk '{print $1}')
    echo $(( kb * 1024 ))
}

_CLEANUP_DONE=0
cleanup() {
    local rc=$?
    if [ "$_CLEANUP_DONE" -eq 1 ]; then
        return
    fi
    _CLEANUP_DONE=1
    stop_ssh_tunnel || true
    rm -f "$UPDATE_DB_INIT_GRADLE" 2>/dev/null || true
    echo -e "\n[Очистка] Возврат в исходный каталог: $CWD"
    cd "$CWD" || true
    trap - EXIT ERR INT TERM
    if [ "$rc" -ne 0 ]; then
        exit "$rc"
    fi
}
trap cleanup EXIT ERR INT TERM

die() {
    DEPLOY_FAILED=1
    log_action "ERROR" "Скрипт аварийно завершен: $1"
    fail "$1"
    if [ "$TOMCAT_STOPPED" -eq 1 ] && [ "$BACKUP_CREATED" -eq 1 ] && [ "$ROLLBACK_OFFERED" -eq 0 ]; then
        offer_rollback_on_failure
    fi
    echo FAIL
    exit "${2:-1}"
}

show_log_tail() {
    local n="${1:-15}"
    if [ -f "$LOG" ] && [ -s "$LOG" ]; then
        fail "Последние строки из $LOG:"
        tail -n "$n" "$LOG" | sed 's/^/  /'
    fi
}

rsync_supports_gnu_progress() {
    rsync --info=progress2 --version >/dev/null 2>&1
}

rsync_progress_args() {
    if rsync_supports_gnu_progress; then
        printf '%s' '--info=progress2'
    else
        printf '%s' '--progress'
    fi
}

homebrew_gnu_rsync_libexec() {
    if [ -d "/opt/homebrew/opt/rsync/libexec/rsync.d" ]; then
        echo "/opt/homebrew/opt/rsync/libexec/rsync.d"
    elif [ -d "/usr/local/opt/rsync/libexec/rsync.d" ]; then
        echo "/usr/local/opt/rsync/libexec/rsync.d"
    else
        echo ""
    fi
}

prepend_homebrew_gnu_rsync_path() {
    local libexec
    libexec=$(homebrew_gnu_rsync_libexec)
    if [ -n "$libexec" ]; then
        export PATH="${libexec}:${PATH}"
        log_action "INFO" "PATH обновлён: ${libexec} (GNU rsync из Homebrew)"
    fi
}

rsync_error_is_gnu_compat() {
    local err_text="${1:-}"
    echo "$err_text" | grep -qiE 'unrecognized option|--info=progress2|unknown option.*progress'
}

# Установка пакета Homebrew (без интерактивного запроса; pv-визуализация, кроме pv).
brew_install_pkg() {
    local brew_pkg="$1"

    log_action "INFO" "Установка ${brew_pkg} через Homebrew"
    info "Установка ${brew_pkg}: brew install ${brew_pkg} ..."

    if [ "$brew_pkg" = "pv" ]; then
        if ! brew install "$brew_pkg" >>"$LOG" 2>&1; then
            fail "Не удалось установить ${brew_pkg} (см. $LOG)."
            log_action "ERROR" "brew install ${brew_pkg} завершился с ошибкой"
            return 1
        fi
    elif [ "$QUIET_MODE" -eq 1 ]; then
        if ! brew install "$brew_pkg" >>"$LOG" 2>&1; then
            fail "Не удалось установить ${brew_pkg} (см. $LOG)."
            log_action "ERROR" "brew install ${brew_pkg} завершился с ошибкой"
            return 1
        fi
    else
        if command -v pv >/dev/null 2>&1; then
            if ! brew install "$brew_pkg" 2>&1 | pv -l -p --timer --rate 2>>"$LOG" | tee -a "$LOG"; then
                fail "Не удалось установить ${brew_pkg} (см. $LOG)."
                log_action "ERROR" "brew install ${brew_pkg} завершился с ошибкой"
                return 1
            fi
        elif ! brew install "$brew_pkg" >>"$LOG" 2>&1; then
            fail "Не удалось установить ${brew_pkg} (см. $LOG)."
            log_action "ERROR" "brew install ${brew_pkg} завершился с ошибкой"
            return 1
        fi
    fi
    return 0
}

# Интерактивная установка утилиты через Homebrew (pv-визуализация для brew install, кроме самого pv).
ensure_brew_tool() {
    local tool="$1"
    local brew_pkg="${2:-$tool}"

    if command -v "$tool" >/dev/null 2>&1; then
        return 0
    fi

    if ! command -v brew >/dev/null 2>&1; then
        fail "Утилита ${tool} не найдена. Homebrew недоступен (https://brew.sh)."
        log_action "ERROR" "Утилита ${tool} не найдена, Homebrew отсутствует"
        return 1
    fi

    read -r -p "Утилита ${tool} не найдена. Установить её через Homebrew? [y/N]: " answer
    if [[ ! "$answer" =~ ^[YyДд]$ ]]; then
        fail "Утилита ${tool} обязательна для работы скрипта."
        log_action "INFO" "Пользователь отказался от установки ${tool}"
        return 1
    fi

    if ! brew_install_pkg "$brew_pkg"; then
        return 1
    fi

    if [ "$tool" = "rsync" ]; then
        prepend_homebrew_gnu_rsync_path
    fi

    if command -v "$tool" >/dev/null 2>&1; then
        info "${GREEN}✓${NC} ${tool} установлен: $(command -v "$tool")"
        log_action "SUCCESS" "${tool} установлен: $(command -v "$tool")"
        return 0
    fi

    fail "После brew install ${brew_pkg} команда ${tool} по-прежнему недоступна."
    log_action "ERROR" "После brew install ${brew_pkg} команда ${tool} недоступна"
    return 1
}

install_gnu_rsync_via_brew() {
    log_action "INFO" "Попытка установки GNU rsync через Homebrew (brew install rsync)"
    if ! brew_install_pkg rsync; then
        return 1
    fi
    prepend_homebrew_gnu_rsync_path
    if rsync_supports_gnu_progress; then
        info "${GREEN}✓${NC} GNU rsync: $(rsync --version 2>/dev/null | head -1)"
        log_action "SUCCESS" "GNU rsync установлен: $(command -v rsync)"
        return 0
    fi
    fail "GNU rsync установлен, но --info=progress2 по-прежнему недоступен."
    log_action "WARN" "После brew install rsync флаг --info=progress2 не поддерживается"
    return 1
}

ensure_gnu_rsync_or_fallback() {
    if rsync_supports_gnu_progress; then
        return 0
    fi

    if [ "$QUIET_MODE" -eq 1 ]; then
        log_action "INFO" "Quiet-режим: BSD/openrsync — --progress без запроса установки GNU rsync"
        return 1
    fi

    log_action "INFO" "Обнаружен BSD/openrsync — предложение установки GNU rsync"
    if ! command -v brew >/dev/null 2>&1; then
        info "Homebrew не найден. Установите GNU rsync вручную (https://brew.sh) или используйте -q/--quiet."
        log_action "INFO" "Homebrew отсутствует — пропуск установки GNU rsync"
        return 1
    fi

    read -r -p "Обнаружен macOS openrsync без поддержки GNU-флагов. Установить GNU rsync через Homebrew? [y/N]: " answer
    if [[ ! "$answer" =~ ^[YyДд]$ ]]; then
        info "Используется --progress (BSD/openrsync)."
        log_action "INFO" "Пользователь отказался от установки GNU rsync — fallback на --progress"
        return 1
    fi

    install_gnu_rsync_via_brew
}

maybe_offer_gnu_rsync_on_failure() {
    local err_capture="${1:-}"
    if [ -z "$err_capture" ] && [ -f "$LOG" ]; then
        err_capture=$(grep -iE 'rsync:|rsync error|unrecognized|unknown option' "$LOG" | tail -8 || true)
    fi

    if ! rsync_error_is_gnu_compat "$err_capture"; then
        return 1
    fi
    if rsync_supports_gnu_progress; then
        return 1
    fi

    log_action "WARN" "Сбой rsync из-за несовместимости GNU-флагов (--info=progress2)"

    if [ "$QUIET_MODE" -eq 1 ]; then
        return 1
    fi
    if ! command -v brew >/dev/null 2>&1; then
        info "Homebrew не найден. Установите GNU rsync вручную (https://brew.sh) или запустите с -q (fallback --progress)."
        return 1
    fi

    read -r -p "Обнаружен macOS openrsync без поддержки GNU-флагов. Установить GNU rsync через Homebrew? [y/N]: " answer
    if [[ ! "$answer" =~ ^[YyДд]$ ]]; then
        log_action "INFO" "После сбоя rsync пользователь отказался от установки GNU rsync"
        return 1
    fi

    install_gnu_rsync_via_brew
}

show_rsync_failure() {
    local context="${1:-Не удалось выполнить rsync}"
    fail "$context"
    if [ -f "$LOG" ] && [ -s "$LOG" ]; then
        local rsync_err
        rsync_err=$(grep -iE 'rsync:|rsync error|unknown option|failed|error' "$LOG" | tail -8 || true)
        if [ -n "$rsync_err" ]; then
            fail "Последняя ошибка rsync (из $LOG):"
            echo "$rsync_err" | sed 's/^/  /'
        else
            show_log_tail 8
        fi
    fi
    if ! rsync_supports_gnu_progress; then
        info "Используется BSD/openrsync — прогресс через --progress."
    fi
    fail "Проверьте SSH ${SSH_USER}@${DB_SERVER} и каталог ${TOMCAT_WARS_DIR} — подробности в $LOG."
}

ssh_cmd() {
    ssh -o ConnectTimeout=15 -o BatchMode=yes "${SSH_USER}@${DB_SERVER}" "$@"
}

check_and_fix_option() {
    local option_name="$1"
    local default_value="$2"
    local comment="$3"
    if ! grep -q "^[[:space:]]*${option_name}=" "$CONFIG_FILE"; then
        echo -e "\n${RED}Предупреждение:${NC} В конфиге нет опции ${WHITE}${option_name}${NC}."
        read -r -p "Добавить значение по умолчанию \"${default_value}\"? [y/N]: " answer
        if [[ "$answer" =~ ^[YyДд]$ ]]; then
            echo -e "\n# ${comment}\n${option_name}=\"${default_value}\"" >>"$CONFIG_FILE"
            info "${GREEN}✓${NC} В конфиг добавлена опция ${option_name}=\"${default_value}\""
            log_action "INFO" "В конфиг интерактивно добавлена опция ${option_name}=\"${default_value}\""
        else
            info "Используется временное значение для текущего запуска: ${default_value}"
            CONFIG_TEMP_VARS+=("$option_name")
            CONFIG_TEMP_VALUES+=("$default_value")
            log_action "INFO" "Опция ${option_name} пропущена пользователем, используется временный дефолт: ${default_value}"
        fi
    fi
}

apply_config_temp_overrides() {
    local i
    for i in "${!CONFIG_TEMP_VARS[@]}"; do
        eval "${CONFIG_TEMP_VARS[$i]}=\"${CONFIG_TEMP_VALUES[$i]}\""
    done
}

load_or_create_config() {
    if [ ! -f "$CONFIG_FILE" ]; then
        cat >"$CONFIG_FILE" <<'EOF'
# Параметры подключения
DB_SERVER="hr.hunttech.ru"
DB_USER="replica"
SSH_USER="root"
# Пути на сервере
TOMCAT_WARS_DIR="/opt/tomcat/webapps"
TOMCAT_SERVICE_NAME="tomcat"
FILE_STORAGE_REMOTE="/opt/app_home/fileStorage"
# База данных (деплой / pg_dump)
DB_NAME="itpearls"
DB_DUMP_USER="postgres"
REMOTE_DB_PORT="5432"
DB_UPDATE_USER="cuba"
DB_UPDATE_PASSWORD=""
# Пути на локальной машине (Mac)
FILE_STORAGE_LOCAL="/opt/app_home"
# Каталог для сохранения финальных архивов (.tgz) локально
ARCHIVE_DIR="../"
EOF
        info "Создан файл конфигурации: $CONFIG_FILE"
        log_action "INFO" "Создан файл конфигурации: $CONFIG_FILE"
    else
        check_and_fix_option "DB_SERVER" "hr.hunttech.ru" "Параметры подключения"
        check_and_fix_option "DB_USER" "replica" "Пользователь PostgreSQL для pg_basebackup"
        check_and_fix_option "SSH_USER" "root" "Пользователь SSH"
        check_and_fix_option "TOMCAT_WARS_DIR" "/opt/tomcat/webapps" "Каталог WAR на сервере"
        check_and_fix_option "TOMCAT_SERVICE_NAME" "tomcat" "Имя systemd-службы Tomcat"
        check_and_fix_option "FILE_STORAGE_REMOTE" "/opt/app_home/fileStorage" "Путь fileStorage на сервере"
        check_and_fix_option "DB_NAME" "itpearls" "Имя базы данных CUBA"
        check_and_fix_option "DB_DUMP_USER" "postgres" "Пользователь для pg_dump на сервере"
        check_and_fix_option "REMOTE_DB_PORT" "5432" "Порт PostgreSQL на сервере"
        check_and_fix_option "DB_UPDATE_USER" "cuba" "Пользователь для CUBA updateDb"
        check_and_fix_option "DB_UPDATE_PASSWORD" "" "Пароль для CUBA updateDb (пусто = из ~/.pgpass)"
        check_and_fix_option "FILE_STORAGE_LOCAL" "/opt/app_home" "Пути на локальной машине (Mac)"
        check_and_fix_option "ARCHIVE_DIR" "../" "Каталог для сохранения финальных архивов (.tgz) локально"
    fi

    # shellcheck source=/dev/null
    source "$CONFIG_FILE"
    apply_config_temp_overrides
    CONFIG_SOURCED=1
}

resolve_archive_paths() {
    if [ -z "${ARCHIVE_DIR:-}" ]; then
        ARCHIVE_DIR="${current_catalog}/.."
    elif [[ "$ARCHIVE_DIR" != /* ]]; then
        ARCHIVE_DIR="${current_catalog}/${ARCHIVE_DIR}"
    fi
    mkdir -p "$ARCHIVE_DIR"
    ARCHIVE_DIR=$(cd -P "$ARCHIVE_DIR" && pwd)
}

usage() {
    cat <<EOF
deploy-prod.sh — безопасный деплой WAR HuntTech на production-сервер

Конфигурация: ./hunttech.conf (общий с pull-prod.sh)
Локальные WAR: ../tomcat_wars/*.war

Использование:
  ./deploy-prod.sh [опции]

Опции:
  -h, --help          Показать справку и выйти
  -t, --check-config  Проверка конфигурации, SSH и локальных WAR без деплоя
  -q, --quiet         Краткий вывод в консоль
  -y, --yes           Автоматически согласиться на бэкап перед деплоем
      --no-start      Деплой WAR и updateDb без запуска Tomcat
      --skip-db       Пропустить проверку и обновление структуры БД
      --archive-dir <path>  Каталог локальных архивов (ARCHIVE_DIR из hunttech.conf)

Опции подключения (переопределяют hunttech.conf):
  -s, --server <host>       Хост (DB_SERVER)
      --ssh-user <user>     Пользователь SSH
      --tomcat-dir <path>   Каталог WAR на сервере
      --wars-dir <path>     Локальный каталог WAR (по умолчанию ../tomcat_wars)
      --tomcat-service <n>  Имя systemd-службы Tomcat

Безопасность:
  Перед деплоем спрашивается: «Создать резервную копию окружения на сервере?»
  При сбое после бэкапа предлагается автоматический откат (WAR + БД).

Лог: ./deploy-prod.log
EOF
}

parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            -h|--help)
                usage
                exit 0
                ;;
            -s|--server)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <host>"
                DB_SERVER="$2"
                shift 2
                ;;
            --ssh-user)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <user>"
                SSH_USER="$2"
                shift 2
                ;;
            --tomcat-dir)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <path>"
                TOMCAT_WARS_DIR="$2"
                shift 2
                ;;
            --wars-dir)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <path>"
                TOMCAT_WARS_LOCAL="$2"
                shift 2
                ;;
            --tomcat-service)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <name>"
                TOMCAT_SERVICE_NAME="$2"
                shift 2
                ;;
            --archive-dir)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <path>"
                ARCHIVE_DIR="$2"
                shift 2
                ;;
            -q|--quiet)
                QUIET_MODE=1
                shift
                ;;
            -t|--check-config)
                CHECK_CONFIG=1
                shift
                ;;
            --no-start)
                NO_START=1
                shift
                ;;
            -y|--yes)
                AUTO_YES=1
                shift
                ;;
            --skip-db)
                SKIP_DB_UPDATE=1
                shift
                ;;
            --)
                shift
                break
                ;;
            -*)
                die "Неизвестный аргумент: $1 (см. --help)"
                ;;
            *)
                die "Неожиданный аргумент: $1 (см. --help)"
                ;;
        esac
    done
}

banner() {
    echo "*******************************************************"
    echo "*******************************************************"
    echo "**                                                   **"
    echo "**                   HuntTech                        **"
    echo "**        Деплой WAR на production-сервер            **"
    echo "**                                                   **"
    echo "*******************************************************"
    echo "*******************************************************"
}

count_local_wars() {
    find "$TOMCAT_WARS_LOCAL" -maxdepth 1 -name '*.war' 2>/dev/null | wc -l | tr -d ' '
}

local_wars_total_bytes() {
    local total=0 f sz
    for f in "$TOMCAT_WARS_LOCAL"/*.war; do
        [ -f "$f" ] || continue
        sz=$(stat -f%z "$f" 2>/dev/null || stat -c%s "$f" 2>/dev/null || echo 0)
        total=$(( total + sz ))
    done
    echo "$total"
}

check_config_and_report() {
    local errors=0 war_count

    if [ "$CONFIG_SOURCED" -eq 0 ]; then
        load_or_create_config
    fi

    log_action "INFO" "Режим проверки конфигурации (--check-config)"
    info_stage "=== Проверка конфигурации deploy-prod.sh ==="
    info "Конфиг: $CONFIG_FILE"
    info "DB_SERVER=$DB_SERVER  SSH_USER=$SSH_USER"
    info "TOMCAT_WARS_DIR=$TOMCAT_WARS_DIR  TOMCAT_SERVICE_NAME=$TOMCAT_SERVICE_NAME"
    info "DB_NAME=$DB_NAME  DB_DUMP_USER=$DB_DUMP_USER  REMOTE_DB_PORT=$REMOTE_DB_PORT"
    info "TOMCAT_WARS_LOCAL=$TOMCAT_WARS_LOCAL"
    resolve_archive_paths
    info "ARCHIVE_DIR=$ARCHIVE_DIR"

    info_stage "--- Локальные WAR ---"
    war_count=$(count_local_wars)
    if [ "$war_count" -eq 0 ]; then
        fail "Не найдены *.war в $TOMCAT_WARS_LOCAL"
        errors=$((errors + 1))
    else
        info "${GREEN}✓${NC} Найдено WAR: $war_count ($(local_wars_total_bytes) байт)"
        ls -lh "$TOMCAT_WARS_LOCAL"/*.war 2>/dev/null | sed 's/^/  /' || true
    fi

    info_stage "--- SSH ${SSH_USER}@${DB_SERVER} ---"
    info_n "Проверка SSH (ConnectTimeout=5) ... "
    if ssh_cmd "exit 0" >>"$LOG" 2>&1; then
        ok
    else
        fail "SSH-подключение не удалось (см. $LOG)"
        errors=$((errors + 1))
    fi

    info_stage "--- Удалённый Tomcat ---"
    info_n "Каталог WAR на сервере ... "
    if ssh_cmd "[ -d '${TOMCAT_WARS_DIR}' ]" >>"$LOG" 2>&1; then
        ok
        ssh_cmd "ls -lh '${TOMCAT_WARS_DIR}'/*.war 2>/dev/null || echo '(нет WAR)'" 2>>"$LOG" || true
    else
        fail "Каталог не найден: ${TOMCAT_WARS_DIR}"
        errors=$((errors + 1))
    fi

    info_stage "--- rsync ---"
    if command -v rsync >/dev/null 2>&1; then
        if rsync_supports_gnu_progress; then
            info "${GREEN}✓${NC} rsync GNU"
        else
            info "${GREEN}✓${NC} rsync BSD/openrsync (--progress)"
        fi
    else
        fail "rsync не найден"
        errors=$((errors + 1))
    fi

    if [ "$errors" -eq 0 ]; then
        info_stage "${GREEN}Проверка конфигурации пройдена успешно.${NC}"
        return 0
    fi
    fail "Проверка завершена с ошибками: ${errors}"
    return 1
}

preflight_checks() {
    log_action "INFO" "Начало этапа: предварительные проверки"
    info_stage "=== Предварительные проверки ==="
    local missing=0 war_count

    war_count=$(count_local_wars)
    if [ "$war_count" -eq 0 ]; then
        fail "Не найдены *.war в $TOMCAT_WARS_LOCAL — соберите WAR (./gradlew buildWar) или скопируйте из pull-prod."
        missing=1
    else
        info "Локальные WAR: $war_count файл(ов), ~$(local_wars_total_bytes) байт"
    fi

    ensure_brew_tool pv || missing=1

    if ! command -v ssh >/dev/null 2>&1; then
        ensure_brew_tool ssh openssh || missing=1
    fi
    if ! command -v rsync >/dev/null 2>&1; then
        ensure_brew_tool rsync || missing=1
    fi
    ensure_gnu_rsync_or_fallback || true
    if command -v rsync >/dev/null 2>&1; then
        if rsync_supports_gnu_progress; then
            info "rsync: GNU ($(rsync --version 2>/dev/null | head -1))"
        else
            info "rsync: BSD/openrsync — загрузка WAR с --progress"
            log_action "INFO" "BSD/openrsync обнаружен — используется --progress"
        fi
    fi

    info_n "Проверка SSH ${SSH_USER}@${DB_SERVER} ... "
    if ssh_cmd "exit 0" >>"$LOG" 2>&1; then
        ok
    else
        fail "SSH-подключение не удалось (см. $LOG)"
        missing=1
    fi

    if [ "$missing" -ne 0 ]; then
        die "Предварительные проверки не пройдены."
    fi
    log_action "SUCCESS" "Предварительные проверки пройдены"
}

ask_backup_confirmation() {
    if [ "$AUTO_YES" -eq 1 ]; then
        info "Флаг -y: бэкап на сервере будет создан автоматически."
        return 0
    fi
    read -r -p "Создать резервную копию окружения на сервере перед деплоем? [y/N]: " answer
    if [[ "$answer" =~ ^[YyДд]$ ]]; then
        return 0
    fi
    info "Бэкап пропущен по запросу оператора."
    return 1
}

create_remote_backup() {
    log_action "INFO" "Начало этапа: резервное копирование на сервере"
    info_stage "=== Резервное копирование на сервере ==="

    BACKUP_REMOTE_DIR="/tmp/cuba_deploy_backup_$(date +%Y%m%d_%H%M%S)"
    info "Каталог бэкапа: ${BACKUP_REMOTE_DIR}"

    if ! ssh_cmd "mkdir -p '${BACKUP_REMOTE_DIR}/wars'" >>"$LOG" 2>&1; then
        die "Не удалось создать каталог бэкапа на сервере."
    fi

    info_n "Копирование WAR с ${TOMCAT_WARS_DIR} ... "
    if ! ssh_cmd "cp -a '${TOMCAT_WARS_DIR}'/*.war '${BACKUP_REMOTE_DIR}/wars/' 2>/dev/null" >>"$LOG" 2>&1; then
        fail
        die "Не удалось скопировать WAR в бэкап (нет файлов или нет прав)."
    fi
    ok

    info_n "pg_dump базы ${DB_NAME} ... "
    if ! ssh_cmd "su - postgres -c \"pg_dump -p ${REMOTE_DB_PORT} -U ${DB_DUMP_USER} -Fc -f '${BACKUP_REMOTE_DIR}/${DB_NAME}.dump' '${DB_NAME}'\"" >>"$LOG" 2>&1; then
        fail
        die "pg_dump на сервере не удался (см. $LOG). Проверьте DB_DUMP_USER и права."
    fi
    ok

    ssh_cmd "echo 'backup_created=$(date -Iseconds 2>/dev/null || date)' > '${BACKUP_REMOTE_DIR}/backup.meta'" >>"$LOG" 2>&1 || true
    BACKUP_CREATED=1
    log_action "SUCCESS" "Бэкап создан: ${BACKUP_REMOTE_DIR}"
    info "${GREEN}✓${NC} Бэкап: WAR + ${DB_NAME}.dump в ${BACKUP_REMOTE_DIR}"
}

stop_remote_tomcat() {
    log_action "INFO" "Остановка Tomcat (${TOMCAT_SERVICE_NAME})"
    info_n "Остановка Tomcat (${TOMCAT_SERVICE_NAME}) ... "
    if ssh_cmd "systemctl stop '${TOMCAT_SERVICE_NAME}' 2>/dev/null || service '${TOMCAT_SERVICE_NAME}' stop 2>/dev/null || true" >>"$LOG" 2>&1; then
        ok
    else
        fail
        die "Не удалось остановить Tomcat."
    fi
    sleep 2
}

clear_remote_tomcat_cache() {
    info_n "Очистка распакованных каталогов CUBA ... "
    local dirs rm_cmd="true"
    for d in $TOMCAT_UNPACK_DIRS; do
        rm_cmd="${rm_cmd} && rm -rf '${TOMCAT_WARS_DIR}/${d}'"
    done
    if ssh_cmd "$rm_cmd" >>"$LOG" 2>&1; then
        ok
    else
        fail "Предупреждение: не удалось очистить распакованные каталоги (см. $LOG)"
    fi
}

upload_and_deploy_wars() {
    log_action "INFO" "Начало этапа: загрузка WAR на сервер"
    info_stage "=== Загрузка WAR на сервер (pv + SSH) ==="

    local total_bytes war_file
    total_bytes=$(local_wars_total_bytes)

    info "Загрузка ~${total_bytes} байт в ${SSH_USER}@${DB_SERVER}:${TOMCAT_WARS_DIR}/"

    for war_file in "$TOMCAT_WARS_LOCAL"/*.war; do
        [ -f "$war_file" ] || continue
        local war_name war_size upload_rc=0
        war_name=$(basename "$war_file")
        war_size=$(stat -f%z "$war_file" 2>/dev/null || stat -c%s "$war_file" 2>/dev/null || echo 0)
        info "  → ${war_name} (${war_size} байт, pv -s)"

        if ! pv_file "$war_size" "$war_file" \
            | ssh -o ConnectTimeout=15 -o BatchMode=yes \
                "${SSH_USER}@${DB_SERVER}" \
                "cat > '${TOMCAT_WARS_DIR}/${war_name}'" 2>>"$LOG"; then
            upload_rc=1
        fi

        if [ "$upload_rc" -ne 0 ]; then
            fail "Не удалось загрузить ${war_name} (см. $LOG)."
            return 1
        fi
    done

    ok
    log_action "SUCCESS" "WAR загружены в ${TOMCAT_WARS_DIR}"
}

stop_ssh_tunnel() {
    if [ -n "$SSH_TUNNEL_PID" ] && kill -0 "$SSH_TUNNEL_PID" 2>/dev/null; then
        kill "$SSH_TUNNEL_PID" 2>/dev/null || true
        wait "$SSH_TUNNEL_PID" 2>/dev/null || true
        log_action "INFO" "SSH-туннель остановлен (PID $SSH_TUNNEL_PID)"
    fi
    SSH_TUNNEL_PID=""
}

start_ssh_tunnel() {
    local port="$1"
    stop_ssh_tunnel
    info_n "SSH-туннель localhost:${port} → ${DB_SERVER}:${REMOTE_DB_PORT} ... "
    ssh -f -N -o ExitOnForwardFailure=yes -o BatchMode=yes \
        -L "${port}:127.0.0.1:${REMOTE_DB_PORT}" \
        "${SSH_USER}@${DB_SERVER}" >>"$LOG" 2>&1
    SSH_TUNNEL_PID=$(pgrep -f "ssh -f -N.*${port}:127.0.0.1:${REMOTE_DB_PORT}" | head -1 || true)
    if [ -z "$SSH_TUNNEL_PID" ]; then
        fail
        die "Не удалось поднять SSH-туннель к PostgreSQL."
    fi
    ok
    log_action "INFO" "SSH-туннель активен: PID=$SSH_TUNNEL_PID port=$port"
}

resolve_db_update_password() {
    if [ -n "$DB_UPDATE_PASSWORD" ]; then
        return 0
    fi
    if [ -f "$HOME/.pgpass" ]; then
        local line pass
        line=$(grep -E "^[^:]+:${REMOTE_DB_PORT}:\*:${DB_UPDATE_USER}:" "$HOME/.pgpass" | head -1 || true)
        if [ -n "$line" ]; then
            pass="${line##*:}"
            DB_UPDATE_PASSWORD="$pass"
            return 0
        fi
        line=$(grep -E "^127\.0\.0\.1:${LOCAL_TUNNEL_PORT}:\*:${DB_UPDATE_USER}:" "$HOME/.pgpass" | head -1 || true)
        if [ -n "$line" ]; then
            DB_UPDATE_PASSWORD="${line##*:}"
            return 0
        fi
    fi
    read -r -s -p "Пароль PostgreSQL для ${DB_UPDATE_USER}@${DB_NAME}: " DB_UPDATE_PASSWORD
    echo
}

write_update_db_init_gradle() {
    UPDATE_DB_INIT_GRADLE="${current_catalog}/.deploy-updateDb-init.gradle"
    cat >"$UPDATE_DB_INIT_GRADLE" <<'GRADLE_EOF'
if (project.hasProperty('deployDbHost')) {
    project(':app-core').tasks.named('updateDb').configure {
        host = project.property('deployDbHost')
        if (project.hasProperty('deployDbPort')) {
            port = project.property('deployDbPort') as Integer
        }
        if (project.hasProperty('deployDbName')) {
            dbName = project.property('deployDbName')
        }
        if (project.hasProperty('deployDbUser')) {
            dbUser = project.property('deployDbUser')
        }
        if (project.hasProperty('deployDbPassword')) {
            dbPassword = project.property('deployDbPassword')
        }
    }
}
GRADLE_EOF
}

check_remote_schema_drift() {
    local local_schema="/tmp/deploy_local_schema_${DB_NAME}.sql"
    local remote_schema="/tmp/deploy_remote_schema_${DB_NAME}.sql"
    local pg11_bin=""

    if [ -d "/opt/homebrew/opt/postgresql@11/bin" ]; then
        pg11_bin="/opt/homebrew/opt/postgresql@11/bin"
    elif [ -d "/usr/local/opt/postgresql@11/bin" ]; then
        pg11_bin="/usr/local/opt/postgresql@11/bin"
    fi

    info_n "Сравнение схемы БД (локальная vs ${DB_SERVER}) ... "

    if [ -n "$pg11_bin" ] && [ -x "${pg11_bin}/pg_dump" ]; then
        if ! "${pg11_bin}/pg_dump" -h localhost -p 5432 -U "$DB_UPDATE_USER" --schema-only "$DB_NAME" \
            >"$local_schema" 2>>"$LOG"; then
            echo
            info "Локальный pg_dump недоступен — проверка схемы пропущена (будет только updateDb)."
            rm -f "$local_schema" "$remote_schema"
            return 2
        fi
    else
        echo
        info "Локальный PostgreSQL 11 не найден — проверка схемы пропущена."
        return 2
    fi

    if ! ssh_cmd "pg_dump -p ${REMOTE_DB_PORT} -U ${DB_DUMP_USER} --schema-only '${DB_NAME}'" \
        >"$remote_schema" 2>>"$LOG"; then
        echo
        fail "Удалённый pg_dump --schema-only не удался."
        rm -f "$local_schema" "$remote_schema"
        return 1
    fi

    sed -i.bak '/^--/d; /^$/d' "$local_schema" "$remote_schema" 2>/dev/null || true

    if diff -q "$local_schema" "$remote_schema" >/dev/null 2>&1; then
        ok
        info "Структура БД синхронна с локальной."
        rm -f "$local_schema" "$remote_schema" "${local_schema}.bak" "${remote_schema}.bak"
        return 0
    fi

    echo
    fail "Структура локальной и удалённой БД различается."
    info "Краткий diff (CREATE/ALTER):"
    diff "$remote_schema" "$local_schema" 2>/dev/null \
        | grep -E 'CREATE TABLE|ALTER TABLE|CREATE INDEX' | head -20 | sed 's/^/  /' || true
    rm -f "$local_schema" "$remote_schema" "${local_schema}.bak" "${remote_schema}.bak"
    return 1
}

run_cuba_update_db() {
    log_action "INFO" "Запуск CUBA updateDb через SSH-туннель"
    resolve_db_update_password
    write_update_db_init_gradle
    start_ssh_tunnel "$LOCAL_TUNNEL_PORT"

    info "Сборка скриптов миграции (assembleDbScripts)..."
    if ! ./gradlew :app-core:assembleDbScripts --no-daemon -q >>"$LOG" 2>&1; then
        die "assembleDbScripts завершился с ошибкой (см. $LOG)."
    fi

    info "Применение миграций (updateDb → localhost:${LOCAL_TUNNEL_PORT}/${DB_NAME})..."
    if ! ./gradlew :app-core:updateDb \
        -I "$UPDATE_DB_INIT_GRADLE" \
        -PdeployDbHost=127.0.0.1 \
        -PdeployDbPort="$LOCAL_TUNNEL_PORT" \
        -PdeployDbName="$DB_NAME" \
        -PdeployDbUser="$DB_UPDATE_USER" \
        -PdeployDbPassword="$DB_UPDATE_PASSWORD" \
        --no-daemon >>"$LOG" 2>&1; then
        show_log_tail 20
        die "CUBA updateDb завершился с ошибкой (см. $LOG)."
    fi

    stop_ssh_tunnel
    log_action "SUCCESS" "CUBA updateDb выполнен"
    info "${GREEN}✓${NC} Структура БД обновлена средствами CUBA Platform."
}

db_structure_validation_and_update() {
    log_action "INFO" "Начало этапа: проверка и обновление структуры БД"
    info_stage "=== Проверка и обновление структуры БД ==="

    info_n "Dry-run: сборка скриптов миграции (assembleDbScripts) ... "
    if ! ./gradlew :app-core:assembleDbScripts --no-daemon -q >>"$LOG" 2>&1; then
        echo
        die "assembleDbScripts (dry-run) завершился с ошибкой (см. $LOG)."
    fi
    ok
    log_action "INFO" "assembleDbScripts dry-run успешен"

    local drift_rc=0
    check_remote_schema_drift || drift_rc=$?

    if [ "$drift_rc" -eq 0 ]; then
        info "Обновление структуры БД не требуется."
        log_action "INFO" "Схема БД синхронна — updateDb пропущен"
        return 0
    fi

    fail "[WARN] Структура базы данных на сервере устарела."
    if [ "$drift_rc" -eq 2 ]; then
        info "Детальное сравнение схемы недоступно — рекомендуется updateDb."
        log_action "WARN" "Сравнение схемы пропущено — предложен updateDb"
    else
        log_action "WARN" "Структура БД на сервере устарела (diff схем)"
    fi

    local answer=""
    if [ "$AUTO_YES" -eq 1 ]; then
        answer="y"
    else
        read -r -p "Обновить структуру БД средствами CUBA Platform? [y/N]: " answer
    fi
    if [[ ! "$answer" =~ ^[YyДд]$ ]]; then
        info "Обновление структуры БД пропущено по запросу оператора."
        log_action "INFO" "updateDb пропущен пользователем"
        return 0
    fi

    run_cuba_update_db
}

start_remote_tomcat() {
    log_action "INFO" "Запуск Tomcat (${TOMCAT_SERVICE_NAME})"
    info_n "Запуск Tomcat (${TOMCAT_SERVICE_NAME}) ... "
    if ssh_cmd "systemctl start '${TOMCAT_SERVICE_NAME}' 2>/dev/null || service '${TOMCAT_SERVICE_NAME}' start" >>"$LOG" 2>&1; then
        ok
    else
        die "Не удалось запустить Tomcat."
    fi
    sleep 3
    info_n "Статус Tomcat ... "
    local status
    status=$(ssh_cmd "systemctl is-active '${TOMCAT_SERVICE_NAME}' 2>/dev/null || echo unknown" 2>>"$LOG" || echo unknown)
    if [ "$status" = "active" ]; then
        ok
    else
        info "статус: ${status} (проверьте journalctl на сервере)"
    fi
}

rollback_system() {
    if [ "$BACKUP_CREATED" -ne 1 ] || [ -z "$BACKUP_REMOTE_DIR" ]; then
        fail "Откат невозможен: бэкап на сервере не создавался."
        return 1
    fi

    log_action "WARN" "Начало отката из ${BACKUP_REMOTE_DIR}"
    info_stage "=== Откат из бэкапа ${BACKUP_REMOTE_DIR} ==="

    stop_remote_tomcat
    clear_remote_tomcat_cache

    info_n "Восстановление WAR ... "
    if ! ssh_cmd "cp -a '${BACKUP_REMOTE_DIR}/wars/'*.war '${TOMCAT_WARS_DIR}/'" >>"$LOG" 2>&1; then
        fail
        die "Не удалось восстановить WAR из бэкапа."
    fi
    ok

    info_n "Восстановление БД из pg_dump ... "
    if ! ssh_cmd "su - postgres -c \"pg_restore -p ${REMOTE_DB_PORT} -U ${DB_DUMP_USER} -d '${DB_NAME}' --clean --if-exists '${BACKUP_REMOTE_DIR}/${DB_NAME}.dump'\"" >>"$LOG" 2>&1; then
        fail
        die "pg_restore не удался (см. $LOG). Возможно, потребуется ручное восстановление."
    fi
    ok

    if [ "$NO_START" -eq 0 ]; then
        start_remote_tomcat
    fi

    log_action "SUCCESS" "Откат завершён из ${BACKUP_REMOTE_DIR}"
    info "${GREEN}✓${NC} Система восстановлена из бэкапа."
    return 0
}

offer_rollback_on_failure() {
    if [ "$ROLLBACK_OFFERED" -eq 1 ]; then
        return 0
    fi
    ROLLBACK_OFFERED=1
    if [ "$BACKUP_CREATED" -ne 1 ]; then
        return 0
    fi
    echo
    fail "[ОШИБКА] Деплой завершился сбоем."
    local answer=""
    read -r -p "Вернуть систему в первоначальное состояние из бэкапа? [y/N]: " answer
    if [[ "$answer" =~ ^[YyДд]$ ]]; then
        rollback_system || true
    else
        info "Откат не выполнен. Бэкап сохранён: ${BACKUP_REMOTE_DIR}"
    fi
}

on_deploy_error() {
    local rc=$?
    if [ "$rc" -eq 0 ]; then
        return 0
    fi
    DEPLOY_FAILED=1
    if [ "$TOMCAT_STOPPED" -eq 1 ]; then
        offer_rollback_on_failure
    fi
    return "$rc"
}

main_deploy() {
    trap on_deploy_error ERR

    if ask_backup_confirmation; then
        create_remote_backup
    fi

    stop_remote_tomcat
    TOMCAT_STOPPED=1
    clear_remote_tomcat_cache
    upload_and_deploy_wars

    if [ "$SKIP_DB_UPDATE" -eq 0 ]; then
        db_structure_validation_and_update
    else
        info "Флаг --skip-db: проверка и updateDb пропущены."
    fi

    if [ "$NO_START" -eq 0 ]; then
        start_remote_tomcat
    else
        info "Флаг --no-start: Tomcat не запускался."
    fi

    trap - ERR
}

# --- main ---
load_or_create_config
parse_args "$@"

if [ "$CHECK_CONFIG" -eq 1 ]; then
    : >"$LOG"
    log_action "INFO" "Запуск deploy-prod.sh --check-config"
    check_config_and_report
    exit $?
fi

resolve_archive_paths

: >"$LOG"
log_action "INFO" "Запуск deploy-prod.sh"

banner
if [ "$DB_SERVER" = "$DEFAULT_DB_SERVER" ]; then
    info "Сервер: ${GREEN}${DB_SERVER}${NC} (значение по умолчанию)"
else
    info "Сервер: ${GREEN}${DB_SERVER}${NC} (hunttech.conf или CLI)"
fi
info "Конфиг: $CONFIG_FILE"
info "ARCHIVE_DIR=$ARCHIVE_DIR"
info "Локальные WAR: $TOMCAT_WARS_LOCAL → ${TOMCAT_WARS_DIR}"
if [ "$NO_START" -eq 1 ]; then info "Флаг: --no-start"; fi
if [ "$SKIP_DB_UPDATE" -eq 1 ]; then info "Флаг: --skip-db"; fi
if [ "$AUTO_YES" -eq 1 ]; then info "Флаг: -y (авто-бэкап и updateDb)"; fi
if [ "$QUIET_MODE" -eq 1 ]; then info "Флаг: --quiet"; fi

preflight_checks
main_deploy

log_action "SUCCESS" "deploy-prod.sh завершён успешно"
info_stage "${GREEN}Деплой на ${DB_SERVER} завершён успешно.${NC}"
if [ "$BACKUP_CREATED" -eq 1 ]; then
    info "Бэкап на сервере: ${BACKUP_REMOTE_DIR}"
fi
if [ "$NO_START" -eq 1 ]; then
    info "Tomcat не запускался (--no-start). Запустите вручную: systemctl start ${TOMCAT_SERVICE_NAME}"
fi
