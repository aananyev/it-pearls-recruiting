# Руководство внешнего разработчика по интеграции с HRM HuntTech API
## HuntTech External Integration Developer Guide (CUBA REST API v2)

> **Версия API**: CUBA REST API v2 (`/hrm/rest/v2`)  
> **Целевая аудитория**: Разработчики внешних систем (ATS, карьерных порталов, ботов, корпоративных интеграционных шин), создающие клиентское приложение для взаимодействия с HRM HuntTech.  
> **Дата актуализации**: 2026-09-23  
> **Версия платформы HRM HuntTech**: 0.717+  

---

## 1. Быстрый старт (Quickstart за 5 минут)

Для открытия вакансии и автоматического создания проекта в HRM HuntTech внешнему приложению достаточно выполнить два HTTP-запроса:
1. Получить OAuth2 Bearer-токен.
2. Отправить POST-запрос с текстом вакансии. Встроенный AI-конвейер HuntTech сам определит заказчика, контакты, проект, город, ставку, опыт работы, сформирует 4 стандарта артефактов, сделает перевод на английский язык и создаст каноническую вакансию.

### Шаг 1: Получение токена (cURL)
```bash
curl -s -X POST -u "hrm-rest:local-hrm-rest-secret-2026" \
  -d "grant_type=password&username=alan&password=Dodo-2012" \
  http://localhost:8080/hrm/rest/v2/oauth/token
```
**Ответ:**
```json
{
  "access_token": "qdptAHrN8ZCLOG7qCLciANgwrhg",
  "token_type": "bearer",
  "expires_in": 3599,
  "scope": "rest-api"
}
```

### Шаг 2: Отправка вакансии (cURL)
```bash
curl -s -X POST \
  -H "Authorization: Bearer qdptAHrN8ZCLOG7qCLciANgwrhg" \
  -H "Content-Type: application/json" \
  -d '{
    "request": {
      "vacancyName": "Архитектор Системный",
      "externalId": "13994",
      "comment": "Заказчик: SSP. Ответственный со стороны заказчика: @KareevaTatyana (Татьяна Кареева). Актирование 2 месяца.\n\nID 13994 Архитектор Системный\n\nОграничение по ставке T&M (без НДС)\nSenior/ - руб.\nКлючевые компетенции\nJava Liquibase CI/CD DWH Python Greenplum ETL Airflow Apache Spark\nФормат работы Удаленно\nПродолжительность проекта Больше года\nТребования: опыт от 3 лет в КХД..."
    }
  }' \
  http://localhost:8080/hrm/rest/v2/services/hunttech_ExternalIntegrationService/createProjectAndVacancy
```
**Ответ:**
```json
{
  "success": true,
  "projectId": "207af1f3-0ffa-7eaf-0f44-5c1426d8389a",
  "vacancyId": "0546a73b-aa72-adc2-436a-662b5329742b",
  "externalId": "13994",
  "status": "CREATED"
}
```

---

## 2. Архитектура и сетевые параметры

### 2.1 Контуры развертывания
| Контур | Базовый URL REST API | Примечание |
|---|---|---|
| **Локальная разработка** | `http://localhost:8080/hrm/rest/v2` | Для отладки и запуска в локальном окружении |
| **Тестовый / Стейджинг** | `http://192.168.1.135:8080/hrm/rest/v2` | Сервер тестирования интеграций |
| **Продакшен** | `https://hr.hunttech.ru/hrm/rest/v2` | Продуктивный контур (строго HTTPS) |

### 2.2 Аутентификация (OAuth 2.0)
API защищен по протоколу OAuth 2.0 (Resource Owner Password Credentials Grant):
- **Эндпоинт**: `POST /oauth/token`
- **Заголовок**: `Authorization: Basic <base64(client_id:client_secret)>`
- **Тело (form-urlencoded)**:
  - `grant_type=password`
  - `username=<логин пользователя>`
  - `password=<пароль>`
- **Клиентские учетные данные**:
  - `client_id`: `hrm-rest`
  - `client_secret`: согласовывается с администратором HuntTech (в dev: `local-hrm-rest-secret-2026`).
- **Срок действия токена**: 3600 секунд (1 час). Рекомендуется кэшировать токен в вашем приложении и обновлять при получении HTTP 401.

