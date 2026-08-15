#!/bin/bash
# Надёжный локальный запуск CUBA/Tomcat для HRM HuntTech (без голого gradlew restart).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# ==============================================================================
# СЕРИАЛИЗАЦИЯ ЛОКАЛЬНОГО ДЕПЛОЯ (протокол 3 агентов, 2026-08-15)
# Один деплой/рестарт в один момент времени: flock-mutex + проверки git/gradle.
# Прямые вызовы gradle deploy / startup.sh мимо этого скрипта запрещены.
# ==============================================================================
DEPLOY_LOCK_FILE="${DEPLOY_LOCK_FILE:-$ROOT/deploy/.local-deploy.lock}"
DEPLOY_LOCK_TIMEOUT="${DEPLOY_LOCK_TIMEOUT:-600}"   # секунд ожидания mutex
SKIP_GIT_CHECK="${SKIP_GIT_CHECK:-false}"
DEPLOY_LOG="${DEPLOY_LOG:-$ROOT/deploy/tomcat/logs/local-deploy.log}"

for arg in "$@"; do
    case "$arg" in
        --force|--skip-git-check) SKIP_GIT_CHECK=true ;;
        -h|--help)
            echo "Использование: $0 [--force|--skip-git-check]"
            echo "  --force / --skip-git-check  пропустить проверки git (ветка master + чистая копия)"
            echo "  Mutex: $DEPLOY_LOCK_FILE (ожидание до ${DEPLOY_LOCK_TIMEOUT} с)"
            exit 0
            ;;
    esac
done

mkdir -p "$(dirname "$DEPLOY_LOG")"

# shlock (macOS): lock-файл с PID оболочки-владельца; stale-лок после kill -9
# снимается автоматически (shlock проверяет живость PID через kill(0)).
acquire_lock() {
    local waited=0
    while ! shlock -f "$DEPLOY_LOCK_FILE" -p $$; do
        if [ "$waited" -ge "$DEPLOY_LOCK_TIMEOUT" ]; then
            echo "❌ Уже идёт локальный деплой/сборка (lock: $DEPLOY_LOCK_FILE,"
            echo "   владелец PID $(cat "$DEPLOY_LOCK_FILE" 2>/dev/null))."
            echo "   Дождитесь завершения текущего процесса (протокол 3 агентов);"
            echo "   если владелец убит -9 — shlock подхватит лок автоматически."
            exit 1
        fi
        sleep 2
        waited=$((waited + 2))
    done
    echo "🔒 Mutex получен: $DEPLOY_LOCK_FILE (единственный процесс деплоя)"
}

cleanup_lock() {
    rm -f "$DEPLOY_LOCK_FILE"
}
trap cleanup_lock EXIT

acquire_lock
echo "[$(date '+%Y-%m-%d %H:%M:%S')] deploy start PID=$$ args=$*" >> "$DEPLOY_LOG"

# Деплой/рестарт — только из чистой ветки master (общая копия; протокол 3 агентов).
check_git_clean() {
    if [ "$SKIP_GIT_CHECK" = "true" ]; then
        log "Проверка git пропущена (--force)."
        return 0
    fi
    local branch
    branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    if [ "$branch" != "master" ]; then
        log "❌ Деплой разрешён только из ветки master (текущая: $branch)."
        log "   Worktree разработчика — только smoke со своим --force, деплой общей среды — только Hermes-1."
        exit 1
    fi
    if [ -n "$(git status --porcelain --untracked-files=no 2>/dev/null)" ]; then
        log "❌ Рабочая копия не чистая — деплой из грязной копии запрещён (протокол 3 агентов)."
        log "   Закоммитьте/застейджите правки или осознанно используйте --force."
        exit 1
    fi
    log "✅ git: ветка master, рабочая копия чистая (среди tracked-файлов)."
}

APP_URL="${APP_URL:-http://localhost:8080/app/}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-300}"
POLL_INTERVAL="${POLL_INTERVAL:-5}"
PROJECT_MARKER="hunttech_recruiting/deploy/tomcat"
LOCAL_APP_HOME="${LOCAL_APP_HOME:-$ROOT/deploy/tomcat/app_home}"
LOCAL_APP_PROPERTIES="$LOCAL_APP_HOME/local.app.properties"
# Штатный запуск сохраняет все scheduled tasks. Для изолированной диагностики
# JobCandidateEdit передайте LOCAL_SCHEDULING_ACTIVE=false явно.
LOCAL_SCHEDULING_ACTIVE="${LOCAL_SCHEDULING_ACTIVE:-true}"
LOCAL_JAVA_XMS="${LOCAL_JAVA_XMS:-1024m}"
LOCAL_JAVA_XMX="${LOCAL_JAVA_XMX:-4096m}"
HEAP_DUMP_DIR="${HEAP_DUMP_DIR:-$ROOT/deploy/tomcat/logs/heapdumps}"
GC_LOG_FILE="${GC_LOG_FILE:-$ROOT/deploy/tomcat/logs/gc.log}"
JVM_DIAGNOSTICS_DIR="${JVM_DIAGNOSTICS_DIR:-$ROOT/deploy/tomcat/logs/diagnostics}"

