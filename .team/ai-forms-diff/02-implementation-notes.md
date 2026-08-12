# Отчёт о приведении Edit-форм AI-конфигурации к эталону IteractionListEdit

Дата: 2026-08-12
Формы: `AiFunctionConfigurationEdit`, `AdminAiConfigurationEdit`, `UserAiConfigurationEdit`, `UserAiFunctionOverrideEdit` (CUBA 7.3, HRM HuntTech).

## 1. Что сделано по каждой форме

### 1.1 AiFunctionConfigurationEdit (`ai-function-configuration-edit.xml`)
- **Полоса-заголовок навигации**: label «Разделы» получил `stylename="label-nav-title ai-function-configuration-navigation-title"` (контракт §4.1; общий `.label-nav-title` не тронут).
- **edit-form-control добавлен** на 4 поля, где его не было: `descriptionField` (textArea), `adminConfigurationField` (lookupPickerField), `systemPromptField` (textArea), `promptTemplateField` (textArea). Итого 15/15 полей ввода покрыты.
- **Caption через msg-ключи** добавлены всем 15 полям (включая 2 checkBox): `aiFunctionCode.caption` … `aiFunctionConfigurationVersion.caption`.
- **Русские inline-комментарии** перед каждым открывающим элементом (xml-screen-documentation.mdc).
- **Не менялось**: id, property, dataContainer, optionsContainer, loaders, views, JPQL, actions (lookup/clear), invoke (`focusMainSection` и др.), dialogMode 100%×100%, Java-контроллер (`AiFunctionConfigurationEdit.java` — focus-методы не тронуты).

### 1.2 AdminAiConfigurationEdit (`admin-ai-configuration-edit.xml`)
- **Полоса-заголовок навигации**: `stylename="label-nav-title admin-ai-configuration-navigation-title"`.
- **edit-form-control добавлен** на 3 поля: `lastTestStatusField` (textField, read-only), `lastErrorField` (textArea, read-only), `apiKeyInput` (passwordField; по-прежнему БЕЗ `property=`, шифрование в middleware).
- **Caption через msg-ключи** всем полям (включая checkBox `activeField`): `adminName.caption` … `adminNewApiKey.caption`. Жёсткая подпись «Новый API-ключ» заменена на `msg://adminNewApiKey.caption`.
- **Русские inline-комментарии** перед каждым открывающим элементом.
- **Не менялось**: id, property, dataContainer, loaders, views, actions, invoke, `onBeforeCommitChanges` (шифрование secret), dialogMode, Java-контроллер.

### 1.3 UserAiFunctionOverrideEdit (`user-ai-function-override-edit.xml`)
- **Полоса-заголовок навигации**: `stylename="label-nav-title user-ai-function-override-navigation-title"`.
- **edit-form-control добавлен** на 2 lookupPickerField: `aiFunctionField`, `userAiConfigurationField`. Итого 3/3 полей ввода.
- **Caption через msg-ключи**: `overrideAiFunction.caption`, `overrideEnabled.caption`, `overrideUserAiConfiguration.caption`, `overrideModelName.caption`.
- **Русские inline-комментарии** перед каждым открывающим элементом.
- **Не менялось**: id, property, dataContainer, optionsContainer, loaders, JPQL (`:user` row scope, `executionPolicy <> 'ADMIN_ONLY'` в CDATA), views, actions, invoke (`focusMainSection`/`focusModelSection`), dialogMode, Java-контроллер (включая `onBeforeCommitChanges` и detached-read).

### 1.4 UserAiConfigurationEdit (`user-ai-configuration-edit.xml`) — полная перестройка
- **Было**: legacy-диалог `450 × AUTO, forceDialog="true"`, один `<form>` с 4 полями, без sidebar/cards/toolbar/footer-контракта.
- **Стало** (по образцу `vacancy-prompt-template-edit.xml`):
  - dialogMode → `100% × 100% modal`;
  - двухпанельная композиция: `hbox mainLayout (edit-screen-layout)` → sidebar `270px (edit-sidebar)`: identity (subtitle + живой providerCode в title) → label-navigation с полосой «Разделы» (`label-nav-title user-ai-configuration-navigation-title`) → spacer → hint;
  - workspace: `edit-toolbar` (заголовок + описание) → `edit-workspace-scroll` → `edit-workspace-content` → 2 карточки `groupBox showAsPanel="true" stylename="edit-card"`: «Основное» (`providerCodeField`, `defaultModelNameField`, `isActiveField`) и «Безопасность» (`apiKeyField`);
  - footer `edit-footer-actions` со spacer и кнопками `commitAndCloseBtn`/`closeBtn` (actions `windowCommitAndClose`/`windowClose` сохранены);
  - **все 4 поля** получили `dataContainer="userAiConfigurationDc"` (ранее наследовали от `<form>`), `property`, `stylename="edit-form-control"` и caption через msg-ключи (`userProviderCode.caption`, `userDefaultModelName.caption`, `userIsActive.caption`, `userApiKey.caption`).
