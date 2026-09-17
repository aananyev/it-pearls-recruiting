# ЗАДАНИЕ ANTIGRAVITY — LLM-chat: роли, Local Chat и manager/write Hermes

Дата: 2026-09-17
Репозиторий: `aananyev/it-pearls-recruiting`
Базовая ветка ChatGPT: `agent/llm-chat-manager-hermes-access`

## Цель
Довести до production-ready состояния разграничение вкладок LLM-chat и подключить отдельный Hermes-контур, который может инициировать разрешённые изменения HRM для менеджеров/директоров.

## Уже сделано ChatGPT
1. В `llm-chat-screen.xml` добавлена третья вкладка `hermesManagerChatTab` — «Hermes — управление HRM».
2. До безопасного backend-подключения её поле ввода и Send намеренно отключены.
3. В `web-permissions.xml` зарегистрированы specific permissions:
   - `hunttech.ai.useLocalChat`
   - `hunttech.ai.useManagerHermesWrite`
4. Добавлены RU captions/предупреждения.

## Обязательная архитектура безопасности
НЕЛЬЗЯ реализовывать manager Hermes как браузерный прямой доступ к PostgreSQL или как unrestricted DB-credential внутри контейнера.

Причина: прямой SQL не применяет CUBA entity/attribute/access-group permissions конкретного пользователя.

Любые INSERT/UPDATE должны выполняться внутри HRM/CUBA server-side security-context текущего пользователя либо через делегированный серверный контракт, который однозначно восстанавливает пользователя и повторно проверяет права.

DELETE запрещён безусловно на всех уровнях: UI, service API, Hermes tools и DB account. Не реализовывать delete-tool и не принимать SQL DELETE/DDL/TRUNCATE.

## Шаг 1 — синхронизация
1. Работай только в `/Users/alekseyananyev/StudioProjects/hrm-antigravity` и своей ветке `agent/antigravity-dev`.
2. `git fetch origin`.
3. Забери изменения ветки `origin/agent/llm-chat-manager-hermes-access` в свою ветку (merge/cherry-pick без переписывания чужих изменений).
4. Перед правками проверь текущий HEAD master и возможные свежие изменения LLM-chat.

## Шаг 2 — определить реальные роли CUBA
Подключись к локальной/разрешённой БД настроек пользователей и ролей, затем установи ТОЧНЫЕ role codes/UUID/наименования, соответствующие бизнес-понятиям «Менеджер» и «Директор».

Не хардкодь русские captions ролей в Java как основу авторизации. Роли должны получить specific permissions:
- менеджер/директор: `hunttech.ai.useLocalChat = allowed`
- менеджер/директор: `hunttech.ai.useManagerHermesWrite = allowed`
- остальные пользователи: эти grants отсутствуют.

Проверь также возможность применить UI-component restriction к:
- `chatTabSheet[localChatTab]`
- `chatTabSheet[hermesManagerChatTab]`

Основной runtime-check всё равно должен быть через `Security.isSpecificPermitted(...)` в контроллере/сервисе.

## Шаг 3 — логика видимости LLM-chat
В `LlmChatScreen`:
1. `localChatTab` показывать только если `security.isSpecificPermitted("hunttech.ai.useLocalChat")`.
2. `hermesManagerChatTab` показывать только если `security.isSpecificPermitted("hunttech.ai.useManagerHermesWrite")`.
3. Обычный пользователь не должен видеть «Локальный чат» и manager/write Hermes.
4. Менеджер/директор должен видеть обе эти вкладки.
5. Если скрытая вкладка была selected по сохранённым UI settings — переключиться на доступную вкладку безопасно.
6. Проверять permission не только в UI: manager/write backend service обязан повторно делать server-side check.

