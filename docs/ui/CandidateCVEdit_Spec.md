# CandidateCVEdit — спецификация визуального и функционального контракта

**Проект:** HRM HuntTech
**Платформа:** CUBA Platform 7.3-SNAPSHOT
**UI controller:** `hunttech_CandidateCV.edit`
**Controller:** `modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java`
**Descriptor:** `modules/web/src/com/company/hunttech/web/screens/candidatecv/candidate-cv-edit.xml`
**Локальный SCSS namespace:** `.candidate-cv-editor`
**Статус:** label-навигация физически добавлена в XML для всех вкладок; проверка ожидает Hermes

## Назначение и бизнес-смысл (What & Why)

`CandidateCVEdit` объединяет подготовку кандидата к представлению заказчику: связывает кандидата с позицией и вакансией, хранит исходное и нормализованное резюме HRM HuntTech, поддерживает сопроводительное письмо, дерево навыков, фотографию и дополнительные файлы. Экран должен позволять рекрутеру последовательно подготовить полный пакет представления кандидата, не теряя исходные документы и не смешивая редактируемый текст резюме со справочными рекомендациями.

Визуальный рефакторинг приводит экран к общей UI/UX-концепции HRM HuntTech, уже применённой в `JobCandidateEdit` и `ExtSettingsWindow`. Цель — повысить читаемость и скорость работы пользователя без изменения бизнес-логики, lifecycle, загрузки данных, распознавания документов, сохранения или permissions.

## UI Context & Navigation

Экран открывается как modal editor сущности `CandidateCV` размером `1200×800 px`. Основные точки входа находятся в сценариях работы с резюме кандидата, представлениями на вакансии и связанных экранах `JobCandidate`.

Внутри формы сохраняются пять вкладок в исходном порядке:

1. `tabCandidate` — «Кандидат»: связи с кандидатом, позицией, вакансией и владельцем, ссылки и файлы резюме. Фотография вынесена в постоянную левую панель.
2. `tabCV` — «Резюме»: редактируемый текст резюме HRM HuntTech и рекомендации.
3. `tabLetter` — «Сопроводительное письмо»: письмо, комментарий и vacancy-dependent рекомендации.
4. `tabSkillTree` — дерево навыков, повторный разбор резюме и сопоставление с вакансией.
5. `tabFiles` — дополнительные composition-файлы `CandidateCV`.

Постоянная левая sidebar повторяет профильный паттерн `JobCandidateEdit` и обязательный порядок Edit-форм HRM HuntTech: фотография → ФИО → индивидуальная label-навигация активной вкладки → детализация резюме и вакансии → служебные элементы. Навигационные пункты физически объявлены в XML как borderless-кнопки с label-подобным оформлением. Правая workspace содержит `TabSheet` и footer. Ширина sidebar фиксирована на `312px`, а рабочая область расширяется на оставшееся пространство.

Связанные документы:

- [Общая UI/UX-концепция](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md);
- [JobCandidateEdit](JobCandidateEdit_Spec.md);
- [JobCandidate](../entities/job-candidate/JobCandidate.md);
- [OpenPosition](../entities/open-position/OpenPosition_Spec.md);
- [SkillTree](../entities/skill-tree/SkillTree.md).

## Behavior Summary

- открытие формы → `@LoadDataBeforeShow` инициирует стандартный lifecycle → pre-load listener не разрешает загрузить вакансии до установки фильтра пользователя;
- загрузка `candidateCVDc` → runtime-view содержит прямой `CandidateCV.fileImageFace` → единый `OvaFallbackImage` получает фотографию через стандартный data binding;
- `BeforeShow` → значение `TEXT_CV` сохраняется только как baseline → тяжёлый `RichTextArea` не инициализируется преждевременно;
- переключение вкладки → показывается только её статически объявленный navigation-container → остальные наборы скрыты;
- выбор пункта label-навигации → меняется active-style и вызывается `focus()` штатного компонента справа → данные и selected tab не изменяются;
- первый выбор `tabCV` → `cvTextInitialized == false` → текст CV, рекомендации и подсветка компетенций инициализируются один раз;
- первый выбор `tabSkillTree` → `skillTabInitialized == false` → CV инициализируется по необходимости, затем выполняется существующий разбор навыков;
- повторный выбор `tabSkillTree` → флаг уже установлен → лишняя повторная инициализация не выполняется;
- сохранение без открытия `tabCV` → `cvTextInitialized == false` → существующий `TEXT_CV` и `contactInfoChecked` не перезаписываются;
- изменение текста CV → значение отличается от baseline → `contactInfoChecked` сбрасывается и новый текст сохраняется;
- выбор кандидата в `candidateField` → меняется `candidateCVDc.candidate` → ФИО, текущая позиция и фотография в sidebar обновляются стандартным CUBA data binding без дополнительного loader;
- выбор должности в `resumePositionField` → меняется `candidateCVDc.resumePosition` → значение «Должность в резюме» в sidebar обновляется сразу;
- выбор вакансии в `candidateCVFieldOpenPosition` → меняется `candidateCVDc.toVacancy` → вакансия и проект в sidebar обновляются сразу, после чего контроллер читает `needLetter` и `templateLetter` и сохраняет существующую логику warning, шаблона и рекомендаций;
- включение «только мои подписки» → loader получает параметр `subscriber` → показываются доступные пользователю вакансии;
- выключение фильтра → параметр удаляется → выполняется существующая загрузка всех вакансий;
- ввод ссылки исходного резюме → значение не `null` → ссылка показывается и кнопка импорта текста становится enabled;
- загрузка PDF → существующий parser извлекает текст и изображения → при наличии изображений открывается окно выбора фотографии;
- загрузка DOCX → Apache POI извлекает текст → текст записывается в lazy-managed `candidateCVRichTextArea`;
- загрузка DOC → контроллер показывает существующее предупреждение о нереализованной функции → визуальный слой не меняет это поведение;
- фотография отсутствует, очищена или недоступна в storage → `OvaFallbackImage` показывает `icons/no-programmer.jpeg` без второго компонента и ручного `visible`;
- создание, редактирование или удаление дополнительного файла → используются actions таблицы → composition-коллекция сохраняется вместе с `CandidateCV`;
- hover, focus, disabled, read-only и validation → меняется только presentation → `visible`, `editable`, `enable`, `required`, validators и permissions остаются исходными.

