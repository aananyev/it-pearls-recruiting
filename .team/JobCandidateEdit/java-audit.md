# Аудит функциональности JobCandidateEdit — «Контракт функциональности» для редизайна компоновки

- **Дата:** 2026-08-03
- **Проект:** HRM HuntTech, CUBA Platform 7.3 (Vaadin 8)
- **Контроллер:** `modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java` (4371 стр.)
- **Дескриптор:** `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml` (1680 стр., 128 КБ)
- **Цель:** предстоящий редизайн **только presentation** (XML stylename/layout, SCSS) НЕ должен сломать функциональность и бизнес-логику. Код НЕ меняется.
- **Проверено:** полный текст Java-контроллера, XML-дескриптор, `JobCanidateDetailScreenFragment.java`/`.xml`, `ContactInfo.java`, `HistoryRowData.java`, `views.xml` (global), `JobCandidateEditLayoutContractTest.java`.

> ⚠️ **Главное правило:** всё, что перечислено ниже, — НЕИЗМЕНЯЕМО при presentation-редизайне. Разрешено менять только `stylename`, `caption`, размеры, обёртки-контейнеры (не затрагивающие id/связи) и SCSS.

---

## 1. НЕизменяемые элементы (контракт)

### 1.1 Аннотации и каркас контроллера (JobCandidateEdit.java)

| Элемент | Строки Java | Комментарий |
|---|---|---|
| `@UiController("hunttech_JobCandidate.edit")` | 56 | Идентификатор экрана (регистрация, ссылки из других экранов) |
| `@UiDescriptor("job-candidate-edit.xml")` | 57 | Имя дескриптора |
| `@EditedEntityContainer("jobCandidateDc")` | 58 | **id контейнера редактируемой сущности НЕ менять** |
| `@LoadDataBeforeShow` | 59 | Жизненный цикл загрузки |

### 1.2 `@Inject`-поля контроллера (id-контракт с XML)

Все id ниже обязаны существовать в XML ровно один раз (проверено grep-ом — все присутствуют). Удаление/переименование → `IllegalStateException` при старте или NPE в рантайме.

**Сервисы/инфраструктура (безопасны от XML, но обязательны):** `dataManager` (62), `dialogs` (64), `screenBuilders` (70), `userSession` (72), `metadata` (74), `dataContext` (78), `webBrowserTools` (110), `pdfParserService` (112), `notifications` (116), `starsAndOtherService` (122), `screens` (124), `parseCVService` (126), `uiComponents` (128), `backgroundWorker` (130), `log` (132), `getRoleService` (213), `interactionService` (223), `fileLoader` (225), `userSessionSource` (227), `fragments` (229), `messageBundle` (268), `resumeRecognitionService` (297), `openPositionService` (299).

