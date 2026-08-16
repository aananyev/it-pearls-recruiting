# Задание: контракт пользовательской нотификации об AI-операциях

Дата: 2026-08-16
Автор: Hermes-2 (ветка agent/hermes2-dev)
Адресат: Hermes-1 (проверка PR, merge, deploy)

## Что сделано

Введён контракт для ВСЕХ сервисов, которые вызывают AI-функции: каждая реальная
AI-операция, инициированная из UI, завершается **исчезающим оповещением пользователя
стандартными средствами CUBA Platform** (Notifications, TRAY, правый нижний угол,
автоскрытие 5 с), в котором указано, **какая модель что сделала** и **кто является
собственником использованного API** — административное API (корпоративное подключение)
или API пользователя (личное подключение + override).

Полный текст контракта: `docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md`.

### Поток метаданных (слой 1 — middleware)

1. `modules/global/src/com/company/hunttech/service/AiExecutionResult.java` — результат
   AI-выполнения: payload (`getText()`/`getImage()`) + метаданные нотификации
   (`getFunctionCode()`, `getFunctionName()`, `getModelName()`, `getProviderCode()`,
   `getCredentialOwner()`).
2. `modules/global/src/com/company/hunttech/service/AiCredentialOwner.java` — enum
   `ADMIN` (корпоративное подключение `HUNTTECH_ADMIN_AI_CONFIGURATION`) / `USER`
   (личное подключение `HUNTTECH_USER_AI_CONFIGURATION` + override).
3. `AiExecutionService.executeText/executeImage` возвращают `AiExecutionResult` вместо
   `String`/`byte[]`. `AiExecutionServiceBean`: пользовательский путь проставляет
   `credentialOwner=USER`, административный — `ADMIN` (включая image-пути и fallback).
4. Фасады пробрасывают метаданные: `ProjectAiService` → `AiExecutionResult`;
   `SkillAnalysisService` → новый `SkillAnalysisResult` (навыки + `aiExecution`,
   `null` при классическом fallback — нотификация «обработано ИИ» не показывается);
   `ProjectLogoImageProcessingService` → `ProcessedImage.getAiExecution()` (заполнен
   только при реальном применении AI-функции логотипа).
5. `HrmAiService` (legacy, вызывается ботами) сохранён с возвратом `String` —
   сознательное исключение, зафиксировано в контракте и тесте.

### Слой 2 — web-нотификация

6. `modules/web/src/com/company/hunttech/web/util/AiOperationNotifier.java` — единая
   точка показа: TRAY, BOTTOM_RIGHT, `withHideDelayMs(5000)`, HTML; блок
   «Модель: X · Провайдер: Y / Собственник API: корпоративный (администратора) или
   личный (пользователя)».

### Слой 3 — экраны

7. `ProjectEdit` — «Кратко» и AI-обработка загруженного описания: нотификация с
   моделью/собственником API в `done()`.
8. `CandidateCVEdit` — «Сканировать навыки»: в исчезающую нотификацию статистики
   (5 с) добавлен блок модели/собственника API от `AiOperationNotifier`.
9. `WebProjectLogoFileUploadField` — логотип, обработанный AI-функцией
   `PROJECT_LOGO_IMAGE_GENERATE`: нотификация «Логотип обработан с помощью AI»
   с моделью/собственником API; фото кандидата (локальный rembg) — прежняя
   нотификация без собственника API (локальная нейросеть, не внешний API).

### Тесты

- Новый контракт-тест `AiUserNotificationContractTest` (reflection + source-проверки:
  возврат метаданных, enum собственника, TRAY/5 с/подписи, подключение во всех 3 экранах).
- `SkillAnalysisServiceBeanTest` переведён на `SkillAnalysisResult` + 2 новых кейса:
  метаданные при AI-анализе; `aiExecution == null` при классическом fallback.
- `ProjectLogoAiFunctionSeedContractTest` обновлён под новый возврат `executeImage`.

## Как проверено

- `:app-core:test` — `AiUserNotificationContractTest`, `SkillAnalysisServiceBeanTest`,
  `ProjectLogoAiFunctionSeedContractTest`, `ProjectShortDescriptionAiContractTest`,
  `ProjectDescriptionAiUploadContractTest`, `ProjectLogoImageProcessingServiceCoreBeanLookupTest`,
  `ScreenViewIntegrityTest` (через `scripts/agent-gradle.sh`, сериализация соблюдена).

## Что ожидается от Hermes-1

- Проверить PR (база master, ветка agent/hermes2-dev, метка WAITING_FOR_HERMES).
- После merge + deploy + restart: smoke — «Кратко» в ProjectEdit и «Сканировать навыки»
  в CandidateCVEdit показывают исчезающую нотификацию с моделью и строкой
  «Собственник API: корпоративный (администратора)» (при активном пользовательском
  override — «личный (пользователя)»); загрузка логотипа с работающей AI-функцией —
  аналогичная нотификация; без AI (классический fallback) блок не показывается.
