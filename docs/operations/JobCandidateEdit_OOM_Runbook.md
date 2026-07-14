# Локальная диагностика OutOfMemoryError при открытии JobCandidateEdit

## Назначение

Runbook используется для воспроизводимой проверки карточки кандидата HRM HuntTech после ошибок `java.lang.OutOfMemoryError: Java heap space`. Процедура исключает старые классы Tomcat, параллельный FTS и недостаточный диагностический heap.

## Условия запуска

- ветка: `agent/job-candidate-edit-layout-fix`;
- Java 11;
- локальный PostgreSQL с БД `hunttech`;
- свободно не менее 6 ГБ RAM и достаточно места для heap dump;
- чистое рабочее дерево Git.

## Что делает scripts/start-app.sh

1. Проверяет PostgreSQL.
2. Останавливает процессы Tomcat проекта на портах 8080 и 8787.
3. Удаляет старые exploded-приложения `app`, `app-core`, `hrm`, `hrm-core`.
4. Очищает соответствующие каталоги `work` и содержимое `temp`.
5. Выполняет `./gradlew clean deploy -x test`.
6. Создаёт локальный `${app.home}/local.app.properties` с `cuba.schedulingActive=false`.
7. Запускает Tomcat с диагностическими параметрами heap.
8. Ожидает HTTP 200.
9. Сохраняет `VM.command_line`, `VM.flags`, `GC.heap_info` и стартовую гистограмму классов.

## Параметры JVM по умолчанию

```text
-Xms1024m
-Xmx4096m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=deploy/tomcat/logs/heapdumps
-Xlog:gc*:file=deploy/tomcat/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=20M
```

Переопределение:

```bash
LOCAL_JAVA_XMS=1536m \
LOCAL_JAVA_XMX=6144m \
./scripts/start-app.sh
```

## Локальное отключение scheduler и FTS

По умолчанию скрипт записывает:

```properties
cuba.schedulingActive=false
```

Это временно отключает все scheduled tasks в локальном окружении, включая `cuba_FtsManager.processQueue`. Production-конфигурация не изменяется.

Для отдельной проверки планировщика:

```bash
LOCAL_SCHEDULING_ACTIVE=true ./scripts/start-app.sh
```

Включать планировщик следует только после успешной проверки `JobCandidateEdit` без OOM.

## Порядок проверки

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)

./gradlew :app-core:test \
  --tests "com.company.hunttech.core.PdfParserServiceBeanTest" \
  --no-daemon

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon

chmod +x scripts/start-app.sh
./scripts/start-app.sh
```

После HTTP 200:

1. Открыть существующего кандидата с объёмным резюме.
2. Дождаться фонового формирования блока навыков.
3. Закрыть и повторно открыть карточку не менее пяти раз.
4. Создать нового кандидата и закрыть форму без сохранения.
5. Проверить все вкладки формы.
6. Убедиться, что в `catalina.out` отсутствуют новые OOM и массовые `NullPointerException` из `PdfParserServiceBean.parseSkillTree`.

## Диагностические файлы

```text
deploy/tomcat/logs/catalina.out
deploy/tomcat/logs/gc.log
deploy/tomcat/logs/heapdumps/
deploy/tomcat/logs/diagnostics/jvm-command-line.txt
deploy/tomcat/logs/diagnostics/jvm-flags.txt
deploy/tomcat/logs/diagnostics/heap-info.txt
deploy/tomcat/logs/diagnostics/class-histogram-startup.txt
```

При повторном OOM не добавлять `.hprof` в Git. Для анализа передать `Leak_Suspects.zip`, полученный в Eclipse MAT, и фрагмент `catalina.out`.

## Критерии успешности

- `PdfParserServiceBeanTest` — зелёный;
- восемь проверок `ScreenViewIntegrityTest` — зелёные;
- `BUILD SUCCESSFUL`;
- HTTP 200;
- `JobCandidateEdit` открывается без OOM;
- нет стека `Skillsbar.generateSkillLabels → PdfParserService.parseSkillTree` в синхронном `onBeforeShow`;
- нет серии повторяющихся NPE по каждой записи `SkillTree`;
- фактический `-Xmx` подтверждён в `jvm-flags.txt`.

## Откат локальных диагностических настроек

Удалить или изменить строку в:

```text
deploy/tomcat/app_home/local.app.properties
```

Для возврата стандартного heap запустить:

```bash
LOCAL_JAVA_XMS=512m LOCAL_JAVA_XMX=2048m ./scripts/start-app.sh
```

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-14 | Добавлена процедура чистого redeploy, локального отключения scheduler/FTS, heap dump, GC-логирования и проверки JobCandidateEdit после OOM. |
