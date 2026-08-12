# Архитектура управления AI-функциями HRM HuntTech

> Область: централизованное управление AI-функциями, промптами, моделями, корпоративными и пользовательскими API-подключениями.  
> Платформа: CUBA Platform 7.3.  
> Статус: целевая архитектурная спецификация.  
> Базовый GitHub `master` при подготовке документа: `aee44f6cd726b3dbffe305a1414ac0375c93ab1e`.  
> Связанные документы: [AI Integration](../integrations/ai/AI_INTEGRATION.md), [UI/UX Design Concept](HRM_HuntTech_UI_UX_Design_Concept.md), [Edit Screen Shared Style Contract](HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md), [XML Screen Documentation Standard](XML_Screen_Documentation_Standard.md).

## Business & Context Intro

### 1. Назначение и бизнес-смысл (What & Why)

AI в HRM HuntTech должен управляться не отдельными кнопками и не напрямую выбранным пользователем провайдером, а централизованным набором **AI-функций**. Каждая AI-функция описывает бизнес-задачу, допустимый тип обработки, промпт, модель, корпоративное подключение, политику пользовательского замещения и fallback.

Цель архитектуры — дать администратору единый контроль над стоимостью, качеством, безопасностью и назначением AI, одновременно позволяя пользователю использовать собственный более качественный API для отдельных разрешённых функций. Корпоративные секреты при этом не раскрываются обычным пользователям.

Примеры AI-функций:

- стандартизация текста вакансии;
- подготовка текста публикации вакансии;
- анализ и краткое резюме CV;
- подготовка вопросов к интервью;
- трансформация и редактирование текста;
- анализ изображения;
- генерация изображения;
- анализ документа;
- рекомендации по проектам на основании профессионального опыта и компетенций.

### 2. Связи в интерфейсе и навигация (UI Context & Navigation)

Административный контур размещается в существующем меню **«Управление AI»**. Целевая структура меню:

```text
Управление AI
├── Функции AI
├── Корпоративные подключения
├── Шаблоны промптов / Legacy
└── Пользовательские подключения
```

Обычный пользователь не получает доступ к сущности корпоративных подключений и не читает корпоративные ключи через UI, DataManager, REST или browse-view. Пользователь управляет только собственными подключениями и разрешёнными для него переопределениями AI-функций.

AI-вызовы из бизнес-экранов (`ProjectEdit`, `OpenPositionEdit`, `CandidateCVEdit` и других) не должны напрямую выбирать API-ключ или HTTP endpoint. Экран передаёт код функции и контекст в единый execution layer.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- пользователь запускает AI-действие → экран передаёт `functionCode` и бизнес-контекст → resolver определяет эффективную конфигурацию;
- функция запрещает пользовательское замещение → используется только корпоративное подключение;
- функция разрешает пользовательское замещение и у пользователя есть активный override → используется пользовательское подключение;
- пользовательское подключение отсутствует → используется корпоративное подключение;
- пользовательское подключение недоступно и политикой разрешён fallback → запрос повторяется через корпоративное подключение;
- корпоративный ключ используется → секрет расшифровывается только в core-слое непосредственно перед вызовом провайдера;
- администратор меняет prompt/model/policy → бизнес-экраны не изменяются → следующий вызов использует новую конфигурацию;
- AI-функция отключена → вызов блокируется централизованно до обращения к внешнему API.

---

## 1. Нормативные UI-контракты

### 1.1. Edit-формы

Для всех новых Edit-форм AI-подсистемы обязательны актуальные документы:

1. `HRM_HuntTech_UI_UX_Design_Concept.md`;
2. `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`;
3. `XML_Screen_Documentation_Standard.md`;
4. UI-спецификация конкретного экрана.

Edit-форма должна сохранять CUBA-контракты и использовать преимущественно общий UI API `edit-*` и `label-*`. Для сложной формы используются постоянная левая контекстная панель, `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active`, правая рабочая область, карточки и стандартные footer-actions. Локальный root namespace обязателен.

### 1.2. Browse-формы

На базовом `master` `aee44f6cd726b3dbffe305a1414ac0375c93ab1e` отдельный канонический файл общего Browse-контракта в `docs/architecture/` отсутствует. Поэтому настоящий документ **не объявляет новый глобальный Browse-контракт** и не подменяет отсутствующий нормативный документ.

До появления отдельного утверждённого Browse-контракта новые AI Browse-экраны обязаны:

