# OpenPosition — Архитектурная спецификация сущности

> Сущность: `OpenPosition` (JPQL `hunttech_OpenPosition`) · Таблица: `HUNTTECH_OPEN_POSITION`
> Тип документа: Architecture Spec (триггер «Сделай документацию сущности»)
> Дата: 2026-08-01 · Проект: **HRM HuntTech** (CUBA Platform 7.3)

---

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`OpenPosition` — карточка открытой позиции (вакансии) в рекрутинговой системе HRM HuntTech. Сущность хранит полный «паспорт» вакансии: название и внутренний ID (`vacansyName`, `vacansyID`), признак открытости/черновика (`openClose`, `signDraft`), рейтинг и приоритет, требуемый опыт и формат работы (`workExperience`, `remoteWork`, `registrationForWork`), вилку зарплаты и условия оплаты подрядчикам (`salaryMin/Max/IE`, проценты и типы комиссий), принадлежность проекту и компании (`projectName`, `positionType`, `grade`, `cityPosition`), иерархию «родительская/дочерняя» вакансия (`parentOpenPosition`), а также набор LOB-контента для кандидатов: описание на русском/английском (`comment`, `commentEn`), короткое описание, шаблон сопроводительного письма (`templateLetter`), тестовое задание (`exercise`), памятку к собеседованию (`memoForInterview`), чек-лист, карту поиска и план интервью. Дополнительно позиция агрегирует составные коллекции: города (`cities`), навыки (`skillsList`), комментарии (`openPositionComments`), файлы (`someFiles`), трудовые соглашения (`laborAgreement`) и задачи рекрутёров (`candidates` → `RecrutiesTasks`).

Бизнес-роль: позиция — центральная сущность подсистемы подбора: на неё ссылаются кандидаты (через `IteractionList.vacancy` и `RecrutiesTasks.openPosition`), она участвует в процессах согласования (BPM `ProcAttachment`) и в автоматических уведомлениях (Telegram/email при открытии/закрытии).

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экраны зарегистрированы в `web-menu.xml`:

- `hunttech_OpenPosition.browse` — основной справочник вакансий (меню, иконка `COMPASS`); открывается как `StandardLookup`, из него — редактирование позиции (`hunttech_OpenPosition.edit`), просмотр комментариев (`OpenPositionCommentEdit` / `OpenPositionCommentsView`), подписка на позиции, групповые подписки (`RecrutiesTasksGroupSubscribeBrowse`), подбор кандидатов (`Suggestjobcandidate`, `JobCandidateSimpleBrowse`, `JobCandidateSimpleMailBrowse`), выбор городов (`hunttech_SelectCitiesLocation`), просмотр описания (`hunttech_QuickViewOpenPositionDescription`).
- `hunttech_OpenPositionMaster.browse` — «мастер-подбор» (аккордеон: кандидат → типы позиций → компании → проекты → вакансии) с передачей выбранного кандидата (`setJobCandidate`).
- `hunttech_OpenPositionRecruiting.browse` — рекрутинговый browse (наследует `OpenPositionBrowse`).
- `hunttech_OpenPositionOutstaff.browse` — outstaff-вариант browse с фрагментом `OpenPositionOutstaffDetailScreenFragment`.
- `hunttech_ProdOpenPosition.browse` — прод-обёртка browse (`ProdOpenPositionBrowse extends OpenPositionBrowse`).

Фрагменты: `OpenPositionDetailScreenFragment` (детали вакансии в sidebar других форм), `OpenPositionOutstaffDetailScreenFragment`, `QuickViewOpenPositionDescription`, `TextViewScreen`, `Skillsbar`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие browse → загрузка `openPositionsDc` (maxResults 40) с 12 условиями фильтра (приоритет, новизна по `lastOpenDate`, подписки рекрутёра, `openClose`/`signDraft`, не-пауза, `internalProject`, рейтинг, `remoteWork`, `positionType`) → после загрузки контроллер строит кэши (наличие LOB-контента, агрегаты: активные рекрутёры, отправленные CV, средний рейтинг комментариев, подписчики, дочерние позиции, статистика взаимодействий) → таблица `treeDataGrid` с иерархией по `parentOpenPosition` и ~25 генераторами/провайдерами колонок.
- Открытие edit → заголовок с логотипами проекта и владельца, вкладка «О вакансии» с lazy-загрузкой LOB-полей (описания, тестовое, памятка, письмо, навыки, файлы, комментарии, трудовые соглашения — по факту первого переключения на вкладку) → валидация: `checkDuplicatePositionId` (дубль `vacansyID`) и валидаторы зарплат → commit → уведомления (Telegram/email подписчикам), синхронизация дочерних позиций (`openCloseChildVacancy`), BPM-согласование.
- Сохранение/закрытие → при закрытии родительской вакансии проверяется, что закрыты все дочерние («Невозможно закрыть вакансию верхнего уровня…», `msgCanNotCloseWithout`); таймер `closedVacancyTimer` (60 с) показывает обратный отсчёт до автозакрытия по `closingDate`.

