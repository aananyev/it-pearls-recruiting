# Контракт пользовательской нотификации об AI-операциях HRM HuntTech

> Область: все сервисы HRM HuntTech, которые вызывают AI-функции, и все экраны, инициирующие AI-операции.
> Платформа: CUBA Platform 7.3.
> Статус: утверждённый контракт (2026-08-16).
> Связанные документы: [AI Function Management Architecture](HRM_HuntTech_AI_Function_Management_Architecture.md), [AiExecutionService](../../docs/services/AiExecutionService.md), [SkillAnalysisService](../../docs/services/SkillAnalysisService.md), [ProjectAiService](../../docs/services/ProjectAiService.md), [ProjectLogoImageProcessingService](../../docs/services/ProjectLogoImageProcessingService.md).
> Код контракта зафиксирован тестом: `modules/core/test/com/company/hunttech/core/AiUserNotificationContractTest.java`.

## 1. Назначение и бизнес-смысл (What & Why)

AI-операции в HRM HuntTech выполняются централизованно через AI Control Plane:
бизнес-экран не выбирает провайдера, модель и API-ключ — он передаёт стабильный
`functionCode` и контекст. Пользователь при этом остаётся «в неведении», какой
моделью и чьим API-подключением выполнена операция, тогда как:

- стоимость операции несёт либо компания (корпоративное подключение администратора),
  либо сам пользователь (его личный ключ);
- качество результата зависит от модели;
- пользователь вправе знать, что его персональный API-ключ был использован
  (или что операция оплачена корпоративным ключом).

**Контракт устанавливает обязательство:** каждая реальная AI-операция, инициированная
из UI, завершается **исчезающим оповещением пользователя стандартными средствами
CUBA Platform**, в котором указано, **какая модель что сделала** и **кто является
собственником использованного API** — административное API (корпоративное подключение)
или API пользователя (личное подключение).

Контракт не добавляет новых сущностей и миграций: он формализует поток метаданных,
которые execution layer уже знает в момент вызова (модель, провайдер, источник
credentials), и единую форму их показа в UI.

## 2. Область действия (Scope)

### 2.1. Сервисы, вызывающие AI-функции (обязаны возвращать метаданные)

| Сервис | Методы | Возврат | Метаданные |
|---|---|---|---|
| `AiExecutionService` (корень) | `executeText`, `executeImage` | `AiExecutionResult` | функция, модель, провайдер, собственник API |
| `ProjectAiService` | `processUploadedDescription`, `generateShortDescription` | `AiExecutionResult` | пробрасываются от `AiExecutionService` |
| `SkillAnalysisService` | `analyzeAll/Main/Secondary/Tertiary` | `SkillAnalysisResult` (`skills` + `aiExecution`) | `aiExecution` — при AI-анализе; `null` при классическом fallback |
| `ProjectLogoImageProcessingService` | `process` | `ProcessedImage` (+ `aiExecution`) | только когда фон удалён AI-функцией `PROJECT_LOGO_IMAGE_GENERATE` |
| `HrmAiService` (legacy) | `standardizeVacancyDescription`, `generateVacancyArtifact` | `String` | **сознательное исключение**: методы возвращают `String` для внешних потребителей (боты); метаданные доступны на уровне вызываемого `AiExecutionService` |

### 2.2. Экраны, показывающие нотификацию

| Экран / компонент | Операция | Что показывает нотификация |
|---|---|---|
| `ProjectEdit` — кнопка «Кратко» | `PROJECT_SHORT_DESCRIPTION_GENERATE` | «Краткое описание проекта сгенерировано» + модель + собственник API |
| `ProjectEdit` — загрузка описания | `PROJECT_DESCRIPTION_GENERATE` | «Описание проекта обработано ИИ» + модель + собственник API |
| `CandidateCVEdit` — «Сканировать навыки» | `SKILLS_EXTRACT` | «Статистика анализа навыков» + модель + собственник API (добавляются в ту же исчезающую нотификацию) |
| `WebProjectLogoFileUploadField` — логотип | `PROJECT_LOGO_IMAGE_GENERATE` | «Логотип обработан с помощью AI» + модель + собственник API |
| `WebProjectLogoFileUploadField` — фото кандидата | локальный rembg/u2net (не API) | «Фотография обработана с помощью AI» — без собственника API (локальная нейросеть, не внешний API) |

### 2.3. Вне области

