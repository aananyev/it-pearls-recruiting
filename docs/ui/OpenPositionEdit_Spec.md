# OpenPositionEdit — UI Spec

> Форма: редактирование позиции (Edit) · Controller: `hunttech_OpenPosition.edit`
> Дескриптор: `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml`
> Контроллер: `OpenPositionEdit.java` (3338 строк) · Проект: **HRM HuntTech** (CUBA 7.3)

---

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Форма — «паспорт» вакансии: рекрутер ведёт всю информацию о позиции от названия и внутреннего ID до схемы оплаты подрядчиков и текстов для кандидатов. Здесь задаются: принадлежность проекту/компании/городу, тип позиции и грейд, приоритет и дата автозакрытия, вилка зарплаты и зарплата ИП, условия для ресурсера и рекрутера (процент/сумма, НДФЛ), описание на русском и английском, тестовое задание, памятка к собеседованию, шаблон сопроводительного письма, дерево навыков, файлы, новости и комментарии-рейтинги. Форма интегрирована с BPM-согласованием и рассылкой уведомлений (email/Telegram) подписчикам при открытии/закрытии позиции.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из `hunttech_OpenPosition.browse` (действия create/edit), из lookup-экранов подбора и из `JobCandidateEdit` (просмотр позиции кандидата). Из формы доступны: окно выбора городов `hunttech_SelectCitiesLocation` (кнопка «Добавить города»), справочники-просмотры picker-полей (проект, компания, департамент, город), вкладка «Согласование» (фрагмент `bpm_ProcActionsFragment`), окно подписки (кнопка «Подписаться»). После сохранения — возврат в browse; при открытии/закрытии — синхронизация дочерних позиций.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → загрузка `openPositionDc` (view `openPosition-edit-view`, без LOB) → `onAfterShow` догружает LOB основной вкладки, логотипы, новости, настройки и approval-процесс.
- Переключение вкладок → lazy-загрузка LOB/коллекций при первом открытии (8 флагов `*Loaded`).
- Изменение проекта/компании/типа позиции → каскадная подстановка (департаменты, город, описания, название вакансии).
- Сохранение → валидация вилки зарплаты и уникальности `vacansyID` → сбор подписчиков и уведомления (email/Telegram) → после коммита синхронизация дочерних позиций.
- Приоритет → авто-дата закрытия +7 дней; таймер 60 с обновляет обратный отсчёт и автозакрывает вакансию.

---

## 1. Точка вызова и контекст (Invocation & Context)

- `@UiController("hunttech_OpenPosition.edit")`, `@UiDescriptor("open-position-edit.xml")`, `@EditedEntityContainer("openPositionDc")`, `@LoadDataBeforeShow`.
- `dialogMode height="800px" width="1100px"` (модальное окно).
- Источники вызова: browse `OpenPositionBrowse` (create/edit), lookup-экраны, `JobCandidateEdit` (только просмотр связанной позиции).

## 2. Связь с моделью данных (Data & Entity Binding)

- Сущность: `OpenPosition`; контейнер `openPositionDc` (instance, view `openPosition-edit-view` — все поля вкладок кроме LOB).
- Коллекции вкладок: `laborAgreementDc` (LaborAgreement через join openPositions), `commentsOpenPositionDc` (OpenPositionComment, dateComment desc), `someFilesesDc` (SomeFilesOpenPosition), `openPositionSkillsListsDc` (SkillTree, skillName), `procAttachmentsDc` (bpm$ProcAttachment, cacheable), `openPositionNewsDc` (OpenPositionNews + priorityNews, cacheable).
- Options-контейнеры: `openPositionParentDc` (родительские позиции, cacheable), `positionTypesDc` (Position без «(не использовать)», cacheable), `projectNamesDc` (Project не закрытые, cacheable), `companyNamesDc`, `companyDepartamentsDc`, `citiesDc` (cacheable), `gradeDc` (cacheable).
- Facets: `timer closedVacancyTimer` (delay 60000, autostart=false, repeating) — автозакрытие по `closingDate`.
- Lazy-догрузка LOB: `loadPositionWithDescriptionLobs` (reload с view на LOB-поля) по вкладкам.

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

- Родитель: `OpenPositionBrowse` и lookup-экраны.
- Дочерние/диалоги: `SelectCitiesLocation` (выбор городов), `ProcActionsFragment` (BPM), справочные lookup-экраны picker-полей (Project, Company, City, Grade, Position).
- Фрагменты: `bpm_ProcActionsFragment` (вкладка «Согласование»).
- Связанные экраны: `JobCandidateEdit` использует узкий view `openPosition-edit-view` (строка 1117 views.xml).

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Lifecycle

- `onInit` → карты options; `onBeforeShow` (×2) → инициализация типа позиции, проекта, таймера, новостей, блокировок, иконок файлов, approval-процесса, подписки-группы.
- `onAfterShow` → lazy-загрузка LOB основной вкладки, логотипы проекта/владельца (`initProjectImagesOnOpen`), `setTopLabel` (шапка: комиссии, статус), `setOpenPositionNews`.
- `onTabSheetOpenPositionSelectedTabChange` → lazy-загрузка вкладки при первом открытии (флаги `mainTabLobsLoaded`, `exerciseLoaded`, `memoLoaded`, `templateLetterLoaded`, `skillsLoaded`, `filesLoaded`, `commentsTabLoaded`, `laborAgreementLoaded`).
- `closedVacancyTimer` → тик каждые 60 с: обновление обратного отсчёта, автозакрытие.