- **Навигация display-only**: у контроллера нет focus-методов (Java менять запрещено), поэтому пункты «Основное»/«Безопасность» — некликабельные label-элементы `label-nav-item` (паттерн fallback-пунктов эталона `iteraction-list-edit.xml`); первый помечен `label-nav-item-active` статично (Java стили не трогает — конфликта removeStyleName нет).
- **Русские inline-комментарии** перед каждым открывающим элементом.
- **Не менялось**: `userAiConfigurationDc` (id/class/view), loader, все id полей, property-биндинги, actions, Java-контроллер (`UserAiConfigurationEdit.java`), messages-ключи browse/edit captions.

## 2. SCSS partial (7 тем)

Созданы 4 partial (в каждой из 7 тем, байт-идентичны, sha256 = 1 уникальный хэш на форму):

| Файл (`com.company.hunttech/{form}.scss`) | mixin | root-класс | navigation-title |
|---|---|---|---|
| `ai-function-configuration-editor.scss` | `ai-function-configuration-editor-theme` | `.ai-function-configuration-editor` | `ai-function-configuration-navigation-title` |
| `admin-ai-configuration-editor.scss` | `admin-ai-configuration-editor-theme` | `.admin-ai-configuration-editor` | `admin-ai-configuration-navigation-title` |
| `user-ai-configuration-editor.scss` | `user-ai-configuration-editor-theme` | `.user-ai-configuration-editor` | `user-ai-configuration-navigation-title` |
| `user-ai-function-override-editor.scss` | `user-ai-function-override-editor-theme` | `.user-ai-function-override-editor` | `user-ai-function-override-navigation-title` |

Содержимое (по образцу `vacancy-prompt-template-editor.scss` + `iteraction-list-editor.scss`/`iteraction-list-visual-alignment.scss`):
- тёмная sidebar: `background-color: #172638` + `background-image: linear-gradient(180deg, #172638 0%, #132130 58%, #0f1b28 100%)` — **раздельными свойствами** (шорткат `background:` компилируется невалидно);
- каноническая навигация: пункты `min-height: 27px`, padding `3px 10px`, 13px/600, hover `#fff` на `rgba(255,255,255,.08)`, active `#ffb11b` на `rgba(255,177,27,.12)` + `border-left-color: #ffb11b`, radius `0 5px 5px 0`; `:before` скрыт (`display:none; content:none`) — вало-трюк центрирования выталкивал бы caption при flex;
- полоса-заголовок навигации: scoped `.label-navigation .{form}-navigation-title` — min-height 36px, padding `7px 11px`, `#ffb11b` 15px/700, bg `rgba(255,255,255,.045)`, border-bottom `rgba(255,255,255,.14)`, inset-линии `box-shadow: rgba(255,255,255,1) 0 1px 0 0 inset, rgba(244,244,244,1) 0 -1px 0 0 inset` (общий `.label-nav-title` не менялся);
- карточки `edit-card`: padding 0, radius 8px, border/shadow по эталону; `.v-groupbox-caption`/`.v-panel-caption` — min-height 50px, 17px/700, bg mix 68%, border-bottom; `.v-panel-content` — bg panel, border 0;
- поля: 38px/15px/line-height 38px, border `rgba($v-font-color,.20)`, radius 5px, shadow none; textArea — 15px + собственные рамки; подписи `.v-caption .v-captiontext` — 13px/600 mix 72%; focus-ring `rgba($v-selection-color,.20)`; readonly bg mix 62%; checkBox padding 3px 0, label 14px mix 78%;
- кнопки: `.edit-card .v-button` и `.edit-footer-actions .v-button` — min-height 38px, radius 5px.

Подключение в `styles.scss` **всех 7 тем** (halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark):
- `@import "com.company.hunttech/{form}"` — сразу после `@import "com.company.hunttech/vacancy-prompt-template-editor";` (верхний уровень);
- `@include {form}-theme;` — сразу после `@include vacancy-prompt-template-editor-theme;` (внутри корневого селектора темы).
Проверено grep'ом: import=1, include=1 в каждой теме.

## 3. Messages

Добавлены caption-ключи в `messages.properties` и `messages_ru.properties` (EN + RU) для всех 4 форм:
- aifunctionconfiguration: 15 ключей (`aiFunctionCode.caption` … `aiFunctionConfigurationVersion.caption`);
- adminaiconfiguration: 9 ключей (`adminName.caption` … `adminNewApiKey.caption`);
- useraiconfiguration: 4 ключа (`userProviderCode.caption`, `userDefaultModelName.caption`, `userIsActive.caption`, `userApiKey.caption`);
- useraifunctionoverride: 4 ключа (`overrideAiFunction.caption`, `overrideEnabled.caption`, `overrideUserAiConfiguration.caption`, `overrideModelName.caption`).
Существующие ключи (`browseCaption`, `editorCaption`, `userAiConfigurationBrowse.caption`, `userAiConfigurationEdit.caption`) не изменены.

## 4. Подтверждение: бизнес-логика/bindings не тронуты