## 1. Точка вызова и контекст

Форма является `StandardEditor<CandidateCV>`:

```java
@UiController("hunttech_CandidateCV.edit")
@UiDescriptor("candidate-cv-edit.xml")
@EditedEntityContainer("candidateCVDc")
@LoadDataBeforeShow
```

Диалоговый режим:

```xml
<dialogMode height="800"
            modal="true"
            width="1200"/>
```

Корневой presentation-контракт:

```xml
<layout stylename="candidate-cv-editor" ...>
```

`.candidate-cv-editor` не подключает `.job-candidate-editor` или `.ext-settings-window` и не создаёт глобальных Vaadin-селекторов.

## 2. Связь с моделью данных

### 2.1. Контейнеры и loaders

| ID | Назначение | Контракт |
|---|---|---|
| `candidateCVDc` | редактируемая сущность `CandidateCV` | `candidateCV-view` с прямым `fileImageFace` и существующими вложенными properties |
| `someFilesesDc` | composition дополнительных файлов | `property="someFiles"` |
| `skillTreesDc` | composition дерева навыков | `property="skillTree"`, `skillTree-cv-tab-view` |
| `openPositionsDc` | варианты вакансий | `openPosition-candidate-cv-picker-view` |
| `openPositionsDl` | loader вакансий | JPQL и parameter `subscriber` не изменены |
| `resumePositionsDc` | справочник позиций | `_local` |
| `resumePositionsLc` | loader позиций | исходный JPQL не изменён |
| `usersDc` | владельцы резюме | `_local` |
| `usersDl` | loader пользователей | исходный JPQL не изменён |

### 2.2. Поля `candidateCV-view`

Экран сохраняет `candidateCV-view` и его расширение в XML. В функциональном контракте присутствуют:

- `resumePosition`;
- `toVacancy`, `positionType`, `projectName`, `projectDepartment`, `grade`;
- `owner`;
- `fileCV`;
- `originalFileCV`;
- `someFiles`, `fileType`, `fileDescriptor`, `fileOwner`;
- `fileImageFace`;
- `skillTree`;
- `candidate`, `candidate.fileImageFace`, `candidate.personPosition`.

Верхнеуровневый `CandidateCV.fileImageFace` обязателен в runtime-view для data binding `candidatePic` и существующей логики сохранения фотографии. Вложенный `candidate.fileImageFace` относится к связанной сущности `JobCandidate` и не загружает фотографию самого резюме.

Глобальный `views.xml`, entity `CandidateCV`, JPQL и loaders не изменяются; исправление локализовано в runtime-view контейнера экрана.

## 3. Рабочая карта UI-контрактов

Обозначения: «—» — контракт отсутствует; «Java» — компонент инъецируется или обрабатывается контроллером.