- следовать `HRM_HuntTech_UI_UX_Design_Concept.md`;
- использовать штатный CUBA `StandardLookup`/container/loader lifecycle;
- сохранять существующие actions и права доступа;
- использовать локальный visual namespace;
- не применять неограниченные глобальные Vaadin-селекторы;
- показывать в таблице только данные, необходимые для выбора и администрирования;
- не загружать и не отображать секретные API-ключи в browse-view;
- иметь явный toolbar действий `Создать / Редактировать / Удалить / Проверить` только там, где действия разрешены бизнес-контрактом;
- документировать фильтры, loader, view, actions и переход в Edit-форму;
- после появления отдельного утверждённого Browse-контракта привести AI Browse-формы к нему отдельной задачей без изменения бизнес-логики.

Если в более новом `master` появится отдельный Browse-контракт, он имеет приоритет над локальными UI-требованиями настоящего документа.

---

## 2. Текущее состояние AI-подсистемы

На указанном базовом `master` уже существуют:

- `UserAiConfiguration` — персональное подключение пользователя: `user`, `providerCode`, `apiKey`, `defaultModelName`, `isActive`;
- `VacancyPromptTemplate` — промпты для vacancy-сценариев: `code`, `name`, `promptText`, `systemContext`, `temperature`;
- `UserAiProfile` — персональный профессиональный контекст и предпочтения ответа;
- `HrmAiService` / `HrmAiServiceBean` — сервисная оркестрация vacancy AI;
- `AIProvider` / `AIProviderRegistry` — Strategy-слой провайдеров;
- меню `aiAdministration` («Управление AI»).

Текущий `HrmAiServiceBean` выбирает активный `UserAiConfiguration` текущего пользователя по `providerCode`. Системной маршрутизации `function → policy → effective credential` в `master` пока нет.

`UserAiProfile` не должен использоваться для хранения ключей или маршрутизации. Его ответственность — персональный контекст, язык, детализация и стиль ответа.

---

## 3. Целевая модель данных

### 3.1. `AiFunctionConfiguration` — центральная сущность

Главная сущность AI Control Plane. Описывает **что** делает AI и **по каким правилам** выполняется функция.

Рекомендуемые поля:

| Поле | Назначение |
|---|---|
| `code` | стабильный уникальный код функции |
| `name` | человекочитаемое название |
| `description` | бизнес-назначение |
| `capability` | тип AI-задачи |
| `systemPrompt` | system context |
| `promptTemplate` | шаблон основного prompt |
| `temperature` | параметр генерации |
| `maxTokens` | лимит ответа |
| `adminConfiguration` | корпоративное подключение по умолчанию |
| `adminModelName` | модель функции, если отличается от default подключения |
| `executionPolicy` | политика корпоративного/пользовательского выполнения |
| `fallbackPolicy` | поведение при ошибке пользовательского подключения |
| `allowModelOverride` | может ли пользователь заменить модель |
| `active` | функция доступна |
| `version` | версия конфигурации/prompts |

Рекомендуемые значения `capability`:

```text
TEXT_GENERATION
TEXT_ANALYSIS
TEXT_TRANSFORMATION
VISION
IMAGE_GENERATION
EMBEDDING
DOCUMENT_ANALYSIS
AUDIO_TRANSCRIPTION
```

Необязательно реализовывать все capability на первом этапе, но модель данных не должна быть ограничена только `generateText()`.

### 3.2. `AdminAiConfiguration` — корпоративные подключения

Отдельная защищённая сущность для корпоративных credentials.

Рекомендуемые поля:

| Поле | Назначение |
|---|---|
| `name` | понятное администратору имя подключения |
| `providerCode` | код `AIProviderRegistry` |
| `apiKeyEncrypted` | зашифрованный credential |
| `defaultModelName` | модель по умолчанию |
| `baseApiUrl` | optional endpoint override для совместимого gateway |
| `active` | доступность подключения |
| `priority` | порядок при резервировании |
| `lastTestStatus` | результат последней проверки |
| `lastTestAt` | время проверки |
| `lastError` | диагностическая причина без секрета |

Корпоративный credential физически отделён от пользовательских записей. Простого скрытия колонки `apiKey` недостаточно.

### 3.3. `UserAiConfiguration` — персональные credentials

Существующая legacy-сущность сохраняет имя и идентификаторы. Её целевая ответственность — список доступных пользователю персональных подключений.

Рекомендуемое развитие без переименования legacy-полей:

- шифрование значения `apiKey` при хранении;
- `displayName` для различения нескольких подключений;
- статус последней проверки;
- capability metadata при необходимости;
- запрет чтения чужих записей через пользовательский UI.

