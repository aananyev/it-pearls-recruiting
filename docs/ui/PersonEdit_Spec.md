# PersonEdit — карточка человека (справочник «Люди»)

> Сущность: [Person.md](../entities/person/Person.md) · Брендинг: HRM HuntTech
> Эталон компоновки: SkillTreeEdit / гео-формы Country-Region-City (контракт Edit-экранов)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Форма `PersonEdit` (`hunttech_Person.edit`) редактирует запись справочника «Люди» — физического лица (сотрудника компании, руководителя проекта/департамента или контактного лица на стороне клиента). Запись хранит ФИО, дату рождения, контакты (email, телефоны, мессенджеры), местоположение (страна, город), должность и фотографию лица. Записи Person используются как владельцы проектов (`Project.projectOwner`) и контактные лица вакансий, поэтому актуальное фото и контакты важны рекрутёру при работе с проектами и коммуникациях.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из списка «Люди» (`hunttech_Person.browse`, пункт меню) действиями create/edit (модальный полноэкранный диалог). FK-поля формы открывают lookup-диалоги: город (`city-picker-view`), страна (`country-picker-view`), должность (`_minimal`). Другие экраны обращаются к Person через picker'ы владельцев (`project-edit.xml`, `company-edit.xml`, `open-position-*.xml`, dashboard) с view `person-picker-view`/`person-owner-view`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

При открытии: контейнер `personDc` грузит запись с `person-edit-view`, справочники городов/стран/должностей загружаются кэшируемыми loader'ами; если фото не задано — показывается fallback-аватар (`applyFallback`). Пользователь правит поля в трёх карточках, при необходимости загружает/очищает фото (upload с dropZone). Навигация sidebar «Разделы» → фокус переходит к первому полю раздела, подсвечивается активный пункт. «Сохранить» (primary) → штатный commit и закрытие; «Отмена» (secondary) → закрытие без сохранения.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_Person.edit` |
| **Java-класс** | `com.company.hunttech.web.screens.person.PersonEdit` |
| **XML-дескриптор** | `person-edit.xml` |
| **messagesPack** | `com.company.hunttech.web.screens.person` |
| **Базовый класс** | `StandardEditor<Person>` |
| **EditedEntityContainer** | `personDc` |
| **focusComponent** | `firstNameField` |
| **Режим окна** | модальный полноэкранный (`dialogMode` 100%×100%, `modal="true"`) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Person` (`hunttech_Person`) |
| **View** | `person-edit-view` (extends `_minimal`: firstName, middleName, secondName, birdhDate, email, phone, mobPhone, skypeName, telegramName, wiberName, watsupName, sendResumeToEmail, cityOfResidence → city-picker-view, positionCountry → country-picker-view, personPosition → _minimal, companyDepartment → _minimal, fileImageFace → _minimal) |

**Data containers:**

| Container | Тип | Loader | JPQL |
|-----------|-----|--------|------|
| `personDc` | instance | штатный (id из editor) | — |
| `positionCityDc` | collection (City, city-picker-view) | `positionCityLc` cacheable | `select e from hunttech_City e order by e.cityRuName` |
| `positionCountriesDc` | collection (Country, country-picker-view) | `positionCountriesLc` cacheable | `select e from hunttech_Country e order by e.countryRuName` |
| `personPositionsDc` | collection (Position, _minimal) | `personPositionsLc` cacheable | `select e from hunttech_Position e order by e.positionRuName` |

**Привязки property (по карточкам):**

- Карточка «Основные данные» (`personMainForm`): `firstName`, `middleName`, `secondName`, `birdhDate`
- Карточка «Контакты» (`personContactsForm`): `email`, `phone`, `mobPhone`, `skypeName`, `telegramName`, `wiberName`, `watsupName`
- Карточка «Местоположение и должность» (`personLocationForm`): `cityOfResidence` (options `positionCityDc`), `positionCountry` (options `positionCountriesDc`), `personPosition` (options `personPositionsDc`)
- Sidebar: `personSidebarTitle` → `firstName`; аватар `personPic` → `fileImageFace`

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / компонент | Способ открытия |
|-------|-------------------|-----------------|
| Родитель | `hunttech_Person.browse` | create / edit action |
| Lookup targets | picker_lookup на `positionCityField`, `positionCountryField`, `personPositionField` | `screenBuilders.lookup()` |
| Потребители Person | `project-edit.xml`, `company-edit.xml`, `open-position-*.xml`, `my-active-candidates-dashboard.xml` | picker владельца (`person-picker-view` / `person-owner-view`) |

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

- `@LoadDataBeforeShow` + `@EditedEntityContainer("personDc")`: запись грузится до показа.
- `onAfterShow`: если `fileImageFace == null` — `personPic.applyFallback()` (fallback-аватар `icons/no-programmer.jpeg`), иначе фото из контейнера (биндинг dataContainer).
- `onFileImageFaceUploadFileUploadSucceed`: после загрузки файла preview аватара обновляется из `FileDescriptorResource`.

### 4.2 Скрытые вычисления

- Аватар: `ovaFallbackImage` 176×176, круглая обрезка, `SCALE_DOWN`; отсутствие файла не меняет геометрию (стабильный размер).
- Навигация sidebar: `setActiveNavigation` переносит класс `label-nav-item-active` между пунктами (presentation-only, данные не изменяются).

### 4.3 Валидация и сохранение

- Стандартный commit editor-а (`windowCommitAndClose`); уникальность контактов (`mobPhone`, `skypeName`, `telegramName`, `wiberName`, `watsupName` — unique в entity) и `@Email` проверяются на уровне БД/модели.
- Никаких кастомных PreCommit/PostCommit обработчиков нет.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Действие → Условие → Результат |
|---------|-------------------------------|
| Пункт «Основные данные» (`personMainNav`) | Нажал → фокус на `firstNameField`, подсветка пункта → переход к карточке ФИО |
| Пункт «Контакты» (`personContactsNav`) | Нажал → фокус на `emailField`, подсветка пункта → переход к карточке контактов |
| Пункт «Местоположение» (`personLocationNav`) | Нажал → фокус на `positionCityField`, подсветка пункта → переход к карточке географии |
| «Сохранить» (`windowCommitAndClose`, primary) | Нажал → валидация/commit → запись сохранена, окно закрыто |
| «Отмена» (`windowClose`, secondary) | Нажал → закрытие без сохранения |
| Upload фото (`fileImageFaceUpload`) | Загрузил/перетащил файл → IMMEDIATE в `fileImageFace` → preview обновлён; «Очистить» → fallback |

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

```
layout.person-editor (100%×100%)
└── hbox.personMainLayout (edit-screen-layout, expand=personWorkspace)
    ├── vbox.personSidebar (edit-sidebar, 270px, expand=personSidebarSpacer)
    │   ├── vbox.personVisual (edit-sidebar-visual) → personPic (ovaFallbackImage 176×176) + fileImageFaceUpload (dropZone=personVisual)
    │   ├── vbox.personIdentity (edit-sidebar-identity) → personSidebarTitle (edit-sidebar-title, по центру)
    │   ├── vbox.personNavigation (label-navigation) → «Разделы» (label-nav-title person-navigation-title) + 3 пункта (label-nav-item, 27px)
    │   ├── vbox.personSidebarSpacer (edit-sidebar-spacer)
    │   └── label hint (edit-sidebar-hint)
    └── vbox.personWorkspace (edit-workspace, expand=personWorkspaceScroll)
        ├── hbox.personToolbar (edit-toolbar) → title «Карточка человека» + description
        ├── scrollBox.personWorkspaceScroll → vbox.personSections (edit-workspace-content person-content)
        │   ├── groupBox.personMainSection (edit-card) → form.personMainForm (4 поля)
        │   ├── groupBox.personContactsSection (edit-card) → form.personContactsForm (7 полей)
        │   └── groupBox.personLocationSection (edit-card) → form.personLocationForm (3 picker)
        └── hbox.editActions (edit-footer-actions, expand=personActionsSpacer, MIDDLE_RIGHT)
            → personActionsGroup → «Сохранить» (person-editor-primary-action) + «Отмена» (person-editor-secondary-action)