| Component ID | Тип | Вкладка/область | Data container / property | Actions / invoke | Java / состояние | Required | Бизнес-назначение |
|---|---|---|---|---|---|---|---|
| `tabSheet` | `TabSheet` | форма | — | selected-tab listener | Java; имена вкладок читаются lifecycle | — | навигация по пяти разделам |
| `candidateCvMainLayout` | `HBox` | корень формы | — | — | presentation | — | sidebar слева, workspace справа |
| `candidateCvSidebar` | `VBox` | левая панель | — | — | постоянная область | — | контекст кандидата на всех вкладках |
| `candidateLabel` | `HBox` | sidebar / профиль | — | — | presentation | — | фото, ФИО и текущая позиция |
| `iteractionListLabelCandidate` | `Label` | sidebar / профиль | `candidateCVDc / candidate.fullName` | — | live data binding | — | ФИО |
| `iteractionListLabelPosition` | `Label` | sidebar / профиль | `candidateCVDc / candidate.personPosition` | — | live data binding | — | текущая позиция кандидата |
| `candidateCvSidebarResumePosition` | `Label` | sidebar / целевое резюме | `candidateCVDc / resumePosition` | — | live data binding | — | должность в резюме |
| `candidateCvSidebarVacancy` | `Label` | sidebar / целевое резюме | `candidateCVDc / toVacancy` | — | live data binding | — | вакансия представления |
| `candidateCvSidebarProject` | `Label` | sidebar / целевое резюме | `candidateCVDc / toVacancy.projectName` | — | live data binding | — | проект вакансии |
| `candidateCvSectionNavigation` | `VBox` | sidebar / после ФИО | — | статические XML-кнопки | Java синхронизирует active tab | — | label-навигация блоков текущей вкладки |
| `candidateCvCandidateNavigation` | `VBox` | `tabCandidate` | — | 3 navigation invoke | focus существующих полей | — | основные данные и документы |
| `candidateCvCvNavigation` | `VBox` | `tabCV` | — | 2 navigation invoke | focus редактора/рекомендаций | — | текст резюме |
| `candidateCvLetterNavigation` | `VBox` | `tabLetter` | — | 4 navigation invoke | optional visibility | — | письмо, комментарий и рекомендации |
| `candidateCvSkillNavigation` | `VBox` | `tabSkillTree` | — | 2 navigation invoke | focus button/TreeDataGrid | — | проверка и дерево навыков |
| `candidateCvFilesNavigation` | `VBox` | `tabFiles` | — | 1 navigation invoke | focus Table | — | дополнительные файлы |
| `candidateCvWorkspace` | `VBox` | правая область | — | — | expand `tabSheet` | — | вкладки и footer |
| `labelLastRecrutier` | `Label` | sidebar / служебный контекст | — | — | существующее значение | — | служебная информация |
| `machRegexpFromCV` | `Label` | sidebar / служебный контекст | — | — | Java записывает email/телефон | — | результат распознавания |
| `quoteTextArea` | `TextArea` | sidebar / служебный контекст | — | — | Java задаёт текст; `visible=false`, `editable=false` | — | существующая цитата |
| `candidateScrolBox` | `ScrollBox` | `tabCandidate` | — | — | presentation | — | прокрутка вкладки кандидата |
| `candidateVbox` | `HBox` | `tabCandidate` | — | — | presentation | — | одноколоночная рабочая композиция |
| `groupBox` | `VBox` | `tabCandidate` | — | — | presentation | — | основная рабочая область на всю ширину |
| `candidateField` | `SuggestionPickerField` | основные данные | `candidateCVDc / candidate` | `lookup`, `open`, suggestion query | Java injection | yes | выбор кандидата |
| `resumePositionField` | `LookupPickerField` | основные данные | `candidateCVDc / resumePosition`; `resumePositionsDc` | `lookup` | — | yes | позиция резюме |
| `candidateCVFieldOpenPosition` | `LookupPickerField` | основные данные | `candidateCVDc / toVacancy`; `openPositionsDc` | `lookup`, `open`; option icon/style providers | Java injection, `@Subscribe`, `@Install` | no | вакансия |
| `onlyMySubscribeCheckBox` | `CheckBox` | рядом с вакансией | — | value listener | Java enabled/value/filter logic | no | фильтр вакансий пользователя |
| `СandidateCVField` | `LookupField` | основные данные | `candidateCVDc / owner`; `usersDc` | — | legacy ID с кириллической `С` | no | владелец |
| `textFieldIOriginalCV` | `TextField` | оригинальное резюме | `candidateCVDc / linkOriginalCv` | — | Java injection, `@Subscribe` | no | ссылка на источник |
| `loadToCVTextArea` | `Button` | оригинальное резюме | — | `invoke="loadToCVTextArea"` | Java injection; initially disabled | — | импорт веб-текста |
| `originalCVLink` | `Link` | оригинальное резюме | — | URL из Java | Java show/hide | — | открыть источник |
| `fileOriginalCVField` | `FileUploadField` | оригинальное резюме | `candidateCVDc / originalFileCV` | upload events | Java `@Subscribe`; immediate; clear | — | загрузить PDF/DOC/DOCX |
| `textFieldHuntTechCV` | `TextField` | резюме HRM HuntTech | `candidateCVDc / linkHuntTechCV` | — | Java injection, `@Subscribe` | no | ссылка на подготовленное CV |
| `HuntTechCVLink` | `Link` | резюме HRM HuntTech | — | URL/visibility из Java | Java show/hide | — | открыть подготовленное CV |
| `fileCVField` | `FileUploadField` | резюме HRM HuntTech | `candidateCVDc / fileCV` | upload | immediate; clear | — | файл подготовленного CV |
| `dropZone` | `VBox` | sidebar / фото | — | upload drop zone | `dropZone="dropZone"` | — | область drag-and-drop профиля |
| `picVBox` | `VBox` | фото | — | — | единый компонент изображения | — | frame фотографии |
| `candidatePic` | `OvaFallbackImage` | фото | `candidateCVDc / fileImageFace` | встроенный fallback | Java-инъекция базового `Image`; без ручного visibility | — | фотография или theme fallback |
| `fileImageFaceUpload` | `FileUploadField` | фото | `candidateCVDc / fileImageFace` | upload | Java injection; immediate; clear | — | загрузка фотографии |
| `rescanSkills` | `Button` | `tabCV` toolbar | — | `rescanCV` | — | — | повторный разбор |
| `resumeRecognitionButton` | `Button` | `tabCV` toolbar | — | `resumeRecognition` | — | — | распознать контакты |
| `convertToTextButton` | `Button` | `tabCV` toolbar | — | `convertToText` | Java injection; dynamic enabled | — | HTML/text toggle |
| `showOriginalButon` | `Button` | `tabCV` toolbar | — | `showOriginalText` | Java injection; dynamic caption | — | оригинал/подсветка |
| `candidateCVRichTextArea` | `RichTextArea` | `tabCV` | lazy `TEXT_CV` management | value change | Java injection; role-based editable | yes | основной редактор CV |
| `cvResomandation` | `RichTextArea` | `tabCV` | — | — | Java fills; `editable=false` | no | рекомендации по резюме |
| `questionLetterRichTextArea` | `RichTextArea` | `tabLetter` | — | — | Java show/hide; `visible=false`, `editable=false` | no | vacancy template |
| `letterRichTextArea` | `RichTextArea` | `tabLetter` | `candidateCVDc / letter` | — | Java injection | no | сопроводительное письмо |
| `commentLetterRichTextArea` | `RichTextArea` | `tabLetter` | `candidateCVDc / commentLetter` | — | existing binding | no | комментарий |
| `letterRecommendation` | `RichTextArea` | `tabLetter` | — | — | Java show/hide/fill; `visible=false`, `editable=false` | no | рекомендации |
| `rescanResume` | `Button` | `tabSkillTree` toolbar | — | `rescanCV` | — | — | перестроить дерево |
| `checkSkillFromJD` | `Button` | `tabSkillTree` toolbar | — | `checkSkillFromJD` | — | — | сравнить с вакансией |
| `skillTreesTable` | `TreeDataGrid` | `tabSkillTree` | `skillTreesDc` | column generators/style | Java `@Install` | — | иерархия навыков |
| `someFilesTable` | `Table` | `tabFiles` | `someFilesesDc` | `add/create/edit/remove` | actions сохранены | — | дополнительные файлы |
| `datePostField` | `DateField` | footer | `candidateCVDc / datePost` | — | new record default from Java | no | дата представления |
| `editActions` | `HBox` | footer | — | `windowCommitAndClose`, `windowClose` | стандартные actions | — | сохранение и отмена |