---

## 1. Архитектура Сущности (Data Model Layer)

### 1.1 Java-класс и таблица

- Класс: `modules/global/src/com/company/hunttech/entity/OpenPosition.java` (709 строк), `extends StandardEntity`.
- `@NamePattern("%s %s|vacansyID,vacansyName")` — представление «ID Название».
- `@Table(name = "HUNTTECH_OPEN_POSITION", indexes = {IDX_HUNTTECH_OPEN_POSITION_OPEN_CLOSE (OPEN_CLOSE), IDX_HUNTTECH_OPEN_POSITION_PRIORITY (PRIORITY), IDX_HUNTTECH_OPEN_POSITION_VACANSY_NAME (VACANSY_NAME)})`.
- DDL-источники: `modules/core/db/init/hsql/10.create-db.sql` и `modules/core/db/init/postgres/10.create-db.sql` (`create table HUNTTECH_OPEN_POSITION`); последующие изменения — `modules/core/db/update/hsql/20/200205-2-updateOpenPosition.sql`, `200205-2-updateOpenPosition01.sql`, `200409-1/2-createOpenPositionRecrutiesTasksLink.sql` (link-таблица рекрутерских задач).

### 1.2 Скалярные поля (только из Java)

| Поле | Колонка | Тип | Ограничения |
|---|---|---|---|
| `openClose` | OPEN_CLOSE | Boolean | `@NotNull`, default `false` |
| `rating` | RATING | Integer | — |
| `signDraft` | SIGN_DRAFT | Boolean | черновик |
| `lastOpenDate` | LAST_OPEN_DATE | Date (DATE) | — |
| `vacansyName` | VACANSY_NAME | String(250) | `@NotNull`, `nullable=false` |
| `vacansyID` | VACANSY_ID | String(16) | — |
| `remoteWork` | REMOTE_WORK | Integer | `@NotNull`, `nullable=false` |
| `registrationForWork` | REGISTRATION_FOR_WORK | Integer | — |
| `remoteComment` | REMOTE_COMMENT | String(40) | — |
| `commandCandidate` | COMMAND_CANDIDATE | Integer | `@NotNull`, `nullable=false` |
| `salaryMin` / `salaryMax` / `salaryIE` | SALARY_MIN / SALARY_MAX / SALARY_IE | BigDecimal | — |
| `salaryFixLimit` | SALARY_FIX_LIMIT | Boolean | — |
| `salaryCandidateRequest` | SALARY_CANDIDATE_REQUEST | Boolean | — |
| `salaryComment` | SALARY_COMMENT | String | — |
| `outstaffingCost` | OUTSTAFFING_COST | BigDecimal | — |
| `numberPosition` | NUMBER_POSITION | Integer | — |
| `more10NumberPosition` | MORE10_NUMBER_POSITION | Boolean | — |
| `workExperience` | WORK_EXPERIENCE | Integer | `nullable=false` |
| `commandExperience` | COMMAND_EXPERIENCE | Integer | — |
| `comment` / `commentEn` | COMMENT_ / COMMENT_EN | `@Lob` | описание RU/EN |
| `shortDescription` | SHORT_DESCRIPTION | String(250) | `@Length(max=250)` |
| `templateLetter` | TEMPLATE_LETTER | `@Lob` | шаблон письма |
| `needLetter` | NEED_LETTER | Boolean | — |
| `exercise` | EXERCISE | `@Lob` | тестовое задание |
| `needExercise` | NEED_EXERCISE | Boolean | — |
| `priority` / `priorityComment` | PRIORITY / PRIORITY_COMMENT | Integer / String | — |
| `paymentsType` | PAYMENTS_TYPE | Integer | схема оплаты |
| `typeCompanyComission` | TYPE_COMPANY_COMISSION | Integer | — |
| `typeSalaryOfResearcher` | TYPE_SALARY_OF_RESEARCHER | Integer | — |
| `typeSalaryOfRecrutier` | TYPE_SALARY_OF_RECRUTIER | Integer | — |
| `useTaxNDFL` | USE_TAX_NDFL | Boolean | — |
| `internalProject` | INTERNAL_PROJECT | Boolean | `@NotNull`, default `false` |
| `percentComissionOfCompany` | PERCENT_COMISSION_OF_COMPANY | String(5) | — |
| `percentSalaryOfResearcher` | PERCENT_SALARY_OF_RESEARCHER | String(5) | — |
| `percentSalaryOfRecrutier` | PERCENT_SALARY_OF_RECRUTIER | String(5) | — |
| `needMemoForInterview` / `memoForInterview` | NEED_MEMO_FOR_INTERVIEW / MEMO_FOR_INTERVIEW | Boolean / `@Lob` | — |
| `rawDescription` | RAW_DESCRIPTION | `@Lob` | — |
| `interviewChecklist` | INTERVIEW_CHECKLIST | `@Lob` | — |
| `searchMap` | SEARCH_MAP | `@Lob` | — |
| `interviewPlan` | INTERVIEW_PLAN | `@Lob` | — |
| `closingDate` | CLOSING_DATE | Date (DATE) | автозакрытие |

