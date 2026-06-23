# OpenPosition — открытая вакансия

> Транзакционная сущность вакансий; центральный узел рекрутингового процесса.
> Оптимизация: 2026-06-23.

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
| OpenPositionEdit | LOB при открытии | все сразу | main tab + lazy | −4 LOB | вкладки exercise/template/… |

---

## 9. История изменений

| Дата | Изменение |
|------|-----------|
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
