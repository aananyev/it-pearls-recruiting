# Спецификация: Разграничение вкладок LLM-Chat и подключение Hermes Manager (Write-контур)

## 1. Общее назначение и цели

Функционал обеспечивает безопасное разграничение доступа к вкладкам в экране LLM-чата (`LlmChatScreen`) и подключение специализированного контура Hermes для руководства компании (роль «Директор» / `Manager` в CUBA) с возможностью безопасных операций создания и обновления сущностей HRM без предоставления универсального SQL-шлюза.

---

## 2. Разграничение ролей и специфических прав

В системе CUBA зарегистрированы специфические права (`Specific Permission`):
- `hunttech.ai.useLocalChat` — доступ к локальному чату разработки / тестирования.
- `hunttech.ai.useManagerHermesWrite` — доступ к контуру «Hermes — управление HRM» с возможностью безопасных CREATE/UPDATE мутаций данных.

### Бизнес-роли и матрица доступа:
1. **Обычный пользователь (Рекрутер / Исследователь / Собеседующий)**:
   - Вкладка «Локальный чат» — **скрыта** (`visible="false"`).
   - Вкладка «Hermes (Чтение HRM)» — **доступна** (контур `hrm-viewer`).
   - Вкладка «Hermes — управление HRM» — **доступна** для коммуникации с `hrm-operator`.
2. **Менеджер / Директор (CUBA Role: `Manager`), Администратор (CUBA Role: `Administrators`)**:
   - Вкладка «Локальный чат» — **доступна**.
   - Вкладка «Hermes (Чтение HRM)» — **доступна**.
   - Вкладка «Hermes — управление HRM» — **доступна** для коммуникации и безопасных мутаций через CUBA DataManager.

### Миграция БД
Для ролей `Manager` и `Administrators` назначены права через SQL-миграцию:
`modules/core/db/update/postgres/26/260918-1-grantManagerHermesPermissions.sql`

---

## 3. Топология контейнеров Hermes на Production

Разведка production окружения выявила два изолированных Docker-контейнера `nousresearch/hermes-agent:latest`:

| Параметр | Read-only контур (Все пользователи) | Write-контур (Директора / Руководство) |
|---|---|---|
| **Имя контейнера** | `hermes-hrm-viewer` | `hermes-hrm-operator` |
| **Порт хоста** | `127.0.0.1:18823` | `127.0.0.1:18822` |
| **Внутренний порт** | `9119/tcp` | `9119/tcp` |
| **Сеть** | `hermes-hr_default` | `hermes-hr_default` |
| **Профиль агента** | `hrm-viewer` | `hrm-operator` |
| **Каталог профиля** | `/opt/data/profiles/hrm-viewer` | `/opt/data/profiles/hrm-operator` |
| **Режим запуска CLI** | `hermes -p hrm-viewer chat ... --oneshot -Q` | `hermes -p hrm-operator chat ... --oneshot -Q` |
| **Статус Health** | `Up (healthy / running)` | `Up (healthy / running)` |

> **Безопасность**: Production конфигурация во время аудита не изменялась, секреты и токены изолированы в переменных окружения хоста.

---

## 4. Архитектура безопасности и модель мутаций

### 4.1. Категорический отказ от SQL-шлюза
- Передача raw SQL из UI или интерпретатора LLM **строго блокируется** на уровне сервиса с помощью `RAW_SQL_PATTERN`.
- Запрещены любые ключевые конструкции: `SELECT ... FROM`, `INSERT INTO`, `UPDATE ... SET`, `DELETE FROM`, `DROP`, `ALTER`, `TRUNCATE`, `UNION`, `GRANT`, `REVOKE`, `EXEC`.
- При обнаружении сырого SQL генерируется `SecurityException` с понятным сообщением: *«Прямые SQL-запросы запрещены политикой безопасности. Управление данными осуществляется исключительно через безопасные функции HRM»*.

### 4.2. Безусловный запрет DELETE
- Операция `DELETE` **безусловно запрещена** (`DENY`) на уровне бизнес-логики `HermesManagerChatServiceBean`, даже если пользователь или роль обладают техническим `EntityOp.DELETE`.
- Tool для удаления данных для LLM не предоставляется.

