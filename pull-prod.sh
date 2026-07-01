#!/bin/bash
#
# pull-prod.sh — полный локальный backup HuntTech с production-сервера
#
# Что делает:
#   1. pg_basebackup (--wal-method=fetch, tar stream) → временный каталог
#   2. Установка кластера в локальный PGDATA (Homebrew PostgreSQL 11)
#   3. rsync pull fileStorage с сервера
#   4. tar pull Tomcat *.war с сервера
#   5. Архив PGDATA + tomcat_wars → hunttech-basebackup.tgz
#   6. Запуск локальной PostgreSQL
#
# Использование:
#   cd /path/to/it-pearls-recruiting
#   ./pull-prod.sh [опции]
#   ./pull-prod.sh --help
#
# Конфигурация: ./hunttech.conf (создаётся автоматически при первом запуске)
#
# Требования (локально):
#   - brew install postgresql@11 pv rsync
#   - brew install pv   (обязательно — прогресс всех длительных операций)
#   - ~/.pgpass с правами 600, строка: <DB_SERVER>:5432:*:<DB_USER>:<пароль>
#   - SSH-ключ ${SSH_USER}@${DB_SERVER} (для диагностики, rsync, tar pull WAR)
#   - Каталог FILE_STORAGE_LOCAL для fileStorage (создаётся при необходимости)
#
# На сервере — ТОЛЬКО чтение:
#   pg_basebackup (replica), ssh su postgres psql SHOW, rsync pull, tar -cf (read)
#   Никаких рестартов, записи, миграций на продакшене.
#
# Приоритет настроек: значения по умолчанию в скрипте → hunttech.conf → CLI-флаги.
#
# Лог: ./pull-prod.log
#
set -euo pipefail

export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8

# Обход segfault libpq 11.x на macOS при SSL-рукопожатии с удалённым сервером.
export PGSSLMODE="${PGSSLMODE:-disable}"

# --- настраиваемые переменные (значения по умолчанию) ---
CWD=$(pwd)
current_catalog="$CWD"
postgre_temp_database="${current_catalog}/postgre_tmp_database"
ARCHIVE_DIR=""
BACKUPLOG=pull-prod.log
LOG="${CWD}/${BACKUPLOG}"
CONFIG_FILE="${current_catalog}/hunttech.conf"

DEFAULT_DB_SERVER="hr.hunttech.ru"
DB_SERVER="$DEFAULT_DB_SERVER"
DB_USER="replica"
SSH_USER="root"
TOMCAT_WARS_DIR="/opt/tomcat/webapps"
TOMCAT_WARS_LOCAL="${current_catalog}/../tomcat_wars"
FILE_STORAGE_REMOTE="/opt/app_home/fileStorage"
FILE_STORAGE_LOCAL="/opt/app_home"

# Флаги CLI (см. usage / parse_args)
SKIP_DB=0
SKIP_FILES=0
SKIP_WARS=0
SKIP_DOWNLOAD=0
BACKUP_ONLY=0
NO_START=0
QUIET_MODE=0
CHECK_CONFIG=0
CONFIG_SOURCED=0

declare -a CONFIG_TEMP_VARS=()
declare -a CONFIG_TEMP_VALUES=()

PG11_BIN=""
PGDATA=""
PG_BASEBACKUP=""
PG_CTL=""
PSQL=""

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

# pv для файла (аргумент) или потока stdin (без файла — вызывать в pipe).
pv_file() {
    local size="$1"
    local file="$2"
    if [ "$QUIET_MODE" -eq 1 ]; then
        pv -q -s "$size" "$file" 2>/dev/null
    else
        pv -p --timer --rate --bytes -s "$size" "$file" 2>>"$LOG"
    fi
}

pv_stream() {
    local size="${1:-}"
    if [ "$QUIET_MODE" -eq 1 ]; then
        if [ -n "$size" ]; then
            pv -q -s "$size" 2>/dev/null
        else
            pv -q 2>/dev/null
        fi
    else
        if [ -n "$size" ]; then
            pv -p --timer --rate --bytes -s "$size" 2>>"$LOG"
        else
            pv -p --timer --rate --bytes 2>>"$LOG"
        fi
    fi
}

# Размер каталога в байтах (macOS: du -sk → килобайты × 1024).
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

# Гарантированный возврат в исходный каталог при любом выходе (ошибка или успех).
_CLEANUP_DONE=0
cleanup() {
    local rc=$?
    if [ "$_CLEANUP_DONE" -eq 1 ]; then
        return
    fi
    _CLEANUP_DONE=1
    echo -e "\n[Очистка] Возврат в исходный каталог: $CWD"
    cd "$CWD" || true
    trap - EXIT ERR INT TERM
    if [ "$rc" -ne 0 ]; then
        exit "$rc"
    fi
}
trap cleanup EXIT ERR INT TERM

