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
- `tabSocialNetworks` — социальные сети;
- `commentsTab` — комментарии;
- `tabHistory` — история.

Кнопка `openPositionMasterBrowseButton` открывает HR-Мастер для текущего кандидата. Кнопка `moreActionsPopUpButton` содержит существующие действия блокировки и подписки.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие кандидата → загружается основной view → слева показывается профиль, справа рабочие вкладки.
- Открытие таблицы резюме → для связанной вакансии загружается проект и `projectLogo` → колонка показывает логотип либо стандартную заглушку без обращения к unfetched-атрибуту detached-сущности.
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

Визуальная компоновка не меняет сущности, component ID, data bindings, actions, invoke, loaders и JPQL. Для устранения detached/unfetched ошибки `projectLogo` загружается через вложенный view в `job-candidate-edit.xml`: ссылка `projectName.projectLogo` указывается с `view="_local"` непосредственно в экранном view кандидата, без глобального override shared-entity view.

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
│   ├── рейтинг и процент заполнения
│   ├── город / компания / резюме
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
- фотография отображается единым `OvaFallbackImage` размером 176×176 и не искажается;
- при отсутствии фотографии или бинарного файла показывается `icons/no-programmer.jpeg`;
- ФИО в формате `Фамилия Имя` и должность видны непосредственно под фотографией;
- ФИО и должность обновляются сразу при изменении полей и не зависят от lazy-создания вкладки «Основное»;
- ФИО центрировано и использует типографику `candidateRatingLabel`, а остальные sidebar labels уменьшены на один пункт;
- процент заполнения рассчитывается по 15 свойствам `jobCandidateDc` без открытия lazy-вкладок и дополнительных запросов;
- текст карточек имеет размер 16 px и переносится по словам;
- HR-Мастер прижат к нижней границе;
- заблокированный кандидат сохраняет профильное позиционирование ФИО, меняется только цвет.

### Контракт профильного блока

`fullNameField` и `personPositionLabel` являются presentation labels без прямой XML-привязки.
Контроллер заполняет их после показа экрана и синхронизирует по
`InstanceContainer.ItemChangeEvent`/`ItemPropertyChangeEvent` контейнера `jobCandidateDc`.
Поэтому sidebar получает данные даже до создания полей внутри lazy-вкладки.

ФИО формируется из `secondName` и `firstName` через общий null-safe formatter. Результат
одновременно записывается в `JobCandidate.fullName` и выводится под фотографией. Должность
берётся из `personPosition.positionRuName`; отсутствие должности даёт пустую подпись.

При изменении состояния блокировки контроллер сначала восстанавливает постоянный класс
`job-candidate-profile-name`, затем добавляет `h2` либо `h2-red`. Такой порядок обязателен:
`setStyleName()` заменяет все текущие CSS-классы компонента.

SCSS-контракт повторён для `halo`, `havana`, `helium`, `hover`, `hunttech-modern`,
`hunttech-modern-light` и `hunttech-modern-dark`. Должность должна оставаться `display: block`.
Подробное описание lifecycle и пользовательские сценарии приведены в
[`docs/screens/job-candidate/JobCandidateEdit_Spec.md`](../screens/job-candidate/JobCandidateEdit_Spec.md)
и [`job-candidate-edit.md`](../screens/job-candidate/job-candidate-edit.md).

### Правая область

- workspace занимает всю оставшуюся ширину;
- «Еще» находится справа сверху;
- основные действия находятся справа снизу;
- вкладки сохраняют прежние ID и порядок;
- фон и границы одинаково определены для Halo, Hover, Havana и Helium.

### Accordion-заголовки

XML содержит контейнеры `job-candidate-accordion-header` и `job-candidate-accordion-content`. Заголовок раздела отображается над содержимым, слева выводится маркер раскрытого состояния, а заголовок и содержимое образуют единую карточку.

Accordion-слой не удаляет и не подменяет штатную навигацию `TabSheet`. Поведение полей, таблиц, loaders и действий не меняется.