**Компоненты UI (обязаны остаться в XML с тем же id):**
- Sidebar/профиль: `labelCV` (66), `labelQualityPercent` (68), `candidatePic` (100, `ovaFallbackImage`, биндинг `fileImageFace`), `fileImageFaceUpload` (102), `emailLinkButton` (104), `skypeLinkButton` (106), `telegrammLinkButton` (108), `telegrammGroupLinkButton` (118), `candidateRatingLabel` (120), `blockCandidateButton` (195), навигация `candidateNavMain/Positions/Iteraction/Resume/ContactInfo/Comments/History` (197–209), `blockCandidateCheckBox` (215), `fullNameField` (281), `personPositionLabel` (283), `emailLabel` (285), `phoneLabel` (287), `mobilePhoneLabel` (289), `skypuLabel` (291), `telegramLabel` (293), скрытые заголовки `personPositionTitle/emailTitle/phoneTitle/telegramTitle/skypeTitle/jobTitleTitle` (82–92), `skillBox` (231), `lastProjects` (279), `dictionatysTavlesHBox` (277).
- Вкладка «Основное»: `firstNameField` (134), `secondNameField` (136), `middleNameField` (138), `positionsLabel` (140), `currentCompanyField` (142), `personPositionField` (144), `jobCityCandidateField` (146), `birdhDateField` (148).
- Вкладка «Контакты»: `emailField`/`phoneField`/`mobilePhoneField`/`telegramNameField`/`whatsupNameField`/`wiberNameField`/`skypeNameField` (158–163 — не-@Inject, ищутся лениво, см. 1745–1751), `priorityCommunicationMethodRadioButton` (149, 1752), `socialNetworkTable` (164, 1754), `addSocialNetworkListsButton` (294, 1755).
- Вкладка «Позиции и вакансии»: `lastProjectTable` (250, 1489), `suggestVacancyTable` (266, 1490), `lastProjectDl` (233), `lastProjectDc` (252), `suggestOpenPositionDl` (255).
- Вкладка «Взаимодействия»: `jobCandidateIteractionListTable` (151, 1980), `frequentInteractionPopupButton` (153, 1982), `copyIteractionButton` (244, 1984), `vacancyFilterLookupPickerField` (295, 1985), `openPositionProjectDescriptionButton` (152, 1987).
- Вкладка «Резюме и файлы»: `jobCandidateCandidateCvTable` (154, 2115), `scanContactsFromCVButton` (156, 2117), `copyCVButton` (155, 2118), `checkSkillFromJD` (157, 2119).
- Вкладка «Комментарии»: `chatMessageTextField` (270, 1717), `sendCommentButton` (269, 1718), `vacancyPopupPickerField` (271, 1719), `jobCandidateCommentsDataGrid` (272, 1721), `interactionCommentDl` (274).
- Контейнеры/лоадеры: `jobCandidateSocialNetworksDc` (76), `jobCandidateCandidateCvsDc` (114), `jobCandidateIteractionDc` (217), `jobCandidateDl` (219), `jobCandidateDc` (221), `tabSheetSocialNetworks` (235), `openPositionDl` (257), `currentCompaniesLc` (259), `currentCompaniesDc` (261), `citiesDl` (263), `personPositionsLc` (265).

### 1.3 Данные: контейнеры, лоадеры, JPQL (XML-секция `<data>`, строки XML 11–232)

| id | Тип | XML-строки | Кто/что дёргает в Java |
|---|---|---|---|
| `jobCandidateDc` (instance) + view | InstanceContainer | 15–79 | `@EditedEntityContainer`, подписки 1207/1219, `getEditedEntity()` везде |
| `jobCandidateDl` (loader) | InstanceLoader | 67–69 | `jobCandidateDl.load()` — 2621, 2626, 3893, 3904 |
| `jobCandidateCandidateCvsDc` (collection, property `candidateCv`) | CollectionContainer | 71–72 | подписка 2630, `scanContactsFromCVs()` 2744 |
| `jobCandidateSocialNetworksDc` (collection, property `socialNetwork`) | CollectionContainer | 74–75 | `enableDisableContacts()` 674, `initSocialNeiworkTable()` 1807, `addMissingSocialNetworksListsInvoke()` 4051 |
| `jobCandidateIteractionDc` (collection, property `iteractionList`) | CollectionContainer | 77–78 | `setIteractionListVacancyFilter()` 783–796, `suggestVacancyTableNotSendedIconColumnColumnGenerator()` 3568, `addIteractionOfNewCandidate()` 1082 |
| `lastProjectDc` / `lastProjectDl` (keyValue) | KeyValueCollection | 82–112 | JPQL 88–92 (`e.vacancy, max(e.dateIteraction)`, условие `e.candidate = :candidate`), параметр `candidate` (1567, 2408); `lastIteractionCount()` 3429, `addInteractionsViewButton()` 3453 |
| `openPositionDc` / `openPositionDl` | Collection | 114–126 | JPQL 121–123 (только открытые), пре-лоад-гейт 1317, `ensureOpenPositionLoaded()` 1408 |
| `suggestOpenPositionDc` / `suggestOpenPositionDl` | Collection | 128–162 | **параметры `positionType`/`positionTypes` ставятся кодом** (865–872), `maxResults(1)` для новой записи (877), пре-лоад-гейт 1323 |
| `personPositionsDc` / `personPositionsLc` | Collection | 164–176 | JPQL 171–173 (исключён «(не использовать)»), `ensureReferenceLoadersLoaded()` 1400 |
| `currentCompaniesDc` / `currentCompaniesLc` | Collection | 178–185 | JPQL 183, `mergeCreatedCompany()` 1960–1964 (добавление новой компании в контейнер!) |
| `citiesDc` / `citiesDl` | Collection | 187–193 | JPQL 191, `ensureReferenceLoadersLoaded()` 1399 |
| `interactionCommentDc` / `interactionCommentDl` | Collection | 195–231 | **параметры `candidate` и `comment` ставятся кодом** (823–824), пре-лоад-гейт 1324, `reloadInteractions()` 3903 |

