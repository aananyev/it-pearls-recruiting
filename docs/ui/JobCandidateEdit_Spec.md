# JobCandidateEdit — спецификация экрана HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

`JobCandidateEdit` — основная рабочая карточка кандидата в HRM HuntTech. Экран объединяет персональные и профессиональные сведения, контакты, позиции и вакансии, взаимодействия, резюме и файлы, социальные сети, комментарии и историю записи.

Визуальная компоновка должна позволять рекрутеру постоянно видеть краткий профиль кандидата и одновременно работать с детальными данными. Прогрессивная загрузка уменьшает критический путь открытия формы: тяжёлые дочерние коллекции загружаются только тогда, когда пользователь открывает соответствующую вкладку.

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
- `tabSocialNetworks` — социальные сети;
- `commentsTab` — комментарии;
- `tabHistory` — история.

Кнопка `openPositionMasterBrowseButton` открывает HR-Мастер для текущего кандидата. Кнопка `moreActionsPopUpButton` содержит существующие действия блокировки и подписки.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие кандидата → перед `InitEvent` из runtime-view последовательно исключаются `iteractionList`, `candidateCv` и `socialNetwork` → основная карточка открывается без предварительной материализации взаимодействий, резюме и социальных сетей.
- Определение наличия резюме → после показа формы выполняется скалярный `COUNT` по идентификатору кандидата → боковой индикатор показывает «Резюме: ДА» или «Резюме: НЕТ» без загрузки сущностей `CandidateCV`.
- Первое открытие вкладки «Взаимодействия» → существующий `ensureInteractionsLoaded()` выполняет узкий запрос → сущности merge-ятся в экранный `DataContext` и отображаются в штатной таблице.
- Первое открытие вкладки «Резюме» → существующий `ensureCandidateCvLoaded()` выполняет отдельный запрос с `candidateCV-browse-view` → резюме merge-ятся в `DataContext` и отображаются в прежней таблице.
- Отображение логотипов проектов в таблице CV → после штатной загрузки резюме собираются уникальные проекты связанных вакансий → проекты догружаются одним запросом через `project-browse-view` и merge-ятся в тот же `DataContext` с загруженным `projectLogo`.
- Первое открытие вкладки «Контакты» или «Социальные сети» → коллекция `socialNetwork` загружается отдельным запросом через `socialNetworkURLs-view` → строки merge-ятся в экранный `DataContext` и становятся доступны обеим вкладкам.
- Отображение логотипов социальных сетей → после штатной загрузки строк собираются уникальные `SocialNetworkType` → справочники догружаются одним запросом через `socialNetworkType-view` и merge-ятся в тот же `DataContext` с загруженным `logo`.
- Повторное открытие вкладки → проверяются флаги и состояние загрузки свойства → повторная полная загрузка и дублирование строк не выполняются.
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

Визуальная доработка не меняет сущности, component ID, data bindings, actions, invoke, loaders, XML views и JPQL существующего экрана.

### Этап 1 — взаимодействия

Компонент `JobCandidateInitialViewOptimizer` выполняется после штатной dependency injection и до `InitEvent`. Он копирует назначенный `jobCandidateDl` view со всеми fetch mode и вложенными views и исключает свойство `iteractionList`.

### Этап 2 — резюме

Компонент `JobCandidateCvInitialViewOptimizer` также работает через `ControllerDependencyInjector` и не меняет `JobCandidateEdit.java` или `job-candidate-edit.xml`. Он получает runtime-view, уже обработанный другими оптимизаторами, копирует его и исключает только `candidateCv`.

После показа формы компонент выполняет агрегатный запрос:

```jpql
select count(e.id)
from hunttech_CandidateCV e
where e.candidate.id = :candidateId
```

Запрос используется только для бокового индикатора наличия резюме и не загружает текст CV, письма, ссылки, файлы или связанные вакансии.

Штатный `candidateCV-browse-view` намеренно остаётся узким и не загружает LOB-поля. Вложенный `toVacancy.projectName` в нём использует `project-picker-view`, поэтому поле `projectLogo` отсутствует. После загрузки таблицы `JobCandidateCvInitialViewOptimizer` собирает идентификаторы уже загруженных проектов и выполняет один batch-запрос:

```jpql
select e
from hunttech_Project e
where e.id in :projectIds
```

Запрос использует `project-browse-view`, содержащий `projectLogo`. Загруженные проекты merge-ятся в существующий `DataContext`. Merge обогащает managed-экземпляры `Project` и не помечает `CandidateCV`, `OpenPosition` или `Project` изменёнными для commit.

