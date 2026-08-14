# PositionEdit — спецификация экрана

**Проект:** HRM HuntTech
**Платформа:** CUBA Platform 7.3-SNAPSHOT
**UI controller:** `hunttech_Position.edit`
**Controller:** `modules/web/src/com/company/hunttech/web/screens/position/PositionEdit.java`
**Descriptor:** `modules/web/src/com/company/hunttech/web/screens/position/position-edit.xml`
**Статус:** актуально (редизайн по контракту 2026-08-13; фикс UNFETCHED ATTRIBUTE ACCESS 2026-08-11)

## Назначение и Бизнес-смысл (What & Why)

Форма редактирует запись справочника должностей HRM HuntTech: русское и английское наименования позиции плюс два LOB-описания — «Общее описание вакансии» (`standartDescription`) и «Кто это такой» (`whoIsThisGuy`). Должности используются в вакансиях (`OpenPosition.positionType`) и резюме кандидатов; описания служат рекрутеру шаблоном-подсказкой при составлении вакансии. Задача формы — быстро отредактировать названия и шаблонные тексты без потери содержимого LOB-полей.

## Связи в интерфейсе и Навигация (UI Context & Navigation)

Форма открывается из `PositionBrowse` (`hunttech_Position.browse`, меню справочников) по кнопкам edit/create как полноэкранный модальный редактор (`dialogMode 100%×100% modal`). Внешние потребители должности — формы вакансий (lookup через `position-picker-view`) и REST-публичные view; внутри формы дочерних экранов нет. Sidebar формы показывает круглую иллюстрацию должности 176×176 и label-навигацию «Наименование»/«Описание».

## Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие существующей записи → loader загружает `Position` со view `position-edit-view`, расширенным inline LOB-полями → `onBeforeShow` формирует заголовок «en - ru» → два `richTextArea` сразу показывают сохранённые описания.
- Изменение любого поля наименования → заголовок формы пересчитывается (значение «en - ru»).
- Сохранение → стандартный commit `StandardEditor` — LOB-поля сохраняются вместе с названиями.
- Создание новой записи → `windowCommitAndClose` валидирует обязательное русское наименование.

## 1. Точка вызова и контекст

```java
@UiController("hunttech_Position.edit")
@UiDescriptor("position-edit.xml")
@EditedEntityContainer("positionDc")
@LoadDataBeforeShow
public class PositionEdit extends StandardEditor<Position>
```

- Диалоговый режим: `<dialogMode height="100%" width="100%" modal="true"/>` (полноэкранный модальный редактор справочника — общий контракт Edit-экранов; до редизайна 2026-08-13 было `600×800`).
- Открытие из `PositionBrowse` (EditAction/create), а также программно из других форм по `position-picker-view`.
- Доступ: стандартные права на `hunttech_Position` (CRUD).

## 2. Связь с моделью данных

### 2.1. Контейнер и loader

| ID | Назначение | View контейнера |
|----|------------|-----------------|
| `positionDc` | редактируемая сущность `Position` | `<view extends="position-edit-view">` + inline `<property name="standartDescription"/>` + `<property name="whoIsThisGuy"/>` |

Общий `position-edit-view` в `views.xml` намеренно **не содержит** LOB-поля (защита остальных потребителей и тестов); LOB загружаются локальным inline-расширением только на этом экране (Localized In-Screen View Extension). Благодаря этому в `onBeforeShow` не требуется reload+setter — ловушка `IllegalStateException: Cannot get unfetched attribute [standartDescription] from detached object` (2026-08-11) устранена на уровне view.

### 2.2. Привязки полей

| Поле | Компонент | Property | Caption |
|------|-----------|----------|---------|
| `positionRuNameField` | `textField` | `positionRuName` | msg://msgPositionRuName |
| `positionEnNameField` | `textField` | `positionEnName` | msg://msgPositionEnName |
| `standartDescriptionTextArea` | `richTextArea` | `standartDescription` | msg://msgStandartDescription |
| `whoIsThisGuyTextArea` | `richTextArea` | `whoIsThisGuy` | msg://msgWhoThisGuyDescription |

### 2.3. View integrity (Java ↔ view)

| Обращение в Java | Покрытие view |
|------------------|---------------|
| `positionRuNameField.getValue()` | `positionRuName` в `position-edit-view` |
| `positionEnNameField.getValue()` | `positionEnName` в `position-edit-view` |
| `getEditedEntity()` | контейнер `positionDc` — полный inline view (в т.ч. LOB) |

Прямых getter-обращений к LOB-полям entity в контроллере больше нет — данные отдаются richTextArea через data binding.

## 3. Иерархия и взаимосвязь форм

- Родитель: `PositionBrowse` (lookup/CRUD).
- Дочерних экранов/фрагментов/диалогов нет.
- Внешние потребители сущности: `OpenPosition.positionType` (picker), REST-публичные view — используют `position-picker-view`, не затрагиваются inline-расширением.

## 4. Модель поведения и интерактивность

### 4.1. Lifecycle

