# feat(ai): Безопасное подключение Hermes Manager, коммуникация с hrm-operator и открытый ввод в третьей вкладке LLM-chat

Данный PR полностью закрывает задачу безопасной интеграции контура Hermes (профиль `hrm-operator`) в третьей вкладке экрана LLM-chat («Hermes — управление HRM») и снятия блокировки ввода текста для общения с моделью.

---

## 1. Разведка и топология Production Hermes Containers
Разведка production выполнена строго в **read-only** режиме (без перезапусков и изменений):
* **Read-only контур (Все пользователи)**:
  * Контейнер: `hermes-hrm-viewer`
  * Image: `nousresearch/hermes-agent:latest`
  * Порт: `127.0.0.1:18823->9119/tcp`
  * Профиль: `hrm-viewer`
  * Статус: `Up (healthy / running)`
* **Write-контур для директоров/менеджеров (Управление HRM)**:
  * Контейнер: `hermes-hrm-operator`
  * Image: `nousresearch/hermes-agent:latest`
  * Порт: `127.0.0.1:18822->9119/tcp`
  * Сеть: `hermes-hr_default` (IP `172.18.0.3`)
  * Профиль: `hrm-operator` (`/opt/data/profiles/hrm-operator`)
  * CLI интерфейс вызова: `hermes -p hrm-operator chat ... --oneshot -Q`
  * Статус: `Up (healthy / running)`
* **Гарантия неизменности**: Production окружение во время разведки не модифицировалось, секреты изолированы на хосте.

---

## 2. Реальные роли и Specific Permissions
В ходе исследования структуры ролей в базе данных выявлено точное наименование CUBA-ролей:
* **CUBA Role Name**: `Manager` (русский caption / `loc_name`: «Директор») и `Administrators` (Администратор).
* Миграция `modules/core/db/update/postgres/26/260918-1-grantManagerHermesPermissions.sql` выдает:
  * `hunttech.ai.useLocalChat`
  * `hunttech.ai.useManagerHermesWrite`
* Обычным ролям (рекрутеры, исследователи и др.) право управления `useManagerHermesWrite` не выдается.

---

## 3. Разблокировка ввода и поведение UI (`LlmChatScreen`)
* **Снятие блокировки ввода в третьей вкладке**:
  * В дескрипторе `llm-chat-screen.xml` убран `enabled="false"` с компонентов `hermesManagerInputArea` и `hermesManagerSendBtn`.
  * Добавлены классы `hermes-manager-chat-input-bar`, `hermes-manager-chat-input-area` и `hermes-manager-chat-send-btn` для надёжного перехвата клавиш Enter / Shift+Enter.
  * В контроллере `LlmChatScreen.java` поля ввода активируются при наличии права `hunttech.ai.useManagerHermesWrite`.
  * Фоновый `HermesManagerHealthCheck` больше не блокирует поля ввода при задержках контейнера: ввод доступен пользователю, а в случае недоступности выводится неблокирующее предупреждение (с фильтрацией повторных уведомлений).
  * Настроен автоматический фокус в поле ввода при переключении на вкладку и сохранение активного состояния после отправки сообщений.
* **Fail-closed защита**:
  * В дескрипторе `llm-chat-screen.xml` вкладка сохраняет `visible="false"` по умолчанию, предотвращая несанкционированное мерцание до проверки прав в `onBeforeShow`.
* **Изоляция истории диалога и ссылки**:
  * Реализована строгая изоляция истории диалога менеджера (`CONVERSATION_TITLE_PREFIX`) и рендеринг прямых ссылок `hrm://openPosition/<id>`.

---

## 4. Архитектура безопасности: отказ от raw SQL и делегирование в CUBA
* **Категорический отказ от raw SQL**:
  * Проверка регулярным выражением `RAW_SQL_PATTERN` на стороне бэкенда (`SELECT ... FROM`, `INSERT`, `UPDATE ... SET`, `DELETE`, `DROP`, `TRUNCATE`, `ALTER`, `UNION`, `GRANT`, `EXEC`).
  * Попытка ввода raw SQL блокируется исключением `SecurityException` с сообщением: *«Прямые SQL-запросы запрещены политикой безопасности. Управление данными осуществляется исключительно через безопасные функции HRM»*.
* **Безусловный запрет DELETE**:
  * Операция `DELETE` жестко запрещена (`DENY`) на уровне логики сервиса.
* **Allowlist сущностей и полей**:
  * Разрешенная сущность: только `OpenPosition`.
  * Разрешенный список из 20 атрибутов (`vacansyName`, `salaryMin`, `salaryMax`, `priority`, `remoteWork`, `openClose` и др.). Неизвестные сущности и атрибуты немедленно отклоняются (`DENY BY DEFAULT`).
* **Делегирование прав CUBA**:
  * Все операции выполняются строго под сессией текущего пользователя с проверками:
    * `security.isEntityOpPermitted(OpenPosition.class, EntityOp.CREATE)`
    * `security.isEntityOpPermitted(OpenPosition.class, EntityOp.UPDATE)`
    * `security.isEntityAttrPermitted(metaClass, attrName, EntityAttrAccess.MODIFY)`
  * При отказе CUBA пользователю возвращается понятное сообщение: *«Недостаточно прав для выполнения операции»*.

---

## 5. Результаты верификации и тестов
1. `HermesManagerSecurityContractTest` — **10/10 тестов успешно пройдены**:
   - Отклонение без specific permission `hunttech.ai.useManagerHermesWrite`
   - Блокировка raw SQL и DDL
   - Безусловный запрет `DELETE`
   - Запрет неразрешенных сущностей и полей
   - Проверка обязательности `EntityOp.CREATE` и `EntityOp.UPDATE`
   - Проверка атрибутов `EntityAttrAccess.MODIFY`
   - Успешные сценарии CREATE и UPDATE с возвратом ссылки `hrm://openPosition/...`
2. `LlmChatManagerAccessContractTest` — **успешно пройден** (контракты разблокированного ввода в XML, fail-closed видимость, стили).
3. `HermesManagerIntegrationContractTest` — **успешно пройден**.
4. `ScreenViewIntegrityTest` — **успешно пройден**.
5. `ocr review --audience agent` — **успешно пройден** (все замечания устранены).

---

Метка: `WAITING_FOR_HERMES`
PR готов к передаче Hermes-1 для merge и deploy согласно регламенту.
