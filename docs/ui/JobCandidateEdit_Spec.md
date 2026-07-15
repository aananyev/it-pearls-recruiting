# JobCandidateEdit — спецификация экрана HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

`JobCandidateEdit` — основная рабочая карточка кандидата в HRM HuntTech. Экран объединяет персональные и профессиональные сведения, контакты, позиции и вакансии, взаимодействия, резюме и файлы, социальные сети, комментарии и историю записи.

Критический путь открытия формы должен содержать только данные, необходимые рекрутеру для начала работы. Тяжёлые дочерние коллекции загружаются при первом обращении к соответствующей вкладке. Справочник компаний не загружается целиком, а справочник должностей загружается через узкий picker-view.

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

На вкладке «Основное» рекрутер выбирает город, основную должность и компанию кандидата. Поле компании поддерживает suggestion, lookup, open и создание новой компании. Поля города и должности сохраняют существующие lookup-сценарии.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие кандидата → перед `InitEvent` из runtime-view исключаются `iteractionList`, `candidateCv` и `socialNetwork` → основная карточка открывается без предварительной материализации тяжёлых коллекций.
- Открытие формы → XML не содержит `currentCompaniesDc/currentCompaniesLc` → полный справочник компаний не загружается и не сериализуется.
- Ввод в поле «Компания» → после двух символов выполняется ограниченный серверный поиск → отображается не более 50 компаний.
- Создание компании → `CompanyEdit` сохраняет запись → `JobCandidateEdit` точечно загружает созданную компанию по UUID через `company-picker-view`, merge-ит её в текущий `DataContext` и подставляет в поле.
- Загрузка должностей → `personPositionsLc` выполняет прежний JPQL → результаты загружаются через `position-picker-view`, содержащий только picker-поля.
- Выбор должности → `personPositionField` использует прежний `optionsContainer`, lookup и open → выбранная должность сохраняется вместе с кандидатом.
- Загрузка городов → `citiesDl` использует `city-picker-view` → до получения runtime baseline поведение и тип поля города не изменяются.
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

### Cleanup справочника компаний

Из XML и Java удалены:

- `currentCompaniesDc`;
- `currentCompaniesLc`;
- временный `JobCandidateCompanyLoaderOptimizer`.

Create-company flow использует точечный запрос:

```jpql
select e
from hunttech_Company e
where e.id = :companyId
```

Запрос выполняется через `company-picker-view`. Результат merge-ится в текущий `DataContext`, поэтому несохранённые изменения кандидата не теряются.

### Stage 6 — узкий picker-view должностей

Коллекция должностей:

```xml
<collection id="personPositionsDc"
            class="com.company.hunttech.entity.Position">
    <view extends="position-picker-view"/>
    <loader id="personPositionsLc" cacheable="true">
        <query><![CDATA[
            select e from hunttech_Position e
            where e.positionRuName not like '%(не использовать)%'
            order by e.positionRuName
        ]]></query>
    </loader>
</collection>
```

`position-picker-view` наследует `_minimal` и содержит:

- `positionRuName`;
- `positionEnName`.

Ранее применявшийся `position-view` наследовал `_local` и загружал все локальные поля `Position`. Stage 6 уменьшает materialization одной коллекции без изменения её состава, фильтра, сортировки и пользовательских действий.

Не изменены:

- `personPositionField`;
- `optionsContainer="personPositionsDc"`;
- property `personPosition`;
- lookup и open;
- JPQL;
- `cacheable="true"`;
- Java-контроллер;
- глобальное определение views.

### Следующий этап — baseline загрузки городов

`citiesDl` по-прежнему выполняет:

```jpql
select e
from hunttech_City e
order by e.cityRuName
```

через `city-picker-view`.

До измерения количества строк, SQL P50/P95, runtime времени loader и пользовательской задержки запрещено:

- менять `lookupPickerField` на suggestion-компонент;
- откладывать loader до focus/click;
- менять JPQL;
- менять `city-picker-view`;
- добавлять индексы или Liquibase.

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
| `personPositionsDc` | действующие должности через `position-picker-view` |
| `citiesDc` | города через `city-picker-view` |

Правила загрузки:

- `iteractionList` загружается при первом открытии `tabIteraction`;
- `candidateCv` загружается при первом открытии `tabResume`;
- `socialNetwork` загружается при первом открытии `tabContactInfo` или `tabSocialNetworks`;
- `currentCompanyField` выполняет ограниченный серверный поиск только после пользовательского ввода;
- создание компании выполняет точечную загрузку по UUID;
- должности загружаются узким picker-view;
- города остаются без функциональных изменений до baseline;
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

- `personalDataBlock` и `professionalDataBlock` занимают равные доли;
- внутренние `GridLayout` растягиваются на 100%;
- поля ФИО, должности и компании занимают всю доступную ширину;
- высота основных полей — 38 px;
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

---

## 5. Стили и поддержка тем

Все правила ограничены `.job-candidate-editor`. Одинаковый локальный SCSS используется в темах Halo, Hover, Havana и Helium. Глобальные `.v-table`, `.v-label`, `.v-button` и `.v-tabsheet` вне родительского класса не изменяются.

Stage 6 не содержит изменений SCSS.

---

## 6. Контроль качества и развертывание

Проверки Stage 6:

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Runtime-сценарии:

- открыть существующего кандидата;
- раскрыть список должностей;
- убедиться, что записи «(не использовать)» отсутствуют;
- выбрать должность через dropdown;
- выбрать должность через lookup;
- открыть редактор выбранной должности;
- сохранить кандидата и повторно открыть карточку;
- создать нового кандидата;
- проверить отсутствие unfetched/detached ошибок `Position`;
- подтвердить HTTP 200 для `/hrm`.

Подтверждено пользователем и Hermes:

- `position-picker-view` установлен;
- `position-view` удалён из `personPositionsDc`;
- `optionsContainer` сохранён;
- коммит `7f25633a3c96971dbf923d0f51f77424068ddc33` запушен;
- приложение отвечает HTTP 200;
- `VERIFICATION: PASS (5/5)`.

## История изменений

| Дата | Изменение |
|---|---|---|
| 2026-07-15 | Stage 12: проверка фото в file storage вынесена в фон; first paint с заглушкой, фото появляется после AfterShow. |
| 2026-07-15 | Stage 11: фоновый расчёт рейтинга через BackgroundTask; AVG не блокирует first paint, UI-форматирование звёзд сохранено. |
| 2026-07-15 | Stage 10: `hasCandidateCv()` заменён на скалярный `count`-запрос; коллекция CV не материализуется ради индикатора «Резюме: ДА/НЕТ». |
| 2026-07-15 | Stage 9: ленивая загрузка последнего взаимодействия; вызов `InteractionService.getLastIteraction()` удалён из `onBeforeShow`, перенесён в `ensureLastInteractionLoaded()` с кешированием и инвалидацией в `reloadInteractions()`. |
| 2026-07-15 | Stage 6: `personPositionsDc` переведён с `position-view` (`_local`) на `position-picker-view` (`_minimal`); JPQL, optionsContainer, lookup/open и бизнес-поведение сохранены. |
| 2026-07-15 | Cleanup справочника компаний: удалены полный loader и compatibility-контейнер; create-company переведён на точечную загрузку по UUID. |
| 2026-07-15 | Stage 3 прогрессивной загрузки: `socialNetwork` исключён из первичной загрузки. |
| 2026-07-15 | Stage 2 прогрессивной загрузки: `candidateCv` исключён из primary view. |
| 2026-07-15 | Stage 1 прогрессивной загрузки: `iteractionList` исключён из primary view. |
| 2026-07-14 | Реализована двухпанельная компоновка JobCandidateEdit и локальный theme-aware SCSS. |
