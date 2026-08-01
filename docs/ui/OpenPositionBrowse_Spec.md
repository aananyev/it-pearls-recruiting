# OpenPositionBrowse — UI Spec

> Форма: справочник вакансий (Browse) · Controller: `hunttech_OpenPosition.browse`
> Дескриптор: `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-browse.xml`
> Контроллер: `OpenPositionBrowse.java` (4211 строк) · Проект: **HRM HuntTech** (CUBA 7.3)

---

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Экран — главный рабочий список открытых позиций (вакансий) рекрутинговой системы HRM HuntTech. Рекрутер видит иерархию «родительская вакансия → дочерние» (дерево по `parentOpenPosition`), по каждой строке — логотип проекта и компании, «светофор» приоритета, средний рейтинг, города, вилку зарплаты, требуемый опыт и набор индикаторов готовности вакансии: наличие описания, тестового задания, памятки к собеседованию, шаблона сопроводительного письма, а также статистику взаимодействий и количество отправленных кандидатов. Экран решает задачи: мониторинг портфеля вакансий, фильтрация по 12 бизнес-условиям, управление статусами (открыть/закрыть с проверкой дочерних), подписка рекрутёра на позицию, выставление рейтинга-комментария и быстрый переход к кандидатам и деталям.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из главного меню (пункт, иконка `COMPASS`) и как lookup-экран подбора позиции (методы `StandardLookup`). Из строки/кнопок экран открывает: `hunttech_OpenPosition.edit` (редактирование), `OpenPositionCommentsView`/`OpenPositionCommentEdit` (комментарии-рейтинги), `RecrutiesTasksGroupSubscribeBrowse` (групповая подписка), `Suggestjobcandidate` и `JobCandidateSimpleBrowse`/`JobCandidateSimpleMailBrowse` (подбор кандидатов), `hunttech_QuickViewOpenPositionDescription` (просмотр описания), `JobCandidateSimpleBrowse` (список кандидатов по позиции). Детали строки — фрагмент `OpenPositionDetailScreenFragment`. Наследники: `hunttech_OpenPositionRecruiting.browse`, `hunttech_OpenPositionOutstaff.browse`, `hunttech_ProdOpenPosition.browse`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → загрузка `openPositionsDc` (maxResults 40, view `openPosition-browse-view` + BATCH) → `PostLoad` строит пакетные кэши (наличие LOB, агрегаты рекрутёров/CV/рейтинга, подписчики, дочерние позиции, статистика взаимодействий) → рендер дерева с генераторами колонок.
- Фильтры → чекбоксы статусов и lookup-поля меняют JPQL-параметры → перезагрузка коллекции.
- «Срочные вакансии» → лента кнопок позиций с критическим приоритетом; кнопка «Сброс» очищает фильтр.
- Закрытие позиции → проверка «нет незакрытых дочерних» (`msgCanNotCloseWithout`) → закрытие с/без комментария → уведомления подписчиков.
- Подписка → создание `RecrutiesTasks` для текущего рекрутёра (или группы).
- Рейтинг → окно выставления/просмотра комментария-рейтинга позиции.

---

## 1. Точка вызова и контекст (Invocation & Context)

- `@UiController("hunttech_OpenPosition.browse")`, `@UiDescriptor("open-position-browse.xml")`, `@LookupComponent("openPositionsTable")`, `@LoadDataBeforeShow`.
- Меню: пункт с иконкой `COMPASS` (`web-menu.xml`).
- Режимы: обычный browse и lookup-режим (панель `lookupActions` скрыта, показывается при открытии как lookup).
- `dialogMode height="800" width="1000"`.

## 2. Связь с моделью данных (Data & Entity Binding)

- Сущность: `OpenPosition` (`hunttech_OpenPosition`).
- Контейнер: `openPositionsDc` (collection), view `openPosition-browse-view` + BATCH-дополнения: `projectName` (projectLogo, projectDepartment.companyName, projectOwner), `positionType`, `owner`, `openPositionComments`, `someFiles`, `cities` — все вложенные пути, читаемые генераторами (view integrity).
- Loader: `openPositionsDl` (maxResults 40), JPQL `select e from hunttech_OpenPosition e order by e.vacansyName` + 12 условий (приоритет, `lastOpenDate` между, подписки: свои/не чужие/в активных, `openClose`, `signDraft`, пауза, `internalProject`, рейтинг, `remoteWork`, `positionType`).
- Filter `vacancyFilter`: `applyTo="openPositionsTable"`, `dataLoader="openPositionsDl"`, excludeProperties (LOB и служебные); custom-условия: `projectFilter`, `positionFilter`, `projectOwnerFilter`, `newOpenPositionFilter` (lastOpenDate за 3 дня).
- Колонки: `folder`, `icon`, `rating`, `companyLogoColumn`, `projectLogoColumn`, `lastOpenCloseColumn`, `vacansyID`, `vacansyName`, `positionType`, `projectName`, `cityPositionList`, `workExperience`, `salaryMinMax`, `numberPosition`, `description`, `testExserice`, `remoteWork`, `queryQuestion`, `memoForCandidateColumn`, `lastCVSend`, `idStatistics`, `owner`, `candidateSendedColumn`, `openPositionActionButtonColumn` — почти все с `componentRenderer` и провайдерами контроллера.

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

