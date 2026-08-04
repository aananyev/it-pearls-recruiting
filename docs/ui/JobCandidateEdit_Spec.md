# JobCandidateEdit — спецификация экрана HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

`JobCandidateEdit` — основная рабочая карточка кандидата в HRM HuntTech. Экран объединяет персональные и профессиональные сведения, контакты, позиции и вакансии, взаимодействия, резюме и файлы, социальные сети, комментарии и историю записи.

Визуальная компоновка должна позволять рекрутеру постоянно видеть краткий профиль кандидата и одновременно работать с детальными данными, не переходя на отдельные экраны и не теряя контекст подбора.

### 2. Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается:

- из `JobCandidateBrowse` при создании или редактировании кандидата;
- из экранов подбора кандидатов;
- из связанных рекрутинговых сценариев и lookup-компонентов.

Основная навигация выполняется через существующий `tabSheetSocialNetworks`. Состав вкладок сохранён:

- `tabMain` — основные данные;
- `tabContactInfo` — контакты;
- `tabPositions` — позиции и вакансии;
- `tabIteraction` — взаимодействия;
- `tabResume` — резюме и файлы;
- `commentsTab` — комментарии;
- `tabHistory` — история.

Кнопка `openPositionMasterBrowseButton` открывает HR-Мастер для текущего кандидата. Кнопка `moreActionsPopUpButton` (справа сверху) содержит существующие действия блокировки и подписки. Верхняя панель `jobCandidateTopBar` слева показывает заголовок и описание формы (`jobCandidateToolbarTitleBox` со stylename `edit-toolbar-title`/`edit-toolbar-description`).

Левая панель содержит видимую `label-navigation` с пунктами по вкладкам. Клик по пункту
вызывает существующий `invoke`-метод и переключает только текущую вкладку
`tabSheetSocialNetworks`; loaders, validation, bindings и сохранение не меняются. Активный
пункт получает общий класс `label-nav-item-active`.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие кандидата → загружается основной view → слева показывается профиль, справа рабочие вкладки.
- Первое открытие тяжёлой вкладки → устанавливаются обязательные параметры loaders → данные загружаются один раз.
- Изменение поля → данные остаются в штатном `DataContext` → перед сохранением выполняется существующая валидация.
- «Сохранить и закрыть» → выполняется `windowCommitAndClose` → кандидат сохраняется и экран закрывается.
- «Отмена» → выполняется `windowClose` → применяется стандартный сценарий закрытия CUBA.
- «Еще» → открывается существующее popup-меню → выполняются прежние handlers.

---

## 1. Технический контракт экрана

| Параметр | Значение |
|---|---|
| Screen ID | `hunttech_JobCandidate.edit` |
| Controller | `com.company.hunttech.web.screens.jobcandidate.JobCandidateEdit` |
| XML descriptor | `job-candidate-edit.xml` |
| Edited entity | `JobCandidate` |
| Edited container | `jobCandidateDc` |
| Платформа | CUBA Platform 7.3 |
| Корневой style name | `job-candidate-editor` |

Визуальная доработка не меняет сущности, component ID, data bindings, actions, invoke, loaders, views и JPQL.

Обязательные compatibility-компоненты контроллера:

| ID | XML-тип | Назначение |
|---|---|---|
| `lastProjects` | `groupBox` | сохранение существующего `@Inject`-контракта |
| `dictionatysTavlesHBox` | `grid` | сохранение существующего `@Inject`-контракта |
| `candidateNavigation` | `vbox` | сохранение существующих invoke-методов навигации |

---

## 2. Модель данных и загрузка

| Контейнер | Назначение |
|---|---|
| `jobCandidateDc` | редактируемый кандидат |
| `jobCandidateCandidateCvsDc` | резюме кандидата |
| `jobCandidateIteractionDc` | взаимодействия кандидата |
| `jobCandidateSocialNetworksDc` | социальные сети |
| `lastProjectDc` | история рассмотрения по вакансиям |
| `suggestOpenPositionDc` | подходящие вакансии |

Правила:

- ленивое открытие тяжёлых вкладок сохраняется;
- обязательные параметры loaders не изменяются;
- визуальный слой не выполняет дополнительные запросы;
- таблицы продолжают использовать прежние dataContainer и actions.

