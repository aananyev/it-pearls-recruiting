# Отчет: Интеграция персонального плавающего LLM-чата с данными HRM HuntTech (READ-ONLY)

**Дата**: 2026-09-10  
**Автор**: Antigravity IDE (разработчик, поток 2)  
**Ветка**: `agent/antigravity-dev`  
**Целевая ветка (PR)**: `master`  
**Статус**: Завершено, протестировано, готово к слиянию и деплою Hermes-1  
**Версия**: `0.532`  

---

## 1. Бизнес-цель и решаемая задача
Обеспечить персональному плавающему ассистенту `LLM_CHAT` доступ к рабочим данным HRM HuntTech в строгом режиме чтения (**READ-ONLY**):
- Поиск открытых вакансий (`OpenPosition`);
- Поиск и анализ кандидатов (`JobCandidate`, `Person`);
- Анализ резюме (`CandidateCV`) и компетенций (`CandidateSkill`/`SkillTree`);
- Просмотр истории взаимодействий (`IteractionList`);
- Формирование сжатых фактологических докладов («найди», «объясни», «сравни», «сделай сводку»);
- Двусторонняя интерактивная навигация через ссылки `hrm://` с открытием карточек в `AppWorkArea` через `ScreenBuilders` (`OpenMode.NEW_TAB`).

---

## 2. Архитектура решения и выполненные работы

### 2.1. Сервисный слой и выборка данных
- **`HrmDataContextSnapshot`**: легковесный DTO для безопасной передачи среза данных в контекст LLM.
- **`HrmChatDataRetrieverService` / `HrmChatDataRetrieverServiceBean`**:
  - Строгий SELECT-only режим через `DataManager`;
  - Полнотекстовый (FTS) и реляционный JPQL-поиск по ключевым словам и навыкам кандидатов;
  - Пакетная агрегация воронки по вакансиям (`loadFunnelsForVacancies`) в один запрос без N+1;
  - Защита токен-бюджета (компактная Markdown-сериализация, лимит среза до 2000 токенов с защитой суррогатных пар UTF-16).
- **`LlmChatServiceBean`**:
  - Обогащение контекста `executeText` / `executeTextStreaming` срезом данных;
  - Сохранение штатного лимита 1200 токенов для исключения перерасхода квот пользователей.

### 2.2. Data View Integrity
В `views.xml` декларированы легковесные представления, содержащие все необходимые реляционные поля и геттеры:
- `openPosition-llm-view`
- `jobCandidate-llm-view`
- `iteractionList-llm-view`
- `candidateCV-llm-view`

### 2.3. Системные данные и миграции
- Создана миграция `260910-1-enableLlmChatHrmDataGrounding.sql` / `.xml`:
  - Обновлен `system_prompt` функции `LLM_CHAT`;
  - Снят запрет на обработку данных HRM;
  - Закреплены форматы сжатых докладов и синтаксис ссылок `[Текст](hrm://candidate/{UUID})`;
  - Закреплен строгий запрет на раскрытие служебных секретов, API-ключей и нерелевантных ПДн;
  - Миграция идемпотентна, зарегистрирована в `db.changelog-master.xml`.

### 2.4. UI, рендеринг и навигация
- **`MarkdownRenderer`**:
  - Regex-валидация UUID для ссылок `hrm://candidate/...`, `hrm://vacancy/...`, `hrm://interaction/...`;
  - Генерация атрибутов `data-entity` и `data-id` с бейджами `👤`, `💼`, `📋`;
  - Строгая XSS-санитизация HTML.
- **`LlmChatScreen`**:
  - Двусторонний JS-bridge `hunttechOpenHrmEntity`, привязанный к `chatUi.getPage().getJavaScript()`;
  - Открытие карточек `JobCandidateEdit`, `OpenPositionEdit`, `IteractionListEdit` в новой вкладке `AppWorkArea` через `ScreenBuilders.editor(...)` с целевыми view (`jobCandidate-view`, `openPosition-view`, `iteractionList-edit-view`);
  - Валидация прав `Security.isEntityOpPermitted(..., EntityOp.READ)`;
  - Сброс состояния моста в `onAfterClose`.
- **Стили 7 тем CUBA**:
  - `.llm-hrm-entity-link` синхронизирован во всех 7 темах (`hunttech-modern-light`, `hunttech-modern-dark`, `hunttech-modern`, `helium`, `halo`, `havana`, `hover`);
  - Поддержка светлой и темной тем, псевдокласс `:focus-visible` для доступности.

---

## 3. Результаты верификации и тестирования

1. **`HrmChatDataRetrieverContractTest`**: **8/8 PASSED**
   - Контракты выборки данных, FTS, пакетной агрегации воронки, навигации ScreenBuilders, Data View Integrity и стилей 7 тем.
2. **`MarkdownRendererTest`**: **11/11 PASSED**
   - XSS-защита, корректный парсинг `hrm://`, разметка бейджей и ссылок.
3. **`LlmChatFoundationContractTest`**: **19/19 PASSED**
4. **`LlmChatSecurityContractTest`**: **2/2 PASSED**
5. **`ScreenViewIntegrityTest`**: **PASSED** (100%).
6. **`:app-web:buildScssThemes`**: **PASSED** (успешная сборка всех 7 тем, без diff).
7. **Безопасность запуска (`start-app.sh`)**:
   - Guard «Миграции на общую БД применяет только Hermes-1 — деплой ветки запрещён» сработал штатно.
8. **Alibaba OCR Review**:
   - Замечания по типизации параметров JS-моста, привязке к Page UI и `Locale.ROOT` устранены в коммите `2fd80332`.

---

## 4. План миграции данных для деплоя на прод
- **Тип миграции**: обновление системных данных (`system_prompt` конфигурации AI-функции `LLM_CHAT`).
- **Файл миграции**: `260910-1-enableLlmChatHrmDataGrounding.sql` / `260910-1-enableLlmChatHrmDataGrounding.xml`.
- **Изменения структуры БД**: отсутствуют (нет DDL-изменений, нет создания/удаления таблиц или колонок).
- **Бекап**: согласно правилу, перед применением миграций на прод агент Hermes-1 выполняет стандартный бэкап данных (`pg_dump`).
- **Откатоустойчивость**: миграция идемпотентна, при откате системный промпт может быть возвращен к предыдущей версии из истории `hunttech_ai_function_configuration`.

---

## 5. Коммиты ветки
- `3c4d6e54`: feat(llm-chat): этап 1 интеграции с данными HRM (read-only срез данных, views, ссылки на сущности)
- `e8e2055d`: feat(llm-chat): этап 2 интеграции с данными HRM (поиск по навыкам, пакетная агрегация воронки по вакансиям, ссылки на взаимодействия)
- `f750035e`: feat(llm-chat): этапы 3-4 интеграции данных HRM — UI навигация через ScreenBuilders, бейджи сущностей и стили 7 тем
- `993a5fde`: test(llm-chat): этап 5 — расширенное контрактное тестирование навигации ScreenBuilders, integrity views и стилей 7 тем
- `2fd80332`: fix(llm-chat): привязка JS-моста к Page UI, валидация параметров вызова и сброс состояния при закрытии экрана