### Вкладка «Основное»

- `personalDataBlock` и `professionalDataBlock` занимают по 50% доступной ширины;
- родительский `jobCandidateMainSectionContent` использует flex-компоновку;
- промежуток между блоками — 16 px;
- внутренний `GridLayout` принудительно растягивается на 100%;
- колонка подписей имеет ширину 118 px, поэтому поля расположены близко к labels;
- slot-компоненты `GridLayout` растягиваются единым локальным правилом без отдельных селекторов второй колонки;
- поля `firstNameField`, `middleNameField`, `secondNameField`, `personPositionField` и `currentCompanyField` занимают всю доступную ширину;
- шрифт `SuggestionField` ФИО установлен 16 px — такой же, как у `jobCityCandidateField` и остальных полей;
- высота полей — 38 px;
- component ID, properties, actions, queries и search executors не изменяются.

### Вкладка «Контакты»

- основные и дополнительные контакты занимают равные доли;
- подписи имеют фиксированную ширину;
- поля занимают оставшееся пространство;
- `radioButtonGroup` сохраняет прежнюю бизнес-логику и привязку `priorityContact`.

### Вкладка «Резюме и файлы»

- `jobCandidateCandidateCvTable` сохраняет существующий dataContainer и генераторы колонок;
- колонка логотипа проекта читает уже загруженный `Project.projectLogo`;
- при отсутствии логотипа отображается `icons/no-company.png`;
- открытие вкладки не должно приводить к `Cannot get unfetched attribute [projectLogo]`;
- Java-генератор и `FileDescriptorImageHelper` не выполняют дополнительную перезагрузку проекта.

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
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers; логотип проекта обеспечивается view |
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

Одинаковый файл `job-candidate-editor.scss` используется в темах:

- Halo;
- Hover;
- Havana;
- Helium.

Глобальные `.v-table`, `.v-label`, `.v-button` и `.v-tabsheet` вне `.job-candidate-editor` не изменяются.

Для рейтинга используются CSS Unicode escapes (`\2605`, `\2606`), чтобы звёзды не повреждались при сборке SCSS.

### Оптимизация локального SCSS

По результатам завершённого анализа клиентского рендеринга сохранён безопасный рефакторинг локального визуального слоя:

- универсальное правило для всех потомков `.job-candidate-editor` заменено перечнем локальных layout-классов;
- substring selector рейтинга заменён явным списком rating-классов;
- правила sidebar сведены к `job-candidate-sidebar-card`;
- для `job-candidate-form-grid` удалены селекторы `td:nth-child(2)` и глубокие цепочки до внутренних полей;
- slot-компоненты основной формы растягиваются единым локальным правилом;
- повторяющиеся комбинации `width/min-width/max-width` сокращены там, где геометрия уже задаётся XML или flex layout;
- критические `!important` сохранены только для inline-размеров Vaadin и конфликтующих правил тем.

Временные performance-пробы, JFR-сборщики и performance-тесты после завершения анализа удалены. Штатный lifecycle экрана не зависит от диагностического system property.

---

## 6. Контроль качества и развертывание

Обязательные команды:

```bash
git diff --check
./gradlew :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:test --tests '*JobCandidateProjectLogoViewContractTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ручная проверка:

- HTTP 200 для `/hrm`;
- открытие существующего и нового кандидата;
- открытие вкладки «Резюме и файлы» у кандидата с резюме, вакансией и проектом;
- отображение реального логотипа проекта и стандартной заглушки при его отсутствии;
- отсутствие `Cannot get unfetched attribute [projectLogo]` и detached exceptions в журнале;
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
| 2026-07-22 | Глобальный overwrite `openPosition-edit-view` удалён. `projectLogo` загружается через локальный вложенный view `job-candidate-edit.xml`. Исходный shared view восстановлен. |
| 2026-07-22 | (SUPERSEDED) `openPosition-edit-view` дополнен графом `projectName.projectLogo` через глобальный overwrite; заменён локальным экранным view. |
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
