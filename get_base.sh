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
old_archive="${current_catalog}/../hunttech-basebackup-old.tgz"
new_archive="${current_catalog}/../hunttech-basebackup.tgz"
BACKUPBASELOG=backupbase.log
LOG="${CWD}/${BACKUPBASELOG}"
db_server=hr.hunttech.ru
db_user=replica

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

# --- проверки перед стартом ---
preflight_checks() {
    local missing=0

    for bin in pg_basebackup pg_ctl psql; do
        if [ ! -x "$PG11_BIN/$bin" ]; then
            fail "Не найден $PG11_BIN/$bin"
            missing=1
        fi
    done

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

    info_n "Проверка соединения с $db_server ... "
    if ! "$PSQL" -h "$db_server" -U "$db_user" -d postgres -tAc "SELECT version();" >>"$LOG" 2>&1; then
        echo
        fail "Не удалось подключиться к $db_server (см. $LOG)."
        fail "Частая причина на macOS: segfault libpq при SSL — скрипт выставляет PGSSLMODE=disable."
        fail "Проверьте ~/.pgpass и доступность хоста."
        missing=1
    else
        ok
    fi

    if [ "$missing" -ne 0 ]; then
        die "Предварительные проверки не пройдены."
    fi
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

prepare_temp_directory() {
    if [ ! -d "$postgre_temp_database" ]; then
        info_n "Создание временного каталога $postgre_temp_database ... "
        mkdir "$postgre_temp_database"
        ok
        return
    fi

    if is_complete_pgdata "$postgre_temp_database"; then
        info "Временный каталог уже содержит полный base backup."
        read -r -p "Использовать его без повторной загрузки? [y/N]: " reuse
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

download_basebackup() {
    info_n "Переход во временный каталог $postgre_temp_database ... "
    cd "$postgre_temp_database"
    ok

    if [ "${SKIP_DOWNLOAD:-0}" -eq 1 ]; then
        info "Пропуск загрузки — используется существующий backup."
        return
    fi

    info "Загрузка base backup с $db_server (pg_basebackup, WAL stream) ..."
    info "Лог: $LOG"

    if ! "$PG_BASEBACKUP" \
        -P \
        -h "$db_server" \
        -D . \
        -U "$db_user" \
        -X stream \
        --checkpoint=fast \
        --no-slot \
        >>"$LOG" 2>&1; then
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
    info_n "Переход в каталог данных $PGDATA ... "
    cd "$PGDATA"
    ok

    info_n "Остановка локальной PostgreSQL ... "
    if "$PG_CTL" stop -D . -m fast >>"$LOG" 2>&1; then
        ok
    else
        info "не запущена (продолжаем)"
    fi

    info_n "Очистка каталога данных ... "
    find . -mindepth 1 -maxdepth 1 -exec rm -rf {} +
    ok

    info "Копирование backup из $postgre_temp_database в $PGDATA ..."
    if command -v pv >/dev/null 2>&1; then
        file_count=$(find "$postgre_temp_database" -type f | wc -l | tr -d ' ')
        cp -av "$postgre_temp_database"/* . 2>>"$LOG" | pv -l -s "$file_count" >/dev/null
    else
        cp -av "$postgre_temp_database"/* . >>"$LOG" 2>&1
    fi
    ok

    strip_standby_files "$PGDATA"

    info_n "Удаление временного каталога ... "
    rm -rf "$postgre_temp_database"
    ok
}

archive_and_start() {
    cd "$PGDATA"

    info "Архивация прежней копии данных ..."
    rm -f "$old_archive" 2>>"$LOG" || true
    if [ -f "$new_archive" ]; then
        mv "$new_archive" "$old_archive"
    fi

    if command -v pv >/dev/null 2>&1; then
        tar czf - . 2>>"$LOG" | pv -p --timer --rate --bytes >"$new_archive"
    else
        tar czf "$new_archive" . >>"$LOG" 2>&1
    fi
    ok

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
    info "Копирование fileStorage с сервера (rsync) ..."
    if ! rsync -avrltD --stats --ignore-existing \
        "root@${db_server}:/opt/app_home/fileStorage" /opt/app_home/ >>"$LOG" 2>&1; then
        die "Не удалось скопировать fileStorage (нужен SSH-доступ root@${db_server})."
    fi
    ok
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
archive_and_start
sync_file_storage

cd "$CWD"
info "${GREEN}Готово. Локальная PostgreSQL: $PGDATA${NC}"
info "Проверка: ./start-postgres11.sh status"