- **Java-контроллеры не изменялись**: `AiFunctionConfigurationEdit.java`, `AdminAiConfigurationEdit.java`, `UserAiConfigurationEdit.java`, `UserAiFunctionOverrideEdit.java` — без единой правки (focus-методы, detached-read, BeforeCommitChanges, AiProviderCatalog-логика сохранены).
- **XML**: не менялись id, property, dataContainer, optionsContainer, loaders, views, JPQL/CDATA, actions, invoke, required/editable/visible, dialogMode у 3 форм (у UserAiConfigurationEdit dialogMode изменён на 100%×100% modal — разрешено заданием).
- **Entity/views/БД**: не менялись.
- **Контракт-тест** `AiControlPlaneScreenContractTest` — все assert-строки проверены grep'ом и присутствуют:
  - 3 формы: `stylename="edit-screen-layout"`, `stylename="edit-sidebar"`, `width="312px"`, `stylename="label-navigation"`, `label-nav-title`, `label-nav-item label-nav-item-active`, `stylename="edit-workspace"`, `stylename="edit-footer-actions"`, `showAsPanel="true"`;
  - admin edit: `<passwordField id="apiKeyInput"` без `property=`;
  - override edit: `e.user = :user and e.isActive = true`, `e.executionPolicy <> 'ADMIN_ONLY'`, `view="user-ai-configuration-override-picker-view"`, отсутствие `property="apiKey"`.
- **Эталон IteractionListEdit и его SCSS** не изменялись; другие формы не затрагивались (все новые правила scoped под корневые классы 4 форм).

## 5. Верификация

- XML всех 4 форм: `xml.dom.minidom.parse` — PASS; `xml.etree.ElementTree.parse` — PASS.
- Поля: 100% полей ввода (textField/textArea/lookupField/lookupPickerField/passwordField) имеют `edit-form-control`; checkBox — caption, без edit-form-control (контракт).
- Карточки: все groupBox имеют `showAsPanel="true"` и caption; navigation-title присутствует во всех 4 XML.
- sha256: каждая форма — 1 уникальный хэш на 7 тем (28 файлов).
- styles.scss: import+include подтверждены во всех 7 темах.
- Брейсы partial: `{` = `}` (30/30).
- Комментарии XML: русские, смысловые, перед каждым открывающим элементом (проверка покрытия: 40/32/30/26 комментариев).
- НЕ запускались: gradle (buildScssThemes/restart) — по заданию это делает оркестратор.
- НЕ коммитилось в git.

## 6. Список изменённых/созданных файлов

**XML (4):**
- `modules/web/src/com/company/hunttech/web/screens/aifunctionconfiguration/ai-function-configuration-edit.xml`
- `modules/web/src/com/company/hunttech/web/screens/adminaiconfiguration/admin-ai-configuration-edit.xml`
- `modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/user-ai-configuration-edit.xml` (перестройка)
- `modules/web/src/com/company/hunttech/web/screens/useraifunctionoverride/user-ai-function-override-edit.xml`

**Messages (8):**
- `.../aifunctionconfiguration/{messages,messages_ru}.properties`
- `.../adminaiconfiguration/{messages,messages_ru}.properties`
- `.../useraiconfiguration/{messages,messages_ru}.properties`
- `.../useraifunctionoverride/{messages,messages_ru}.properties`

**SCSS partial (28 = 4 формы × 7 тем):**
- `modules/web/themes/{halo,havana,helium,hover,hunttech-modern,hunttech-modern-light,hunttech-modern-dark}/com.company.hunttech/ai-function-configuration-editor.scss`
- `.../admin-ai-configuration-editor.scss`
- `.../user-ai-configuration-editor.scss`
- `.../user-ai-function-override-editor.scss`

**styles.scss (7):**
- `modules/web/themes/{halo,havana,helium,hover,hunttech-modern,hunttech-modern-light,hunttech-modern-dark}/styles.scss`

**Документация (.team/ai-forms-diff/):**
- `01-differences-table.md` (90 пунктов: 80 по AiFunctionConfigurationEdit + 10 по UserAiConfigurationEdit)
- `02-implementation-notes.md` (этот файл)

## 7. Известные ограничения / на усмотрение владельца

- Ширина sidebar 4 форм следует shared-контракту (`edit-sidebar` 270px, 250px ≤1366px), как у образца vacancy-prompt-template; XML `width="312px"` у 3 форм перекрывается shared-CSS (та же ситуация, что и у эталона IteractionListEdit, где SCSS 272px важнее XML 312px). Локальное закрепление ширины в partial не выполнялось, чтобы не создавать рассинхрон «слот ↔ корень» (питфолл visual-alignment).
- Цветовое выделение primary/secondary кнопок footer не добавлялось (вне объёма задания); кнопки получили эталонную геометрию min-height 38px через partial.
- После `buildScssThemes`/деплоя рекомендуется CDP-сверка по метрикам `00-reference-metrics.md` (sidebar фон rgb(23,38,56), пункты 27px, полоса 36px, caption 17px/700, поля 38px/15px, подписи 13px/600).
