# OpenPosition — архитектурная спецификация

> Полная спецификация сущности **OpenPosition** (открытая вакансия), сгенерирована из кода репозитория.
> Living-doc: [OpenPosition.md](../entities/OpenPosition.md)

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.itpearls.entity.OpenPosition` |
| **Имя в CUBA** | `itpearls_OpenPosition` |
| **Таблица БД** | `ITPEARLS_OPEN_POSITION` |
| **NamePattern** | `%s %s\|vacansyID,vacansyName` |
| **Платформа** | CUBA 7.3 |

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Сущность `OpenPosition` моделирует жизненный цикл вакансии в HRM HuntTech: от черновика и согласования приоритета до набора кандидатов и закрытия. Хранит коммерческие и технические параметры позиции (проект, грейд, зарплатная вилка, формат работы, количество мест), текстовые материалы для рекрутёров (описания, письма, тестовые, памятки) и операционные связи — подписки рекрутёров, навыки, комментарии, файлы, трудовые договоры. Иерархия `parentOpenPosition` поддерживает группировку связанных вакансий в дереве browse.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экраны: `itpearls_OpenPosition.browse` (основной список-дерево), `itpearls_OpenPosition.edit` (много вкладок), специализированные browse (recruiting, outstaff, prod, master), фрагмент `itpearls_OpenPositionDetailScreenFragment`, комментарии `OpenPositionComment`, новости `OpenPositionNews`, подписки `RecrutiesTasks`. Входящие FK из `IteractionList.vacancy`, `CandidateCV`, `RecrutiesTasks`. Living-doc: [OpenPosition.md](../entities/OpenPosition.md); UI: [browse](../ui/itpearls_OpenPosition.browse_Spec.md), [edit](../ui/itpearls_OpenPosition.edit_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

**Browse:** фильтры подписки и приоритета; пакетные кэши для колонок без N+1; закрытие вакансии → массовое завершение кандидатов «на рассмотрении» одним commit. **Edit:** ленивые вкладки LOB/коллекций; sync навыков и labor agreement перед сохранением; проверка дубликатов и vacansyID; Telegram при открытии/изменении (ошибка API не отменяет save).

---

## 1. Архитектура Сущности (Data Model Layer)

### 1.1 Класс и таблица

- **Файл entity:** `modules/global/src/com/company/itpearls/entity/OpenPosition.java`
- **Базовый класс:** `StandardEntity` (soft delete, audit-поля)
- **Таблица:** `ITPEARLS_OPEN_POSITION` (`@Table`)

### 1.2 Индексы (`@Table`)

| Имя | Колонки |
|-----|---------|
| `IDX_ITPEARLS_OPEN_POSITION_OPEN_CLOSE` | `OPEN_CLOSE` |
| `IDX_ITPEARLS_OPEN_POSITION_PRIORITY` | `PRIORITY` |
| `IDX_ITPEARLS_OPEN_POSITION_VACANSY_NAME` | `VACANSY_NAME` |

### 1.3 Поля и связи

| Поле Java | Колонка БД | Тип / FK | Ограничения | Описание |
|-----------|------------|----------|-------------|----------|
| `openClose` | `OPEN_CLOSE` | Boolean | `@NotNull`, default `false` | Вакансия закрыта |
| `rating` | `RATING` | Integer | — | Рейтинг |
| `signDraft` | `SIGN_DRAFT` | Boolean | — | Черновик |
| `lastOpenDate` | `LAST_OPEN_DATE` | Date | — | Дата последнего открытия |
| `closingDate` | `CLOSING_DATE` | Date | — | Плановая дата закрытия |
| `vacansyName` | `VACANSY_NAME` | String(250) | `@NotNull` | Название вакансии |
| `vacansyID` | `VACANSY_ID` | String(16) | — | Внешний ID вакансии |
| `grade` | `GRADE_ID` | FK → `Grade` | LAZY | Грейд |
| `remoteWork` | `REMOTE_WORK` | Integer | `@NotNull` | Формат удалённой работы |
| `registrationForWork` | `REGISTRATION_FOR_WORK` | Integer | — | Оформление |
| `remoteComment` | `REMOTE_COMMENT` | String(40) | — | Комментарий к удалёнке |
| `commandCandidate` | `COMMAND_CANDIDATE` | Integer | `@NotNull` | Тип команды кандидата |
| `salaryMin` / `salaryMax` | `SALARY_MIN` / `SALARY_MAX` | BigDecimal | — | Вилка зарплаты |
| `salaryIE` | `SALARY_IE` | BigDecimal | — | Зарплата ИП |
| `salaryFixLimit` | `SALARY_FIX_LIMIT` | Boolean | — | Фиксированный лимит |
| `salaryCandidateRequest` | `SALARY_CANDIDATE_REQUEST` | Boolean | — | Запрос зарплаты у кандидата |
| `salaryComment` | `SALARY_COMMENT` | String | — | Комментарий к зарплате |
| `outstaffingCost` | `OUTSTAFFING_COST` | BigDecimal | — | Стоимость аутстаффа |
| `cityPosition` | `CITY_POSITION_ID` | FK → `City` | LAZY, lookup | Основной город |
| `cities` | — | `OneToMany` → `City` | `@Composition`, CASCADE | Доп. города |
| `positionType` | `POSITION_TYPE_ID` | FK → `Position` | LAZY, lookup | Тип должности |
| `projectName` | `PROJECT_NAME_ID` | FK → `Project` | LAZY, `@NotNull` | Проект |
| `numberPosition` | `NUMBER_POSITION` | Integer | — | Количество позиций |
| `more10NumberPosition` | `MORE10_NUMBER_POSITION` | Boolean | — | Более 10 позиций |
| `workExperience` | `WORK_EXPERIENCE` | Integer | NOT NULL | Опыт работы |
| `commandExperience` | `COMMAND_EXPERIENCE` | Integer | — | Опыт в команде |
| `comment` | `COMMENT_` | String (LOB) | — | Описание RU |
| `commentEn` | `COMMENT_EN` | String (LOB) | — | Описание EN |
| `shortDescription` | `SHORT_DESCRIPTION` | String(250) | `@Length(max=250)` | Краткое описание |
| `templateLetter` | `TEMPLATE_LETTER` | String (LOB) | — | Шаблон письма |
| `needLetter` | `NEED_LETTER` | Boolean | — | Требуется письмо |
| `exercise` | `EXERCISE` | String (LOB) | — | Тестовое задание |
| `needExercise` | `NEED_EXERCISE` | Boolean | — | Требуется тестовое |
| `priority` | `PRIORITY` | Integer | — | Приоритет / статус согласования |
| `priorityComment` | `PRIORITY_COMMENT` | String | — | Комментарий приоритета |
| `skillsList` | — | `OneToMany` → `SkillTree` | `@Composition`, DENY | Навыки |
| `candidates` | link-table | M2M → `RecrutiesTasks` | DENY | Подписки рекрутёров |
| `paymentsType` | `PAYMENTS_TYPE` | Integer | — | Тип оплаты |
| `typeCompanyComission` | `TYPE_COMPANY_COMISSION` | Integer | — | Тип комиссии компании |
| `typeSalaryOfResearcher` | `TYPE_SALARY_OF_RESEARCHER` | Integer | — | Тип зарплаты ресерчера |
| `typeSalaryOfRecrutier` | `TYPE_SALARY_OF_RECRUTIER` | Integer | — | Тип зарплаты рекрутёра |
| `useTaxNDFL` | `USE_TAX_NDFL` | Boolean | — | Учёт НДФЛ |
| `internalProject` | `INTERNAL_PROJECT` | Boolean | `@NotNull`, default `false` | Внутренний проект |
| `percentComissionOfCompany` | `PERCENT_COMISSION_OF_COMPANY` | String(5) | — | % комиссии |
| `percentSalaryOfResearcher` | `PERCENT_SALARY_OF_RESEARCHER` | String(5) | — | % ресерчера |
| `percentSalaryOfRecrutier` | `PERCENT_SALARY_OF_RECRUTIER` | String(5) | — | % рекрутёра |
| `parentOpenPosition` | `PARENT_OPEN_POSITION_ID` | FK → `OpenPosition` | LAZY | Родительская вакансия (дерево) |
| `needMemoForInterview` | `NEED_MEMO_FOR_INTERVIEW` | Boolean | — | Нужна памятка к интервью |
| `memoForInterview` | `MEMO_FOR_INTERVIEW` | String (LOB) | — | Текст памятки |
| `laborAgreement` | link-table | M2M → `LaborAgreement` | `@Composition`, CASCADE | Трудовые договоры |
| `openPositionComments` | — | `OneToMany` → `OpenPositionComment` | `@Composition`, CASCADE | Комментарии |
| `owner` | `OWNER_ID` | FK → `ExtUser` | LAZY | Владелец вакансии |
| `someFiles` | — | `OneToMany` → `SomeFilesOpenPosition` | `@Composition`, CASCADE | Файлы |

### 1.4 Связующие таблицы

| Таблица | Назначение |
|---------|------------|
| `ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK` | M2M `OpenPosition` ↔ `RecrutiesTasks` |
| `ITPEARLS_OPEN_POSITION_LABOR_AGREEMENT_LINK` | M2M `OpenPosition` ↔ `LaborAgreement` |
| `ITPEARLS_INTERVIEW_OPEN_POSITION_LINK` | M2M `Interview` ↔ `OpenPosition` |

### 1.5 Сервисы (core)

| Интерфейс / Bean | Файл | Методы (основные) |
|------------------|------|-------------------|
| `OpenPositionService` / `OpenPositionServiceBean` | `modules/global/.../OpenPositionService.java`, `modules/core/.../OpenPositionServiceBean.java` | `getOpenPositionSet()`, `getOpenPositionList()` (view `openPosition-view`), `setOpenPositionNewsAutomatedMessage(...)`, `createOpenPositionDefault()`, `getOpenPositionDefault()`, `getOpenPositionCloseShortMessage/OpenLongMessage`, `getOpenPositionOpenShortMessage/OpenLongMessage` |
| `OpenPositionCommentService` / `OpenPositionCommentServiceBean` | `modules/global/.../`, `modules/core/.../` | CRUD комментариев |
| `OpenPositionApprovalBean` | `modules/core/.../OpenPositionApprovalBean.java` | `updateState(UUID entityId, Integer state)` — обновление `priority` |
| `OpenPositionCommentChangedListener` | `modules/core/.../listeners/` | Реакция на изменение комментариев |
| JMX `OpenPositionApproval` | `modules/core/.../jmx/OpenPositionApproval.java` | JMX-управление согласованием |

### 1.6 Миграции

- **Init DDL (PostgreSQL):** `modules/core/db/init/postgres/10.create-db.sql` — таблица `ITPEARLS_OPEN_POSITION` и link-таблицы
- **Инкрементальные миграции:** `modules/core/db/update/postgres/**/updateOpenPosition*.sql`, `createOpenPositionComment.sql`, `createInterviewOpenPositionLink.sql` и др. (каталоги `22/`–`24/`)

---

## 2. Слой Выборок Данных (Fetch Plans / Views Layer)

**Файл:** `modules/global/src/com/company/itpearls/views.xml`

| View | extends | Назначение | Ключевые свойства |
|------|---------|------------|-------------------|
| `openPosition-browse-view` | `_minimal` | Browse-списки без LOB | `vacansyID`, `vacansyName`, `openClose`, `signDraft`, `priority`, `rating`, даты, зарплата, FK: `grade`, `positionType`, `cityPosition`, `cities`, `owner`, `parentOpenPosition`, `projectName` (с `projectLogo`, `projectDepartment`→`companyName`, `projectOwner`), минимальные `candidates`, `openPositionComments`, `someFiles` |
| `openPosition-edit-view` | `_minimal` | Edit без LOB и коллекций | Скаляры + FK: `grade`, `cityPosition`, `cities`, `positionType`→`position-picker-view`, `projectName`→`project-edit-view`, `parentOpenPosition`→`openPosition-parent-picker-view`, `owner` |
| `openPosition-picker-view` | `_minimal` | Lookup / FK | `vacansyID`, `vacansyName`, `openClose` |
| `openPosition-parent-picker-view` | `openPosition-picker-view` | Parent lookup | + `projectName`→`project-picker-view` |
| `openPosition-project-tab-view` | `_minimal` | Вкладка проекта | `openClose`, `vacansyName`, `numberPosition`, `positionType`, `createTs` |
| `openPosition-rtasks-browse-view` | `_minimal` | FK в RecrutiesTasks browse | `vacansyName`, `projectName`+logo |
| `openPosition-rtasks-picker-view` | `_minimal` | Picker в RecrutiesTasks edit | Расширенный набор FK |
| `openPosition-iteraction-list-picker-view` | `_minimal` | FK vacancy в IteractionList | Полный набор для карточки кандидата |
| `openPosition-view-iteraction-list` | `_local` | IteractionList | `projectName` с department/company |
| `openPosition-candidate` | `_local` | Кандидаты на вакансии | `grade`, `projectName`, `candidates`, `positionType` |
| `openPosition-view` | `_local` | Legacy / сервисы | Полный `_local` + `skillsList`, `openPositionComments`, `projectName` |
| `openPosition-rtasks-view` | `_local` | RecrutiesTasks | — |
| `someFilesOpenPosition-edit-view` | `_minimal` | Вкладка Files в Edit | `fileDescriptor`, `fileOwner`, `fileType`, … |
| `laborAgreement-openPosition-tab-view` | `_minimal` | Вкладка Labor Agreement | `perhaps`, `company`, `laborAgreementType` |

**Исключения LOB из browse/edit views:** `comment`, `commentEn`, `templateLetter`, `exercise`, `memoForInterview` — загружаются lazy в `OpenPositionEdit` и через batch exists в `OpenPositionBrowse`.

---

## 3. Списочные интерфейсы (Browse Screens)

### 3.1 OpenPositionBrowse (основной)

| Параметр | Значение |
|----------|----------|
| **Контроллер** | `itpearls_OpenPosition.browse` |
| **Класс** | `OpenPositionBrowse` extends `StandardLookup<OpenPosition>` |
| **Descriptor** | `open-position-browse.xml` |
| **Lookup** | `openPositionsTable` (`treeDataGrid`) |
| **Data** | `readOnly="true"`, view `openPosition-browse-view` |

**JPQL loader (`openPositionsDl`):**

```sql
select e from itpearls_OpenPosition e order by e.vacansyName
```

**Условия фильтра (`<condition>`):** `priority`, `lastOpenDate` (новые), подписки `RecrutiesTasks` (`freesubscriber`, `subscriber`, `notsubscriber`, `recrutier`), `openClose`, `signDraft`, `paused`, `internalProject`, `rating`, `remoteWork`, `positionType`.

**Фильтр UI:** `excludeProperties` — `comment`, `numberPosition`, audit-поля; custom-фильтры: `projectFilter`, `positionFilter`, `projectOwnerFilter`, `newOpenPositionFilter`.

**Колонки (treeDataGrid):** `folder`, `icon` (приоритет), `rating`, логотипы company/project, `lastOpenCloseColumn`, `vacansyID`, `vacansyName`, `positionType`, `projectName`, `cityPositionList`, `workExperience`, `salaryMinMax`, `numberPosition`, `description`, `lastCVSend`, `activeRecruiters`, `remoteWork`, `cities`, `owner`, `signDraft`, `openClose`, `internalProject`, `shortDescription`, `priority`, `closingDate` и др. (часть — `componentRenderer` в Java).

**Действия таблицы:** `create`, `edit`, `remove`, `excel`, `list` (print form).

**Java-логика (ключевое):** иерархия `parentOpenPosition`; блок «срочные вакансии» (`urgentlyPositons`); batch-кэши LOB exists и агрегатов в PostLoad; `rowStyleProvider` / `descriptionProvider`; закрытие вакансии с batch `CommitContext` для `IteractionList`; интеграция с `RecrutiesTasks`, `JobCandidateSimpleBrowse`, `OpenPositionCommentsView`, `QuickViewOpenPositionDescription`, `Suggestjobcandidate`.

**Поведение для пользователя (простым языком):**

| Действие | Цепочка |
|----------|---------|
| Открытие списка | Фильтры «открытые» + «моя подписка»; блок срочных вакансий |
| Раскрытие строки | Фрагмент + кнопки edit, open/close, комментарии, подбор |
| Закрыть вакансию | Кандидаты на рассмотрении → диалог → batch end-case → commit |
| Смена приоритета на Low | Диалог недели закрытия → `closingDate` |

### 3.2 Производные Browse-экраны

| Экран | ID | Базовый класс | Descriptor |
|-------|-----|---------------|------------|
| `OpenPositionMasterBrowse` | `itpearls_OpenPositionMaster.browse` | `StandardLookup<OpenPosition>` | `open-position-master-browse.xml` |
| `ProdOpenPositionBrowse` | `itpearls_ProdOpenPosition.browse` | extends `OpenPositionBrowse` | `prod-open-position-browse.xml` |
| `OpenPositionOutstaffBrowse` | `itpearls_OpenPositionOutstaff.browse` | extends `OpenPositionBrowse` | `open-position-outstaff-browse.xml` |
| `OpenPositionRecruitingBrowse` | `itpearls_OpenPositionRecruiting.browse` | extends `OpenPositionBrowse` | `open-position-browse-recrutiting.xml` |

---

## 4. Формы редактирования (Edit Screens)

### 4.1 OpenPositionEdit

| Параметр | Значение |
|----------|----------|
| **Контроллер** | `itpearls_OpenPosition.edit` |
| **Класс** | `OpenPositionEdit` extends `StandardEditor<OpenPosition>` |
| **Descriptor** | `open-position-edit.xml` |
| **Entity container** | `openPositionDc`, view `openPosition-edit-view` |
| **Аннотации** | `@LoadDataBeforeShow`, `@EditedEntityContainer("openPositionDc")` |

**Standalone CollectionLoader (lazy по вкладкам):**

| Loader | Entity | View | Условие JPQL |
|--------|--------|------|--------------|
| `laborAgreementDl` | `LaborAgreement` | `laborAgreement-openPosition-tab-view` | `op = :openPosition` (join) |
| `commentsOpenPositionDl` | `OpenPositionComment` | `openPositionComment-edit-view` | `e.openPosition = :openPosition` |
| `someFilesesDl` | `SomeFilesOpenPosition` | `someFilesOpenPosition-edit-view` | `e.openPosition = :openPosition` |
| `openPositionSkillsListsDl` | `SkillTree` | `skillTree-openPosition-tab-view` | `e.openPosition = :openPosition` |
| `procAttachmentsDl` | `bpm$ProcAttachment` | `procAttachment-browse` | `e.procInstance.entity.entityId = :entityId` |

**Вкладки (`tabSheetOpenPosition`):** `tabOpenPosition` (основная), `laborAgreementTab`, `tabPayments`, описания RU/EN/стандарт/whoIsThisGuy, `tabFiles`, `tabExercise`, `tabMemoForInterview`, `tabTemplateLetter`, `tabSkills`, `tabOpenPositionNews`, `tabApproval`, `commentsTab`.

**Валидация и commit (Java):** проверка дубликата `vacansyID` (`count` с `e.id <> :currentId`); `syncSkillsListToEntity` / `syncLaborAgreementToEntity` перед commit; `ensure*LoadedOnEntity` для unfetched коллекций; таймер `closedVacancyTimer` (60 с) при `closingDate`; Telegram-уведомление через `TelegramService` (ошибки API не блокируют сохранение).

**Поведение для пользователя (простым языком):**

| Момент | Цепочка |
|--------|---------|
| Смена вкладки | Первый заход → загрузка LOB/коллекции этой вкладки |
| Смена positionType/project | Автогенерация vacansyName, логотип, департамент |
| priority = Low | Диалог недели → closingDate + таймер обратного отсчёта |
| Сохранение | Дубликат имени/vacansyID → диалог; shortDescription > 250 → блок; sync skills/labor |
| После сохранения | OpenPositionNews; Telegram; диалог дочерних command-вакансий |

**Lazy LOB:** загрузка `comment`/`commentEn`, `standartDescription`/`whoIsThisGuy`, `templateLetter`, `exercise`, `memoForInterview` по открытию соответствующих вкладок (`ViewBuilder`).

---

## 5. Компоненты и Фрагменты (UI Fragments & Dialogs)

| Компонент | ID | Тип | Descriptor | Назначение |
|-----------|-----|-----|------------|------------|
| `OpenPositionDetailScreenFragment` | `itpearls_OpenPositionDetailScreenFragment` | `ScreenFragment` | `open-position-detail-screen-fragment.xml` | Детали вакансии в browse (логотип, метки needExercise/needLetter) |
| `OpenPositionOutstaffDetailScreenFragment` | `itpearls_OpenPositionOutstaffDetailScreenFragment` | extends detail | `open-position-outstaff-detail-screen-fragment.xml` | Вариант для аутстаффа |
| `QuickViewOpenPositionDescription` | `itpearls_QuickViewOpenPositionDescription` | `Screen` | `quick-view-open-position-description.xml` | Быстрый просмотр описания (RU/EN, проект, компания, CV requirements) |
| `TextViewScreen` | `itpearls_TextViewScreen` | `Screen` | `text-view-screen.xml` | Просмотр текста |
| `SelectCitiesLocation` | `itpearls_SelectCitiesLocation` | `Screen` | `select-cities-location.xml` | Выбор городов |
| `OpenPositionCommentsView` | — | View-экран | `OpenPositionCommentsView.xml` | Просмотр комментариев |
| `OpenPositionCommentEdit` | — | Editor | — | Редактирование комментария |
| `SomeFilesOpenPositionEdit` | — | Editor | — | Файлы вакансии |

**Cross-form FK:** `OpenPosition` используется как FK в `IteractionList.vacancy`, `CandidateCV.toVacancy`, `PersonelReserve`, `RecrutiesTasks`, `Interview`, `Employee`; picker-views: `openPosition-picker-view`, `openPosition-iteraction-list-picker-view`.

**Потребители browse/edit:** `JobCandidateEdit`, `JobCandidateBrowse`, `IteractionListEdit`, `PersonelReserveBrowse`, виджеты `MyActiveCandidatesDashboard`, `SkillsFilterJobCandidateBrowse`.

---

## 6. Инструкция по развертыванию с нуля (Deployment Guide)

### 6.1 База данных

1. PostgreSQL 11+ локально (см. [LOCAL_DATABASE.md](../LOCAL_DATABASE.md)): БД `itpearls`, пользователь `cuba`/`cuba`.
2. Создание схемы:
   ```bash
   ./gradlew createDb
   ./gradlew updateDb
   ```
3. Таблица `ITPEARLS_OPEN_POSITION` создаётся из `modules/core/db/init/postgres/10.create-db.sql`; последующие изменения — Liquibase/SQL в `modules/core/db/update/postgres/`.

### 6.2 app.properties (кэш)

`modules/core/src/com/company/itpearls/app.properties`:

```properties
cuba.dbmsType=postgres
eclipselink.cache.shared.itpearls_OpenPosition=true
eclipselink.cache.size.itpearls_OpenPosition=500
```

### 6.3 FTS (полнотекстовый поиск)

`modules/core/src/com/company/itpearls/fts.xml` — сущность `com.company.itpearls.entity.OpenPosition` с `<include re=".*"/>` (индексируются все строковые поля). Также в FTS: `OpenPositionNews`, `OpenPositionComment`, `SomeFilesOpenPosition`.

### 6.4 Сборка и запуск

```bash
./gradlew setupTomcat deploy start
# http://localhost:8080/app
```

Компиляция web-модуля:

```bash
./gradlew :app-web:compileJava -x test
```

**Java:** для Tomcat рекомендуется **JDK 11** (`JAVA_HOME` в `etc/tomcat-setenv.sh`) — иначе возможны ошибки загрузки screen-классов на Java 17+.

### 6.5 Меню и доступ

Экраны регистрируются через аннотации `@UiController` (CUBA 7 Screens API); пункты меню — `modules/web/src/com/company/itpearls/web-menu.xml` (browse вакансий: `itpearls_OpenPosition.browse`).

### 6.6 Тесты

- `modules/core/test/com/company/itpearls/core/OpenPositionServiceTest.java`
- `OpenPositionCommentServiceTest.java`, `OpenPositionNewsServiceTest.java`

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §3–4: поведение browse/edit простым языком (deep modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Создание архитектурной спецификации `OpenPosition_Spec.md` (one-pass из кода) |
