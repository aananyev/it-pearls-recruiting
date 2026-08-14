# SkillTreeEdit (`hunttech_SkillTree.edit`)

Cross-links: [docs/entities/skill-tree/SkillTree.md](../entities/skill-tree/SkillTree.md) · общий контракт: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md) · эталон простого справочника по контракту: [GeolocationEditForms (country/region/city)](../screens/skill-tree/hunttech_SkillTree.edit_Spec.md)

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

Форма редактирует **элемент дерева компетенций** (`SkillTree`) — иерархический справочник навыков/технологий HRM HuntTech. Элементы дерева привязываются к вакансиям (`OpenPosition.skillsList`), резюме кандидатов (`CandidateCV.skillTree`), используются в фильтре кандидатов по навыкам и в парсинге текста резюме/вакансий (`PdfParserService`). Для каждого навыка форма хранит название, родителя в дереве, специализацию, приоритет категории (предметная область, фреймворки, методология, язык программирования), флаг исключения из парсинга, ссылку на статью Wikipedia, CSS-класс подсветки, логотип и rich-описание. Ссылка на Wiki позволяет одним нажатием загрузить описание статьи и её картинку прямо в форму.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из **SkillTreeBrowse** (`hunttech_SkillTree.browse`, меню «Дерево компетенций») стандартным образом: создание новой записи или «Изменить» по выбранному узлу дерева. Из формы возможны только picker-открытия: выбор родительского элемента (`skillTreeField`, коллекция корней `skillTreesDc`) и специализации (`specialisationField`, коллекция `specialisationDc`). Внутри формы навигация по разделам рабочей области («Основные данные», «Описание») дублируется пунктами label-навигации левой sidebar; визуальный блок sidebar содержит логотип навыка и кнопку его загрузки.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- **Открытие** → в sidebar показываются логотип, тип записи и название навыка; активен пункт «Основные данные», фокус на поле «Навык». Если запись новая или флаг не задан — `notParsing` приводится к `false`.
- **Парсинг Wiki** → пользователь вводит ссылку (поле `wikiPateField`), нажимает «Загрузить описание» → контроллер скачивает статью (Jsoup), кладёт HTML в rich-редактор «Описание навыка» и подставляет первую картинку статьи как превью логотипа. Кнопка активна только пока поле ссылки непустое.
- **Авто-ссылка по названию** → при изменении поля «Навык» контроллер предлагает диалогом подставить ссылку `https://ru.wikipedia.org/wiki/<название>` (если текущая ссылка отличается).
- **Сохранение** → стандартные кнопки OK/Отмена в правом нижнем углу; commit без дополнительных валидаторов.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| Controller ID | `hunttech_SkillTree.edit` (`@UiController`) |
| Дескриптор | `modules/web/src/com/company/hunttech/web/screens/skilltree/skill-tree-edit.xml` |
| Контроллер | `modules/web/src/com/company/hunttech/web/screens/skilltree/SkillTreeEdit.java` |
| Открытие | `SkillTreeBrowse` (StandardEditor: create / edit по дереву) |
| Режим окна | `dialogMode` width/height 100%×100%, modal (по контракту Edit-экранов) |
| Фокус при открытии | `skillNameField` (`focusComponent`) |
| Иконка окна | `CODE_FORK` |
| Сущность | `com.company.hunttech.entity.SkillTree`, view `skillTree-edit-view` |
| Права | стандартные CUBA CRUD на сущность |

## 2. Связь с моделью данных (Data & Entity Binding)

| Контейнер | Класс / view | Loader | Назначение |
|-----------|--------------|--------|------------|
| `skillTreeDc` | `SkillTree` / `skillTree-edit-view` | штатный (`<loader/>`) | редактируемая запись |
| `skillTreesDc` | `SkillTree` / `skillTree-picker-view` | `skillTreesLc` (cacheable) | корни дерева для «Элемент верхнего уровня» |
| `specialisationDc` | `Specialisation` / `specialisation-picker-view` | `specialisationDl` (cacheable) | варианты «Специализация» |

JPQL: `select e from hunttech_SkillTree e where e.skillTree is null order by e.skillName` (корни) и `select e from hunttech_Specialisation e order by e.specRuName` (специализации).