### 4.3. Белый список сущностей и атрибутов (Allowlist)
- Разрешенная сущность первого этапа: `OpenPosition` (`hunttech_OpenPosition`). Все прочие сущности отклоняются (`DENY BY DEFAULT`).
- Разрешенные атрибуты для изменения:
  `vacansyName`, `shortDescription`, `comment`, `commentEn`, `salaryMin`, `salaryMax`, `salaryComment`, `salaryFixLimit`, `salaryCandidateRequest`, `salaryIE`, `remoteWork`, `remoteComment`, `registrationForWork`, `priority`, `priorityComment`, `commandCandidate`, `numberPosition`, `workExperience`, `commandExperience`, `openClose`, `signDraft`.
- Любой неуказанный в allowlist атрибут отклоняется.

### 4.4. Делегирование прав CUBA Security под контекстом пользователя
- Все операции выполняются строго под сессией текущего авторизованного пользователя:
  - Проверка `security.isEntityOpPermitted(OpenPosition.class, EntityOp.CREATE)` перед созданием.
  - Проверка `security.isEntityOpPermitted(OpenPosition.class, EntityOp.UPDATE)` перед обновлением.
  - Проверка каждого атрибута через `security.isEntityAttrPermitted(metaClass, attrName, EntityAttrAccess.MODIFY)`.
  - При отказе CUBA формируется исключение `SecurityException("Недостаточно прав для выполнения операции")`, без раскрытия внутренней структуры БД.
  - Мутации осуществляются через `dataManager.commit(...)` с сохранением аудита (`ExtUser`, `createTs`, `createdBy`, `updateTs`, `updatedBy`).

---

## 5. Компоненты решения

1. **Конфигурация**: `HunttechHermesManagerConfig.java` (`modules/global/src/com/company/hunttech/config/HunttechHermesManagerConfig.java`).
   - Настройки: хост, порт SSH, пользователь SSH, имя контейнера (`hermes-hrm-operator`), профиль (`hrm-operator`), таймауты.
2. **Сервис контракта**: `HermesManagerChatService.java` (`modules/global/src/com/company/hunttech/service/HermesManagerChatService.java`).
   - Удаленный интерфейс для web-клиента.
3. **Реализация сервиса**: `HermesManagerChatServiceBean.java` (`modules/core/src/com/company/hunttech/service/HermesManagerChatServiceBean.java`).
   - Реализует валидацию прав, санитизацию, исполнение запросов к контейнеру `hermes-hrm-operator`, разбор JSON-интентов мутаций (`CREATE`, `UPDATE`), вызовы CUBA DataManager.
4. **Spring Web Proxy**: `modules/web/src/com/company/hunttech/web-spring.xml` (`hunttech_HermesManagerChatService`).
5. **Экран чата**:
   - `llm-chat-screen.xml`: добавлены стили, контейнеры, статус-бар, информационные предупреждения, инпут и кнопка отправки вкладки `hermesManagerChatTab`.
   - `LlmChatScreen.java`:
     - Проверка `security.isSpecificPermitted` для `localChatTab` и `hermesManagerChatTab`.
     - Автопереключение на первую доступную вкладку при скрытии выбранной.
     - Асинхронный health-check контейнера `hermes-hrm-operator` и безопасная блокировка controls при недоступности.
     - Изоляция истории бесед менеджера (`CONVERSATION_TITLE_PREFIX`).
     - Поддержка ссылок `hrm://openPosition/<id>` и их открытие в интерфейсе HRM.

---

## 6. Тестирование и верификация

1. **`HermesManagerSecurityContractTest`** (`modules/core/test/com/company/hunttech/service/HermesManagerSecurityContractTest.java`):
   - Запрет неавторизованного доступа без `hunttech.ai.useManagerHermesWrite`.
   - Запрет raw SQL запросов (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `DROP`, `TRUNCATE`, `ALTER`).
   - Безусловный запрет `DELETE`.
   - Отклонение неразрешенных сущностей и неизвестных атрибутов.
   - Проверка обязательности `EntityOp.CREATE` и `EntityOp.UPDATE`.
   - Проверка требования `EntityAttrAccess.MODIFY` на атрибуты.
   - Успешные сценарии CREATE и UPDATE с возвратом ссылки `hrm://openPosition/<id>`.
2. **`HermesManagerIntegrationContractTest`** (`modules/web/test/com/company/hunttech/web/screens/llmchat/HermesManagerIntegrationContractTest.java`):
   - Проверка видимости и сокрытия вкладок для обычного пользователя и менеджера.
   - Проверка блокировки ввода при отсутствии прав или недоступности бэкенда.
3. **`ScreenViewIntegrityTest`**:
   - Пройдена проверка целостности представлений экранов CUBA без ошибок LazyInitializationException и UnfetchedAttribute.
