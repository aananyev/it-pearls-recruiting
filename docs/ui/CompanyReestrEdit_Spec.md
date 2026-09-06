# Спецификация и архитектурный дизайн: CompanyReestrEdit (`hunttech_CompanyReestr.edit`)

> **Роли**: Аналитик, UI/UX-дизайнер  
> **Основание**: [CompanyEdit_Spec.md](CompanyEdit_Spec.md), [SKILL.md](../../.agents/skills/hunttech-edit-screen-design/SKILL.md), [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)  
> **Статус**: Проектирование и согласование (Planning Mode)

---

## 1. Назначение и контекст экранной формы (What & Why)

### 1.1 Бизнес-назначение
Экранная форма **`CompanyReestrEdit`** предназначена для создания и полнофункционального редактирования компании (`Company`) непосредственно из реестра компаний **`CompanyReestrBrowse`** (`hunttech_CompanyReestr.browse`).

Форма вызывается:
1. При нажатии на кнопку **«Создать компанию»** (`createBtn`) в командном тулбаре `CompanyReestrBrowse`.
2. При нажатии на кнопку **«Редактировать»** (`editBtn`) в командном тулбаре `CompanyReestrBrowse`.
3. При нажатии на кнопку **«Открыть карточку»** (`openEditCardBtn`) в сайдбаре `CompanyReestrBrowse`.
4. При двойном клике (Enter / ItemClick) по строке реестра `companiesTable`.

---

## 2. Архитектура Two-Pane Split View и требования к адаптивности

Форма строится по проверенному двухпанельному стандарту **HRM HuntTech Edit Screen Design**:
1. **Левый Сайдбар (Sidebar)**: строго идентичен форме `CompanyEdit` (ширина 270px, визуальный образ, реквизиты, кнопки навигации «Разделы», подсказка). **Изменять сайдбар запрещено!**
2. **Правая рабочая область (Workspace)**: адаптивная компоновка с тулбаром, скроллером контента, системой вкладок (`tabSheet`), аккордеон-секциями (`edit-accordion-section`) и подвалом действий (`edit-footer-actions`).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Меню приложения / Заголовок окна (DialogMode 100%×100% modal)                                    │
├───────────────────────┬──────────────────────────────────────────────────────────────────────────┤
│ ЛЕВЫЙ САЙДБАР (270px) │ ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ (edit-workspace company-editor-workspace)        │
│ (edit-sidebar)        │ ┌──────────────────────────────────────────────────────────────────────┐ │
│                       │ │ Toolbar (edit-toolbar): Заголовок, описание, «Умное заполнение»      │ │
│ [НЕ МЕНЯТЬ 1-В-1]     │ ├──────────────────────────────────────────────────────────────────────┤ │
│                       │ │ Общий скроллер контента (company-editor-content-scroll):             │ │
│ • scrollBox (100% h)  │ │ ┌──────────────────────────────────────────────────────────────────┐ │ │
│ • Аватар 176×176      │ │ │ Вкладки (TabSheet: edit-tabs, ВСЕ tab margin="false"):           │ │ │
│ • Загрузка логотипа   │ │ │ [Информация о компании] [Реквизиты] [Описание] [Подразделения]   │ │ │
│ • Умная обработка     │ │ │ ┌──────────────────────────────────────────────────────────────┐ │ │ │
│ • Title & Имя         │ │ │ │ Адаптивные аккордеон-карточки (edit-accordion-section)       │ │ │ │
│ • ИНН и Город         │ │ │ │ • Desktop (>=1366px): 2 колонки (50/50)                      │ │ │ │
│ • Навигация «Разделы» │ │ │ │ • Compact (<1366px): 1 колонка (плавный flex-перенос)        │ │ │ │
│ • Spacer & Подсказка  │ │ │ └──────────────────────────────────────────────────────────────┘ │ │ │
│                       │ │ └──────────────────────────────────────────────────────────────────┘ │ │
│                       │ ├──────────────────────────────────────────────────────────────────────┤ │
│                       │ │ Подвал действий (edit-footer-actions): [Сохранить] [Отмена]          │ │
│                       │ └──────────────────────────────────────────────────────────────────────┘ │
└───────────────────────┴──────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Спецификация левого Сайдбара (Неизменяемый эталон CompanyEdit)