- **Диагностические операции AI Control Plane** (`testConnection` в
  `HrmAiService`/экранах «Управление AI») — это проверка подключения, а не бизнес-операция;
  нотификация не требуется (экраны показывают статус проверки напрямую).
- **Внешние потребители** (боты, REST) — UI-нотификация им не адресуется; контракт
  метаданных соблюдается на уровне `AiExecutionService`.
- **Классический fallback без AI** (словарный поиск навыков, классический flood-fill,
  простая конвертация/ресайз) — нотификация «обработано ИИ» **не показывается**
  (честная семантика, см. §5).

## 3. Поток метаданных (Data Flow)

```text
Экран ──functionCode+контекст──▶ AiExecutionService (AI Control Plane)
                                    │ policy: USER_REQUIRED / USER_OVERRIDE_ALLOWED / ADMIN_ONLY
                                    │ fallback: FALLBACK_TO_ADMIN
                                    ▼
                              выполнение провайдером
                                    │
                                    ▼
                        AiExecutionResult{ text/image,
                                           functionCode, functionName,
                                           modelName, providerCode,
                                           credentialOwner: ADMIN | USER }
                                    │
        ┌───────────────────────────┼───────────────────────────────┐
        ▼                           ▼                               ▼
ProjectAiService            SkillAnalysisService            ProjectLogoImageProcessing
  (пробрасывает)            SkillAnalysisResult{             ProcessedImage{ aiExecution }
  AiExecutionResult         skills, aiExecution }                    │
        │                           │                                 ▼
        ▼                           ▼                        WebProjectLogoFileUploadField
   ProjectEdit               CandidateCVEdit                       │
        │                           │                               ▼
        └───────────┬───────────────┴───────────────┬─────────── AiOperationNotifier (web)
                    ▼                               ▼
        исчезающая TRAY-нотификация CUBA   «Модель: X · Провайдер: Y
        (5 с, правый нижний угол)          Собственник API: корпоративный (администратора)
                                           / личный (пользователя)»
```

## 4. Спецификация нотификации (UI Contract)

Реализация — web-утилита `modules/web/src/com/company/hunttech/web/util/AiOperationNotifier.java`.

| Параметр | Значение (контракт) |
|---|---|
| Механизм | стандартные `Notifications` CUBA Platform (никаких кастомных виджетов) |
| Тип | `Notifications.NotificationType.TRAY` |
| Позиция | `Notifications.Position.BOTTOM_RIGHT` (правый нижний угол) |
| Автоскрытие | `withHideDelayMs(5000)` — исчезает сама через 5 секунд |
| Контент | `ContentMode.HTML` |
| Заголовок («что делала») | передаёт вызывающий экран (свои messageBundle-ключи), например «Краткое описание проекта сгенерировано» |
| Описание («какая модель») | `Модель: <modelName> · Провайдер: <providerCode>` |
| Собственник API | `Собственник API: корпоративный (администратора)` для `ADMIN`; `Собственник API: личный (пользователя)` для `USER` |

Пример отображаемого текста нотификации:

```text
Краткое описание проекта сгенерировано
Модель: deepseek-v4-flash · Провайдер: deepseek
Собственник API: корпоративный (администратора)
```

## 5. Определение собственника API (Credential Owner)

**Собственник API** — источник API-credentials, которыми реально выполнен вызов
(с учётом override и fallback). Определяется execution layer в момент вызова:

| Значение `AiCredentialOwner` | Источник | Когда применяется |
|---|---|---|
| `ADMIN` — административное API | `HUNTTECH_ADMIN_AI_CONFIGURATION` (корпоративное подключение, привязано к функции через `admin_configuration_id`; ключ зашифрован) | политика `ADMIN_ONLY`; отсутствие пользовательского override; разрешённый fallback после ошибки пользовательского подключения |
| `USER` — API пользователя | `HUNTTECH_USER_AI_CONFIGURATION` (личный ключ) + `HUNTTECH_USER_AI_FUNCTION_OVERRIDE` (активное замещение функции) | политика `USER_REQUIRED`; политика `USER_OVERRIDE_ALLOWED` при активном usable override |

Правила:

1. **Не выводить секреты**: нотификация показывает только `modelName`, `providerCode`
   и класс собственника — никогда API-ключ, логин, email владельца и т.п.
2. **Эффективная модель**: показывается модель, которая реально выполнила запрос
   (для `ADMIN` — `adminModelName` функции или `defaultModelName` подключения;
   для `USER` — `defaultModelName` личного подключения, при `allowModelOverride` —
   модель override).
