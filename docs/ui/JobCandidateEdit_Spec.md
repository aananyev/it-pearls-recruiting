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

- Открытие кандидата → загружается основной view → слева показывается профиль, справа рабочие вкладки.
- Открытие таблицы резюме → для связанной вакансии загружается проект и `projectLogo` → колонка показывает логотип либо стандартную заглушку без обращения к unfetched-атрибуту detached-сущности.
- Первое открытие тяжёлой вкладки → устанавливаются обязательные параметры loaders → данные загружаются один раз.
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

Визуальная компоновка не меняет сущности, component ID, data bindings, actions, invoke, loaders и JPQL. Для устранения detached/unfetched ошибки `projectLogo` загружается через вложенный view в `job-candidate-edit.xml`: ссылка `projectName.projectLogo` указывается с `view="_local"` непосредственно в экранном view кандидата, без глобального override shared-entity view.

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
| `currentCompanyField` | suggestion, `picker_lookup`, `picker_open`, `createCompany` |
| `personPositionField` | `optionsContainer=personPositionsDc`, lookup и open |
| `jobCityCandidateField` | `optionsContainer=citiesDc`, lookup |
| `jobCandidateIteractionListTable` | прежние actions, columns и handlers |
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers; логотип проекта обеспечивается view |
| `socialNetworkTable` | прежний editor и generators |
| `fileImageFaceUpload` | immediate upload, clear и обновление изображения |

Не допускается менять component ID, dataContainer, property, invoke, action ID и бизнес-валидацию без отдельного разрешения.

---

## 5. Стили и поддержка тем

Все правила ограничены `.job-candidate-editor`. Используются только локальные style name с префиксом `job-candidate-`.

Глобальные `.v-table`, `.v-label`, `.v-button` и `.v-tabsheet` вне родительского класса не изменяются.

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

Acceptance gate:

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
