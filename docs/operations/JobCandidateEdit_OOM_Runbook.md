# Локальная диагностика OutOfMemoryError при открытии JobCandidateEdit

## Назначение

Runbook используется для воспроизводимой проверки карточки кандидата HRM HuntTech после ошибок `java.lang.OutOfMemoryError: Java heap space`. Процедура исключает старые классы Tomcat, фиксирует фактический heap и позволяет отдельно сравнить работу формы с включённым и выключенным scheduler.

Обычный локальный запуск должен сохранять бизнес-функциональность scheduled tasks. Отключение scheduler допустимо только как временный диагностический эксперимент.

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
6. Создаёт или обновляет локальный `${app.home}/local.app.properties`.
7. По умолчанию записывает `cuba.schedulingActive=true`.
8. Запускает Tomcat с диагностическими параметрами heap.
9. Ожидает HTTP 200.
10. Сохраняет `VM.command_line`, `VM.flags`, `GC.heap_info` и стартовую гистограмму классов.

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

## Режимы scheduler и FTS

### Штатный локальный запуск

```bash
./scripts/start-app.sh
```

Скрипт использует:

```properties
cuba.schedulingActive=true
```

В этом режиме работают FTS и остальные scheduled tasks HRM HuntTech. Именно этот режим используется для итоговой функциональной проверки.

### Изолированная диагностика OOM

Чтобы проверить влияние параллельного FTS на heap:

```bash
LOCAL_SCHEDULING_ACTIVE=false ./scripts/start-app.sh
```

Скрипт временно запишет:

```properties
cuba.schedulingActive=false
```

Это отключает все scheduled tasks, а не только FTS. Такой запуск не является штатным и не используется для проверки фоновых бизнес-процессов. После эксперимента приложение необходимо снова запустить без переменной либо с `LOCAL_SCHEDULING_ACTIVE=true`.

## Порядок проверки

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew :app-core:test \
  --tests "com.company.hunttech.core.PdfParserServiceBeanTest" \
  --no-daemon \
  --stacktrace

./gradlew test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon \
  --stacktrace

./gradlew :app-web:buildScssThemes \
  --no-daemon \
  --stacktrace

chmod +x scripts/start-app.sh
./scripts/start-app.sh
```

## Функциональная проверка после HTTP 200

1. Открыть существующего кандидата с объёмным резюме.
2. Зафиксировать время до полного отображения формы.
3. Дождаться фонового формирования блока навыков.
4. Открыть вкладку «Позиции и вакансии».
5. Убедиться, что история рассмотрения содержит вакансии, дату, последнее взаимодействие, рекрутера и ресерчера.
6. Проверить список подходящих вакансий и tooltip строки.
7. Открыть взаимодействия выбранной вакансии кнопкой «Просмотр».
8. Проверить вкладки «Взаимодействия», «Резюме и файлы», «Контакты», «Социальные сети», «Комментарии» и «История».
9. Закрыть и повторно открыть карточку не менее пяти раз.
10. Создать нового кандидата и закрыть форму без сохранения.
11. Проверить сохранение существующего кандидата и отмену изменений.
12. Проверить загрузку и очистку фотографии.

## Проверка бизнес-контракта навыков

В `CandidateCVEdit`:

1. Открыть существующее резюме.
2. Выполнить «Пересканировать резюме».
3. Убедиться, что в таблице навыков отображаются `specialisation`, `wikiPage` и комментарий.
4. Сохранить резюме.
5. Повторно открыть его и убедиться, что `CandidateCV.skillTree` сохранился.

Эта проверка подтверждает, что `PdfParserService` возвращает реальные сущности `SkillTree`, а не transient-объекты.

## Анализ catalina.out

После проверки нового запуска:

```bash
grep -nE \
  'OutOfMemoryError|Java heap space|GuiDevelopmentException|DevelopmentException|NullPointerException|NoSuchMethodError|PdfParserServiceBean|cuba_FtsManager.processQueue' \
  deploy/tomcat/logs/catalina.out
```

Критерии:

- отсутствуют новые `OutOfMemoryError` и `Java heap space`;
- отсутствует синхронная цепочка анализа навыков из `JobCandidateEdit.onBeforeShow`;
- отсутствуют серии `NullPointerException` для строк `SkillTree`;
- нет ошибок `unfetched attribute` при tooltip подходящих вакансий;
- нет ошибок отсутствующего параметра `candidate`, `positionType` или `positionTypes`;
- при штатном запуске scheduler активен;
- при отдельном запуске с `LOCAL_SCHEDULING_ACTIVE=false` вызовы FTS отсутствуют.

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
- `buildScssThemes` — `BUILD SUCCESSFUL`;
- полный `clean deploy` — `BUILD SUCCESSFUL`;
- HTTP 200;
- `JobCandidateEdit` открывается пять раз без OOM;
- фоновые метки навыков появляются после открытия формы;
- вкладка «Позиции и вакансии» загружается только при первом выборе;
- история и подходящие вакансии отображаются без unfetched/null ошибок;
- пересканированные навыки `CandidateCV` сохраняются и повторно открываются;
- `cuba.schedulingActive=true` при штатном запуске;
- фактический `-Xmx` подтверждён в `jvm-flags.txt`.

## Возврат из диагностического режима

После запуска с отключённым scheduler выполнить:

```bash
LOCAL_SCHEDULING_ACTIVE=true ./scripts/start-app.sh
```

Проверить:

```bash
grep '^cuba.schedulingActive=' deploy/tomcat/app_home/local.app.properties
```

Ожидаемое значение:

```text
cuba.schedulingActive=true
```

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-14 | Восстановлен штатный scheduler по умолчанию, отключение FTS выделено в явный диагностический режим; добавлены проверки вкладки позиций и сохранения `CandidateCV.skillTree`. |
| 2026-07-14 | Добавлена процедура чистого redeploy, heap dump, GC-логирования и первичной проверки JobCandidateEdit после OOM. |