log() { printf '%s\n' "$*"; }

is_project_java() {
  local pid="$1"
  ps -p "$pid" -o command= 2>/dev/null | grep -q "$PROJECT_MARKER"
}

find_project_pid() {
  local pid
  for pid in $(pgrep -f "$PROJECT_MARKER" 2>/dev/null || true); do
    if is_project_java "$pid"; then
      printf '%s\n' "$pid"
      return 0
    fi
  done
  return 1
}

kill_stale_project_tomcat() {
  local port pid pids
  for port in 8080 8787; do
    pids="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null || true)"
    for pid in $pids; do
      if is_project_java "$pid"; then
        log "Останавливаю зависший Tomcat (PID $pid, порт $port)..."
        kill "$pid" 2>/dev/null || true
      fi
    done
  done

  local orphans
  orphans="$(pgrep -f "$PROJECT_MARKER" 2>/dev/null || true)"
  for pid in $orphans; do
    if is_project_java "$pid"; then
      log "Останавливаю процесс Tomcat проекта (PID $pid)..."
      kill "$pid" 2>/dev/null || true
    fi
  done

  sleep 2
  for port in 8080 8787; do
    pids="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null || true)"
    for pid in $pids; do
      if is_project_java "$pid"; then
        log "Принудительно: kill -9 PID $pid (порт $port)"
        kill -9 "$pid" 2>/dev/null || true
      fi
    done
  done
}

# Два параллельных gradle-процесса = deadlock на кэше и FTS-локи (протокол 3 агентов).
# Проверка до первого вызова ./gradlew: активные клиенты (не фоновые демоны).
ensure_no_other_gradle() {
  local pids
  pids="$(pgrep -f 'GradleWrapperMain' 2>/dev/null || true)"
  if [ -n "$pids" ]; then
    log "❌ Обнаружены другие gradle-процессы (PID: $(echo "$pids" | tr '\n' ' ')) — параллельная сборка запрещена."
    log "   Дождитесь завершения; после kill -9 чистить ~/.gradle/caches/*.lock и FTS write.lock."
    exit 1
  fi
}

# Порт 8080 после остановки нашего Tomcat должен быть свободен (или занят нашим же процессом).
ensure_port_free_for_restart() {
  local pid
  pid="$(lsof -nP -iTCP:8080 -sTCP:LISTEN -t 2>/dev/null | head -1 || true)"
  if [ -n "$pid" ] && ! is_project_java "$pid"; then
    log "❌ Порт 8080 занят чужим процессом (PID $pid) — рестарт Tomcat невозможен."
    log "   Остановите его вручную: kill $pid (проверьте, что это не важный сервис)."
    exit 1
  fi
}

ensure_postgres() {
  if command -v pg_isready >/dev/null 2>&1 && pg_isready -q 2>/dev/null; then
    log "PostgreSQL: готов (pg_isready)."
    return 0
  fi
  log "PostgreSQL не отвечает — запуск ./start-postgres11.sh start ..."
  ./start-postgres11.sh start
  for _ in $(seq 1 30); do
    if pg_isready -q 2>/dev/null; then
      log "PostgreSQL: готов."
      return 0
    fi
    sleep 1
  done
  log "Ошибка: PostgreSQL не поднялся за 30 с. Проверьте ./start-postgres11.sh status"
  exit 1
}

clean_deployment() {
  log "Удаляю старые exploded-приложения и кэш Tomcat..."
  rm -rf \
    "$ROOT/deploy/tomcat/webapps/app" \
    "$ROOT/deploy/tomcat/webapps/app-core" \
    "$ROOT/deploy/tomcat/webapps/hrm" \
    "$ROOT/deploy/tomcat/webapps/hrm-core" \
    "$ROOT/deploy/tomcat/work/Catalina/localhost/app" \
    "$ROOT/deploy/tomcat/work/Catalina/localhost/app-core" \
    "$ROOT/deploy/tomcat/work/Catalina/localhost/hrm" \
    "$ROOT/deploy/tomcat/work/Catalina/localhost/hrm-core"

  if [ -d "$ROOT/deploy/tomcat/temp" ]; then
    find "$ROOT/deploy/tomcat/temp" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
  fi
}

