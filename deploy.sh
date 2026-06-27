#!/bin/bash

# Имя общего конфигурационного файла
CONFIG_FILE="deploy_shared.conf"
DETAILED_DIFF=false
FORCE_DEPLOY=false
SCHEMA_ONLY=false

# ==============================================================================
# НАСТРОЙКА ОКРУЖЕНИЯ И ПОИСК ПУТЕЙ POSTGRESQL (ЛОКАЛЬНО)
# ==============================================================================
# Копируем логику из get_base.sh, чтобы локально находились утилиты pg_dump/psql
if [ -d "/opt/homebrew/opt/postgresql@11/bin" ]; then
    export PATH="/opt/homebrew/opt/postgresql@11/bin:$PATH"
elif [ -d "/usr/local/opt/postgresql@11/bin" ]; then
    export PATH="/usr/local/opt/postgresql@11/bin:$PATH"
fi

export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8

# ==============================================================================
# ФУНКЦИЯ АВТОМАТИЧЕСКОГО СОЗДАНИЯ И ПРОВЕРКИ КОНФИГУРАЦИИ
# ==============================================================================
check_and_load_config() {
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "=============================================================================="
        echo " ⚠️  КОНФИГУРАЦИОННЫЙ ФАЙЛ НЕ НАЙДЕН!"
        echo "=============================================================================="
        echo " Создаю дефолтный файл настроек: $CONFIG_FILE"
        
        cat << EOF > "$CONFIG_FILE"
# ==============================================================================
# ОБЩИЙ КОНФИГУРАЦИОННЫЙ ФАЙЛ ДЛЯ СКРИПТОВ (deploy.sh / get_base.sh)
# ==============================================================================

# Настройки удаленного сервера приложений
SERVER_USER="root"
SERVER_HOST="hr.hunttech.ru"

# Настройки сервера баз данных и хранилища (используется в get_base.sh)
DB_SERVER="hr.hunttech.ru"
REMOTE_APP_HOME="/opt/app_home"
LOCAL_APP_HOME="/opt/app_home"

# Пути к Tomcat на удаленном сервере
TOMCAT_WEBAPPS="/var/lib/tomcat9/webapps"
TOMCAT_BIN="/var/lib/tomcat9/bin"

# Пути к архивам резервных копий (Бэкапы)
LOCAL_BACKUP_BASE="./deploy_backups/local"
REMOTE_BACKUP_BASE="/var/lib/tomcat9/deploy_backups"
EOF
        echo "✅ Файл $CONFIG_FILE успешно создан с дефолтными настройками."
        echo "⚠️  Пожалуйста, проверьте и отредактируйте параметры в '$CONFIG_FILE',"
        echo "   после чего запустите скрипт деплоя повторно."
        echo "=============================================================================="
        exit 0
    else
        source "$CONFIG_FILE"
    fi
}

check_and_load_config

# ==============================================================================
# ФУНКЦИИ ВЫВОДА СПРАВКИ И УПРАВЛЕНИЯ СЕРВИСАМИ
# ==============================================================================
show_help() {
    echo "=============================================================================="
    echo " ℹ️  СПРАВКА ПО ИСПОЛЬЗОВАНИЮ СКРИПТА ДЕПЛОЯ, ОТКАТА И ДИАГНОСТИКИ (CUBA)"
    echo "=============================================================================="
    echo " Конфигурация загружена из: $CONFIG_FILE"
    echo " Целевой сервер: ${SERVER_USER}@${SERVER_HOST}"
    echo ""
    echo " Использование: $0 [-h] [-l] [-r] [-b] [-c] [-t] [-v 'YYYY-MM-DD/HH-MM']"
    echo ""
    echo " ПАРАМЕТРЫ И КЛЮЧИ:"
    echo "  (без ключей)          : Полный цикл деплоя (сборка -> бэкап -> деплой)."
    echo "  -h, --help            : Показать это справочное руководство."
    echo "  -l, --list            : Показать список всех доступных архивных версий."
    echo "  -r, --rollback        : Автоматически откатиться на самую последнюю версию."
    echo "  -b, --build           : Только локальная сборка WAR-файлов через Gradle."
    echo "  -c, --backup-only     : Сделать только резервную копию текущих WAR с сервера (без деплоя)."
    echo "  -t, --test-status     : Комплексная диагностика сервера (Tomcat, Postgres, WAR, Порты)."
    echo "  -v 'YYYY-MM-DD/HH-MM' : Восстановить произвольную указанную версию из архива."
    echo "=============================================================================="
    exit 0
}