**JPQL, зашитые в Java (не трогать, но и XML их не затрагивает — перечислено для полноты картины):** 167, 168, 170, 304–311 (view `jobCandidate-view-search`), 376–384, 503–509, 1039–1044, 1052–1056 (view `iteraction-view`), 1093–1108 (view `jobCandidate-view`), 1425–1430, 1439–1445 (view `candidateCV-browse-view`), 1461–1467 (view `iteractionList-job-candidate`), 1529–1553 (loadValues для истории позиций), 1674–1680 (view `socialNetworkURLs-view`), 1700–1708 (view `company-picker-view`), 2240–2265, 2361–2376, 3822–3824.

### 1.4 Actions / invoke-методы (XML)

`invoke="..."` в XML (каждый — публичный метод контроллера; удаление кнопки или смена invoke = потеря функции):
- Навигация: `candidateNavMain`, `candidateNavPositions`, `candidateNavIteraction`, `candidateNavResume`, `candidateNavContactInfo`, `candidateNavComments`, `candidateNavHistory` (XML 426–456; Java 4344–4370).
- Sidebar: `createCandidateCv` (529), `createCandidateIteraction` (535), `openPositionMasterBrowseStart` (541).
- Меню «Еще»: `blockCandidateButton` (636), `onButtonSubscribeClick` (642), `onCardAuditInfoClick` (648).
- Основное: `addPositionList` (921).
- Контакты: `addSocialNetworksListsInvoke` (1161), `addMissingSocialNetworksListsInvoke` (1166), `removeEmptySocialNetworkListsButton` (1171).
- Резюме: `scanContactsFromCVs` (1514).
- Комментарии: `sendCommentButtonInvoke` (1597).

Стандартные действия: `windowCommitAndClose` (1666) и `windowClose` (1671) в нижней панели; CRUD-действия таблиц — `socialNetworkTable` create/edit/remove (1148–1151, `editorEnabled="true"` 1129), `jobCandidateIteractionListTable` create/edit/remove/refresh (1337–1342, кнопки buttonsPanel 1346–1359), `jobCandidateCandidateCvTable` create/edit/remove (1500–1504, buttonsPanel 1507–1515).

### 1.5 Жизненный цикл и валидаторы (подписки)