Основные bindings (по `property=` в XML): `skillName`, `prioritySkill`, `notParsing`, `skillTree`, `specialisation`, `wikiPage`, `styleHighlighting`, `comment` (LOB, в карточке «Описание навыка»), `fileImageLogo` (круглый `ovaFallbackImage skillPic` + upload `fileImageSkillUpload`).

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

```
SkillTreeBrowse (дерево компетенций)
 └─ SkillTreeEdit (fullscreen модальный диалог)
     ├─ Sidebar: визуальный блок (логотип + upload) → идентификация → label-навигация → подсказка
     ├─ Workspace: toolbar → карточки «Основные данные» и «Описание навыка» → footer OK/Отмена
     └─ Picker-открытия: родительский элемент (skillTreesDc), специализация (specialisationDc)
```

Дочерних фрагментов и диалогов форма не содержит (кроме программного диалога-подтверждения авто-ссылки Wiki). Footer использует стандартные actions `windowCommitAndClose` / `windowClose`.

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл формы (Lifecycle)

- `onInit` → вешается слушатель изменения текста «Навык» (предложение ссылки Wiki) и заполняются варианты приоритета (`setOptionsSkillPriorityField`, значения из `StandartPrioritySkills`).
- `onBeforeShow` → флаг `notParsing` приводится к `false` для новой записи и для записи с пустым значением.
- `onAfterShow` → если логотип (`fileImageLogo`) не задан — `applyFallback()` показывает fallback-аватар `icons/no-programmer.jpeg` (эталон IteractionListEdit); если логотип ещё не задан, а описание есть и непустое — превью берётся из первой картинки HTML-описания (`setLogo`). Пустое описание не вызывает парсинг Wiki — у новой записи остаётся fallback-аватар.

### 4.2 Скрытые вычисления (без явного клика)

- Приоритет навыка: `optionCaptionProvider` / `optionStyleProvider` подставляют название и цветовой стиль категории по числовому значению `prioritySkill` (−1..4, `StandartPrioritySkills`).
- Поле «Ссылка на Wiki» (`wikiPateField`) управляет кнопкой «Загрузить описание»: непустое значение → кнопка активна; пустое → скрыта.
- После успешного парсинга Wiki: rich-редактор заполняется HTML статьи, а превью логотипа — первой картинкой (`getPicFromWiki`).
- Загрузка файла логотипа через upload → сразу показывается в круглом `skillPic`; при отсутствии файла `ovaFallbackImage` показывает fallback-картинку `icons/no-programmer.jpeg`.

### 4.3 Валидация и сохранение