### 2.3 Таймауты HTTP-запросов (Важно!)
> [!IMPORTANT]
> Метод создания вакансии `createProjectAndVacancy` производит семантический AI-анализ, генерацию 4 документов (описание, чеклист, карта поиска, план интервью) и перевод на английский язык.
> Время выполнения вызова может составлять **от 20 до 75 секунд**.
> **Обязательно установите таймаут чтения (read timeout) вашего HTTP-клиента не менее 90–120 секунд**, иначе клиент оборвет соединение до завершения генерации.

---

## 3. Спецификация метода `createProjectAndVacancy`

Эндпоинт: `POST /services/hunttech_ExternalIntegrationService/createProjectAndVacancy`  
Заголовки:
- `Authorization: Bearer <access_token>`
- `Content-Type: application/json`

### 3.1 Полная модель входного DTO (`request`)

```json
{
  "request": {
    "externalId": "13994",
    "idempotencyKey": "9f7b6a4e-1234-5678-abcd-ef0123456789",
    "correlationId": "corr-req-001",
    "vacancyName": "ID 13994 Архитектор Системный",
    "projectName": "КХД Банка",
    "projectDescription": null,
    "companyName": "SSP",
    "companyId": null,
    "customerContact": "@KareevaTatyana",
    "actPeriod": "2 месяца",
    "shortDescription": null,
    "comment": "Полный текст вакансии...",
    "remoteWork": 1,
    "commandCandidate": 1,
    "workExperience": null,
    "cityName": null,
    "cityId": null,
    "outstaffingCost": null,
    "salaryMin": null,
    "salaryMax": null,
    "gradeId": null,
    "positionTypeId": null,
    "existingProjectId": null
  }
}
```

### 3.2 Описание полей запроса

| Поле | Тип | Обязательность | Поведение и бизнес-логика |
|---|---|:---:|---|
| `vacancyName` | `String` | **Да** | Входное наименование вакансии (до 250 символов). В системе итоговое наименование формируется по каноническому правилу кнопки «Генерировать». |
| `externalId` | `String` | Нет | Уникальный ID вакансии в вашей внешней системе (до 16 символов, например `13994`, `REQ-502`). Сохраняется в поле `vacansy_id`. |
| `comment` | `String` | **Рекомендуется** | **Полный исходный текст вакансии/заявки**. Без изменений сохраняется в `rawDescription` (Оригинал вакансии) и передается в AI. |
| `companyName` | `String` | Нет* | Название или код заказчика (например `SSP`, `Сбербанк`). Если не указано, автоматически извлекается AI из текста. |
| `companyId` | `UUID` | Нет | Прямой идентификатор заказчика из справочника `Company`. |
| `customerContact` | `String` | Нет* | Telegram (`@username`) или ФИО контактного лица заказчика. Если не указано, автоматически извлекается AI из текста. |
| `actPeriod` | `String` | Нет | Период актирования для договора/проекта (по умолчанию: `2 месяца`). |
| `projectName` | `String` | Нет | Название базового проекта. Если не передан, вычисляется AI из текста (например `КХД Банка`). |
| `existingProjectId` | `UUID` | Нет | Идентификатор уже существующего проекта в HuntTech. Если передан, новая вакансия привязывается к нему напрямую. |
| `remoteWork` | `Integer` | Нет | Формат работы: `1` — Удалённо, `2` — Офис, `3` — Гибрид. Если не передан, определяется из текста вакансии. |
| `cityName` | `String` | Нет | Город вакансии. **Для удаленной работы система автоматически выставляет «Регионы РФ (МСК +/- 2 часа)»**. |
| `cityId` | `UUID` | Нет | Прямой UUID города из справочника `City`. |
| `outstaffingCost` | `BigDecimal` | Нет | Почасовая ставка заказчика (T&M). Если не передана, извлекается из текста (например `3500 руб/час`). |
| `salaryMin` | `BigDecimal` | Нет | Нижняя граница оклада (руб/мес) для рекрутинга. При аутстаффинге рассчитывается по справочнику рейтов. |
| `salaryMax` | `BigDecimal` | Нет | Верхняя граница оклада (руб/мес) для рекрутинга. При аутстаффинге рассчитывается по справочнику рейтов. |
| `workExperience` | `Integer` | Нет | Опыт работы (в годах). Если не указан, извлекается из текста, либо по грейду (Junior=2, Middle=3, Senior/Lead/Architect=5). |
| `idempotencyKey` | `String` | **Рекомендуется** | Уникальный ключ идемпотентности запроса (UUID). Предотвращает случайное создание дублей при сетевых сбоях. |
| `correlationId` | `String` | Нет | Сквозной идентификатор для логов и мониторинга (передается во все логи HuntTech). |