ensure_local_app_properties() {
  local temp_file
  mkdir -p "$LOCAL_APP_HOME"
  touch "$LOCAL_APP_PROPERTIES"
  temp_file="$(mktemp)"

  # Значение задаётся явно для воспроизводимого запуска. По умолчанию scheduler
  # включён; false используется только для отдельной диагностики конкуренции с FTS.
  awk -v scheduling_active="$LOCAL_SCHEDULING_ACTIVE" '
    BEGIN { replaced = 0 }
    /^cuba\.schedulingActive=/ {
      print "cuba.schedulingActive=" scheduling_active
      replaced = 1
      next
    }
    { print }
    END {
      if (!replaced) {
        print ""
        print "# Локальный scheduler HRM HuntTech; false — только диагностический режим."
        print "cuba.schedulingActive=" scheduling_active
      }
    }
  ' "$LOCAL_APP_PROPERTIES" > "$temp_file"

  mv "$temp_file" "$LOCAL_APP_PROPERTIES"
  log "Local app properties: cuba.schedulingActive=$LOCAL_SCHEDULING_ACTIVE"
  log "Файл: $LOCAL_APP_PROPERTIES"
}

configure_jvm_diagnostics() {
  mkdir -p "$HEAP_DUMP_DIR" "$(dirname "$GC_LOG_FILE")" "$JVM_DIAGNOSTICS_DIR"

  # Параметры добавляются последними: диагностические значения имеют приоритет
  # над случайно оставшимися локальными -Xms/-Xmx.
  CATALINA_OPTS="${CATALINA_OPTS:-} \
-Xms${LOCAL_JAVA_XMS} \
-Xmx${LOCAL_JAVA_XMX} \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=${HEAP_DUMP_DIR} \
-Xlog:gc*:file=${GC_LOG_FILE}:time,uptime,level,tags:filecount=5,filesize=20M"
  export CATALINA_OPTS

  log "JVM heap: -Xms${LOCAL_JAVA_XMS} -Xmx${LOCAL_JAVA_XMX}"
  log "Heap dumps: $HEAP_DUMP_DIR"
  log "GC log: $GC_LOG_FILE"
}

wait_for_http() {
  local elapsed=0 code
  log "Ожидание ответа $APP_URL (таймаут ${WAIT_TIMEOUT} с, warmup CUBA 1–5 мин)..."
  while [ "$elapsed" -lt "$WAIT_TIMEOUT" ]; do
    code="$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 "$APP_URL" 2>/dev/null || echo '000')"
    if [ "$code" = "200" ]; then
      log "Приложение доступно: HTTP 200 — $APP_URL"
      return 0
    fi
    sleep "$POLL_INTERVAL"
    elapsed=$((elapsed + POLL_INTERVAL))
    log "  ... ещё не готово (HTTP $code), прошло ${elapsed} с"
  done
  log "Таймаут: приложение не ответило HTTP 200 за ${WAIT_TIMEOUT} с."
  log "Проверьте логи: deploy/tomcat/logs/catalina.out"
  log "Порты: nc -z localhost 8080; JDWP: nc -z localhost 8787"
  exit 1
}

write_jvm_diagnostics() {
  local pid
  if ! command -v jcmd >/dev/null 2>&1; then
    log "jcmd не найден — JVM diagnostics пропущены."
    return 0
  fi
  pid="$(find_project_pid || true)"
  if [ -z "$pid" ]; then
    log "PID Tomcat не найден — JVM diagnostics пропущены."
    return 0
  fi

  jcmd "$pid" VM.command_line > "$JVM_DIAGNOSTICS_DIR/jvm-command-line.txt" 2>&1 || true
  jcmd "$pid" VM.flags > "$JVM_DIAGNOSTICS_DIR/jvm-flags.txt" 2>&1 || true
  jcmd "$pid" GC.heap_info > "$JVM_DIAGNOSTICS_DIR/heap-info.txt" 2>&1 || true
  jcmd "$pid" GC.class_histogram > "$JVM_DIAGNOSTICS_DIR/class-histogram-startup.txt" 2>&1 || true
  log "JVM diagnostics: $JVM_DIAGNOSTICS_DIR"
}

ensure_postgres

# Технические guard'ы протокола 3 агентов (деплой только Hermes-1, один процесс в момент).
# Выполняются ДО остановки Tomcat: отказ не роняет работающее приложение.
check_git_clean
ensure_no_other_gradle

kill_stale_project_tomcat
ensure_port_free_for_restart

log "Gradle stop (ошибки игнорируются)..."
./gradlew stop >/dev/null 2>&1 || true

# Схема должна обновляться до deploy: иначе новая entity-модель может обратиться
# к ещё отсутствующей колонке и сорвать открытие экранов после входа.
log "Применяю накопленные миграции CUBA к локальной PostgreSQL..."
./gradlew updateDb --no-daemon --stacktrace

clean_deployment

log "Чистая сборка и deploy без запуска тестов..."
./gradlew clean deploy -x test

# app_home и JVM-параметры задаются после deploy, чтобы их не затронула очистка.
ensure_local_app_properties
configure_jvm_diagnostics

log "Запуск Tomcat..."
./gradlew start

log "URL: $APP_URL"
wait_for_http
write_jvm_diagnostics
echo "[$(date '+%Y-%m-%d %H:%M:%S')] deploy done PID=$$ HTTP 200" >> "$DEPLOY_LOG"
