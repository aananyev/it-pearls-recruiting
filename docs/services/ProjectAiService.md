# ProjectAiService

## Назначение и бизнес-смысл (What & Why)

`ProjectAiService` — domain facade между `ProjectEdit` и общим AI Control Plane HRM HuntTech. Он не хранит prompt/provider/model/API key и не разрешает бизнес-экрану выбирать их напрямую. Это позволяет администратору менять качество, стоимость, модель и содержание prompt без выпуска новой версии ProjectEdit.

## UI Context & Navigation

Потребитель сервиса: `ProjectBrowse → ProjectEdit → Описание проекта → Загрузить описание`. Сервис не имеет собственного UI. Административная конфигурация выполняется в «Управление AI» → «Функции AI» для кода `PROJECT_DESCRIPTION_GENERATE` и в связанных корпоративных/пользовательских подключениях.

## Behavior Summary

- upload извлечён → `ProjectEdit` передал `projectName/sourceFileName/sourceText` → сервис вызывает `AiExecutionService`;
- source text пуст → вызов внешнего AI не выполняется → controlled `DevelopmentException`;
- source text > 120000 символов → внешний AI не вызывается → controlled error;
- кнопка «Кратко» → `ProjectEdit` передал `projectName/descriptionText` → сервис вызывает `AiExecutionService` с функцией `PROJECT_SHORT_DESCRIPTION_GENERATE`;
- краткое описание пусто → вызов внешнего AI не выполняется → controlled `DevelopmentException`;
- функция настроена → resolver выбирает credential/model/provider по policy → результат возвращается экрану;
- prompt/model/policy изменены администратором → сервисный код не меняется → следующий вызов использует новые настройки.

## API

```java
AiExecutionResult processUploadedDescription(String projectName,
                                             String sourceFileName,
                                             String sourceText);

AiExecutionResult generateShortDescription(String projectName,
                                           String descriptionText);
```

Возврат — `AiExecutionResult` (`modules/global/.../service/AiExecutionResult.java`):
`getText()` — сгенерированный текст (для подстановки в сущность/форму) + метаданные
для контракта пользовательской нотификации: `getModelName()`/`getProviderCode()`
(какая модель), `getCredentialOwner()` (собственник API — `AiCredentialOwner.ADMIN`
корпоративное подключение или `USER` личное подключение пользователя). Экраны
показывают исчезающую TRAY-нотификацию (web-утилита `AiOperationNotifier`) — полный
контракт: [HRM_HuntTech_AI_User_Notification_Contract](../architecture/HRM_HuntTech_AI_User_Notification_Contract.md).

Стабильные function code:

```text
PROJECT_DESCRIPTION_GENERATE
PROJECT_SHORT_DESCRIPTION_GENERATE
```

Context:

```text
processUploadedDescription:      projectName, sourceFileName, sourceText
generateShortDescription:        projectName, sourceText
```

## Граница ответственности

`ProjectAiService` отвечает за бизнес-контракт Project AI и валидацию размера контекста. `AiExecutionService` отвечает за prompt rendering, execution policy, user/admin override, fallback и выбор provider. `AIProviderRegistry` отвечает за vendor-specific transport.

## Безопасность

Сервис не принимает API key/model/provider от ProjectEdit и не возвращает credential. Реальные секреты не входят в migration seed, docs или tests.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-16 | Контракт пользовательской нотификации: методы возвращают `AiExecutionResult` (текст + модель, провайдер, собственник API); `ProjectEdit` показывает исчезающую нотификацию «какая модель что сделала + чей API» |
| 2026-08-14 | Добавлен `generateShortDescription(projectName, descriptionText)` для функции `PROJECT_SHORT_DESCRIPTION_GENERATE` (кнопка «Кратко» ProjectEdit, sidebar-раздел «Коротко») |
| 2026-08-12 | Создан facade `ProjectAiService` для `PROJECT_DESCRIPTION_GENERATE` поверх AI Control Plane |