### 3.4. `UserAiFunctionOverride` — замещение конкретной функции

Связь пользователя, AI-функции и его персонального подключения.

Минимальные поля:

```text
user
aiFunction
userAiConfiguration
modelName
enabled
```

Обязательный уникальный контракт:

```text
USER_ID + AI_FUNCTION_ID
```

Пользовательское подключение не должно автоматически заменять корпоративный API для всех AI-действий приложения.

### 3.5. `UserAiProfile`

Существующая сущность остаётся отдельным контекстным слоем:

```text
AiFunctionConfiguration = что и по каким правилам делать
Credential routing       = через какое подключение выполнять
UserAiProfile            = как адаптировать ответ под пользователя
```

---

## 4. Политики выполнения

Рекомендуемый enum `AiExecutionPolicy`:

### `ADMIN_ONLY`

Функция всегда выполняется через корпоративное подключение. Пользовательский override запрещён.

Подходит для регламентных, фоновых и других функций, для которых администратор должен полностью контролировать провайдера, стоимость и контракт обработки.

### `USER_OVERRIDE_ALLOWED`

Основной интерактивный сценарий:

1. есть активный override пользователя → использовать его;
2. override отсутствует → использовать корпоративное подключение;
3. пользовательский API недоступен → применить `fallbackPolicy`.

### `USER_REQUIRED`

Функция доступна только при настроенном персональном подключении. Корпоративные лимиты не используются.

Рекомендуемый `AiFallbackPolicy`:

```text
NO_FALLBACK
FALLBACK_TO_ADMIN
FALLBACK_TO_SECONDARY_ADMIN
```

---

## 5. Resolver и execution layer

### 5.1. `AiConfigurationResolver`

Единая точка выбора effective configuration.

Целевой алгоритм:

```text
functionCode
    ↓
AiFunctionConfiguration
    ↓
active?
    ↓
executionPolicy
    ├── ADMIN_ONLY → AdminAiConfiguration
    ├── USER_REQUIRED → UserAiFunctionOverride → UserAiConfiguration
    └── USER_OVERRIDE_ALLOWED
            ├── override есть → UserAiConfiguration
            └── override нет → AdminAiConfiguration
    ↓
AiExecutionContext
```

`AiExecutionContext` должен содержать уже разрешённые:

- provider code;
- effective model;
- prompt/system prompt;
- generation options;
- ссылку на credential source;
- fallback policy;
- function code/version.

Секрет в DTO web-слоя не передаётся.

### 5.2. `AiExecutionService`

Бизнес-экраны должны зависеть от единого фасада уровня функции, например:

```java
AiResponse execute(String functionCode, Map<String, Object> context);
```

или от типизированного запроса `AiRequest`, но не от `providerCode` и не от `apiKey`.

Допустимо сохранять узкие domain facade-сервисы (`AiProjectService`, `AiVacancyService`, `AiInterviewQuestionService`), если они отвечают только за подготовку бизнес-контекста и делегируют выбор prompt/provider/key/model в `AiExecutionService`.

### 5.3. `AIProviderRegistry`

Существующий Strategy-подход сохраняется:

```text
providerCode → Java implementation
```

База данных определяет **что использовать**, а Java-провайдер определяет **как вызвать конкретный API**. Не следует переносить в БД детали OAuth, Anthropic Messages, Gemini GenerateContent, YandexGPT или другого vendor-specific протокола.

---

## 6. Безопасность credentials

### 6.1. Корпоративные ключи

Обычный пользователь не должен иметь:

- entity READ на корпоративный credential;
- browse/edit screen permission;
- REST-доступ;
- доступ через generic entity inspector;
- возможность увидеть ключ в логах или exception message.

### 6.2. Шифрование

Текущее хранение `UserAiConfiguration.apiKey` как обычного строкового значения допустимо только как legacy-состояние, но не как целевая модель корпоративных секретов.

Целевой компонент:

```text
AiSecretService
```

Поток:

```text
UI / resolver
    ↓
credential id
    ↓
core AiSecretService
    ↓ decrypt непосредственно перед вызовом
AIProvider
```

В UI после сохранения показывается только состояние `Ключ настроен`. Операции «показать существующий ключ» быть не должно.

---

## 7. Формы управления AI

### 7.1. `AiFunctionConfigurationBrowse`

Показывает:

- код;
- название;
- capability;
- execution policy;
- корпоративное подключение по имени без секрета;
- effective model;
- active;
- version.

Основные действия: create/edit, activate/deactivate, test configuration при наличии безопасного тестового сценария.

