# JobCandidateEdit — спецификация экрана HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

`JobCandidateEdit` — основная рабочая карточка кандидата в HRM HuntTech. Экран объединяет персональные и профессиональные сведения, контакты, позиции и вакансии, взаимодействия, резюме и файлы, социальные сети, комментарии и историю записи.

Критический путь открытия формы должен содержать только данные, необходимые рекрутеру для начала работы. Тяжёлые дочерние коллекции загружаются при первом обращении к соответствующей вкладке. Справочник компаний не загружается целиком: поле компании использует ограниченный серверный поиск.

### 2. Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается:

- из `JobCandidateBrowse` при создании или редактировании кандидата;
- из экранов подбора кандидатов;
- из связанных рекрутинговых сценариев и lookup-компонентов.

Основная навигация выполняется через существующий `tabSheetSocialNetworks`:

- `tabMain` — основные данные;
- `tabContactInfo` — контакты;
- `tabPositions` — позиции и вакансии;
- `tabIteraction` — взаимодействия;
- `tabResume` — резюме и файлы;
- `tabSocialNetworks` — социальные сети;
- `commentsTab` — комментарии;
- `tabHistory` — история.

Поле `currentCompanyField` позволяет найти существующую компанию, открыть её карточку, выбрать компанию через lookup или создать новую компанию в `CompanyEdit`.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие кандидата → перед `InitEvent` из runtime-view исключаются `iteractionList`, `candidateCv` и `socialNetwork` → основная карточка открывается без предварительной материализации тяжёлых коллекций.
- Открытие формы → XML не содержит `currentCompaniesDc/currentCompaniesLc` → полный справочник компаний не загружается и не сериализуется в initial open.
- Ввод в поле «Компания» → после двух символов выполняется ограниченный серверный поиск → отображается не более 50 компаний.
- Выбор через suggestion или lookup → выбранная `Company` устанавливается в `currentCompanyField` → связь сохраняется вместе с кандидатом.
- Открытие выбранной компании → action `picker_open` открывает `CompanyEdit` → после закрытия выбранное значение сохраняется.
- Создание компании → `CompanyEdit` сохраняет новую запись → `JobCandidateEdit` точечно загружает только созданную компанию по ID через `company-picker-view`, merge-ит её в текущий `DataContext` и подставляет в поле.
- Отмена создания компании → SQL и merge не выполняются → прежнее значение поля не меняется, действие создания снова доступно.
- Первое открытие вкладки «Взаимодействия» → выполняется узкий запрос → строки merge-ятся в экранный `DataContext`.
- Первое открытие вкладки «Резюме» → выполняется отдельный запрос через `candidateCV-browse-view` → резюме отображаются без загрузки LOB-полей в initial open.
- Первое открытие вкладки «Контакты» или «Социальные сети» → коллекция `socialNetwork` загружается отдельным запросом → повторная полная загрузка не выполняется.
- «Сохранить и закрыть» → выполняется `windowCommitAndClose` → кандидат сохраняется и экран закрывается.
- «Отмена» → выполняется `windowClose` → применяется стандартный сценарий закрытия CUBA.

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

Сущности, поля entity, миграции Liquibase, component ID, основные actions и бизнес-правила сохранения кандидата не изменяются.

### Этап 1 — взаимодействия

`JobCandidateInitialViewOptimizer` копирует runtime-view `jobCandidateDl` и исключает `iteractionList` из первичной загрузки. Коллекция загружается существующим методом `ensureInteractionsLoaded()` при первом открытии вкладки взаимодействий.

### Этап 2 — резюме

`JobCandidateCvInitialViewOptimizer` исключает `candidateCv` из первичной загрузки. Наличие резюме определяется скалярным `COUNT`, а коллекция загружается методом `ensureCandidateCvLoaded()` при первом открытии вкладки резюме.

После загрузки CV проекты связанных вакансий догружаются одним batch-запросом через `project-browse-view`, чтобы генератор логотипа не обращался к unfetched `projectLogo`.

### Этап 3 — социальные сети

`JobCandidateSocialNetworkInitialViewOptimizer` исключает `socialNetwork` из первичной загрузки. Коллекция загружается при первом открытии вкладки контактов или социальных сетей. Уникальные `SocialNetworkType` догружаются одним batch-запросом через `socialNetworkType-view`.

### Оптимизация справочника компаний — cleanup Stage 3

Из XML и Java удалены:

- `currentCompaniesDc`;
- `currentCompaniesLc`;
- временный `JobCandidateCompanyLoaderOptimizer`.

Полный запрос справочника больше не существует в экране:

```jpql
select e
from hunttech_Company e
order by e.comanyName
```

Create-company flow использует только одну запись:

```jpql
select e
from hunttech_Company e
where e.id = :companyId
```

Запрос выполняется через `company-picker-view`. Результат merge-ится в текущий `DataContext`, поэтому несохранённые изменения кандидата не теряются.

### Следующий этап — baseline suggestion-поиска компаний

До изменения поведения необходимо измерить текущий поиск:

```jpql
select e
from hunttech_Company e
where lower(e.comanyName) like lower(:searchString)
order by e.comanyName, e.companyShortName
```

Текущие параметры:

| Параметр | Значение |
|---|---|
| Минимальная длина | 2 символа |
| Лимит | 50 |
| Задержка | 300 мс |
| Семантика | contains: `%строка%` |
| View | `company-picker-view` |

На baseline-этапе запрещено менять JPQL, параметры поля, индексы или расширения PostgreSQL. Реализация допускается только после сравнения текущего contains-поиска с prefix-поиском, увеличением минимальной длины и возможным trigram-индексом.

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