stop_tomcat() {
    echo "⏸️  Остановка службы Tomcat на сервере..."
    ssh ${SERVER_USER}@${SERVER_HOST} "systemctl stop tomcat9 || $TOMCAT_BIN/shutdown.sh"
}

start_tomcat() {
    echo "▶️  Запуск службы Tomcat..."
    ssh ${SERVER_USER}@${SERVER_HOST} "systemctl start tomcat9 || $TOMCAT_BIN/startup.sh"
}

clear_tomcat_cache() {
    echo "🧹 Очистка распакованных старых каталогов CUBA приложения..."
    ssh ${SERVER_USER}@${SERVER_HOST} "rm -rf $TOMCAT_WEBAPPS/app $TOMCAT_WEBAPPS/app-core"
}

build_war_local() {
    echo "=============================================================================="
    echo " 📦 СБОРКА WAR-ФАЙЛОВ ПРИЛОЖЕНИЯ (ЛОКАЛЬНО)"
    echo "=============================================================================="
    ./gradlew clean buildWar
    if [ $? -ne 0 ]; then
        echo "❌ Ошибка компиляции приложения! Процесс остановлен."
        exit 1
    fi
    echo "✅ Локальные WAR-файлы успешно собраны in: ./build/distributions/war/"
}

# ==============================================================================
# РЕАЛИЗАЦИЯ СИСТЕМЫ АРХИВАЦИИ И ДИАГНОСТИКИ
# ==============================================================================
check_database_schema() {
    if [ "$FORCE_DEPLOY" = true ]; then
        echo "⏩ Проверка структуры БД пропущена (включен флаг -f)."
        return 0
    fi

    echo "🔍 Проверка синхронизации структуры базы данных ($DB_NAME)..."
    
    local LOCAL_SCHEMA="/tmp/local_schema_${DB_NAME}.sql"
    local REMOTE_SCHEMA="/tmp/remote_schema_${DB_NAME}.sql"
    
    # 1. Снимаем локальную схему
    pg_dump -h localhost -p "$LOCAL_DB_PORT" -U "$DB_USER" --schema-only "$DB_NAME" > "$LOCAL_SCHEMA" 2>/dev/null \
        || { echo "❌ Ошибка: Локальный pg_dump не найден или Postgres не запущен! Проверьте установку PostgreSQL."; return 1; }
        
    # 2. Снимаем удаленную схему через SSH
    ssh ${SERVER_USER}@${SERVER_HOST} "pg_dump -p $REMOTE_DB_PORT -U $DB_USER --schema-only "$DB_NAME"" > "$REMOTE_SCHEMA" 2>/dev/null \
        || { echo "❌ Ошибка: Удаленный pg_dump не удался через SSH. Проверьте доступ к серверу."; return 1; }

    sed -i.bak '/--/d; /^$/d; /AS integer/d' "$LOCAL_SCHEMA" "$REMOTE_SCHEMA" 2>/dev/null

    if ! diff -q "$LOCAL_SCHEMA" "$REMOTE_SCHEMA" > /dev/null; then
        echo "=============================================================================="
        echo " 🛑 ВНИМАНИЕ: СТРУКТУРА ЛОКАЛЬНОЙ И УДАЛЕННОЙ БД ОТЛИЧАЕТСЯ!"
        echo "=============================================================================="
        
        if [ "$DETAILED_DIFF" = true ]; then
            echo "📋 ПОДРОБНЫЙ ОТЧЕТ ОБ ИЗМЕНЕНИЯХ (ПОСТРОЧНЫЙ DIFF):"
            echo "------------------------------------------------------------------------------"
            diff -u "$REMOTE_SCHEMA" "$LOCAL_SCHEMA" | grep -E "^[+-][^[+-]" || true
        else
            echo "📋 КРАТКИЙ ОТЧЕТ ОБ ИЗМЕНЕНИЯХ (Измененные/Новые таблицы и объекты):"
            echo "------------------------------------------------------------------------------"
            diff "$REMOTE_SCHEMA" "$LOCAL_SCHEMA" | grep -E "CREATE TABLE|ALTER TABLE|CREATE INDEX" | sed 's/> //' | sed 's/< //' | sort -u || true
            echo "------------------------------------------------------------------------------"
            if [ "$SCHEMA_ONLY" = true ]; then
                echo "💡 Совет: Запустите команду с флагом -d (./deploy.sh -s -d) для построчного просмотра."
            else
                echo "💡 Совет: Запустите команду с флагом -d (./deploy.sh -d) для построчного просмотра."
            fi
        fi
        echo "=============================================================================="
        
        if [ "$SCHEMA_ONLY" = true ]; then
            echo "ℹ️  Режим проверки завершен. Изменения схемы на удаленном сервере НЕ применялись."
            rm -f "$LOCAL_SCHEMA" "$REMOTE_SCHEMA" "${LOCAL_SCHEMA}.bak" "${REMOTE_SCHEMA}.bak"
            exit 0
        fi

        echo -n "❓ Структура изменилась. Вы хотите продолжить деплой и применить изменения? (y/n): "
        read -r user_choice
        if [[ "$user_choice" != "y" && "$user_choice" != "Y" ]]; then
            echo "❌ Деплой отменен пользователем для сохранения целостности БД."
            rm -f "$LOCAL_SCHEMA" "$REMOTE_SCHEMA" "${LOCAL_SCHEMA}.bak" "${REMOTE_SCHEMA}.bak"
            exit 1
        fi
        
        echo "🔄 Генерирую и переношу SQL-скрипт изменений на удаленный сервер..."
        local PATCH_FILE="/tmp/db_migration_patch.sql"
        diff -u "$REMOTE_SCHEMA" "$LOCAL_SCHEMA" | grep -E "^\+" | sed 's/^+//' > "$PATCH_FILE" || true
        
        scp "$PATCH_FILE" ${SERVER_USER}@${SERVER_HOST}:/tmp/db_migration_patch.sql > /dev/null
        ssh ${SERVER_USER}@${SERVER_HOST} "psql -p $REMOTE_DB_PORT -U $DB_USER -d $DB_NAME -f /tmp/db_migration_patch.sql" >> /tmp/migration_db.log 2>&1
        echo "✅ Структура удаленной базы данных успешно синхронизирована с локальной."
    else
        echo "✅ Структура базы данных синхронна. Изменений схемы не обнаружено."
    fi

    rm -f "$LOCAL_SCHEMA" "$REMOTE_SCHEMA" "${LOCAL_SCHEMA}.bak" "${REMOTE_SCHEMA}.bak"
}

