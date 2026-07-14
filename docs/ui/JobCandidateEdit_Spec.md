# JobCandidateEdit — спецификация экрана HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

`JobCandidateEdit` — основная рабочая карточка кандидата в HRM HuntTech. Экран объединяет персональные и профессиональные сведения, контакты, историю взаимодействий, резюме и файлы, социальные сети, комментарии, историю рассмотрения по вакансиям и список подходящих позиций.

Редизайн решает задачу быстрого визуального чтения карточки: ключевой профиль кандидата постоянно доступен слева, а рабочие данные открываются справа без потери функций и без изменения модели сохранения.

### 2. Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается:

- из `JobCandidateBrowse` при создании или редактировании кандидата;
- из экранов подбора и связанных рекрутинговых сценариев;
- из lookup-компонентов, которым требуется открыть выбранного кандидата.

Основная навигация выполняется через существующий `tabSheetSocialNetworks`. Состав и ID вкладок сохранены:

- `tabMain` — основные данные;
- `tabPositions` — позиции и вакансии;
- `tabIteraction` — взаимодействия;
- `tabResume` — резюме и файлы;
- `tabContactInfo` — контакты;
- `tabSocialNetworks` — социальные сети;
- `commentsTab` — комментарии;
- `tabHistory` — история.

Кнопка `openPositionMasterBrowseButton` открывает HR-Мастер для текущего кандидата. Кнопка `moreActionsPopUpButton` сохраняет существующие дополнительные действия блокировки и подписки.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие кандидата → загружается основной view → профиль отображается в двухпанельной форме.
- Первое открытие тяжёлой вкладки → соответствующие loaders получают обязательные параметры → данные загружаются один раз.
- После отображения → фоновая задача анализирует последнее резюме → метки навыков добавляются на UI-потоке.
- Редактирование поля → существующий `DataContext` фиксирует изменение → штатная валидация выполняется перед сохранением.
- «Сохранить и закрыть» → вызывается `windowCommitAndClose` → кандидат сохраняется и экран закрывается.
- «Отмена» → вызывается `windowClose` → применяется стандартный сценарий закрытия CUBA.
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

Редизайн изменяет только XML-компоновку и локальный SCSS. Java-контроллер, component ID, bindings, actions, invoke, loaders, views и JPQL не изменяются.

Обязательные compatibility-компоненты контроллера сохранены:

| ID | XML-тип | Назначение |
|---|---|---|
| `lastProjects` | `groupBox` | Сохранение существующего `@Inject`-контракта |
| `dictionatysTavlesHBox` | `grid` | Сохранение существующего `@Inject`-контракта |
| `candidateNavigation` | `vbox` | Сохранение существующих кнопок и invoke вертикальной навигации |

Рабочие таблицы `lastProjectTable` и `suggestVacancyTable` существуют в одном экземпляре внутри `tabPositions`.

---

## 2. Модель данных, views и loaders

### Основные контейнеры

| Контейнер | Тип данных | Назначение |
|---|---|---|
| `jobCandidateDc` | `JobCandidate` | редактируемый кандидат |
| `jobCandidateCandidateCvsDc` | `CandidateCV` | резюме кандидата |
| `jobCandidateIteractionDc` | `IteractionList` | взаимодействия кандидата |
| `jobCandidateSocialNetworksDc` | `SocialNetworkURLs` | социальные сети |
| `lastProjectDc` | `KeyValueEntity` | история рассмотрения по вакансиям |
| `suggestOpenPositionDc` | `OpenPosition` | подходящие вакансии |

### Правила загрузки

- Защита loaders от запуска без обязательных параметров сохраняется.
- Тяжёлые вкладки инициализируются лениво существующим контроллером.
- Inline-view `suggestOpenPositionDc` содержит только связи, используемые таблицей и tooltip.
- Визуальная перестройка не меняет момент загрузки, параметры и состав данных.

---

## 3. Визуальная структура варианта 3

```text
jobCandidateMainLayout
├── jobCandidateSidebar — 312 px
│   ├── candidateProfileHeader
│   │   ├── фотография 176 × 176 px
│   │   ├── загрузка/очистка фотографии
│   │   ├── ФИО
│   │   └── должность
│   ├── candidateProfileSummary
│   │   ├── рейтинг
│   │   └── город / компания / резюме
│   ├── candidateProfileContacts
│   │   └── email / телефон / Telegram
│   ├── candidateSidebarSpacer
│   └── candidateProfileFooter — HR-Мастер
└── jobCandidateWorkspace
    ├── jobCandidateTopBar — аудит слева, «Еще» справа
    ├── tabSheetSocialNetworks
    └── jobCandidateBottomBar — «Сохранить и закрыть», «Отмена»
```

### Левая профильная панель

- Фиксированная ширина — `312 px`, на экранах до 1366 px — `286 px`.
- Темный фон, контрастный текст и локальные карточки.
- Фотография круглая, пропорции сохраняются через `SCALE_DOWN` и `object-fit: cover`.
- ФИО, должность и значения карточек допускают перенос по словам.
- Длинные компания, email и Telegram не выходят за границы панели.
- `candidateSidebarSpacer` занимает свободную высоту и прижимает HR-Мастер к нижней границе.
- Дублирующая вертикальная навигация сохранена в XML и Java для совместимости; в варианте 3 пользователю показывается штатная горизонтальная навигация `TabSheet`.