## 4. Lifecycle и защищённые динамические состояния

### 4.1. Lazy-init

Контроллер проверяет именно имена:

```java
"tabCV".equals(selectedTab.getName())
"tabSkillTree".equals(selectedTab.getName())
```

Поэтому сохраняются:

- `tabSheet`;
- `tabCV`;
- `tabSkillTree`;
- порядок selected-tab event;
- расположение `candidateCVRichTextArea` внутри `tabCV`;
- расположение `skillTreesTable` внутри `tabSkillTree`;
- отсутствие новых listener, loader или автозапуска.

### 4.2. Статическая label-навигация вкладок

`candidate-cv-edit.xml` содержит пять отдельных navigation-container и все кликабельные пункты. Контроллер не удаляет XML-компоненты и не создаёт им замену через `UiComponents`. Пункты оформлены как borderless-кнопки, поскольку CUBA `Label` не имеет click event, но визуально соответствуют вертикальному label-индексу `SettingsWindow`.

Обязательный порядок sidebar:

1. `candidatePic` — визуальный образ;
2. `iteractionListLabelCandidate` — наименование экземпляра;
3. `candidateCvSectionNavigation` — label-навигация;
4. `candidateCvSidebarTargetCard` — детализация основных элементов;
5. `candidateCvSidebarMetaCard` и spacer — прочее по необходимости.

Наборы вкладок:

- `tabCandidate`: основные данные, оригинальное резюме, резюме HRM HuntTech;
- `tabCV`: текст резюме, рекомендации;
- `tabLetter`: шаблон вакансии, письмо, внутренний комментарий, рекомендации;
- `tabSkillTree`: проверка навыков, дерево навыков;
- `tabFiles`: дополнительные файлы.

`syncSidebarSectionNavigation()` управляет только visibility контейнеров. Навигация не вызывает `tabSheet.setSelectedTab()`, не меняет `cvTextInitialized`/`skillTabInitialized`, не запускает loaders и не изменяет значения entity.

Геометрия пунктов label-навигации выровнена по эталону `JobCandidateEdit` (1:1 с блоком `.label-nav-item` в `job-candidate-editor.scss`): пункт — `min-height: 27px`, `height: auto`, `padding: 3px 10px`, `line-height: 20px`, `font-weight: 600`, `opacity: 1`; контейнер навигации — `padding-top: 6px`, `padding-bottom: 2px`, `display: block`, `min-width: 0`; caption наследует `line-height` пункта (без локального override). Правило: `candidate-cv-editor.scss`, идентично во всех 7 темах.

### 4.2.1. Заголовки разделов sidebar (полоса-заголовок)