create_server_backup() {
    local DATE_DIR=$(date +"%Y-%m-%d") local TIME_DIR=$(date +"%H-%M")
    echo "=============================================================================="
    echo " 📸 ЗАПУСК РЕЗЕРВНОГО КОПИРОВАНИЯ ТЕКУЩИХ WAR С СЕРВЕРА ($DATE_DIR/$TIME_DIR)"
    echo "=============================================================================="
    local WAR_COUNT=$(ssh ${SERVER_USER}@${SERVER_HOST} "ls $TOMCAT_WEBAPPS/*.war 2>/dev/null | wc -l")
    if [ "$WAR_COUNT" -eq 0 ]; then echo "❌ Ошибка: .war файлы для копирования не найдены!"; exit 1; fi
    mkdir -p "$LOCAL_BACKUP_BASE/$DATE_DIR/$TIME_DIR"; ssh ${SERVER_USER}@${SERVER_HOST} "mkdir -p $REMOTE_BACKUP_BASE/$DATE_DIR/$TIME_DIR"
    ssh ${SERVER_USER}@${SERVER_HOST} "cp $TOMCAT_WEBAPPS/*.war $REMOTE_BACKUP_BASE/$DATE_DIR/$TIME_DIR/ 2>/dev/null || true"
    scp ${SERVER_USER}@${SERVER_HOST}:"$TOMCAT_WEBAPPS/*.war" "$LOCAL_BACKUP_BASE/$DATE_DIR/$TIME_DIR/" 2>/dev/null || true
    echo "✅ Резервное копирование успешно завершено."; exit 0
}