---

## 3. Визуальная компоновка

```text
jobCandidateMainLayout
├── jobCandidateSidebar
│   ├── фотография
│   ├── ФИО
│   ├── label-navigation по вкладкам
│   ├── рейтинг и процент заполнения
│   ├── город / компания / резюме
│   ├── email / телефон / Telegram
│   ├── растягиваемое свободное пространство
│   └── быстрые действия: резюме, взаимодействие, HR-Мастер
└── jobCandidateWorkspace
    ├── верхняя панель: заголовок/описание формы слева, «Еще» справа
    ├── горизонтальные вкладки
    ├── видимые accordion-заголовки разделов
    └── нижняя панель: «Сохранить и закрыть», «Отмена»
```

### Левая панель

- ширина — 312 px (>1366px); Vaadin slot-обёртка sidebar имеет тот же фиксированный размер,
  чтобы workspace не начинался под левой панелью; адаптивные тиры повторяют эталон
  `IteractionListEdit`: 296 px при ≤1366px и 284 px при ≤1100px (компонент и slot — одной ширины);
- `job-candidate-sidebar` использует `box-sizing: border-box` — ширина 312 px включает
  внутренние отступы (24/20/18 px), поэтому левый блок не выходит за границы экрана;
- sidebar-карточки (`job-candidate-sidebar-card`) также `box-sizing: border-box` и имеют
  видимую рамку `rgba(255,255,255,.35)` на тёмном фоне;
- тёмный фон задаётся самому `jobCandidateSidebar` и его Vaadin slot-обёртке;
- фотография отображается единым `OvaFallbackImage` размером 176×176 и не искажается;
- при отсутствии фотографии или бинарного файла показывается `icons/no-programmer.jpeg`;
- ФИО в формате `Фамилия Имя` и должность видны непосредственно под фотографией;
- ФИО и должность обновляются сразу при изменении полей и не зависят от lazy-создания вкладки «Основное»;
- `candidateNavigation` расположен сразу после профильного блока, до карточек детализации, по общему Edit-контракту HRM HuntTech;
- ФИО центрировано и использует типографику `candidateRatingLabel`, а остальные sidebar labels уменьшены на один пункт;
- процент заполнения рассчитывается по 15 свойствам `jobCandidateDc` без открытия lazy-вкладок и дополнительных запросов;
- текст карточек имеет размер 15 px и переносится по словам;
- кнопки `Загрузить` и `Очистить` под фотографией имеют одинаковую ширину 96px,
  высоту 36px и общий sidebar button-style;
- быстрые действия прижаты к нижней границе;
- заблокированный кандидат сохраняет профильное позиционирование ФИО, меняется только цвет.
- `candidateNavigation` использует общие классы `label-navigation`, `label-nav-title`,
  `label-nav-item`, `label-nav-item-active`; локальный `job-candidate-nav-item` оставлен
  только для цветовой адаптации sidebar;
- пункты `label-nav-item` повторяют эталон `IteractionListEdit`
  (`iteraction-list-visual-alignment.scss:112–161`): `min-height: 27px`, `height: auto`,
  `padding: 3px 10px`, `line-height: 20px`, шрифт 13px/600; активный пункт — жёлтый
  `#ffb11b` на `rgba(255,177,27,.12)` с жёлтой левой границей, hover — белый текст на
  `rgba(255,255,255,.08)`; вертикальное центрирование текста и маркера — flex'ом;
- внутренние sidebar-блоки и Vaadin slots имеют `min-width: 0`/`max-width: 100%`, поэтому
  карточки, кнопки и длинные подписи не выходят за границы панели;
- горизонтальные вкладки не обрезают captions: `tabcontainer` получает локальную
  горизонтальную прокрутку, а подписи остаются `nowrap` без `ellipsis`, чтобы названия
  разделов были видны полностью и не расширяли рабочую область за границу формы;

### Контракт профильного блока

`fullNameField` и `personPositionLabel` являются presentation labels без прямой XML-привязки.
Контроллер заполняет их после показа экрана и синхронизирует по
`InstanceContainer.ItemChangeEvent`/`ItemPropertyChangeEvent` контейнера `jobCandidateDc`.
Поэтому sidebar получает данные даже до создания полей внутри lazy-вкладки.