| Подписка | Строки | Что делает (не сломать!) |
|---|---|---|
| `onAfterShow` | 632–647 | обновление профиля, процент заполнения, блокировка, фоновый парсинг навыков |
| `onBeforeShow` | 707–751 | инициализация вкладок, labelCV ДА/НЕТ, `setStatus(0)` для новой, ссылки, фото, `lastIteraction`, видимость `blockCandidateButton` по ролям |
| `onInit` | 1315–1340 | **пре-лоад-гейты лоадеров** (1317–1324), слушатель смены вкладок (1328–1337), `configureAvailableComponentRenderers()` |
| `onBeforeCommitChanges1` | 949–979 | **проверка дубликата** (читает `firstNameField`, `secondNameField`, `personPositionField`, `jobCityCandidateField` → не удалять/не скрывать эти поля) |
| `onBeforeCommitChanges` | 982–992 | `replaceE_yo()`, `setFullNameCandidate()`, `checkTelegramName()`, `trimTelegramName()`, `addIteractionOfNewCandidate()` |
| `onChange` (DataContext) | 1141–1144 | пересчёт процента заполнения |
| `onJobCandidateDcItemChange` / `ItemPropertyChange` (DATA_CONTAINER) | 1207–1234 | синхронизация sidebar-ФИО и `fullName` (денормализованное свойство!) |
| `onJobCandidateCandidateCvsDcItemChange` (DATA_CONTAINER) | 2630–2633 | скан контактов из резюме |
| `@Subscribe("fileImageFaceUpload")` | 844–847 | fallback аватара |
| `@Subscribe` кнопки-ссылки | 2437–2468 | mailto / t.me / skype |
| `@Subscribe("firstNameField"/"secondNameField"/"personPositionField"/"phoneLabel")` | 3918–3939, 4010–4015 | синхронизация ФИО/должности/телефона |

**`focusComponent` в XML отсутствует** (grep по обоим дескрипторам пуст) — контракта нет; при желании добавить можно, но только на существующий id.

### 1.6 Ленивая инициализация вкладок (жёсткие id-зависимости)

Java инициализирует UI по одному разу при выборе вкладки и ищет компоненты через `getWindow().getComponentNN(...)`. **id вкладок захардкожены** и обязаны остаться: `tabMain` (1820), `tabContactInfo` (1738), `tabPositions` (1484), `tabIteraction` (1976), `tabResume` (2111), `commentsTab` (1716), `tabHistory` (4328–4334, навигация). Смена имени вкладки = молчаливая потеря всей инициализации вкладки.

---

## 2. View integrity — атрибуты и контейнеры (защита от UNFETCHED ATTRIBUTE ACCESS)

### 2.1 View контейнера `jobCandidateDc` (XML 19–64) — расширять/сужать нельзя

```xml
extends="_local"
cityOfResidence (view="_local")
currentCompany (fetch=BATCH, _local + companyGroup)
fileImageFace (_local)
positionList (fetch=BATCH, _local + positionList)
personPosition (_local)
iteractionList (fetch=BATCH, _minimal + vacancy(openPosition-edit-view + projectName _local) + iteractionType(iteraction-list-type-view + signSendToClient + signEndCase))
socialNetwork (fetch=BATCH, _minimal)
candidateCv (fetch=BATCH, _minimal + toVacancy(openPosition-edit-view + projectName _local + projectDescription + projectLogo _minimal + projectDepartment _minimal))
```

### 2.2 Getter'ы в Java и их источники (критические цепочки)