### 7.2. `AiFunctionConfigurationEdit`

Edit-форма строится по общему Edit-контракту HRM HuntTech.

Рекомендуемая label-навигация:

```text
Основное
Маршрутизация
Промпт
Параметры модели
Безопасность и fallback
```

Sidebar содержит наименование функции, capability, active-state и policy summary. В правой рабочей области — тематические карточки. Standard editor actions остаются в footer.

### 7.3. `AdminAiConfigurationBrowse`

Показывает только безопасные поля: name/provider/model/status/lastTestAt/active. `apiKeyEncrypted` отсутствует в browse-view.

### 7.4. `AdminAiConfigurationEdit`

Edit-форма по общему Edit-контракту. Поле нового ключа — password input. При редактировании пустое значение означает «оставить существующий credential», новое значение — «заменить».

### 7.5. Пользовательские override

Пользователь видит только собственные подключения и разрешённые AI-функции. Для функции в `ADMIN_ONLY` элемент замещения отсутствует или read-only.

---

## 8. ProjectEdit и текущая реализация AI

### 8.1. Фактическое состояние GitHub master

В `master` `aee44f6cd726b3dbffe305a1414ac0375c93ab1e` контроллер `ProjectEdit` не содержит AI-сервисов и AI-вызовов. Текущий XML `project-edit.xml` содержит четыре вкладки: основные параметры, описание проекта, вакансии и шаблон письма. Поэтому незапушенную разработку AI нельзя считать частью подтверждённого состояния GitHub до появления отдельного commit/PR.

Также текущий `ProjectEdit` визуально ещё не приведён к обязательному общему Edit-контракту: форма остаётся TabSheet-компоновкой без постоянной левой `label-navigation`. Это отдельный UI-вопрос и не является основанием смешивать редизайн ProjectEdit с AI-архитектурой.

### 8.2. Оценка текущей незапушенной реализации

По текущему рабочему контексту в ProjectEdit реализуются/прорабатываются отдельные сервисы генерации описания проекта, vacancy-контента, вопросов для интервью, рекомендаций и выбора AI-провайдера; персональный `UserAiConfiguration` имеет приоритет, при его отсутствии предусмотрен административный fallback.

Эта реализация **частично соответствует** целевой архитектуре:

| Текущий подход | Оценка | Целевое решение |
|---|---|---|
| AI-действие запускается из ProjectEdit | соответствует | UI остаётся инициатором функции |
| отдельные domain AI-сервисы | допустимо | должны делегировать в единый `AiExecutionService` |
| `AIProvider`/provider service скрывает HTTP | соответствует | сохранить Strategy/registry |
| персональный API может иметь приоритет над системным | соответствует идее | приоритет должен задаваться **для конкретной AI-функции** |
| fallback на административный API | соответствует идее | должен управляться `executionPolicy`/`fallbackPolicy` |
| prompt-классы хранятся в Java | не соответствует | prompt должен управляться через `AiFunctionConfiguration`/админ-UI |
| ProjectEdit знает, какой provider использовать | не соответствует, если присутствует | экран передаёт только `functionCode` и контекст |
| один пользовательский provider заменяет AI глобально | не соответствует | нужен `UserAiFunctionOverride` |
| отдельный административный HTTP API внутри той же функции | требует обоснования | предпочтительно core credential + resolver; HTTP boundary только при реальной отдельной системе |

### 8.3. Рекомендуемые function codes для ProjectEdit

```text
PROJECT_DESCRIPTION_GENERATE
PROJECT_VACANCY_CONTEXT_GENERATE
PROJECT_INTERVIEW_QUESTIONS_GENERATE
PROJECT_RECOMMENDATIONS_GENERATE
```

Конкретные коды должны быть утверждены до миграции и затем использоваться как стабильный контракт.

### 8.4. Ограничение для скоринга кандидатов

AI в HRM HuntTech не должен ранжировать, оценивать пригодность или рекомендовать кандидатов для трудоустройства на основании возраста или других чувствительных/защищённых характеристик. Возрастной скоринг не включается в целевую архитектуру AI-функций.

Допустимый сценарий рекомендаций по проектам должен опираться на профессиональные сведения: навыки, опыт, роль, стек, проектный опыт и иные относящиеся к работе данные. Результат AI должен оставаться вспомогательным объяснимым предложением для пользователя, а не автоматическим решением о найме или отказе.

### 8.5. Вывод по ProjectEdit

**Архитектурное ядро идеи вписывается в концепцию, но текущая незапушенная реализация нуждается в адаптации до включения в master.**

