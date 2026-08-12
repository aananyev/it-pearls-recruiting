# AiFunctionConfigurationEdit

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Edit централизованно задаёт назначение AI-функции, prompt, capability, корпоративную модель и правила пользовательского замещения.

### UI Context & Navigation

Открывается из `hunttech_AiFunctionConfiguration.browse`. Sidebar содержит identity и label-навигацию «Основное / Маршрутизация / Промпт / Параметры модели».

### Behavior Summary

- новая запись → устанавливаются безопасные defaults → пользователь заполняет функцию;
- существующая запись → `code` read-only → стабильный API-контракт не меняется;
- navigation click → только focus/active-state → entity/loaders не изменяются;
- save → стандартный DataContext commit.

## 1. Invocation & Context

Controller `hunttech_AiFunctionConfiguration.edit`; `StandardEditor<AiFunctionConfiguration>`; edited container `aiFunctionDc`.

## 2. Data & Entity Binding

`aiFunctionDc` использует `ai-function-configuration-edit-view`; `adminConfigurationsDc` — safe `admin-ai-configuration-browse-view`, только active corporate connections.

## 3. Form Hierarchy

Parent: AI function browse. Picker relation: `AdminAiConfiguration` без secret field.

## 4. Behavior Model

Defaults: TEXT_GENERATION, USER_OVERRIDE_ALLOWED, FALLBACK_TO_ADMIN, temperature 0.7, active, version 1. `codeField` блокируется для persisted entity.

## 5. Actions & Buttons Logic

Sidebar buttons presentation-only; footer использует `windowCommitAndClose`/`windowClose`.

## 6. Visual Layout Schema

`edit-screen-layout` → sidebar 312px (`edit-sidebar`, identity, `label-navigation`) → `edit-workspace` → toolbar → scroll → четыре `edit-card` → `edit-footer-actions`. Локальный root `ai-function-configuration-editor`.

## Data View Integrity

Все getter-поля контроллера входят в `ai-function-configuration-edit-view`; prompt LOB загружаются только в edit/execution views; corporate secret не входит в options view.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создана Edit-форма AI-функции по общему Edit-контракту |
