# External Integration API Foundation — Контракт и руководство по интеграции

> Документация подпроекта BL-2026-036 (BL-2026-025..BL-2026-030).  
> Контур: CUBA REST API v2 (`/hrm/rest/v2`).  
> Дата актуализации: 2026-09-23.

---

## 1. Архитектурный обзор

External Integration API предоставляет безопасный программный интерфейс для взаимодействия внешних информационных систем с HRM HuntTech.

### Ключевые принципы безопасности:
- **Транспорт**: протокол HTTPS, базовый путь `/hrm/rest/v2`.
- **Аутентификация**: OAuth2 Password Grant с выдачей Bearer-токена (RFC 6749).
- **Ролевая модель**: выделенная роль `REST Внешняя интеграция` (security scope `GENERIC_UI`, specific permission `cuba.restApi.enabled`, service permissions).
- **Защита данных**: прямой generic CRUD над сущностями закрыт; все операции выполняются через методы middleware-сервисов с валидацией, дедупликацией и аудитом.
- **Идемпотентность и трассировка**: поддержка `Idempotency-Key` и `X-Correlation-Id` (или параметра `correlationId` в теле DTO).

---

## 2. Аутентификация (OAuth2)

### 2.1 Получение access token

```http
POST /hrm/rest/v2/oauth/token
Authorization: Basic <base64(client_id:client_secret)>
Content-Type: application/x-www-form-urlencoded

grant_type=password&username=<integration_user>&password=<integration_password>
```

#### Параметры окружения:
- `client_id`: `hrm-rest`
- `client_secret`: задаётся в `${app.home}/local.app.properties` (для локального dev: `local-hrm-rest-secret-2026`)
- `username`: технический пользователь интеграции (например, `ext-integration-client`)

#### Пример ответа:
```json
{
  "access_token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "token_type": "bearer",
  "refresh_token": "f0e1d2c3-b4a5-6789-0123-456789abcdef",
  "expires_in": 3600,
  "scope": "rest-api"
}
```

---

## 3. Read-Only API справочников (BL-2026-029)

Все вызовы сервиса выполняются методом `POST` на URL сервиса CUBA REST:
`POST /hrm/rest/v2/services/hunttech_ExternalReferenceDataService/{methodName}`  
Заголовок: `Authorization: Bearer <access_token>`  
Content-Type: `application/json`

### 3.1 Методы сервиса

| Метод | Описание | Сущность |
|---|---|---|
| `getCities` | Справочник городов (с кодами ФИАС) | `City` |
| `getPositions` | Справочник должностей и специализаций | `Position` |
| `getGrades` | Справочник квалификационных грейдов | `Grade` |
| `getInteractionTypes` | Справочник типов взаимодействий | `Iteraction` |
| `getSkills` | Дерево навыков и технологий | `SkillTree` |
| `getCountries` | Справочник стран мира (Alpha3 / 2-буквенный код) | `Country` |

### 3.2 Формат запроса фильтрации (`ReferenceFilterDto`)

```json
{
  "filter": {
    "search": "Java",
    "limit": 50,
    "offset": 0,
    "parentId": null,
    "correlationId": "req-12345-abc"
  }
}
```

### 3.3 Формат успешного ответа (`ReferenceListResponseDto`)

```json
{
  "success": true,
  "totalCount": 1,
  "correlationId": "req-12345-abc",
  "items": [
    {
      "id": "e3b0c442-98fc-1c14-9afbf4c8996fb924",
      "code": "JAVA_DEV",
      "name": "Разработчик Java",
      "parentId": null,
      "active": true,
      "description": "Java Developer"
    }
  ]
}
```

### 3.4 Формат ответа при ошибке (`ApiErrorResponseDto`)