ФИО формируется из `secondName` и `firstName` через общий null-safe formatter. Результат
одновременно записывается в `JobCandidate.fullName` и выводится под фотографией. Должность
берётся из `personPosition.positionRuName`; отсутствие должности даёт пустую подпись.

При изменении состояния блокировки контроллер сначала восстанавливает постоянный класс
`job-candidate-profile-name edit-sidebar-title`, затем добавляет `h2` либо `h2-red`.
Такой порядок обязателен: `setStyleName()` заменяет все текущие CSS-классы компонента.

SCSS-контракт повторён для `halo`, `havana`, `helium`, `hover`, `hunttech-modern`,
`hunttech-modern-light` и `hunttech-modern-dark`. Должность должна оставаться `display: block`.
Подробное описание lifecycle и пользовательские сценарии приведены в
[`docs/screens/job-candidate/JobCandidateEdit_Spec.md`](../screens/job-candidate/JobCandidateEdit_Spec.md)
и [`job-candidate-edit.md`](../screens/job-candidate/job-candidate-edit.md).

### Правая область

- workspace занимает всю оставшуюся ширину;
- `jobCandidateWorkspace` использует общий `edit-workspace`;
- `jobCandidateTopBar` использует общий `edit-toolbar`, а `tabSheetSocialNetworks` — `edit-tabs`;
- слева в toolbar выводится заголовок и описание формы: `jobCandidateToolbarTitleBox`
  (`msg://editorCaption` + `mainMsg://msgCandidate`, stylename
  `edit-toolbar-title`/`edit-toolbar-description`); `expand` панели переведён на этот блок;
- «Еще» находится справа сверху;
- основные действия находятся справа снизу;
- вкладки сохраняют прежние ID и порядок, но используют компактные подписи 14 px и меньшие боковые отступы, чтобы строка вкладок помещалась в стандартный диалог 1200 px;
- активная вкладка выделяется theme-aware `$v-selection-color` с нижней границей 3 px;
- нижняя панель действий занимает 100% ширины workspace; Vaadin slots внутри панели
  локально переведены на flex, поэтому «Сохранить и закрыть» и «Отмена» сгруппированы
  внизу справа, а не распределены по ширине footer;
- внутренние контейнеры вкладок «Резюме» и «Комментарии» имеют width 100%, чтобы таблицы не сжимались до intrinsic width;
- колонка действий соцсетей фиксирована на 220 px, а таблица занимает оставшуюся ширину;
- табличные карточки вкладки «Позиции и вакансии» получают `min-width: 0` и локальную горизонтальную прокрутку таблицы, если набор колонок шире доступной области;
- фон и границы одинаково определены для Halo, Hover, Havana, Helium и modern-тем.

### Accordion-заголовки

XML содержит контейнеры `job-candidate-accordion-header` и `job-candidate-accordion-content`. Заголовок раздела отображается над содержимым как flat-section-header эталона `IteractionListEdit` (фон `#eef3f8`, нижняя граница, скругление `8px 8px 0 0`). Декоративный маркер `▼` (`:before`) удалён: секции не сворачиваются, маркер вводил в заблуждение. Заголовок и содержимое образуют единую карточку.

Accordion-слой не удаляет и не подменяет штатную навигацию `TabSheet`. Поведение полей, таблиц, loaders и действий не меняется.

### Вкладка «Основное»

- `personalDataBlock` и `professionalDataBlock` размещаются один над другим (вертикальная раскладка): родительский `jobCandidateMainSectionContent` — `vbox`, каждая карточка занимает 100% доступной ширины;
- мёртвый класс `job-candidate-half-card` (горизонтальная пара) удалён из формы целиком
  (P2-10 дизайн-ревью 2026-08-03); карточки «Персональные данные»/«Профессиональные
  данные» стоят одна над другой на 100% ширины (вертикальный стек);
