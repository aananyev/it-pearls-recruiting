# OpenPosition — открытая вакансия

> Транзакционная сущность вакансий; центральный узел рекрутингового процесса.
> Оптимизация: 2026-06-23.
> Архитектурная спецификация: [OpenPosition_Spec.md](OpenPosition_Spec.md)
> UI Spec: [browse](../../screens/open-position/hunttech_OpenPosition.browse_Spec.md), [edit](../../screens/open-position/hunttech_OpenPosition.edit_Spec.md), [detail fragment](../../screens/open-position/hunttech_OpenPositionDetailScreenFragment_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

`OpenPosition` — транзакционная сущность открытой вакансии в HRM HuntTech: название и внешний ID (`vacansyName`, `vacansyID`), статус открыта/закрыта (`openClose`), приоритет согласования, черновик (`signDraft`), пауза, рейтинг, иерархия через `parentOpenPosition`. Вакансия описывает требования к кандидату (грейд, опыт, удалёнка, вилка зарплаты, тип должности `positionType`, проект `projectName`, города), LOB-тексты (описание RU/EN, шаблон письма, тестовое, памятка к интервью), навыки (`skillsList`), подписки рекрутёров (`RecrutiesTasks`), комментарии и файлы. Центральный узел рекрутингового процесса: к вакансии привязываются `IteractionList` и подбор кандидатов.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Главный browse — `hunttech_OpenPosition.browse` (меню, дерево `treeDataGrid`). Edit — `hunttech_OpenPosition.edit` с вкладками (основное, навыки, labor agreement, файлы, комментарии). Дополнительные browse: `OpenPositionRecruiting`, `OpenPositionOutstaff`, `ProdOpenPosition`, `OpenPositionMaster`. Фрагмент деталей строки — `hunttech_OpenPositionDetailScreenFragment`. Связанные сущности: `Project`, `Position`, `Grade`, `City`, `OpenPositionComment`, `OpenPositionNews`, `RecrutiesTasks`, `JobCandidate` (через взаимодействия и suggest). Архитектурная спецификация: [OpenPosition_Spec.md](OpenPosition_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

**Browse:** при открытии — фильтры «только открытые» и «только моя подписка»; пакетная подготовка данных для колонок; раскрытие строки с фрагментом и кнопками; закрытие вакансии может массово завершить взаимодействия с кандидатами «на рассмотрении».

**Edit:** много вкладок с ленивой загрузкой LOB и коллекций; автогенерация имени вакансии; проверка дубликатов и vacansyID; shortDescription ≤ 250 символов; уведомления и Telegram при открытии/закрытии.

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.hunttech.entity.OpenPosition` |
| **Имя в CUBA** | `hunttech_OpenPosition` |
| **Таблица БД** | `HUNTTECH_OPEN_POSITION` |
| **Тип данных** | транзакционная |
| **Критичность** | высокая |

### LOB-поля

| Поле | Стратегия |
|------|-----------|
| `comment`, `commentEn` | lazy reload при первом открытии вкладки «Описание должности» (`loadJobDescriptionTab`) |
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

Лента комментариев OpenPositionEdit читает feedback-итерации `hunttech_IteractionList` через view
`iteractionList-view` (JPQL `e.vacancy = :openPosition and e.iteractionType.signFeedback = true`);
для `recrutier` во view задекларированы `userAvatar`/`officialPhoto` (view `_minimal`) и
`fileImageFace` (`_local`) — их читает `ExtUser.resolveProfilePhoto()` при отрисовке аватара автора;
без них LAZY-доступ к `userAvatar` на detached-рекрутере давал ValidationException «null Session»
(UNFETCHED ATTRIBUTE ACCESS) при первом открытии вкладки «Комментарии».

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
| Вкладки | Lazy LOB/collections при первом выборе; label-навигация sidebar — набор пунктов активной вкладки (`syncSidebarNavigation`, 11 наборов `openPosition*TabNavigation`), клик по пункту фокусирует первый элемент блока ввода; вкладка «Комментарии» — лента из `commentsOpenPositionDc` + feedback-итераций (`iteractionList-view`), подпись «кандидат / должность» null-safe; sidebar-пара 50/50 «Статус вакансии»/«Приоритет» (`vacancyStateSummary`) обновляется из edited-объекта (`refreshSidebarStatus`/`refreshSidebarPriority`) |
| Название вакансии | При открытии — значение `vacansyName` из сущности без изменений (обработчик смены грейда игнорирует событие привязки, `isUserOriginated=false` — иначе «<Грейд> null» затирал бы сохранённое название); при действиях пользователя каскады: смена грейда → префикс названия, тип позиции/проект/город → генерация (`generatePositionName*`/`generateVacancyName`), кнопка «Генерировать» → полная перегенерация |
| Сохранение | sync skills + laborAgreement; дубликат имени/vacansyID; shortDescription ≤ 250 |
| После save | OpenPositionNews; Telegram (ошибка не блокирует) |

### Java-оптимизации

- **OpenPositionBrowse:** `removeCandidatesWithConsideration` — batch `CommitContext` для `IteractionList` при закрытии вакансии (один `dataManager.commit` вместо N×`commit(jc)`)
- **OpenPositionBrowse:** `refreshBrowseLobExistsCaches` — batch `exists` для comment/exercise/memo/templateLetter/project/company descriptions (без загрузки LOB-текста); `positionEnName`/`positionRuName` — light strings; lazy load полного текста в `descriptionProvider`/tooltip и при «Описание»/details
- **OpenPositionBrowse:** `refreshBrowseAggregateCaches` — activeRecruitersCountByPosition (`count(e.reacrutier)`), sentCvCountByPosition, avgRatingByPosition (batch в PostLoad; rowStyleProvider, lastCVSend, rating)
- **OpenPositionEdit:** lazy LOB/collections по `tabSheetOpenPosition`; вкладки Skills, Labor Agreement, Files, Comments — standalone `CollectionLoader` с `:openPosition` в JPQL `<condition>` + `PreLoadEvent.preventLoad()` до `setParameter`; rescан навыков — `openPositionSkillsListsDc` / `skillTrees`, guard `screenFullyLoaded`, sync в entity только в `onBeforeCommitChanges`; `syncLaborAgreementToEntity`/`syncSkillsListToEntity` — skip если вкладка не открыта; коллекции `laborAgreement`/`skillsList` декларированы inline-свойствами в view контейнера `openPositionDc` (open-position-edit.xml / open-position-edit-preview.xml), прямой `setLaborAgreement`/`setSkillsList` безопасен — woven-сеттер коллекции читает getter для change detection и падает `IllegalStateException`, если атрибут отсутствует во fetch group контейнера; Telegram-уведомление при commit — не блокирует сохранение при ошибке API; таймер закрытия вакансии (`closedVacancyTimer`) — 60 с, `autostart=false`, старт/стоп в `initClosedVacancyTimerFacet` при `closingDate` (AfterShow + ValueChange); дубликат `vacansyID` — `loadValue(count)` с `=` в `onBeforeCommitChanges`, при edit — `e.id <> :currentId`

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
| OpenPositionEdit | LOB при открытии | все сразу | lazy по вкладкам | −6 LOB | comment/commentEn + LOB типа позиции — только вкладка «Описание должности»; exercise/memo/template — свои вкладки; `companyNamesLc` cacheable |
| OpenPositionEdit | SQL при открытии (коллекции без параметра) | 3 лишних SELECT всех строк | 0 | −3 запроса | `openPositionNewsLc` (все новости всех позиций: Seq Scan 35 649 строк / ~40ms), `projectNamesLc` (все проекты, дубль с `initProjectNameField`), `companyDepartamentsLc` (все департаменты, не cacheable) — `preventAutoLoadUntilParameterSet` в `onInit` |
| OpenPositionEdit | SQL при открытии (опции компаний/позиций/городов) | ~5 940 (5 658 Company + 188 Position + 93 City по ID) | 3 (по 1 SELECT на список) | −5 937 | cacheable-загрузчики `companyNamesLc`/`positionTypesLc`/`citiesDl` отключены: CUBA entity cache при выборке списка делает find() по ID на каждую строку (N+1-цикл 7-9 с) |
| OpenPositionEdit | Время открытия (p50, прогретые) | 9-14 с | 2.7-3.1 с | −~75% | после фикса cacheable-справочников; остаток — UIDL ~2.2 МБ (сериализация ~6 000 опций) |

---

## 9. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-13 | OpenPositionEdit: вкладка «Описание должности» — аккордеон `openPositionAccordion` (RichTextArea `comment`/`commentEn`/`standartDescription`/`whoIsThisGuy`) растягивается на оставшуюся высоту вкладки (требование пользователя): контейнер вкладки scrollBox → vbox `jobDescriptionVBox` (`expand="descriptionsAccordionHBox"`), SCSS во всех 7 темах — min-height секции/редактора 360/260px → 120px + `overflow-y: auto` страховка. Только UI-компоновка; entity, views, lazy-логика `loadJobDescriptionTab`, бизнес-логика не менялись; контрактные тесты PASS. CDP: fullscreen 1920×1080 — аккордеон 474px (было 360), редактор 372px (было 260), не шире экрана |
| 2026-08-11 | OpenPositionEdit/Preview: исправлен `IllegalStateException: Cannot get unfetched attribute [laborAgreement]` при сохранении вакансии с открытой вкладкой «Трудовой договор». Регрессия 2026-08-10: в `syncLaborAgreementToEntity`/`syncSkillsListToEntity` замена `dataContext.merge(reloaded)` на прямой сеттер — woven-сеттер коллекции читает getter для change detection, а атрибута нет в fetch group контейнера (detached `openPosition-edit-view`) → падение при коммите. Фикс: коллекции `laborAgreement`/`skillsList` задекларированы inline-свойствами в view контейнера `openPositionDc` обоих XML (edit + preview); ensure-методы reload+setter удалены как ловушка. Общий `openPosition-edit-view` не менялся (он nested в job-candidate-edit.xml — коллекции там раздули бы граф). Тесты: OpenPositionEditDetachedObjectTest 6/6 (новый контракт inline-декларации), ScreenViewIntegrityTest 8/8, LayoutContract 20/20, OptionsIntegrity 5/5 PASS |
| 2026-08-10 | OpenPositionEdit: устранён главный N+1 открытия формы — cacheable-загрузчики крупных справочников-опций (`companyNamesLc` ~5 660 компаний, `positionTypesLc` ~188, `citiesDl` ~293) отключены (cacheable="true" → обычный loader): CUBA entity cache при выборке списка выполнял точечный find() по ID для каждой строки (~5 658 SELECT Company + ~188 Position + ~93 City на открытие, 7-9 с). Замер до/после: SQL при открытии 6 215 → 216 (Company 5 658 → 1), время открытия 9-14 с → 2.7-3.1 с (p50, прогретые; 1-е после рестарта ~6 с; остаток — UIDL ~2.2 МБ: сериализация ~6 000 опций компаний/городов/позиций). Добавлен тест-класс `OpenPositionEditOptionsIntegrityTest` (5 тестов: view опций декларируют fileCompanyLogo/projectLogo — защита от UNFETCHED в optionImageProvider; список компаний грузится без lazy; XML формы не содержит cacheable на крупных loaders — регрессия find-цикла). Вид/компоновка/бизнес-логика не менялись; 39 профильных тестов PASS |
| 2026-08-10 | OpenPositionEdit: исправлена потеря `openClose` при сохранении (toggle «Закрыть/Открыть вакансию») + оптимизация коммита. (1) `syncLaborAgreementToEntity`/`syncSkillsListToEntity` в `onBeforeCommitChanges`: `dataContext.merge(reloaded)` заменён на перенос только коллекций (`setLaborAgreement`/`setSkillsList`) — merge копировал состояние из БД поверх изменений формы (UPDATE без OPEN_CLOSE, version-only; ~40 SELECT SYS_FILE на коммит). (2) `onBeforeShow1` и `onBeforeCommitChanges1`: чекбокс `openClosePositionCheckBox` при null синхронизируется из `getEditedEntity().getOpenClose()` вместо жёсткого `false` (раньше закрытая вакансия при повторном входе показывала «Закрыть вакансию» и молча открывалась при сохранении). (3) из `onAfterShow` убран `rescanJobDescription()` — пересборка навыков только при изменении описания (−2-3 SQL при открытии). CDP-проверка: закрытие→OK→`open_close=t`; вход в закрытую→«Открыть вакансию»/«ЗАКРЫТА»; сохранение без изменений→осталась закрытой; открытие→OK→`open_close=f`. Вид/компоновка/бизнес-логика не менялись; compileJava PASS |
| 2026-08-10 | OpenPositionEdit: багфикс «<Грейд> null» в поле «Вакансия» при открытии формы — `onGradeLookupPickerFieldValueChange` перегенерировал название на событии привязки грейда к контейнеру (`isUserOriginated=false`; `gradeDc` ещё не загружена, поле названия не привязано): `setValue(gradeName + " " + null)` затирал сохранённый `vacansyName`. Добавлен guard `isUserOriginated` (при открытии название берётся из сущности без изменений — `vacansyName` не переписывается) + null-guard в ветке добавления префикса (при пустом названии — только грейд). Пользовательская смена грейда актуализирует префикс по-прежнему; `views.xml` и структура БД не менялись; compileJava PASS |
| 2026-08-09 | OpenPositionEdit: предотвращена автозагрузка коллекций с незаданным параметром (CUBA игнорирует JPQL-условие → SELECT всех строк): `preventAutoLoadUntilParameterSet` для `openPositionNewsLc` (новости — только вкладка «Новости»), `projectNamesLc` (проекты — только из `initProjectNameField`), `companyDepartamentsLc` (департаменты — только при выборе компании). Убраны 3 лишних SQL при открытии формы (новости: Seq Scan 35 649 строк, проекты с глубоким view, все департаменты); поведение не изменено; view не менялись; compileJava + ScreenViewIntegrityTest + контрактные тесты PASS; CDP-проверка всех вкладок без ошибок |
| 2026-08-09 | OpenPositionEdit: lazy-load оптимизация открытия формы — (1) LOB `comment`/`commentEn` и LOB-описания типа позиции (`standartDescription`/`whoIsThisGuy`) убраны из `onBeforeShow` (раньше 4 синхронных LOB-запроса на каждый показ формы), догружаются при первом открытии вкладки «Описание должности» (`loadJobDescriptionTab`, флаг `jobDescriptionLobsLoaded`; база `openPositionText` обновляется после догрузки для корректной работы новостей); (2) новости — `setOpenPositionNews` → lazy `loadOpenPositionNewsTab` (вкладка «Новости»); (3) BPM-вложения и `ProcActionsFragment.init` — из `onBeforeShow1` в lazy `ensureApprovalProcessLoaded` (вкладка «Согласование», выполняется и для новых); (4) `companyNamesLc` → `cacheable="true"`. View не менялись; `compileJava` + `ScreenViewIntegrityTest` + `OpenPositionEditDetachedObjectTest` PASS |
| 2026-08-09 | OpenPositionEdit sidebar: под парой «Статус вакансии»+«Приоритет» добавлена кнопка `openClosePositionButton` — toggle OPEN_CLOSE (открыта false/none → «Закрыть вакансию», закрыта true → «Открыть вакансию», надпись `refreshOpenCloseButton()`); invoke `openClosePositionToggle` инвертирует `openClose`, синхронизирует чекбокс «Закрыта» (блокировка полей `disableEnableFields`) и обновляет sidebar; нотификация всем пользователям — `UiNotificationEvent` (GlobalEvents addon, стандартный инструмент CUBA) через существующий `onBeforeCommitChanges3 → publishEventMessage`; визуал 1:1 с footer-кнопками JobCandidateEdit (`job-candidate-profile-footer .v-button`) — класс `open-position-editor-open-close-button` во всех 7 темах; messages_ru +`msgCloseVacancy`/`msgOpenVacancy`; контрактные тесты `vacancyCloseButtonSitsUnderStatusPriorityPair` + `vacancyCloseButtonScssCarriesJobCandidateFooterLookInAllThemes`; view не менялись |
| 2026-08-09 | OpenPositionEdit sidebar: блок срочности приведён к эталону IteractionListEdit — горизонтальная пара ячеек 50/50 `vacancyStateSummary` («Статус вакансии» `vacancyStatusSummary` + `statusOfVacansyLabel` с цветовой индикацией h3-gray/h3-green/h3-red от `refreshSidebarStatus()`; «Приоритет» `vacancyPrioritySummary` со светофором); ячейки с рамками выделения (фон/border/radius 1:1 с `iteraction-list-vacancy-state-cell`), новые классы `open-position-editor-sidebar-caption/-value/-value-row`; формат работы — строка под парой; SCSS во всех 7 темах идентичны; Java +`refreshSidebarStatus()` (presentation-only, из edited-объекта); контрактные тесты `vacancyStatePairMirrorsIteractionListEdit` + `vacancyStatePairScssCarriesHighlightFramesInAllThemes`; view не менялись (все атрибуты из `openPosition-edit-view`) |
| 2026-08-09 | OpenPositionEdit: label-навигация sidebar стала попланочной — 11 наборов пунктов по вкладкам `tabSheetOpenPosition` (`openPositionMainTabNavigation`, `openPositionLaborTabNavigation`, `openPositionJobDescriptionTabNavigation`, `openPositionFilesTabNavigation`, `openPositionExerciseTabNavigation`, `openPositionMemoTabNavigation`, `openPositionTemplateLetterTabNavigation`, `openPositionSkillsTabNavigation`, `openPositionNewsTabNavigation`, `openPositionApprovalTabNavigation`, `openPositionCommentsTabNavigation`); `syncSidebarNavigation()` показывает набор активной вкладки и подсвечивает первый пункт; клики фокусируют первый элемент ввода секции (18 пунктов; «Согласование»/«Комментарии» — без фокуса, блоки без полей ввода); секции «Оплата ресерчерам/рекрутерам» (label-only) не включены; контрактный тест `sidebarNavigationSetsFollowTabs` + актуализация UI Spec |
| 2026-06-29 | OpenPositionEdit: `projectNamesDc` — nested `projectDepartment.companyName.cityOfCompany` в view loader; исправлен unfetched `cityOfCompany` при смене Project |
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
| 2026-06-23 | OpenPositionEdit: `openPositionSkillsListsDc` — standalone `CollectionLoader` (`hunttech_SkillTree` по `openPosition`), убран `property="skillsList"`; lazy `loadSkillsList()` по вкладке Skills — исправлен unfetched `skillsList` при `@LoadDataBeforeShow` |
| 2026-06-23 | OpenPositionBrowse: batch-кэш `positionEnName`/`positionRuName` в PostLoad + defensive `PersistenceHelper.isLoaded`/`dataManager.reload` в columnGenerator и descriptionProvider; `openPosition-browse-view.positionType` — явные свойства вместо nested view ref |
| 2026-06-23 | OpenPositionEdit: аудит views — убраны `skillsList`/`laborAgreement` из `openPosition-edit-view`; views на collection loaders (`openPositionComments`, `someFiles`, `laborAgreement`); `openPosition-parent-picker-view`; `person-owner-view.personPosition` → `position-picker-view`; lazy-guard `setIconSomeFileTab`, `ensureOpenPositionCommentsLoaded`, reload Position LOB при смене типа |
| 2026-06-23 | `openPosition-browse-view` / `openPosition-edit-view`: `positionType` → `position-picker-view` с `positionRuName`/`positionEnName` — исправлен unfetched attribute в OpenPositionBrowse (columnGenerator, descriptionProvider) и OpenPositionEdit |
| 2026-06-23 | OpenPositionEdit: исправлен `ViewBuilder.add()` для LOB (`comment`/`commentEn`, `standartDescription`/`whoIsThisGuy`) — отдельные `.add()` вместо двухаргументной формы (nested view) |
| 2026-06-23 | `openPosition-browse-view`: добавлен `projectOwner` (`person-owner-view`) в `projectName` — исправлен unfetched attribute в OpenPositionBrowse |
| 2026-06-23 | openPosition-browse/edit-view, batch LOB в Browse, lazy вкладки Edit, OpenPositionServiceTest, документация |