| Путь | Где читается в Java | Источник (контейнер) |
|---|---|---|
| `personPosition.positionRuName` | 942, 961, 2536, 3630–3640 | `jobCandidateDc` → `personPosition` (_local) |
| `positionList[].positionList.positionRuName` | 2536 | `jobCandidateDc` → `positionList` (BATCH) |
| `candidateCv[].textCV / letter / contactInfoChecked / linkOriginalCv / linkHuntTechCV / lintToCloudFile / datePost / owner` | 1415–1417, 2143–2220, 2604–2610, 2682–2693, 2745–2765 | `jobCandidateDc` → `candidateCv` (+ merge из `candidateCV-browse-view`, 1447–1450) |
| `candidateCv[].toVacancy.vacansyName / lastOpenDate / comment` | 2642, 2658, 3606–3619, 3688–3690 | `candidateCv.toVacancy` (openPosition-edit-view) |
| `candidateCv[].toVacancy.projectName.projectDescription / projectLogo / projectName` | 4103–4154 | `candidateCv.toVacancy.projectName` (XML 54–61) |
| `candidateCv[].resumePosition.positionRuName / positionEnName` | 3625–3646 | `candidateCv.resumePosition` |
| `iteractionList[].vacancy.vacansyName / openClose / comment` | 2015–2025, 3473–3478, 3491–3496, 3508–3513, 3688–3690 | `jobCandidateDc` → `iteractionList` → `vacancy` (openPosition-edit-view) |
| `iteractionList[].vacancy.projectName.projectDescription / projectLogo / projectOwner` | 3256–3293, 3526–3540, 4204–4237 | `iteractionList.vacancy.projectName` (XML 37–40) |
| `iteractionList[].vacancy.projectName.projectDepartment.companyName.comanyName / companyDescription / workingConditions` | 3268–3293 | **глубокая цепочка** — см. 2.3 |
| `iteractionList[].iteractionType.pic / signSendToClient / signEndCase / iterationName` | 2381–2382, 3571–3593, 311–315 (фрагмент) | `iteractionList.iteractionType` (iteraction-list-type-view + 2 sign-поля, XML 42–47) |
| `iteractionList[].currentOpenClose / numberIteraction / rating / comment / addDate / addString / addInteger / dateIteraction / recrutierName / recrutier.name / createdBy` | 2027–2085, 3469–3518, 3681–3789 | `jobCandidateDc` → `iteractionList` (_minimal покрывает) |
| `socialNetwork[].networkURLS / socialNetworkURL.socialNetwork / .logo / .comment / .socialNetworkURL` | 674–676, 2874–2878, 3094–3108, 3241, 4077, 4261–4306 | `jobCandidateDc` → `socialNetwork` (_minimal) **+ merge из `socialNetworkURLs-view`** (1679, 1682–1685) — см. 2.3 |
| `getEditedEntity().email/phone/mobilePhone/telegramName/telegramGroup/skypeName/whatsupName/wiberName/priorityContact/status/fullName/blockCandidate/fileImageFace/createdBy/createTs/updatedBy/updateTs` | 638, 724, 1227–1229, 1253–1264, 2420–2473, 3949, 3994–4001 | `jobCandidateDc` (_local) |
| `lastProjectDc` item `.getValue("vacancy")` (KeyValue) | 3453–3454 | `lastProjectDc` (keyValue, свойство `vacancy`, XML 106–107) |
| `suggestOpenPositionDc` item: `vacansyName / projectName / projectOwner / owner / lastOpenDate / comment` | 3520–3559 | `suggestOpenPositionDc` view (XML 131–139) |

### 2.3 Тонкие места (обязательные для сохранения механизмы)

1. **`socialNetwork` таблица**: колонка `networkName` биндится на `property="socialNetworkURL.socialNetwork"` (XML 1137–1138), а генераторы `socialNetworkLogoColumn`/`linkToWeb` читают `getLogo()`, `getComment()`, `getNetworkURLS()`. Этого нет в view `_minimal` контейнера — безопасность обеспечивает **`ensureSocialNetworksLoaded()` (1668–1688), который мержит элементы с view `socialNetworkURLs-view`** (в нём есть `socialNetworkURL` с logo/comment, views.xml 482–489). Перенос таблицы в другой контейнер или удаление этого merge = UNFETCHED ATTRIBUTE / пустые ячейки.
2. **`openPositionDescription()` (3251–3298)** читает `vacancy.projectName.projectDepartment.companyName.*` — цепочка доступна только потому, что `iteractionList.vacancy` грузится view `openPosition-edit-view`, где есть `projectDepartment` (view 232+). Менять view контейнера нельзя.
3. **`CARD_COMPLETION_PROPERTIES` (178–181)** читает через `candidate.getValue(property)`: `firstName, middleName, secondName, birdhDate, cityOfResidence, personPosition, currentCompany, email, phone, mobilePhone, telegramName, whatsupName, wiberName, skypeName, priorityContact` — все обязаны оставаться в view `jobCandidateDc` (защищено `readCandidatePropertySafely`, но лучше не провоцировать).
4. **Ленивые коллекции**: `ensureInteractionsLoaded()` (1455–1476, view `iteractionList-job-candidate`), `ensureCandidateCvLoaded()` (1433–1453, view `candidateCV-browse-view`), `ensureSocialNetworksLoaded()`, `ensurePositionListLoaded()` (1690–1693, через `getPositionList()` контейнера) — результаты **мержатся в `dataContext` и подменяют коллекции edited entity**; смена контейнеров/биндингов это ломает.

