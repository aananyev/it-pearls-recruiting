# Отчёт Hermes-1: синхронизация deployment-артефактов LLM-чата (PR #240)

Дата: 07.09.2026, 15:25 (+04)

## 1. Синхронизация с origin/master
- Рабочая копия `hunttech_recruiting`: master, актуален.
- SHA на момент проверки: `14eb50fc` (PR #245) → после коммита фиксов деплоя `16e47fac` (запушен в origin/master).
- PR #240 (компактный launcher LLM-чата) присутствует в истории master (`27d4614b` — синхронизация antigravity с PR #240/#241).
- Правка исходников НЕ потребовалась.

## 2. Песочница ChatGPT (`/Users/alekseyananyev/StudioProjects/chatgpt`)
- **Исходники идентичны master**: `diff -rq` web/core/gui/themes/messages — 0 расхождений (кроме .DS_Store).
- **Фактическое состояние JAR расходится с постановкой задачи**: в задаче указан старый `app-web-0.468-SNAPSHOT.jar`, фактически в песочнице уже развёрнут `app-web-0.485-SNAPSHOT.jar` (собран 07.09 14:49 — песочница была пересобрана ранее).
- Песочница запущена: Tomcat на **8081** (PID 76968), app_home отдельный (`chatgpt/deploy/app_home`), connectionUrlList → 8081 (корректно для песочницы).

## 3. tomcat-hermes (`hunttech_recruiting/deploy/tomcat-hermes`)
- Содержит артефакты от **17.08.2026**: `app-web-0.159-SNAPSHOT.jar`, `app-global/gui-0.164-SNAPSHOT.jar`.
- **Не запущен** (нет процесса), **не используется ни одним скриптом** (grep scripts/, .ai/ — 0 ссылок), логи обрываются на 18.08.
- Это мёртвая staging-копия месячной давности. Частично обновлять её (новый web-JAR поверх старого global/gui) — значит собрать нерабочую смесь; полный пересбор не имеет смысла для неиспользуемого окружения.
- **Рекомендация**: удалить каталог `deploy/tomcat-hermes` или пересобрать целиком, если он снова понадобится. Решение — за пользователем (удаление не выполнялось).

## 4. Проверка артефактов LLM-чата (JAR app-web-0.485-SNAPSHOT.jar, обе копии)
| Критерий | Результат | Доказательство |
|---|---|---|
| `modal="true"` | ✅ | `llm-chat-screen.xml` в JAR: `<dialogMode width="420px" height="560px" modal="true" resizable="true" closeable="true"/>` |
| Размер чата 420×560 | ✅ | те же атрибуты dialogMode |
| Draggable launcher | ✅ | `llm-chat-launcher.js` в JAR: drag-логика (dragging, clampPosition, localStorage позиции, класс `llm-chat-launcher-dragging`) |
| Отсутствие мобильного fullscreen | ✅ | 0 совпадений `fullscreen`/mobile-правил в launcher.js, LlmChatScreen.java, ExtMainScreen.java |
| Новый launcher вместо `llm-chat-launcher-bar` | ✅ | 0 совпадений `llm-chat-launcher-bar` в исходниках, JAR и скомпилированном CSS; новый launcher: `ExtMainScreen` → `llm-chat-launcher` / `llm-chat-launcher-window` + `LlmChatLauncherExtension` (@JavaScript llm-chat-launcher.js) |

## 5. Деплой и runtime-проверка (основной контур, 8080)
- Штатная сборка/деплой выполнены ранее в сессии: `clean deploy -x test` + `:app-web-toolkit:buildWidgetSet/deploy` (widgetset 3м01с), JAR `app-web-0.485-SNAPSHOT.jar` от 14:01.
- **Найдена и исправлена блокирующая проблема**: в `deploy/app_home/local.app.properties` осталась «единоразовая» строка `cuba.connectionUrlList=http://localhost:8081/hrm-core` — основной Tomcat на 8080 слал remoting-вызовы в песочницу ChatGPT (чужой core/БД-кэш). Убрана, Tomcat перезапущен.
- После перезапуска: `Server URLs: [http://localhost:8080/hrm-core/remoting]` ✅ (было 8081).
- HTTP: Main App(8080) 200, Widgetset 200, Sandbox App(8081) 200.
- Транспортный smoke LLM-чата (`scripts/verify-llm-chat-staging.sh`):
  - 8081 (песочница): push asset 200, WebSocket handshake 101, параллельные 8/8 — **PASS**
  - 8080 (основной): push asset 200, WebSocket handshake 101, параллельные 8/8 — **PASS**

## 6. Коммиты Hermes-1 в master
- `16e47fac` — fix(deploy): JPDA debug отключён по умолчанию (unset JPDA_OPTS, JPDA_ENABLED=true для включения), поддержка JPDA_OPTS в start-app.sh. Версия: 0.485 → 0.486 (pre-commit hook).

## Итог
- Deployment-артефакты основного контура и песочницы **актуальны** (0.485-SNAPSHOT, содержат PR #240).
- Все 5 критериев проверки LLM-чата **подтверждены** в JAR и исходниках.
- Runtime-проверка (HTTP + WebSocket smoke) — **PASS** на обоих портах.
- Единственный устаревший артефакт — мёртвый `deploy/tomcat-hermes` (0.159, 17.08); не используется, к удалению/пересборке требуется решение пользователя.
- Критический фикс: основной Tomcat больше не подключён к core песочницы (connectionUrlList 8081 → убран).