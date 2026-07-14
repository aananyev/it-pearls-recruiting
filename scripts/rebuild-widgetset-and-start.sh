#!/bin/bash
# Чистая пересборка HRM HuntTech с обязательной компиляцией Vaadin widgetset.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APP_CONTEXT="${APP_CONTEXT:-app}"
APP_URL="${APP_URL:-http://localhost:8080/${APP_CONTEXT}/}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-300}"
POLL_INTERVAL="${POLL_INTERVAL:-5}"
WIDGETSET_NAME="com.company.hunttech.web.toolkit.ui.AppWidgetSet"
WIDGETSET_RELATIVE_PATH="VAADIN/widgetsets/${WIDGETSET_NAME}/${WIDGETSET_NAME}.nocache.js"
DEPLOYED_WIDGETSET="$ROOT/deploy/tomcat/webapps/${APP_CONTEXT}/${WIDGETSET_RELATIVE_PATH}"
PROJECT_MARKER="hunttech_recruiting/deploy/tomcat"

log() {
    printf '%s\n' "$*"
}

is_project_java() {
    local pid="$1"
    ps -p "$pid" -o command= 2>/dev/null | grep -q "$PROJECT_MARKER"
}

stop_project_tomcat() {
    local pid pids

    ./gradlew stop >/dev/null 2>&1 || true

    pids="$(pgrep -f "$PROJECT_MARKER" 2>/dev/null || true)"
    for pid in $pids; do
        if is_project_java "$pid"; then
            log "Останавливаю Tomcat HRM HuntTech, PID=$pid"
            kill "$pid" 2>/dev/null || true
        fi
    done

    sleep 2

    pids="$(pgrep -f "$PROJECT_MARKER" 2>/dev/null || true)"
    for pid in $pids; do
        if is_project_java "$pid"; then
            log "Принудительно останавливаю Tomcat HRM HuntTech, PID=$pid"
            kill -9 "$pid" 2>/dev/null || true
        fi
    done
}

clean_tomcat_deployment() {
    log "Удаляю старые exploded-приложения и кэш Tomcat"
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

build_application_with_widgetset() {
    log "Очищаю Gradle build"
    ./gradlew clean --no-daemon --stacktrace

    log "Компилирую Java web-модуля"
    ./gradlew :app-web:compileJava --no-daemon --stacktrace

    log "Компилирую Vaadin widgetset ${WIDGETSET_NAME}"
    ./gradlew :app-web-toolkit:buildWidgetSet --no-daemon --stacktrace

    log "Разворачиваю приложение после сборки widgetset"
    ./gradlew deploy -x test --no-daemon --stacktrace
}

verify_deployed_widgetset() {
    if [ ! -s "$DEPLOYED_WIDGETSET" ]; then
        log "ОШИБКА: widgetset не развернут или пуст:"
        log "$DEPLOYED_WIDGETSET"
        log "Найденные nocache.js:"
        find "$ROOT" -path '*/VAADIN/widgetsets/*/*.nocache.js' -type f -print 2>/dev/null || true
        exit 1
    fi

    log "Widgetset развернут: $DEPLOYED_WIDGETSET"
    ls -lh "$DEPLOYED_WIDGETSET"
}

wait_for_application() {
    local elapsed=0
    local code

    log "Ожидаю HTTP 200 от $APP_URL"
    while [ "$elapsed" -lt "$WAIT_TIMEOUT" ]; do
        code="$(curl -s -o /dev/null -w '%{http_code}' "$APP_URL" || true)"
        if [ "$code" = "200" ]; then
            log "Приложение отвечает HTTP 200"
            return 0
        fi
        sleep "$POLL_INTERVAL"
        elapsed=$((elapsed + POLL_INTERVAL))
    done

    log "ОШИБКА: приложение не вернуло HTTP 200 за ${WAIT_TIMEOUT} секунд"
    tail -n 300 "$ROOT/deploy/tomcat/logs/catalina.out" 2>/dev/null || true
    exit 1
}

verify_widgetset_http() {
    local widgetset_url="${APP_URL%/}/${WIDGETSET_RELATIVE_PATH}"
    local response_file
    local code

    response_file="$(mktemp)"
    code="$(curl -sS -o "$response_file" -w '%{http_code}' "$widgetset_url" || true)"

    if [ "$code" != "200" ] || [ ! -s "$response_file" ]; then
        log "ОШИБКА: widgetset недоступен по HTTP"
        log "URL: $widgetset_url"
        log "HTTP: $code"
        rm -f "$response_file"
        exit 1
    fi

    if grep -qiE '<html|404|not found' "$response_file"; then
        log "ОШИБКА: вместо JavaScript widgetset сервер вернул HTML/страницу ошибки"
        log "URL: $widgetset_url"
        head -n 30 "$response_file"
        rm -f "$response_file"
        exit 1
    fi

    log "Widgetset доступен: HTTP 200, $(wc -c < "$response_file" | tr -d ' ') байт"
    rm -f "$response_file"
}

if [ -z "${JAVA_HOME:-}" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 11)"
fi
export PATH="$JAVA_HOME/bin:$PATH"

log "Java:"
java -version

stop_project_tomcat
clean_tomcat_deployment
build_application_with_widgetset
verify_deployed_widgetset

log "Запускаю Tomcat"
./gradlew start --no-daemon --stacktrace

wait_for_application
verify_widgetset_http

log "HRM HuntTech запущен, widgetset загружен корректно"
log "URL: $APP_URL"