die() {
    log_action "ERROR" "Скрипт аварийно завершен: $1"
    fail "$1"
    echo FAIL
    exit "${2:-1}"
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
FILE_STORAGE_REMOTE="/opt/app_home/fileStorage"
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
        check_and_fix_option "TOMCAT_WARS_DIR" "/opt/tomcat/webapps" "Пути на сервере"
        check_and_fix_option "FILE_STORAGE_REMOTE" "/opt/app_home/fileStorage" "Путь fileStorage на сервере"
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

check_dir_writable() {
    local dir="$1"
    local label="$2"
    local test_file="${dir}/.pull-prod-write-test"

    info_n "Запись в ${label} (${dir}) ... "
    if ! mkdir -p "$dir" 2>>"$LOG"; then
        echo
        fail "Не удалось создать каталог: ${dir}"
        return 1
    fi
    if touch "$test_file" 2>>"$LOG" && rm -f "$test_file" 2>>"$LOG"; then
        ok
        return 0
    fi
    echo
    fail "Каталог недоступен для записи: ${dir}"
    return 1
}

check_config_and_report() {
    local errors=0

    if [ "$CONFIG_SOURCED" -eq 0 ]; then
        load_or_create_config
    fi

    log_action "INFO" "Режим проверки конфигурации (--check-config)"
    info_stage "=== Проверка конфигурации pull-prod.sh ==="
    info "Конфиг: $CONFIG_FILE"
    info "DB_SERVER=$DB_SERVER  DB_USER=$DB_USER  SSH_USER=$SSH_USER"
    info "TOMCAT_WARS_DIR=$TOMCAT_WARS_DIR"
    info "FILE_STORAGE_REMOTE=$FILE_STORAGE_REMOTE"
    info "FILE_STORAGE_LOCAL=$FILE_STORAGE_LOCAL"

    info_stage "--- PostgreSQL 11 (локально) ---"
    setup_pg11_paths
    info "PG11_BIN=$PG11_BIN"
    info "PGDATA=$PGDATA"
    for bin in pg_basebackup pg_ctl psql; do
        if [ -x "$PG11_BIN/$bin" ]; then
            info "${GREEN}✓${NC} $PG11_BIN/$bin"
        else
            fail "Не найден исполняемый файл: $PG11_BIN/$bin"
            errors=$((errors + 1))
        fi
    done

    info_stage "--- ~/.pgpass ---"
    if [ ! -f "$HOME/.pgpass" ]; then
        fail "Не найден ~/.pgpass — добавьте строку: ${DB_SERVER}:5432:*:${DB_USER}:<пароль>"
        log_action "ERROR" "Не найден ~/.pgpass"
        errors=$((errors + 1))
    else
        local pgpass_mode
        pgpass_mode=$(stat -f '%OLp' "$HOME/.pgpass" 2>/dev/null || stat -c '%a' "$HOME/.pgpass" 2>/dev/null)
        if [ "$pgpass_mode" = "600" ]; then
            info "${GREEN}✓${NC} ~/.pgpass (права 600)"
            log_action "INFO" "~/.pgpass найден, права 600"
        else
            fail "Неверные права на ~/.pgpass (сейчас ${pgpass_mode:-?}, нужно 600): chmod 600 ~/.pgpass"
            log_action "ERROR" "Неверные права на ~/.pgpass: ${pgpass_mode:-?}"
            errors=$((errors + 1))
        fi
    fi

    info_stage "--- Сеть: PostgreSQL ${DB_SERVER}:5432 ---"
    if ! check_remote_port; then
        log_action "ERROR" "Порт 5432 на ${DB_SERVER} недоступен"
        errors=$((errors + 1))
    else
        log_action "INFO" "Порт 5432 на ${DB_SERVER} доступен"
    fi

    info_stage "--- SSH ${SSH_USER}@${DB_SERVER} ---"
    info_n "Проверка SSH (ConnectTimeout=5) ... "
    if ssh -o ConnectTimeout=5 -o BatchMode=yes "${SSH_USER}@${DB_SERVER}" "exit 0" >>"$LOG" 2>&1; then
        ok
        log_action "INFO" "SSH-подключение к ${SSH_USER}@${DB_SERVER} успешно"
    else
        fail "SSH-подключение к ${SSH_USER}@${DB_SERVER} не удалось (см. $LOG)"
        log_action "ERROR" "SSH-подключение к ${SSH_USER}@${DB_SERVER} не удалось"
        errors=$((errors + 1))
    fi

    info_stage "--- Локальные каталоги (запись) ---"
    resolve_archive_paths
    info "ARCHIVE_DIR=$ARCHIVE_DIR"
    if ! check_dir_writable "$ARCHIVE_DIR" "ARCHIVE_DIR"; then
        log_action "ERROR" "ARCHIVE_DIR недоступен для записи: $ARCHIVE_DIR"
        errors=$((errors + 1))
    else
        log_action "INFO" "ARCHIVE_DIR доступен для записи: $ARCHIVE_DIR"
    fi
    if ! check_dir_writable "$FILE_STORAGE_LOCAL" "FILE_STORAGE_LOCAL"; then
        log_action "ERROR" "FILE_STORAGE_LOCAL недоступен для записи: $FILE_STORAGE_LOCAL"
        errors=$((errors + 1))
    else
        log_action "INFO" "FILE_STORAGE_LOCAL доступен для записи: $FILE_STORAGE_LOCAL"
    fi

    if [ "$errors" -eq 0 ]; then
        info_stage "${GREEN}Проверка конфигурации пройдена успешно.${NC}"
        log_action "SUCCESS" "Проверка конфигурации пройдена успешно"
        return 0
    fi

    fail "Проверка конфигурации завершена с ошибками: ${errors}"
    log_action "ERROR" "Проверка конфигурации завершена с ошибками: ${errors}"
    return 1
}

usage() {
    cat <<EOF
pull-prod.sh — полный локальный backup HuntTech с production-сервера

Скачивает base backup PostgreSQL, при необходимости устанавливает в локальный
PGDATA, синхронизирует fileStorage и WAR Tomcat, упаковывает tar.gz и
(в полном режиме) запускает локальную PostgreSQL.

На сервере — только чтение (pg_basebackup, rsync pull, tar).

Конфигурация: ./hunttech.conf
  При первом запуске файл создаётся автоматически с параметрами по умолчанию.
  Приоритет: значения скрипта → hunttech.conf → CLI-флаги.

Использование:
  ./pull-prod.sh [опции]

Опции подключения (переопределяют hunttech.conf):
  -s, --server <host>       Хост PostgreSQL / SSH (DB_SERVER)
  -u, --db-user <user>      Пользователь PostgreSQL для pg_basebackup (DB_USER)
      --ssh-user <user>     Пользователь SSH (SSH_USER)
      --tomcat-dir <path>   Каталог WAR на сервере (TOMCAT_WARS_DIR)
      --local-storage <path> Локальный каталог для fileStorage (FILE_STORAGE_LOCAL)
      --archive-dir <path>  Локальный каталог для сохранения готовых архивов tgz
                            (например, /Volumes/ExternalHD/backups)

Опции режима:
  -h, --help        Показать эту справку и выйти
  -t, --check-config  Интерактивная проверка hunttech.conf и окружения
                      (PostgreSQL 11, ~/.pgpass, порт 5432, SSH, каталоги
                      ARCHIVE_DIR и FILE_STORAGE_LOCAL) без выполнения backup
  -c, --cached      Использовать готовый backup во временном каталоге
                    (postgre_tmp_database) без повторной загрузки и без запроса
  -b, --backup-only Скачать данные и создать архив без установки в PGDATA
                    и без запуска локальной PostgreSQL; в архив попадают
                    временный каталог backup и tomcat_wars
  --skip-db         Пропустить загрузку base backup и установку в PGDATA
  --skip-files      Пропустить rsync fileStorage (каталог FILE_STORAGE_LOCAL отдельно)
  --skip-wars       Пропустить загрузку Tomcat *.war
  -q, --quiet        Включить краткий режим вывода в консоль (скрывает прогресс-бары
                     и детальные шаги). По умолчанию вывод подробный.

Примеры:
  ./pull-prod.sh                    полный цикл (по умолчанию)
  ./pull-prod.sh -c                 повторно использовать кэш backup БД
  ./pull-prod.sh -b                 только скачать и упаковать, без PGDATA
  ./pull-prod.sh --skip-files       БД + WAR, без fileStorage
  ./pull-prod.sh --skip-db --skip-wars   только fileStorage + архив WAR
  ./pull-prod.sh -s staging.example.com --ssh-user deploy
  ./pull-prod.sh -t                  проверить конфиг и окружение без backup

Лог: ./pull-prod.log
EOF
}

setup_pg11_paths() {
    if [ -n "$PG11_BIN" ] && [ -x "$PG11_BIN/pg_basebackup" ]; then
        return 0
    fi
    if [ -d "/opt/homebrew/opt/postgresql@11/bin" ]; then
        PG11_BIN="/opt/homebrew/opt/postgresql@11/bin"
        PGDATA="/opt/homebrew/var/postgresql@11"
    elif [ -d "/usr/local/opt/postgresql@11/bin" ]; then
        PG11_BIN="/usr/local/opt/postgresql@11/bin"
        PGDATA="/usr/local/var/postgresql@11"
    else
        die "Не найден PostgreSQL 11 (brew install postgresql@11)."
    fi
    export PATH="$PG11_BIN:$PATH"
    PG_BASEBACKUP="$PG11_BIN/pg_basebackup"
    PG_CTL="$PG11_BIN/pg_ctl"
    PSQL="$PG11_BIN/psql"
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
            -u|--db-user)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <user>"
                DB_USER="$2"
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
            --local-storage)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <path>"
                FILE_STORAGE_LOCAL="$2"
                shift 2
                ;;
            --archive-dir)
                [ $# -ge 2 ] || die "Опция $1 требует аргумент <path>"
                ARCHIVE_DIR="$2"
                shift 2
                ;;
            -c|--cached)
                SKIP_DOWNLOAD=1
                shift
                ;;
            -b|--backup-only)
                BACKUP_ONLY=1
                NO_START=1
                shift
                ;;
            --skip-db)
                SKIP_DB=1
                NO_START=1
                shift
                ;;
            --skip-files)
                SKIP_FILES=1
                shift
                ;;
            --skip-wars)
                SKIP_WARS=1
                shift
                ;;
            -q|--quiet)
                QUIET_MODE=1
                shift
                ;;
            -t|--check-config)
                CHECK_CONFIG=1
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
    echo "**     Полный локальный backup (БД + файлы + WAR)    **"
    echo "**                                                   **"
    echo "*******************************************************"
    echo "*******************************************************"
}