### 1.3 Связи (FK и коллекции)

| Поле | Тип связи | Колонка/Link | Политика удаления |
|---|---|---|---|
| `grade` | `@ManyToOne LAZY` → `Grade` | GRADE_ID | — |
| `cityPosition` | `@ManyToOne LAZY` → `City` | CITY_POSITION_ID | — |
| `positionType` | `@ManyToOne LAZY` → `Position` | POSITION_TYPE_ID | — |
| `projectName` | `@ManyToOne LAZY`, `optional=false` → `Project` | PROJECT_NAME_ID | — |
| `owner` | `@ManyToOne LAZY` → `ExtUser` | OWNER_ID | — |
| `parentOpenPosition` | `@ManyToOne LAZY` (self) → `OpenPosition` | PARENT_OPEN_POSITION_ID | иерархия вакансий |
| `cities` | `@OneToMany @Composition` → `City` (mappedBy `openPosition`) | — | `CASCADE` |
| `skillsList` | `@OneToMany @Composition` → `SkillTree` (mappedBy `openPosition`) | — | `DENY` |
| `candidates` | `@ManyToMany` → `RecrutiesTasks` | HUNTTECH_OPEN_POSITION_RECRUTIES_TASKS_LINK | `DENY` |
| `laborAgreement` | `@ManyToMany @Composition` → `LaborAgreement` | HUNTTECH_OPEN_POSITION_LABOR_AGREEMENT_LINK | `CASCADE` |
| `openPositionComments` | `@OneToMany @Composition` → `OpenPositionComment` (mappedBy `openPosition`) | — | `CASCADE` |
| `someFiles` | `@OneToMany @Composition` → `SomeFilesOpenPosition` (mappedBy `openPosition`) | — | `CASCADE` |

### 1.4 Связанные сущности

- `OpenPositionComment` (`HUNTTECH_OPEN_POSITION_COMMENT`): `openPosition`, `rating` (Integer), `comment`, `user` (ExtUser), `dateComment` (Date). Комментарий-рейтинг позиции.
- `OpenPositionNews` (`HUNTTECH_OPEN_POSITION_NEWS`): `subject`, `openPosition`, `dateNews`, `comment`, `candidates` (JobCandidate), `author` (ExtUser), `priorityNews` (Boolean). Новости вакансии; автоматические сообщения формирует `OpenPositionServiceBean.setOpenPositionNewsAutomatedMessage`.
- `SomeFilesOpenPosition`: `@NamePattern("%s %s|openPosition,fileDescription")`; поля `fileDescription`, `fileLink`, `fileComment`, `fileDescriptor` (FileDescriptor), `fileOwner` (ExtUser), `fileType`.
- `OpenPositionPriority` — вспомогательный класс с `Integer id` (не entity-таблица).

### 1.5 Потребители (кто ссылается на позицию)

- `JobCandidate` → через `IteractionList.vacancy` (события кандидата) и `RecrutiesTasks.openPosition` (подписки рекрутёров).
- `CandidateCV.toVacancy` — привязка резюме к вакансии (`candidateCV-view-Letter` включает `toVacancy`).
- `Project.openPosition` — обратная связь проект→позиции (view `companyDc` в master-browse).
- `Interview.openPositions` — позиции собеседования (master-browse, контейнер `interviewDc`).
- BPM: `ProcAttachment.procInstance.entity` — вложения процесса согласования.

---

## 2. Слой Выборок Данных (Fetch Plans / Views Layer)

Источник: `modules/global/src/com/company/hunttech/views.xml`. Прямые views сущности `hunttech_OpenPosition`:

| View | Строка | Содержимое |
|---|---|---|
| `openPosition-project-tab-view` | 120 | `openClose`, `vacansyName`, `numberPosition`, `positionType` (position-picker-view), `createTs` |
| `openPosition-picker-view` | 158 | `vacansyID`, `vacansyName`, `openClose` |
| `openPosition-candidate-cv-picker-view` | 164 | + `needLetter`, `templateLetter`, `comment` (для CandidateCV edit) |
| `openPosition-parent-picker-view` | 169 | + `projectName` (project-picker-view) |
| `openPosition-rtasks-browse-view` | 172 | `vacansyName`, `projectName` (+`projectLogo`) |
| `openPosition-rtasks-picker-view` | 178 | `vacansyName`, `openClose`, `grade`, `cityPosition`, `cities`, `positionType`, `projectName`, `owner` |
| `openPosition-browse-view` | 188 | Широкий набор: ID/имя/флаги, приоритет/рейтинг, даты, remoteWork, need-флаги, зарплаты, numberPosition, опыт, internalProject, shortDescription; FK: `grade`, `positionType`, `cityPosition`, `cities` (city-picker-view), `owner` (extUser-picker-view), `parentOpenPosition` (openPosition-picker-view), `projectName` (project-picker-view + `projectLogo`, `projectDepartment.companyName`, `projectOwner`); коллекции: `candidates`, `openPositionComments`, `someFiles` (_minimal) |
| `openPosition-edit-view` | 232 | Поля формы без LOB: всё из browse-view кроме `comment`/`commentEn`/`exercise`/`templateLetter`/`memoForInterview`/LOB-полей + `salaryIE`, `registrationForWork`, `paymentsType`/типы комиссий/проценты, `priorityComment`; FK `projectName` (project-edit-view), `parentOpenPosition` (parent-picker) |
| `openPosition-view` | 400 | `overwrite=true`, `systemProperties=true`, `extends=_local`: широкий legacy-view (`grade`, `cityPosition`, `positionType`, `skillsList` (skillTree-openPosition-tab-view), `parentOpenPosition`, `cities`, `owner`, `projectName`(+department/companyName/projectLogo/projectOwner), `openPositionComments`(+`user.fileImageFace`)) |
| `openPosition-candidate` | 947 | `grade`, `projectName`(+department/companyName), `candidates`(+`reacrutier`, `openPosition`), `positionType` |
| `openPosition-rtasks-view` | 1037 | `projectName`, `candidates`(+`startDate`,`endDate`), `grade`, `cityPosition`, `cities`, `positionType`, `owner`, `laborAgreement`, `skillsList` |
| `openPosition-view-iteraction-list` | 1079 | `projectName`(+department/companyName) |
| `openPosition-iteraction-list-picker-view` | 1086 | Широкий picker для IteractionList: имя/флаги/даты/зарплаты/`comment` + FK (`grade`, `positionType`, `owner`, `cityPosition`, `cities`, `projectName` с projectTree/projectOwner/projectDepartment.companyName) |
| `openPosition-edit-view` (второй) | 1117 | Узкий view «только поля, реально используемые в JobCandidateEdit»: `vacansyName`, `openClose`, `priority`, `lastOpenDate`, `comment`, `owner` |

Views связанных сущностей: `openPositionComment-browse-view` (282), `openPositionComment-edit-view` (288, `systemProperties=true`), `openPositionNews-browse-view` (295), `openPositionNews-edit-view` (303), `someFilesOpenPosition-edit-view` (312); legacy: `openPositionNews-view` (1192), `openPositionComment-view` (1275), `someFilesOpenPosition-view` (1314).

Потребители views: browse использует `openPosition-browse-view` с BATCH-дополнениями (см. §3); edit — `openPosition-edit-view` (+ lazy-reload LOB по вкладкам); `JobCandidateEdit` — узкий `openPosition-edit-view` (строка 1117); `IteractionListEdit` — `openPosition-iteraction-list-picker-view`; `RecrutiesTasks` — `openPosition-rtasks-*`.

---

## 3. Списочные интерфейсы (Browse Screens)

### 3.1 `OpenPositionBrowse` — `hunttech_OpenPosition.browse`

