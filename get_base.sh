#!/bin/bash
# Загрузка base backup с продакшена в локальный PostgreSQL 11 (Homebrew).
set -euo pipefail

# --- пути PostgreSQL 11 (Homebrew: Intel / Apple Silicon) ---
if [ -d "/opt/homebrew/opt/postgresql@11/bin" ]; then
    PG11_BIN="/opt/homebrew/opt/postgresql@11/bin"
    PGDATA="/opt/homebrew/var/postgresql@11"
elif [ -d "/usr/local/opt/postgresql@11/bin" ]; then
    PG11_BIN="/usr/local/opt/postgresql@11/bin"
    PGDATA="/usr/local/var/postgresql@11"
else
    echo -e "\033[31mОшибка: не найден PostgreSQL 11 (brew install postgresql@11).\033[0m"
    exit 1
fi

export PATH="$PG11_BIN:$PATH"
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8

# Обход segfault libpq 11.x на macOS при SSL-рукопожатии с удалённым сервером.
# prefer/require/allow падают с «(null)» и Segmentation fault: 11; disable — стабильно.
export PGSSLMODE="${PGSSLMODE:-disable}"

CWD=$(pwd)
current_catalog="$CWD"
postgre_temp_database="${current_catalog}/postgre_tmp_database"
ARCHIVE_DIR="${current_catalog}/.."
BACKUPBASELOG=backupbase.log
LOG="${CWD}/${BACKUPBASELOG}"
db_server=hr.hunttech.ru
db_user=replica
REMOTE_FILE_STORAGE_DIR="${REMOTE_FILE_STORAGE_DIR:-/opt/app_home/fileStorage}"
LOCAL_FILE_STORAGE_DIR="${LOCAL_FILE_STORAGE_DIR:-${current_catalog}/fileStorage}"
LOCAL_APP_PROPERTIES="${LOCAL_APP_PROPERTIES:-${current_catalog}/local.app.properties}"

PG_BASEBACKUP="$PG11_BIN/pg_basebackup"
PG_CTL="$PG11_BIN/pg_ctl"
PSQL="$PG11_BIN/psql"

RED='\033[31m'
GREEN='\033[32m'
WHITE='\033[37m'
NC='\033[0m'

info()  { echo -e "${WHITE}$*${NC}"; }
info_n() { printf "${WHITE}%s${NC}" "$*"; }
ok()    { echo -e "${GREEN}OK${NC}"; }
fail()  { echo -e "${RED}$*${NC}"; }

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
    fail "$1"
    echo FAIL
    exit "${2:-1}"
}

banner() {
    echo "*******************************************************"
    echo "*******************************************************"
    echo "**                                                   **"
    echo "**                   HuntTech                        **"
    echo "**       Загрузка базы из основной площадки          **"
    echo "**                                                   **"
    echo "*******************************************************"
    echo "*******************************************************"
}

# Лог WAL/репликации на удалённом сервере (диагностика перед basebackup).
log_remote_replication_settings() {
    info "Запрос конфигурации репликации с сервера..."
    {
        echo "--- remote replication settings $(date) ---"
        "$PSQL" -h "$db_server" -U "$db_user" -d postgres \
            -c "SHOW max_wal_senders; SHOW wal_keep_segments; SHOW max_replication_slots;"
        # PG14+: wal_keep_size (на PG11 сервере команда может вернуть ошибку — это нормально)
        "$PSQL" -h "$db_server" -U "$db_user" -d postgres \
            -c "SHOW wal_keep_size;" 2>&1 || true
    } >>"$LOG" 2>&1 || true
}

show_log_tail() {
    local n="${1:-15}"
    if [ -f "$LOG" ] && [ -s "$LOG" ]; then
        fail "Последние строки из $LOG:"
        tail -n "$n" "$LOG" | sed 's/^/  /'
    fi
}

check_remote_port() {
    info_n "Проверка доступности порта 5432 на $db_server ... "
    if command -v nc >/dev/null 2>&1; then
        if nc -z -w 5 "$db_server" 5432 >>"$LOG" 2>&1; then
            ok
            return 0
        fi
        echo
        fail "Порт 5432 на $db_server недоступен (nc -z)."
        return 1
    fi
    if (echo >/dev/tcp/"$db_server"/5432) 2>/dev/null; then
        ok
        return 0
    fi
    echo
    fail "Порт 5432 на $db_server недоступен (/dev/tcp)."
    return 1
}

