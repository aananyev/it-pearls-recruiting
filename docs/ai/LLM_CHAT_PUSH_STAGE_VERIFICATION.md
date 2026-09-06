# Проверка Vaadin push для LLM-чата

Дата проверки: 2026-09-05

## Статус

Частично подтверждено локально. Полная staging-приёмка через reverse proxy/балансировщик ожидает отдельный staging URL и тестовую учётную запись.

## Что проверено

- Текущая ветка `feat/llm-chat`, локальный кандидат для staging `dd19ea8`; до staging нужен опубликованный и approved commit PR.
- 2026-09-06 widgetset текущей ветки собран Gradle-задачами `:app-web:processResources :app-web-toolkit:buildWidgetSet :app-web-toolkit:webArchive`.
- Свежий артефакт `app-web-toolkit-0.468-SNAPSHOT-client.jar` содержит `AppWidgetSet.nocache.js`; `app-web-0.468-SNAPSHOT.jar` содержит `LlmChatLauncherExtension` и `llm-chat-launcher.js`.
- Локальный Vaadin push asset на `http://127.0.0.1:8080/hrm/VAADIN/vaadinPush.debug.js` возвращает HTTP 200.
- Корректный WebSocket handshake на `/hrm/PUSH/` возвращает HTTP 101 и Vaadin push frame.
- Concurrency probe: 8 параллельных handshake-запросов, результат `8/8 HTTP 101`.

Для повторения read-only transport smoke используется `scripts/verify-llm-chat-staging.sh`:

```bash
scripts/verify-llm-chat-staging.sh https://staging.example/hrm 8
```

Скрипт проверяет только push asset и WebSocket handshake. Он не выполняет вход, не отправляет сообщения, не вызывает LLM и не меняет данные.

## Ограничения evidence

- Локальный Tomcat запущен из `/Users/alekseyananyev/StudioProjects/hunttech_recruiting`, commit `2fc96ead`, версия `0.434`; это не текущий PR и он не использовался как доказательство поведения нового `LlmChatScreen`.
- Локальный endpoint проверен напрямую, без reverse proxy и балансировщика.
- Страница открывается без авторизации; authenticated push-событие и обновление floating-чата через UI не проверялись, потому что staging URL и тестовые credentials не предоставлены.
- 8 handshakes — smoke/concurrency probe transport, а не нагрузочный тест production-профиля.

## Обязательная staging-проверка перед production

1. Развернуть опубликованный approved commit PR (на момент обновления локальный кандидат `dd19ea8`) в отдельном staging.
2. Проверить HTTP 200 widgetset/push asset и WebSocket `101` через фактический proxy/балансировщик.
3. Выполнить authenticated сценарий: открыть чат, отправить тестовый запрос, убедиться в push-обновлении дельт, завершении и отсутствии повторного provider-вызова.
4. Разорвать push-канал и убедиться, что recovery polling 3 секунды завершает отображение без дубля сообщения.
5. Проверить ограниченный reconnect/proxy smoke, sticky-session/affinity и таймауты proxy. Массовый прогон 20–50 UI-сессий отменён владельцем и не является обязательным критерием текущего этапа.
6. Зафиксировать latency первого delta, reconnect rate, error rate и отсутствие утечек между пользователями.

## Вывод

Транспортный слой локального Tomcat совместим с WebSocket и принимает параллельные подключения. Применимость к текущему PR через staging proxy, а также authenticated UI/load acceptance остаются незакрытыми до появления staging URL, тестовой учётной записи и параметров proxy.