- Дескриптор: `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-browse.xml` (508 строк); контроллер `OpenPositionBrowse.java` (4211 строк), `extends StandardLookup<OpenPosition>`, `@LookupComponent("openPositionsTable")`, `@LoadDataBeforeShow`.
- **Data (`readOnly="true"`)**: коллекция `openPositionsDc` с view `openPosition-browse-view`, дополненной BATCH-подгрузкой `projectName` (projectLogo, projectDepartment.companyName, projectOwner), `positionType`, `owner`, `openPositionComments`, `someFiles`, `cities`; loader `openPositionsDl` `maxResults="40"`, JPQL `select e from hunttech_OpenPosition e order by e.vacansyName` + 12 условий: `priority=:priority`, `lastOpenDate` между now−N и now+1, не в закрытых подписках, в подписках рекрутёра, не в чужих подписках, `openClose=:openClosePos`, `signDraft=:signDraft`, `priority>:paused`, в активных подписках `:recrutier` на `:nowDate`, `internalProject`, `priority>=:rating`, `remoteWork`, `positionType`.
- **Filter**: `excludeProperties="comment,numberPosition,version,createTs,..."`, `properties include=".*" exclude="id|...|shortDescription|commandExperience"`; custom-условия: `projectFilter`, `positionFilter`, `projectOwnerFilter`, `newOpenPositionFilter` (`@between({E}.lastOpenDate, now-3, now, day)`).
- **Таблица `treeDataGrid openPositionsTable`**: `hierarchyProperty="parentOpenPosition"`, `bodyRowHeight="60px"`, `selectionMode=SINGLE`, `editorEnabled=false`, `reorderingAllowed=true`; actions: `create`, `edit`, `remove`, `excel` (скрыт), `list` (listPrintForm). Колонки: `folder`, `icon` (светофор приоритета), `rating` (средний рейтинг комментариев), `companyLogoColumn`, `projectLogoColumn`, `lastOpenCloseColumn`, `vacansyID`, `vacansyName`, `positionType`, `projectName`, `cityPositionList`, `workExperience`, `salaryMinMax`, `numberPosition`, `description`, `testExserice`, `remoteWork`, `queryQuestion`, `memoForCandidateColumn`, `lastCVSend`, `idStatistics`, `owner`, `candidateSendedColumn`, `openPositionActionButtonColumn`. Почти каждая колонка — `componentRenderer` с генератором/провайдером.
- **Генераторы/провайдеры (Java)**: projectLogoColumn и companyLogoColumn (изображения + HTML-подсказки, lazy-описания проектов/компаний), icon (иконки traffic-lights по приоритету), rating (avg rating из комментариев), vacansyID, vacansyName (HTML: имя + проект + владелец), positionType (RU/EN, lazy), projectName, cityPositionList, workExperience, salaryMinMax (формат «X т.р./Y т.р.», tooltip), numberPosition, description (наличие comment), testExserice (наличие exercise), remoteWork (иконка+подпись), queryQuestion (шаблон письма), memoForCandidateColumn, lastCVSend, idStatistics (статистика взаимодействий: счётчики по типам взаимодействий с учётом родительских/дочерних), candidateSendedColumn (количество отправленных CV), openPositionActionButtonColumn (popup-кнопки: открыть/закрыть, комментарий, отчёты, подписка, подбор кандидатов), folder (иконка папки для вакансий с детьми), `rowStyleProvider`, `detailsGenerator`, styleProvider'ы (стили строк по приоритету/remoteWork/зарплате).
- **Кэширование (PostLoad)**: LOB-флаги (comment, exercise, memo, templateLetter — включая templateLetter проекта/департамента), описания проектов/компаний, `positionEn/RuNameCache`, агрегаты (активные рекрутёры по позициям, отправленные CV, средний рейтинг), подписчики `subscribersByPosition`, `positionsWithChildren`, статистика взаимодействий.
- **Жизненный цикл**: `onBeforeShow` (init фильтра, чекбоксов, urgent-позиций), `onAfterShow` (x2), подписки на `remoteWorkLookupField`/`notLowerRatingLookupField` (фильтры), `checkBoxOnlyOpenedPosition`, `checkBoxOnlyNotPaused`, `checkBoxOnlyMySubscribe`, `signDraftCheckBox`, `openPositionsTable` EditorClose.
- **Кнопки панели**: create/edit/remove; `openCloseButton` (popup «Открыть/Закрыть» — actions в XML закомментированы), `buttonSubscribe` (подписка на выбранную), `groupSubscribe` (скрыта), `buttonExcel` (скрыта), `suggestCandidateButton` (скрыта), `reportsPopupButton` (скрыта; action `listPrintFormAction` → `getMemoForCandidate`), `setRatingButton` (popup: `setRatingComment` — выставление рейтинга-комментария, `viewRatingComment` — просмотр).
- **Urgent-позиции**: groupBox `urgentlyPositons` + горизонтальный `scrollBox` с кнопками срочных вакансий (`setUrgentlyPositios(priority)` по `QUERY_URGENTLY_POSITIONS`), `clearUrgentFilter` — сброс.
- **Прочее**: `subscribePosition` (подписка текущего рекрутёра), `groupSubscribe` (массовая), `removeCandidatesWithConsideration` (снятие кандидатов из рассмотрения), `getMemoForCandidate` (мемо-отчёт), `setRatingComment`/`openPositionCommentViewInvoke` (экран комментариев `OpenPositionCommentsView`), `openCloseButtonInvoke`/`WithCommentInvoke` (закрытие позиции, при закрытии родителя проверяется отсутствие незакрытых детей — `msgCanNotCloseWithout`).

### 3.2 `OpenPositionMasterBrowse` — `hunttech_OpenPositionMaster.browse`