### Правая рабочая область

- `jobCandidateWorkspace` занимает всю ширину после sidebar.
- Верхняя панель содержит служебную информацию и кнопку «Еще».
- Нижняя панель является реальным XML-контейнером, а не `position: fixed`-эмуляцией.
- Кнопки сохранения и отмены перемещены без создания копий и продолжают использовать прежние actions.

### Вкладка «Основное»

`jobCandidateMainSectionContent` содержит две равные карточки:

- `personalDataBlock` — 50% доступной ширины;
- `professionalDataBlock` — 50% доступной ширины.

Между карточками используется промежуток 16 px. Внутри:

- подписи имеют фиксированную визуальную ширину;
- поля занимают оставшееся пространство;
- `SuggestionField`, `PickerField`, `LookupPickerField`, `SuggestionPickerField`, `DateField` и их Vaadin wrappers растянуты на 100%;
- высота полей — 38 px;
- заголовок карточки — 18 px, основной текст — 16 px.

### Остальные вкладки

- `tabContactInfo` использует две равные карточки основных и дополнительных контактов.
- `tabPositions`, `tabIteraction`, `tabResume`, `tabSocialNetworks` и `commentsTab` сохраняют цепочки `expand`, необходимые для полной высоты таблиц.
- Таблицы и DataGrid занимают доступную ширину и высоту, сохраняя actions, columns и dataContainer.
- `tabHistory` оформлен как компактная карточка без дублирования ID служебных labels.

---

## 4. Локальные стили и темы

Общий mixin:

```scss
@mixin job-candidate-editor-theme
```

Селекторы ограничены корнем:

```scss
.job-candidate-editor
```

Один визуальный слой синхронизирован для тем:

- Halo;
- Hover;
- Havana;
- Helium.

Запрещены глобальные изменения `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` без родительского селектора `.job-candidate-editor`.

Ключевые классы:

| Класс | Назначение |
|---|---|
| `job-candidate-main-layout` | двухпанельная геометрия |
| `job-candidate-sidebar` | профильная панель |
| `job-candidate-avatar` | фотография кандидата |
| `job-candidate-sidebar-card` | локальные карточки sidebar |
| `job-candidate-workspace` | правая рабочая область |
| `job-candidate-top-bar` | верхняя панель |
| `job-candidate-tabs` | горизонтальные вкладки |
| `job-candidate-card` | карточки содержимого |
| `job-candidate-form-grid` | формы вкладки «Основное» |
| `job-candidate-form-row` | строки контактных полей |
| `job-candidate-table` | таблицы и DataGrid |
| `job-candidate-bottom-bar` | нижняя панель действий |

---

## 5. Actions, поля и неизменяемые контракты

| Компонент | Сохранённый контракт |
|---|---|
| `windowCommitAndCloseButton` | action `windowCommitAndClose` |
| кнопка отмены | action `windowClose` |
| `moreActionsPopUpButton` | прежний popup и дочерние handlers |
| `fileImageFaceUpload` | прежний data binding и обработчики загрузки |
| `firstNameField`, `middleNameField`, `secondNameField` | прежние properties, query и search executor |
| `currentCompanyField` | прежние actions и поиск компании |
| `jobCandidateIteractionListTable` | прежние actions, columns и handlers |
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers |
| `socialNetworkTable` | прежний editor и column generators |

Редизайн не меняет required, editable, visible, enabled, selectionMode, порядок сохранения или бизнес-валидацию.

---

## 6. Контроль качества и развертывание

Обязательные проверки разработчика/DevOps:

```bash
git diff --check
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

После pull ветки Hermes выполняет только сборку и развертывание, не изменяя исходники:

```bash
git fetch origin
git switch agent/job-candidate-edit-layout-fix
git pull --ff-only origin agent/job-candidate-edit-layout-fix
./scripts/rebuild-widgetset-and-start.sh
```

Функциональная проверка:

- открыть существующего и нового кандидата;
- проверить сохранение, отмену и меню «Еще»;
- проверить фотографию и HR-Мастер;
- пройти все вкладки;
- проверить подсказки ФИО и picker actions;
- проверить таблицы и ленивую загрузку;
- проверить темы Halo, Hover, Havana и Helium;
- подтвердить HTTP 200 для контекста `/hrm`.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-14 | Реализован дизайн JobCandidateEdit по варианту 3: темная профильная панель, равные карточки 50/50, горизонтальные вкладки и нижняя панель действий; визуальный слой синхронизирован для Halo, Hover, Havana и Helium. |
| 2026-07-14 | Восстановлена полная функциональность `tabPositions`, добавлена отложенная загрузка истории и защищены параметры loaders. |
| 2026-07-14 | Анализ навыков сохранён как фоновая операция; исправлены XML-контракты, data binding и цепочки `expand`. |
