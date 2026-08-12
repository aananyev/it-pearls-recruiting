# AiFunctionConfigurationBrowse

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Browse показывает администратору каталог бизнес-функций AI и их routing policy без загрузки больших prompt LOB и без корпоративных секретов.

### UI Context & Navigation

Открывается из «Управление AI» → «Функции AI». Create/Edit ведут в `hunttech_AiFunctionConfiguration.edit`.

### Behavior Summary

- открытие → `aiFunctionsDl` загружает safe browse-view → таблица показывает code/name/capability/policy/model/state;
- create/edit/remove → стандартные CUBA actions → lifecycle `StandardLookup` не переопределяется;
- filter → работает через `aiFunctionsDl` → prompt/secret не добавляются в view.

## 1. Invocation & Context

Controller: `hunttech_AiFunctionConfiguration.browse`; class `AiFunctionConfigurationBrowse`; base `StandardLookup<AiFunctionConfiguration>`; lookup component `aiFunctionsTable`.

## 2. Data & Entity Binding

Container `aiFunctionsDc`, view `ai-function-configuration-browse-view`, loader `aiFunctionsDl`; JPQL `select e from hunttech_AiFunctionConfiguration e order by e.name`.

## 3. Form Hierarchy

Parent: menu «Управление AI». Child: `hunttech_AiFunctionConfiguration.edit`.

## 4. Behavior Model

`@LoadDataBeforeShow` выполняет стандартную загрузку. Контроллер не читает LOB/getters вне browse-view и не инициирует AI API.

## 5. Actions & Buttons Logic

`create`, `edit`, `remove`; lookup select/cancel скрыты при menu mode.

## 6. Visual Layout Schema

Filter → CRUD toolbar → полноширинный DataGrid. Локальный root `ai-function-configuration-browse`; глобальные Vaadin-селекторы не добавлены.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создан Browse AI-функций на `StandardLookup` с safe browse-view |