Заголовки «Разделы вкладки» (`candidate-cv-navigation-title`) и «Резюме для вакансии» (`candidate-cv-sidebar-card-title`) оформлены как полоса-заголовок по контракту `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` §4.1: две горизонтальные inset-линии (белая сверху `rgba(255,255,255,1) 0 1px 0 0 inset`, светлая снизу `rgba(244,244,244,1) 0 -1px 0 0 inset`), разделитель снизу `border-bottom: 1px solid rgba(255,255,255,.14)`, полоса `rgba(255,255,255,.045)`, текст `#ffb11b` 15px/700, `min-height: 36px`, `padding: 7px 11px`; заголовок карточки растянут на её ширину (`margin: -14px -14px 12px`, верхние углы скруглены `8px 8px 0 0`). Правило: `candidate-cv-editor.scss`, идентично во всех 7 темах.

### 4.3. Visibility

Контроллер динамически показывает или скрывает:

- `originalCVLink`;
- `HuntTechCVLink`;
- `questionLetterRichTextArea`;
- `letterRecommendation`.

Фотография больше не участвует в ручном переключении visibility: `OvaFallbackImage` сохраняет одну геометрию и самостоятельно выбирает bound resource или fallback. XML не делает остальные скрытые компоненты видимыми статически.

### 4.4. Enabled, editable и required

Контроллер изменяет:

- enabled `loadToCVTextArea`;
- enabled `convertToTextButton`;
- editable `candidateCVRichTextArea` для групп `ACCOUNTING` и `MANAGEMENT`.

XML сохраняет:

- `candidateField required="true"`;
- `resumePositionField required="true"`;
- `candidateCVRichTextArea required="true"`;
- `questionLetterRichTextArea editable="false"`;
- `cvResomandation editable="false"`;
- `letterRecommendation editable="false"`.

## 5. Actions, invoke и table contracts

### 5.1. Picker actions

- `candidateField`: `picker_lookup`, `picker_open`;
- `resumePositionField`: `picker_lookup`;
- `candidateCVFieldOpenPosition`: `picker_lookup`, `picker_open`.

`optionIconProvider` и `optionStyleProvider` вакансии остаются в Java без изменений.

### 5.2. Invoke

| Component | Invoke |
|---|---|
| `loadToCVTextArea` | `loadToCVTextArea` |
| `rescanSkills` | `rescanCV` |
| `resumeRecognitionButton` | `resumeRecognition` |
| `convertToTextButton` | `convertToText` |
| `showOriginalButon` | `showOriginalText` |
| `rescanResume` | `rescanCV` |
| `checkSkillFromJD` | `checkSkillFromJD` |

### 5.3. SkillTree

Сохраняются:

- `dataContainer="skillTreesDc"`;
- `hierarchyColumn="skillName"`;
- `hierarchyProperty="skillTree"`;
- колонки `skillName`, `specialisation`, `wikiPage`, `isComment`;
- widths, maximumWidth, sort, renderers и `rowsCount`;
- Java generators `wikiPage`, `isComment` и style provider.

### 5.4. Дополнительные файлы

Сохраняются колонки:

- `fileDescription`;
- `fileType.nameFileType`;
- `fileComment`;
- `fileDescriptor.size`;
- `fileOwner.name`.

Сохраняются actions `add`, `create`, `edit`, `remove` и buttons `someFilesTable.create`, `someFilesTable.edit`, `someFilesTable.remove`.

## 6. Upload и parsing

### 6.1. Оригинальное резюме

`fileOriginalCVField` сохраняет:

- `dataContainer="candidateCVDc"`;
- `property="originalFileCV"`;
- `fileStoragePutMode="IMMEDIATE"`;
- `showFileName="true"`;
- `showClearButton="true"`.

Фактическое поведение контроллера:

- PDF: извлечение текста и изображений;
- DOCX: извлечение текста через Apache POI;
- DOC: warning о том, что функция загрузки пока не реализована.

### 6.2. Резюме HRM HuntTech

`fileCVField` сохраняет binding `fileCV`, immediate upload, filename и clear button.

### 6.3. Фотография

`candidatePic` является единым `OvaFallbackImage` размером `176×176 px`, сохраняет binding `candidateCVDc / fileImageFace`, upload `fileImageFaceUpload` и `dropZone="dropZone"`. Круглая геометрия задаётся `ovalWidth`/`ovalHeight`, `SCALE_DOWN` исключает искажение, а `fallbackThemePath="icons/no-programmer.jpeg"` обслуживает отсутствие, очистку и недоступность файла.

`candidateFaceDefaultImage`, `setCandidatePicImage()` и `@Subscribe("candidatePic")` удалены. Контроллер сохраняет совместимую инъекцию `Image candidatePic` и программную установку `FileDescriptorResource` после выбора изображения из PDF; `OvaFallbackImage` наследует базовый CUBA `Image`.

## 7. Визуальная компоновка

### 7.1. Исходная композиция

Исходный экран использовал:

- обычный `GroupBox` для верхнего контекста;
- `TabSheet` с базовым `framed`-оформлением;
- поля вкладки «Кандидат» шириной `70%`;
- отдельную photo drop zone справа;
- toolbar без локальной визуальной системы;
- таблицу дополнительных файлов высотой `300px`;
- footer без выраженного отделения.

### 7.2. Новая композиция

```text
┌──────────────────────┬──────────────────────────────────────────────┐
│ фото кандидата       │ Кандидат · Резюме · Письмо · Навыки · Файлы│
│ ФИО                  ├──────────────────────────────────────────────┤
│ текущая позиция      │ карточки / редактор 4:1 / таблицы           │
│                      │                                              │
│ Должность в резюме   │                                              │
│ Вакансия             │                                              │
│ Проект               ├──────────────────────────────────────────────┤
│ служебный контекст   │ дата представления       сохранить · отмена  │
└──────────────────────┴──────────────────────────────────────────────┘
```

Изменения presentation:

- постоянная левая панель шириной `312px` оформлена в стиле `JobCandidateEdit`: тёмный градиент, круглая фотография и акцент `#ffb11b`; Vaadin slot имеет тот же размер, поэтому workspace не заезжает под sidebar;
- фотография и upload находятся в sidebar; `candidatePic` использует единый `OvaFallbackImage` без параллельного fallback-компонента;
- ФИО, текущая позиция, должность резюме, вакансия и проект привязаны непосредственно к `candidateCVDc`; изменения picker-полей отображаются сразу без ручного копирования значений;
- `TabSheet` и footer находятся только в правой workspace, поэтому sidebar остаётся непрерывной по всей высоте;
- вкладки имеют высоту `48px`, заметное selected-состояние и полностью видимые captions без ellipsis; при нехватке места работает локальный горизонтальный overflow `TabSheet`;
- основные данные и документы собраны в локальные карточки радиусом `8px`; фотография вынесена в постоянный профиль sidebar;
- picker-поля, suggestion/input controls и upload/clear-кнопки занимают доступную ширину, имеют общий локальный control-стиль и не обрезают actions;
- `onlyMySubscribeCheckBox` остаётся непосредственно под vacancy picker;
- photo drop zone использует профильную геометрию `196×238px`, единый `OvaFallbackImage` имеет размер и oval-геометрию `176×176px`;
- `tabCV` и `tabLetter` сохраняют пропорцию `4:1`;
- toolbar имеет минимальную высоту `58px`, кнопки — не менее `38px`;
- `RichTextArea` и таблицы используют доступную высоту;
- footer постоянно находится вне прокручиваемого содержимого вкладки;
- primary presentation применяется только к `windowCommitAndClose`, action не меняется.

## 8. Локальный SCSS и темы

Для каждой темы существует файл:

```text
modules/web/themes/<theme>/com.company.hunttech/candidate-cv-editor.scss
```

Он подключается в соответствующий `styles.scss` через:

```scss
@import "com.company.hunttech/candidate-cv-editor";
@include candidate-cv-editor-theme;
```

Поддерживаемые темы:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

SCSS использует `$v-app-background-color`, `$v-panel-background-color`, `$v-font-color`, `$v-selection-color`. Vaadin-селекторы допустимы только внутри `.candidate-cv-editor`. Анимации, тяжёлые изображения и глобальные overrides не добавляются.

Локально оформлены:

- hover и selected tab;
- focus;
- disabled;
- read-only;
- validation error;
- required indicator;
- picker actions и кнопки;
- upload и filenames;
- link;
- checkbox;
- RichTextArea;
- table/grid header и selection;
- TreeDataGrid hierarchy;
- footer.

## 9. Сохранённые функциональные контракты

Минимальное изменение контроллера ограничено удалением ручного fallback. Неизменными остаются:

1. `CandidateCV.java` и `views.xml`;
2. entity metadata, БД, Liquibase и SQL;
3. loaders, JPQL и loader parameters;
4. data containers и properties;
5. component ID `candidatePic`, captions и имена вкладок;
6. picker actions и `invoke`;
7. validators, required, permissions и editable;
8. upload properties и drop zone;
9. типы `RichTextArea`, picker, upload, `Table`, `TreeDataGrid`, `TabSheet`;
10. распознавание, parsing, рекомендации, письмо, SkillTree и сохранение;
11. lazy-init `tabCV` и `tabSkillTree`;
12. сохранение существующего `TEXT_CV`, если вкладка CV не открывалась.

## 10. Регрессионная проверка

`CandidateCVEditVisualContractTest` проверяет:

- Git blob SHA контроллера, entity и `views.xml`;
- все защищённые legacy ID;
- порядок пяти вкладок;
- lifecycle-имена `tabCV` и `tabSkillTree`;
- bindings, optionsContainer, picker actions;
- invoke;
- upload properties и `dropZone`;
- единый `OvaFallbackImage`, fallback path и отсутствие manual visibility;
- actions таблицы файлов;
- неизменные JPQL-фрагменты;
- наличие `.candidate-cv-editor`, `.candidate-cv-sidebar` и `.candidate-cv-workspace-shell`;
- live bindings sidebar: `candidate.fullName`, `candidate.personPosition`, `resumePosition`, `toVacancy`, `toVacancy.projectName`;
- подключение SCSS во всех семи темах;
- отсутствие зависимости от `.job-candidate-editor` и `.ext-settings-window`;
- отсутствие top-level глобальных Vaadin-селекторов.

