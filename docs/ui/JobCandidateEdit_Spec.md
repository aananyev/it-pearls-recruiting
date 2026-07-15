# JobCandidateEdit — спецификация экрана HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

`JobCandidateEdit` — основная рабочая карточка кандидата в HRM HuntTech. Экран объединяет персональные и профессиональные сведения, контакты, позиции и вакансии, взаимодействия, резюме и файлы, социальные сети, комментарии и историю записи.

Критический путь открытия формы должен содержать только данные, необходимые рекрутеру для начала работы. Тяжёлые дочерние коллекции загружаются при первом обращении к соответствующей вкладке. Справочник компаний не загружается целиком, справочник должностей использует узкий picker-view, а последнее взаимодействие загружается только по запросу пользователя в copy-flow.

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
- Открытие формы → `interactionService.getLastIteraction()` не вызывается → middleware round-trip последнего взаимодействия исключён из critical path.
- Ввод в поле «Компания» → после двух символов выполняется ограниченный серверный поиск → отображается не более 50 компаний.
- Создание компании → `CompanyEdit` сохраняет запись → `JobCandidateEdit` точечно загружает созданную компанию по UUID через `company-picker-view`, merge-ит её в текущий `DataContext` и подставляет в поле.
- Загрузка должностей → `personPositionsLc` выполняет прежний JPQL → результаты загружаются через `position-picker-view`, содержащий только picker-поля.
- Выбор должности → `personPositionField` использует прежний `optionsContainer`, lookup и open → выбранная должность сохраняется вместе с кандидатом.
- Загрузка городов → `citiesDl` использует `city-picker-view` → до получения runtime baseline поведение и тип поля города не изменяются.
- Индикатор резюме → текущий `hasCandidateCv()` проверяет коллекцию `candidateCv` → Stage 10 обязан заменить чтение на скалярный запрос без материализации резюме.
- Первое открытие вкладки «Взаимодействия» → выполняется узкий запрос → строки merge-ятся в экранный `DataContext`.
- Копирование без выбранной строки → `ensureLastInteractionLoaded()` один раз получает последнее взаимодействие → повторный вызов использует кеш, включая кешированный `null`.
- Изменение взаимодействий → `reloadInteractions()` инвалидирует кеш последнего взаимодействия → следующее копирование получает актуальную запись.
- Первое открытие вкладки «Резюме» → выполняется отдельный запрос через `candidateCV-browse-view` → резюме отображаются без загрузки LOB-полей в initial view.
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

`JobCandidateInitialViewOptimizer` копирует runtime-view `jobCandidateDl` и исключает `iteractionList` из первичной загрузки. Коллекция загружается методом `ensureInteractionsLoaded()` при первом открытии вкладки взаимодействий.

### Этап 2 — резюме

`JobCandidateCvInitialViewOptimizer` исключает `candidateCv` из первичной загрузки. Полная коллекция загружается методом `ensureCandidateCvLoaded()` при первом открытии вкладки резюме.

Текущая реализация индикатора «Резюме: ДА/НЕТ» вызывает `hasCandidateCv()`, который обращается к `getEditedEntity().getCandidateCv()`. Это не соответствует lazy-contract и является предметом Stage 10. После Stage 10 индикатор должен использовать только скалярный `COUNT` по UUID кандидата.

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
- глобальное определение views.

### Stage 9 — ленивая загрузка последнего взаимодействия

Коммит `facbd44bffb68b0f681501bca3efb4ce5e09f2c3` удаляет синхронный вызов `InteractionService.getLastIteraction()` из `onBeforeShow()`.

Текущий контракт:

- `ensureLastInteractionLoaded()` вызывается только в `copyIteractionJobCandidate()` при отсутствии выбранной строки;
- новый кандидат не выполняет middleware-вызов;
- значение и `null` кешируются в пределах экземпляра экрана;
- ветка копирования выбранной строки не выполняет дополнительный запрос;
- `reloadInteractions()` инвалидирует кеш;
- `QUERY_GET_LAST_ITERACTION` и закомментированный legacy-метод удалены.

### Stage 10 — скалярная проверка наличия резюме

Stage 10 должен заменить чтение коллекции в `hasCandidateCv()` на запрос вида:

```jpql
select count(e.id)
from hunttech_CandidateCV e
where e.candidate.id = :candidateId
```

Параметр передаётся как UUID. Запрос не должен загружать `CandidateCV` entities, `textCV`, файлы или associations. `ensureCandidateCvLoaded()` и загрузка вкладки резюме не изменяются.

### Загрузка городов

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
- последнее взаимодействие загружается только для copy-flow без выбранной строки;
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

Stage 9 и Stage 10 не содержат изменений SCSS.

---

## 6. Контроль качества и развертывание

Обязательные проверки для Java-этапов:

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

`ScreenViewIntegrityTest`: 8/8 PASS.

Runtime-сценарии Stage 9:

- открыть существующего и нового кандидата без вызова `getLastIteraction`;
- выполнить Copy с выбранной строкой без дополнительного запроса;
- выполнить Copy без выбранной строки с переносом вакансии последнего взаимодействия;
- проверить диалог при отсутствии взаимодействий;
- после `reloadInteractions()` подтвердить загрузку актуальной последней записи;
- проверить отсутствие unfetched/detached, NPE и дублей;
- подтвердить HTTP 200 для `/hrm`.

Runtime-сценарии Stage 10:

- кандидат с CV показывает `Резюме: ДА`;
- кандидат без CV показывает `Резюме: НЕТ`;
- новый кандидат не выполняет CandidateCV-запрос;
- initial open выполняет только scalar COUNT и не загружает CandidateCV entities;
- вкладка «Резюме» загружает записи через `candidateCV-browse-view`;
- сохранение без открытия вкладки не удаляет резюме;
- отсутствует `Cannot get unfetched attribute [candidateCv]`;
- подтверждён HTTP 200.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-15 | Stage 9: `getLastIteraction` удалён из `onBeforeShow`, добавлены lazy-load, кеширование `null` и инвалидация после `reloadInteractions()`. |
| 2026-07-15 | Подготовлен Stage 10: зафиксирован текущий риск чтения unfetched `candidateCv` и контракт скалярной проверки индикатора резюме. |
| 2026-07-15 | Stage 6: `personPositionsDc` переведён с `position-view` (`_local`) на `position-picker-view` (`_minimal`); JPQL, optionsContainer, lookup/open и бизнес-поведение сохранены. |
| 2026-07-15 | Cleanup справочника компаний: удалены полный loader и compatibility-контейнер; create-company переведён на точечную загрузку по UUID. |
| 2026-07-15 | Stage 3 прогрессивной загрузки: `socialNetwork` исключён из первичной загрузки. |
| 2026-07-15 | Stage 2 прогрессивной загрузки: `candidateCv` исключён из primary view. |
| 2026-07-15 | Stage 1 прогрессивной загрузки: `iteractionList` исключён из primary view. |
| 2026-07-14 | Реализована двухпанельная компоновка JobCandidateEdit и локальный theme-aware SCSS. |
