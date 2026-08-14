# JobCandidateEdit — спецификация экрана HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

`JobCandidateEdit` — основная рабочая карточка кандидата в HRM HuntTech. Экран объединяет персональные и профессиональные сведения, контакты, позиции и вакансии, взаимодействия, резюме и файлы, социальные сети, комментарии и историю записи.

Критический путь открытия должен содержать только данные, необходимые рекрутеру для начала работы. Тяжёлые дочерние коллекции, вычисления и обращения к файловому хранилищу не должны блокировать first paint.

### 2. Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается:

- из `JobCandidateBrowse` при создании или редактировании кандидата;
- из экранов подбора кандидатов;
- из связанных рекрутинговых сценариев и lookup-компонентов.

Основная навигация выполняется через `tabSheetSocialNetworks`:

- `tabMain` — основные данные;
- `tabPositions` — позиции и вакансии;
- `tabIteraction` — взаимодействия;
- `tabResume` — резюме и файлы;
- `tabContactInfo` — контакты;
- `tabSocialNetworks` — социальные сети;
- `commentsTab` — комментарии;
- `tabHistory` — история.

На вкладке «Основное» рекрутер выбирает город, основную должность и компанию кандидата. Поле компании поддерживает suggestion, lookup, open и создание новой компании.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие кандидата → перед `InitEvent` из runtime-view последовательно исключаются `iteractionList`, `candidateCv` и `socialNetwork` → основная карточка открывается без предварительной материализации взаимодействий, резюме и социальных сетей.
- Отображение вкладки «Основное» → экран создаёт две равные карточки со строками по шаблону вкладки «Контакты» → подписи и связанные поля ввода видимы и занимают доступную ширину.
- Определение наличия резюме → после показа формы выполняется скалярный `COUNT` по идентификатору кандидата → боковой индикатор показывает «Резюме: ДА» или «Резюме: НЕТ» без загрузки сущностей `CandidateCV`.
- Первое открытие вкладки «Взаимодействия» → существующий `ensureInteractionsLoaded()` выполняет узкий запрос → сущности merge-ятся в экранный `DataContext` и отображаются в штатной таблице.
- Первое открытие вкладки «Резюме» → существующий `ensureCandidateCvLoaded()` выполняет отдельный запрос с `candidateCV-browse-view` → резюме merge-ятся в `DataContext` и отображаются в прежней таблице.
- Отображение логотипов проектов в таблице CV → после штатной загрузки резюме собираются уникальные проекты связанных вакансий → проекты догружаются одним запросом через `project-browse-view` и merge-ятся в тот же `DataContext` с загруженным `projectLogo`.
- Первое открытие вкладки «Контакты» или «Социальные сети» → коллекция `socialNetwork` загружается отдельным запросом через `socialNetworkURLs-view` → строки merge-ятся в экранный `DataContext` и становятся доступны обеим вкладкам.
- Отображение логотипов социальных сетей → после штатной загрузки строк собираются уникальные `SocialNetworkType` → справочники догружаются одним запросом через `socialNetworkType-view` и merge-ятся в тот же `DataContext` с загруженным `logo`.
- Повторное открытие вкладки → проверяются флаги и состояние загрузки свойства → повторная полная загрузка и дублирование строк не выполняются.
- Изменение поля → данные остаются в штатном `DataContext` → перед сохранением выполняется существующая валидация.
- «Сохранить и закрыть» → выполняется `windowCommitAndClose` → кандидат сохраняется и экран закрывается.
- «Отмена» → выполняется `windowClose` → применяется стандартный сценарий CUBA.

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

Визуальная доработка не меняет сущности, component ID, data bindings, actions, invoke, loaders, XML views и JPQL существующего экрана. Java-контроллер `JobCandidateEdit.java` не изменяется.

### Этап 1 — взаимодействия

`JobCandidateInitialViewOptimizer` копирует runtime-view `jobCandidateDl` и исключает `iteractionList` из первичной загрузки. Коллекция загружается методом `ensureInteractionsLoaded()` при первом открытии вкладки взаимодействий.

### Этап 2 — резюме

`JobCandidateCvInitialViewOptimizer` исключает `candidateCv` из первичной загрузки. Optimizer отвечает только за сокращение view и установку hydration-listener’ов.

Индикатор наличия резюме принадлежит контроллеру `JobCandidateEdit`:

```jpql
select count(e)
from hunttech_CandidateCV e
where e.candidate.id = :candidateId
  and e.deleteTs is null
```

Запрос:

- выполняется только после first paint внутри `BackgroundTask`;
- получает UUID кандидата, а не detached entity;
- не загружает `CandidateCV`, `textCV`, файлы и связанные entity-графы;
- является единственным источником `labelCV`;
- не устанавливает `candidateCvLoaded` и не изменяет коллекцию кандидата.

Полная коллекция загружается методом `ensureCandidateCvLoaded()` только при первом открытии `tabResume`. После загрузки проекты связанных вакансий догружаются одним запросом через `project-browse-view`.

### Этап 3 — социальные сети

`JobCandidateSocialNetworkInitialViewOptimizer` исключает `socialNetwork` из первичной загрузки. Коллекция загружается при первом открытии вкладки контактов или социальных сетей. Уникальные `SocialNetworkType` догружаются одним batch-запросом через `socialNetworkType-view`.

### Этапы 9–14 — операции после first paint

| Этап | Результат |
|---|---|
| Stage 9 | Последнее взаимодействие загружается только при копировании без выбранной строки. |
| Stage 10 | Индикатор CV переведён с чтения коллекции на scalar `COUNT`. |
| Stage 11 | Scalar `AVG` рейтинга перенесён в `BackgroundTask`. |
| Stage 12 | Проверка фотографии в file storage перенесена в `BackgroundTask`. |
| Stage 13 | Scalar `COUNT` индикатора CV перенесён после first paint. |
| Stage 14 | Удалён дублирующий `COUNT` из optimizer; сохранён один background-источник. |

### Справочник компаний

Из XML и Java удалены полный контейнер и loader компаний. `currentCompanyField` выполняет серверный suggestion-поиск:

```jpql
select e
from hunttech_Company e
where lower(e.comanyName) like lower(:searchString)
order by e.comanyName, e.companyShortName
```

Результат ограничен 50 строками и загружается через `company-picker-view`.

Create-company flow точечно загружает сохранённую запись:

```jpql
select e
from hunttech_Company e
where e.id = :companyId
```

### Справочники должностей и городов

`personPositionsLc` использует `position-picker-view` и сохраняет прежний JPQL, фильтр и сортировку. `citiesDl` использует `city-picker-view`.

Изменение типа picker-компонентов, JPQL, индексов или Liquibase допускается только после отдельного runtime baseline.

---

## 2. Модель данных и загрузка

| Контейнер / данные | Назначение | Момент загрузки |
|---|---|---|
| `jobCandidateDc` | редактируемый кандидат | initial load |
| `jobCandidateCandidateCvsDc` | резюме кандидата | первое открытие `tabResume` |
| `jobCandidateIteractionDc` | взаимодействия | первое открытие `tabIteraction` |
| `jobCandidateSocialNetworksDc` | социальные сети | первое открытие контактов или соцсетей |
| `lastProjectDc` | история рассмотрения | после фоновой подготовки `tabPositions` |
| `suggestOpenPositionDc` | подходящие вакансии | первое открытие `tabPositions` |
| `personPositionsDc` | справочник должностей через `position-picker-view` | инициализация `tabMain` |
| `citiesDc` | справочник городов через `city-picker-view` | инициализация `tabMain` |
| `labelCV` | наличие неудалённого CV | background scalar `COUNT` после first paint |
| `candidateRatingLabel` | средний рейтинг | background scalar `AVG` после first paint |
| `candidatePic` | фотография | background file-storage check после first paint |

Граф для логотипа проекта в таблице резюме:

```text
JobCandidate.candidateCv
└── CandidateCV.toVacancy
    └── OpenPosition.projectName
        ├── Project.projectDescription
        ├── Project.projectLogo → FileDescriptor (_local)
        └── Project.projectDepartment
```

`Project.projectLogo` остаётся `FetchType.LAZY`. Загрузка осуществляется локально в экранном view `job-candidate-edit.xml` через вложенное свойство `<property name="projectLogo" view="_local"/>`. Глобальный shared-entity view `openPosition-edit-view` не изменяется.

Правила:

- ленивое открытие тяжёлых вкладок сохраняется;
- обязательные параметры loaders не изменяются;
- визуальный слой не выполняет дополнительные запросы;
- таблицы продолжают использовать прежние dataContainer и actions;
- ссылочные атрибуты, читаемые генераторами колонок, должны иметь явный вложенный view.

---

## 3. Визуальная компоновка

```text
jobCandidateMainLayout
├── jobCandidateSidebar
│   ├── фотография
│   ├── ФИО
│   ├── label-navigation по вкладкам
│   ├── рейтинг и процент заполнения
│   ├── город / компания / индикатор резюме
│   ├── email / телефон / Telegram
│   ├── растягиваемое свободное пространство
│   └── быстрые действия: резюме, взаимодействие, HR-Мастер
└── jobCandidateWorkspace
    ├── верхняя панель: служебные данные и «Еще»
    ├── TabSheet
    └── нижняя панель: «Сохранить и закрыть», «Отмена»
```

