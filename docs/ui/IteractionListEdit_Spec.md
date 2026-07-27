# IteractionListEdit — спецификация Edit-экрана HRM HuntTech

> Screen ID: `hunttech_IteractionList.edit`
> Entity: `IteractionList`
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`
> Business-controller: `IteractionListEdit`
> Presentation-controller: `IteractionListEditAccordionNavigation`
> Платформа: CUBA Platform 7.3-SNAPSHOT

## 1. Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` создаёт или изменяет один факт взаимодействия рекрутёра с кандидатом в контексте вакансии. Запись связывает участника подбора, вакансию, тип и результат контакта, дополнительные данные конкретного типа взаимодействия, оценку, канал коммуникации и комментарий.

Экран влияет на последующие процессы подбора: цепочку взаимодействий, статус кандидата, подписки, уведомления, автоматические новости вакансии, сообщения и сценарии начала или завершения работы сотрудника. Поэтому визуальный рефакторинг не вправе менять бизнес lifecycle, entity, сервисы, loaders, JPQL, bindings, validators и стандартные CUBA actions.

Пять быстрых действий сокращают повторяющийся ввод. Они отражают наиболее частые типы взаимодействий текущего рекрутёра за последний календарный месяц и передают в поле типа точный объект `Iteraction`.

## 2. UI Context & Navigation

Экран сохраняет legacy screen ID `hunttech_IteractionList.edit` и все существующие точки открытия:

- создание взаимодействия из browse;
- редактирование существующей записи;
- открытие из карточки кандидата;
- сценарий копирования предыдущего взаимодействия;
- связанные recruiter/vacancy workflows.

Композиция состоит из постоянной sidebar и правой рабочей области:

```text
IteractionListEdit
├── edit-sidebar
│   ├── candidate/project visual
│   ├── identity
│   ├── label-navigation
│   ├── service context
│   └── vacancy context
└── edit-workspace
    ├── edit-toolbar
    ├── mostPopularQuickActions
    ├── edit-workspace-scroll
    │   ├── participantsAccordion
    │   ├── interactionAccordion
    │   ├── resultAccordion
    │   └── commentAccordion
    └── edit-footer-actions
```

`label-navigation` содержит только четыре реальных рабочих раздела. Блок пяти быстрых действий всегда видим над scroll-area и не является аккордеоном.

## 3. Behavior Summary

- открытие нового взаимодействия → базовый controller задаёт номер, дату и текущего рекрутёра → первый аккордеон открыт;
- открытие существующего взаимодействия → прежний loader и view загружают entity → presentation-слой не меняет данные;
- статистика текущего пользователя найдена → создаются до пяти активных быстрых кнопок → каждая хранит точный `Iteraction`;
- статистика содержит менее пяти типов → недостающие позиции показывают `Нет данных` и `enabled=false` → click listener отсутствует;
- клик активной быстрой кнопки → `iteractionTypeField.setValue(interaction)` и `focus()` → штатный value-change handler управляет dynamic fields;
- клик пункта sidebar → раскрывается связанный GroupBox, остальные сворачиваются → фокус переводится в первое рабочее поле;
- ручное раскрытие GroupBox → active-state sidebar синхронизируется → рекурсивные expanded events блокируются флагом;
- выбор кандидата или вакансии → выполняются прежние handlers, фильтры, предупреждения и контекстные обновления;
- выбор типа → сохраняются прежние captions, visible/required и действие `buttonCallAction`;
- `Подписаться` → вызывается `onButtonSubscribeClick`;
- `Сохранить и закрыть` → выполняется `windowCommitAndClose`;
- `Отмена` → выполняется `windowClose`.

## 4. Аудит функционального контракта