diagnose_psql_failure() {
    local psql_output
    psql_output=$(tail -n 30 "$LOG" 2>/dev/null || true)

    show_log_tail 15
    check_remote_port || true

    if echo "$psql_output" | grep -qiE 'password authentication failed|authentication failed|FATAL:.*password'; then
        fail "Диагноз: ошибка аутентификации — проверьте пароль в ~/.pgpass для $db_user@$db_server."
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

# --- проверки перед стартом ---
preflight_checks() {
    local missing=0

    for bin in pg_basebackup pg_ctl psql; do
        if [ ! -x "$PG11_BIN/$bin" ]; then
            fail "Не найден $PG11_BIN/$bin"
            missing=1
        fi
    done

    if ! command -v rsync >/dev/null 2>&1; then
        fail "Не найден rsync — он нужен для копирования fileStorage."
        missing=1
    fi

    if [ ! -f "$HOME/.pgpass" ]; then
        fail "Не найден ~/.pgpass — добавьте строку для пользователя replica:"
        info "  hr.hunttech.ru:5432:*:replica:<пароль>"
        info "  chmod 600 ~/.pgpass"
        missing=1
    elif [ "$(stat -f '%OLp' "$HOME/.pgpass" 2>/dev/null || stat -c '%a' "$HOME/.pgpass" 2>/dev/null)" != "600" ]; then
        fail "Неверные права на ~/.pgpass (нужно 600): chmod 600 ~/.pgpass"
        missing=1
    fi

    info "Проверка клиента: $("$PG_BASEBACKUP" --version)"
    info "Режим SSL: PGSSLMODE=$PGSSLMODE"
    info "Локальный fileStorage: $LOCAL_FILE_STORAGE_DIR"

    info_n "Проверка соединения с $db_server ... "
    if ! "$PSQL" -h "$db_server" -U "$db_user" -d postgres -tAc "SELECT version();" >>"$LOG" 2>&1; then
        echo
        fail "Не удалось подключиться к $db_server."
        diagnose_psql_failure
        log_remote_replication_settings
        missing=1
    else
        ok
        log_remote_replication_settings
    fi

    if [ "$missing" -ne 0 ]; then
        die "Предварительные проверки не пройдены."
    fi
}

# PGDATA задан и существует как каталог (перед rm/cp/pg_ctl)
validate_pgdata_path() {
    if [ -z "${PGDATA:-}" ]; then
        die "PGDATA не задан — отмена деструктивных операций."
    fi
    if [ ! -d "$PGDATA" ]; then
        die "Каталог PGDATA не существует: $PGDATA"
    fi
}

# Минимум для pg_ctl start: PG_VERSION или postgresql.conf
is_ready_for_pgctl_start() {
    [ -f "$1/PG_VERSION" ] || [ -f "$1/postgresql.conf" ]
}

# Полный backup: PG_VERSION + каталог base/
is_complete_pgdata() {
    [ -f "$1/PG_VERSION" ] && [ -d "$1/base" ] && [ -n "$(ls -A "$1/base" 2>/dev/null || true)" ]
}

# Неполная загрузка: что-то есть, но кластер не готов
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

# Порт из postgresql.conf (по умолчанию 5432).
get_pgdata_port() {
    local port=""
    if [ -f "${PGDATA}/postgresql.conf" ]; then
        port=$(grep -E '^[[:space:]]*port[[:space:]]*=' "${PGDATA}/postgresql.conf" \
            | tail -1 | sed -E 's/^[[:space:]]*port[[:space:]]*=[[:space:]]*//;s/[[:space:]]*$//')
    fi
    echo "${port:-5432}"
}

# pg_ctl status: 0 — сервер запущен для данного PGDATA.
is_postgres_running_on_pgdata() {
    "$PG_CTL" status -D "$PGDATA" >>"$LOG" 2>&1
}

# Слушает ли кто-то указанный порт (LISTEN).
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
    if [ ! -d "$postgre_temp_database" ]; then
        info_n "Создание временного каталога $postgre_temp_database ... "
        mkdir "$postgre_temp_database"
        ok
        return
    fi

    if is_complete_pgdata "$postgre_temp_database"; then
        info "Временный каталог уже содержит полный base backup."
        read -r -p "Использовать его без повторной загрузки? [y/N]: " reuse || true
        if [[ "$reuse" =~ ^[YyДд]$ ]]; then
            SKIP_DOWNLOAD=1
            ok
            return
        fi
        info_n "Удаление прежнего backup во временном каталоге ... "
        rm -rf "${postgre_temp_database:?}/"*
        ok
        return
    fi

    if is_incomplete_pgdata "$postgre_temp_database"; then
        info "Временный каталог содержит незавершённую загрузку — будет очищен."
        rm -rf "${postgre_temp_database:?}/"*
        ok
        return
    fi

    info_n "Временный каталог существует (пустой) ... "
    ok
}