3. **Fallback честен**: если пользовательский вызов упал и сработал `FALLBACK_TO_ADMIN`,
   нотификация сообщает `ADMIN` (реально выполнил корпоративный ключ).
4. **Локальные нейросети — не API**: rembg/u2net выполняется на сервере приложения
   без внешних API и ключей; нотификация о нём не содержит собственника API
   («Фон удалён автоматически нейросетью»), а при классическом конвейере не
   показывается вовсе.

## 6. Контракт сервисов (Service Contract)

### 6.1. `AiExecutionService` (корень)

```java
AiExecutionResult executeText(String functionCode, Map<String, Object> context);
AiExecutionResult executeImage(String functionCode, Map<String, Object> context,
                               byte[] sourceImage, String sourceMimeType);
```

`AiExecutionResult` (modules/global, `com.company.hunttech.service`):

- `getText()` / `getImage()` — payload (по capability);
- `getFunctionCode()`, `getFunctionName()` — что делала (стабильный код + отображаемое имя функции);
- `getModelName()`, `getProviderCode()` — какая модель и через какой провайдер;
- `getCredentialOwner()` — собственник API: `AiCredentialOwner.ADMIN | USER`.

Обязательства бина `AiExecutionServiceBean`:

- путь пользовательского override (`executeWithUser` / `executeWithUserImage`) →
  `credentialOwner = USER`;
- административный путь (`executeWithAdmin` / `executeWithAdminImage`) →
  `credentialOwner = ADMIN`;
- каждый успешный реальный вызов возвращает заполненные метаданные.

### 6.2. Фасады

- `ProjectAiService`: методы возвращают `AiExecutionResult` (проброс от
  `AiExecutionService`); экран `ProjectEdit` показывает нотификацию в `done()`.
- `SkillAnalysisService`: методы возвращают `SkillAnalysisResult`
  (`getSkills()` + `getAiExecution()`); `getAiExecution() == null` при классическом
  fallback — экран в этом случае не добавляет блок «модель/собственник API».
- `ProjectLogoImageProcessingService` → `ProcessedImage.getAiExecution()` заполнен
  только при реальном применении AI-функции `PROJECT_LOGO_IMAGE_GENERATE`.
- `HrmAiService` (legacy, для ботов): возвращает `String`; контракт метаданных
  соблюдается вызываемым `AiExecutionService`.

### 6.3. Обязательство экранов

Каждый экран/компонент, инициирующий AI-операцию, обязан в `done()`-обработчике
вызвать `AiOperationNotifier.show(notifications, result, caption, detail)` (или
добавить `AiOperationNotifier.buildDescription(...)` в существующую исчезающую
нотификацию). Нотификация показывается **только при реальном AI-выполнении**
(метаданные не `null`).

## 7. Тесты контракта

`modules/core/test/com/company/hunttech/core/AiUserNotificationContractTest.java`
фиксирует:

- возврат `AiExecutionResult` методами `AiExecutionService` (reflection);
- структуру результата: модель, провайдер, собственник API;
- enum `AiCredentialOwner` с `ADMIN` и `USER`;
- простановку собственника в обоих путях бина (source-проверки);
- проброс метаданных фасадами `ProjectAiService` / `SkillAnalysisService`;
- legacy-контракт `HrmAiService` (`String`);
- нотификацию: TRAY, BOTTOM_RIGHT, автоскрытие 5 с, подписи «Модель», «Провайдер»,
  «Собственник API: корпоративный (администратора) / личный (пользователя)»;
- подключение нотификации в `ProjectEdit`, `CandidateCVEdit`, `WebProjectLogoFileUploadField`;
- семантику fallback: `SkillAnalysisServiceBean` возвращает `aiExecution == null`
  при классическом поиске.

Функционально fallback-семантика покрыта `SkillAnalysisServiceBeanTest`
(метаданные при AI; `null` при сбое AI-шлюза).

## 8. История изменений

| Дата | Изменение |
|---|---|
| 2026-08-16 | Контракт введён: `AiExecutionResult`/`AiCredentialOwner`/`SkillAnalysisResult`; `AiExecutionService` и фасады возвращают метаданные; web-утилита `AiOperationNotifier` (TRAY, 5 с); нотификации в `ProjectEdit` («Кратко», upload), `CandidateCVEdit` («Сканировать навыки»), `WebProjectLogoFileUploadField` (логотип при AI-функции); контракт-тест `AiUserNotificationContractTest` |