`CandidateCVEditPhotoViewContractTest` дополнительно проверяет, что `fileImageFace` является прямым property runtime-view `candidateCVDc`, а `candidatePic` и `fileImageFaceUpload` сохраняют binding `candidateCVDc / fileImageFace`.

## 11. Обязательные проверки Hermes

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.CandidateCVEditVisualContractTest' \
          --tests 'com.company.hunttech.core.CandidateCVEditPhotoViewContractTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидания:

- `CandidateCVEditVisualContractTest`: PASS;
- `CandidateCVEditPhotoViewContractTest`: 2/2 PASS;
- `ScreenViewIntegrityTest`: 8/8 PASS;
- Data View Integrity: PASS;
- SCSS build: PASS;
- `BUILD SUCCESSFUL`;
- local deploy: PASS;
- HTTP `/hrm/`: 200;
- critical Tomcat errors: NONE.

До отчёта Hermes compile, tests, SCSS, assemble, local deploy, functional smoke и visual smoke имеют статус `NOT VERIFIED`.

## 12. Functional smoke

Проверяются сценарии исходного задания:

- открытие без преждевременного CV/SkillTree процесса;
- пять вкладок в исходном порядке;
- candidate, position и vacancy pickers;
- немедленное обновление sidebar при смене кандидата, должности резюме и вакансии;
- фильтр подписок, option icons/styles;
- owner, ссылки, оба upload и clear;
- открытие существующего `CandidateCV` с фотографией → фото отображается без `instantiatingValueholderWithNullSession`;
- открытие существующего `CandidateCV` без фотографии → отображается fallback без unfetched-ошибки;
- photo upload, clear, повторное открытие, drop zone и fallback;
- lazy CV, PDF/DOCX parsing, warning для DOC, recognition, text conversion и original view;
- role-based read-only и сохранение `TEXT_CV`;
- письмо, комментарий, vacancy template и recommendation;
- первый и повторный вход в SkillTree, rescanning и comparison;
- create/edit/remove дополнительных файлов;
- `datePost`, commit-and-close и cancel;
- сохранение без открытия CV.

## 13. Visual smoke

В каждой из семи тем:

- hard reload с отключённым browser cache;
- `.candidate-cv-editor` присутствует в DOM;
- локальный SCSS присутствует в собранном `styles.css`;
- tabs имеют заметное selected-состояние;
- picker actions не обрезаны;
- filename и clear button видимы;
- sidebar занимает полную высоту, не перекрывает workspace и сохраняет ширину;
- фото не искажено и отображается круглым;
- ФИО, текущая позиция, должность резюме, вакансия и проект синхронно меняются при изменении соответствующих picker-полей;
- RichTextArea используют доступную высоту;
- recommendation-панели читаемы;
- SkillTree не имеет необоснованной горизонтальной прокрутки;
- таблица файлов и footer доступны;
- скрытые элементы не оставляют крупных пустот;
- focus, disabled и read-only различимы;
- другие формы не получают локальные стили CandidateCVEdit;
- Tomcat logs не содержат новых критических ошибок.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-16 | Кнопки экрана CandidateCVEdit: текст внутри всех кнопок выровнен по центру (nav-пункты sidebar, toolbar, footer, popupButton, document-карточки — `text-align: center`, иконка по вертикали с отступом `margin-right: 6px`); навигации добавлены тематические пиктограммы FontAwesome: Основные данные — `USER`, Исходное резюме — `FILE_TEXT_O`, Резюме HuntTech — `FILE_WORD_O`, Текст резюме — `ALIGN_LEFT`, Рекомендации — `COMMENT`, Шаблон письма — `FILE_TEXT_O`, Письмо — `SEND`, Комментарий — `COMMENT`, Действия с навыками — `MAGIC`, Дерево навыков — `SITEMAP`, Файлы — `FOLDER_O`; toolbar: «Пересканировать резюме» — `REPEAT`, «Проверить навыки» — `SEARCH`; footer: ОК — `CHECK`, Отмена — `TIMES` (существовавшие `DOWNLOAD`/`MAGIC`/`BARS` сохранены). Только SCSS+XML в 4 канонических темах (halo, havana, hover, hunttech-modern), Antigravity-темы (helium и др.) не менялись; Java, бизнес-логика не менялись; контрактный тест `CandidateCVEditVisualContractTest` обновлён |
| 2026-08-16 | Заголовок «Основные навыки» в sidebar приведён к стилю остальных заголовков тем — полоса-заголовок `candidate-cv-skills-title` (шрифт 15px/700, цвет `#ffb11b`, min-height 36px, две горизонтальные inset-линии над и под текстом + `border-bottom` — контракт §4.1, 1:1 с «Разделы»); все заголовки тем sidebar («Резюме для вакансии», «Разделы», «Основные навыки») переведены в ЗАГЛАВНЫЕ буквы (`text-transform: uppercase`); XML: заголовку навыков добавлен локальный класс полосы; только SCSS-форматирование в 4 канонических темах (halo, havana, hover, hunttech-modern), Antigravity-темы (helium и др.) не менялись; Java, бизнес-логика не менялись; контрактный тест `CandidateCVEditVisualContractTest` обновлён |
| 2026-08-16 | Блок «Резюме для вакансии» в sidebar приведён к образцу `JobCandidateEdit` (единый стиль с дизайном Antigravity): убрана рамка-карточка блока (было `border 1px`, `background rgba(255,255,255,0.055)`, `border-radius 8px`, `padding 14px` → стало `border: 0`, `background: transparent`, `padding: 0 0 10px`), заголовок блока стал лаконичным (28px/14px/700 вместо полосы 36px/15px/700 с inset-линиями и отрицательным margin), добавлен стиль подписей `.candidate-cv-sidebar-field-label`; label-навигация дополнена фиксом высоты пункта 46px→27px (скрыт `.v-button:before` — по эталону `job-candidate-editor.scss`, приёмка 2026-08-03); только SCSS-форматирование в 4 канонических темах (halo, havana, hover, hunttech-modern), Antigravity-темы (helium и др.) уже имели этот вид; XML, Java, бизнес-логика не менялись; контрактный тест `CandidateCVEditVisualContractTest` обновлён |
| 2026-08-16 | Отступы и интервалы label-навигации sidebar приведены к эталону `JobCandidateEdit` (1:1 с блоком `.label-nav-item` в `job-candidate-editor.scss`): контейнер `padding-top: 6px` + `padding-bottom: 2px` (было 10px сверху, без нижнего), пункты `min-height: 27px` (было 38px), `padding: 3px 10px` (было 8px 10px), `line-height: 20px` (было 18px), `font-weight: 600`, `opacity: 1`, caption без локального `line-height`; заголовок навигации получил `height: auto`. Только SCSS-форматирование label-навигации: XML, Java, бизнес-логика и остальные компоненты формы не менялись; контрактный тест `CandidateCVEditVisualContractTest` обновлён на новый канон |
| 2026-08-16 | «AI-нотификации 2 раза»: перед анализом навыков показывается стартовая исчезающая TRAY-нотификация «Запущен AI-анализ навыков резюме…» (`AiOperationNotifier.showStarted`, 5 с, обещание итоговой с моделью и собственником API) |
| 2026-08-16 | Контракт пользовательской нотификации: нотификация «Статистика анализа навыков» (TRAY, автоскрытие 5 с) дополнена блоком «какая модель что сделала + собственник API» от `AiOperationNotifier` (модель, провайдер, корпоративный/личный ключ); `SkillAnalysisService` возвращает `SkillAnalysisResult` с метаданными AI-выполнения (при классическом fallback блок не показывается) |
| 2026-08-08 | Заголовкам разделов sidebar «Разделы вкладки» (`candidate-cv-navigation-title`) и «Резюме для вакансии» (`candidate-cv-sidebar-card-title`) добавлены две горизонтальные inset-линии полосы (белая сверху, светлая снизу) + разделитель `border-bottom`, как у заголовков секций OpenPositionEdit/IteractionListEdit — контракт §4.1; SCSS `candidate-cv-editor.scss` во всех 7 темах; добавлен контрактный тест `CandidateCVEditVisualContractTest.sectionTitlesHaveTwoInsetLinesLikeInfoCaption`. |
| 2026-08-03 | Левая панель CandidateCVEdit доведена до общего Edit-контракта: sidebar расширен до 312px во всех семи темах, пункты навигации выровнены до 38px, tab captions видны полностью без ellipsis, input/picker/upload controls растягиваются одной шириной и используют единый визуальный стиль. Бизнес-логика, Java, loaders, bindings и actions не менялись. |
| 2026-07-26 | Исправлена незавершённая label-навигация: все пункты статически объявлены в XML, закреплён порядок sidebar «образ → наименование → навигация → детализация → прочее», добавлены invoke-handler и контрактные проверки |
| 2026-07-26 | `candidatePic` переведён на единый `OvaFallbackImage` 176×176 px с `icons/no-programmer.jpeg`; удалены `candidateFaceDefaultImage`, `setCandidatePicImage()` и ручное переключение visibility; обновлены оба контрактных теста фотографии |
| 2026-07-25 | В runtime-view `candidateCVDc` добавлен верхнеуровневый `CandidateCV.fileImageFace`; устранён вызов unfetched getter в `setCandidatePicImage()` и добавлен отдельный Data View Integrity тест фотографии |
| 2026-07-25 | Добавлена постоянная левая панель в стиле `JobCandidateEdit`: фотография, ФИО, текущая позиция, должность резюме, вакансия и проект; значения синхронизируются через прямой `candidateCVDc` data binding без изменения контроллера |
| 2026-07-25 | Выполнен строго визуальный рефакторинг `CandidateCVEdit`: добавлена карточная компоновка, постоянный контекст кандидата, локальный namespace для семи тем, регрессионная защита legacy ID, bindings, actions, invoke, upload, lazy-init и неизменности Java/entity/views |