В соответствии с требованием пользователя сайдбар переносится **полностью без изменений** со 100% идентичной версткой и логикой контроллера:

### 3.1 Разметка Сайдбара
- Контейнер: `<vbox id="companyEditorSidebar" stylename="edit-sidebar" width="270px" height="100%" spacing="false">`
- Вертикальный скроллер: `<scrollBox id="companySidebarScroll" width="100%" height="100%" orientation="vertical" scrollBars="vertical">`
- Блок визуального образа `companyEditorSidebarVisual`:
  - Обёртка аватара `companyLogoPicBox` (176×176px).
  - Компонент круглого логотипа: `<ovaFallbackImage id="companyLogoFileImage" dataContainer="companyDc" property="fileCompanyLogo" width="176px" height="176px" ovalWidth="176px" ovalHeight="176px" ovalBackground="#3a3e44" fallbackThemePath="icons/no-company.png" scaleMode="SCALE_DOWN"/>`.
  - Загрузчик логотипа: `<upload id="companyLogoFileUpload" fileStoragePutMode="IMMEDIATE" dataContainer="companyDc" property="fileCompanyLogo" dropZone="companyEditorSidebarVisual" showClearButton="true"/>`.
  - Кнопка умной обработки: `<button id="enhanceCompanyLogoBtn" caption="Умная обработка фото" icon="font-icon:MAGIC" stylename="secondary candidate-btn enhance-company-logo-btn" width="100%"/>`.
- Блок идентификации `companyEditorSidebarIdentity`:
  - `label id="companySidebarTitle"` (динамический заголовок компании).
  - `label id="companySidebarName"` (`property="companyShortName"`, bold).
- Блок сводки реквизитов `companyEditorSidebarSummary`:
  - ИНН: `companySidebarInnBox` (`label id="companySidebarInn"`).
  - Город: `companySidebarCityBox` (`label id="companySidebarCity"`).
- Секционная навигация `companyEditorSidebarNavigation`:
  - Заголовок: `<label value="Разделы" stylename="label-nav-title company-editor-navigation-title"/>`.
  - Кнопка `companyEditorNavMain`: «Информация о компании» (`icon="BUILDING"`).
  - Кнопка `companyEditorNavRequisites`: «Официальные реквизиты» (`icon="FILE_TEXT_O"`).
  - Кнопка `companyEditorNavDescription`: «Описание компании» (`icon="INFO_CIRCLE"`).
  - Кнопка `companyEditorNavDepartments`: «Подразделения» (`icon="SITEMAP"`).
- Спейсер `companySidebarSpacer` (`height="100%"`) и подсказка `companySidebarHint` (`value="msg://msgCompanySidebarHint"`).

### 3.2 Логика контроллера для Сайдбара
- `updateSidebarTitle()`: динамический title (наименование компании либо `browseCaption`).
- `onEnhanceCompanyLogoBtnClick()`: обработка логотипа через `ProjectLogoImageProcessingService` (удаление фона, ресайз, вписывание в круг) с регистрацией в `pendingRemovalLogoDescriptors`.
- `onAfterCommitChanges()`: очистка старых файловых дескрипторов логотипа.
- `onCompanyEditorNav...Click()`: переключение активной вкладки и класса `label-nav-item-active`.
- `onMainTabSelectedTabChangeNav()`: синхронизация подсветки кнопок навигации при смене вкладки `TabSheet`.

---

## 4. Спецификация адаптивной правой части (Workspace)

