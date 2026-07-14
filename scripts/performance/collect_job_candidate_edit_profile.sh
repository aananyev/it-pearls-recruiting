#!/usr/bin/env bash
set -euo pipefail

# Собирает JFR и структурированные замеры JobCandidateEdit без изменения данных приложения.
# Перед запуском Tomcat должен быть запущен с:
# -Dhrm.jobCandidateEdit.performance.enabled=true

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

DURATION_SECONDS="${DURATION_SECONDS:-180}"
WARMUP_OPENS="${WARMUP_OPENS:-2}"
APP_LOG="${APP_LOG:-deploy/tomcat/logs/app.log}"
OUTPUT_ROOT="${OUTPUT_ROOT:-build/performance/job-candidate-edit}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_DIR="${OUTPUT_ROOT}/${TIMESTAMP}"
mkdir -p "$OUTPUT_DIR"

if [[ -n "${TOMCAT_PID:-}" ]]; then
  PID="$TOMCAT_PID"
else
  PID="$(pgrep -f 'org.apache.catalina.startup.Bootstrap' | head -n 1 || true)"
fi

if [[ -z "${PID:-}" ]]; then
  echo "Не найден PID Tomcat. Передайте TOMCAT_PID=<pid>." >&2
  exit 2
fi

if [[ ! -r "$APP_LOG" ]]; then
  echo "Не найден читаемый лог: $APP_LOG" >&2
  echo "Передайте APP_LOG=/полный/путь/к/app.log" >&2
  exit 3
fi

if ! command -v jcmd >/dev/null 2>&1; then
  echo "Команда jcmd не найдена. Требуется JDK 11." >&2
  exit 4
fi

if ! command -v jfr >/dev/null 2>&1; then
  echo "Команда jfr не найдена. Требуется JDK 11." >&2
  exit 5
fi

PROPERTIES_FILE="$OUTPUT_DIR/vm-system-properties.txt"
jcmd "$PID" VM.system_properties > "$PROPERTIES_FILE"
if ! grep -q '^hrm.jobCandidateEdit.performance.enabled=true$' "$PROPERTIES_FILE"; then
  echo "Tomcat запущен без -Dhrm.jobCandidateEdit.performance.enabled=true" >&2
  echo "Перезапустите Tomcat с диагностическим system property." >&2
  exit 6
fi

START_LINE=$(( $(wc -l < "$APP_LOG") + 1 ))
JFR_FILE="$OUTPUT_DIR/job-candidate-edit.jfr"
JFR_NAME="JobCandidateEdit_${TIMESTAMP}"

jcmd "$PID" JFR.start \
  name="$JFR_NAME" \
  settings=profile \
  filename="$JFR_FILE" \
  duration="${DURATION_SECONDS}s" \
  dumponexit=true

cat <<EOF
Сбор запущен на ${DURATION_SECONDS} секунд.

Во время записи выполните сценарии:
1. Два прогревочных открытия кандидата.
2. Не менее 15 открытий существующего кандидата с фотографией, резюме и взаимодействиями.
3. Не менее 10 открытий кандидата с большим числом взаимодействий и резюме.
4. Не менее 10 открытий формы создания нового кандидата.
5. Каждый раз дождитесь полного отображения формы и закройте её без сохранения.

Не переключайте тяжёлые вкладки во время замера стартового открытия.
EOF

sleep "$DURATION_SECONDS"

# JFR с duration обычно сохраняется автоматически. Даём JVM время завершить запись.
for _ in $(seq 1 20); do
  [[ -s "$JFR_FILE" ]] && break
  sleep 1
done

if [[ ! -s "$JFR_FILE" ]]; then
  echo "JFR-файл не создан: $JFR_FILE" >&2
  jcmd "$PID" JFR.check || true
  exit 7
fi

STRUCTURED_LOG="$OUTPUT_DIR/job-candidate-edit-startup.log"
tail -n "+$START_LINE" "$APP_LOG" | grep 'JOB_CANDIDATE_EDIT_PERF|' > "$STRUCTURED_LOG" || true

if [[ ! -s "$STRUCTURED_LOG" ]]; then
  echo "В логе не найдены структурированные замеры JobCandidateEdit." >&2
  exit 8
fi

jfr summary "$JFR_FILE" > "$OUTPUT_DIR/jfr-summary.txt"
jfr print \
  --events jdk.ExecutionSample,jdk.NativeMethodSample,jdk.FileRead,jdk.FileWrite,jdk.SocketRead,jdk.SocketWrite,jdk.GarbageCollection,jdk.GCHeapSummary,jdk.ThreadPark \
  "$JFR_FILE" > "$OUTPUT_DIR/jfr-events.txt"

grep -n -B 20 -A 60 \
  -E 'JobCandidateEdit|InteractionService|FileDescriptorImageHelper|DataManager|RdbmsStore|QueryImpl' \
  "$OUTPUT_DIR/jfr-events.txt" > "$OUTPUT_DIR/job-candidate-edit-jfr-slices.txt" || true

python3 scripts/performance/job_candidate_edit_performance_report.py \
  --log "$STRUCTURED_LOG" \
  --markdown "$OUTPUT_DIR/job-candidate-edit-performance-report.md" \
  --csv "$OUTPUT_DIR/job-candidate-edit-performance-phases.csv" \
  --warmup-opens "$WARMUP_OPENS"

cat <<EOF

Сбор завершён.
Каталог результатов: $OUTPUT_DIR

Передайте для анализа:
- job-candidate-edit-performance-report.md
- job-candidate-edit-performance-phases.csv
- job-candidate-edit-startup.log
- jfr-summary.txt
- job-candidate-edit-jfr-slices.txt
- catalina.out или app.log за тот же интервал
EOF