`currentCompaniesDc/currentCompaniesLc` отсутствуют.

Правила загрузки:

- `iteractionList` загружается при первом открытии `tabIteraction`;
- `candidateCv` загружается при первом открытии `tabResume`;
- `socialNetwork` загружается при первом открытии `tabContactInfo` или `tabSocialNetworks`;
- логотипы проектов и типов социальных сетей догружаются batch-запросами после загрузки соответствующих строк;
- `currentCompanyField` выполняет ограниченный серверный поиск только после пользовательского ввода;
- создание компании выполняет точечную загрузку по UUID;
- сохранение кандидата без открытия ленивых вкладок не должно удалять существующие коллекции;
- повторное открытие вкладок не должно создавать дубли или повторные полные запросы.

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
    ├── accordion-заголовки разделов
    └── нижняя панель: «Сохранить и закрыть», «Отмена»
```

### Левая панель

- ширина — 312 px, на экранах до 1366 px — 286 px;
- фотография отображается круглой без искажения пропорций;
- ФИО имеет размер 24 px;
- текст карточек имеет размер 16 px и переносится по словам;
- HR-Мастер прижат к нижней границе.

### Правая область

- workspace занимает всю оставшуюся ширину;
- «Еще» находится справа сверху;
- основные действия находятся справа снизу;
- вкладки сохраняют прежние ID и порядок;
- оформление поддерживается в Halo, Hover, Havana и Helium.

### Вкладка «Основное»

- `personalDataBlock` и `professionalDataBlock` занимают равные доли;
- внутренние `GridLayout` растягиваются на 100%;
- поля ФИО, должности и компании занимают всю доступную ширину;
- высота полей — 38 px.

### Вкладка «Контакты»

- основные и дополнительные контакты занимают равные доли;
- подписи имеют фиксированную ширину;
- поля занимают оставшееся пространство;
- `radioButtonGroup` сохраняет привязку `priorityContact`.

---

## 4. Actions и неизменяемые контракты

| Компонент | Контракт |
|---|---|
| `windowCommitAndCloseButton` | action `windowCommitAndClose` |
| кнопка отмены | action `windowClose` |
| `moreActionsPopUpButton` | прежний popup и handlers |
| `fileImageFaceUpload` | прежняя загрузка фотографии |
| поля ФИО | прежние properties, search executors и listeners |
| `currentCompanyField` | suggestion, `picker_lookup`, `picker_open`, `createCompany` |
| `jobCandidateIteractionListTable` | прежние actions, columns и handlers |
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers |
| `socialNetworkTable` | прежний editor и generators |
| комментарии | прежние поле ввода, отправка и ответы |

Оптимизация компаний не меняет CRUD резюме и социальных сетей, распознавание контактов, копирование CV, проверку навыков, загрузку файлов и связи CV с вакансией.

---

## 5. Стили и поддержка тем

Общий mixin:

```scss
@mixin job-candidate-editor-theme
```

Все правила ограничены `.job-candidate-editor`. Одинаковый локальный SCSS используется в темах Halo, Hover, Havana и Helium. Глобальные `.v-table`, `.v-label`, `.v-button` и `.v-tabsheet` вне родительского класса не изменяются.

Cleanup Stage 3 и baseline suggestion-поиска не содержат изменений SCSS.

---

## 6. Контроль качества и развертывание

Проверки cleanup Stage 3:

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:test --tests "com.company.hunttech.web.screens.jobcandidate.JobCandidateCreatedCompanyResolverTest" --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Подтверждено:

- `JobCandidateCreatedCompanyResolverTest`: 3/3 PASS;
- `ScreenViewIntegrityTest`: BUILD SUCCESSFUL;
- `clean assemble`: BUILD SUCCESSFUL;
- `/hrm`: HTTP 200;
- ручные сценарии suggestion, lookup, open, create и повторного открытия кандидата пройдены.

Критерии следующего baseline-этапа:

- один прогревочный и не менее пяти измерительных запусков каждого поискового выражения;
- MIN, MAX, AVG, P50 и P95 для SQL и пользовательской задержки;
- количество просмотренных и возвращённых строк;
- `EXPLAIN (ANALYZE, BUFFERS)` текущего contains-поиска;
- read-only сравнение с prefix-поиском и вводом от трёх символов;
- проверка существующих индексов и доступности `pg_trgm` без создания объектов БД;
- отдельный вывод о влиянии `suggestionsLimit=50`;
- отсутствие изменений production и бизнес-логики.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-15 | Cleanup Stage 3 справочника компаний: удалены `currentCompaniesDc/currentCompaniesLc` и временный optimizer; create-company переведён на точечную загрузку `Company` по UUID через `company-picker-view`; ручные тесты пройдены. |
| 2026-07-15 | Stage 3 прогрессивной загрузки: `socialNetwork` исключён из runtime-view первичной загрузки; коллекция загружается при первом открытии вкладки контактов или социальных сетей. |
| 2026-07-15 | Исправлена загрузка логотипов `SocialNetworkType` и `Project` batch-запросами без unfetched-ошибок. |
| 2026-07-15 | Stage 2 прогрессивной загрузки: `candidateCv` исключён из primary view; наличие резюме определяется скалярным `COUNT`. |
| 2026-07-15 | Stage 1 прогрессивной загрузки: `iteractionList` исключён из primary view и загружается при первом открытии вкладки. |
| 2026-07-15 | Завершено диагностическое performance-тестирование и удалены временные runtime-пробы. |
| 2026-07-14 | Реализована двухпанельная компоновка JobCandidateEdit и локальный theme-aware SCSS. |