*\* Внимание: Если `companyName`/`companyId` или `customerContact` не переданы явно и не смогли быть распознаны AI из текста, запрос вернет ошибку `CUSTOMER_NOT_FOUND` или `CUSTOMER_CONTACT_NOT_FOUND`.*

---

## 4. Встроенная бизнес-логика (Что HuntTech делает автоматически)

Вам не нужно реализовывать сложный парсинг на стороне вашего приложения. Сервис HuntTech выполняет следующие шаги:

1. **Определение формы сотрудничества (Аутстаффинг vs Рекрутинг)**:
   - Если указана ставка в час (`руб/час`, `T&M`, `рейт`, `ограничение по ставке`) или форма не указана -> выставляется **Аутстаффинг** (`registrationForWork = 0`).
   - Если указан четкий ежемесячный оклад (`руб/мес`, `в месяц`, `оклад`, `gross`, `net`) без почасовой ставки -> выставляется **Рекрутинг** (`registrationForWork = 1`).
2. **Расчет зарплатного предложения по справочнику «Рейты по аутстафу»**:
   - При наличии ставки заказчика (например `3500 руб/час`) система ищет в таблице `hunttech_OutstaffingRates` строку с `rate <= customerRate ORDER BY rate DESC LIMIT 1`. Из найденной строки заполняются `salaryMin` (min оклад), `salaryMax` (max оклад), `salaryIE` (для ИП/самозанятых).
   - Если ставка заказчика не указана (например `Senior/ - руб.`): выставляется флаг `salaryCandidateRequest = true` (**«Ориентируемся на запрос кандидата»**), а числовые поля зарплаты остаются пустыми.
3. **Автоподбор города для удаленки**:
   - Если вакансия удаленная (`remoteWork = 1` или в тексте есть слова «Удаленно», «Remote»), в поле город автоматически привязывается канонический город: **«Регионы РФ (МСК +/- 2 часа)»**.
4. **Опыт работы**:
   - Если в тексте есть требование (например «опыт от 3-х лет») -> ставится `3`.
   - Если точное число не указано, вычисляется по квалификации:
     - `Junior` -> 2 года
     - `Middle` / `Regular` -> 3 года
     - `Senior` / `Lead` / `Architect` -> от 5 лет.
5. **Дата закрытия вакансии**:
   - Если в тексте есть дедлайн (например: «резюме принимаются до 25.10.2026» или «прием до 15 ноября»), дата автоматически парсится и записывается в поле `closingDate`.
6. **Автор записи**:
   - В поле «Ответственный» (`owner_id`) всегда назначается системный пользователь **`hunttech`**, создатель — `hunttech`.
7. **Генерация AI-артефактов и английского описания**:
   - Формируется 4 документа в чистом HTML:
     - Описание вакансии (`comment_`);
     - Чеклист отбора кандидатов (`interviewChecklist`);
     - Карта поиска (`searchMap`);
     - План собеседования (`interviewPlan`).
   - Формируется перевод описания на английский язык (`commentEn`) в формате валидного HTML.
8. **Каноническое название вакансии (`vacansyName`)**:
   - Автоматически рассчитывается по правилу формы OpenPosition:
     `[Grade] [PositionRu] / [PositionEn] ([ProjectName], [CityName])`
     *Пример: `Senior Системный архитектор / System Architect (SSP "КХД Банка. Проект Татьяны Кареевой" /Штат HuntTech ТК/ГПХ или ИП. Актирование 2 месяца/, Регионы РФ (МСК +/- 2 часа))`*

---

## 5. Обработка ошибок (Error Handling)

При возникновении ошибок валидации или отсутствия зависимых сущностей сервис возвращает HTTP 200 со структурой `ProjectVacancyResponseDto`, где `success = false`:

```json
{
  "success": false,
  "projectId": null,
  "vacancyId": null,
  "externalId": "13994",
  "status": null,
  "correlationId": "corr-req-001",
  "message": "Не удалось найти контактное лицо со стороны заказчика в справочнике 'Люди'. Укажите ФИО или Telegram ответственного заказчика в описании вакансии или поле customerContact.",
  "errorDetails": {
    "success": false,
    "errorCode": "CUSTOMER_CONTACT_NOT_FOUND",
    "message": "Не удалось найти контактное лицо со стороны заказчика в справочнике 'Люди'. Укажите ФИО или Telegram ответственного заказчика в описании вакансии или поле customerContact.",
    "correlationId": "corr-req-001",
    "timestamp": "2026-09-23 16:11:08.416",
    "details": []
  }
}
```

### Коды ошибок (`errorCode`)

| Код ошибки | Причина | Что сделать во внешнем приложении |
|---|---|---|
| `VALIDATION_ERROR` | Не передано `vacancyName` или невалидный формат UUID | Проверьте наличие поля `vacancyName` и корректность UUID |
| `CUSTOMER_NOT_FOUND` | Не удалось определить компанию-заказчика | Передайте явное название компании в `companyName` (например `"SSP"`) или добавьте в текст: `"Заказчик: SSP"` |
| `CUSTOMER_CONTACT_NOT_FOUND` | Не найдено контактное лицо клиента в HuntTech | Укажите Telegram или ФИО контакта в `customerContact` (например `"@KareevaTatyana"`) или в тексте |
| `NOT_FOUND` | Переданный `existingProjectId` не найден | Проверьте корректность UUID существующего проекта |
| `SYSTEM_ERROR` | Ошибка базы данных или сбой транзакции | Повторите запрос через 5 секунд с тем же `idempotencyKey` |

---

## 6. Примеры реализации на популярных языках

### 6.1 Python 3 (`requests`)

```python
import uuid
import requests

HRM_BASE_URL = "http://localhost:8080/hrm/rest/v2"
CLIENT_ID = "hrm-rest"
CLIENT_SECRET = "local-hrm-rest-secret-2026"
USERNAME = "alan"
PASSWORD = "Dodo-2012"

class HuntTechClient:
    def __init__(self, base_url=HRM_BASE_URL):
        self.base_url = base_url
        self.token = None

    def authenticate(self):
        url = f"{self.base_url}/oauth/token"
        response = requests.post(
            url,
            auth=(CLIENT_ID, CLIENT_SECRET),
            data={
                "grant_type": "password",
                "username": USERNAME,
                "password": PASSWORD
            },
            timeout=15
        )
        response.raise_for_status()
        data = response.json()
        self.token = data["access_token"]
        return self.token

    def create_vacancy(self, external_id: str, vacancy_text: str, vacancy_name: str = None):
        if not self.token:
            self.authenticate()

        url = f"{self.base_url}/services/hunttech_ExternalIntegrationService/createProjectAndVacancy"
        headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }

        payload = {
            "request": {
                "externalId": external_id,
                "idempotencyKey": str(uuid.uuid4()),
                "vacancyName": vacancy_name or f"Вакансия {external_id}",
                "comment": vacancy_text,
                "remoteWork": 1
            }
        }

        # Таймаут 120 секунд для ожидания завершения AI-конвейера
        response = requests.post(url, headers=headers, json=payload, timeout=120)
        
        if response.status_code == 401:
            # Токен истек - обновляем и повторяем запрос 1 раз
            self.authenticate()
            headers["Authorization"] = f"Bearer {self.token}"
            response = requests.post(url, headers=headers, json=payload, timeout=120)

        response.raise_for_status()
        result = response.json()

        if not result.get("success"):
            error_code = result.get("errorDetails", {}).get("errorCode")
            error_msg = result.get("message")
            raise RuntimeError(f"HuntTech API Error [{error_code}]: {error_msg}")

        return result

# Пример вызова:
if __name__ == "__main__":
    client = HuntTechClient()
    text = (
        "Заказчик: SSP. Ответственный: @KareevaTatyana (Татьяна Кареева). Актирование 2 месяца.\n"
        "ID 13994 Архитектор Системный\n"
        "Ограничение по ставке T&M: Senior / - руб.\n"
        "Формат работы: Удаленно.\n"
        "Опыт в КХД от 3 лет..."
    )
    res = client.create_vacancy("13994", text, "Архитектор Системный")
    print(f"Вакансия успешно создана! ID: {res['vacancyId']}, Project ID: {res['projectId']}")
```

---

### 6.2 Node.js / TypeScript (`axios` / `fetch`)