- Обязательное поле `skillName` (not null в сущности) проверяется штатным механизмом CUBA.
- Дополнительных бизнес-валидаторов нет; сохранение — стандартное commit + close.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Действие → Условие → Результат |
|---------|-------------------------------|
| Пункт label-навигации «Основные данные» (`skillTreeMainNav`, active по умолчанию) | Нажал пункт → фокус на поле «Навык» → пункт подсвечивается жёлтым (#ffb11b), «Описание» — обычный |
| Пункт label-навигации «Описание» (`skillTreeDescriptionNav`) | Нажал пункт → фокус на rich-редактор описания → пункт подсвечивается, «Основные данные» — обычный |
| Кнопка «Загрузить описание» (`parseWikiText`, invoke `parseWikiToDescription`) | Нажал → поле «Ссылка на Wiki» непустое → скачивание статьи (Jsoup), заполнение описания HTML и превью логотипа первой картинкой |
| Изменение «Навык» (`skillNameField`) | Ввёл текст → предлагаемая ссылка Wiki отличается от сохранённой → диалог «Внести туда новые данные?» → Да: подставить ссылку |
| Кнопка OK (`windowCommitAndClose`) | Нажал OK → проверка обязательных полей → commit → закрытие |
| Кнопка Отмена (`windowClose`) | Нажал Отмена → закрытие без сохранения |

## 6. Визуальная компоновка элементов (Visual Layout Schema)

```
window skill-tree-editor (100%×100%, modal)
└─ layout (expand=skillTreeMainLayout)
   └─ hbox skillTreeMainLayout (edit-screen-layout)
      ├─ vbox skillTreeSidebar (edit-sidebar, 312px, тёмная #172638→#0f1b28, padding 14px 16px 12px)
      │   ├─ visual (edit-sidebar-visual, прозрачный): круглый ovaFallbackImage skillPic (логотип 176×176, border-radius 50%, чистый круг без рамки — эталон JobCandidateEdit, fallback no-programmer.jpeg) + upload fileImageSkillUpload (dropZone; кнопки «Загрузить»/«Очистить» — пара 96×36, полупрозрачный белый фон, центрирована)
      │   ├─ identity (edit-sidebar-identity): title skillTreeSidebarTitle (skillName, жёлтый #ffb11b 18px) СВЕРХУ + subtitle «Навык» (12px/400) СНИЗУ
      │   ├─ label-navigation: заголовок-полоса «Разделы» (skill-tree-navigation-title, 36px, #ffb11b, inset-линии) + 2 пункта (27px/13px/600, active #ffb11b; :before halo-темы отключён display:none/content:none — текст центрирован, подсветка не смещается на строку выше)
      │   ├─ spacer (edit-sidebar-spacer)
      │   └─ hint (edit-sidebar-hint): подсказка о роли навыка
      └─ vbox skillTreeWorkspace (edit-workspace)
          ├─ toolbar (edit-toolbar): «Карточка навыка» (edit-toolbar-title) + пояснение (edit-toolbar-description)
          ├─ scrollBox skillTreeWorkspaceScroll (edit-workspace-scroll)
          │   └─ vbox skillTreeSections (edit-workspace-content skill-tree-content, отступы 8px 20px 24px — как iteractionListSections)
          │       ├─ groupBox skillTreeMainSection (edit-card, showAsPanel, «Основные данные», caption 13px/600)
          │       │   ├─ row1 (expand=skillNameField): skillNameField (edit-form-control) + skillPriorityField (220px, edit-form-control) + notParsingCheckBox
          │       │   ├─ row2: skillTreeField (50%, edit-form-control) + specialisationField (50%, edit-form-control)
          │       │   └─ row3 (expand=wikiPateField): wikiPateField (edit-form-control) + styleHighlightingField (240px, edit-form-control) + parseWikiText
          │       └─ groupBox skillTreeDescriptionSection (edit-card, showAsPanel, «Описание навыка», caption 13px/600)
          │           └─ richTextArea skillCommentRichTextArea (property comment, stylename skill-description-rich-text: высота calc(100vh − 230px), min-height 320px — почти на весь экран)
          └─ footer editActions (edit-footer-actions, 11px 20px): skillTreeActionsSpacer (expand) + skillTreeActionsGroup (AUTO, MIDDLE_RIGHT: windowCommitAndClose + windowClose)
```

### Стили и сообщения

- Локальный SCSS: `skill-tree-editor.scss` (7 тем, sha256-идентичны): тёмная sidebar, каноническая label-навигация (hover белый на rgba(255,255,255,.08), active #ffb11b на rgba(255,177,27,.12) с жёлтой border-left), полоса-заголовок навигации с inset-линиями, карточки-панели и поля 38px по контракту.
- Кнопки «Загрузить»/«Очистить» загрузчика логотипа (`fileImageSkillUpload`) — канон кнопок upload в тёмном sidebar (эталон JobCandidateEdit): пара 96×36, полупрозрачный белый фон `rgba(255,255,255,.06)`, рамка `rgba(255,255,255,.34)`, скругление 5px, светлый текст 14px/600 `#f8fafc`, центрирование flex'ом в блоке визуала (`justify-content: center`, зазор 10px; промежуточный `.c-fileupload-wrapper` растянут на ширину блока).
- Captions полей — `msg://` из `com.company.hunttech.web.screens.skilltree` (`msgSkill`, `msgPrioritySkill`, `msgNotParsing`, `msgSpecialisation`, `msgWikiPage`, `msgStyleHighlighting`, `msgParseWikiText`, `msgSkillComment`) и `mainMsg://msgSkillTree` («Элемент верхнего уровня»).
- Sidebar/toolbar-подписи («Навык», «Разделы», «Основные данные», «Описание», «Карточка навыка») — статические русские строки (как у гео-справочников).

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-12 | Аватар `skillPic` приведён к размеру 176×176 (width/height/ovalWidth/ovalHeight в XML + SCSS `.skill-tree-logo-image` во всех 7 темах) — в точности как `candidatePic` эталона JobCandidateEdit; контрактный тест обновлён (`ovalWidth/ovalHeight=176px`). |
| 2026-08-12 | Исправлено смещение подсветки label-навигации: halo-тема добавляет nav-кнопке `:before` (inline-block, vertical-align:middle), который при `display:flex` у `.v-button-wrap` выталкивает текст вниз («выделение на строку выше текста») — псевдоэлемент отключён `.label-nav-item:before/.v-button-label-nav-item:before { display:none; content:none }` во всех 7 темах; контрактный тест дополнен тремя проверками; CDP: текст по центру кнопки (delta=0). |
| 2026-08-12 | `richTextArea` «Описание навыка» расширена по вертикали до высоты экрана: вместо фикс. `height="280px"` — stylename `skill-description-rich-text` с высотой `calc(100vh − 230px)` и `min-height: 320px` (все 7 тем); обновлены §6 и комментарий XML. |
| 2026-08-12 | Аватар `skillPic` приведён к виду блока визуала эталона IteractionListEdit: геометрия 176px → 96×96 (XML `width/height/ovalWidth/ovalHeight` + SCSS `.skill-tree-logo-image` во всех 7 темах), убрана рамка 3px rgba(255,255,255,.90), фон и тень — чистый круг border-radius 50% как `candidateImage` эталона; контрактный тест обновлён (`ovalWidth/ovalHeight=96px`, проверка отсутствия рамки) |
| 2026-08-12 | Кнопки «Загрузить»/«Очистить» загрузчика логотипа приведены к канону тёмного sidebar (эталон JobCandidateEdit): пара 96×36, полупрозрачный белый фон `rgba(255,255,255,.06)`, рамка `rgba(255,255,255,.34)`, скругление 5px, светлый текст 14px/600 `#f8fafc`, центрирование flex'ом (`.c-fileupload-wrapper` растянут на ширину визуального блока) — вместо дефолтных белых Vaadin-кнопок; SCSS во всех 7 темах (sha256-идентичны); контрактный тест дополнен методом `uploadButtonsFollowCanonicalDarkSidebarStyle` |
| 2026-08-12 | Приведение к эталону IteractionListEdit, финальная доводка: аватар `skillPic` 96px → 176px (как candidatePic в JobCandidateEdit/CandidateCVEdit, SCSS `.skill-tree-logo-image` во всех 7 темах), пейкеры row2 (`skillTreeField`, `specialisationField`) — слот/обёртка/компонент явно 50%/100%/100% (эффективная 50/50 вместо 25%; убран `stylename=edit-form-control`, shared `width:100%!important` отменён локально), ряд 4 (чекбокс «Не участвовать в парсинге» + spacer + кнопка «Загрузить описание»), расширенные поля «Навык»/«Wiki» (expand больше не съедается чекбоксом и кнопкой), sidebar без сдвига (16,14) — padding только на `.edit-sidebar`, не на слоте |
| 2026-08-12 | Доводка до эталона IteractionListEdit: spacer sidebar `skillTreeSidebarSpacer` 100%×100%, контент рабочей области `stylename="edit-workspace-content skill-tree-content"` (отступы 8px 20px 24px как `iteractionListSections`), footer-композиция «expand-спейсер + группа AUTO/MIDDLE_RIGHT» (`skillTreeActionsGroup`), caption карточек 13px/600; контрактный тест расширен методом `visualContractFollowsIteractionListEditReference` |
| 2026-08-11 | Визуальный блок sidebar: прямоугольное image 140px заменено на круглый `ovaFallbackImage skillPic` (96px, border-radius 50%, рамка 3px rgba(255,255,255,.90), fallback `icons/no-programmer.jpeg` с object-view-box) — эталон IteractionListEdit; SCSS `.skill-tree-logo-image` во всех 7 темах (sha256-идентичны); контрактный тест дополнен проверками ovaFallbackImage |
| 2026-08-11 | Рефакторинг Edit-формы по контракту Edit-экранов: sidebar 270px (логотип + upload, идентификация, label-навигация «Разделы»), workspace с toolbar, карточки-панели edit-card/showAsPanel, edit-form-control на полях, footer по контракту; presentation-only навигация (focusMainSection/focusDescriptionSection); сохранены все id/bindings/loaders/actions/JPQL; локальный SCSS skill-tree-editor.scss в 7 темах |
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