Штатный `socialNetworkURLs-view` остаётся узким: он загружает данные строки и ссылку на `SocialNetworkType`, но не поле `logo`. При первом открытии вкладки контактов или социальных сетей компонент собирает уникальные идентификаторы типов и выполняет один batch-запрос:

```jpql
select e
from hunttech_SocialNetworkType e
where e.id in :typeIds
```

Запрос использует `socialNetworkType-view`, содержащий `logo`. Загруженные справочники merge-ятся в текущий `DataContext`; генератор колонки получает загруженный `FileDescriptor`, а `SocialNetworkURLs` и `SocialNetworkType` не помечаются изменёнными для commit.

### Этап 3 — социальные сети

Компонент `JobCandidateSocialNetworkInitialViewOptimizer` выполняется через тот же lifecycle-extension point и копирует уже оптимизированный runtime-view без свойства `socialNetwork`. Проверенные компоненты Stage 1 и Stage 2, контроллер и XML-дескриптор не изменяются.

После `AfterShow` компонент подключает listener к `tabSheetSocialNetworks`. При первом выборе `tabContactInfo` или `tabSocialNetworks` он проверяет `PersistenceHelper.isLoaded(candidate, "socialNetwork")`. Если штатный код вкладки «Контакты» уже загрузил коллекцию, дополнительный SQL не выполняется. В остальных случаях выполняется узкий запрос:

```jpql
select e
from hunttech_SocialNetworkURLs e
where e.jobCandidate.id = :candidateId
order by e.networkName
```

Строки загружаются через `socialNetworkURLs-view`, merge-ятся в экранный `DataContext` и устанавливаются в композицию кандидата. Для нового кандидата SQL не выполняется: используется штатная пустая композиция и прежняя логика добавления отсутствующих типов социальных сетей.

Обязательные compatibility-компоненты контроллера:

| ID | XML-тип | Назначение |
|---|---|---|
| `lastProjects` | `groupBox` | сохранение существующего `@Inject`-контракта |
| `dictionatysTavlesHBox` | `grid` | сохранение существующего `@Inject`-контракта |
| `candidateNavigation` | `vbox` | сохранение существующих invoke-методов навигации |
| `labelCV` | `label` | отображение скалярного результата наличия резюме |
| `tabSheetSocialNetworks` | `tabSheet` | подключение ленивой загрузки коллекций и постзагрузочной гидратации логотипов |

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

Правила прогрессивной загрузки:

- `iteractionList` исключён из runtime-view первичной загрузки `jobCandidateDl` и загружается при первом открытии `tabIteraction`;
- `candidateCv` исключён из runtime-view первичной загрузки и загружается при первом открытии `tabResume`;
- индикатор наличия CV вычисляется скалярным `COUNT`, поэтому полная коллекция не требуется при открытии формы;
- `ensureCandidateCvLoaded()` использует существующий `candidateCV-browse-view`, merge-ит результат в экранный `DataContext` и устанавливает коллекцию кандидату;
- логотипы проектов не расширяют основной CV view и догружаются одним запросом только после открытия `tabResume`;
- уникальные project ID собираются по уже загруженной цепочке `CandidateCV → OpenPosition → Project`, без обращения к unfetched `projectLogo`;
- проекты загружаются через `project-browse-view` и merge-ятся в текущий `DataContext`, поэтому генератор `projectLogoColumn` получает загруженный `projectLogo`;
- после CRUD резюме флаг гидратации сбрасывается, чтобы логотипы нового набора проектов могли быть догружены повторно;
- `socialNetwork` исключён из runtime-view первичной загрузки Stage 3;
- открытие `tabContactInfo` или `tabSocialNetworks` загружает коллекцию через `socialNetworkURLs-view` только при незагруженном свойстве;
- штатная загрузка вкладки «Контакты» и компонент Stage 3 не выполняют два запроса благодаря `PersistenceHelper.isLoaded`;
- уникальные ID типов соцсетей собираются без обращения к unfetched `logo`;
- `SocialNetworkType` догружаются через `socialNetworkType-view` одним запросом после штатного заполнения контейнера;
- первичная инициализация нескольких строк соцсетей не запускает отдельный запрос на каждую строку;
- после CRUD социальных сетей флаг гидратации логотипов сбрасывается для нового набора справочников;
- `ensureInteractionsLoaded()` использует существующий `iteractionList-job-candidate`, merge-ит результат в `DataContext` и заполняет прежний `jobCandidateIteractionDc`;
- каждый оптимизатор копирует все остальные свойства, fetch mode, вложенные views и флаг `loadPartialEntities`;
- component ID, loader ID, actions, invoke и бизнес-правила не изменены;
- таблицы продолжают использовать прежние dataContainer и actions;
- сохранение кандидата без открытия вкладок не должно удалять или обнулять существующие взаимодействия, резюме и социальные сети.

