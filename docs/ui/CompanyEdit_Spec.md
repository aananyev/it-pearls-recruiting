# CompanyEdit — редактирование компании (`hunttech_Company.edit`)

> Сущность: [Company.md](../entities/company/Company.md) · Legacy Spec: [hunttech_Company.edit_Spec.md](../screens/company/hunttech_Company.edit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Карточка компании HRM HuntTech: реквизиты юридического лица / компании-клиента (наименование, форма собственности, группа компаний, директор), география (город → регион → страна с каскадным автозаполнением), адрес, логотип, LOB-описания (описание компании и условия работы) и состав подразделений. Компания используется как FK текущего работодателя кандидата, заказчика в проектах и родителя подразделений, поэтому форма должна быстро открываться и подгружать тяжёлые поля лениво.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из browse-списков `hunttech_Company.browse` (и специализированных `OurCompany` / `ClientsCompany`) кнопками «Создать»/«Изменить», а также из карточки кандидата (поле «Компания» в `JobCandidateEdit`). Внутри формы — три вкладки TabSheet: «Информация о компании» (карточки реквизитов и адреса), «Описание компании» (LOB-редакторы), «Департамент» (dataGrid подразделений с CRUD). Пункты sidebar-навигации «Разделы» дублируют вкладки.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие существующей записи → LOB-поля (адрес, описание, условия работы) и подразделения подгружаются лениво при первом переключении на соответствующую вкладку (флаги `addressLoaded`/`companyDescriptionLoaded`/`departmentsLoaded`).
- Новая запись → `ourClient = false`; адрес не грузится.
- Смена города → контроллер через `dataManager.reload` (views `city-location-view`/`region-browse-view`) автоматически подставляет регион и страну; смена региона — страну.
- Логотип: загрузка IMMEDIATE проходит умный конвейер (общий с ProjectEdit — `WebProjectLogoFileUploadField` + `ProjectLogoImageProcessingService`, конфиг `hunttech.projectLogo.*`): PNG, ресайз до 300×300, удаление белого фона (rembg/AI/классика), вписывание в круг — затем пишет файл в `fileCompanyLogo`; `WebOvaFallbackImage` сам показывает fallback `icons/no-company.png` при отсутствии файла. Повторная загрузка без закрытия экрана обрабатывается заново.
- Сохранение/отмена — стандартные `windowCommitAndClose`/`windowClose` в правом нижнем углу footer.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_Company.edit` |
| **Java-класс** | `com.company.hunttech.web.screens.company.CompanyEdit` |
| **XML-дескриптор** | `company-edit.xml` |
| **messagesPack** | `com.company.hunttech.web.screens.company` |
| **Базовый класс** | `StandardEditor<Company>` |
| **EditedEntityContainer** | `companyDc` |
| **focusComponent** | `companyOwnershipField` |
| **dialogMode** | `height="100%" width="100%" modal="true"` (контракт §5.3) |
| **Загрузка данных** | `@LoadDataBeforeShow` |
| **Иконка окна** | `BUILDING` |

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Company` (таблица `HUNTTECH_COMPANY`) |
| **View контейнера** | `company-edit-view` (без LOB; `departmentOfCompany` обязателен — nested container) |
| **Instance** | `companyDc` + вложенный collection `departmentOfCompanyDc` (`property="departmentOfCompany"`) |
| **Collections** | `companyOwnershipsDc` (_minimal), `companyDirectorsDc` (person-picker-view, cacheable), `companyGroupDc` (companyGroup-picker-view, cacheable), `cityOfCompaniesDc` (city-location-view, cacheable), `regionOfCompaniesDc` (region-browse-view, cacheable), `countryOfCompaniesDc` (country-picker-view, cacheable) |

JPQL-загрузчики справочников сохранены 1:1 (`hunttech_Ownershup`, `hunttech_Person`, `hunttech_CompanyGroup order by companyRuGroupName`, `hunttech_City`, `hunttech_Region`, `hunttech_Country`).

Property-биндинги полей: `ourLegalEntity`, `ourClient`, `companyOwnership`, `comanyName`, `companyShortName`, `companyGroup`, `companyDirector`, `cityOfCompany`, `regionOfCompany`, `countryOfCompany`, `addressOfCompany`, `companyDescription`, `workingConditions`, `fileCompanyLogo`; колонки dataGrid — `departamentRuName`, `departamentDirector`, `departamentHrDirector`.

Критичные nested paths из Java: `cityOfCompany` → `cityRegion` → `regionCountry` (обработчики value-change грузят `city-location-view` / `region-browse-view`). View integrity: все инжектируемые id (9 компонентов) присутствуют в XML.

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `hunttech_Company.browse` / `OurCompany` / `ClientsCompany` | create / edit action |
| Внешний создатель | `JobCandidateEdit` (поле «Компания») | commit дочернего редактора → merge в DataContext кандидата |
| Lookup targets | picker_lookup на FK-полях (собственность, директор, город/регион/страна) | `screenBuilders.lookup()` |
| Дочерние формы | `hunttech_CompanyDepartament.edit` (из dataGrid подразделений) | create / edit action dataGrid |

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл (Lifecycle)

- `onBeforeShowSidebar` (presentation): динамический title sidebar — `comanyName` либо `browseCaption` («Компании») для новой записи.
- `onAfterShow`: новая запись → `ourClient = false`; существующая → первая ленивая загрузка адреса.
- `onMainTabSelectedTabChange` (бизнес): ленивая загрузка LOB/подразделений по вкладкам (существующая запись).
- `onMainTabSelectedTabChangeNav` (presentation, отдельный обработчик): синхронизация активного пункта sidebar-навигации.

### 4.2 Скрытые вычисления

- Sidebar title: наименование компании по центру (жёлтый `#ffb11b`, 18px/700).
- Активный пункт навигации «Разделы»: `label-nav-item-active` управляется контроллером (`setNavigationActive`/`resetNavigationActiveStyles`), базовый класс `label-nav-item` не снимается.
- Видимость навигации (контракт §3.6, эталон OpenPositionEdit): контейнер `label-navigation` показывается только на вкладках с двумя и более блоками ввода (`tabConpanyDetails` — 2 карточки); одноблочные вкладки («Описание компании» — карточка LOB, «Департамент» — dataGrid) скрывают навигацию целиком вместе с заголовком; контейнер и кнопки остаются в XML/Java как контракт инжекции (`TABS_WITH_SIDEBAR_NAVIGATION`).
- Логотип: `WebOvaFallbackImage` сам читает `fileCompanyLogo` из контейнера и показывает fallback `icons/no-company.png` при отсутствии файла; ручные переключатели пары image (legacy `setCompanyPicImage`, `companyDefaultLogoFileImage`) удалены.

### 4.3 Валидация и сохранение

Обязательные поля: `cityOfCompanyField`, `regionOfCompanyField`, `countryOfCompanyField` (`required="true"`). Сохранение — стандартный commit `StandardEditor`; дополнительных BeforeCommit-обработчиков нет.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Цепочка |
|---------|---------|
| Пункт навигации «Разделы» (`companyEditorNavMain/Description/Departments`) | Клик → активный пункт (`label-nav-item-active`) → `mainTab.setSelectedTab(...)` соответствующей вкладки; навигация видима только на вкладке `tabConpanyDetails` (правило 3.6) |
| Вкладка TabSheet | Смена → (бизнес) ленивая загрузка LOB, если ещё не грузилась; (presentation) активный пункт навигации |
| Загрузчик логотипа (`companyLogoFileUpload`) | Выбор файла → умная обработка (`WebProjectLogoFileUploadField`: PNG, ресайз 300px, удаление белого фона, круг) → IMMEDIATE в `fileCompanyLogo` → OvaFallbackImage перечитывает контейнер; «Очистить» → сброс значения; повторная загрузка в том же экране обрабатывается заново |
| `cityOfCompanyField` | Выбор города → reload `city-location-view` → `regionOfCompany`; если регион найден → reload `region-browse-view` → `countryOfCompany` |
| `regionOfCompanyField` | Выбор региона → reload `region-browse-view` → `countryOfCompany` |
| Footer «Сохранить и закрыть» / «Отмена» | `windowCommitAndClose` (primary) / `windowClose` (secondary), правый нижний угол |

## 6. Визуальная компоновка элементов (Visual Layout Schema)

Двухпанельная компоновка `edit-screen-layout` (контракт §4–5, эталон ProjectEdit):

- **Sidebar 270px** (`company-editor` namespace, тёмный `#172638`, `padding: 14px 16px 12px`, border-right + тень, тонкий скроллбар):
  1. visual `edit-sidebar-visual` — `ovaFallbackImage companyLogoFileImage` 176×176 (ovalBackground `#3a3e44`, fallback `icons/no-company.png`, `SCALE_DOWN`) + upload 96×36 (канон §4.1);
  2. identity `edit-sidebar-identity` — живой title `companySidebarTitle` по центру, без подписи типа записи;
  3. навигация `label-navigation` — полоса-заголовок «Разделы» (`label-nav-title company-editor-navigation-title`, inset-линии §4.1) + 3 пункта 27px (`label-nav-item`, активный `#ffb11b`); пункты по высоте контента (`height: auto`, перенос длинной подписи), локальное wrap-правило с принудительной высотой отсутствует (контракт §3.1); на одноблочных вкладках контейнер скрывается (правило 3.6);
  4. spacer `edit-sidebar-spacer` (100%×100%) + hint `edit-sidebar-hint`.
- **Workspace**: toolbar (`edit-toolbar-title` 20px + `edit-toolbar-description`), tabSheet `edit-tabs` (НЕ framed; общие стили тем), вкладки:
  - `tabConpanyDetails` — scrollBox `edit-workspace-scroll` → `edit-workspace-content` → карточки `edit-card`+`showAsPanel`: `companyMainCard` («Реквизиты компании») и `companyAddressCard` («Адрес компании»);
  - `companyDescriptionTab` — карточка `companyDescriptionCard` с двумя RichTextArea;
  - `tabCompanyDepartament` — карточка `companyDepartmentsCard` с dataGrid `departmentOfCompanyTable` + buttonsPanel.
  - Строки парных полей внутри workspace сохраняют две колонки при достаточной ширине и переносятся в одну без горизонтального выхода при сужении окна. Sidebar остаётся фиксированной панелью 270px: его размер, состав и компоновка не участвуют в адаптивном правиле.
- **Footer** `edit-footer-actions`: expand-спейсер + группа AUTO/MIDDLE_RIGHT (`spacing="true"`) → `company-editor-primary-action` / `company-editor-secondary-action` (40px/14px/600).

Локальный SCSS: `company-editor.scss` в 7 темах (md5-идентичны), подключение `@import` + `@include company-editor-theme` в `styles.scss` каждой темы. Новые msg-ключи: `msgCompanySidebarHint`, `msgCompanyToolbarDescription`, `msgCompanyMainSection`, `msgCompanyAddressSection`, `msgCompanyDescriptionSection`, `msgCompanyDepartmentsSection`, `msgCompanyGroup` (в `screens/company/messages.properties` и `messages_ru.properties`); исправлена опечатка `msgCompanyDetail` (→ «Информация о компании») в главных messages.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-09-02 | Исправлена адаптивная компоновка правой части: `company-main-tab` больше не наследует высоту полей ввода; внутренние `.v-expand/.v-slot` строк hbox возвращаются в normal flow с переносом. Размер, компоновка и содержимое sidebar не изменялись. |
| 2026-08-15 | Умная обработка логотипа компании (как у проекта): `WebProjectLogoFileUploadField` расширен на свойство `fileCompanyLogo` — PNG, ресайз 300px, удаление белого фона (rembg/AI/классика), вписывание в круг; конфиг общий `hunttech.projectLogo.*`; повторная загрузка без закрытия экрана обрабатывается заново (фикс кэша `processedDescriptor`) |
| 2026-08-14 | Сверка с эталоном (контракт §3.1/§3.6/§4.1): пункты навигации по высоте контента (`height: auto`), удалено локальное wrap-правило с принудительной высотой, полоса-заголовок `height: auto`; реализовано правило 3.6 — `label-navigation` скрывается на одноблочных вкладках (`TABS_WITH_SIDEBAR_NAVIGATION`) |
| 2026-08-14 | Рефакторинг по контракту Edit-форм (эталон ProjectEdit): sidebar 270px с логотипом `ovaFallbackImage` 176×176 и навигацией «Разделы» (вкладки), карточки `edit-card`+`showAsPanel`, footer primary/secondary, `dialogMode` 100%×100% modal; `WebOvaFallbackImage` вместо пары image, presentation-навигация в Java; `company-editor.scss` ×7 тем; новый `CompanyEditLayoutContractTest` (7/7) |
