#!/bin/bash
# Сериализованный прогон Gradle для агентов-разработчиков (Antigravity/Hermes-2).
# Один gradle-процесс в момент времени (протокол 3 агентов): два параллельных
# gradle = deadlock на ~/.gradle/caches и конфликт FTS-локов.
#
# Использование (из СВОЕГО worktree):
#   bash <root>/scripts/agent-gradle.sh :app-web:compileJava
#   bash <root>/scripts/agent-gradle.sh :app-core:test --tests "com.company.hunttech.core.ScreenViewIntegrityTest"
#   bash <root>/scripts/agent-gradle.sh :app-web:buildScssThemes
#
# Скрипт берёт тот же mutex, что и scripts/start-app.sh (deploy/.local-deploy.lock),
# поэтому сборка не пересечётся ни с деплоем Hermes-1, ни с другими агентами.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORKDIR="$(pwd)"
LOCK_FILE="${AGENT_GRADLE_LOCK:-$ROOT/deploy/.local-deploy.lock}"
LOCK_TIMEOUT="${AGENT_GRADLE_LOCK_TIMEOUT:-600}"   # секунд ожидания mutex
GRADLE_ARGS=("$@")

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
    echo "Использование: agent-gradle.sh <gradle-args...>"
    echo "  Сериализует gradle-прогоны агентов через $LOCK_FILE"
    echo "  Пример: agent-gradle.sh :app-web:compileJava"
    echo "  Переменные: AGENT_GRADLE_LOCK, AGENT_GRADLE_LOCK_TIMEOUT"
    exit 0
fi

if [ ${#GRADLE_ARGS[@]} -eq 0 ]; then
    echo "❌ Нет аргументов gradle (см. agent-gradle.sh --help)"
    exit 1
fi

if [ "$WORKDIR" = "$ROOT" ]; then
    echo "⚠️  Выполняется из общей копии ($ROOT)."
    echo "    Агенты-разработчики должны собирать из СВОЕГО worktree."
    echo "    Общая копия — зона Hermes-1 (деплой через scripts/start-app.sh)."
    exit 1
fi
if ! git -C "$WORKDIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "❌ $WORKDIR не является git-worktree — запускайте из worktree агента."
    exit 1
fi

mkdir -p "$(dirname "$LOCK_FILE")"

acquire_lock() {
    local waited=0
    while ! shlock -f "$LOCK_FILE" -p $$; do
        if [ "$waited" -ge "$LOCK_TIMEOUT" ]; then
            echo "❌ Уже идёт деплой/сборка (lock: $LOCK_FILE, владелец PID $(cat "$LOCK_FILE" 2>/dev/null))."
            echo "   Дождитесь завершения текущего процесса (протокол 3 агентов)."
            exit 1
        fi
        sleep 2
        waited=$((waited + 2))
    done
    echo "🔒 Mutex получен: $LOCK_FILE (сборка сериализована)"
}
cleanup_lock() { rm -f "$LOCK_FILE"; }
trap cleanup_lock EXIT

acquire_lock
echo "[$(date '+%Y-%m-%d %H:%M:%S')] agent-gradle PID=$$ workdir=$WORKDIR args=${GRADLE_ARGS[*]}" >> "$ROOT/deploy/tomcat/logs/local-deploy.log"

# Убеждаемся, что нет ДРУГИХ активных gradle-клиентов (не демонов).
local_pids="$(pgrep -f 'GradleWrapperMain' 2>/dev/null || true)"
if [ -n "$local_pids" ]; then
    echo "❌ Обнаружены другие gradle-процессы (PID: $(echo "$local_pids" | tr '\n' ' ')) — параллельная сборка запрещена."
    exit 1
fi

cd "$WORKDIR"
echo "🧪 gradle ${GRADLE_ARGS[*]} (workdir: $WORKDIR)"
"./gradlew" "${GRADLE_ARGS[@]}"
rc=$?

echo "[$(date '+%Y-%m-%d %H:%M:%S')] agent-gradle done PID=$$ rc=$rc" >> "$ROOT/deploy/tomcat/logs/local-deploy.log"
exit $rc
