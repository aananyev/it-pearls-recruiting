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

Label navigation presentation-only. Footer standard commit/close.

## 6. Visual Layout Schema

Shared Edit: sidebar 312px → workspace → toolbar → three `edit-card` → footer. Root `admin-ai-configuration-editor`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создана Edit-форма corporate credential с unbound secret input |