---

## 3. Визуальная компоновка

```text
jobCandidateMainLayout
├── jobCandidateSidebar
│   ├── фотография
│   ├── ФИО
│   ├── рейтинг и процент заполнения
│   ├── город / компания / индикатор резюме
│   ├── email / телефон / Telegram
│   ├── растягиваемое свободное пространство
│   └── HR-Мастер
└── jobCandidateWorkspace
    ├── верхняя панель: служебные данные и «Еще»
    ├── горизонтальные вкладки
    ├── видимые accordion-заголовки разделов
    └── нижняя панель: «Сохранить и закрыть», «Отмена»
```

### Левая панель

- ширина — 312 px, на экранах до 1366 px — 286 px;
- тёмный фон задаётся самому `jobCandidateSidebar` и его Vaadin slot-обёртке;
- фотография отображается круглой и не искажается;
- ФИО имеет размер 24 px;
- текст карточек имеет размер 16 px и переносится по словам;
- индикатор `labelCV` обновляется после скалярной проверки наличия резюме;
- HR-Мастер прижат к нижней границе;
- повторный вывод ФИО скрывается только стилем, component ID остаётся доступным контроллеру.

### Правая область

- workspace занимает всю оставшуюся ширину;
- «Еще» находится справа сверху;
- основные действия находятся справа снизу;
- вкладки сохраняют прежние ID и порядок;
- фон и границы одинаково определены для Halo, Hover, Havana и Helium.

### Accordion-заголовки

XML содержит контейнеры `job-candidate-accordion-header` и `job-candidate-accordion-content`. Accordion-слой не удаляет и не подменяет штатную навигацию `TabSheet`. Поведение полей, таблиц, loaders и действий не меняется.

### Вкладка «Основное»

- `personalDataBlock` и `professionalDataBlock` занимают по 50% доступной ширины;
- родительский `jobCandidateMainSectionContent` использует flex-компоновку;
- промежуток между блоками — 16 px;
- внутренний `GridLayout` растягивается на 100%;
- колонка подписей имеет ширину 118 px;
- поля `firstNameField`, `middleNameField`, `secondNameField`, `personPositionField` и `currentCompanyField` занимают всю доступную ширину;
- шрифт `SuggestionField` ФИО установлен 16 px;
- высота полей — 38 px.

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
| `fileImageFaceUpload` | прежняя загрузка фотографии |
| поля ФИО | прежние properties, search executors и listeners |
| `currentCompanyField` | прежние lookup/open/create действия |
| `jobCandidateIteractionListTable` | прежние actions, columns и handlers |
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers |
| `socialNetworkTable` | прежний editor и generators |
| комментарии | прежние поле ввода, отправка и ответы |

Stage 3 не меняет CRUD резюме, распознавание контактов, копирование CV, проверку навыков, загрузку файлов, связи CV с вакансией, CRUD социальных сетей и реализации генераторов `projectLogoColumn` и `socialNetworkLogoColumn`.

---

## 5. Стили и поддержка тем

Общий mixin:

```scss
@mixin job-candidate-editor-theme
```

Все правила ограничены `.job-candidate-editor`. Одинаковый файл `job-candidate-editor.scss` используется в темах Halo, Hover, Havana и Helium.

Глобальные `.v-table`, `.v-label`, `.v-button` и `.v-tabsheet` вне `.job-candidate-editor` не изменяются. Для рейтинга используются CSS Unicode escapes (`\2605`, `\2606`).

По результатам анализа клиентского рендеринга сохранены:

- локальные layout-классы вместо универсального selector всех потомков;
- явный список rating-классов;
- единый класс `job-candidate-sidebar-card`;
- упрощённые правила `job-candidate-form-grid`;
- минимально необходимый набор `!important` для конфликтов с inline-размерами Vaadin.

Stage 3 не содержит изменений SCSS.

---

## 6. Контроль качества и развертывание

Обязательные проверки Hermes:

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:test --tests "com.company.hunttech.web.screens.jobcandidate.JobCandidateCvInitialViewOptimizerTest" --no-daemon --stacktrace
./gradlew :app-web:test --tests "com.company.hunttech.web.screens.jobcandidate.JobCandidateSocialNetworkInitialViewOptimizerTest" --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

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
- в логах должны отсутствовать unfetched/detached-ошибки, `IllegalStateException`, `NullPointerException` Stage 3 и `OutOfMemoryError`;
- SQL-доказательство должно разделять initial open, первое открытие вкладки социальных сетей и повторное открытие;
- отчёт сохранить в `docs/performance-archive/2026-07-15/stage-3-social-networks-lazy/`.

## История изменений

| Дата | Изменение |
|---|---|
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
