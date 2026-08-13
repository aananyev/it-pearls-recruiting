# UserAiConfigurationEdit (`hunttech_UserAiConfiguration.edit`)

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Форма ведёт персональные ключи пользователей для AI-провайдеров: какой провайдер (`providerCode`), секретный API-ключ (`apiKey`), модель по умолчанию (`defaultModelName`) и активность (`isActive`). Это позволяет каждому пользователю использовать собственные учётные данные вместо корпоративных и переключать модель без обращения к администратору.

### UI Context & Navigation

Раньше — модальный диалог 450px внутри ExtUser edit. После рефакторинга — полноэкранная двухпанельная форма по общему Edit-контракту (sidebar + workspace), открывается из `hunttech_UserAiConfiguration.browse`. Sidebar содержит identity и label-навигацию «Основные данные / Параметры модели» (display-only, в Java focus-методов нет).

### Behavior Summary

- новая запись → пустая форма → пользователь выбирает провайдера и вводит ключ;
- `apiKey` — секретное поле ввода (тип password), не выводится в browse;
- `isActive` — чекбокс активности подключения;
- save → стандартный DataContext commit.

## 1. Invocation & Context

Controller `hunttech_UserAiConfiguration.edit`; `StandardEditor<UserAiConfiguration>`; открывается из browse. dialogMode `100%×100% modal` (было 450×AUTO legacy-диалог).

## 2. Data & Entity Binding

`userAiConfigDc` (instance container, view `user-ai-configuration-edit-view`): `providerCodeField`, `apiKeyField` (password), `defaultModelNameField`, `isActiveField` (checkBox) — все с `stylename="edit-form-control"` и caption msg-ключами.

## 3. Form Hierarchy

Parent: UserAiConfiguration browse. Отдельных picker-форм нет (providerCode — свободный ввод).

## 4. Behavior Model

Lifecycle стандартный CUBA StandardEditor: Init → BeforeShow (bindings) → commit. Специфической Java-логики нет; навигация sidebar — display-only label-пункты.

## 5. Actions & Buttons Logic

Footer `edit-footer-actions`: `windowCommitAndClose` (Сохранить) / `windowClose` (Отмена). Sidebar-пункты кликабельны только как визуальная навигация (без focus-перехода — контроллер не менялся).

## 6. Visual Layout Schema

`edit-screen-layout` → sidebar 312px (`edit-sidebar`: тёмная #172638 с градиентом, padding 14px 16px 12px, border-right rgba(15,23,42,0.78), box-shadow, media-тиры 296/284px; слот — только фон) → `edit-workspace` → toolbar → scroll → 2 карточки `edit-card` (showAsPanel="true": «Основные данные» — providerCode/apiKey, «Параметры модели» — defaultModelName/isActive) → `edit-footer-actions`. Локальный root `user-ai-configuration-editor`; полоса-заголовок навигации `user-ai-configuration-navigation-title` (§4.1, inset-линии). Локальный SCSS partial `user-ai-configuration-editor.scss` во всех 7 темах (sha256-идентичен).

## Data View Integrity

Все поля контроллера входят в view контейнера `userAiConfigDc`; секретный `apiKey` не выводится ни в одном browse-view.

Sidebar-иллюстрация: `ovalImage` отображается 176×176 через прямой theme-ресурс и использует отдельный theme asset `icons/ai/user-ai-configuration.png` размером 200×200. Графика в чёрно-серо-бело-красной палитре HRM HuntTech объединяет HuntTech-монограмму, пользовательский профиль и ключ, визуально обозначая персональный AI credential.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-13 | Фикс наложения шапок карточек в правой части формы: в shared-контракт `edit-screen-shared-styles.scss` (7 тем) добавлен сброс `margin-top: 0 !important` для `.edit-card > .v-panel-captionwrap` / `.c-groupbox-captionwrap` — базовое правило halo-темы выносило шапку панели на 50px вверх, шапки наезжали на соседние карточки и toolbar (эталон — open-position-editor) |
| 2026-08-13 | Sidebar-иллюстрация переведена с `ovaFallbackImage` на `ovalImage` с прямым `<theme path="icons/ai/user-ai-configuration.png">` — отображение гарантировано без fallback-механики |
| 2026-08-13 | Общий fallback `icons/hunttech-logo.png` заменён на тематическую иллюстрацию персонального AI-подключения `icons/ai/user-ai-configuration.png`: исходный asset 200×200, отображение `ovalImage` с `<theme path="icons/ai/user-ai-configuration.png">` сохранено 176×176 |
| 2026-08-13 | Размер sidebar-логотипа `ovalImage` приведён к эталону JobCandidateEdit: 176×176 (было 96×96) |
| 2026-08-12 | Рефакторинг из legacy-диалога 450×AUTO в полноэкранную двухпанельную форму по общему Edit-контракту: sidebar 312px + label-навигация + toolbar + 2 карточки edit-card + footer; локальный partial `user-ai-configuration-editor.scss` (7 тем, sha256-идентичен); edit-form-control на все 4 поля + caption msg-ключами; dialogMode 100%×100% modal. Эталон — IteractionListEdit |