## Шаг 4 — разведка production Hermes (ТОЛЬКО read-only reconnaissance)
По разрешённому SSH/DevOps-доступу на production:
1. Выполни `docker ps --format '{{.Names}}\t{{.Image}}\t{{.Ports}}'`.
2. Подтверди контейнер `hrm-viewer`.
3. Найди второй Hermes-контейнер — тот, который НЕ `hrm-viewer` и предназначен/способен к изменению HRM.
4. Зафиксируй:
   - container name;
   - image/tag;
   - exposed/internal ports;
   - network name;
   - health endpoint;
   - фактический chat/API contract;
   - auth mechanism;
   - volumes/config/env names БЕЗ публикации секретов.
5. НИЧЕГО на production не изменяй на этапе разведки. Не рестартуй контейнеры и не меняй env.

## Шаг 5 — подключить manager/write Hermes
После выяснения API реализуй отдельный server-side adapter/service, не смешивая его с локальным `LlmChatService`.

Требования:
- URL/port/token — только config/env, никаких секретов в git;
- timeout, обработка 4xx/5xx, correlation/request id;
- server-side check `hunttech.ai.useManagerHermesWrite` до каждого запроса;
- Hermes получает минимальный контекст пользователя, необходимый для делегирования;
- никакого произвольного SQL из браузера.

Предпочтительный контракт: Hermes формирует структурированное намерение/операцию (entity + CREATE/UPDATE + id + fields), а HRM выполняет mutation через CUBA `DataManager`/service под security-context пользователя.

Для каждой mutation:
1. CREATE: `security.isEntityOpPermitted(EntityClass, EntityOp.CREATE)`.
2. UPDATE: `security.isEntityOpPermitted(EntityClass, EntityOp.UPDATE)`.
3. Для каждого изменяемого атрибута: `security.isEntityAttrPermitted(..., EntityAttrAccess.MODIFY)` или эквивалент API вашей версии CUBA.
4. DataManager должен применять constraints/access groups текущего пользователя.
5. DELETE: всегда reject независимо от роли/permission.
6. Любая неизвестная entity/field/operation: reject deny-by-default.

Начальный allowlist сущностей — только реально требуемые сценарии, прежде всего `OpenPosition`. Не делай универсальный unrestricted CRUD gateway.

## Шаг 6 — UX manager Hermes
После успешного backend binding:
- включи `hermesManagerInputArea` и `hermesManagerSendBtn` только при наличии permission и доступном Hermes health;
- история/ответы manager Hermes должны быть отделены от local chat;
- перед изменением показывай понятный preview намерения, если API это поддерживает;
- после успешного CREATE/UPDATE выводи ссылку на HRM entity через существующий механизм `hunttechOpenHrmEntity`;
- ошибки прав объясняй как отказ CUBA permissions, не как техническую ошибку БД.

## Шаг 7 — тесты
Обязательно добавить/обновить контрактные тесты:
1. обычный пользователь: localChatTab hidden, manager tab hidden;
2. manager/director permission: localChatTab visible, manager tab visible;
3. manager backend без specific permission: 403/exception;
4. CREATE без EntityOp.CREATE: reject;
5. UPDATE без EntityOp.UPDATE: reject;
6. изменение закрытого атрибута: reject;
7. DELETE всегда reject;
8. невозможность отправить произвольный SQL/DDL/TRUNCATE;
9. Hermes unavailable: UI остаётся безопасным и не включает write controls.

Запусти профильные тесты + `ScreenViewIntegrityTest` и сборку проекта.

## Шаг 8 — PR и передача Hermes-1
Не push в master напрямую. Создай/обнови PR из `agent/antigravity-dev` в `master`, приложи:
- точное имя второго production-контейнера;
- архитектуру делегирования CUBA permissions;
- список изменённых файлов;
- результаты тестов;
- подтверждение DELETE-deny;
- подтверждение, что production на этапе разведки не менялся.

Дальше передай PR Hermes-1 для штатного merge + local deploy согласно `.ai/instructions/hermes1-profile.md`.