- промежуток между блоками — 16 px;
- внутренний `GridLayout` принудительно растягивается на 100%;
- колонка подписей имеет ширину 112 px; при ширине окна до 1366 px она компактно уменьшается до 96 px. Правило применяется к фактическим Vaadin `HBox` slots внутри `GridLayout`, поэтому длинные подписи не распадаются на слоги;
- slot-компоненты `GridLayout` растягиваются единым локальным правилом без отдельных селекторов второй колонки;
- поля `firstNameField`, `middleNameField`, `secondNameField`, `personPositionField` и `currentCompanyField` занимают всю доступную ширину;
- поля ввода получили общий `edit-form-control` совместно с существующими локальными стилями;
- шрифт `SuggestionField` ФИО установлен 15 px — такой же, как у `jobCityCandidateField` и остальных полей;
- высота полей — 38 px (включая `.c-suggestionfield` ФИО: добавлен в правило высоты
  `.job-candidate-form-grid`/`.job-candidate-form-row`, селектор-потомок shared
  `.edit-form-control .c-suggestionfield` на самом input не срабатывал);
- ширина строк ввода строго одинакова: колонка подписей в строках `job-candidate-form-grid`
  фиксирована на 112px (96px при ≤1366px), поле занимает `calc(100% - 112px)` —
  «Имя», «Отчество», «Фамилия» (и «Должность», «Компания») одной ширины при любой длине
  подписи; мёртвый stylename `job-candidate-name-row` из XML удалён — геометрию строк
  дают общие правила `.job-candidate-form-grid`;
- component ID, properties, actions, queries и search executors не изменяются.

### Вкладка «Контакты»

- основные и дополнительные контакты — две карточки 50/50 через `job-candidate-card-row`
  (мёртвые `job-candidate-half-card`/`job-candidate-contact-card` удалены);
- подписи строк контактов фиксированы на 100 px (`.job-candidate-form-row > .v-slot:first-child`,
  ранее 150px, в media — 128px); у XML-подписей (`<label … stylename="small">`) возвращён
  явный атрибут `width="100px"` — он гарантирует одинаковую длину строк ввода и единый отступ
  поля в обеих карточках независимо от SCSS-каскада и длин текста подписей;
- поля занимают оставшееся пространство;
- секция «Социальные сети» переведена с фиксированной высоты `560px` на `height="AUTO"`;
  таблица `socialNetworkTable` получила явный `min-height: 320px`, чтобы не схлопываться
  при малом числе строк; колонка `networkName` — caption `msg://msgNetworkName`;
- `radioButtonGroup` сохраняет прежнюю бизнес-логику и привязку `priorityContact`;
- пара «Способ связи» (label + `radioButtonGroup`) выровнена по вертикали: обоим элементам
  добавлен `align="MIDDLE_LEFT"` — подпись центрирована относительно радиокнопок.

---

## 4. Actions и неизменяемые контракты

| Компонент | Контракт |
|---|---|
| `windowCommitAndCloseButton` | action `windowCommitAndClose` |
| кнопка отмены | action `windowClose` |
| `moreActionsPopUpButton` | прежний popup и handlers |
| `fileImageFaceUpload` | прежняя загрузка фотографии |
| поля ФИО | прежние properties, search executors и listeners |
| `currentCompanyField` | прежние lookup/open/create действия |
| `jobCandidateIteractionListTable` | прежние actions, columns и handlers |
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers |
| `socialNetworkTable` | прежний editor и generators |
| комментарии | прежние поле ввода, отправка и ответы |

---

## 5. Стили и поддержка тем

Общий mixin:

```scss
@mixin job-candidate-editor-theme
```

Все правила ограничены:

```scss
.job-candidate-editor
```

Одинаковый файл `job-candidate-editor.scss` (7 идентичных копий) используется во всех
поддерживаемых темах:

- Halo, Hover, Havana, Helium;
- hunttech-modern, hunttech-modern-light, hunttech-modern-dark.

Порядок подключения соответствует контракту 6.4
`HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`: `@import`/`@include`
`job-candidate-editor.scss` стоит ПОСЛЕ `edit-screen-shared-styles`. В halo/havana/helium/hover
подключение перенесено из `*-ext.scss` в `styles.scss`; в modern-темах строка перемещена
после shared. Ранее слой формы подключался до shared, и shared-геометрия label-навигации
(24px) не применялась к форме.

Глобальные `.v-table`, `.v-label`, `.v-button` и `.v-tabsheet` вне `.job-candidate-editor` не изменяются.

Для рейтинга используются CSS Unicode escapes (`\2605`, `\2606`), чтобы звёзды не повреждались при сборке SCSS.