# Форматирование прогресса pg_basebackup в [====> ] с таймером и ETA.
format_pg_progress() {
    local bar_width=40
    local pct total current filled elapsed eta i
    local bar=""
    local last_pct=-1
    local start_sec=$SECONDS
    while IFS= read -r line; do
        if [[ "$line" =~ ^[[:space:]]*([0-9]+)/([0-9]+)[[:space:]]*kB[[:space:]]*\(([0-9]+)%\) ]]; then
            current="${BASH_REMATCH[1]}"
            total="${BASH_REMATCH[2]}"
            pct="${BASH_REMATCH[3]}"
            elapsed=$(( SECONDS - start_sec ))
            eta=0
            if [ "$pct" -gt 0 ] && [ "$pct" -lt 100 ]; then
                eta=$(( elapsed * 100 / pct - elapsed ))
            fi
            filled=$(( pct * bar_width / 100 ))
            bar="["
            for ((i=0; i<bar_width; i++)); do
                if   [ "$i" -lt "$filled" ]; then bar+="="
                elif [ "$i" -eq "$filled" ] && [ "$pct" -lt 100 ]; then bar+=">"
                else bar+=" "; fi
            done
            bar+="]"
            printf "\r%s %3d%% | %s/%s kB | прошло: %02d:%02d | ETA: %02d:%02d" \
                "$bar" "$pct" "$current" "$total" \
                $((elapsed/60)) $((elapsed%60)) \
                $((eta/60)) $((eta%60))
            last_pct=$pct
        else
            [ "$last_pct" -ge 0 ] && printf "\n"
            echo "$line"
            last_pct=-1
        fi
    done
    [ "$last_pct" -ge 0 ] && printf "\n"
}

download_basebackup() {
    info_n "Переход во временный каталог $postgre_temp_database ... "
    cd "$postgre_temp_database"
    ok

    if [ "${SKIP_DOWNLOAD:-0}" -eq 1 ]; then
        info "Пропуск загрузки — используется существующий backup."
        return
    fi

    log_remote_replication_settings

    info "Загрузка base backup с $db_server (pg_basebackup, --wal-method=fetch) ..."
    echo -e "${WHITE}Прогресс:${NC}"

    # fetch вместо stream (-X stream): стабильнее через сеть, без долгого WAL-streaming
    set +e
    "$PG_BASEBACKUP" \
        -P \
        -h "$db_server" \
        -D . \
        -U "$db_user" \
        --wal-method=fetch \
        --checkpoint=fast \
        --no-slot \
        2>&1 | tee -a "$LOG" | format_pg_progress
    RC=${PIPESTATUS[0]}
    set -e
    if [ "$RC" -ne 0 ]; then
        die "Не удалось загрузить base backup с $db_server (подробности в $LOG)."
    fi

    if ! is_complete_pgdata .; then
        die "Загрузка завершилась, но во временном каталоге нет полного кластера (PG_VERSION/base)."
    fi

    ok
}