# Оценка размера кластера на удалённом сервере (байты, +15% на WAL/overhead fetch).
get_remote_cluster_size_bytes() {
    local size raw
    raw=$("$PSQL" -h "$DB_SERVER" -U "$DB_USER" -d postgres -tAc \
        "SELECT COALESCE(sum(pg_database_size(oid)), 0) FROM pg_database WHERE datistemplate = false;" \
        2>>"$LOG" || true)
    if [ -z "$raw" ] || [ "$raw" = "0" ]; then
        raw=$(ssh "${SSH_USER}@${DB_SERVER}" \
            "su - postgres -c \"psql -d postgres -tAc \\\"SELECT COALESCE(sum(pg_database_size(oid)), 0) FROM pg_database WHERE datistemplate = false;\\\"\"" \
            2>>"$LOG" || echo "0")
    fi
    size=$(( raw * 115 / 100 ))
    if [ "$size" -le 0 ]; then
        size=1073741824
        info "Не удалось оценить размер кластера — pv использует запас 1 GiB."
    fi
    echo "$size"
}

# Суммарный размер *.war на сервере (Linux stat -c%s).
get_tomcat_wars_size_bytes() {
    local size
    size=$(ssh "${SSH_USER}@${DB_SERVER}" \
        "find '${TOMCAT_WARS_DIR}' -maxdepth 1 -name '*.war' -exec stat -c%s {} + 2>/dev/null | awk '{s+=\$1} END{print s+0}'" \
        2>>"$LOG" || echo "0")
    echo "${size:-0}"
}

show_log_tail() {
    local n="${1:-15}"
    if [ -f "$LOG" ] && [ -s "$LOG" ]; then
        fail "Последние строки из $LOG:"
        tail -n "$n" "$LOG" | sed 's/^/  /'
    fi
}