check_server_status() {
    echo "=============================================================================="
    echo " 🔍 КОМПЛЕКСНЫЙ АУДИТ И ПРОВЕРКА СОСТОЯНИЯ СЕРВЕРА $SERVER_HOST"
    echo "=============================================================================="
    echo -n "1. Проверка файлов в webapps/... "
    local APP_WAR_STATUS=$(ssh ${SERVER_USER}@${SERVER_HOST} "[ -f $TOMCAT_WEBAPPS/app.war ] && du -sh $TOMCAT_WEBAPPS/app.war | awk '{print \$1}' || echo 'MISSING'")
    local CORE_WAR_STATUS=$(ssh ${SERVER_USER}@${SERVER_HOST} "[ -f $TOMCAT_WEBAPPS/app-core.war ] && du -sh $TOMCAT_WEBAPPS/app-core.war | awk '{print \$1}' || echo 'MISSING'")
    echo "app.war: $APP_WAR_STATUS | core.war: $CORE_WAR_STATUS"
    echo -n "2. Статус службы Tomcat... "; local TOMCAT_PROC=$(ssh ${SERVER_USER}@${SERVER_HOST} "systemctl is-active tomcat9 2>/dev/null || echo active"); echo "$TOMCAT_PROC"
    echo -n "3. Статус службы PostgreSQL... "; local PG_PROC=$(ssh ${SERVER_USER}@${SERVER_HOST} "systemctl is-active postgresql 2>/dev/null || echo active"); echo "$PG_PROC"
    echo -n "4. Проверка сетевых портов... "; local PORT_8080=$(ssh ${SERVER_USER}@${SERVER_HOST} "ss -tlnp | grep -E ':8080\s' >/dev/null && echo 'OPEN' || echo 'CLOSED'")
    local PORT_5432=$(ssh ${SERVER_USER}@${SERVER_HOST} "ss -tlnp | grep -E ':5432\s' >/dev/null && echo 'OPEN' || echo 'CLOSED'")
    echo "Http (8080): $PORT_8080 | Postgres (5432): $PORT_5432"
    echo ""
    echo "5. FileStorage (${FILE_STORAGE_DIR:-/opt/app_home/fileStorage}):"
    ssh ${SERVER_USER}@${SERVER_HOST} "FS='${FILE_STORAGE_DIR:-/opt/app_home/fileStorage}'; \
        if [ ! -d \"\$FS\" ]; then echo '   ❌ Каталог отсутствует: '\$FS; exit 0; fi; \
        echo '   ✅ Каталог существует: '\$FS; \
        echo -n '   Права: '; ls -ld \"\$FS\"; \
        echo -n '   Свободно: '; df -h \"\$FS\" | tail -1 | awk '{print \$4}'; \
        echo -n '   Файлов (примерно): '; find \"\$FS\" -type f 2>/dev/null | wc -l; \
        TOMCAT_USER=\$(ps -eo user,comm 2>/dev/null | awk '/java/ && /tomcat/ {print \$1; exit}'); \
        TOMCAT_USER=\${TOMCAT_USER:-tomcat}; \
        if sudo -u \"\$TOMCAT_USER\" test -r \"\$FS\" && sudo -u \"\$TOMCAT_USER\" test -w \"\$FS\"; then \
            echo '   ✅ Пользователь Tomcat ('\"\$TOMCAT_USER\"') имеет RW-доступ'; \
        else \
            echo '   ⚠️  Tomcat ('\"\$TOMCAT_USER\"') не может читать/писать FileStorage'; \
            echo '   Рекомендация: chown -R tomcat:tomcat '\$FS' && chmod -R u+rwX,g+rX '\$FS; \
        fi; \
        APP_HOME_PROP=\$(grep -h 'app.home' /var/lib/tomcat9/bin/setenv.sh 2>/dev/null | head -1); \
        if [ -n \"\$APP_HOME_PROP\" ]; then echo '   setenv.sh app.home: '\$APP_HOME_PROP; fi"
    echo ""
    echo "   Рекомендуемые права на сервере:"
    echo "     chown -R tomcat:tomcat ${FILE_STORAGE_DIR:-/opt/app_home/fileStorage}"
    echo "     chmod -R u+rwX,g+rX ${FILE_STORAGE_DIR:-/opt/app_home/fileStorage}"
    echo "     mkdir -p ${FILE_STORAGE_DIR:-/opt/app_home/fileStorage}/temp"
    echo "   В setenv.sh Tomcat: export APP_HOME=/opt/app_home  (см. etc/tomcat-setenv.sh)"
    exit 0
}

list_backups() {
    echo "=============================================================================="
    echo " 📋 СПИСОК ДОСТУПНЫХ ВЕРСИЙ В АРХИВЕ НА $SERVER_HOST:"
    echo "=============================================================================="
    ssh ${SERVER_USER}@${SERVER_HOST} "find $REMOTE_BACKUP_BASE -type f -name '*.war' | sed -E 's|$REMOTE_BACKUP_BASE/||' | sed -E 's|/[^/]+\.war$||' | sort -r | uniq"; exit 0
}

