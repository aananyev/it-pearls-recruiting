# OpenPositionEditPreview — изолированный preview новой компоновки вакансии

## Назначение и бизнес-смысл (What & Why)

`OpenPositionEditPreview` — параллельный экран проверки новой компоновки карточки вакансии HRM HuntTech. Он нужен для безопасной визуальной доводки одного из наиболее важных рабочих экранов рекрутера до того, как новая компоновка заменит действующий `OpenPositionEdit`.

Preview сохраняет существующую модель вакансии и весь бизнес-контракт legacy-экрана: редактирование реквизитов, команды или одиночной вакансии, проекта, заказчика, локации, количества позиций, зарплатных параметров, договоров, оплат, описаний, файлов, тестового задания, памятки интервью, шаблона письма, навыков, новостей, согласования и комментариев. Новые сущности, поля, сервисы, JPQL, loaders, views и сценарии сохранения не вводятся.

Архитектура оставляет место для будущих AI-помощников анализа вакансии и поиска кандидатов за счёт самостоятельной рабочей области и смысловых accordion-секций. В текущем этапе AI-компоненты, запросы и настройки отсутствуют.

## UI Context & Navigation

Экран имеет отдельный идентификатор `hunttech_OpenPosition.editPreview` и маршрут `open-position-edit-preview`. Он не зарегистрирован в меню, не указан в browse-экранах и не подменяет стандартный editor `hunttech_OpenPosition.edit`.

Preview предназначен для ручной проверки администратором или разработчиком, которому выдано разрешение на экран. Для существующей вакансии используется прямой URL редактора:

```text
http://localhost:8080/hrm/#main/open-position-edit-preview?id=<encoded-open-position-id>
```

UUID в URL кодируется штатным механизмом CUBA URL Navigation. Практический безопасный способ получить ссылку — использовать `UrlRouting.getRouteGenerator().getEditorRoute(entity, OpenPositionEditPreview.class)` в отладочном коде или открыть маршрут после назначения экранного разрешения. В рамках текущей задачи отдельная кнопка, menu item, browse action или сервис генерации ссылки не создаются.

Иерархия формы:

```text
HrmMainScreen / прямой route
└── OpenPositionEditPreview
    ├── постоянная sidebar вакансии
    └── рабочая область
        ├── toolbar
        ├── TabSheet с исходными 12 вкладками
        └── footer со штатными actions
```

## Behavior Summary

- открытие preview по маршруту с `id` → CUBA восстанавливает editor entity → preview проверяет загрузку lazy-связи `positionType` и при необходимости догружает её узким view → затем полностью выполняется штатный `OpenPositionEdit.onBeforeShow`;
- открытие legacy `hunttech_OpenPosition.edit` → используются прежние контроллер и XML → preview не участвует в вызове;
- выбор пункта label-навигации → меняется выбранная вкладка `tabSheetOpenPosition` → entity, loader-параметры, validation и save lifecycle не изменяются;
- выбор вкладки TabSheet → выполняется существующий обработчик `OpenPositionEdit` → тяжёлые LOB и коллекции загружаются по прежним правилам;
- изменение `commandCandidate` → legacy-контроллер управляет видимостью `tabPayments` → preview синхронизирует только видимость соответствующего navigation-пункта;
- раскрытие или сворачивание accordion → меняется только presentation-состояние секции → значения полей и DataContext сохраняются;
- сохранение и закрытие → выполняется `windowCommitAndClose` → работают прежние проверки, синхронизация коллекций и автоматические новости;
- отмена → выполняется `windowClose` → изменения не сохраняются;
- дальнейшее изменение legacy-экрана → preview не подменяет его автоматически → перенос новой компоновки выполняется только отдельным согласованным этапом.

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|---|---|
| Controller | `OpenPositionEditPreview` |
| Screen ID | `hunttech_OpenPosition.editPreview` |
| Route | `open-position-edit-preview` |
| XML | `open-position-edit-preview.xml` |
| Базовый controller | `OpenPositionEdit` |
| Edited container | `openPositionDc` |
| Главное назначение | визуальная проверка параллельного варианта |
| Menu registration | отсутствует |
| Browse integration | отсутствует |
| Legacy replacement | отсутствует |

Доступ регулируется штатным screen permission CUBA. Администратор имеет возможность открыть экран после локального deploy; разработчику выдаётся разрешение на `hunttech_OpenPosition.editPreview`. Дополнительная роль и изменение модели безопасности не создаются.

## 2. Связь с моделью данных (Data & Entity Binding)

Preview использует те же компоненты данных, views, loader ID и JPQL, что и `open-position-edit.xml`:

- `openPositionDc` / `openPositionDl` — редактируемая `OpenPosition`, view `openPosition-edit-view`;
- `laborAgreementDc` / `laborAgreementDl`;
- `commentsOpenPositionDc` / `commentsOpenPositionDl`;
- `someFilesesDc` / `someFilesesDl`;
- `openPositionSkillsListsDc` / `openPositionSkillsListsDl`;
- `procAttachmentsDc` / `procAttachmentsDl`;
- `openPositionParentDc` / `openPositionParentDl`;
- `positionTypesDc` / `positionTypesLc`;
- `openPositionNewsDc` / `openPositionNewsLc`;
- `projectNamesDc` / `projectNamesLc`;
- `companyNamesDc` / `companyNamesLc`;
- `companyDepartamentsDc` / `companyDepartamentsLc`;
- `citiesDc` / `citiesDl`;
- `gradeDc` / `gradeDl`;
- `closedVacancyTimer`.

Data View Integrity основного XML не изменяется. Для прямой URL-навигации контроллер preview выполняет отдельную узкую догрузку только `OpenPosition.positionType` и полей `Position.positionRuName`, `positionEnName`, `standartDescription`, `whoIsThisGuy`. Это необходимо до первого getter унаследованного lifecycle, потому что route может восстановить detached `OpenPosition` с неинициализированным EclipseLink value holder.

Догруженная связь устанавливается в текущий редактируемый экземпляр до вызова `super.onBeforeShow(event)`. Отдельный DataContext, альтернативная сущность или изменение значения `positionType` не создаются: в форму переносится то же значение связи из БД.

Все редактируемые компоненты сохраняют исходные `dataContainer`, `property`, `optionsContainer`, `required`, action ID и `invoke`.

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

`OpenPositionEditPreview` наследует `OpenPositionEdit`, чтобы не копировать и не расходиться с бизнес-логикой контроллера. Preview переопределяет только `onBeforeShow(BeforeShowEvent)` как технический guard URL-навигации:

1. проверяет `PersistenceHelper.isLoaded(openPosition, "positionType")`;
2. для существующей detached-сущности догружает `positionType` узким view через `DataManager`;
3. устанавливает загруженную связь в редактируемый экземпляр;
4. вызывает `super.onBeforeShow(event)` без изменения порядка и содержания legacy-инициализации.

После этой подготовки validation, сохранение, загрузка LOB, генераторы таблиц, сервисные вызовы и все бизнес-обработчики выполняются базовым `OpenPositionEdit`.

Остальная собственная Java-логика ограничена:

1. переключением существующих вкладок из label-навигации;
2. назначением active-стиля выбранному пункту;
3. синхронизацией видимости пункта «Оплаты» с существующим условием `commandCandidate == 1`.

Ни `OpenPositionEdit.java`, ни `open-position-edit.xml`, ни browse-экраны, ни `web-menu.xml` не изменяются.

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1. TabSheet

Сохранены порядок и ID вкладок:

1. `tabOpenPosition`;
2. `laborAgreementTab`;
3. `tabPayments`;
4. `tabJobDescription`;
5. `tabFiles`;
6. `tabExercise`;
7. `tabMemoForInterview`;
8. `tabTemplateLetter`;
9. `tabSkills`;
10. `tabOpenPositionNews`;
11. `tabApproval`;
12. `commentsTab`.

`tabPayments` по-прежнему скрыта для одиночной вакансии и показывается legacy-обработчиком для карточки команды. Preview не меняет это условие.

### 4.2. Защита URL lifecycle

При прямом route CUBA может передать editor detached `OpenPosition`, у которого `positionType` представлен неинициализированным lazy value holder без persistence session. Прямой вызов `getPositionType()` в `OpenPositionEdit.ensurePositionLobsLoaded()` в таком состоянии приводит к EclipseLink `ValidationException`.

Preview предотвращает ошибку до входа в базовый lifecycle:

```text
OpenPositionEditPreview.onBeforeShow
├── PersistenceHelper.isLoaded(positionType)
├── при необходимости DataManager.load(OpenPosition + positionType LOB)
├── editedPosition.setPositionType(reloadedPosition.getPositionType())
└── super.onBeforeShow(event)
```

Guard не выполняется для новой сущности и не создаёт дополнительный запрос, если связь уже загружена. Legacy-экран не изменяется.

### 4.3. Progressive loading

Обработчик `OpenPositionEdit.onTabSheetOpenPositionSelectedTabChange()` остаётся источником истины для ленивой загрузки:

- LOB `comment` и `commentEn`;
- `exercise`;
- `memoForInterview`;
- `templateLetter`;
- навыки;
- файлы;
- комментарии;
- трудовые договоры;
- BPM attachments и новости согласно существующему lifecycle.

