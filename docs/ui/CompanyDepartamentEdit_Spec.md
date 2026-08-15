# CompanyDepartamentEdit — редактирование департамента (`hunttech_CompanyDepartament.edit`)

> Сущность: [CompanyDepartament.md](../entities/company-departament/CompanyDepartament.md) · Legacy Spec: [hunttech_CompanyDepartament.edit_Spec.md](../screens/company-departament/hunttech_CompanyDepartament.edit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Карточка подразделения (департамента) внутри компании-клиента HRM HuntTech: название, компания-владелец, директор и директор по персоналу, количество открытых позиций, краткое описание (LOB), состав проектов департамента (composition) и шаблон сопроводительного письма (LOB). Департамент связывает проекты с организационной структурой заказчика (FK `Project.projectDepartment`), поэтому форма должна быстро открываться и подгружать тяжёлые поля лениво.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из browse-списка `hunttech_CompanyDepartament.browse` (кнопки «Создать»/«Изменить») и из карточки компании `CompanyEdit` (dataGrid подразделений на вкладке «Департамент»). Внутри формы — три вкладки TabSheet: «Проект» (карточки реквизитов и краткого описания), «Открытые позиции» (таблица проектов с CRUD), «Информация в сопроводительном письме» (LOB-редактор). Пункты sidebar-навигации «Разделы» дублируют вкладки.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие существующей записи → LOB-поля (краткое описание, шаблон письма) и проекты подгружаются лениво при первом переключении на соответствующую вкладку (флаги `departamentDescriptionLoaded`/`templateLetterLoaded`/`projectsLoaded`).
- Новая запись → тяжёлые поля не грузятся вообще.
- Sidebar-навигация «Разделы»: клик по пункту переключает вкладку; смена вкладки синхронизирует активный пункт (presentation-only, эталон ProjectEdit/CompanyEdit).
- Title sidebar — название департамента (динамически подставляется контроллером, для новой записи — «Департамент»).
- Сохранение/отмена — стандартные `windowCommitAndClose`/`windowClose` в правом нижнем углу footer.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_CompanyDepartament.edit` |
| **Java-класс** | `com.company.hunttech.web.screens.companydepartament.CompanyDepartamentEdit` |
| **XML-дескриптор** | `company-departament-edit.xml` |
| **messagesPack** | `com.company.hunttech.web.screens.companydepartament` |
| **Базовый класс** | `StandardEditor<CompanyDepartament>` |
| **EditedEntityContainer** | `companyDepartamentDc` |
| **focusComponent** | `departamentRuNameField` (был legacy `form` — удалён) |
| **dialogMode** | `height="100%" width="100%" modal="true"` (контракт §5.3) |
| **Загрузка данных** | `@LoadDataBeforeShow` |
| **Иконка окна** | `GROUP` |

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `CompanyDepartament` (таблица `HUNTTECH_COMPANY_DEPARTAMENT`) |
| **View контейнера** | `companyDepartament-edit-view` (без LOB; `projectOfDepartment` обязателен — nested container) |
| **Instance** | `companyDepartamentDc` + вложенный collection `companyDepartamentProjectOfDepartmentsDc` (`property="projectOfDepartment"`) |
| **Collections** | `companyNamesDc` (company-picker-view, cacheable), `departamentHrDirectorsDc` (person-picker-view, cacheable), `departamentDirectorsDc` (person-picker-view, cacheable) |

JPQL-загрузчики справочников сохранены 1:1 (`hunttech_Company` без «(не использовать)» + order by name; `hunttech_Person` order by secondName, firstName).

Property-биндинги полей: `departamentRuName`, `companyName`, `departamentHrDirector`, `departamentDirector`, `departamentNumberOfProgrammers`, `departamentDescription`, `templateLetter`; колонки таблицы — `projectName`, `startProjectDate`, `endProjectDate`.

Критичные nested paths из Java: lazy-reload `departamentDescription`, `templateLetter`, `projectOfDepartment` (view `project-department-child-view`). View integrity: все инжектируемые id (6 компонентов) присутствуют в XML.

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `hunttech_CompanyDepartament.browse` | create / edit action |
| Родитель (cross-form) | `CompanyEdit` (dataGrid подразделений, вкладка «Департамент») | create / edit action dataGrid |
| Lookup targets | picker_lookup на FK-полях (компания, директора) | `screenBuilders.lookup()` |
| Дочерние формы | `hunttech_Project.edit` (из таблицы проектов) | create / edit action table |

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл (Lifecycle)

- `onBeforeShowSidebar` (presentation): динамический title sidebar — `departamentRuName` либо `browseCaption` («Департамент») для новой записи.
- `onAfterShow`: существующая запись → первая ленивая загрузка краткого описания.
- `onTabSheetDepartmentSelectedTabChange` (бизнес): ленивая загрузка LOB/проектов по вкладкам (существующая запись).
- `onTabSheetDepartmentSelectedTabChangeNav` (presentation, отдельный обработчик): синхронизация активного пункта sidebar-навигации.

### 4.2 Скрытые вычисления

- Sidebar title: название департамента по центру (жёлтый `#ffb11b`, 18px/700).
- Активный пункт навигации «Разделы»: `label-nav-item-active` управляется контроллером (`setNavigationActive`/`resetNavigationActiveStyles`), базовый класс `label-nav-item` не снимается.
- Видимость навигации (контракт §3.6, эталон OpenPositionEdit): контейнер `label-navigation` показывается только на вкладках с двумя и более блоками ввода (`tabEditProject` — 2 карточки); одноблочные вкладки («Открытые позиции» — таблица, «Шаблон письма» — редактор) скрывают навигацию целиком вместе с заголовком; контейнер и кнопки остаются в XML/Java как контракт инжекции (`TABS_WITH_SIDEBAR_NAVIGATION`).
- Иллюстрация: статичный `ovalImage` 176×176 (тема `icons/dictionaries/company-departament.png`) — загрузка изображения формой не предусмотрена, fallback-компонент не используется.

### 4.3 Валидация и сохранение

Обязательное поле: `companyNameField` (`required="true"`). Сохранение — стандартный commit `StandardEditor`; дополнительных BeforeCommit-обработчиков нет.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Цепочка |
|---------|---------|
| Пункт навигации «Разделы» (`companyDepartamentNavMain/Projects/Template`) | Клик → активный пункт (`label-nav-item-active`) → `tabSheetDepartment.setSelectedTab(...)` соответствующей вкладки; навигация видима только на вкладке `tabEditProject` (правило 3.6) |
| Вкладка TabSheet | Смена → (бизнес) ленивая загрузка LOB/проектов, если ещё не грузились; (presentation) видимость навигации по правилу 3.6 + активный пункт |
| Таблица проектов (`companyDepartamentTable`) | Кнопки «Добавить»/«Изменить»/«Удалить» → action-цепочки add/edit/remove таблицы (CRUD composition `projectOfDepartment`) |
| Footer «Сохранить и закрыть» / «Отмена» | `windowCommitAndClose` (primary) / `windowClose` (secondary), правый нижний угол |

## 6. Визуальная компоновка элементов (Visual Layout Schema)

Двухпанельная компоновка `edit-screen-layout` (контракт §4–5, эталон ProjectEdit/CompanyEdit):

- **Sidebar 270px** (`company-departament-editor` namespace, тёмный `#172638`, `padding: 14px 16px 12px`, border-right + тень, тонкий скроллбар):
  1. visual `edit-sidebar-visual` — статичный `ovalImage companyDepartamentLogoImage` 176×176 (`company-departament-editor-logo-image`, `SCALE_DOWN`, theme `icons/dictionaries/company-departament.png`); без upload/fallback;
  2. identity `edit-sidebar-identity` — живой title `companyDepartamentSidebarTitle` по центру, без подписи типа записи;
  3. навигация `label-navigation` — полоса-заголовок «Разделы» (`label-nav-title company-departament-editor-navigation-title`, inset-линии §4.1) + 3 пункта 27px (`label-nav-item`, активный `#ffb11b`); пункты по высоте контента (`height: auto`, перенос длинной подписи), локальное wrap-правило с принудительной высотой отсутствует (контракт §3.1); на одноблочных вкладках контейнер скрывается (правило 3.6);
  4. spacer `edit-sidebar-spacer` (100%×100%) + hint `edit-sidebar-hint`.
- **Workspace**: toolbar (`edit-toolbar-title` 20px + `edit-toolbar-description`), tabSheet `edit-tabs` (НЕ framed; общие стили тем), вкладки:
  - `tabEditProject` — scrollBox `edit-workspace-scroll` → `edit-workspace-content` → карточки `edit-card`+`showAsPanel`: `companyDepartamentMainCard` («Реквизиты департамента», 5 полей 100%) и `companyDepartamentDescriptionCard` (текст-область «Краткое описание», LOB lazy);
  - `tabOpenPosition` — карточка `companyDepartamentProjectsCard` с table `companyDepartamentTable` + buttonsPanel (add/edit/remove);
  - `tabTemplateLetter` — карточка `companyDepartamentTemplateCard` с richTextArea `templateLetterRichTextArea` (LOB lazy).
- **Footer** `edit-footer-actions`: expand-спейсер + группа AUTO/MIDDLE_RIGHT (`spacing="true"`) → `company-departament-editor-primary-action` / `company-departament-editor-secondary-action` (40px/14px/600).

Локальный SCSS: `company-departament-editor.scss` в 7 темах (md5-идентичны; ограничения `.v-richtextarea` и `.v-table` от переполнения flex-карточек), подключение `@import` + `@include company-departament-editor-theme` в `styles.scss` каждой темы; иконка `icons/dictionaries/company-departament.png` (200×200, стиль серии: фон `#172638`, символ `#f8fafc`, акцент `#e74c3c`) во всех 7 темах. Новые msg-ключи: `msgCompanyDepartamentSidebarHint`, `msgCompanyDepartamentToolbarDescription`, `msgDepartamentMainSection`, `msgDepartamentDescriptionSection`, `msgDepartamentProjectsSection`, `msgDepartamentTemplateSection` (оба файла пакета).

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-14 | Сверка с эталоном (контракт §3.1/§3.6/§4.1): пункты навигации по высоте контента (`height: auto`, перенос подписи), удалено локальное wrap-правило с принудительной высотой, полоса-заголовок `height: auto`; реализовано правило 3.6 — `label-navigation` скрывается на одноблочных вкладках (`TABS_WITH_SIDEBAR_NAVIGATION`) |
| 2026-08-14 | Рефакторинг по контракту Edit-форм (эталон ProjectEdit/CompanyEdit): sidebar 270px со статичной иллюстрацией `ovalImage` 176×176 и навигацией «Разделы» (вкладки), карточки `edit-card`+`showAsPanel`, footer primary/secondary, `dialogMode` 100%×100% modal; удалена legacy-форма `form` (focusComponent → `departamentRuNameField`), presentation-навигация в Java, lazy-load 1:1; `company-departament-editor.scss` ×7 тем, иконка `company-departament.png` ×7 тем; новый `CompanyDepartamentEditLayoutContractTest` (7/7) |
