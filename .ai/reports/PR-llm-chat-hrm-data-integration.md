## Что сделано
Полнофункциональная интеграция плавающего персонального LLM-чата (`LLM_CHAT`) со срезом данных HRM HuntTech в строгом режиме чтения (**READ-ONLY**):
1. **Сервисный слой выборки данных**:
   - `HrmDataContextSnapshot` — DTO среза данных (вакансии, кандидаты, резюме, взаимодействия).
   - `HrmChatDataRetrieverService` / `HrmChatDataRetrieverServiceBean` — безопасное извлечение сущностей через `DataManager` (SELECT-only), FTS, JPQL-поиск по навыкам кандидатов, пакетная агрегация воронки по вакансиям (`loadFunnelsForVacancies`) в один запрос без N+1.
   - Токен-бюджетирование среза (до 2000 токенов с защитой суррогатных пар UTF-16) и сохранение штатного лимита 1200 токенов для исключения перерасхода квот пользователей.
2. **Data View Integrity**:
   - В `views.xml` задекларированы легковесные представления `openPosition-llm-view`, `jobCandidate-llm-view`, `iteractionList-llm-view`, `candidateCV-llm-view`.
3. **Системные данные и промпты**:
   - Миграция `260910-1-enableLlmChatHrmDataGrounding.sql` / `.xml` актуализирует системный промпт `LLM_CHAT`, снимая запрет на работу с данными кандидатов и вакансий и фиксируя шаблоны сжатых докладов и синтаксис ссылок `hrm://`.
4. **UI-рендеринг и навигация**:
   - `MarkdownRenderer`: Regex-валидация UUID, рендеринг бейджей `👤`, `💼`, `📋`, атрибутов `data-entity` и `data-id`, XSS-санитизация.
   - `LlmChatScreen`: двусторонний JS-bridge `hunttechOpenHrmEntity`, привязанный к `chatUi.getPage().getJavaScript()`, безопасное открытие карточек через `ScreenBuilders.editor(...).withOpenMode(OpenMode.NEW_TAB)` с загрузкой полных представлений (`jobCandidate-view`, `openPosition-view`, `iteractionList-edit-view`) и проверкой прав `Security.isEntityOpPermitted`.
   - Стили `.llm-hrm-entity-link` синхронизированы во всех 7 темах CUBA.

## Как проверено
- `HrmChatDataRetrieverContractTest`: 8 контрактов — **PASSED**.
- `MarkdownRendererTest`: 11 тестов — **PASSED**.
- `LlmChatFoundationContractTest`: 19 тестов — **PASSED**.
- `LlmChatSecurityContractTest`: 2 теста — **PASSED**.
- `ScreenViewIntegrityTest`: **PASSED**.
- `:app-web:buildScssThemes`: **PASSED** (7 тем скомпилированы чисто).
- Alibaba OCR Review: рекомендации по типизации параметров моста и `Locale.ROOT` устранены.

## Что ждём от Hermes-1
- Проверить PR, применить миграцию `260910-1-enableLlmChatHrmDataGrounding.sql` (бэкап БД, CUBA updateDb) и задеплоить на прод.