- Дескриптор `open-position-master-browse.xml` (623 строки), контроллер `OpenPositionMasterBrowse.java` (664 строки), `extends StandardLookup<OpenPosition>`, `@LookupComponent("openPositionsTable")`.
- **Data**: `interviewDc` (Interview, `order by dateInterview`, вложенная коллекция `openPositions`), `userDc` (активные `sec$User`), `jobCandidateDc` (JobCandidate `_local`+firstName/secondName/middleName/personPosition, без «(не использовать)»), `openPositionsDc` (типы `Position` из незакрытых вакансий + условие по приоритету/commandCandidate), `companyDc` (компании, вложенные departmentOfCompany.projectOfDepartment.openPosition).
- **Layout**: `accordion mainAccordion` — секции: кандидат (`jobCandidateField` lookup + `notLowerRatingLookupField`), таблица типов позиций (`openPositionsTable`), компании (`companyTable`), проекты (`projectNameTable`), вакансии (`vacansyNameTable`).
- **Контроллер**: `setJobCandidate`/`getJobCandidate` (передача кандидата из вызывающего экрана), каскадные фильтры при выборе в таблицах (`onOpenPositionsTableSelection`, `onCompanyTableSelection`, `onProjectNameTableSelection`, `onVacansyNameTableSelection`), `clearFilters`, `nextTab`/`previonsTab` (навигация по аккордеону), `saveLetter`, подписки `onMainAccordionSelectedTabChange`, `onJobCandidateFieldValueChange`.

### 3.3 Наследники browse

- `OpenPositionRecruitingBrowse` — `hunttech_OpenPositionRecruiting.browse`, дескриптор `openpositionrecruiting/open-position-browse-recrutiting.xml`, `extends OpenPositionBrowse` (рекрутинговый вид).
- `OpenPositionOutstaffBrowse` — `hunttech_OpenPositionOutstaff.browse`, дескриптор `openpositionoutstaff/open-position-outstaff-browse.xml`, `extends OpenPositionBrowse` (outstaff-вид; детали — `OpenPositionOutstaffDetailScreenFragment`).
- `ProdOpenPositionBrowse` — `hunttech_ProdOpenPosition.browse`, дескриптор `prod-open-position-browse.xml` (44 строки), `extends OpenPositionBrowse` (обёртка для прод-конфигурации).

---

## 4. Формы редактирования (Edit Screens)

### 4.1 `OpenPositionEdit` — `hunttech_OpenPosition.edit`

- Дескриптор `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml` (1104 строки); контроллер `OpenPositionEdit.java` (3338 строк), `extends StandardEditor<OpenPosition>`, `@EditedEntityContainer("openPositionDc")`, `@LoadDataBeforeShow`. `dialogMode height="800px" width="1100px"`.
- **Контейнеры/loaders**: `openPositionDc` (view `openPosition-edit-view`); `laborAgreementDc` (`LaborAgreement` через `join e.openPositions op where op=:openPosition`); `commentsOpenPositionDc` (`OpenPositionComment` по `openPosition`, `order by dateComment desc`); `someFilesesDc` (SomeFiles по `openPosition`); `openPositionSkillsListsDc` (SkillTree по `openPosition`, `order by skillName`); `procAttachmentsDc` (BPM `ProcAttachment` по `procInstance.entity.entityId=:entityId`, cacheable); `openPositionParentDc` (родительские: не закрытые и `commandCandidate != 0`, cacheable); `positionTypesDc` (Position без «(не использовать)», cacheable); `openPositionNewsDc` (News по позиции + `priorityNews`, cacheable); `projectNamesDc` (Project: не закрытые, без «используется в открытых» и т.д., cacheable); `companyNamesDc`; `companyDepartamentsDc` (по выбранной компании); `citiesDc` (cacheable); `gradeDc` (cacheable). Facets: `timer closedVacancyTimer` (delay 60000, autostart=false, repeating).
- **Шапка**: `groupBox msgTitle` — `signDraftLabel` (h2, «черновик»), `labelOpenPosition` (table-wordwrap, имя), `labelTopComissionRecrutier`/`labelTopComissionResearcher` (h4, html), `projectLogoImage` (ovaFallbackImage 70px, fallback `icons/no-company.png`), `projectOwnerImage` (ovaFallbackImage 70px oval, fallback `icons/no-programmer.jpeg`), `closedVacancyInfoLabel` (WARNING — обратный отсчёт до закрытия).
- **Вкладки `tabSheetOpenPosition` (stylename framed)**:
  - `tabOpenPosition` «О вакансии» (USERS): `vacansyIDTextField`, `vacansyNameField` (required), `gradeLookupPickerField`, кнопка `generateVacancyNameFieldButton` (генерация имени), `projectNameField` (+ companyName/департамент/город), `commandOrPosition` (радио «Команда/Вакансия»), `parentOpenPositionField`, `remoteWorkField`, `registrationForWorkField` (optionIconProvider/optionStyleProvider), блок зарплат (`openPositionFieldSalaryMin/Max` с валидаторами, `salaryIE`, `salaryFixLimit`, `salaryCandidateRequest`, `salaryComment`, `outstaffingCost`), `numberPosition`/`more10...`, `workExperienceGroupBox` (опыт), аккордеоны описаний: `openPositionRuTabAccordion` (RichTextArea `comment`), `openPositionEnTabAccordion` (`commentEn`), `openPositionStandartDescriptionAccorden` (`shortDescription`), `openPositionWhoIsThisGuyAccorden`; `openClosePositionCheckBox` (+ `disableEnableFields`), `needExerciseCheckBox`/exercise, needLetter/templateLetter, needMemoForInterview/memoForInterview, `openPostionNewsDataGrid` (новости, detailsGenerator), комментарии.
  - `laborAgreementTab` — таблица трудовых соглашений (многие-ко-многим, синхронизация в контроллере `syncLaborAgreementToEntity`).
  - `tabPayments` — `groupBoxPaymentsResearcher`, `groupBoxPaymentsRecrutier`, `groupBoxPaymentsDetail`: радио-группы `paymentsType`, `typeCompanyComission`, `typeSalaryOfResearcher`, `typeSalaryOfRecrutier`, поля процентов/сумм, `checkBoxUseNDFL`, итоговые `labelRecrutierSalary`/`labelResearcherSalary`.
  - `tabFiles` — таблица `someFilesTable` (файлы позиции, `newEntitySupplier`).
  - `tabExercise` — RichTextArea тестовое задание (lazy).
  - `tabMemoForInterview` — RichTextArea памятка (lazy).
  - `tabTemplateLetter` — RichTextArea шаблон письма (lazy).
  - `tabSkills` — таблица навыков (SkillTree).
  - `tabOpenPositionNews` — новости.
  - `tabApproval` — `procActionsBox` + `procActionsFragment` (BPM-согласование, вложения процесса).
  - `commentsTab` — лента комментариев (`commentsOpenPositionDc`), scrollBox, поле ввода + `replyButtonInvoke`/`createComment`.