check_remote_port() {
    info_n "Проверка доступности порта 5432 на $DB_SERVER ... "
    if command -v nc >/dev/null 2>&1; then
        if nc -z -w 5 "$DB_SERVER" 5432 >>"$LOG" 2>&1; then
            ok
            return 0
        fi
        echo
        fail "Порт 5432 на $DB_SERVER недоступен (nc -z)."
        return 1
    fi
    if (echo >/dev/tcp/"$DB_SERVER"/5432) 2>/dev/null; then
        ok
        return 0
    fi
    echo
    fail "Порт 5432 на $DB_SERVER недоступен (/dev/tcp)."
    return 1
}

diagnose_psql_failure() {
    local psql_output
    psql_output=$(tail -n 30 "$LOG" 2>/dev/null || true)

    show_log_tail 15
    check_remote_port || true

    fail "Подсказка: проверьте порт 5432, ~/.pgpass и параметры SSH — подробности в $LOG."

    if echo "$psql_output" | grep -qiE 'password authentication failed|authentication failed|FATAL:.*password'; then
        fail "Диагноз: ошибка аутентификации — проверьте пароль в ~/.pgpass для $DB_USER@$DB_SERVER."
    elif echo "$psql_output" | grep -qiE 'Connection refused|could not connect to server'; then
        fail "Диагноз: connection refused — PostgreSQL не слушает порт или firewall блокирует доступ."
    elif echo "$psql_output" | grep -qiE 'timeout|timed out|Operation timed out|No route to host'; then
        fail "Диагноз: таймаут сети — хост недоступен или порт фильтруется."
    elif echo "$psql_output" | grep -qiE 'SSL|ssl|certificate'; then
        fail "Диагноз: проблема SSL (PGSSLMODE=$PGSSLMODE). Попробуйте: export PGSSLMODE=disable"
    elif echo "$psql_output" | grep -qi 'Segmentation fault'; then
        fail "Диагноз: segfault libpq на macOS — скрипт уже выставляет PGSSLMODE=disable; обновите клиент PostgreSQL."
    else
        fail "Диагноз: не удалось классифицировать ошибку — см. лог выше и ~/.pgpass (chmod 600)."
    fi
}

preflight_checks() {
    log_action "INFO" "Начало этапа: предварительные проверки"
    info_stage "=== Предварительные проверки ==="
    local missing=0
    local needs_remote=0
    local needs_pv=0

    if [ "$SKIP_FILES" -eq 0 ] || [ "$SKIP_WARS" -eq 0 ] || [ "$SKIP_DB" -eq 0 ]; then
        needs_remote=1
    fi
    if [ "$SKIP_DB" -eq 0 ] || [ "$SKIP_WARS" -eq 0 ]; then
        needs_pv=1
    elif [ -d "$TOMCAT_WARS_LOCAL" ] && [ -n "$(ls -A "$TOMCAT_WARS_LOCAL" 2>/dev/null || true)" ]; then
        needs_pv=1
    fi

    if [ "$needs_pv" -eq 1 ] && ! command -v pv >/dev/null 2>&1; then
        die "Установите pv через: brew install pv"
    fi

    if [ "$SKIP_DB" -eq 0 ]; then
        setup_pg11_paths

        for bin in pg_basebackup pg_ctl psql; do
            if [ ! -x "$PG11_BIN/$bin" ]; then
                fail "Не найден $PG11_BIN/$bin"
                missing=1
            fi
        done

        if [ ! -f "$HOME/.pgpass" ]; then
            fail "Не найден ~/.pgpass — добавьте строку для пользователя $DB_USER:"
            info "  ${DB_SERVER}:5432:*:${DB_USER}:<пароль>"
            info "  chmod 600 ~/.pgpass"
            missing=1
        elif [ "$(stat -f '%OLp' "$HOME/.pgpass" 2>/dev/null || stat -c '%a' "$HOME/.pgpass" 2>/dev/null)" != "600" ]; then
            fail "Неверные права на ~/.pgpass (нужно 600): chmod 600 ~/.pgpass"
            missing=1
        fi
    fi

    if [ "$SKIP_FILES" -eq 0 ]; then
        if ! command -v rsync >/dev/null 2>&1; then
            fail "Не найден rsync — нужен для fileStorage."
            missing=1
        fi
    fi

    if [ "$needs_remote" -eq 1 ]; then
        if ! command -v ssh >/dev/null 2>&1; then
            fail "Не найден ssh — нужен для диагностики, WAR и rsync."
            missing=1
        fi
    fi

    if ! command -v gzip >/dev/null 2>&1; then
        fail "Не найден gzip — нужен для архивации."
        missing=1
    fi

    if [ "$SKIP_DB" -eq 0 ]; then
        info "Проверка клиента: $("$PG_BASEBACKUP" --version)"
        info "Режим SSL: PGSSLMODE=$PGSSLMODE"
    fi
    if [ "$needs_pv" -eq 1 ]; then
        info "Проверка pv: $(pv --version 2>/dev/null | head -1 || echo pv)"
    fi

    if [ "$SKIP_DB" -eq 0 ] && [ "$needs_remote" -eq 1 ]; then
        info "Запрос конфигурации репликации с сервера через SSH..."
        ssh "${SSH_USER}@${DB_SERVER}" \
            "su - postgres -c \"psql -d postgres -c 'SHOW max_wal_senders; SHOW wal_keep_segments; SHOW max_replication_slots;'\"" \
            >>"$LOG" 2>&1 || true

        info_n "Проверка соединения с $DB_SERVER ... "
        if ! "$PSQL" -h "$DB_SERVER" -U "$DB_USER" -d postgres -tAc "SELECT version();" >>"$LOG" 2>&1; then
            echo
            fail "Не удалось подключиться к $DB_SERVER."
            diagnose_psql_failure
            missing=1
        else
            ok
            {
                echo "--- remote replication settings via libpq $(date) ---"
                "$PSQL" -h "$DB_SERVER" -U "$DB_USER" -d postgres \
                    -c "SHOW max_wal_senders; SHOW wal_keep_segments; SHOW max_replication_slots;"
                "$PSQL" -h "$DB_SERVER" -U "$DB_USER" -d postgres \
                    -c "SHOW wal_keep_size;" 2>&1 || true
            } >>"$LOG" 2>&1 || true
        fi
    fi

    if [ "$missing" -ne 0 ]; then
        die "Предварительные проверки не пройдены."
    fi
    log_action "SUCCESS" "Этап завершен успешно: предварительные проверки"
}