- Родитель: меню → browse.
- Дочерние: `OpenPositionEdit` (редактирование), `OpenPositionCommentEdit`/`OpenPositionCommentsView` (комментарии), `RecrutiesTasksGroupSubscribeBrowse` (подписки), `Suggestjobcandidate`/`JobCandidateSimpleBrowse`/`JobCandidateSimpleMailBrowse` (кандидаты), `QuickViewOpenPositionDescription` (описание).
- Фрагменты: `OpenPositionDetailScreenFragment` (детали строки), `Skillsbar`.
- Наследники контроллера: `OpenPositionRecruitingBrowse`, `OpenPositionOutstaffBrowse`, `ProdOpenPositionBrowse`.

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Lifecycle

- `onInit` → карты options (remoteWork, приоритеты, опыт), список пользователей.
- `onOpenPositionsDlPostLoad` → пересчёт всех кэшей browse (LOB-признаки, агрегаты, подписчики, дети, статистика) пакетными JPQL-запросами по id.
- `onBeforeShow` / `onAfterShow` (×2) → инициализация чекбоксов и фильтров, `initOpenCloseButton` (подключение действий popup), `setButtonsEnableDisable` по ролям (`GetRoleService`, `StandartRoles`), `initGroupSubscribeButton`.

### 4.2 Скрытые вычисления

- Генераторы колонок (~25 `@Install`): логотипы (lazy-описания проекта/компании), светофор приоритета, средний рейтинг (`avgRating`), HTML-название вакансии (имя+проект+владелец), вилка зарплаты, статистика взаимодействий (с учётом дочерних позиций), popup-меню строки (`initActionButton*`).
- Индикаторы `setSign*` (закрытие, комментарии, памятка, тестовое, письмо, описание, вложения).
- Стили строк (`rowStyleProvider`, `styleProvider`) по приоритету/статусу/зарплате.
- Кэши: `lazyCommentTextCache` и др. — LOB-тексты догружаются по одному при наведении (без N+1 в batch).

### 4.3 Валидация/сохранение

- Экран read-only: `data readOnly="true"`; изменения — через дочерние экраны (edit, комментарии, подписки).
- Закрытие позиции: проверка дочерних незакрытых; при наличии — диалог запрета.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

- `openPositionsTable.create/edit/remove` — стандартные CRUD-действия таблицы.
- `openCloseButton` (popup) — действия «Закрыть» / «Закрыть с комментарием» подключаются в `initOpenCloseButton`; цепочка: выбор → проверка дочерних → `openCloseVacancy` → уведомления.
- `buttonSubscribe` → `subscribePosition` — подписка текущего рекрутёра.
- `groupSubscribe` (скрыта) → массовая подписка.
- `setRatingButton` (popup) → `setRatingComment` (выставить рейтинг) / `openPositionCommentViewInvoke` (просмотр).
- `reportsPopupButton` (скрыт) → `listPrintFormAction` → `getMemoForCandidate` (печатная форма «Памятка для кандидата»).
- `suggestCandidateButton` (скрыт) → `suggestCandidateButton` (подбор кандидатов).
- Чекбоксы `checkBoxOnlyOpenedPosition` / `signDraftCheckBox` / `checkBoxOnlyNotPaused` / `checkBoxOnlyMySubscribe` → переустановка JPQL-параметров → перезагрузка.
- `notLowerRatingLookupField` / `remoteWorkLookupField` → фильтры приоритета и формата работы.
- `clearUrgentPos` → `clearUrgentFilter` (сброс срочной ленты).
- `lookupSelectAction`/`lookupCancelAction` — выбор/отмена в lookup-режиме.
- Popup-кнопки строк (`openPositionActionButtonColumn`): открыть/закрыть, комментарии, отчёты, подписка, подбор кандидатов (`initActionButton*`).

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- `layout` (expand `openPositionsTable`): `groupBox urgentlyPositons` (лента срочных, collapsable) → `filter vacancyFilter` (collapsed) → `radioButtonGroup subscribeRadioButtonGroup` → `treeDataGrid openPositionsTable` (hierarchyProperty `parentOpenPosition`, bodyRowHeight 60px, single-selection) с `buttonsPanel openPositionButtonsPanel` → `hbox vacancyFilterCheckBoxesHBox` (чекбоксы статусов слева, фильтры справа) → `hbox lookupActions` (скрыт).
- Стили/классы: `table-wordwrap`, `large`, `borderless`, `h2`/`h4`; колонки фиксированной ширины для индикаторов (35–120px).

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-01 | Создание Spec; полное inline-документирование XML (66 комментариев), бизнес-id всем элементам (filter→vacancyFilter, buttonsPanel→openPositionButtonsPanel, id для hbox-контейнеров и lookup-кнопок), javadoc-покрытие контроллера (181 метод + класс). |