Navigation-пункты вызывают `TabSheet.setSelectedTab()` и тем самым проходят через тот же listener, а не запускают loaders напрямую.

### 4.4. Accordion

Большие смысловые блоки и таблицы оформлены стандартным `GroupBoxLayout` с `collapsable="true"`. Сворачивание не выгружает данные, не сбрасывает значения и не запускает запросы. Это presentation-операция.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

Сохранены штатные действия и методы:

- `windowCommitAndClose`;
- `windowClose`;
- `subscribePosition`;
- `generateNameFieldButton`;
- `addListCity`;
- `setSalaryFieldButtonInvoke`;
- `addShortDescription`;
- `rescanJobDescription`;
- `addOpenPositionNewsButton`;
- действия create/edit/remove таблиц договоров, файлов и новостей;
- BPM fragment `bpm_ProcActionsFragment`.

Preview не вводит альтернативные save-кнопки, черновик, обход validation или отдельный DataContext.

## 6. Визуальная компоновка элементов (Visual Layout Schema)

```text
openPositionPreviewMainLayout
├── openPositionPreviewSidebar — 312 px
│   ├── projectLogoImage + projectOwnerImage
│   ├── labelOpenPosition + signDraftLabel
│   ├── label-navigation по 12 вкладкам
│   ├── ключевые параметры OpenPosition
│   ├── ownerTextField
│   └── существующая подписка
└── openPositionPreviewWorkspace
    ├── toolbar
    ├── tabSheetOpenPosition
    │   ├── Основное — пять accordion-секций
    │   ├── Трудовые договоры — параметры + accordion-таблица
    │   ├── Оплаты — прежние расчётные блоки
    │   ├── Описание — опыт + описания + короткое описание
    │   ├── Файлы — accordion-таблица
    │   ├── Тестовое задание — accordion + RichTextArea
    │   ├── Памятка интервью — accordion + RichTextArea
    │   ├── Шаблон письма — accordion + RichTextArea
    │   ├── Навыки — accordion + TreeDataGrid
    │   ├── Новости — accordion + DataGrid
    │   ├── Согласование — accordion + BPM fragment
    │   └── Комментарии — accordion + ScrollBox
    └── editActions
```

Используются существующие стили `edit-*`, `label-navigation`, `label-nav-*`, `light`, `framed` и `compact-tabbar`. Файлы тем и общие SCSS не изменяются. Префикс `open-position-preview-*` служит локальным namespace и не добавляет глобальные селекторы.

### 6.1. Адаптивность

- sidebar имеет контрактную ширину 312 px;
- рабочая область получает оставшуюся ширину через `expand`;
- прокрутка выполняется внутри вкладок;
- таблицы и RichTextArea имеют относительную либо явную высоту;
- поля не переносятся между бизнес-вкладками;
- на небольшом viewport пользователь сохраняет доступ к содержимому через внутреннюю вертикальную прокрутку.

## 7. Ограничения этапа

В текущем PR запрещены и отсутствуют:

- изменение legacy `OpenPositionEdit`;
- изменение вызовов legacy editor;
- menu item или browse action preview;
- изменение `OpenPosition`, БД или Liquibase;
- изменение `views.xml`;
- изменение сервисов и JPQL;
- изменение общих или тематических SCSS;
- AI-анализ, генерация текста или поиск кандидатов;
- production deploy;
- merge без прямой команды Алексея.

## 8. Проверки Hermes

Hermes проверяет точный HEAD PR:

1. XML parsing и компиляцию web-модуля;
2. `OpenPositionEditPreviewLayoutTest`;
3. `OpenPositionEditPreviewRouteGuardTest`;
4. `ScreenViewIntegrityTest` — 8/8 PASS;
5. сборку SCSS всех тем без изменения их файлов;
6. `clean assemble` — BUILD SUCCESSFUL;
7. local deploy и HTTP `/hrm/` = 200;
8. открытие preview по route под администратором;
9. отсутствие `ValidationException` на lazy `positionType`;
10. все 12 вкладок, accordion-секции и label-навигацию;
11. сохранение и отмену;
12. отсутствие unfetched/detached/RPC/runtime ошибок;
13. неизменность legacy editor и его вызовов.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-01 | Добавлена preview-специфичная защита URL lifecycle: lazy-связь `positionType` догружается узким view до вызова унаследованного `OpenPositionEdit.onBeforeShow`; legacy-экран не изменён. |
| 2026-08-01 | Создан изолированный preview новой двухпанельной компоновки OpenPositionEdit без замены legacy-экрана и изменения бизнес-логики. |
