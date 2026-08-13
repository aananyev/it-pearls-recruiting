# AdminAiConfigurationEdit

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Edit создаёт и обновляет corporate provider/model/credential. Сохранённый secret никогда не показывается; новый plaintext живёт только в unbound password input до middleware encryption.

### UI Context & Navigation

Открывается только из корпоративного AI Browse при наличии screen/entity permissions. Sidebar: «Основное / Подключение / Безопасность».

### Behavior Summary

- новое подключение → ввести provider/model/key → middleware encrypt → commit ciphertext;
- редактирование без нового key → старый ciphertext сохраняется;
- редактирование с новым key → permission check/encrypt → ciphertext заменяется;
- encryption config отсутствует → commit блокируется с уведомлением.

## 1. Invocation & Context

`hunttech_AdminAiConfiguration.edit`; `StandardEditor<AdminAiConfiguration>`; `adminConfigurationDc`.

## 2. Data & Entity Binding

Edit-view содержит ciphertext для сохранения состояния DataContext, но XML не имеет bound component к `apiKeyEncrypted`. `apiKeyInput` — unbound `passwordField`.

## 3. Form Hierarchy

Parent corporate browse. Entity `@SystemLevel`.

## 4. Behavior Model

`BeforeCommitChanges` шифрует новый key через `AiCredentialService`; `preventCommit()` применяется при ошибке или отсутствии key в новой записи. Provider list соответствует существующему registry-каталогу.

## 5. Actions & Buttons Logic

Label navigation presentation-only. Footer standard commit/close. Кнопки footer обёрнуты в паттерн эталона IteractionListEdit: `hbox editActions` (`edit-footer-actions`) → `expand=editActionsSpacer` (vbox 1px) → `hbox editActionsGroup` (AUTO, MIDDLE_RIGHT, `spacing="true"` — межкнопочный зазор 10px) — кнопки прижаты в правый нижний угол. OK получает `admin-ai-configuration-primary-action` (белый текст на primary), Отмена — `admin-ai-configuration-secondary-action` (прозрачный фон).

## 6. Visual Layout Schema

Shared Edit: sidebar 312px → workspace → toolbar → three `edit-card` → footer. Root `admin-ai-configuration-editor`.

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
| Textarea (`lastErrorField`) | 15px, line-height 21.75px, padding 4px |
| Password (`apiKeyInput`) | 38px/15px, border rgba(52,66,79,0.2), radius 5px |
| Footer | min-height 62px, padding 11px 20px, border-top rgba(52,66,79,0.16), shadow 0 -2px 8px rgba(15,23,42,0.04) |
| Footer-кнопки | min-height 40px, padding 0 18px, 14px/600, border-radius 4px; OK primary, Отмена transparent |

Sidebar-логотип: `ovaFallbackImage` 176×176, border-radius 50% — как фото кандидата в JobCandidateEdit (эталон).

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-13 | Размер sidebar-логотипа `ovaFallbackImage` приведён к эталону JobCandidateEdit: 176×176 (было 96×96) |
| 2026-08-12 | Детальный дифф по эталону IteractionListEdit (30 отличий): footer-паттерн `expand=editActionsSpacer` + `editActionsGroup` (AUTO, MIDDLE_RIGHT) — кнопки прижаты в правый нижний угол; primary/secondary-классы кнопок (`admin-ai-configuration-primary-action`/`secondary-action`); sidebar title 18px/700 `#ffb11b`/24px, subtitle 12px/400 `rgba(248,250,252,0.72)`/17px; toolbar title 20px/700, description mix 60%/18px, border-bottom 0.16; карточки margin-bottom 12px; чекбокс padding 3px 8px; textarea line-height 21.75px/padding 4px; footer min-height 62px/padding 11px 20px/border-top 0.16/shadow 0 -2px 8px; кнопки footer 40px/padding 0 18px/600/radius 4px + hover brightness(0.98) + focus ring. Обновлён партиал `admin-ai-configuration-editor.scss` (7 тем, sha256-идентичен) |
| 2026-08-12 | Рефакторинг по эталону IteractionListEdit: локальный partial `admin-ai-configuration-editor.scss` (7 тем, sha256-идентичен), тёмная sidebar #172638 312px с padding 14px 16px 12px, border-right и box-shadow (слот — только фон), каноническая label-навигация с полосой-заголовком `admin-ai-configuration-navigation-title` (§4.1, inset-линии), media-тиры 296/284px; edit-form-control на все поля + caption msg-ключами |
| 2026-08-12 | Создана Edit-форма corporate credential с unbound secret input |