```json
{
  "success": false,
  "errorMessage": "Некорректный параметр limit: значение не может быть отрицательным",
  "correlationId": "req-12345-abc",
  "totalCount": 0,
  "items": [],
  "errorDetails": {
    "success": false,
    "errorCode": "VALIDATION_ERROR",
    "message": "Некорректный параметр limit: значение не может быть отрицательным",
    "correlationId": "req-12345-abc",
    "timestamp": "2026-09-23T10:00:00.000Z",
    "details": []
  }
}
```

---

## 4. Predefined REST Queries (Альтернативный быстрый доступ)

Для обратной совместимости и быстрого кэширования доступны стандартные именованные JPQL-запросы:
- `POST /hrm/rest/v2/queries/hunttech_City/cityAll`
- `POST /hrm/rest/v2/queries/hunttech_Position/positionAll`
- `POST /hrm/rest/v2/queries/hunttech_Grade/gradeAll`
- `POST /hrm/rest/v2/queries/hunttech_Country/countryAll`
- `POST /hrm/rest/v2/queries/hunttech_SkillTree/skillAll`
- `POST /hrm/rest/v2/queries/hunttech_Iteraction/iteractionTypeAll`

---

## 5. API создания компаний (BL-2026-027)

Метод создания или дедупликации организации в справочнике `Company`.  
URL: `POST /hrm/rest/v2/services/hunttech_ExternalIntegrationService/createCompany`  
Заголовок: `Authorization: Bearer <access_token>`  
Content-Type: `application/json`

### 5.1 Формат запроса (`CompanyCreateRequestDto`)

```json
{
  "request": {
    "externalId": "partner-comp-1001",
    "idempotencyKey": "idem-key-99128",
    "correlationId": "corr-comp-99128",
    "companyName": "ООО Инновационные Системы",
    "companyShortName": "Инновационные Системы",
    "legalEntityName": "Общество с ограниченной ответственностью 'Инновационные Системы'",
    "inn": "7701987654",
    "kpp": "770101001",
    "ogrn": "1027700132195",
    "addressOfCompany": "г. Москва, ул. Ленина, д. 10",
    "cityId": "0c5b2444-70a0-4932-980c-b4dc0d3f02b5",
    "countryId": null,
    "regionId": null,
    "website": "https://example.com"
  }
}
```

### 5.2 Формат ответа (`CompanyResponseDto`)

#### При успешном создании новой компании (`status: "CREATED"`):
```json
{
  "success": true,
  "companyId": "3f2c5891-9a7c-48be-9fae-d4c391748201",
  "externalId": "partner-comp-1001",
  "companyName": "ООО Инновационные Системы",
  "status": "CREATED",
  "correlationId": "corr-comp-99128"
}
```

#### При обнаружении существующей компании-дубликата (`status: "EXISTING_FOUND"`):
```json
{
  "success": true,
  "companyId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
  "externalId": "partner-comp-1001",
  "companyName": "ООО Инновационные Системы",
  "status": "EXISTING_FOUND",
  "correlationId": "corr-comp-99128"
}
```
Дедупликация выполняется по совпадению ИНН (при наличии) либо по нечувствительному к регистру наименованию компании, исключая появление дубликатов в справочнике.

---

## 6. API создания проекта и вакансии с AI-генерацией артефактов (BL-2026-025 / BL-2026-036)

Транзакционный метод создания проекта и связанной вакансии (или добавления вакансии в существующий проект) с автоматической интеллектуальной упаковкой:
- **Оригинал вакансии**: входящий текст из поля `comment` (или `shortDescription`) сохраняется в неизменном виде в атрибуте сущности **«Оригинал вакансии»** (`OpenPosition.rawDescription`).
- **Внутренняя AI-генерация 4 артефактов стандарта**: на основе оригинала вакансии нейросеть автоматически формирует и записывает:
  1. **«Описание вакансии»** (`STANDARDIZE_VACANCY`) ➔ поле `OpenPosition.comment` (`COMMENT_`);
  2. **«Чеклист»** (`VACANCY_CHECKLIST`) ➔ поля `OpenPosition.interviewChecklist` и `exercise`;
  3. **«Карта поиска»** (`VACANCY_SEARCH_MAP`) ➔ поля `OpenPosition.searchMap` и `memoForInterview`;
  4. **«План собеседования»** (`VACANCY_INTERVIEW_PLAN`) ➔ поля `OpenPosition.interviewPlan` и `templateLetter`.
