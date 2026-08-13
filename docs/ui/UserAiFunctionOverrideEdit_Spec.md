# UserAiFunctionOverrideEdit

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Edit связывает одну разрешённую AI-функцию с одним собственным активным `UserAiConfiguration`, не раскрывая API key и не изменяя корпоративную конфигурацию.

### UI Context & Navigation

Открывается из персонального override browse. Sidebar: «AI-функция / Моё подключение».

### Behavior Summary

- новая запись → user=current session, enabled=true;
- options функций → только active и не `ADMIN_ONLY`;
- options credentials → только active текущего пользователя, safe view без key;
- before commit → ownership/policy/active guards → невалидный commit блокируется;
- model override → editable только если функция разрешает `allowModelOverride`.

## 1. Invocation & Context

`hunttech_UserAiFunctionOverride.edit`; `StandardEditor<UserAiFunctionOverride>`.

## 2. Data & Entity Binding

`overrideDc` view `user-ai-function-override-edit-view`; `functionsDc` safe function browse-view; `userConfigurationsDc` safe `user-ai-configuration-override-picker-view` с обязательным `:user`.

## 3. Form Hierarchy

Parent personal override browse. Relations: AiFunctionConfiguration, UserAiConfiguration текущего пользователя.

## 4. Behavior Model

`BeforeShow` устанавливает `:user` до automatic load. `BeforeCommitChanges` повторно валидирует policy и ownership независимо от options filtering.

## 5. Actions & Buttons Logic

Label navigation только focus/active-state; standard commit/close footer.

## 6. Visual Layout Schema

Shared Edit: 312px sidebar → workspace → two `edit-card` → footer. Root `user-ai-function-override-editor`.

## Data View Integrity

Контроллер не читает `apiKey`; safe picker view не содержит secret. Execution view с key используется только core resolver.

Sidebar-иллюстрация: `ovalImage` отображается 176×176 через прямой theme-ресурс и использует отдельный theme asset `icons/ai/user-ai-function-override.png` размером 200×200. Графика в фирменной чёрно-серо-бело-красной палитре HRM HuntTech объединяет HuntTech-монограмму, две конфигурации и switch/arrow, чтобы показать смысл per-function пользовательского замещения.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-13 | Чекбоксы форм переведены на общие стили темы CUBA Platform (Valo): из локальных партиалов удалена кастомная стилизация квадратика/подписи — устранён наезд чекбокса на элементы под ним, выравнивание квадратика и подписи штатное (тема) |
| 2026-08-13 | Фикс наложения шапок карточек в правой части формы: в shared-контракт `edit-screen-shared-styles.scss` (7 тем) добавлен сброс `margin-top: 0 !important` для `.edit-card > .v-panel-captionwrap` / `.c-groupbox-captionwrap` — базовое правило halo-темы выносило шапку панели на 50px вверх, шапки наезжали на соседние карточки и toolbar (эталон — open-position-editor) |
| 2026-08-13 | Sidebar-иллюстрация переведена с `ovaFallbackImage` на `ovalImage` с прямым `<theme path="icons/ai/user-ai-function-override.png">` — отображение гарантировано без fallback-механики |
| 2026-08-13 | Общий fallback `icons/hunttech-logo.png` заменён на тематическую иллюстрацию пользовательского AI-замещения `icons/ai/user-ai-function-override.png`: исходный asset 200×200, отображение `ovalImage` с `<theme path="icons/ai/user-ai-function-override.png">` сохранено 176×176 |
| 2026-08-13 | Размер sidebar-логотипа `ovalImage` приведён к эталону JobCandidateEdit: 176×176 (было 96×96) |
| 2026-08-12 | Рефакторинг по эталону IteractionListEdit: локальный partial `user-ai-function-override-editor.scss` (7 тем, sha256-идентичен), тёмная sidebar #172638 312px с padding 14px 16px 12px, border-right и box-shadow (слот — только фон), каноническая label-навигация с полосой-заголовком `user-ai-function-override-navigation-title` (§4.1, inset-линии), media-тиры 296/284px; edit-form-control на все поля + caption msg-ключами |
| 2026-08-12 | Создана Edit-форма персонального per-function override по общему Edit-контракту |