Правая часть проектируется с адаптивной сеткой полей, предотвращающей переполнение и появление горизонтального скролла на экранах от 1280px до 4K.

### 4.1 Тулбар (Toolbar)
- Заголовок: `companyEditorToolbarTitle` (`msg://browseCaption`).
- Описание: `companyEditorToolbarDescription` (`msg://msgCompanyToolbarDescription`).
- Кнопка быстрого вызова мастера: `smartFillCompanyBtn` («Умное заполнение реквизитов», icon MAGIC), открывающая `SmartCompanyRequisitesUploadScreen`.

### 4.2 Вкладка 1: «Информация о компании» (`tabConpanyDetails`)
- Обязательный атрибут: `margin="false"`.
- Аккордеон-карточка **«Реквизиты компании»** (`companyMainCard`, `stylename="edit-card"`):
  - Верхний ряд чекбоксов: `ourLegalEnityCheckBox` («Наше юридическое лицо») и `checkBoxOurClient` («Наш клиент»).
  - Адаптивный ряд 1 (50/50 на Desktop, 1 колонка на Compact):
    - `comanyNameField`: Наименование компании (required).
    - `companyShortNameField`: Краткое наименование / бренд.
  - Адаптивный ряд 2 (50/50 на Desktop, 1 колонка на Compact):
    - `companyGroupLookupPickerField`: Группа / холдинг (`companyGroupDc`).
    - `companyDirectorField`: Генеральный директор (`companyDirectorsDc`).
  - Адаптивный ряд 3 (50/50 на Desktop, 1 колонка на Compact):
    - `phoneMainField`: Телефон.
    - `emailMainField`: Email.
  - Поле на всю ширину: `websiteMainField`: Веб-сайт.
- Аккордеон-карточка **«Адрес компании»** (`companyAddressCard`):
  - Адаптивный ряд 1: `countryOfCompanyField` и `regionOfCompanyField` (с каскадной зависимостью).
  - Поле на всю ширину: `cityOfCompanyField` (город).
  - Поле на всю ширину: `addressOfCompanyField` (улица, дом, офис).

### 4.3 Вкладка 2: «Официальные реквизиты» (`companyRequisitesTab`)
- Обязательный атрибут: `margin="false"`.
- Верхняя панель: кнопка `smartUploadRequisitesBtn` («Умная загрузка реквизитов»).
- Карточка **«Основные юридические реквизиты»** (`companyRequisitesMainCard`):
  - Ряд 1: `legalEntityNameField` (Юрлицо) + `companyOwnershipRequisitesField` (Форма собственности).
  - Ряд 2: `innField` (ИНН) + `kppField` (КПП).
  - Ряд 3: `ogrnField` (ОГРН) + `okpoField` (ОКПО).
  - Ряд 4: `oktmoField` (ОКТМО) + `okvedField` (ОКВЭД).
  - Поле на всю ширину: `companyDirectorRequisitesField` (Генеральный директор).
- Карточка **«Адрес и местонахождение организации»** (`companyRequisitesAddressCard`):
  - Ряд 1: Страна + Регион.
  - Ряд 2: Город + Адресная строка.
  - Поля адресов: `legalAddressField` (Юридический), `actualAddressField` (Фактический), `postalAddressField` (Почтовый).
- Карточка **«Банковские реквизиты»** (`companyRequisitesBankCard`):
  - Ряд 1: БИК Банка + Наименование банка.
  - Ряд 2: Расчетный счет + Корреспондентский счет.
- Карточка **«Официальные контакты»** (`companyRequisitesContactsCard`):
  - Ряд 1: Телефон + Email.
  - Ряд 2: Веб-сайт.

### 4.4 Вкладка 3: «Описание компании» (`companyDescriptionTab`)
- Обязательный атрибут: `margin="false"`.
- Карточка **«Описание компании»**: `richTextArea id="companyDescritionRichTextArea"` (height 260px).
- Карточка **«Условия работы»**: `richTextArea id="companyWorkingConditionsRichTextArea"` (height 260px).