---

## 3. Риски presentation-правок

### 3.1 БЕЗОПАСНО менять (не влияет на Java)

- `stylename` / `caption` / `description` (кроме caption-ключей, которых нет в messages — проверять `messages.properties`/`messages_ru.properties`).
- `width` / `height` / `align` / `spacing` / `margin` / `box.expandRatio` на контейнерах.
- Обёртки-контейнеры (`vbox`/`hbox`/`grid`/`scrollBox`) вокруг существующих элементов, **не меняя id, property, dataContainer, optionsContainer, actions, invoke**.
- SCSS: любые изменения в 7 темах (`modules/web/themes/*/com.company.hunttech/`) — но 7 копий `job-candidate-editor.scss` и `edit-screen-shared-styles` должны оставаться идентичными между темами (проверяет `JobCandidateEditLayoutContractTest.everyThemeContainsLocalLayoutGuards`/`jobCandidateEditorScssIsIdenticalAcrossAllThemes`).
- `dialogMode` (1200×750) — менять можно, но это UX-решение, не функциональность.

### 3.2 ОПАСНО / ЗАПРЕЩЕНО менять

| Атрибут | Почему опасно |
|---|---|
| **`id` любого компонента** | Все id из п.1.2, 1.3, 1.4 и колонок таблиц используются в Java (`@Inject`, `getComponentNN`, `getColumn`, `@Subscribe("...")`, invoke). Даже `visible="false"` «скрытые контракты» (см. 3.3) нельзя удалять — только оставлять скрытыми |
| **`property` / `dataContainer`** у полей и таблиц | Ломает биндинг и логику, читающую значения через контейнеры; полный список биндингов — п.2.1 и список ниже |
| **`optionsContainer`** | `citiesDc` (822), `personPositionsDc` (860), `openPositionDc` (1587) — источники опций picker'ов |
| **`tab id`** (7 вкладок) | Захардкожены в Java (п.1.6); переименование = молчаливая потеря инициализации вкладки |
| **`invoke="..."`** | Каждый invoke — публичный метод контроллера (п.1.4) |
| **Колонки таблиц** (id, наличие, `property`, рендереры) | Генераторы/style/description провайдеры навешиваются в Java по id колонок: `socialNetworkTable` → `socialNetworkLogoColumn`, `linkToWeb`, `networkName`(property `socialNetworkURL.socialNetwork`), `networkURLS`; `lastProjectTable` → `number`,`vacancy`,`max`,`lastInteraction`,`researcher`,`recruter`,`idViewIteractionsButton` (generator-атрибуты в XML 1231–1250); `suggestVacancyTable` → `notSendedIconColumn`, `vacansyName`; `jobCandidateIteractionListTable` → `icon`,`projectLogoColumn`,`numberIteraction`,`rating`,`vacancy`,`currentOpenCloseColumn`,`iteractionType`,`commentColumn`,`recrutier`,`dateIteraction`; `jobCandidateCandidateCvTable` → `projectLogoColumn`,`datePost`,`toVacancy`,`resumePosition`,`iconOriginalCVFile`,`iconHuntTechCVFile`,`letter`,`candidateOriginalCVColumn`,`candidateHuntTechCVColumn`,`createdBy`; `jobCandidateCommentsDataGrid` → `commentDialog`,`dateIteraction`,`recrutier`,`comment` |
| **`actions` таблиц / `editorEnabled`** | `socialNetworkTable` инлайн-редактирование (1129) + CRUD; `jobCandidateIteractionListTable` create/edit/remove/refresh + buttonsPanel-кнопки, ссылающиеся на `action="<table>.<action>"`; `jobCandidateCandidateCvTable` create/edit/remove |
| **`enable="false"` стартовые состояния** | `checkSkillFromJD` (1442), `scanContactsFromCVButton` (1513), `sendCommentButton` (1595), `openPositionProjectDescriptionButton` (1358), `frequentInteractionPopupButton` (1350), `copyCVButton` — Java включает их по контексту; смена начального `enable` меняет UX-логику |
| **`visible="false"` скрытых контрактов** | См. 3.3 |
| **Перенос компонентов между вкладками** | Ленивая инициализация (п.1.6) привязана к имени вкладки + `getWindow().getComponentNN`; формально id резолвится из всего окна, но порядок инициализации/пре-лоад-гейты лоадеров (`positionsTabLoaded`, `commentsTabInitialized` и т.д., 1317–1324) завязаны на логику «вкладка → её компоненты». Переносить можно только с полным пересмотром теста контракта и ручной проверкой |
| **`expand` на `jobCandidateMainLayout`/`jobCandidateWorkspace`/`tabSheetSocialNetworks`** | Меняет распределение высоты; при неверном значении таблицы схлопываются (риск косметический, но ломает визуальный контракт теста) |
| **Удаление/переименование `width="312px"` у `jobCandidateSidebar`** | Явно проверяется тестом контракта (строки 54–55 теста) |

