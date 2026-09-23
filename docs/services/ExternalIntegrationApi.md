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