validate_pgdata_path() {
    if [ -z "${PGDATA:-}" ]; then
        die "PGDATA не задан — отмена деструктивных операций."
    fi
    if [ ! -d "$PGDATA" ]; then
        die "Каталог PGDATA не существует: $PGDATA"
    fi
}

is_ready_for_pgctl_start() {
    [ -f "$1/PG_VERSION" ] || [ -f "$1/postgresql.conf" ]
}

is_complete_pgdata() {
    [ -f "$1/PG_VERSION" ] && [ -d "$1/base" ] && [ -n "$(ls -A "$1/base" 2>/dev/null || true)" ]
}

is_incomplete_pgdata() {
    if [ ! -d "$1" ]; then
        return 1
    fi
    if [ -z "$(ls -A "$1" 2>/dev/null || true)" ]; then
        return 1
    fi
    if is_complete_pgdata "$1"; then
        return 1
    fi
    return 0
}

get_pgdata_port() {
    local port=""
    if [ -f "${PGDATA}/postgresql.conf" ]; then
        port=$(grep -E '^[[:space:]]*port[[:space:]]*=' "${PGDATA}/postgresql.conf" \
            | tail -1 | sed -E 's/^[[:space:]]*port[[:space:]]*=[[:space:]]*//;s/[[:space:]]*$//')
    fi
    echo "${port:-5432}"
}

is_postgres_running_on_pgdata() {
    "$PG_CTL" status -D "$PGDATA" >>"$LOG" 2>&1
}

is_port_listening() {
    local port="$1"
    if command -v lsof >/dev/null 2>&1; then
        lsof -nP -iTCP:"$port" -sTCP:LISTEN >>"$LOG" 2>&1
        return $?
    fi
    if command -v nc >/dev/null 2>&1; then
        nc -z localhost "$port" >>"$LOG" 2>&1
        return $?
    fi
    return 1
}

prepare_temp_directory() {
    log_action "INFO" "Начало этапа: подготовка временного каталога backup"
    info_stage "=== Подготовка временного каталога backup ==="
    if [ "${SKIP_DOWNLOAD:-0}" -eq 1 ]; then
        if is_complete_pgdata "$postgre_temp_database"; then
            info "Используется кэшированный backup из $postgre_temp_database (--cached)."
            log_action "SUCCESS" "Этап завершен успешно: подготовка временного каталога backup"
            return
        fi
        die "Флаг --cached: полный backup во временном каталоге не найден: $postgre_temp_database"
    fi

    if [ ! -d "$postgre_temp_database" ]; then
        info_n "Создание временного каталога $postgre_temp_database ... "
        mkdir "$postgre_temp_database"
        ok
        log_action "SUCCESS" "Этап завершен успешно: подготовка временного каталога backup"
        return
    fi

    if is_complete_pgdata "$postgre_temp_database"; then
        info "Временный каталог уже содержит полный base backup."
        read -r -p "Использовать его без повторной загрузки? [y/N]: " reuse
        if [[ "$reuse" =~ ^[YyДд]$ ]]; then
            SKIP_DOWNLOAD=1
            ok
            log_action "SUCCESS" "Этап завершен успешно: подготовка временного каталога backup"
            return
        fi
        info_n "Удаление прежнего backup во временном каталоге ... "
        rm -rf "${postgre_temp_database:?}/"*
        ok
        log_action "SUCCESS" "Этап завершен успешно: подготовка временного каталога backup"
        return
    fi

    if is_incomplete_pgdata "$postgre_temp_database"; then
        info "Временный каталог содержит незавершённую загрузку — будет очищен."
        rm -rf "${postgre_temp_database:?}/"*
        ok
        log_action "SUCCESS" "Этап завершен успешно: подготовка временного каталога backup"
        return
    fi

    info_n "Временный каталог существует (пустой) ... "
    ok
    log_action "SUCCESS" "Этап завершен успешно: подготовка временного каталога backup"
}

# Распаковка вложенных base.tar / pg_wal.tar после pg_basebackup -Ft (формат tar stream).
unpack_pg_basebackup_tar_members() {
    local dir="$1"
    local member_bytes

    if [ -f "$dir/base.tar" ]; then
        member_bytes=$(stat -f%z "$dir/base.tar" 2>/dev/null || stat -c%s "$dir/base.tar" 2>/dev/null || du_bytes "$dir/base.tar")
        info "Распаковка base.tar (pv, ${member_bytes} байт) ..."
        if ! pv_file "$member_bytes" "$dir/base.tar" \
            | tar -xf - -C "$dir" 2>>"$LOG"; then
            die "Не удалось распаковать base.tar (см. $LOG)."
        fi
        rm -f "$dir/base.tar"
    fi

    if [ -f "$dir/pg_wal.tar" ]; then
        member_bytes=$(stat -f%z "$dir/pg_wal.tar" 2>/dev/null || stat -c%s "$dir/pg_wal.tar" 2>/dev/null || du_bytes "$dir/pg_wal.tar")
        info "Распаковка pg_wal.tar (pv, ${member_bytes} байт) ..."
        mkdir -p "$dir/pg_wal"
        if ! pv_file "$member_bytes" "$dir/pg_wal.tar" \
            | tar -xf - -C "$dir/pg_wal" 2>>"$LOG"; then
            die "Не удалось распаковать pg_wal.tar (см. $LOG)."
        fi
        rm -f "$dir/pg_wal.tar"
    fi
}

