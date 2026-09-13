# Отчет реализации: Интеграция с сервисами Yandex 360 (Календарь CalDAV, Телемост, Wiki, Почта) и AI-оркестрация

**Дата**: 13 сентября 2026 г.  
**Роли**: Руководитель проектов, Аналитик, UI/UX-дизайнер, Java Backend-разработчик, Frontend-разработчик, Технический писатель, Автоматизированный тестировщик (QA).  
**Ветка**: `agent/antigravity-dev`  
**Контекст задачи**: Предоставление универсальной формы настройки подключения к сервисам экосистемы Яндекс для каждого пользователя (в окне `ExtSettingsWindow` и карточке `ExtUserEdit`), реализация Java-сервисов работы с CalDAV и Telemost API с шифрованием токенов, а также интеллектуальная оркестрация через локальный LLM-чат и агента Hermes с автоматическим созданием видеовстреч и фиксацией в истории взаимодействий `hunttech_IteractionList`.

---

## 1. Архитектура решения

### 1.1 Модель данных и персистентность
- **Сущность `UserYandexConfiguration`** (`modules/global/src/com/company/hunttech/entity/UserYandexConfiguration.java`):
  - Привязка 1-к-1 к пользователю `ExtUser`.
  - Поля аккаунта и защищенного доступа: `accountEmail`, `oauthTokenEncrypted`, `refreshTokenEncrypted`.
  - Настройки Яндекс.Календаря (CalDAV): `calendarBaseUrl`, `personalCalendarName`, `personalCalendarPath`, `clientInterviewCalendarName`, `clientInterviewCalendarPath`, `defaultTimeZone`.
  - Настройки Яндекс.Телемоста: `telemostBaseUrl`, `telemostAutoRecord`, `telemostAiSummary`, `telemostDefaultAccessLevel`.
  - Настройки корпоративной Яндекс.Вики: `wikiBaseUrl`, `wikiOrgId`, `wikiCollabId`.
  - Статусы подключения: `calendarConnected`, `telemostConnected`, `wikiConnected`, `lastSyncAt`.
- **Liquibase миграция**: `modules/core/db/changelog/260913-3-add-user-yandex-configuration.xml`, включена в `db.changelog-master.xml`.
- **Представление данных**: `userYandexConfiguration-view` в `views.xml`.

### 1.2 Сервисный слой (Core & Global)
- **`YandexIntegrationService` / `YandexIntegrationServiceBean`**:
  - Полная поддержка протокола CalDAV: обнаружение через `PROPFIND` (`calendar-home-set`), парсинг XML-ответов с фильтрацией `resourcetype`.
  - Безопасная отправка HTTP-запросов с поддержкой нестандартных методов WebDAV (`PROPFIND`, `REPORT`, `MKCALENDAR`) через reflection fallback.
  - Генерация iCalendar RFC 5545 с поддержкой часовых поясов (`Europe/Saratov`, `Europe/Moscow`, `UTC`), автоматическим добавлением Телемост-ссылки и описания.
  - Отправка инвайтов через CalDAV Outbox с заголовком `Origin: https://calendar.yandex.ru` участникам встречи.
  - Создание конференций Яндекс.Телемост через `POST /conferences` с настройками доступа (`anyone` / `organization`).
  - Шифрование и дешифрование токенов с использованием `AiSecretService`.
  - Диагностический метод `testConnection(userId, serviceType)` для проверки `AUTH`, `CALENDAR`, `TELEMOST`, `WIKI`.
- **`AiYandexOrchestrationService` / `AiYandexOrchestrationServiceBean`**:
  - Распознавание намерений пользователя на естественном русском языке:
    - Различение личного календаря (`PERSONAL`) и календаря собеседований с заказчиком (`CLIENT_INTERVIEW`, «Hunttech у заказчика»).
    - Извлечение ФИО кандидата и поиск карточки в `hunttech_JobCandidate`.
    - Парсинг даты и времени («завтра в 15:00», «послезавтра в 16:30», явные даты).
    - Определение необходимости видеозвонка (Телемост генерируется по умолчанию для собеседований, исключая явно очные встречи «в офисе» / «очно»).
  - Автоматическая фиксация созданных встреч в истории взаимодействий кандидата `hunttech_IteractionList` с типом «Собеседование» и ссылкой на Телемост.

### 1.3 Пользовательский интерфейс (Web)
- **Окно персональных настроек (`ExtSettingsWindow`)**:
  - Добавлена новая вкладка `yandexServicesTab` и боковая кнопка навигации `navTabYandex` («Сервисы Яндекс 360»).
  - Секции с аккордеонами и карточками:
    1. Авторизация и аккаунт Яндекс 360 (Email, OAuth токен, Refresh токен, кнопка «Проверить доступ»).
    2. Яндекс.Календарь (CalDAV URL, часовой пояс, названия календарей, кнопка «Найти календари»).
    3. Яндекс.Телемост (API URL, автозапись, AI-конспект, уровень доступа, кнопка «Тестовый звонок»).
    4. Яндекс.Вики (URL, ID организации, кнопка «Проверить Вики»).
  - Индикатор статуса подключения с динамической подсветкой (`bold friendly` / `bold edit-help`).
- **Административная форма пользователя (`ExtUserEdit`)**:
  - Добавлена вкладка `yandexTab` и кнопка навигации `yandexTabNav`.
  - Полнофункциональное управление параметрами Яндекс 360 администратором с диагностикой связи.

### 1.4 Интеграция с AI Агентами и Чатом
- **Локальный LLM-чат (`LlmChatServiceBean`)**: перехват команд бронирования встреч («создай в моем календаре...», «создай в календаре собеседования с заказчиком...»), бронирование через `AiYandexOrchestrationService` и возврат готового отчета с ссылками на Телемост.
- **Hermes AI (`HermesChatServiceBean`)**: обогащение системного промпта инструкциями по сервисам Yandex 360 и перехват задач бронирования встреч рекрутинга.

---

## 2. Результаты тестирования и верификации

1. **Контрактное тестирование**:
   - `com.company.hunttech.core.YandexIntegrationContractTest`:
     - `testUserYandexConfigurationStructure` — PASSED
     - `testLiquibaseChangelogAndMasterRegistration` — PASSED
     - `testViewsXmlRegistration` — PASSED
     - `testWebSpringServiceRegistration` — PASSED
     - `testAiYandexIntentClassification` — PASSED
2. **Сборка всех модулей Gradle**:
   - `bash ../hunttech_recruiting/scripts/agent-gradle.sh assemble` — `BUILD SUCCESSFUL in 8s` (32 задачи up-to-date/executed).
3. **Open Code Review (Alibaba OCR CLI)**:
   - Все критические замечания (поддержка PROPFIND в HttpURLConnection, соответствие ID вкладок в `ExtUserEdit`, актуализация состояния datasource после диагностики) устранены.
