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

- Открытие существующего кандидата → runtime-view исключает `iteractionList`, `candidateCv` и `socialNetwork` → тяжёлые коллекции не материализуются в initial load.
- Открытие формы → до first paint показывается заглушка фотографии → проверка file storage выполняется отдельной фоновой задачей после `AfterShow`.
- Открытие формы → индикатор резюме получает нейтральное значение `Резюме: …` → после first paint единственный background scalar `COUNT` устанавливает `ДА` или `НЕТ`.
- Открытие формы → средний рейтинг не рассчитывается в `onBeforeShow` → после first paint отдельная фоновая задача выполняет scalar `AVG` и отображает звёзды.
- Ввод в поле «Компания» → после двух символов выполняется ограниченный серверный поиск → отображается не более 50 компаний.
- Создание компании → `CompanyEdit` сохраняет запись → созданная компания точечно загружается по UUID через `company-picker-view`, merge-ится в текущий `DataContext` и подставляется в поле.
- Первое открытие вкладки «Взаимодействия» → выполняется отдельный запрос → строки merge-ятся в экранный `DataContext`.
- Первое открытие вкладки «Резюме» → выполняется запрос через `candidateCV-browse-view` → после загрузки одним batch-запросом гидратируются проекты и логотипы.
- Первое открытие вкладки «Контакты» или «Социальные сети» → загружается `socialNetwork` → типы социальных сетей и логотипы гидратируются отдельным batch-запросом.
- Первое открытие вкладки «Позиции и вакансии» → запускается фоновая агрегация истории → loader’ы UI выполняются после установки обязательных параметров.
- Копирование взаимодействия без выбранной строки → последнее взаимодействие загружается лениво и кешируется → повторный сервисный запрос не выполняется до инвалидации.
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

Сущности, поля entity, миграции Liquibase, component ID, основные actions и бизнес-правила сохранения кандидата не изменяются в рамках performance-этапов.

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

Правила загрузки:

- сохранение кандидата без открытия ленивых вкладок не должно удалять существующие коллекции;
- коллекции после отдельной загрузки merge-ятся в текущий `DataContext`;
- повторное открытие вкладки не выполняет повторную полную загрузку;
- фоновые задачи передают UUID и скалярные DTO, но не экранные entity-графы;
- UI изменяется только на UI-потоке в `done()` или `handleException()`;
- hydration добавляет недостающие поля managed-сущностям и не изменяет связи;
- новый кандидат не выполняет CV `COUNT`, rating `AVG` и file-storage check.

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
│   └── HR-Мастер
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
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers |
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
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Acceptance gate:

- `ScreenViewIntegrityTest` — 8/8;
- итоговый build — `BUILD SUCCESSFUL`;
- существующий кандидат открывается без `unfetched` и detached ошибок;
- новый кандидат открывается без необязательных SQL-запросов;
- быстрый выход не вызывает UI-thread exception;
- сохранение без открытия ленивых вкладок не удаляет дочерние данные;
- hydration логотипов CV и социальных сетей работает после открытия соответствующих вкладок;
- `/hrm` отвечает HTTP 200.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-15 | Stage 14: удалён дублирующий CandidateCV `COUNT` из `JobCandidateCvInitialViewOptimizer`; единственный индикатор CV загружается фоновой задачей контроллера. |
| 2026-07-15 | Stage 13: индикатор «Резюме: ДА/НЕТ» перенесён в background scalar `COUNT` после first paint. |
| 2026-07-15 | Stage 12: проверка фотографии в file storage вынесена в фон. |
| 2026-07-15 | Stage 11: фоновый расчёт рейтинга через `BackgroundTask`; форматирование звёзд сохранено. |
| 2026-07-15 | Stage 10: чтение коллекции CV заменено scalar `COUNT`. |
| 2026-07-15 | Stage 9: последнее взаимодействие загружается лениво при фактическом использовании. |
| 2026-07-15 | Stage 6: `personPositionsDc` переведён на `position-picker-view`. |
| 2026-07-15 | Удалён полный loader компаний; create-company переведён на точечную загрузку по UUID. |
| 2026-07-15 | `socialNetwork`, `candidateCv` и `iteractionList` исключены из initial view. |
| 2026-07-14 | Реализована двухпанельная компоновка и локальный theme-aware SCSS. |