download_basebackup() {
    log_action "INFO" "Начало этапа: загрузка base backup PostgreSQL"
    info_stage "=== Загрузка base backup PostgreSQL ==="
    info_n "Переход во временный каталог $postgre_temp_database ... "
    cd "$postgre_temp_database"
    ok

    if [ "${SKIP_DOWNLOAD:-0}" -eq 1 ]; then
        info "Пропуск загрузки — используется существующий backup."
        log_action "SUCCESS" "Этап завершен успешно: загрузка base backup PostgreSQL"
        return
    fi

    local est_bytes
    est_bytes=$(get_remote_cluster_size_bytes)

    info_stage "Загрузка base backup с $DB_SERVER (pg_basebackup -Ft, --wal-method=fetch) ..."
    info "Ожидаемый объём потока: ~${est_bytes} байт"
    info "Лог: $LOG"

    # -Ft -D - : внешний tar-поток (base.tar + pg_wal.tar) → pv → tar -xf во временный каталог.
    if ! "$PG_BASEBACKUP" \
        -h "$DB_SERVER" \
        -U "$DB_USER" \
        -Ft \
        -D - \
        --wal-method=fetch \
        --checkpoint=fast \
        --no-slot \
        2>>"$LOG" \
        | pv_stream "$est_bytes" \
        | tar -xf - -C "$postgre_temp_database" 2>>"$LOG"; then
        die "Не удалось загрузить base backup с $DB_SERVER (подробности в $LOG)."
    fi

    unpack_pg_basebackup_tar_members "$postgre_temp_database"

    if ! is_complete_pgdata "$postgre_temp_database"; then
        die "Загрузка завершилась, но во временном каталоге нет полного кластера (PG_VERSION/base)."
    fi

    ok
    log_action "SUCCESS" "Этап завершен успешно: загрузка base backup PostgreSQL"
}

strip_standby_files() {
    local target="$1"
    local removed=0
    for f in recovery.conf recovery.done standby.signal postmaster.pid; do
        if [ -e "$target/$f" ]; then
            rm -f "$target/$f"
            removed=1
        fi
    done
    if [ -f "$target/postgresql.auto.conf" ] && grep -q "primary_conninfo" "$target/postgresql.auto.conf" 2>/dev/null; then
        sed -i.bak '/primary_conninfo/d' "$target/postgresql.auto.conf"
        rm -f "$target/postgresql.auto.conf.bak"
        removed=1
    fi
    if [ "$removed" -eq 1 ]; then
        info "Удалены файлы standby/recovery — локальная БД будет в режиме primary (не read-only)."
    fi
}