# Убрать файлы standby/recovery — иначе локальный кластер стартует read-only.
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
    validate_pgdata_path

    if ! is_complete_pgdata "$postgre_temp_database"; then
        die "Временный каталог не содержит полный backup — установка в PGDATA отменена."
    fi

    info_n "Переход в каталог данных $PGDATA ... "
    cd "$PGDATA"
    ok

    info_n "Остановка локальной PostgreSQL ... "

    # Останавливаем launchd-сервис (если есть), иначе launchd перезапускает postgres
    local launchd_service=""
    if launchctl list 2>/dev/null | grep -qi "postgres"; then
        launchd_service=$(launchctl list 2>/dev/null | grep -i "postgres" | head -1 | awk '{print $3}')
        if [ -n "$launchd_service" ]; then
            info "сервис $launchd_service → bootout..."
            launchctl bootout "gui/$(id -u)/${launchd_service}" 2>>"$LOG" || true
            sleep 1
        fi
    fi

    # Читаем PID процесса из postmaster.pid (если файл существует)
    local old_pid=""
    if [ -f "postmaster.pid" ]; then
        old_pid=$(head -1 "postmaster.pid" 2>/dev/null || true)
    fi

    # Штатная остановка через pg_ctl (может не быть запущенного сервера — не ошибка)
    "$PG_CTL" stop -D . -m fast >>"$LOG" 2>&1 || true
    sleep 1

    # pg_ctl stop не всегда убивает процесс (баг libpq на macOS) —
    # проверяем напрямую через PID из postmaster.pid
    if [ -n "$old_pid" ] && kill -0 "$old_pid" 2>/dev/null; then
        info "жив (PID $old_pid) → immediate shutdown..."
        "$PG_CTL" stop -D . -m immediate >>"$LOG" 2>&1 || true
        sleep 1
    fi

    if [ -n "$old_pid" ] && kill -0 "$old_pid" 2>/dev/null; then
        info "не реагирует → SIGKILL (PID $old_pid)..."
        kill -9 "$old_pid" 2>/dev/null || true
        sleep 1
    fi

    # Добиваем любые оставшиеся postgres-процессы с этим PGDATA
    local stray_pids
    stray_pids=$(ps auxww 2>/dev/null | grep "[p]ostgres.*-D.*${PGDATA}" | awk '{print $2}' || true)
    if [ -n "$stray_pids" ]; then
        info "добиваем stray: $stray_pids"
        for p in $stray_pids; do
            kill -9 "$p" 2>/dev/null || true
        done
        sleep 1
    fi

    ok

    local pg_port
    pg_port=$(get_pgdata_port)
    if is_port_listening "$pg_port"; then
        die "Порт $pg_port занят — очистка $PGDATA отменена (postgres, возможно, ещё держит файлы)."
    fi

    info_n "Очистка каталога данных ... "
    rm -rf "${PGDATA:?}"/*
    ok

    info "Копирование backup из $postgre_temp_database в $PGDATA ..."
    if command -v pv >/dev/null 2>&1; then
        copy_size=$(($(du -sk "$postgre_temp_database" 2>/dev/null | cut -f1) * 1024))
        if ! tar -cf - -C "$postgre_temp_database" . 2>>"$LOG" \
            | pv -p --timer --rate --bytes -s "${copy_size:-0}" \
            | tar -xf - -C "$PGDATA" 2>>"$LOG"; then
            die "Не удалось скопировать backup в $PGDATA (см. $LOG)."
        fi
    else
        if ! cp -a "$postgre_temp_database"/. "$PGDATA"/ >>"$LOG" 2>&1; then
            die "Не удалось скопировать backup в $PGDATA (см. $LOG)."
        fi
    fi

    if ! is_complete_pgdata "$PGDATA"; then
        die "После копирования $PGDATA не содержит полный кластер — pg_ctl не запускается."
    fi
    ok

    strip_standby_files "$PGDATA"

    info_n "Удаление временного каталога ... "
    rm -rf "$postgre_temp_database"
    ok
}

# Архивация свежего backup после установки в PGDATA (перед запуском PostgreSQL).
archive_fresh_backup() {
    validate_pgdata_path
    cd "$PGDATA"

    local date_stamp
    date_stamp=$(date +%Y-%m-%d)
    local archive_name="${date_stamp} HUNTTECH DataBase.tgz"
    local archive_path="${ARCHIVE_DIR}/${archive_name}"

    if [ -f "$archive_path" ]; then
        info "Архив ${archive_name} уже существует — пропуск."
        return
    fi

    info "Архивация свежей базы: ${archive_name} ..."
    if command -v pv >/dev/null 2>&1; then
        local arch_size
        arch_size=$(($(du -sk . 2>/dev/null | cut -f1) * 1024))
        if ! tar -cf - . 2>>"$LOG" \
            | pv -p --timer --rate --bytes -s "${arch_size:-0}" \
            | gzip -c >"$archive_path"; then
            die "Не удалось создать архив $archive_path (см. $LOG)."
        fi
    else
        if ! tar -czf "$archive_path" . >>"$LOG" 2>&1; then
            die "Не удалось создать архив $archive_path (см. $LOG)."
        fi
    fi
    if [ ! -s "$archive_path" ]; then
        die "Архив $archive_path пуст или не создан."
    fi
    ok

    # Ротация: оставить 3 последних архива
    local archives=()
    while IFS= read -r a; do
        archives+=("$a")
    done < <(find "$ARCHIVE_DIR" -maxdepth 1 -name '????-??-?? HUNTTECH DataBase.tgz' 2>/dev/null | sort -r)

    local count=${#archives[@]}
    if [ "$count" -le 3 ]; then
        return
    fi

    info "Найдено архивов: $count (храним 3 последних)"
    local to_delete=("${archives[@]:3}")

    echo -e "${RED}Следующие старые архивы (${#to_delete[@]} шт.) могут быть удалены:${NC}"
    for a in "${to_delete[@]}"; do
        echo "  - $(basename "$a")"
    done

    read -r -p "Удалить эти старые архивы? [y/N]: " cleanup_old || true
    if [[ "$cleanup_old" =~ ^[YyДд]$ ]]; then
        for a in "${to_delete[@]}"; do
            rm -f "$a"
            info "  Удалён: $(basename "$a")"
        done
        ok
    else
        info "Старые архивы сохранены."
    fi
}

archive_and_start() {
    validate_pgdata_path
    cd "$PGDATA"

    if ! is_ready_for_pgctl_start "$PGDATA"; then
        die "В $PGDATA нет PG_VERSION и postgresql.conf — запуск PostgreSQL отменён."
    fi
    if ! is_complete_pgdata "$PGDATA"; then
        die "В $PGDATA неполный кластер (нет PG_VERSION/base) — запуск PostgreSQL отменён."
    fi

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
}

sync_file_storage() {
    prepare_local_file_storage

    info "Копирование fileStorage с сервера (rsync) ..."
    info "  source: root@${db_server}:${REMOTE_FILE_STORAGE_DIR}/"
    info "  target: ${LOCAL_FILE_STORAGE_DIR}/"
    if ! rsync -avrltD --stats --ignore-existing \
        "root@${db_server}:${REMOTE_FILE_STORAGE_DIR}/" "${LOCAL_FILE_STORAGE_DIR}/" >>"$LOG" 2>&1; then
        die "Не удалось скопировать fileStorage (нужен SSH-доступ root@${db_server})."
    fi

    configure_local_file_storage
    validate_local_file_storage
    ok
}

prepare_local_file_storage() {
    if [ -L "$LOCAL_FILE_STORAGE_DIR" ] && [ ! -e "$LOCAL_FILE_STORAGE_DIR" ]; then
        info "Удаление битой ссылки fileStorage: $LOCAL_FILE_STORAGE_DIR"
        rm -f "$LOCAL_FILE_STORAGE_DIR"
    fi

    if [ -e "$LOCAL_FILE_STORAGE_DIR" ] && [ ! -d "$LOCAL_FILE_STORAGE_DIR" ]; then
        die "LOCAL_FILE_STORAGE_DIR существует, но это не каталог: $LOCAL_FILE_STORAGE_DIR"
    fi

    mkdir -p "$LOCAL_FILE_STORAGE_DIR"
    mkdir -p "$LOCAL_FILE_STORAGE_DIR/temp"
}

set_local_property() {
    local key="$1"
    local value="$2"

    touch "$LOCAL_APP_PROPERTIES"
    if grep -qE "^${key}=" "$LOCAL_APP_PROPERTIES"; then
        sed -i.bak -E "s|^${key}=.*|${key}=${value}|" "$LOCAL_APP_PROPERTIES"
        rm -f "${LOCAL_APP_PROPERTIES}.bak"
    else
        printf '%s=%s\n' "$key" "$value" >>"$LOCAL_APP_PROPERTIES"
    fi
}

configure_local_file_storage() {
    info "Обновление локальной CUBA-конфигурации fileStorage: $LOCAL_APP_PROPERTIES"
    if [ ! -s "$LOCAL_APP_PROPERTIES" ]; then
        printf '%s\n' '# Local CUBA overrides for Gradle/Tomcat startup.' >"$LOCAL_APP_PROPERTIES"
    fi
    set_local_property "cuba.fileStorageDir" "$LOCAL_FILE_STORAGE_DIR"
    set_local_property "cuba.tempDir" "$LOCAL_FILE_STORAGE_DIR/temp"
}

validate_local_file_storage() {
    local file_count
    file_count=$(find "$LOCAL_FILE_STORAGE_DIR" -type f | wc -l | tr -d ' ')
    if [ "$file_count" -eq 0 ]; then
        die "После rsync локальный fileStorage пуст: $LOCAL_FILE_STORAGE_DIR"
    fi
    info "Локальный fileStorage готов: $LOCAL_FILE_STORAGE_DIR ($file_count файлов)"
}

# --- main ---
SKIP_DOWNLOAD=0
banner
info "Основная площадка: ${GREEN}${db_server}${NC}"
: >"$LOG"

preflight_checks
prepare_temp_directory
download_basebackup
install_to_local_pgdata
archive_fresh_backup
archive_and_start
sync_file_storage

info "${GREEN}Готово. Локальная PostgreSQL: $PGDATA${NC}"
info "Локальный fileStorage: $LOCAL_FILE_STORAGE_DIR"
info "CUBA overrides: $LOCAL_APP_PROPERTIES"
info "Проверка: ./start-postgres11.sh status"