- **Подробное руководство**: см. специализированный документ [VACANCY_API_AI_INGEST_INTEGRATION.md](../integrations/VACANCY_API_AI_INGEST_INTEGRATION.md).

URL: `POST /hrm/rest/v2/services/hunttech_ExternalIntegrationService/createProjectAndVacancy`  
Заголовок: `Authorization: Bearer <access_token>`  
Content-Type: `application/json`

### 6.1 Формат запроса (`ProjectVacancyCreateRequestDto`)

```json
{
  "request": {
    "externalId": "partner-vac-501",
    "idempotencyKey": "idem-vac-501",
    "correlationId": "corr-vac-501",
    "vacancyName": "Senior Java Developer / Kotlin",
    "projectName": "Проект Финтех 2.0",
    "projectDescription": "Разработка высоконагруженной платежной платформы",
    "companyName": "SSP",
    "companyId": null,
    "customerContact": "@KareevaTatyana",
    "actPeriod": "2 месяца",
    "shortDescription": "Разработка микросервисов ядра биллинга",
    "comment": "Заказчик: SSP. Ответственный: @KareevaTatyana. Ограничение по ставке T&M: 3500 руб/час. Требуется опыт работы от 5 лет. Формат работы: Удаленно.",
    "remoteWork": 1,
    "commandCandidate": 1,
    "workExperience": null,
    "cityName": "Регионы РФ (МСК +/- 2 часа)",
    "cityId": null,
    "outstaffingCost": 3500.00,
    "salaryMin": null,
    "salaryMax": null,
    "gradeId": null,
    "positionTypeId": null
  }
}
```

Если необходимо привязать вакансию к уже существующему проекту, вместо `projectName` передается `existingProjectId`:
```json
{
  "request": {
    "existingProjectId": "4a28f731-9a7c-48be-9fae-d4c391748201",
    "vacancyName": "QA Automation Lead",
    "comment": "Требуется опыт тестирования от 3 лет. Формат: Удаленно."
  }
}
```

### 6.2 Формат ответа (`ProjectVacancyResponseDto`)

Успешное создание:
```json
{
  "success": true,
  "projectId": "4a28f731-9a7c-48be-9fae-d4c391748201",
  "vacancyId": "b182c943-3fae-4d81-9b12-9812470123ef",
  "externalId": "partner-vac-501",
  "status": "CREATED",
  "correlationId": "corr-vac-501",
  "message": null,
  "errorDetails": null
}
```

Ошибка валидации / бизнес-правил:
```json
{
  "success": false,
  "projectId": null,
  "vacancyId": null,
  "externalId": null,
  "status": null,
  "correlationId": "corr-vac-501",
  "message": "Не удалось определить компанию заказчика (клиента) из описания вакансии или переданных параметров. Заполните поле companyName/companyId или укажите заказчика в тексте вакансии.",
  "errorDetails": {
    "success": false,
    "errorCode": "CUSTOMER_NOT_FOUND",
    "message": "Не удалось определить компанию заказчика (клиента) из описания вакансии или переданных параметров. Заполните поле companyName/companyId или укажите заказчика в тексте вакансии.",
    "correlationId": "corr-vac-501",
    "timestamp": "2026-09-23 16:11:08.416",
    "details": []
  }
}
```