Сохраняются:

- AI-кнопки/действия как инициаторы бизнес-функций;
- domain facade-сервисы;
- provider abstraction;
- идея personal-over-admin fallback.

Перерабатываются:

- хранение prompt в Java;
- выбор provider непосредственно бизнес-сервисом/экраном;
- глобальное пользовательское замещение;
- административные credentials;
- маршрутизация и fallback — через `AiConfigurationResolver`;
- возрастной скоринг — исключается.

---

## 9. Границы изменений для первого этапа

Первый этап AI Control Plane не должен менять другие бизнес-сущности и формы.

Разрешённая область:

- новые AI entity/enums;
- Liquibase для AI-таблиц;
- существующая `UserAiConfiguration` и её формы;
- `VacancyPromptTemplate` и её формы при отдельной миграционной необходимости;
- новые AI Browse/Edit-формы;
- core resolver/execution/secret services;
- AI provider layer;
- документация и тесты.

Без отдельного прямого разрешения не изменяются:

- `Project`/`ProjectEdit`;
- `OpenPosition`/`OpenPositionEdit`;
- `JobCandidate`/`JobCandidateEdit`;
- `CandidateCV`/`CandidateCVEdit`;
- иные бизнес-экраны.

Интеграция каждой бизнес-формы с function codes выполняется отдельным этапом после стабилизации AI Control Plane.

---

## 10. Миграционная стратегия

### Этап 1 — Data model

Создать `AiFunctionConfiguration`, `AdminAiConfiguration`, `UserAiFunctionOverride`, enums и документацию. Не подключать бизнес-экраны.

### Этап 2 — Secret layer

Ввести `AiSecretService`, защитить новые корпоративные ключи и подготовить безопасную миграцию существующих пользовательских credentials.

### Этап 3 — Admin UI

Создать Browse/Edit-формы AI-функций и корпоративных подключений с соблюдением UI-контрактов.

### Этап 4 — Resolver

Реализовать и протестировать `AiConfigurationResolver` для `ADMIN_ONLY`, `USER_OVERRIDE_ALLOWED`, `USER_REQUIRED` и fallback.

### Этап 5 — Execution API

Ввести единый `AiExecutionService`/`AiRequest`/`AiResponse`, адаптировать существующие текстовые провайдеры без изменения их vendor-specific HTTP деталей.

### Этап 6 — Legacy vacancy migration

Перевести `VacancyPromptTemplate`/`HrmAiService` на function-based execution отдельным PR.

### Этап 7 — Business screen integration

Подключать `ProjectEdit`, `OpenPositionEdit`, `CandidateCVEdit` и другие формы по одной функции/экрану, отдельными ветками и PR.

---

## 11. Тестовый контракт

### Entity / resolver

Обязательно проверить:

- уникальность `AiFunctionConfiguration.code`;
- уникальность `UserAiFunctionOverride(user, function)`;
- запрет override для `ADMIN_ONLY`;
- user override → user credential;
- отсутствие override → admin credential;
- disabled user credential → fallback согласно policy;
- disabled function → внешний API не вызывается;
- отсутствие admin credential → контролируемая ошибка;
- секрет не попадает в DTO, browse view и exception message.

### Core services

Каждый новый `*Service.java`/`*ServiceBean.java` получает тест в `modules/core/test/`.

### UI

Для новых Browse/Edit-экранов:

- `ScreenViewIntegrityTest`: ожидается `8/8 PASS` после включения экранов в соответствующий контракт проекта;
- Data View Integrity: каждый getter контроллера присутствует во view контейнера;
- contract test layout/actions/bindings;
- `buildScssThemes` при изменении SCSS;
- visual smoke light/dark;
- `clean assemble`;
- local deploy и `/hrm/` HTTP 200 выполняет Hermes по точному HEAD.

---

## 12. Нефункциональные требования

- AI-вызов не выполняется при открытии Edit/Browse-формы сам по себе;
- тяжёлый внешний вызов запускается только по явному действию пользователя или утверждённому background job;
- request/response logging не содержит API key и чувствительные payload без отдельной политики;
- timeout и ошибка провайдера преобразуются в контролируемый доменный результат;
- prompt имеет стабильный code/version для воспроизводимости;
- изменение admin configuration не требует изменения бизнес-форм;
- цена/лимит могут быть добавлены позднее без изменения function/credential relationship.

---

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создана целевая архитектура AI Control Plane: AI-функции, корпоративные и пользовательские credentials, per-function override, resolver/fallback, требования Browse/Edit и оценка интеграции ProjectEdit |