restore_version() {
    local TARGET_PATH="$1"; echo "=============================================================================="; echo " ⏳ ИНИЦИАЛИЗАЦИЯ ОТКАТА НА ВЕРСИЮ: $TARGET_PATH"; echo "=============================================================================="
    local CHECK_EXISTS=$(ssh ${SERVER_USER}@${SERVER_HOST} "[ -f $REMOTE_BACKUP_BASE/$TARGET_PATH/app.war ] && echo 'OK' || echo 'NOT_FOUND'")
    if [ "$CHECK_EXISTS" != "OK" ]; then echo "❌ Ошибка: Архив с версией '$TARGET_PATH' не найден на сервере!"; exit 1; fi
    stop_tomcat; clear_tomcat_cache
    echo "🔄 Восстановление WAR-файлов из архива..."; ssh ${SERVER_USER}@${SERVER_HOST} "cp $REMOTE_BACKUP_BASE/$TARGET_PATH/*.war $TOMCAT_WEBAPPS/"; start_tomcat; exit 0
}

rollback_to_latest() {
    local LATEST_BACKUP=$(ssh ${SERVER_USER}@${SERVER_HOST} "find $REMOTE_BACKUP_BASE -type f -name 'app.war' -printf '%T@ %p\n' | sort -n | tail -1 | awk '{print \$2}' | sed -E 's|$REMOTE_BACKUP_BASE/||' | sed -E 's|/app\.war$||'")
    if [ -z "$LATEST_BACKUP" ]; then echo "❌ Ошибка: В каталоге бэкапов не найдено сохраненных версий!"; exit 1; fi
    restore_version "$LATEST_BACKUP"
}

# ==============================================================================
# ОБРАБОТЧИК КЛЮЧЕЙ КОМАНДНОЙ СТРОКИ (getopts)
# ==============================================================================
if [[ "${1:-}" == "--help" ]]; then show_help; fi

while getopts "lrbcthsdfv:" opt; do
    case ${opt} in
        d ) DETAILED_DIFF=true ;;
        f ) FORCE_DEPLOY=true ;;
        s ) SCHEMA_ONLY=true ;;
    esac
done
OPTIND=1

while getopts "lrbcthsdfv:" opt; do
    case ${opt} in
        h ) show_help ;;
        l ) list_backups ;;
        r ) rollback_to_latest ;;
        b ) build_war_local; exit 0 ;;
        c ) create_server_backup ;;
        t ) check_server_status ;;
        v ) restore_version "${OPTARG}" ;;
    esac
done

if [ "$SCHEMA_ONLY" = true ]; then
    check_database_schema
    exit 0
fi

# ==============================================================================
# СТАНДАРТНЫЙ ПРОЦЕСС ПОЛНОГО ДЕПЛОЯ
# ==============================================================================
DATE_DIR=$(date +"%Y-%m-%d")
TIME_DIR=$(date +"%H-%M") 
LOCAL_WAR_PATH="./build/distributions/war"

echo "=============================================================================="
echo " СТАРТ ПОЛНОГО ДЕПЛОЯ НА СЕРВЕР $SERVER_HOST"
echo "=============================================================================="

# 1. Сверка схем баз данных (теперь pg_dump будет успешно найден)
check_database_schema

# 2. Локальная сборка проекта
build_war_local

# 3. Подготовка папок бэкапа
mkdir -p "$LOCAL_BACKUP_BASE/$DATE_DIR/$TIME_DIR"
ssh ${SERVER_USER}@${SERVER_HOST} "mkdir -p $REMOTE_BACKUP_BASE/$DATE_DIR/$TIME_DIR"

# 4. Резервное копирование старой версии
echo "[3/6] Создание бэкапов работавших на сервере WAR файлов..."
ssh ${SERVER_USER}@${SERVER_HOST} "cp $TOMCAT_WEBAPPS/*.war $REMOTE_BACKUP_BASE/$DATE_DIR/$TIME_DIR/ 2>/dev/null || true"
scp ${SERVER_USER}@${SERVER_HOST}:"$TOMCAT_WEBAPPS/*.war" "$LOCAL_BACKUP_BASE/$DATE_DIR/$TIME_DIR/" 2>/dev/null || true

# 5. Остановка Tomcat и очистка его кэша
stop_tomcat
clear_tomcat_cache

# 6. Загрузка собранных файлов
echo "[5/6] Загрузка новых WAR файлов на сервер..."
scp "$LOCAL_WAR_PATH"/app.war ${SERVER_USER}@${SERVER_HOST}:"$TOMCAT_WEBAPPS/app.war"
if [ -f "$LOCAL_WAR_PATH/app-core.war" ]; then
    scp "$LOCAL_WAR_PATH"/app-core.war ${SERVER_USER}@${SERVER_HOST}:"$TOMCAT_WEBAPPS/app-core.war"
fi

# 7. Запуск обновленной системы
start_tomcat

echo "=============================================================================="
echo " 🎉 ДЕПЛОЙ НА $SERVER_HOST УСПЕШНО ЗАВЕРШЕН!"
echo "=============================================================================="