- `personalDataBlock` и `professionalDataBlock` занимают равные доли;
- внутренние `GridLayout` растягиваются на 100%;
- основные поля занимают доступную ширину;
- фотография до фоновой проверки отображается заглушкой;
- индикаторы рейтинга и CV обновляются после first paint;
- оформление поддерживается в Halo, Hover, Havana и Helium.

- ширина — 312 px, на экранах до 1366 px — 286 px;
- тёмный фон задаётся самому `jobCandidateSidebar` и его Vaadin slot-обёртке;
- фотография отображается единым `OvaFallbackImage` размером 176×176 и не искажается;
- при отсутствии фотографии или бинарного файла показывается `icons/no-programmer.jpeg`;
- ФИО в формате `Фамилия Имя` и должность видны непосредственно под фотографией;
- ФИО и должность обновляются сразу при изменении полей и не зависят от lazy-создания вкладки «Основное»;
- `candidateNavigation` расположен сразу после профильного блока, до карточек детализации, по общему Edit-контракту HRM HuntTech;
- ФИО центрировано и использует типографику `candidateRatingLabel`, а остальные sidebar labels уменьшены на один пункт;
- процент заполнения рассчитывается по 15 свойствам `jobCandidateDc` без открытия lazy-вкладок и дополнительных запросов;
- текст карточек имеет размер 16 px и переносится по словам;
- быстрые действия прижаты к нижней границе;
- заблокированный кандидат сохраняет профильное позиционирование ФИО, меняется только цвет.
- `candidateNavigation` использует общие классы `label-navigation`, `label-nav-title`,
  `label-nav-item`, `label-nav-item-active`; локальный `job-candidate-nav-item` оставлен
  только для цветовой адаптации sidebar;
- внутренние sidebar-блоки и Vaadin slots имеют `min-width: 0`/`max-width: 100%`, поэтому
  карточки, кнопки и длинные подписи не выходят за границы панели;

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
- «Еще» находится справа сверху;
- основные действия находятся справа снизу;
- вкладки сохраняют прежние ID и порядок, но используют компактные подписи 14 px и меньшие боковые отступы, чтобы строка вкладок помещалась в стандартный диалог 1200 px;
- нижняя панель действий занимает 100% ширины workspace и выравнивает кнопки по правому краю;
- внутренние контейнеры вкладок «Резюме» и «Комментарии» имеют width 100%, чтобы таблицы не сжимались до intrinsic width;
- колонка действий соцсетей фиксирована на 220 px, а таблица занимает оставшуюся ширину;
- табличные карточки вкладки «Позиции и вакансии» получают `min-width: 0` и локальную горизонтальную прокрутку таблицы, если набор колонок шире доступной области;
- фон и границы одинаково определены для Halo, Hover, Havana, Helium и modern-тем.

### Accordion-заголовки

XML содержит контейнеры `job-candidate-accordion-header` и `job-candidate-accordion-content`. Accordion-слой не удаляет и не подменяет штатную навигацию `TabSheet`. Поведение полей, таблиц, loaders и действий не меняется.

### Вкладка «Основное»

- `jobCandidateMainSectionContent` использует тот же строковый паттерн `job-candidate-card-row`, что и вкладка «Контакты»;
- `personalDataBlock` и `professionalDataBlock` занимают равные доли доступной ширины благодаря `box.expandRatio="1"`;
- каждое поле помещено в отдельный `hbox` со стилем `job-candidate-form-row`;
- подпись имеет фиксированную ширину 118 px;
- атрибут `expand` строки указывает на поле, поэтому компонент ввода занимает всё оставшееся пространство;
- поля `firstNameField`, `middleNameField`, `secondNameField`, `birdhDateField`, `jobCityCandidateField`, `personPositionField` и `currentCompanyField` видимы и растянуты по ширине;
- контейнер `positionsControl` сохраняет существующие `positionsLabel` и `addPositions`, выводя выбранные дополнительные позиции и кнопку справа;
- component ID, `dataContainer`, `property`, `required`, actions и JPQL-запросы suggestion-компонентов не изменены;
- `JobCandidateEdit.java` не изменён.

### Вкладка «Контакты»

- основные и дополнительные контакты занимают равные доли;
- подписи имеют фиксированную ширину;
- поля занимают оставшееся пространство;
- `radioButtonGroup` сохраняет прежнюю бизнес-логику и привязку `priorityContact`.

---

## 4. Actions и неизменяемые контракты

