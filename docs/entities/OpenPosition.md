# OpenPosition — открытая вакансия

> Транзакционная сущность вакансий; центральный узел рекрутингового процесса.
> Оптимизация: 2026-06-23.
> Архитектурная спецификация: [OpenPosition_Spec.md](../architecture/OpenPosition_Spec.md)
> UI Spec: [browse](../ui/itpearls_OpenPosition.browse_Spec.md), [edit](../ui/itpearls_OpenPosition.edit_Spec.md), [detail fragment](../ui/itpearls_OpenPositionDetailScreenFragment_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

`OpenPosition` — транзакционная сущность открытой вакансии в HRM HuntTech: название и внешний ID (`vacansyName`, `vacansyID`), статус открыта/закрыта (`openClose`), приоритет согласования, черновик (`signDraft`), пауза, рейтинг, иерархия через `parentOpenPosition`. Вакансия описывает требования к кандидату (грейд, опыт, удалёнка, вилка зарплаты, тип должности `positionType`, проект `projectName`, города), LOB-тексты (описание RU/EN, шаблон письма, тестовое, памятка к интервью), навыки (`skillsList`), подписки рекрутёров (`RecrutiesTasks`), комментарии и файлы. Центральный узел рекрутингового процесса: к вакансии привязываются `IteractionList` и подбор кандидатов.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Главный browse — `itpearls_OpenPosition.browse` (меню, дерево `treeDataGrid`). Edit — `itpearls_OpenPosition.edit` с вкладками (основное, навыки, labor agreement, файлы, комментарии). Дополнительные browse: `OpenPositionRecruiting`, `OpenPositionOutstaff`, `ProdOpenPosition`, `OpenPositionMaster`. Фрагмент деталей строки — `itpearls_OpenPositionDetailScreenFragment`. Связанные сущности: `Project`, `Position`, `Grade`, `City`, `OpenPositionComment`, `OpenPositionNews`, `RecrutiesTasks`, `JobCandidate` (через взаимодействия и suggest). Архитектурная спецификация: [OpenPosition_Spec.md](../architecture/OpenPosition_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

**Browse:** при открытии — фильтры «только открытые» и «только моя подписка»; пакетная подготовка данных для колонок; раскрытие строки с фрагментом и кнопками; закрытие вакансии может массово завершить взаимодействия с кандидатами «на рассмотрении».

**Edit:** много вкладок с ленивой загрузкой LOB и коллекций; автогенерация имени вакансии; проверка дубликатов и vacansyID; shortDescription ≤ 250 символов; уведомления и Telegram при открытии/закрытии.

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.itpearls.entity.OpenPosition` |
| **Имя в CUBA** | `itpearls_OpenPosition` |
| **Таблица БД** | `ITPEARLS_OPEN_POSITION` |
| **Тип данных** | транзакционная |
| **Критичность** | высокая |

### LOB-поля

| Поле | Стратегия |
|------|-----------|
| `comment`, `commentEn` | lazy reload на вкладке Edit (основная вкладка) |
| `templateLetter`, `exercise`, `memoForInterview` | lazy reload по вкладкам |

---

## 4. Представления (views.xml)

| View | Назначение |
|------|------------|
| `openPosition-browse-view` | Browse без LOB, узкие FK; `positionType` с явными `positionRuName`/`positionEnName`; `projectName` включает `projectOwner` (`person-owner-view`) для колонок/рендереров |
| `openPosition-edit-view` | Edit без LOB и коллекций; `positionType` → `position-picker-view`; `projectName` → `project-edit-view`; `parentOpenPosition` → `openPosition-parent-picker-view` |
| `openPosition-parent-picker-view` | parent lookup с `projectName` |
| `laborAgreement-openPosition-tab-view` | вкладка Labor Agreement в Edit |
| `laborAgeementType-picker-view` | display `nameAgreement` в гриде |
| `openPosition-picker-view` | lookup / FK |
| `openPosition-rtasks-browse-view` | FK в RecrutiesTasks browse |
| `openPosition-rtasks-picker-view` | picker в RecrutiesTasks edit |
| `openPosition-iteraction-list-picker-view` | FK vacancy в IteractionList |
| `openPosition-view` | legacy |

---

## 5. Экраны

| Экран | View |
|-------|------|
| OpenPositionBrowse | `openPosition-browse-view` + batch exists-кэши LOB + lazy load текста |
| OpenPositionEdit | `openPosition-edit-view` + lazy вкладки |

### Поведение экранов (из Java)

#### OpenPositionBrowse

| Момент | Цепочка |
|--------|---------|
| Открытие | Фильтры opened + mySubscribe; срочные вакансии; Excel — только Manager |
| После load | Batch-кэши LOB exists, агрегаты рекрутеров/CV/рейтинга |
| Закрыть вакансию | → диалог кандидатов на рассмотрении → batch end-case → commit → уведомления |
| Смена приоритета Low | → диалог недели + closingDate |

#### OpenPositionEdit

| Момент | Цепочка |
|--------|---------|
| Вкладки | Lazy LOB/collections при первом выборе |
| Сохранение | sync skills + laborAgreement; дубликат имени/vacansyID; shortDescription ≤ 250 |
| После save | OpenPositionNews; Telegram (ошибка не блокирует) |

### Java-оптимизации

- **OpenPositionBrowse:** `removeCandidatesWithConsideration` — batch `CommitContext` для `IteractionList` при закрытии вакансии (один `dataManager.commit` вместо N×`commit(jc)`)
- **OpenPositionBrowse:** `refreshBrowseLobExistsCaches` — batch `exists` для comment/exercise/memo/templateLetter/project/company descriptions (без загрузки LOB-текста); `positionEnName`/`positionRuName` — light strings; lazy load полного текста в `descriptionProvider`/tooltip и при «Описание»/details
- **OpenPositionBrowse:** `refreshBrowseAggregateCaches` — activeRecruitersCountByPosition (`count(e.reacrutier)`), sentCvCountByPosition, avgRatingByPosition (batch в PostLoad; rowStyleProvider, lastCVSend, rating)
- **OpenPositionEdit:** lazy LOB/collections по `tabSheetOpenPosition`; вкладки Skills, Labor Agreement, Files, Comments — standalone `CollectionLoader` с `:openPosition` в JPQL `<condition>` + `PreLoadEvent.preventLoad()` до `setParameter`; rescан навыков — `openPositionSkillsListsDc` / `skillTrees`, guard `screenFullyLoaded`, sync в entity только в `onBeforeCommitChanges`; `syncLaborAgreementToEntity` — skip если вкладка не открыта, `ensureLaborAgreementLoadedOnEntity` (reload + merge) перед `setLaborAgreement`; Telegram-уведомление при commit — не блокирует сохранение при ошибке API; таймер закрытия вакансии (`closedVacancyTimer`) — 60 с, `autostart=false`, старт/стоп в `initClosedVacancyTimerFacet` при `closingDate` (AfterShow + ValueChange); дубликат `vacansyID` — `loadValue(count)` с `=` в `onBeforeCommitChanges`, при edit — `e.id <> :currentId`

---

## 7. Производительность

| Экран | Метрика | Было | Стало | Δ | Комментарий |
|-------|---------|------|-------|---|-------------|
| OpenPositionBrowse | View | `openPosition-view` + inline `_local` | `openPosition-browse-view` | — | убраны LOB из SELECT |
| OpenPositionBrowse | LOB в основном SELECT | 5+ полей | 0 | −5 LOB | browse-view без LOB |
| OpenPositionBrowse | SQL batch PostLoad (exists) | 6+ full LOB | 4 exists/страница | −LOB TOAST | comment/exercise/memo/template + project/company exists |
| OpenPositionBrowse | SQL tooltip/hover | batch LOB | 1×id при hover | −(N−k) | lazy text cache per field |
| OpenPositionBrowse | SQL close vacancy (consideration) | N×commit(JobCandidate) | 1×CommitContext | −(N−1) commit | только новые IteractionList, FK candidate/vacancy |
| OpenPositionBrowse | SQL на строку (rowStyle, lastCVSend, rating) | 3×N | 3 batch/страница | −(3N−3) | aggregate-кэши в PostLoad |
| OpenPositionBrowse | SQL на строку (subscribers, stats, folder children) | до 4×N | 3 batch/страница | −(4N−3) | `subscribersByPosition`, `interactionStats*Cache`, `positionsWithChildren` |
| OpenPositionBrowse | Loader page size | default | `maxResults=40` | cap rows | `openPositionsDl` |
| OpenPositionEdit | LOB при открытии | все сразу | main tab + lazy | −4 LOB | вкладки exercise/template/… |

---

## 9. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | OpenPositionBrowse: `maxResults=40`; batch subscribers (`QUERY_SUBSCRIBERS_BY_POSITIONS` + `SUBSCRIBERS_TASKS_VIEW`), interaction stats (`QUERY_COUNT_ITERACTIONS_BY_POSITIONS`), parent-folder (`QUERY_CHILD_POSITIONS_BY_PARENTS`); `fetch="BATCH"` в browse XML |
| 2026-06-26 | Deep modernization: поведение browse/edit простым языком; Behavior Summary переписан |
| 2026-06-26 | Добавлен Business & Context Intro (Living Documentation standard) |
| 2026-06-23 | OpenPositionBrowse: `removeCandidatesWithConsideration` — batch `CommitContext` для `IteractionList` при закрытии вакансии (один commit вместо N×`commit(jc)`) |
| 2026-06-23 | OpenPositionEdit: `closedVacancyTimer` — интервал 60 с, `autostart=false`; `initClosedVacancyTimerFacet` на AfterShow и смене `closingDate`; таймер стартует только при заданной дате закрытия |
| 2026-06-23 | OpenPositionBrowse: 3-уровневая загрузка — batch exists-флаги LOB (comment/exercise/memo/templateLetter/project/company) вместо полного текста в PostLoad; lazy load текста в tooltip/descriptionProvider и `loadOpenPositionWithDescriptionLobs` при «Описание»; aggregate-кэши исправлены (`.properties("openPosition"/"vacancy")`, параметр `positions`) |
| 2026-06-23 | OpenPositionBrowse: batch-кэши `activeRecruitersCountByPosition` (count reacrutier), `sentCvCountByPosition`, `avgRatingByPosition` в PostLoad — устранён N+1 в rowStyleProvider, lastCVSend, rating |
| 2026-06-23 | OpenPositionBrowse: проверка `templateLetter` через `StringUtils.isNotBlank` вместо `!= ""` (`getQueryQuestion`, `getTemplateLetter`) |
| 2026-06-23 | OpenPositionEdit: commit не блокируется ошибкой Telegram API (HTTP 400 и др.) — `TelegramServiceBean` пропускает send при выключенном боте/пустых token/chat_id, логирует warning без токена; `notifyTelegramOpenPositionChange` показывает предупреждение пользователю |
| 2026-06-23 | OpenPositionEdit: `syncLaborAgreementToEntity` — skip если вкладка Labor Agreement не открыта; `ensureLaborAgreementLoadedOnEntity` (reload + `dataContext.merge`) перед `setLaborAgreement` — исправлен `Cannot get unfetched attribute [laborAgreement]` при OK/close |
| 2026-06-23 | Стартовая ошибка `Unable to read class: %sjava.lang.Object` — **не связана** с OpenPositionEdit; причина Tomcat на Java 17/22 вместо 11 (`UiControllerResourceMeta.traverseForRoute`); исправлено принудительным `JAVA_HOME` в `etc/tomcat-setenv.sh` |
| 2026-06-23 | OpenPositionEdit: lazy tab loaders (`laborAgreementDl`, `commentsOpenPositionDl`, `someFilesesDl`, `openPositionSkillsListsDl`, `procAttachmentsDl`) — `:openPosition`/`:entityId` в JPQL `<condition>` (не в основном WHERE); `PreLoadEvent.preventLoad()` при отсутствии параметра — исправлен `IllegalStateException` при открытии из Browse (`@LoadDataBeforeShow`) |
| 2026-06-23 | OpenPositionEdit: `rescanJobDescription` — убран `setSkillsList` на detached entity; rescан только через `skillTrees` + `openPositionSkillsListsDc`; guard `screenFullyLoaded` в `onOpenPositionRichTextAreaValueChange` (флаг `true` в конце `onAfterShow`, не срабатывает при `@LoadDataBeforeShow`); начальный rescан — только явный вызов из `onAfterShow`; `syncSkillsListToEntity` + `ensureSkillsListLoadedOnEntity` перед commit; задеплоено |
| 2026-06-23 | OpenPositionEdit: исправлен `StackOverflowError` при открытии/смене `positionType` — guard `applyingPositionTypeFromHandler`, пропуск `setPositionType` при уже загруженных LOB (`standartDescription`/`whoIsThisGuy`), общий `loadPositionWithDescriptionLobs` |
| 2026-06-23 | OpenPositionEdit: `laborAgreementDc`, `commentsOpenPositionDc`, `someFilesesDc` — standalone `CollectionLoader` (JPQL по `openPosition` / join M2M), убраны `property=` на `openPositionDc`; lazy load по вкладкам; `syncLaborAgreementToEntity` перед commit; исправлен unfetched `laborAgreement` при `@LoadDataBeforeShow` |
| 2026-06-23 | OpenPositionEdit: `openPositionSkillsListsDc` — standalone `CollectionLoader` (`itpearls_SkillTree` по `openPosition`), убран `property="skillsList"`; lazy `loadSkillsList()` по вкладке Skills — исправлен unfetched `skillsList` при `@LoadDataBeforeShow` |
| 2026-06-23 | OpenPositionBrowse: batch-кэш `positionEnName`/`positionRuName` в PostLoad + defensive `PersistenceHelper.isLoaded`/`dataManager.reload` в columnGenerator и descriptionProvider; `openPosition-browse-view.positionType` — явные свойства вместо nested view ref |
| 2026-06-23 | OpenPositionEdit: аудит views — убраны `skillsList`/`laborAgreement` из `openPosition-edit-view`; views на collection loaders (`openPositionComments`, `someFiles`, `laborAgreement`); `openPosition-parent-picker-view`; `person-owner-view.personPosition` → `position-picker-view`; lazy-guard `setIconSomeFileTab`, `ensureOpenPositionCommentsLoaded`, reload Position LOB при смене типа |
| 2026-06-23 | `openPosition-browse-view` / `openPosition-edit-view`: `positionType` → `position-picker-view` с `positionRuName`/`positionEnName` — исправлен unfetched attribute в OpenPositionBrowse (columnGenerator, descriptionProvider) и OpenPositionEdit |
| 2026-06-23 | OpenPositionEdit: исправлен `ViewBuilder.add()` для LOB (`comment`/`commentEn`, `standartDescription`/`whoIsThisGuy`) — отдельные `.add()` вместо двухаргументной формы (nested view) |
| 2026-06-23 | `openPosition-browse-view`: добавлен `projectOwner` (`person-owner-view`) в `projectName` — исправлен unfetched attribute в OpenPositionBrowse |
| 2026-06-23 | openPosition-browse/edit-view, batch LOB в Browse, lazy вкладки Edit, OpenPositionServiceTest, документация |
