#!/bin/bash
# Быстрый деплой (Fast Deployment) для агентов разработки HRM HuntTech.
# Не перезапускает Tomcat, не сбрасывает кэш виджетов/тем, собирает инкрементально за секунды.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

DEPLOY_LOCK_FILE="${DEPLOY_LOCK_FILE:-$ROOT/deploy/.local-deploy.lock}"
DEPLOY_LOCK_TIMEOUT=120
TARGET="conf"

while [ $# -gt 0 ]; do
    case "$1" in
        --conf|--xml) TARGET="conf"; shift ;;
        --web) TARGET="web"; shift ;;
        --themes|--scss) TARGET="themes"; shift ;;
        --core) TARGET="core"; shift ;;
        --all) TARGET="all"; shift ;;
        -h|--help)
            echo "Использование: $0 [--conf|--web|--themes|--core|--all]"
            echo "  --conf / --xml   Быстрый деплой XML-экранов и messages в conf (3-5 сек, без рестарта)"
            echo "  --web            Инкрементальный деплой Java-контроллеров web-модуля (15-20 сек)"
            echo "  --themes         Деплой только SCSS-тем без виджетов (20-30 сек)"
            echo "  --core           Инкрементальный деплой сервисов core-модуля (15-20 сек)"
            echo "  --all            Быстрый деплой всех модулей без clean (30-40 сек)"
            exit 0
            ;;
        *)
            echo "Неизвестный аргумент: $1 (см. $0 --help)"
            exit 1
            ;;
    esac
done

acquire_lock() {
    local waited=0
    while ! shlock -f "$DEPLOY_LOCK_FILE" -p $$; do
        if [ "$waited" -ge "$DEPLOY_LOCK_TIMEOUT" ]; then
            echo "❌ Уже идёт сборка или деплой (lock: $DEPLOY_LOCK_FILE)."
            exit 1
        fi
        sleep 1
        waited=$((waited + 1))
    done
    echo "🔒 Mutex получен: $DEPLOY_LOCK_FILE"
}

cleanup_lock() {
    rm -f "$DEPLOY_LOCK_FILE"
}
trap cleanup_lock EXIT

acquire_lock

GRADLE_BIN="$ROOT/gradlew"
if [ ! -x "$GRADLE_BIN" ]; then
    GRADLE_BIN="gradle"
fi

case "$TARGET" in
    conf)
        echo "⚡ Быстрый деплой XML-дескрипторов и локализации (:app-web:deployConf)..."
        "$GRADLE_BIN" :app-web:deployConf
        echo "✅ XML-экраны и локализация обновлены. Закройте и откройте экран в браузере."
        ;;
    web)
        echo "⚡ Инкрементальный деплой web-модуля (:app-web:deploy -x test)..."
        "$GRADLE_BIN" :app-web:deploy -x test
        echo "✅ Web-модуль обновлен в Tomcat за считанные секунды."
        ;;
    themes)
        echo "⚡ Быстрый деплой тем (:app-web:deployThemes)..."
        "$GRADLE_BIN" :app-web:deployThemes
        echo "✅ Темы обновлены. Нажмите Ctrl+Shift+R в браузере."
        ;;
    core)
        echo "⚡ Инкрементальный деплой core-модуля (:app-core:deploy -x test)..."
        "$GRADLE_BIN" :app-core:deploy -x test
        echo "✅ Core-модуль обновлен в Tomcat."
        ;;
    all)
        echo "⚡ Быстрый деплой без clean (deploy -x test)..."
        "$GRADLE_BIN" deploy -x test
        echo "✅ Все модули инкрементально развернуты."
        ;;
esac