### 6.3 Бизнес-правила заполнения полей вакансии
- **Город вакансии**: для удаленной работы (по тексту или `remoteWork = 1`) автоматически выставляется город **«Регионы РФ (МСК +/- 2 часа)»** (`hunttech_City`).
- **Оформление (`registrationForWork`)**: по умолчанию устанавливается **Аутстаффинг** (`0`). Если в тексте указан четкий ежемесячный оклад (руб/мес, в месяц, net/gross) без почасовой ставки, устанавливается **Рекрутинг** (`1`).
- **Ставка и расчет зарплатного предложения**:
  - При аутстаффинге ставка заказчика пишется в `outstaffingCost`.
  - По справочнику «Рейты по аутстафу» (`hunttech_OutstaffingRates`) подбирается ставка по правилу: точное совпадение либо ближайшее значение в меньшую сторону (`rate <= :customerRate ORDER BY rate DESC LIMIT 1`). Из найденной строки заполняются `salaryMin`, `salaryMax`, `salaryIE`, `salaryComment`.
  - Если ставку заказчика определить не удалось: выставляется флаг `salaryCandidateRequest = true` («Ориентируемся на запрос кандидата»), а числовые поля зарплаты остаются пустыми.
- **Автор записи**: в качестве ответственного (`owner_id`) всегда назначается системный пользователь **`hunttech`** (`ExtUser`), `createdBy = "hunttech"`.
- **Дата закрытия (`closingDate`)**: автоматически извлекается из фраз «резюме принимаются до [дата]», «прием резюме до...».
- **Общий опыт работы (`workExperience`)**: вычисляется из текста (например «опыт от 3 лет»), а при отсутствии — по грейду: Junior = 2 года, Middle/Regular = 3 года, Senior/Lead/Architect = от 5 лет.
- **Английское описание (`commentEn`)**: AI выполняет перевод стандартизированного описания на английский язык с сохранением структуры HTML (`<h3>`, `<p>`, `<ul>`, `<li>`).
- **Каноническое название вакансии (`vacansyName`)**: формируется вызовом алгоритма кнопки «Генерировать» формы `OpenPositionEdit`:
  `[Grade] [PositionRu] / [PositionEn] ([ProjectName], [CityName])`.
- **Транзакционность и идемпотентность**: проект и вакансия коммитятся в одной транзакции; повторные запросы с тем же `idempotencyKey` возвращают закэшированный результат без дублирования.

---

## 7. API создания резюме кандидата CandidateCV (BL-2026-028)

Метод создания отдельного резюме кандидата с привязкой к существующему `JobCandidate`.  
URL: `POST /hrm/rest/v2/services/hunttech_ExternalIntegrationService/createCandidateCV`  
Заголовок: `Authorization: Bearer <access_token>`  
Content-Type: `application/json`

### 7.1 Формат запроса (`CandidateCvCreateRequestDto`)
```json
{
  "request": {
    "candidateId": "91a82f14-3c81-4b12-9214-9812470123ef",
    "vacancyId": "b182c943-3fae-4d81-9b12-9812470123ef",
    "positionId": "71234567-89ab-cdef-0123-456789abcdef",
    "textCv": "Опыт работы: 5 лет Java/Kotlin, Spring Boot, микросервисы",
    "resumeUrl": "https://hh.ru/resume/abcdef123456",
    "coverLetter": "Добрый день! Рассматриваю предложения по разработке бэкенда.",
    "externalId": "ext-cv-101",
    "idempotencyKey": "idem-cv-101",
    "correlationId": "corr-cv-101"
  }
}
```

### 7.2 Формат ответа (`CandidateCvResponseDto`)
```json
{
  "success": true,
  "candidateCvId": "c8124891-4fae-4d81-9b12-1247890123aa",
  "candidateId": "91a82f14-3c81-4b12-9214-9812470123ef",
  "externalId": "ext-cv-101",
  "status": "CREATED",
  "correlationId": "corr-cv-101"
}
```

---

## 8. API создания взаимодействия IteractionList (BL-2026-028)

Метод фиксации события взаимодействия рекрутера с кандидатом по вакансии.  
URL: `POST /hrm/rest/v2/services/hunttech_ExternalIntegrationService/createInteraction`  
Заголовок: `Authorization: Bearer <access_token>`  
Content-Type: `application/json`