### Оптимизация локального SCSS

По результатам завершённого анализа клиентского рендеринга сохранён безопасный рефакторинг локального визуального слоя:

- универсальное правило для всех потомков `.job-candidate-editor` заменено перечнем локальных layout-классов;
- substring selector рейтинга заменён явным списком rating-классов;
- правила sidebar сведены к `job-candidate-sidebar-card`;
- для `job-candidate-form-grid` удалены селекторы `td:nth-child(2)` и глубокие цепочки до внутренних полей;
- slot-компоненты основной формы растягиваются единым локальным правилом;
- строка вкладок уплотнена локально внутри `.job-candidate-editor`, без изменения глобального `edit-tabs`;
- табличные и кнопочные контейнеры получают `min-width: 0`/фиксированную ширину там, где длинные captions могли вытеснять соседние элементы;
- повторяющиеся комбинации `width/min-width/max-width` сокращены там, где геометрия уже задаётся XML или flex layout;
- критические `!important` сохранены только для inline-размеров Vaadin и конфликтующих правил тем.

Временные performance-пробы, JFR-сборщики и performance-тесты после завершения анализа удалены. Штатный lifecycle экрана не зависит от диагностического system property.

---

## 6. Контроль качества и развертывание

Обязательные команды:

```bash
git diff --check
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.JobCandidateEditLayoutContractTest' --no-daemon --stacktrace
```

Ручная проверка:

- HTTP 200 для `/hrm`;
- открытие существующего и нового кандидата;
- сохранение и отмена;
- меню «Еще»;
- фотография и HR-Мастер;
- accordion-заголовки на каждой вкладке;
- одинаковый шрифт полей ФИО, города, должности и компании;
- ширина полей на вкладках «Основное» и «Контакты»;
- рейтинг без повреждённых символов;
- все таблицы, actions и ленивые loaders;
- темы Halo, Hover, Havana и Helium.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-03 | **Фиксы приёмки (браузер-верификация, theme halo):** 1) навигация 46px→27px — базовая halo-кнопка `.v-button:before` вставляла фантомный inline-block (второй line box); скрыт `:before { display:none !important }` у `label-nav-item`/`job-candidate-nav-item`/`v-button-label-nav-item` (7 тем); 2) поля ФИО выровнены — у required-полей (Имя/Фамилия) CUBA добавлял пустой caption-слот 20px справа (`v-caption-on-right`); обёртка переведена в `display:block`, слот скрыт, поле растянуто на слот (все три поля одной ширины); 3) возвращена таблица «Социальные сети» — после `height="AUTO"` секции dataGrid схлопывался до 0px, т.к. `#socialNetworkTable` не рендерит id на корневом div; высота задана через `.v-slot-job-candidate-table`/`.c-data-grid-composition` + `.v-grid` (`min-height: 320px !important`). Presentation-only, bindings/логика не менялись. |
| 2026-08-04 | Вкладка «Контакты»: подписям строк ввода в карточках «Основные контакты» (Email, Телефон, Мобильный, Telegram) и «Дополнительные контакты» (WhatsApp, Viber, Skype) возвращён явный `width="100px"` в XML — все 7 строк ввода одной длины и с одинаковым отступом поля независимо от SCSS-каскада. Presentation-only, bindings/логика не менялись. |
| 2026-08-04 | **View integrity fix (IllegalStateException `projectLogo`)**: inline-override `projectName view="_local"` в `jobCandidateDc` не загружал FK `projectLogo` (`@ManyToOne` FileDescriptor) и `projectDepartment→companyName` — генераторы колонок логотипа (`jobCandidateCandidateCvTableProjectLogoColumnColumnGenerator`, `jobCandidateIteractionListTableProjectLogoColumnColumnGenerator`) и `openPositionDescription()` падали на detached Project. Исправлено: `projectName view="_minimal"` + явные nested-поля (`projectName`, `projectDescription`, `projectLogo view="_local"`, `projectDepartment view="companyDepartament-picker-view"` → `companyName` → `workingConditions`/`companyDescription`) для `candidateCv.toVacancy` и `iteractionList.vacancy`; `suggestOpenPositionDc.projectName` — `_minimal` + `projectOwner view="person-owner-view"` (descriptionProvider). View расширен только в экранном XML, бизнес-логика не менялась. |
| 2026-08-04 | Вкладка «Контакты»: пара «Способ связи» (label + `radioButtonGroup`) выровнена по вертикали — обоим элементам добавлен `align="MIDDLE_LEFT"` (подпись стоит на одной линии с радиокнопками). Изменение presentation-only, bindings/логика не менялись. |
| 2026-08-03 | **Дизайн-ревью компоновки JobCandidateEdit (22 файла, presentation-only)** по заданию `.team/JobCandidateEdit/design-notes.md` (дефекты P1-1…P3-14); Java-контроллер не менялся. XML: toolbar получил заголовок/описание (`jobCandidateToolbarTitleBox`, `edit-toolbar-title`/`-description`, `expand` переведён на него); все русские подписи переведены на `msg://`/`mainMsg://` (+24 ключа в `messages*.properties`); секция «Социальные сети» — `height="AUTO"` + `min-height: 320px` у `socialNetworkTable`; удалены мёртвые stylename (`job-candidate-half-card`, `-contact-card`, `-positions-layout`, `-table-comments`, `-info-grid`, `-sidebar-grid`, `-name-row`); добавлены captions колонок `networkName`/`vacancy`/`iteractionType`/`recrutier`; панель комментариев переведена с `well`/`large` на `edit-card`/`edit-form-control`. SCSS ×7 тем: label-навигация по эталону 27px/3px/20px (было 38px/8px/18px), убран маркер `▼` у заголовков секций, карточки «Основного» — вертикальный стек (flex 50/50 остался только у `job-candidate-card-row`), подписи вкладок 12px→14px + theme-aware `$v-selection-color`, поля 16px→15px при высоте 38px, подписи строк контактов 150px→100px, sidebar-тиры 296px (≤1366) и 284px (≤1100), удалены мёртвые `.job-candidate-audit-*`; `styles.scss` ×7 — порядок слоёв по контракту 6.4 (после `edit-screen-shared-styles`); `JobCandidateEditLayoutContractTest` — ассерты 27px/14px. Бизнес-логика, loaders, actions и bindings не менялись. |
| 2026-07-31 | Полное inline-документирование XML-дескриптора `job-candidate-edit.xml` по правилу xml-screen-documentation.mdc: смысловые комментарии перед каждым открывающим элементом (window, data/instance/view/loader/query/condition, layout-контейнеры, вкладки, поля, таблицы, actions, кнопки). Структура XML не изменялась (проверено: теги/атрибуты/CDATA идентичны, добавлены только комментарии); сохранены незакоммиченные правки рабочей копии (кнопка `cardAuditInfoButton` в меню «Еще», `jobCandidateMainSectionContent` переведён на vbox). Контрактный тест и ScreenViewIntegrityTest — зелёные. |
| 2026-07-31 | Причины 2–4 выхода tabsheet за границы экрана (адаптивность на разных мониторах, по «Правилам компоновки экранов» CUBA): добавлены `box-sizing: border-box` + `width: 100%` для `.v-tabsheet-tabcontainer` и `.v-tabsheet-content` (padding 24/36px больше не расширяет их за 100%); `.job-candidate-tabs` получил `overflow: hidden` (перекрыт базовый `overflow: visible`); в media ≤1366px max-width вкладок 112px → 96px (7 вкладок = 672px влезают в узкий workspace). Во всех 7 темах. |
| 2026-07-31 | Причина 1 переполнения правого блока JobCandidateEdit: убран жёсткий `min-width` у `.job-candidate-editor` (база 1180px, media ≤1366px — 1024px) во всех 7 темах. Ранее при вьюпорте уже 1024px редактор принудительно растягивался шире экрана, и tabsheet с родительским workspace выходили за правую границу (горизонтальный скролл страницы). Контент по-прежнему ограничен `overflow: hidden` workspace. |
| 2026-07-31 | Ширина полей «Имя», «Отчество», «Фамилия» и остальных строк `job-candidate-form-grid` зафиксирована: колонка подписей в строках-контейнерах получила фиксированные 112px (`.v-horizontallayout > .v-expand > .v-slot-small`), поле — `calc(100% - 112px)` (`.v-slot-edit-form-control`), во всех 7 темах. Ранее на широких экранах (>1366px) подписи имели natural width, и поля ФИО различались по ширине. |
| 2026-07-31 | Высота полей «Имя», «Фамилия», «Отчество» (SuggestionField, `.c-suggestionfield`) приведена к высоте полей «Должность»/«Компания»: добавлены `.c-suggestionfield`/`.c-suggestion-field` в правило `height: 38px` формы (`.job-candidate-form-grid`/`.job-candidate-form-row`) во всех 7 темах. Ранее селектор-потомок `.edit-form-control .c-suggestionfield` не срабатывал на самом элементе input, и поля ФИО оставались 28px из базовой темы. |
| 2026-07-31 | Левая панель JobCandidateEdit: добавлен `box-sizing: border-box` для `job-candidate-sidebar` (ширина 312px теперь включает padding — блок не выходит за границы экрана) и для sidebar-карточек; рамки карточек усилены с `rgba(255,255,255,.18)` до `.35` для читаемости границ на тёмном фоне. Во всех 7 темах. |
| 2026-07-31 | Пункты label-навигации JobCandidateEdit приведены к размеру подписей полей формы: `font-size` 9px → 12px (совпадает с `.v-label-small` подписей «Город»/«Компания») во всех 7 темах; min-height, padding и line-height не менялись; идентичность 7 SCSS-файлов сохранена. |
| 2026-07-31 | Вкладка «Основное» JobCandidateEdit переведена на вертикальную раскладку: `jobCandidateMainSectionContent` изменён с `hbox` на `vbox`, карточки «Персональные данные» и «Профессиональные данные» размещаются одна над другой на всю ширину; удалён мёртвый класс `job-candidate-half-card`. В контракт-тест добавлена проверка вертикального порядка блоков. |
| 2026-07-31 | В меню «Еще» JobCandidateEdit добавлен пункт «Создано/Изменено» (`cardAuditInfoButton`, invoke `onCardAuditInfoClick`): уведомление с автором карточки (createdBy + createTs) и последним редактором (updatedBy + updateTs), формат даты dd.MM.yyyy HH:mm. В контракт-тест добавлены проверки кнопки. |
| 2026-07-31 | Удалён служебный элемент «Создано/Изменено» (дата + автор) из верхней панели JobCandidateEdit: XML-блок `jobCandidateAuditBox`/`createdUpdatedLabel` удалён, `expand` панели переключён на `moreActionsPopUpButton`; из контроллера удалены поле `createdUpdatedLabel`, метод `setCreatedUpdatedLabel()` и его вызов. |
| 2026-07-31 | Label-навигация JobCandidateEdit уменьшена целиком ещё на 30% (все визуальные параметры ×0.7): пункты — min-height 27→19px, padding 2×8px, шрифт 13→9px, line-height 20→14px; заголовок — padding 3px, шрифт 11→8px, line-height 12→8px; контейнер — padding 6×2px. |
| 2026-07-31 | Контейнер label-навигации JobCandidateEdit уплотнён на 30%: padding-top 12px → 8px, padding-bottom 3px; заголовок «Разделы формы» — padding 6px → 4px, line-height 16px → 12px. Пункты не менялись. |
| 2026-07-31 | Sidebar JobCandidateEdit приведён к контрактному порядку: блок «Рейтинг/Карточка» (`candidateProfileSummary`) перемещён между должностью кандидата и label-навигацией (visual → identity → summary → navigation). Обновлён контракт-тест порядка. |
| 2026-07-31 | Высота пунктов label-навигации уменьшена на 30%: min-height 38px → 27px, вертикальный padding 8px → 3px (текст 13px/20px не тронут, центрирование маркера flex'ом сохранено). Применено в JobCandidateEdit, эталоне IteractionListEdit и shared (34px → 24px). |
| 2026-07-31 | В JobCandidateEdit перенесена полная геометрия эталона label-навигации (вариант A, 1:1 с `iteraction-list-visual-alignment.scss:112–144`): min-height 38px, height auto, padding 8×10px, `border-radius: 0 5px 5px 0`, caption display:block; поверх shared flex-центрирования wrap — маркер точно по центру текста. |
| 2026-07-31 | Точное вертикальное центрирование маркера label-навигации: `.v-button-label-nav-item .v-button-wrap` в shared переведён на `display: flex; align-items: center` без min-height (текст ровно по центру жёлтой полоски-маркера). |
| 2026-07-31 | Label-навигация JobCandidateEdit приведена к утверждённому эталону `IteractionListEdit` во всех 7 темах: shared-геометрия пунктов (34px/13px) + эталонная цветовая адаптация active/hover — жёлтый `#ffb11b` на `rgba(255,177,27,.12)` с жёлтой левой границей, hover — белый на `rgba(255,255,255,.08)` (1:1 с `iteraction-list-visual-alignment.scss`). Ранее отличалась (скругление `0 5px 5px 0`, высота 36px, свой заголовок). |
| 2026-07-31 | SCSS-слой JobCandidateEdit синхронизирован во всех 7 темах по контракту Edit-форм: единый идентичный `job-candidate-editor.scss` (Вариант 3) во всех темах; исправлен артефакт подключения — `@include job-candidate-editor-theme` перенесён из невызываемого `@mixin star` в mixin темы для havana/helium/hover (ранее локальные стили формы в этих темах не применялись); в тестах закреплены идентичность 7 файлов и корректная схема подключения. |
| 2026-07-29 | По визуальной проверке во внутреннем браузере на `1280×720` устранено перекрытие sidebar/workspace: Vaadin slot sidebar зафиксирован на 312px во всех темах, вкладки ограничены `112px` с ellipsis, чтобы длинные captions не выходили за границы формы. |
| 2026-07-29 | Компоновка JobCandidateEdit уточнена по контракту Edit-форм: `candidateNavigation` перемещён сразу под профиль, строка вкладок уплотнена для диалога 1200 px, стабилизированы ширина footer, контейнеров резюме/комментариев, действий соцсетей и табличных карточек без изменения бизнес-логики. |
| 2026-07-29 | На реальной локальной сборке устранена регрессия компоновки вкладки «Основное»: фиксированная колонка подписей применяется к Vaadin `HBox` slots во всех семи темах; data bindings, loaders, actions и бизнес-логика не менялись. |
| 2026-07-29 | Экран приведён к общему контракту Edit-форм: добавлены `edit-*`/`label-*` stylename, видимая sidebar-навигация с active-state, сохранение `edit-sidebar-title` при runtime-обновлении ФИО и защита внутренних sidebar-блоков от переполнения. |
| 2026-07-21 | Исправлены типографика и центрирование ФИО, вывод должности, размеры sidebar labels и одинаковая ширина трёх полей ФИО; процент заполнения считается по 15 полям без изменения lazy-алгоритма. |
| 2026-07-21 | Профиль кандидата переведён на единый `OvaFallbackImage` 176×176 с локальным fallback; ФИО и должность синхронизируются через `jobCandidateDc` независимо от lazy-вкладок. |
| 2026-07-21 | Во всех семи темах восстановлена видимость должности и сохранение структурного класса ФИО при блокировке; добавлены регрессионные тесты и пользовательская инструкция. |
| 2026-07-15 | Завершено performance-тестирование: удалены временные runtime-пробы, performance-тесты, JFR/лог-скрипты и диагностическое system property; оптимизированный SCSS сохранён. |
| 2026-07-15 | Выполнен этап 1 клиентской оптимизации: удалён универсальный selector потомков, упрощены цепочки Vaadin-селекторов и сокращены принудительные CSS-ограничения без изменения XML и бизнес-логики. |
| 2026-07-14 | Добавлены диагностическое профилирование жизненного цикла JobCandidateEdit, unit-тесты, JFR-сборщик и генератор отчёта по времени открытия формы. |
| 2026-07-14 | Увеличены и приближены к подписям поля ФИО, должности и компании; шрифт SuggestionField синхронизирован с остальными полями вкладки «Основное» во всех темах. |
| 2026-07-14 | Исправлено фактическое отображение варианта 3: возвращены accordion-заголовки, растянуты GridLayout и поля, исправлены фон sidebar, повтор ФИО и кодировка звёзд рейтинга во всех темах. |
| 2026-07-14 | Реализована двухпанельная компоновка JobCandidateEdit, нижняя панель действий и локальный визуальный слой для подключённых тем. |
| 2026-07-14 | Сохранены XML-контракты, data bindings, actions, loaders и бизнес-логика экрана. |
