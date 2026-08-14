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
│   ├── ФИО и должность
│   ├── рейтинг и процент заполнения
│   ├── город / компания / индикатор резюме
│   ├── email / телефон / Telegram
│   ├── вертикальная навигация
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

### Вкладка «Резюме и файлы»

### Правая область

- workspace занимает всю оставшуюся ширину;
- «Еще» находится справа сверху;
- основные действия находятся справа снизу;
- вкладки сохраняют прежние ID и порядок;
- фон и границы одинаково определены для Halo, Hover, Havana и Helium.

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

Текущая перекомпоновка не содержит изменений SCSS.

---

## 6. Контроль качества и развертывание

Обязательные проверки при изменении контроллера или progressive loading:

```bash
git diff --check
./gradlew :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:test --tests '*JobCandidateProjectLogoViewContractTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
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
| 2026-07-15 | Вкладка «Основное» переведена с `GridLayout` на строковые `hbox`-контейнеры по шаблону вкладки «Контакты»: подписи и поля ввода видимы, а component ID, data bindings, actions, JPQL и `JobCandidateEdit.java` не изменены. |
| 2026-07-15 | Stage 3 прогрессивной загрузки: `socialNetwork` исключён из runtime-view первичной загрузки; коллекция загружается при первом открытии вкладки «Контакты» или «Социальные сети» с защитой от повторного SQL. |
| 2026-07-15 | Исправлена ошибка при открытии вкладки социальных сетей: уникальные `SocialNetworkType` догружаются batch-запросом через `socialNetworkType-view` и merge-ятся в `DataContext`, поэтому генератор не обращается к unfetched `logo`. |
| 2026-07-15 | Исправлена ошибка Stage 2 при открытии вкладки «Резюме»: проекты связанных вакансий догружаются batch-запросом через `project-browse-view` и merge-ятся в `DataContext`, поэтому генератор логотипа не обращается к unfetched `projectLogo`. |
| 2026-07-15 | Stage 2 прогрессивной загрузки: `candidateCv` исключён из runtime-view первичной загрузки; наличие резюме определяется скалярным `COUNT`, а полная коллекция загружается существующим методом при первом открытии вкладки «Резюме». |
| 2026-07-15 | Stage 1 прогрессивной загрузки: runtime-view `jobCandidateDl` копируется без `iteractionList`; взаимодействия загружаются существующим методом при первом открытии вкладки, а XML и контроллер сохранены без изменений. |
| 2026-07-15 | Завершено performance-тестирование: удалены временные runtime-пробы, performance-тесты, JFR/лог-скрипты и диагностическое system property; оптимизированный SCSS сохранён. |
| 2026-07-15 | Выполнен этап 1 клиентской оптимизации: удалён универсальный selector потомков, упрощены цепочки Vaadin-селекторов и сокращены принудительные CSS-ограничения без изменения XML и бизнес-логики. |
| 2026-07-14 | Добавлены диагностическое профилирование жизненного цикла JobCandidateEdit, unit-тесты, JFR-сборщик и генератор отчёта по времени открытия формы. |
| 2026-07-14 | Увеличены и приближены к подписям поля ФИО, должности и компании; шрифт SuggestionField синхронизирован с остальными полями вкладки «Основное» во всех темах. |
| 2026-07-14 | Исправлено фактическое отображение варианта 3: возвращены accordion-заголовки, растянуты GridLayout и поля, исправлены фон sidebar, повтор ФИО и кодировка звёзд рейтинга во всех темах. |
| 2026-07-14 | Реализована двухпанельная компоновка JobCandidateEdit, нижняя панель действий и локальный визуальный слой для подключённых тем. |
| 2026-07-14 | Сохранены XML-контракты, data bindings, actions, loaders и бизнес-логика экрана. |