- `@LoadDataBeforeShow` — entity загружается до `BeforeShowEvent`; loader использует inline view контейнера (базовый `position-edit-view` + LOB), поэтому LOB доступны сразу.
- `onBeforeShow(BeforeShowEvent)` — единственное действие: `setLabel()` — формирует заголовок `textPositionName` как «en - ru» из текущих значений полей. (Исторический reload+setter удалён — сеттер на detached с незагруженным LOB падал `IllegalStateException`.)

### 4.2. Скрытые вычисления

- `setLabel()`: конкатенация `positionEnNameField.getValue() + " - " + positionRuNameField.getValue()` → `textPositionName.setValue(...)`.

### 4.3. Валидация и сохранение

- Обязательное поле: `positionRuName` (`@NotNull` в entity, `nullable=false` в БД); при commit CUBA валидирует его.
- Сохранение — стандартный `windowCommitAndClose`: `dataContext` коммитит `Position` вместе с LOB-полями.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Действие | Цепочка |
|---------|----------|---------|
| `positionEnNameField` / `positionRuNameField` | text change | изменение значения → `setLabel()` → заголовок формы обновляется |
| Кнопка «Наименование» (`mainNav`, sidebar) | нажатие | `focusMainSection()` → фокус на `positionRuNameField` + активный пункт навигации (presentation-only, entity/loaders не меняются) |
| Кнопка «Описание» (`descriptionNav`, sidebar) | нажатие | `focusDescriptionSection()` → фокус на `standartDescriptionTextArea` + активный пункт навигации |
| Кнопка Сохранить (`windowCommitAndClose`) | commit | валидация обязательных полей → сохранение entity + LOB → закрытие диалога |
| Кнопка Отмена (`windowClose`) | закрытие | закрытие без сохранения |

## 6. Визуальная компоновка элементов

```
┌──────────────┬─────────────────────────────────────┐
│ SIDEBAR 270px│ Toolbar: «Карточка должности»       │
│ visual       │   + описание (edit-toolbar)         │
│ ovalImage    ├─────────────────────────────────────┤
│ 176×176      │ scrollBox (edit-workspace-scroll)   │
│ identity     │  ┌ edit-card «Наименование» ──────┐ │
│  «Должность» │  │ textPositionName (h1)          │ │
│  title (18px)│  │ positionRuNameField (100%)     │ │
│ navigation   │  │ positionEnNameField (100%)     │ │
│  «Разделы»   │  └────────────────────────────────┘ │
│  · Наименование│ ┌ edit-card «Описание» ──────────┐ │
│  · Описание  │  │ standartDescriptionTextArea     │ │
│ hint         │  │ whoIsThisGuyTextArea            │ │
│              │  └────────────────────────────────┘ │
│              ├─────────────────────────────────────┤
│              │ footer:        [Сохранить] [Отмена] │
└──────────────┴─────────────────────────────────────┘
```

- Корневой `layout stylename="position-editor" expand="mainLayout"` + `hbox mainLayout` (`edit-screen-layout`).
- Sidebar `vbox sidebar` (`edit-sidebar` 270px, тёмная `#172638→#0f1b28`): visual-блок с круглой иллюстрацией `ovalImage` 176×176 (theme-ресурс `icons/dictionaries/position.png`, stylename `dictionary-logo-image`), identity (subtitle «Должность», title `positionRuName` 18px/700 `#ffb11b`), label-навигация «Разделы» (пункты «Наименование»/«Описание», полоса-заголовок с inset-линиями), spacer и hint.
- Workspace `vbox workspace` (`edit-workspace`): toolbar (`edit-toolbar-title`/`edit-toolbar-description`), `scrollBox` с двумя карточками `edit-card` (`showAsPanel="true"`): «Наименование» (заголовок `textPositionName` + два textField с `edit-form-control`) и «Описание» (два `richTextArea` по 220px).
- Footer `hbox editActions` (`edit-footer-actions` 62px, кнопки 40px/600): `windowCommitAndClose` (`c-primary-action`) / `windowClose`.
- Messages: локальный пакет `com.company.hunttech.web.screens.position` (`messages.properties` / `messages_ru.properties`).

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-13 | Редизайн по общему контракту Edit-экранов: двухпанельная композиция (sidebar 270px + workspace), круглый `ovalImage` 176×176 с theme-ресурсом `icons/dictionaries/position.png`, label-навигация «Разделы» («Наименование»/«Описание» → `focusMainSection`/`focusDescriptionSection`), карточки `edit-card` «Наименование»/«Описание», footer 62px; `dialogMode` 600×800 → 100%×100% modal; исправлены перепутанные captions полей (`positionRuNameField` → `msgPositionRuName`, `positionEnNameField` → `msgPositionEnName`); Java: добавлены `focusMainSection()`/`focusDescriptionSection()` (фокус + active-nav), сохранены `setLabel()` и все binding-контракты; тест `DictionaryEditSidebarRedesignContractTest` |
| 2026-08-11 | Фикс UNFETCHED ATTRIBUTE ACCESS: LOB-поля вынесены в inline view контейнера `positionDc` (`<view extends="position-edit-view">`), reload+setter в `onBeforeShow` удалён; тест `PositionServiceTest.testEditViewLoadsLobFields` |
