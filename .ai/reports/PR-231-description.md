## Что сделано

1. **Сценарий «Умная загрузка описания вакансии» (Smart AI Wizard)**:
   - В диалоге `SmartOpenPositionUploadScreen` (вызов из реестра открытых вакансий по кнопке `smartUploadBtn` с иконкой волшебной палочки) поддержана загрузка произвольного текста/ссылки/файлов, структурированное распознавание через AI и создание черновика вакансии.
   - Вакансии, созданные через умную загрузку, сохраняются как черновики (`signDraft = true`) со статусом «На проверку» (`priority = -2` / `OpenPositionPriority.UNDER_REVIEW`).

2. **Каноническая генерация наименования вакансии**:
   - Найден и применен алгоритм кнопки «Генерировать» из `OpenPositionEdit.java` (`generateVacancyName()`).
   - Метод `generateCanonicalVacancyName` формирует каноническое имя по единому стандарту:
     `[Grade] [PositionRu] / [PositionEn] ([Project], [City])`
     (с безопасной обработкой удаленного формата работы по всей РФ и отсутствия EN-названия).

3. **Запрет заведения новых должностей и гео-данных**:
   - Подбор должности выполняется исключительно среди существующих 220 записей справочника `hunttech_Position` через `findBestMatchingPositionType` (точное совпадение, поиск подстроки, токенный скоринг). Для вакансии SSP 67156 «Разработчик 1С:WMS» сопоставляется должность «Разработчик 1С» (`1C Developer`). Заведение новых должностей запрещено.
   - Запрещено заведение новых стран, городов и регионов: метод `findCity` выполняет только `SELECT` по `hunttech_City`.

4. **Генерация подчиненной сущности Project**:
   - Если открытый проект не найден в БД, генерируется новая подчиненная сущность `Project` (с заполнением `projectName`, `shortDescription`, `projectDescription`) и сохраняется в едином `CommitContext`. Добавлена дедупликация проектов-заглушек.

5. **Каноническая привязка навыков**:
   - Навыки переиспользуются из справочника `hunttech_SkillTree` (без нарушения уникального индекса `idx_hunttech_skill_tree_uk_skill_name`), а связка с вакансией создается через каноническую сущность `OpenPositionSkill` (`CandidateSkillPriority.MAIN` = 10).

6. **Административные AI-промпты**:
   - В `hunttech_ai_function_configuration` и `hunttech_vacancy_prompt_template` зарегистрированы промпты: `VACANCY_SMART_PARSE_JSON` (JSON повторяет поля сущности `OpenPosition`), `STANDARDIZE_VACANCY` (14 разделов), `VACANCY_CHECKLIST`, `VACANCY_SEARCH_MAP`, `VACANCY_INTERVIEW_PLAN`.

---

## Как проверено

1. **Модульные и контрактные тесты**:
   - `com.company.hunttech.core.SmartOpenPositionIngestServiceContractTest` — зелёный (100% PASS).
   - `com.company.hunttech.core.SmartOpenPosition10VacanciesIngestTest` (10 различных вакансий) — зелёный (10/10 PASS).

2. **Code review через Alibaba OCR CLI**:
   - Сессия `ocr review -c HEAD --audience agent` выявила 5 замечаний.
   - Все замечания устранены в коммите `e5ea034f` (сохранение `rawVacansyName`, устранение дублирования поиска проектов, защита от дубликатов stub-проектов, документация Javadoc).

3. **Сквозной UI-тест в браузере (пользователь `alan`)**:
   - Путь: Главное меню → Хедхантинг (Общее) → Реестр открытых вакансий → кнопка «Умная загрузка».
   - Загружено полное описание вакансии SSP Soft 67156 («Разработчик 1С:WMS»).
   - Успешно распознано каноническое наименование: `Senior Разработчик 1С / 1C Developer (Новый проект)`.
   - Черновик создан в БД: ID `5f42c521-7f8c-63dc-1861-38e6a7ae9301`, `sign_draft = true`, `priority = -2`, привязана существующая должность `Разработчик 1С`, создан проект `Новый проект`, привязаны 5 навыков через `OpenPositionSkill`.

4. **Стенд**:
   - Общий Tomcat на порту 8080 штатно возвращен на ветку `master` (`bash ../hunttech_recruiting/scripts/start-app.sh`, HTTP 200).

---

## Что ожидается от Hermes-1

1. Проверить контрактную сборку и тесты.
2. Выполнить слияние (merge) PR в `master`.
3. Задеплоить актуальный `master` (версия build.gradle ≥ 0.448).

WAITING_FOR_HERMES