| Компонент | Контракт |
|---|---|
| `windowCommitAndCloseButton` | action `windowCommitAndClose` |
| кнопка отмены | action `windowClose` |
| `moreActionsPopUpButton` | прежний popup и handlers |
| `currentCompanyField` | suggestion, `picker_lookup`, `picker_open`, `createCompany` |
| `personPositionField` | `optionsContainer=personPositionsDc`, lookup и open |
| `jobCityCandidateField` | `optionsContainer=citiesDc`, lookup |
| `jobCandidateIteractionListTable` | прежние actions, columns и handlers |
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers; логотип проекта обеспечивается view |
| `socialNetworkTable` | прежний editor и generators |
| `fileImageFaceUpload` | immediate upload, clear и обновление изображения |

Визуальная перекомпоновка вкладки «Основное» не меняет CRUD резюме, распознавание контактов, копирование CV, проверку навыков, загрузку файлов, связи CV с вакансией, CRUD социальных сетей и реализации генераторов `projectLogoColumn` и `socialNetworkLogoColumn`.

---

## 5. Стили и поддержка тем

Все правила ограничены `.job-candidate-editor`. Используются только локальные style name с префиксом `job-candidate-`.

```scss
@mixin job-candidate-editor-theme
```

Все правила ограничены `.job-candidate-editor`. Одинаковый файл `job-candidate-editor.scss` используется в темах Halo, Hover, Havana и Helium.

Глобальные `.v-table`, `.v-label`, `.v-button` и `.v-tabsheet` вне `.job-candidate-editor` не изменяются. Для рейтинга используются CSS Unicode escapes (`\2605`, `\2606`).

По результатам анализа клиентского рендеринга сохранены:

- локальные layout-классы вместо универсального selector всех потомков;
- явный список rating-классов;
- единый класс `job-candidate-sidebar-card`;
- строковый класс `job-candidate-form-row`, общий для вкладок «Основное» и «Контакты»;
- минимально необходимый набор `!important` для конфликтов с inline-размерами Vaadin.

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

Обязательные проверки при изменении контроллера или progressive loading:

```bash
git diff --check
./gradlew :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:test --tests '*JobCandidateProjectLogoViewContractTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.JobCandidateEditLayoutContractTest' --no-daemon --stacktrace
```

Проверка вкладки «Основное»:

- открыть существующего кандидата и нового кандидата;
- проверить видимость подписей и компонентов ввода «Имя», «Отчество», «Фамилия», «Дата рождения», «Город», «Должность», «Компания», «Доп. позиции»;
- проверить ввод и изменение ФИО и даты рождения;
- проверить lookup города и должности;
- проверить поиск, lookup и open для компании;
- проверить отображение `positionsLabel` и действие `addPositions`;
- проверить сохранение и отмену без изменения прежней бизнес-логики;
- проверить отсутствие ошибок CUBA layout/expand и runtime-ошибок по изменённому сценарию.

Ручная проверка Stage 3 и регрессии Stage 1–2:

- HTTP 200 для `/hrm`;
- открыть тяжёлого существующего кандидата и убедиться, что initial open не материализует `socialNetwork`;
- открыть сначала `tabSocialNetworks`, не открывая `tabContactInfo`, и проверить загрузку всех строк и логотипов;
- открыть сначала `tabContactInfo`, затем `tabSocialNetworks`, и подтвердить отсутствие второго полного запроса;
- повторно переключить обе вкладки и проверить отсутствие дубликатов и повторной полной загрузки;
- проверить кандидата без социальных сетей и строку без выбранного `SocialNetworkType`;
- проверить нового кандидата: создание строк справочников, редактирование, отмену и сохранение;
- выполнить создание, редактирование и удаление социальной сети у существующего кандидата;
- сохранить существующего кандидата без открытия вкладок и подтвердить, что социальные сети не удалены;
- сохранить после открытия вкладок и убедиться в отсутствии дубликатов;
- проверить логотипы типов соцсетей без `Cannot get unfetched attribute [logo]`;
- повторно открыть вкладки «Взаимодействия» и «Резюме» и подтвердить сохранение Stage 1–2;
- проверить `projectLogo`, `candidateCv`, `iteractionList` и `socialNetwork` по логам;
- в логах должны отсутствовать unfetched/detached-ошибки, `IllegalStateException`, `NullPointerException` по изменённому сценарию и `OutOfMemoryError`;
- SQL-доказательство должно разделять initial open, первое открытие вкладки социальных сетей и повторное открытие;
- отчёт по текущей задаче сохранить в `docs/performance-archive/2026-07-15/main-tab-field-layout/`.

## История изменений

| Дата | Изменение |
|---|---|
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