### 4.5 Вкладка 4: «Департаменты / Подразделения» (`tabCompanyDepartament`)
- Обязательный атрибут: `margin="false"`.
- Карточка `companyDepartmentsCard`:
  - Таблица `departmentOfCompanyTable` (`dataGrid`, dataContainer `departmentOfCompanyDc`).
  - Колонки: Наименование подразделения, Руководитель подразделения, HR-директор.
  - Панель кнопок: «Создать», «Редактировать», «Удалить».

### 4.6 Подвал действий (Footer Actions)
- Контейнер: `<hbox id="editActions" stylename="edit-footer-actions">`
- Кнопки:
  - `windowCommitAndClose`: «Сохранить и закрыть» (`stylename="company-editor-primary-action"`).
  - `windowClose`: «Отмена» (`stylename="company-editor-secondary-action"`).

---

## 5. Адаптивные правила верстки (Responsive Rules)

1. **Базовый режим Desktop (>= 1366px)**:
   - Сайдбар: 270px (фиксирован).
   - Правая часть: занимает всё оставшееся пространство (`expand="companyEditorWorkspace"`).
   - Поля внутри карточек располагаются по 2 в строку (50% / 50%).
2. **Компактный режим Compact / Split-Window (1024px - 1365px)**:
   - Сайдбар: строго 270px (без сжатия, контент внутри сайдбара скроллится).
   - Правая часть: парные строки полей ввода автоматически переносятся в колонку (100% width) за счет CSS-правил `@media (max-width: 1365px)` в пространстве `.company-editor`.
   - Исключается появление горизонтальной полосы прокрутки.
3. **Общий скроллер Workspace**:
   - `scrollBox id="companyEditorContentScrollBox"` имеет `width="100%"` и `height="100%"`.
   - Внутренние `scrollBox` вкладок имеют `stylename="company-tab-scroll"` с `overflow-x: hidden`, исключая конфликт двойных скроллбаров.

---

## 6. Data View Integrity и Контракт вызова из CompanyReestrBrowse

### 6.1 View контейнеров данных
- Главный контейнер `companyDc` использует `view="company-edit-view"`.
- Во view присутствуют все поля:
  - Идентификация: `comanyName`, `companyShortName`, `legalEntityName`, `ourClient`, `ourLegalEntity`.
  - Связи: `companyGroup`, `companyOwnership`, `companyDirector`, `cityOfCompany`, `regionOfCompany`, `countryOfCompany`.
  - Медиа: `fileCompanyLogo`.
  - Реквизиты: `inn`, `kpp`, `ogrn`, `okpo`, `oktmo`, `okved`, `legalAddress`, `actualAddress`, `postalAddress`, `bik`, `bankName`, `settlementAccount`, `correspondentAccount`, `phone`, `email`, `website`.
  - Коллекции: `departmentOfCompany` (view `companyDepartament-department-child-view`).
- LOB-атрибуты (`addressOfCompany`, `companyDescription`, `workingConditions`) догружаются лениво при переходе на соответствующие вкладки.

### 6.2 Интеграция в CompanyReestrBrowse
В `company-reestr-browse.xml` и `CompanyReestrBrowse.java`:
- Действия таблицы:
  - `<action id="create" type="create" screenClass="com.company.hunttech.web.screens.company.CompanyReestrEdit"/>`
  - `<action id="edit" type="edit" screenClass="com.company.hunttech.web.screens.company.CompanyReestrEdit"/>`
- Кнопка сайдбара:
  - `openEditCardBtn`: `screenBuilders.editor(companiesTable).withScreenClass(CompanyReestrEdit.class).editEntity(selected).withOpenMode(OpenMode.DIALOG).show()`
- Кнопки тулбара:
  - `createBtn`: привязана к `companiesTable.create`
  - `editBtn`: привязана к `companiesTable.edit`