```typescript
import axios, { AxiosInstance } from 'axios';
import { v4 as uuidv4 } from 'uuid';

interface CreateVacancyRequest {
  externalId?: string;
  idempotencyKey?: string;
  vacancyName: string;
  comment?: string;
  companyName?: string;
  customerContact?: string;
  remoteWork?: number;
  outstaffingCost?: number;
}

interface CreateVacancyResponse {
  success: boolean;
  projectId?: string;
  vacancyId?: string;
  externalId?: string;
  status?: string;
  message?: string;
  errorDetails?: {
    errorCode: string;
    message: string;
  };
}

export class HuntTechApiClient {
  private token: string | null = null;
  private readonly baseUrl: string;

  constructor(baseUrl = 'http://localhost:8080/hrm/rest/v2') {
    this.baseUrl = baseUrl;
  }

  async authenticate(): Promise<string> {
    const authHeader = Buffer.from('hrm-rest:local-hrm-rest-secret-2026').toString('base64');
    const params = new URLSearchParams({
      grant_type: 'password',
      username: 'alan',
      password: 'Dodo-2012',
    });

    const res = await axios.post(`${this.baseUrl}/oauth/token`, params.toString(), {
      headers: {
        Authorization: `Basic ${authHeader}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      timeout: 10000,
    });

    this.token = res.data.access_token;
    return this.token!;
  }

  async createVacancy(req: CreateVacancyRequest): Promise<CreateVacancyResponse> {
    if (!this.token) {
      await this.authenticate();
    }

    const payload = {
      request: {
        idempotencyKey: req.idempotencyKey || uuidv4(),
        ...req,
      },
    };

    const res = await axios.post<CreateVacancyResponse>(
      `${this.baseUrl}/services/hunttech_ExternalIntegrationService/createProjectAndVacancy`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${this.token}`,
          'Content-Type': 'application/json',
        },
        timeout: 120000, // 120 сек таймаут
      }
    );

    if (!res.data.success) {
      throw new Error(`[${res.data.errorDetails?.errorCode}] ${res.data.message}`);
    }

    return res.data;
  }
}
```

---

### 6.3 Java (Spring `WebClient`)

```java
package com.example.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class HuntTechIntegrationService {

    private final WebClient webClient;
    private String cachedToken;

    public HuntTechIntegrationService(String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public synchronized String obtainToken() {
        Map<?, ?> response = webClient.post()
                .uri("/oauth/token")
                .headers(headers -> headers.setBasicAuth("hrm-rest", "local-hrm-rest-secret-2026"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("username", "alan")
                        .with("password", "Dodo-2012"))
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(15));

        this.cachedToken = (String) response.get("access_token");
        return this.cachedToken;
    }

    public Map<?, ?> createVacancy(String externalId, String vacancyName, String vacancyText) {
        if (cachedToken == null) {
            obtainToken();
        }

        Map<String, Object> requestBody = Map.of(
                "request", Map.of(
                        "externalId", externalId,
                        "idempotencyKey", UUID.randomUUID().toString(),
                        "vacancyName", vacancyName,
                        "comment", vacancyText,
                        "remoteWork", 1
                )
        );

        return webClient.post()
                .uri("/services/hunttech_ExternalIntegrationService/createProjectAndVacancy")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cachedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(120))
                .block();
    }
}
```

---

## 7. Чеклист готовности к интеграции (Go-Live Checklist)

Перед включением отправки в продуктивный контур проверьте следующие пункты:
- [ ] **HTTP Таймаут**: таймаут вашего HTTP-клиента выставлен не менее чем на **90 секунд** (рекомендовано 120с).
- [ ] **Idempotency-Key**: при каждом запросе генерируется новый `UUID` (или детерминированный ключ вашей заявки) для предотвращения случайного дублирования вакансий при повторных сетевых вызовах.
- [ ] **Авторизация**: реализовано сохранение `access_token` и его повторный запрос при получении ошибки `401 Unauthorized`.
- [ ] **Заказчик и контакт**: в тексте заявки присутствуют имя заказчика (например `Заказчик: SSP`) и Telegram ответственного (например `@username`), либо они передаются явно в полях `companyName` и `customerContact`.
- [ ] **Текст вакансии**: полный оригинальный текст заявки передается в поле `comment`.
- [ ] **Обработка ошибок**: в вашем коде обрабатываются бизнес-ошибки `CUSTOMER_NOT_FOUND` и `CUSTOMER_CONTACT_NOT_FOUND`.
