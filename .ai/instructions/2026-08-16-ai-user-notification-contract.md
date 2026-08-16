# Задание: контракт пользовательской нотификации об AI-операциях

Дата: 2026-08-16
Автор: Hermes-2 (ветка agent/hermes2-dev)
Адресат: Hermes-1 (проверка PR, merge, deploy)

## Дополнение (2026-08-16): «AI-нотификации 2 раза»

По запросу пользователя AI-нотификации показываются **2 раза** — при начале
обработки и после её завершения:

- `AiOperationNotifier.showStarted(notifications, caption, detail)` — стартовая
  исчезающая TRAY-нотификация (5 с, правый нижний угол); при пустом detail —
  обещание итоговой: «После завершения будет указана модель и собственник API».
- Подключена: `ProjectEdit` («Кратко», AI-обработка описания — вместо прежних
  «сырых» TRAY), `CandidateCVEdit` («Запущен AI-анализ навыков резюме…»),
  `WebProjectLogoFileUploadField` (логотип — по флагу
  `hunttech.projectLogo.ai.enabled`; фото кандидата — по
  `hunttech.projectLogo.rembg.enabled`; при чисто классическом конвейере старт
  не показывается).
- Итоговые нотификации не изменились (модель + собственник API).
- Контракт-тест дополнен: `aiNotificationsAreShownTwiceStartedAndCompleted`
  (13/13 зелёные), контракт-документ §4, спеки экранов, отчёт обновлены.

## Дополнение (2026-08-16): аудит всех AI-вызовов — нотификации во всех формах

Проведён полный аудит всех AI-вызовов (middleware + UI). Полный инвентарь:

| Экран / компонент | Операция | Нотификация |
|---|---|---|
| ProjectEdit — «Кратко» | PROJECT_SHORT_DESCRIPTION_GENERATE | ✅ контракт (старт + итог с моделью/собственником) |
| ProjectEdit — upload описания | PROJECT_DESCRIPTION_GENERATE | ✅ контракт (старт + итог) |
| CandidateCVEdit — «Сканировать навыки» | SKILLS_EXTRACT | ✅ контракт (старт + итог) |
| WebProjectLogoFileUploadField — логотип | PROJECT_LOGO_IMAGE_GENERATE | ✅ контракт (старт + итог) |
| WebProjectLogoFileUploadField — фото кандидата | локальный rembg | ✅ нотификация (без собственника — локальная NN) |
| UserAiConfigurationBrowse — «Проверить подключение» | TEST_CONNECTION | ✅ БЫЛО ❌ → переведено на контракт |
| ExtSettingsWindow — «Проверить подключение» | TEST_CONNECTION | ✅ БЫЛО ❌ → переведено на контракт |

Найденные пробелы закрыты:
- `HrmAiService.testConnection` возвращал `void` — теперь `AiExecutionResult`
  (модель, провайдер, собственник API = личный ключ пользователя `USER`); реальный
  AI-вызов остался прямым (проверка конкретного credential до назначения на функцию).
- `UserAiConfigurationBrowse`/`ExtSettingsWindow`: обычные TRAY/HUMANIZED без
  автоскрытия и без модели → контрактная исчезающая TRAY (5 с) с блоком
  «Модель · Провайдер / Собственник API: личный (пользователя)».
- Контракт-документ §2: диагностика перемещена из «вне области» в область действия.
- Контракт-тест: `connectionTestIsRealAiCallWithContractNotification` (14/14 зелёные).
- Доки: `docs/services/HrmAiService.md`, `docs/integrations/ai/AI_INTEGRATION.md`,
  `docs/integrations/ai/USER_AI_SETTINGS_IMPLEMENTATION.md`.

Прямых AI-вызовов в админ-экранах AI Control Plane (adminaiconfiguration,
aifunctionconfiguration, useraifunctionoverride) нет; `JobCandidateEdit` использует
фото-нотификацию через `WebProjectLogoFileUploadField`. Боты (HrmAiService String)
— вне UI-контракта (зафиксировано в §2.3).

## Основное изменение (первоначальный контракт)

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