```

### Ключевые классы и стили

| Элемент | Stylename / источник |
|---------|----------------------|
| Корень формы | `person-editor` (локальный namespace) + `edit-screen-layout` |
| Sidebar | `edit-sidebar` 270px, тёмный градиент #172638, padding 14px 16px 12px, border-right, тень (partial `person-editor.scss`, 7 тем) |
| Аватар | `person-logo-image` — круг 176×176, border-radius 50% |
| Кнопки upload | 96×36, rgba(255,255,255,.06) / рамка rgba(255,255,255,.34), 14px/600 |
| Навигация | `label-navigation`, `label-nav-title person-navigation-title` (полоса-заголовок), `label-nav-item` 27px, active #ffb11b |
| Карточки | `edit-card` (radius 8px), caption 46px/16px/700, content 14px 16px 16px |
| Поля | `edit-form-control` + `.edit-card .v-textfield/.v-filterselect/.v-datefield` 38px, caption 13px/600 |
| Footer | `edit-footer-actions` (min-height 62px, padding 11px 20px), кнопки 40px/14px/600/radius 4px |
| Контент | `person-content` — padding 8px 20px 24px |

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-14 | Рефакторинг по контракту Edit-экранов (эталон SkillTreeEdit/гео-формы): sidebar 270px с ovaFallbackImage-аватаром 176×176 и upload, identity без подписи типа записи (title по центру), label-навигация «Разделы» (3 пункта, presentation-only), карточки Основные данные/Контакты/Местоположение, footer primary/secondary; контроллер переведён на OvaFallbackImage (applyFallback), legacy image-дубль удалён; создан `PersonEditLayoutContractTest` |