### 3.3 Скрытые компоненты-«контракты» (XML 546–606) — НЕ удалять, НЕ переименовывать, НЕ расскрывать без согласования

`fullNameTextField` (свойство `fullName`!), `blockCandidateCheckBox` (свойство `blockCandidate`), `mobilePhoneLabel`, `emailLabel`, `skypuLabel`, `telegramLabel`, `skypeLinkButton`, `telegrammGroupLinkButton`, `personPositionTitle`, `emailTitle`, `phoneTitle`, `telegramTitle`, `skypeTitle`, `jobTitleTitle`, `skillBox` (HBox, в него рендерится Skillsbar-фрагмент, 466–470), `lastProjects` (groupBox), `dictionatysTavlesHBox` (grid).

`lastProjects`/`dictionatysTavlesHBox` — legacy-заглушки для `@Inject` (586–606); их удаление = сбой инжекции при старте экрана.

### 3.4 Ограничения теста `JobCandidateEditLayoutContractTest.java` (защищает presentation-контракт)

При XML-правках тест `./gradlew :app-core:test --no-daemon` сломается, если:
- нарушен порядок sidebar: `candidatePic → fullNameField → personPositionLabel → candidateProfileSummary → candidateNavigation → candidateProfileContacts → candidateSidebarSpacer → candidateProfileFooter` (строки 36–44 теста);
- `stylename="job-candidate-navigation label-navigation"` и `label-nav-item label-nav-item-active` отсутствуют (46–47);
- `jobCandidateSidebar` не 312px (54–55); нет `job-candidate-social-actions` (56); `tabResumeVbox`/`tabCommentsVbox` без `spacing="true" width="100%" height="100%"` (57–58); `jobCandidateBottomBar` без `width="100%"` (59–60); нет `cardAuditInfoButton` с `invoke="onCardAuditInfoClick"` (61–62);
- `jobCandidateMainSectionContent` не vbox или `personalDataBlock`/`professionalDataBlock` не внутри него (70–77); в «Основном» появляется `job-candidate-half-card` (84–85);
- в 7 SCSS-темах пропадают guard-правила (94–110).

---

## 4. Вывод

**Да, редизайн компоновки безопасно выполнить только через stylename/layout в XML и SCSS** — при соблюдении контракта:

1. **Не менять** ни одного `id`, `property`, `dataContainer`, `optionsContainer`, `invoke`, имени вкладки, колонки таблиц, `actions`, стартовых `enable`/`visible`-состояний и **не удалять** скрытые компоненты-контракты (п.3.3).
2. **Не трогать** секцию `<data>` (п.1.3) и view контейнера `jobCandidateDc` (п.2.1) — это фундамент, на котором держатся все ленивые загрузки и генераторы.
3. Все изменения — только: смена `stylename`/`caption`, добавление/замена layout-обёрток (vbox/hbox/grid/scrollBox) вокруг существующих элементов, размеры/отступы, SCSS (с обязательной синхронизацией 7 тем).
4. После правок прогнать `JobCandidateEditLayoutContractTest` + detached-тесты форм (паттерн `cuba-edit-form-contract-audit`) и вручную открыть форму (ленивые вкладки инициализируются только при переключении — проверять все 7 вкладок).
5. Жёсткие якоря, которые проще всего случайно задеть: `jobCandidateSidebar` 312px, порядок sidebar, tab-id'ы, `fullNameTextField`/`blockCandidateCheckBox` (скрытые, но биндятся), `socialNetworkTable` (живёт за счёт merge с `socialNetworkURLs-view`).

**Границы допустимого:** перестановка секций внутри вкладки, перегруппировка карточек, изменение высот/ширин, новые stylename-классы — безопасны. **Перенос компонентов между вкладками — под запретом без отдельного согласования** (ленивая инициализация и пре-лоад-гейты лоадеров завязаны на вкладку).

---

## Приложение А. Карта биндингов полей `dataContainer="jobCandidateDc"` (XML)

| Поле (id) | property | XML-строка |
|---|---|---|
| `candidatePic` | `fileImageFace` | 284–285 |
| `fileImageFaceUpload` | `fileImageFace` | 308, 313 |
| `cityOfCandidate` (label) | `cityOfResidence` | 378–379 |
| `currentCompanyLabel` (label) | `currentCompany` | 393–394 |
| `phoneLabel` (label) | `phone` | 496–497 |
| `fullNameTextField` (скрыт) | `fullName` | 550–551 |
| `blockCandidateCheckBox` (скрыт) | `blockCandidate` | 554–555 |
| `firstNameField` | `firstName` | 724–725 |
| `middleNameField` | `middleName` | 754–755 |
| `secondNameField` | `secondName` | 782–783 |
| `birdhDateField` | `birdhDate` | 807–808 |
| `jobCityCandidateField` | `cityOfResidence` + options `citiesDc` | 819–823 |
| `personPositionField` | `personPosition` + options `personPositionsDc` | 860–862 |
| `currentCompanyField` | `currentCompany` | 882–883 |
| `emailField` | `email` | 980–981 |
| `phoneField` | `phone` | 991–992 |
| `mobilePhoneField` | `mobilePhone` | 1002–1003 |
| `telegramNameField` | `telegramName` | 1013–1014 |
| `whatsupNameField` | `whatsupName` | 1050–1051 |
| `wiberNameField` | `wiberName` | 1061–1062 |
| `skypeNameField` | `skypeName` | 1072–1073 |
| `priorityCommunicationMethodRadioButton` | `priorityContact` | 1093–1094 |
| `historyCreatedByLabel` | `createdBy` | 1633–1634 |
| `historyCreateTsLabel` | `createTs` | 1641–1642 |

## Приложение Б. Связанные классы (не входят в компоновку формы)

- `JobCanidateDetailScreenFragment` — ScreenFragment, используется **в browse-экранах** (`JobCandidateBrowse.java:1024`, `PersonelReserveBrowse.java:1692`), не в JobCandidateEdit; редизайном edit-формы не затрагивается, но его собственный XML-контракт (`jobCandidatesDc` + 25+ @Inject id) защищён аналогично.
- `ContactInfo.java` — plain-Java парсер (email/phone/urls), к XML не привязан.
- `HistoryRowData.java` — DTO агрегации истории позиций (фоновый поток), к XML не привязан.
