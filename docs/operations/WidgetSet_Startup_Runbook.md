# Восстановление Vaadin widgetset при локальном запуске HRM HuntTech

## Назначение и бизнес-смысл

Vaadin widgetset содержит клиентский JavaScript-код компонентов интерфейса HRM HuntTech. Без файла `AppWidgetSet.nocache.js` серверная часть CUBA может отвечать по HTTP, однако браузер не сможет инициализировать пользовательский интерфейс.

Runbook применяется при сообщении:

```text
Failed to load the widgetset: ./VAADIN/widgetsets/com.company.hunttech.web.toolkit.ui.AppWidgetSet/com.company.hunttech.web.toolkit.ui.AppWidgetSet.nocache.js
```

## Причина ошибки

Ошибка возникает, когда каталог развернутого web-приложения очищен, но custom widgetset не был повторно скомпилирован и скопирован в exploded-приложение перед запуском Tomcat.

Изменения Java-контроллеров экранов сами по себе не удаляют widgetset. Проблема относится к последовательности `clean → buildWidgetSet → deploy → start`.

Параметр после `?` в URL является cache-busting timestamp. Он не создаёт файл и не является причиной ошибки.

## Исправленный сценарий запуска

Используется скрипт:

```text
scripts/rebuild-widgetset-and-start.sh
```

Он последовательно:

1. останавливает Tomcat проекта;
2. удаляет старые exploded-приложения и кэш Tomcat;
3. выполняет `clean`;
4. компилирует Java web-модуля;
5. явно выполняет `:app-web-toolkit:buildWidgetSet`;
6. выполняет `deploy`;
7. проверяет наличие и ненулевой размер `AppWidgetSet.nocache.js`;
8. запускает Tomcat;
9. ждёт HTTP 200 от приложения;
10. проверяет HTTP 200 и содержимое самого JavaScript-файла.

## Запуск

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"
chmod +x scripts/rebuild-widgetset-and-start.sh
./scripts/rebuild-widgetset-and-start.sh
```

Для контекста `/hrm`:

```bash
APP_CONTEXT=hrm \
APP_URL=http://localhost:8080/hrm/ \
./scripts/rebuild-widgetset-and-start.sh
```

## Контрольные пути

Для контекста `/app`:

```text
deploy/tomcat/webapps/app/VAADIN/widgetsets/com.company.hunttech.web.toolkit.ui.AppWidgetSet/com.company.hunttech.web.toolkit.ui.AppWidgetSet.nocache.js
```

Для контекста `/hrm`:

```text
deploy/tomcat/webapps/hrm/VAADIN/widgetsets/com.company.hunttech.web.toolkit.ui.AppWidgetSet/com.company.hunttech.web.toolkit.ui.AppWidgetSet.nocache.js
```

## Ручная проверка

```bash
WIDGETSET='com.company.hunttech.web.toolkit.ui.AppWidgetSet'

test -s "deploy/tomcat/webapps/app/VAADIN/widgetsets/$WIDGETSET/$WIDGETSET.nocache.js"

curl -sS -o /tmp/app-widgetset.js -w 'HTTP %{http_code}\n' \
  "http://localhost:8080/app/VAADIN/widgetsets/$WIDGETSET/$WIDGETSET.nocache.js"

wc -c /tmp/app-widgetset.js
head -c 200 /tmp/app-widgetset.js
```

Критерии:

- файл существует и имеет ненулевой размер;
- URL widgetset возвращает HTTP 200;
- ответ содержит JavaScript, а не HTML-страницу 404;
- после полного обновления браузера сообщение `Failed to load the widgetset` отсутствует.

## Диагностика неуспешной сборки

```bash
./gradlew :app-web-toolkit:buildWidgetSet --no-daemon --stacktrace

find . \
  -path '*/VAADIN/widgetsets/*/*.nocache.js' \
  -type f \
  -print

tail -n 500 deploy/tomcat/logs/catalina.out
```

Нельзя копировать старый widgetset вручную из другой ветки или предыдущей сборки: он должен соответствовать текущему набору клиентских компонентов.

## Проверка после запуска

После успешной загрузки widgetset необходимо открыть `JobCandidateEdit` и проверить:

- существующего и нового кандидата;
- все вкладки формы;
- вкладку «Позиции и вакансии»;
- отсутствие `GuiDevelopmentException`, `DevelopmentException` и `OutOfMemoryError`;
- отсутствие новых сообщений `Failed to load the widgetset` в браузере и логах.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-14 | Добавлен обязательный этап `:app-web-toolkit:buildWidgetSet`, проверка развернутого `AppWidgetSet.nocache.js` и HTTP-контроль статического ресурса перед функциональным тестированием. |