| Компонент / поле | Бизнес-смысл | Binding / источник | Обработчик | Loader / view | Новое расположение |
|---|---|---|---|---|---|
| `iteractionListDc` | редактируемый факт взаимодействия | `IteractionList` | lifecycle `StandardEditor` | `iteractionListDl`, `iteractionList-edit-view` | корневой data container |
| `candidateImage` | фотография кандидата | `candidate.fileImageFace` | существующее обновление кандидата; `OvaFallbackImage` | `iteractionList-edit-view` и suggestion view | sidebar, главный образ 112 px |
| `projectLogoImage` | логотип проекта | `vacancy.projectName.projectLogo` | существующий vacancy handler | `iteractionList-edit-view` | sidebar, вторичный образ 80 px |
| `iteractionCandidateNameLabel` | ФИО кандидата | `candidate.fullName` | container binding | `iteractionList-edit-view` | sidebar identity |
| `iteractionVacancyNameLabel` | название вакансии | `vacancy.vacansyName` | container binding | `iteractionList-edit-view` | sidebar identity |
| `numberIteractionField` | номер взаимодействия | `numberIteraction` | `setIteractionNumber()` | entity view | sidebar service context |
| `dateIteractionField` | дата взаимодействия | `dateIteraction` | `setCurrentDate()`, editability lifecycle | entity view | sidebar service context |
| `companyLabel` | компания / подразделение | runtime value | `vacancyFieldValueChange()` | vacancy picker view | sidebar vacancy context |
| `projectLabel` | проект | runtime value | `vacancyFieldValueChange()` | vacancy picker view | sidebar vacancy context |
| `statusOfVacansyLabel` | состояние вакансии | runtime value | `setStatusOfVacancyLabel()` | vacancy picker view | sidebar vacancy context |
| `alternativeVacancyLinkButton` | предупреждение об альтернативных вакансиях | runtime description | vacancy handler | query базового controller | sidebar warning |
| `currentPriorityLabel` / `trafficLighterImage` | приоритет вакансии | runtime value/resource | `setPriorityLabel()` | vacancy picker view | sidebar vacancy context |
| `outstaffingCostHBox` | стоимость аутстаффинга | `vacancy.outstaffingCost` | runtime visibility | `iteractionList-edit-view` | sidebar vacancy context |
| `ratingLabel` / `ratingImage` | текущая оценка | runtime presentation | rating handler | entity view | sidebar context |
| `candidateField` | кандидат | `candidate` | `onCandidateFieldValueChange`, copy/check | suggestion query, `jobCandidate-iteraction-list-suggestion-view` | «Кандидат и вакансия», колонка 1 |
| `vacancyFiels` | вакансия | `vacancy` | `onVacancyFielsValueChange` | `openPositionsDl`, `openPosition-iteraction-list-picker-view` | «Кандидат и вакансия», колонка 2 |
| `onlyMySubscribeCheckBox` | фильтр вакансий по подписке | runtime | listener базового controller | `openPositionsDl` | отдельная строка первого аккордеона |
| `iteractionTypeField` | тип взаимодействия | `iteractionType` | `onIteractionTypeFieldValueChange` | `iteractionTypesLc`, `iteraction-list-type-view` | «Тип взаимодействия и действие» |
| `buttonCallAction` | динамическое действие типа | `invoke="callActionEntity"` | существующий invoke | без нового loader | второй аккордеон |
| `addString` | дополнительное текстовое значение | `addString` | `changeField()` | entity view | второй аккордеон |
| `addDate` | дополнительная дата | `addDate` | `changeField()` | entity view | второй аккордеон |
| `addInteger` | дополнительное числовое значение | `addInteger` | `changeField()` | entity view | второй аккордеон |
| `ratingField` | результат / оценка | `rating` | rating option provider/listener | entity view | «Результат», колонка 1 |
| `recrutierField` | рекрутёр | `recrutier` | lifecycle текущего пользователя | `usersDc` | «Результат», колонка 2 |
| `communicationMethodField` | канал коммуникации | `communicationMethod` | container binding | entity view | полноширинная строка результата |
| `commentField` | комментарий | `comment` | lazy reload и required handler | узкий runtime view `comment` | «Комментарий» |
| `subscribeButton` | подписка пользователя | invoke | `onButtonSubscribeClick` | прежние сервисы | footer |
| commit button | сохранение | CUBA action | `windowCommitAndClose` | `DataContext` / editor lifecycle | footer |
| close button | отмена | CUBA action | `windowClose` | без commit | footer |
| `mostPopularHbox` | пять быстрых позиций | `InteractionService` + placeholders | exact-object click listener | сервисный запрос | постоянная карточка под toolbar |

## 5. Исторический контракт пяти быстрых взаимодействий

### 5.1. Источник и ранжирование

Единственный источник:

```java
InteractionService.getMostPolularIteraction(User user, int maxCount)
```

Неизменяемый алгоритм реализации 2024 года:

```text
текущий рекрутёр из UserSession
→ период от момента открытия до одного календарного месяца назад
→ IteractionList с непустым iteractionType
→ group by iteractionType
→ count
→ order by count DESC
→ первые пять
```

UI-controller не содержит локальную агрегацию, альтернативный JPQL или caption parsing.

### 5.2. Точный объект и placeholders

Базовый controller создаёт активную кнопку и замыкает в listener точный объект:

```java
iteractionTypeField.setValue(interaction);
iteractionTypeField.focus();
```

Presentation-controller не пересоздаёт активные listeners. Он:

1. сохраняет фактически созданные кнопки;
2. назначает одинаковый presentation stylename и expand ratio;
3. добавляет tooltip с полным caption;
4. дополняет количество до пяти;
5. создаёт placeholders `Нет данных`, `enabled=false`, без click listener.

## 6. Новая архитектура XML

### 6.1. Sidebar

Нормативная ширина:

- desktop: `270px`;
- viewport до `1366px`: `250px`.

Sidebar имеет собственный вертикальный scroll и не создаёт горизонтальную прокрутку. Порядок:

```text
фото и логотип → ФИО и вакансия → label-navigation
→ номер и дата → контекст вакансии → spacer
```

Фотография кандидата — `112px`, логотип проекта — `80px`, `scaleMode="SCALE_DOWN"`, fallback `icons/no-company.png`.

### 6.2. Workspace

Workspace занимает остаток ширины и содержит:

1. toolbar;
2. постоянную карточку быстрых действий;
3. единственную вертикальную scroll-area;
4. footer.

Промежуточный `TabSheet` удалён: экран редактирует один бизнес-объект и не требует вкладочного уровня.

### 6.3. Аккордеоны

| Раздел | Поля | Focus target | Default |
|---|---|---|---|
| Кандидат и вакансия | `candidateField`, `vacancyFiels`, `onlyMySubscribeCheckBox` | `candidateField` | открыт |
| Тип взаимодействия и действие | `iteractionTypeField`, `buttonCallAction`, `addString`, `addDate`, `addInteger` | `iteractionTypeField` | закрыт |
| Результат взаимодействия | `ratingField`, `recrutierField`, `communicationMethodField` | `ratingField` | закрыт |
| Комментарий | `commentField` | `commentField` | закрыт |

Все GroupBox имеют `height="AUTO"`. `popularAccordion` остаётся только невидимым compatibility-компонентом для legacy field injection базового controller; он не является рабочим разделом, не отображается и не входит в navigation.

## 7. Presentation-controller

`IteractionListEditAccordionNavigation`:

- переопределяет только legacy presentation Init-handler;
- не меняет business lifecycle базового controller;
- создаёт четыре keyboard-доступные borderless-кнопки;
- сохраняет `label-nav-item` и меняет только `label-nav-item-active`;
- синхронизирует клик и ручное раскрытие GroupBox;
- защищает listeners от рекурсивного входа;
- не использует `DataManager`, loaders, `InteractionService`, `getEditedEntity()`, `setValue()` или commit;
- нормализует пять визуальных позиций после базового `BeforeShow`.

## 8. Data View Integrity

Контракт view не изменён. Обязательная проверка:

- `iteractionList-edit-view` содержит все XML bindings и getters controller;
- `jobCandidate-iteraction-list-suggestion-view` содержит поля suggestion и изображение кандидата;
- `openPosition-iteraction-list-picker-view` содержит vacancy context, project/logo, priority, status и outstaffing;
- `iteraction-list-type-view` содержит поля, читаемые dynamic handler;
- detached entity getter не читается до проверки loaded-state;
- lazy comment загружается узким view и переносится в редактируемую entity без N+1.

Проверяемые сценарии: новый объект, существующий объект, копирование, отсутствующие фото/логотип/FileStorage, сохранение без открытия каждого аккордеона и повторное открытие.

## 9. SCSS

Единственный исходный shared partial:

```text
modules/web/themes/common/edit-screen-shared-styles.scss
```

Семь theme-local путей являются symbolic links на общий partial. Это сохраняет импорт, совместимый с изолированной компиляцией тем CUBA 7.3, но исключает семь расходящихся копий правил.

Локальные правила остаются ограничены:

```scss
.iteraction-list-editor {
    // iteraction-list-* only
}
```

Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet`, `.v-panel` не изменяются без semantic/root scope.

## 10. Сохранённые контракты

Не изменены:

- entity `IteractionList`;
- БД и Liquibase;
- `InteractionService`;
- месячный период, текущий пользователь, group/count/order;
- screen ID;
- data containers, loaders, JPQL и views;
- component ID рабочих полей;
- property bindings;
- required и validators;
- picker actions;
- `invoke="callActionEntity"`;
- `invoke="onButtonSubscribeClick"`;
- `windowCommitAndClose`;
- `windowClose`;
- подписки, уведомления и save lifecycle.

## 11. Обязательная проверка Hermes

Проверять только точный HEAD PR:

```bash
git diff --check
./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew test --tests '*IteractionList*Test' --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

После сборки:

- clean local deploy;
- `http://localhost:8080/hrm/` = HTTP 200;
- Tomcat critical errors = NONE;
- functional smoke всех полей, кнопок, save/cancel;
- quick actions: 0, 1–4, 5+ типов;
- visual smoke семи тем, desktop и viewport `<=1366px`;
- horizontal scroll отсутствует;
- P1=0, P2=0.

## 12. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Экран перепроектирован с нуля: двухпанельный XML, четыре рабочих аккордеона, постоянные пять быстрых позиций, синхронная label-навигация и единый shared SCSS source. |
| 2026-07-27 | Форма приведена к общему UI-контракту Edit-экранов. |