- **Lifecycle и lazy-загрузка (Java)**: флаги `mainTabLobsLoaded`, `exerciseLoaded`, `memoLoaded`, `templateLetterLoaded`, `skillsLoaded`, `filesLoaded`, `commentsTabLoaded`, `laborAgreementLoaded`; `onTabSheetOpenPositionSelectedTabChange` → `loadMainTabLobs`, `loadExerciseLob`, `loadMemoForInterviewLob`, `loadTemplateLetterLob`, `loadSkillsList`, `loadSomeFiles`, `loadCommentsTab`, `loadLaborAgreement` — LOB-поля догружаются reload'ом с view при первом открытии вкладки (не тянутся в edit-view). `screenFullyLoaded` блокирует ValueChange RichTextArea до `onAfterShow`.
- **Поведение**: `onBeforeShow` (init: projectName, positionType-описания, таймер, новости, disableEnableFields, setRadioButtons, setHiddeField, иконки файлов, approval-процесс, подписки-группы); `onBeforeCommitChanges` — `checkDuplicatePositionId` (дубль `vacansyID`), `publishEventMessage`, `sendClosePositionMessage`/`sendOpenPositionMessage` (тексты из `OpenPositionServiceBean`), `getAllSubscibers`/`getSubscriberMaillist`/`getRecrutiersMaillist` (рассылка email); `onBeforeCommitChanges1/3/4` — подготовка коллекций (skills, laborAgreement, комментарии); `onAfterCommitChanges` — `notifyTelegramOpenPositionChange` (Telegram-уведомление), `openCloseChildVacancy` (синхронизация дочерних вакансий при открытии/закрытии родителя); `onPriorityFieldValueChange` → `setClosingWeek` (авто-дата закрытия через неделю); `onCompanyNameFieldValueChange` → `setCityNameOfCompany`; `onProjectNameFieldValueChange1` → подстановка позиции/описаний; `onCommandOrPositionValueChange`, `onParentOpenPositionFieldValueChange` (предзаполнение projectName); валидаторы зарплат `openPositionFieldSalaryMinValidator/MaxValidator`; пересчёт комиссий при `radioButtonGroupPaymentsTypeValueChange`, `textFieldPercentOrSumValueChange` и т.д.
- **Диалог выбора городов**: `hunttech_SelectCitiesLocation` (`select-cities-location.xml`, 56 строк; `SelectCitiesLocation.java`, 95 строк): radioButtonGroup `countries` + `twinColumn citiesLocationTwinColumn` (property `cities`, reorderable, addAllBtnEnabled), loader `cityOptionDc` по `cityRegion.regionCountry.countryRuName=:country`; применяется для массовой привязки городов позиции.