### 8.1 Формат запроса (`InteractionCreateRequestDto`)
```json
{
  "request": {
    "candidateId": "91a82f14-3c81-4b12-9214-9812470123ef",
    "vacancyId": "b182c943-3fae-4d81-9b12-9812470123ef",
    "interactionTypeId": "31234567-89ab-cdef-0123-456789abcdef",
    "comment": "Проведено техническое интервью. Кандидат ответил на все вопросы по архитектуре.",
    "communicationMethod": "Telegram",
    "rating": 5,
    "externalId": "ext-act-201",
    "idempotencyKey": "idem-act-201",
    "correlationId": "corr-act-201"
  }
}
```

### 8.2 Формат ответа (`InteractionResponseDto`)
```json
{
  "success": true,
  "interactionId": "e9124891-4fae-4d81-9b12-3347890123cc",
  "candidateId": "91a82f14-3c81-4b12-9214-9812470123ef",
  "vacancyId": "b182c943-3fae-4d81-9b12-9812470123ef",
  "numberInteraction": 42,
  "externalId": "ext-act-201",
  "status": "CREATED",
  "correlationId": "corr-act-201"
}
```

---

## 9. Комплексный API создания кандидата, резюме и взаимодействия (BL-2026-026)

Атомарный транзакционный метод создания новой анкеты кандидата с резюме и первым взаимодействием по вакансии, либо обогащения существующей карточки кандидата при обнаружении дубля.  
URL: `POST /hrm/rest/v2/services/hunttech_ExternalIntegrationService/createCandidateWithDetails`  
Заголовок: `Authorization: Bearer <access_token>`  
Content-Type: `application/json`

### 9.1 Формат запроса (`CandidateCompositeCreateRequestDto`)
```json
{
  "request": {
    "firstName": "Алексей",
    "secondName": "Смирнов",
    "middleName": "Петрович",
    "phone": "+7 (999) 111-22-33",
    "mobilePhone": "+7 (999) 111-22-33",
    "email": "smirnov@example.com",
    "telegramName": "@smirnov_dev",
    "cityId": "0c5b2444-70a0-4932-980c-b4dc0d3f02b5",
    "positionId": "71234567-89ab-cdef-0123-456789abcdef",
    "companyId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
    "cvText": "Senior Fullstack Developer. React, TypeScript, Java, Spring.",
    "cvUrl": "https://hh.ru/resume/998877",
    "coverLetter": "Готов рассмотреть удаленную работу.",
    "vacancyId": "b182c943-3fae-4d81-9b12-9812470123ef",
    "interactionComment": "Кандидат откликнулся на позицию через внешний портал",
    "communicationMethod": "Портал",
    "rating": 4,
    "externalId": "partner-applicant-881",
    "idempotencyKey": "idem-applicant-881",
    "correlationId": "corr-applicant-881"
  }
}
```

### 9.2 Формат ответа (`CandidateCompositeResponseDto`)
```json
{
  "success": true,
  "candidateId": "91a82f14-3c81-4b12-9214-9812470123ef",
  "candidateCvId": "c8124891-4fae-4d81-9b12-1247890123aa",
  "interactionId": "e9124891-4fae-4d81-9b12-3347890123cc",
  "externalId": "partner-applicant-881",
  "status": "CREATED",
  "correlationId": "corr-applicant-881"
}
```

Если кандидат уже найден в базе по номеру телефона, email или ФИО:
```json
{
  "success": true,
  "candidateId": "91a82f14-3c81-4b12-9214-9812470123ef",
  "candidateCvId": "c8124891-4fae-4d81-9b12-1247890123aa",
  "interactionId": "e9124891-4fae-4d81-9b12-3347890123cc",
  "externalId": "partner-applicant-881",
  "status": "EXISTING_FOUND",
  "correlationId": "corr-applicant-881"
}
```
При дедупликации дубликат карточки кандидата не создается, а новое резюме и взаимодействие по вакансии корректно прикрепляются к существующему кандидату.



