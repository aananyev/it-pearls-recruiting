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

Sidebar buttons presentation-only; footer использует `windowCommitAndClose`/`windowClose`. Кнопки footer обёрнуты в паттерн эталона IteractionListEdit: `hbox editFooterActions` (`edit-footer-actions`) → `expand=editActionsSpacer` (vbox 1px) → `hbox editActionsGroup` (AUTO, MIDDLE_RIGHT, `spacing="true"` — межкнопочный зазор 10px) — кнопки прижаты в правый нижний угол. OK получает `ai-function-configuration-primary-action` (белый текст на primary), Отмена — `ai-function-configuration-secondary-action` (прозрачный фон).

## 6. Visual Layout Schema

`edit-screen-layout` → sidebar 312px (`edit-sidebar`, identity, `label-navigation`) → `edit-workspace` → toolbar → scroll → четыре `edit-card` → `edit-footer-actions`. Локальный root `ai-function-configuration-editor`.

Точные значения (эталон IteractionListEdit, CDP-сверка 2026-08-12):

| Элемент | Значение |
|---|---|
| Sidebar title | 18px/700 `#ffb11b`, line-height 24px |
| Sidebar subtitle | 12px/400 `rgba(248,250,252,0.72)`, line-height 17px |
| Toolbar | min-height 58px, padding 10px 20px, border-bottom rgba(52,66,79,0.16) |
| Toolbar title | 20px/700 mix 92%, line-height 27px |
| Toolbar description | 12px mix 60%, line-height 18px |
| Карточка `edit-card` | border-radius 8px, border rgba(52,66,79,0.15), shadow 0 2px 8px rgba(15,23,42,0.05), margin-bottom 12px |
| Чекбокс | padding 3px 8px, label 14px mix 78% |
| Textarea | 15px, line-height 21.75px, padding 4px |
| Footer | min-height 62px, padding 11px 20px, border-top rgba(52,66,79,0.16), shadow 0 -2px 8px rgba(15,23,42,0.04) |
| Footer-кнопки | min-height 40px, padding 0 18px, 14px/600, border-radius 4px; OK primary, Отмена transparent |

## Data View Integrity

Все getter-поля контроллера входят в `ai-function-configuration-edit-view`; prompt LOB загружаются только в edit/execution views; corporate secret не входит в options view.

Sidebar-логотип: `ovaFallbackImage` 176×176, border-radius 50% — как фото кандидата в JobCandidateEdit (эталон).

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-13 | Размер sidebar-логотипа `ovaFallbackImage` приведён к эталону JobCandidateEdit: 176×176 (было 96×96) |
| 2026-08-12 | Детальный дифф по эталону IteractionListEdit (30 отличий): footer-паттерн `expand=editActionsSpacer` + `editActionsGroup` (AUTO, MIDDLE_RIGHT) — кнопки прижаты в правый нижний угол; primary/secondary-классы кнопок (`ai-function-configuration-primary-action`/`secondary-action`); sidebar title 18px/700 `#ffb11b`/24px, subtitle 12px/400 `rgba(248,250,252,0.72)`/17px; toolbar title 20px/700, description mix 60%/18px, border-bottom 0.16; карточки margin-bottom 12px; чекбокс padding 3px 8px; textarea line-height 21.75px/padding 4px; footer min-height 62px/padding 11px 20px/border-top 0.16/shadow 0 -2px 8px; кнопки footer 40px/padding 0 18px/600/radius 4px + hover brightness(0.98) + focus ring. Обновлён партиал `ai-function-configuration-editor.scss` (7 тем, sha256-идентичен) |
| 2026-08-12 | Рефакторинг по эталону IteractionListEdit: локальный partial `ai-function-configuration-editor.scss` (7 тем, sha256-идентичен), тёмная sidebar #172638 312px с padding 14px 16px 12px, border-right и box-shadow (слот — только фон), каноническая label-навигация с полосой-заголовком `ai-function-configuration-navigation-title` (§4.1, inset-линии), media-тиры 296/284px; edit-form-control на все 13 полей + caption msg-ключами |
| 2026-08-12 | Создана Edit-форма AI-функции по общему Edit-контракту |