### 4.2 Скрытые вычисления

- Каскады: проект → компания/департамент/город/описания/название; тип позиции → описания (RU/EN, «кто этот парень», стандартное) и навыки; приоритет → `setClosingWeek` (+7 дней); город → название.
- Пересчёт комиссий: `calculateComission` (компания/ресурсер/рекрутер), `calculateResearcherSalary`/`calculateRecrutierSalary`, итоговые подписи `setResearcherSalaryLabel`/`setRecrutierSalaryLabel`, HTML-шапка `setTopLabel`.
- Генераторы: `openPostionNewsDataGridDetailsGenerator` (детали новости), `skillImageColumnRenderer`, `openPositionSkillsListTableIsCommentColumnGenerator`.
- Сканирование описания: `addShortDescription` (короткое описание) и `rescanJobDescription`/`reloadSkillsForOpenPositionTab` (пересборка навыков).

### 4.3 Валидация/сохранение

- `openPositionFieldSalaryMinValidator`/`MaxValidator` — вилка мин ≤ макс.
- `onBeforeCommitChanges` → `checkDuplicatePositionId` (дубль `vacansyID` → диалог), `publishEventMessage`, сбор подписчиков (`getAllSubscibers`, `getSubscriberMaillist`, `getRecrutiersMaillist`), `sendOpenPositionMessage`/`sendClosePositionMessage`.
- `onBeforeCommitChanges1/3/4` → синхронизация коллекций (навыки, соглашения, комментарии) и статусов.
- `onAfterCommitChanges` → `notifyTelegramOpenPositionChange`; `onAfterCommitChanges1` → `openCloseChildVacancy` (дочерние позиции).
- Ограничения: `vacansyNameField` required, `positionTypeField`/`remoteWorkField`/`numberPositionField`/`priorityField` required; LOB-редакторы блокируются до полной загрузки (`screenFullyLoaded`).

## 5. Логика управляющих элементов (Actions & Buttons Logic)

- `generateVacancyNameFieldButton` → `generateNameFieldButton` → `generateVacancyName` (название по типу/проекту/городу).
- `addCity` → `addListCity` — окно `SelectCitiesLocation` (массовый выбор городов по стране).
- `setSalaryFieldButton` → `setSalaryFieldButtonInvoke` — подстановка зарплаты из рейтов аутстафа.
- `scanJDButton` → `addShortDescription` — извлечение короткого описания; `rescanSkills` → `rescanJobDescription` — пересборка навыков.
- `setRatingButton`-аналог: кнопки комментариев в ленте — `createComment`, `replyButtonInvoke`.
- `addOpenPositionNewsButton` → `addOpenPositionNewsButton` — создание новости.
- `subscribePositionButton` → `subscribePosition` — подписка рекрутёра.
- `windowCommitAndCloseButton` → `windowCommitAndClose` (сохранить и закрыть); `windowCloseButton` → закрыть без сохранения.
- Чекбоксы вкладок: `needExerciseCheckBox`, `needMemoCheckBox`, `needLetterCheckBox` — показывают/блокируют редакторы; `openClosePositionCheckBox` (скрыт) → `disableEnableFields`; `signDraftCheckBox` → черновик.
- Таблицы: `laborAgreementDataGrid` (create/edit/remove, inline-редактирование), `someFilesTable` (add/create/edit/remove), `openPostionNewsDataGrid` (create/remove), `openPositionSkillsListTable` (только просмотр, actions закомментированы).

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- `layout expand="tabSheetGroupBox"`: `groupBox positionHeaderGroupBox` (шапка: label-заголовок, комиссии, логотип проекта `projectLogoImage`, аватар владельца `projectOwnerImage` 70×70, `closedVacancyInfoLabel`) → `vbox tabSheetGroupBox` → `tabSheet tabSheetOpenPosition` (stylename framed, 12 вкладок) → `vbox forExpand` (владелец `ownerTextField` borderless + `editActions`: подписка, сохранить, закрыть).
- Вкладки: «О вакансии» (scrollBox `mainTabScrollBox`, секции groupBox: настройки вакансии, команда/вакансия, тип проекта, количество персонала, зарплата; аккордеон описаний `openPositionAccordion`), «Трудовые соглашения» (`laborAgreementGroupBox`), «Оплата» (3 колонки `companyPaymentsVBox`/`researcherPaymentsVBox`/`recrutierPaymentsVBox`, детали свёрнуты), «Описание должности» (`tabJobDescription`), «Файлы», «Тестовое задание», «Памятка», «Шаблон письма», «Навыки» (treeDataGrid по skillTree), «Новости», «Согласование» (BPM), «Комментарии» (scrollBox-лента).
- Стили: `large` (ключевые поля), `light` (groupBox-секции), `h2`/`h4` (шапка), `borderless` (владелец), `table-wordwrap`, `framed` (tabSheet).

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-01 | Создание Spec; полное inline-документирование XML (168 комментариев), бизнес-id всем элементам (buttonsPanel→laborAgreement/someFiles/openPositionNewsButtonsPanel, id для всех hbox/vbox/groupBox-контейнеров и кнопок), javadoc-покрытие контроллера (163 метода + класс). |