install_to_local_pgdata() {
    log_action "INFO" "Начало этапа: установка backup в локальный PGDATA"
    info_stage "=== Установка backup в локальный PGDATA ==="
    validate_pgdata_path

    if ! is_complete_pgdata "$postgre_temp_database"; then
        die "Временный каталог не содержит полный backup — установка в PGDATA отменена."
    fi

    info_n "Переход в каталог данных $PGDATA ... "
    cd "$PGDATA"
    ok

    info_n "Остановка локальной PostgreSQL ... "
    local stop_rc=0
    if ! "$PG_CTL" stop -D . -m fast >>"$LOG" 2>&1; then
        stop_rc=1
    fi

    if is_postgres_running_on_pgdata; then
        die "Не удалось остановить локальный PostgreSQL. Очистка отменена."
    elif [ "$stop_rc" -ne 0 ]; then
        info "не запущена (продолжаем)"
    else
        ok
    fi

    local pg_port
    pg_port=$(get_pgdata_port)
    if is_port_listening "$pg_port"; then
        die "Порт $pg_port занят — очистка $PGDATA отменена (postgres, возможно, ещё держит файлы)."
    fi

    info_n "Очистка каталога данных ... "
    rm -rf "${PGDATA:?}"/*
    ok

    local copy_bytes
    copy_bytes=$(du_bytes "$postgre_temp_database")
    info_stage "Копирование backup из $postgre_temp_database в $PGDATA (~${copy_bytes} байт) ..."

    if ! tar -cf - -C "$postgre_temp_database" . 2>>"$LOG" \
        | pv_stream "$copy_bytes" \
        | tar -xf - -C "$PGDATA" 2>>"$LOG"; then
        die "Не удалось скопировать backup в $PGDATA (см. $LOG)."
    fi

    if ! is_complete_pgdata "$PGDATA"; then
        die "После копирования $PGDATA не содержит полный кластер — pg_ctl не запускается."
    fi
    ok

    strip_standby_files "$PGDATA"

    info_n "Удаление временного каталога ... "
    rm -rf "$postgre_temp_database"
    ok
    log_action "SUCCESS" "Этап завершен успешно: установка backup в локальный PGDATA"
}

sync_file_storage() {
    log_action "INFO" "Начало этапа: синхронизация fileStorage"
    info_stage "=== Синхронизация fileStorage (rsync pull) ==="
    mkdir -p "$FILE_STORAGE_LOCAL"
    if [ "$QUIET_MODE" -eq 1 ]; then
        if ! rsync -a --ignore-existing \
            "${SSH_USER}@${DB_SERVER}:${FILE_STORAGE_REMOTE}" "${FILE_STORAGE_LOCAL}/" \
            >>"$LOG" 2>&1; then
            die "Не удалось скопировать fileStorage (нужен SSH-доступ ${SSH_USER}@${DB_SERVER})."
        fi
    else
        if ! rsync -a --info=progress2 --ignore-existing \
            "${SSH_USER}@${DB_SERVER}:${FILE_STORAGE_REMOTE}" "${FILE_STORAGE_LOCAL}/" \
            2>&1 | tee -a "$LOG"; then
            die "Не удалось скопировать fileStorage (нужен SSH-доступ ${SSH_USER}@${DB_SERVER})."
        fi
    fi
    ok
    log_action "SUCCESS" "Этап завершен успешно: синхронизация fileStorage"
}

download_tomcat_wars() {
    log_action "INFO" "Начало этапа: загрузка Tomcat WAR"
    info_stage "=== Загрузка Tomcat WAR ==="
    local war_bytes
    war_bytes=$(get_tomcat_wars_size_bytes)

    mkdir -p "$TOMCAT_WARS_LOCAL"

    if [ "${war_bytes:-0}" -le 0 ]; then
        info "На сервере не найдены *.war или размер 0 — пропуск загрузки Tomcat WAR."
        log_action "SUCCESS" "Этап завершен успешно: загрузка Tomcat WAR (пропуск, нет файлов)"
        return 0
    fi

    info_stage "Загрузка WAR с ${SSH_USER}@${DB_SERVER}:${TOMCAT_WARS_DIR} ..."
    info "Суммарный размер WAR на сервере: ${war_bytes} байт"

    # Только чтение на сервере: tar -cf с wildcard *.war
    if ! ssh "${SSH_USER}@${DB_SERVER}" \
        "cd '${TOMCAT_WARS_DIR}' && tar -cf - --wildcards '*.war'" \
        2>>"$LOG" \
        | pv_stream "$war_bytes" \
        | tar -xf - -C "$TOMCAT_WARS_LOCAL" 2>>"$LOG"; then
        die "Не удалось загрузить WAR-файлы Tomcat (нужен SSH-доступ ${SSH_USER}@${DB_SERVER})."
    fi
    ok
    log_action "SUCCESS" "Этап завершен успешно: загрузка Tomcat WAR"
}

# Удаление временного staging-каталога архивации (игнорирует ошибки rm).
remove_archive_staging() {
    local dir="${1:-}"
    if [ -n "$dir" ] && [ -d "$dir" ]; then
        rm -rf "$dir" 2>>"$LOG" || true
    fi
}

archive_and_start() {
    log_action "INFO" "Начало этапа: архивация и запуск PostgreSQL"
    info_stage "=== Архивация и запуск PostgreSQL ==="
    local db_source="" db_parent="" db_name=""
    local start_pg=0
    local total_bytes=0
    local has_wars=0 has_db=0 staging=""
    local wars_name

    wars_name=$(basename "$TOMCAT_WARS_LOCAL")

    if [ "$BACKUP_ONLY" -eq 1 ]; then
        if is_complete_pgdata "$postgre_temp_database"; then
            db_source="$postgre_temp_database"
            db_parent=$(dirname "$postgre_temp_database")
            db_name=$(basename "$postgre_temp_database")
            has_db=1
        elif [ "$SKIP_DB" -eq 0 ]; then
            die "Режим --backup-only: во временном каталоге нет полного backup для архивации."
        fi
    elif [ "$SKIP_DB" -eq 0 ]; then
        validate_pgdata_path
        if ! is_ready_for_pgctl_start "$PGDATA"; then
            die "В $PGDATA нет PG_VERSION и postgresql.conf — архивация отменена."
        fi
        if ! is_complete_pgdata "$PGDATA"; then
            die "В $PGDATA неполный кластер (нет PG_VERSION/base) — архивация отменена."
        fi
        db_source="$PGDATA"
        db_parent=$(dirname "$PGDATA")
        db_name=$(basename "$PGDATA")
        has_db=1
        start_pg=1
    fi

    if [ -d "$TOMCAT_WARS_LOCAL" ] && [ -n "$(ls -A "$TOMCAT_WARS_LOCAL" 2>/dev/null || true)" ]; then
        has_wars=1
    fi

    if [ "$has_db" -eq 0 ] && [ "$has_wars" -eq 0 ]; then
        if [ "$SKIP_DB" -eq 1 ] && [ "$SKIP_WARS" -eq 1 ]; then
            info "Пропуск архивации: --skip-db и --skip-wars, нечего упаковывать (fileStorage хранится отдельно в $FILE_STORAGE_LOCAL)."
            log_action "SUCCESS" "Этап завершен успешно: архивация (пропуск, нечего упаковывать)"
            return 0
        fi
        die "Нет данных для архивации (ни БД, ни tomcat_wars)."
    fi

    info "Архивация прежней копии данных ..."
    rm -f "$old_archive" 2>>"$LOG" || true
    if [ -f "$new_archive" ]; then
        mv "$new_archive" "$old_archive"
    fi

    if [ "$has_db" -eq 1 ]; then
        total_bytes=$(du_bytes "$db_source")
    fi
    if [ "$has_wars" -eq 1 ]; then
        total_bytes=$(( total_bytes + $(du_bytes "$TOMCAT_WARS_LOCAL") ))
    fi

    if [ "$has_db" -eq 1 ] && [ "$BACKUP_ONLY" -eq 1 ]; then
        info_stage "Создание архива $new_archive (backup-only: временный каталог + WAR, ~${total_bytes} байт) ..."
    elif [ "$has_db" -eq 1 ]; then
        info_stage "Создание архива $new_archive (PGDATA + WAR, ~${total_bytes} байт) ..."
    else
        info_stage "Создание архива $new_archive (только tomcat_wars, ~${total_bytes} байт) ..."
    fi

    if [ "$has_db" -eq 1 ] && [ "$has_wars" -eq 1 ]; then
        staging=$(mktemp -d "${current_catalog}/.archive-staging.XXXXXX") \
            || die "Не удалось создать временный каталог архивации."

        if ! ln -s "$db_source" "$staging/$db_name" 2>>"$LOG"; then
            remove_archive_staging "$staging"
            die "Не удалось подготовить staging для данных БД (см. $LOG)."
        fi
        if ! ln -s "$TOMCAT_WARS_LOCAL" "$staging/$wars_name" 2>>"$LOG"; then
            remove_archive_staging "$staging"
            die "Не удалось подготовить staging для tomcat_wars (см. $LOG)."
        fi

        if ! tar -chf - -C "$staging" . 2>>"$LOG" \
            | pv_stream "$total_bytes" \
            | gzip -c >"$new_archive"; then
            remove_archive_staging "$staging"
            die "Не удалось создать архив $new_archive (см. $LOG)."
        fi
        remove_archive_staging "$staging"
        staging=""
    elif [ "$has_db" -eq 1 ]; then
        info "Каталог tomcat_wars пуст или отсутствует — в архив попадут только данные БД."
        if ! tar -cf - -C "$db_parent" "$db_name" 2>>"$LOG" \
            | pv_stream "$total_bytes" \
            | gzip -c >"$new_archive"; then
            die "Не удалось создать архив $new_archive (см. $LOG)."
        fi
    else
        if ! tar -cf - -C "$(dirname "$TOMCAT_WARS_LOCAL")" "$wars_name" 2>>"$LOG" \
            | pv_stream "$total_bytes" \
            | gzip -c >"$new_archive"; then
            die "Не удалось создать архив $new_archive (см. $LOG)."
        fi
    fi

    if [ -n "$staging" ]; then
        remove_archive_staging "$staging"
    fi

    if [ ! -s "$new_archive" ]; then
        die "Архив $new_archive пуст или не создан."
    fi
    ok

    if [ "$start_pg" -eq 0 ]; then
        NO_START=1
        if [ "$BACKUP_ONLY" -eq 1 ]; then
            info "Режим --backup-only: локальная PostgreSQL не запускается."
        elif [ "$SKIP_DB" -eq 1 ]; then
            info "Флаг --skip-db: локальная PostgreSQL не запускается."
        fi
        log_action "SUCCESS" "Этап завершен успешно: архивация и запуск PostgreSQL"
        return 0
    fi

    setup_pg11_paths
    cd "$PGDATA"

    info_stage "Запуск локальной PostgreSQL ..."
    info_n "Запуск PostgreSQL ... "
    if ! "$PG_CTL" start -D . >>"$LOG" 2>&1; then
        die "Не удалось запустить PostgreSQL (см. $LOG)."
    fi
    ok

    info_n "Проверка режима (pg_is_in_recovery) ... "
    local in_recovery
    in_recovery=$("$PSQL" -h localhost -U postgres -d postgres -tAc "SELECT pg_is_in_recovery();" 2>>"$LOG" || echo "error")
    if [ "$in_recovery" = "t" ]; then
        fail
        die "Кластер в режиме recovery (read-only). Удалите recovery.conf/standby.signal и перезапустите."
    elif [ "$in_recovery" = "f" ]; then
        ok
    else
        info "не удалось проверить (возможно, нет роли postgres локально)"
    fi
    log_action "SUCCESS" "Этап завершен успешно: архивация и запуск PostgreSQL"
}

# --- main ---
load_or_create_config
parse_args "$@"

if [ "$CHECK_CONFIG" -eq 1 ]; then
    : >"$LOG"
    log_action "INFO" "Запуск pull-prod.sh --check-config"
    check_config_and_report
    exit $?
fi

resolve_archive_paths
new_archive="${ARCHIVE_DIR}/hunttech-basebackup.tgz"
old_archive="${ARCHIVE_DIR}/hunttech-basebackup-old.tgz"

: >"$LOG"
log_action "INFO" "Запуск скрипта pull-prod.sh"

banner
if [ "$DB_SERVER" = "$DEFAULT_DB_SERVER" ]; then
    info "Сервер: ${GREEN}${DB_SERVER}${NC} (значение по умолчанию)"
else
    info "Сервер: ${GREEN}${DB_SERVER}${NC} (hunttech.conf или CLI)"
fi
info "Конфиг: $CONFIG_FILE"
info_stage "Каталог архивов: ${ARCHIVE_DIR}"
if [ "$SKIP_DB" -eq 1 ]; then info "Флаг: --skip-db"; fi
if [ "$SKIP_FILES" -eq 1 ]; then info "Флаг: --skip-files"; fi
if [ "$SKIP_WARS" -eq 1 ]; then info "Флаг: --skip-wars"; fi
if [ "$SKIP_DOWNLOAD" -eq 1 ]; then info "Флаг: --cached"; fi
if [ "$BACKUP_ONLY" -eq 1 ]; then info "Флаг: --backup-only"; fi
if [ "$QUIET_MODE" -eq 1 ]; then info_stage "Флаг: --quiet (краткий вывод)"; fi

preflight_checks
if [ "$SKIP_DB" -ne 1 ]; then
    prepare_temp_directory
    download_basebackup
fi
if [ "$BACKUP_ONLY" -ne 1 ] && [ "$SKIP_DB" -ne 1 ]; then
    install_to_local_pgdata
fi
if [ "$SKIP_FILES" -ne 1 ]; then sync_file_storage; fi
if [ "$SKIP_WARS" -ne 1 ]; then download_tomcat_wars; fi
archive_and_start

log_action "SUCCESS" "Скрипт pull-prod.sh завершен успешно"

if [ "$BACKUP_ONLY" -eq 1 ]; then
    info_stage "${GREEN}Готово (backup-only). Локальный PGDATA не изменён, PostgreSQL не запускалась.${NC}"
    info "Временный backup: $postgre_temp_database"
elif [ "$SKIP_DB" -eq 0 ]; then
    info_stage "${GREEN}Готово. Локальная PostgreSQL: $PGDATA${NC}"
else
    info_stage "${GREEN}Готово (без операций с БД).${NC}"
fi
info "Архив: $new_archive"
info "Tomcat WAR: $TOMCAT_WARS_LOCAL"
if [ "$SKIP_FILES" -eq 0 ]; then
    info "FileStorage: $FILE_STORAGE_LOCAL/fileStorage"
fi
if [ "${NO_START:-0}" -ne 1 ]; then
    info "Проверка: ./start-postgres11.sh status"
fi