### 4.2 Диалоги просмотра

- `QuickViewOpenPositionDescription` (`hunttech_QuickViewOpenPositionDescription`, `quick-view-open-position-description.xml`): быстрый просмотр требований к сопроводительному письму (`setCvRequirement`).
- `TextViewScreen` (`text-view-screen.xml`): просмотр произвольного текста.

---

## 5. Компоненты и Фрагменты (UI Fragments & Dialogs)

- `OpenPositionDetailScreenFragment` (`hunttech_OpenPositionDetailScreenFragment`, `openpositionfragments/open-position-detail-screen-fragment.xml` + Java 140 строк): детальная карточка вакансии — логотип компании/проекта (`setDefaultCompanyLogo`), удалённость (`setRemoteLabel`), подписчики-рекрутёры (`setSubscribersRecruters`), набор labels (`setLabels`), установка сущности (`setOpenPosition`).
- `OpenPositionOutstaffDetailScreenFragment` (`hunttech_OpenPositionOutstaffDetailScreenFragment`) — `extends OpenPositionDetailScreenFragment`, outstaff-переопределение `setLabels`.
- `QuickViewOpenPositionDescription` / `TextViewScreen` — см. §4.2.
- `Skillsbar` (`com.company.hunttech.web.screens.fragments.Skillsbar`) — фрагмент навыков, используется browse-экранами.
- `procActionsFragment` — штатный фрагмент BPM-процесса CUBA (вкладка «Согласование» edit-экрана).
- Cross-form FK: `OpenPosition` выбирается в `IteractionList` (поле «Вакансия», view `openPosition-iteraction-list-picker-view`), в `RecrutiesTasks` (подписки, view `openPosition-rtasks-picker-view`), в `CandidateCV` (`toVacancy`, view `openPosition-candidate-cv-picker-view`), в `JobCandidateEdit` (узкий `openPosition-edit-view`, строка 1117).

---

## 6. Инструкция по развертыванию с нуля (Deployment Guide)

1. **Схема БД**: таблица `HUNTTECH_OPEN_POSITION` создаётся CUBA-механизмом из `modules/core/db/init/{hsql,postgres}/10.create-db.sql`; последующие изменения — Liquibase-подобные миграции `modules/core/db/update/hsql/20/20*.sql` (применяются через `./gradlew updateDb --no-daemon`, фиксируются в `sys_db_changelog`). Индексы: `OPEN_CLOSE`, `PRIORITY`, `VACANSY_NAME` (в `@Table`).
2. **Сборка и деплой** (локальный стенд, см. `docs/LOCAL_DATABASE.md`): PostgreSQL 11 (launchd `com.itpearls.postgresql11`, `cuba@127.0.0.1:5432`) → `./gradlew :app-web:buildScssThemes --no-daemon` (при SCSS-правках) → `./gradlew restart --no-daemon` → проверки `http://localhost:8080/hrm/` = 200 и widgetset `AppWidgetSet.nocache.js` = 200; старт Tomcat после поднятия PostgreSQL (иначе TelegramBotComponent падает с «Parent Spring context is null»).
3. **Полнотекстовый поиск**: сущности `OpenPosition`, `OpenPositionNews`, `OpenPositionComment` включены в `modules/core/src/com/company/hunttech/fts.xml` (FTS-индексы создаются штатно при деплое).
4. **Автотесты**: `modules/core/test/.../core/OpenPositionServiceTest.java`, `OpenPositionNewsServiceTest.java`, `OpenPositionCommentServiceTest.java` (интеграционные, `HunttechTestContainer`); прогон `./gradlew :app-core:test --tests "com.company.hunttech.core.OpenPosition*" --no-daemon`.
5. **Кэш и производительность**: справочники-options (страны/города/грeйды/типы позиций/проекты/родительские вакансии) — `cacheable="true"` loaders; browse-view сужен (без LOB), LOB-признаки считаются batch-запросами в `PostLoad`; edit-форма грузит LOB lazy по вкладкам.
6. **Интеграции**: Telegram-уведомления об открытии/закрытии (`TelegramService`, тексты `OpenPositionServiceBean.getOpenPosition{Open,Close}{Short,Long}Message`), email-рассылки подписчикам, BPM-согласование (`OpenPositionApprovalBean.updateState`, вкладка `tabApproval`), новости позиции (`setOpenPositionNewsAutomatedMessage`).

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-01 | Создание Architecture Spec по триггеру: полный разбор entity, views.xml, browse/edit экранов, фрагментов, сервисов и интеграций из кода. |
