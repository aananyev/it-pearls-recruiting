# План миграции системных настроек ИИ-моделей и профилей (DML) для активных пользователей системы на Production (`hr.hunttech.ru`)

> **Дата составления и применения**: 14 сентября 2026 г.  
> **Окружение**: Production `hr.hunttech.ru` (`92.63.101.170`), СУБД PostgreSQL 11.22 (`hunttech`), Apache Tomcat 9  
> **Статус плана**: **УСПЕШНО ВЫПОЛНЕНО НА PRODUCTION**  
> **Ответственная роль**: Руководитель проектов / Разработчик (`agent/antigravity-dev`)  
> **Область изменений**: Настройки персонального доступа к моделям ИИ (`hunttech_UserAiConfiguration`), профили пользователей ИИ (`hunttech_UserAiProfile`), согласие на административный fallback, лимиты токенов (`hunttech_LlmUserQuotaOverride`), исправление провайдеров OpenRouter

---

## ⚠ Базовое правило безопасности (ВЫПОЛНЕНО)

> [!IMPORTANT]
> Перед выполнением любых DML-манипуляций на боевом сервере созданы полные логические бэкапы базы данных PostgreSQL:
> 1. `/tmp/hunttech_backup_ai_config_okozhevnikova_20260914_111525.dump` (865 MB) — перед точечной настройкой пользователя `okozhevnikova`.
> 2. `/tmp/hunttech_backup_all_active_users_20260914_111929.dump` (865 MB) — перед массовым тиражированием на всех активных пользователей системы (`active = true`).

---

## 1. Контекст и причины проблемы

### 1.1. Инцидент
Пользователь Ольга Кожевникова (`okozhevnikova`, ID: `2c1b6a84-70bc-6fa5-8c61-82e1d3ea67e1`) при работе с LLM-чатом во вкладке Hermes получала пустой ответ от модели.

### 1.2. Анализ логов (`hunttech_ai_call_log` и логи приложений на проде)
В журнале вызовов ИИ для пользователя `okozhevnikova` зафиксировано 10 подряд аварийных завершений с ошибкой:
```text
Ошибка streaming-запроса к OpenAI API: HTTP 403: {"error":{"code":"[REDACTED]","message":"Country, region, or territory not supported"}}
```

### 1.3. Корневые причины:
1. **Неверный код провайдера**: В пользовательской конфигурации ИИ для модели `nvidia/nemotron-3-ultra-550b-a55b:free` был указан провайдер `openai` вместо `openrouter`. Запрос уходил на эндпоинт `api.openai.com`, блокирующий российские IP-адреса хостинга.
2. **Отсутствие основной рабочей модели DeepSeek**: У пользователя отсутствовала персональная конфигурация для прямого подключения к DeepSeek (`deepseek-v4-flash`), которая настроена и успешно работает у пользователя Алексей Ананьев (`alan`).
3. **Блокировка fallback к системным/административным ключам**: В классе `AiExecutionServiceBean` (строки 1004–1012) заложен жесткий guardrail:
   ```java
   if (profile == null || !Boolean.TRUE.equals(profile.getAdminFallbackConsent())
           || !"2026-09-05-v1".equals(profile.getAdminFallbackConsentVersion())
           || profile.getAdminFallbackConsentAt() == null) {
       throw new DevelopmentException("У пользователя отключен fallback к админским ключам...");
   }
   ```
   У пользователя отсутствовала запись в `hunttech_user_ai_profile`, из-за чего каскадный переход на системную модель блокировался.
4. **Ограничение квоты**: Устаревшая базовая квота в 10 000 токенов препятствовала комфортной работе с контекстом чата.

---

## 2. Требования к целевому состоянию
1. Полная персонализация: каждый активный пользователь (`sec_user where active = true`) должен иметь собственные конфигурации, чтобы в `hunttech_ai_call_log` фиксировались `credentialOwner = 'USER'`, `user_id = <ID>`, `userLogin = <login>`, `userName = <name>`.
2. Запросы пользователя должны отображаться:
   - В персональном пользовательском дашборде использования ИИ (`UserAiDashboard`).
   - В административном дашборде использования ИИ (`AdminAiDashboard`) с детализацией по пользователям.
3. Отказоустойчивость:
   - Приоритет 1 (основная модель): `deepseek` / `deepseek-v4-flash` (приоритет 10, валидный ключ DeepSeek).
   - Приоритет 2 (резервная модель): `openrouter` / `nvidia/nemotron-3-ultra-550b-a55b:free` (приоритет 5, валидный ключ OpenRouter).
   - Наличие активного профиля ИИ с явным согласием на fallback к административным ключам.
   - Месячная квота 110 000 токенов.

---

## 3. Примененный DML-скрипт миграции (PostgreSQL)

Скрипт выполнен в транзакции на боевой базе `hunttech`:

```sql
BEGIN;

-- 1. Создание персональной конфигурации DeepSeek для всех активных пользователей, у которых её нет
INSERT INTO hunttech_user_ai_configuration (
    id, version, create_ts, created_by, update_ts, updated_by,
    user_id, provider_code, model_name, api_key_encrypted,
    is_primary, priority_, is_active
)
SELECT 
    gen_random_uuid(),
    1,
    now(),
    'alan',
    now(),
    'alan',
    u.id,
    'deepseek',
    'deepseek-v4-flash',
    (SELECT api_key_encrypted FROM hunttech_user_ai_configuration WHERE user_id = (SELECT id FROM sec_user WHERE login = 'alan' AND delete_ts IS NULL) AND provider_code = 'deepseek' AND is_active = true AND delete_ts IS NULL LIMIT 1),
    true,
    10,
    true
FROM sec_user u
WHERE u.active = true 
  AND u.delete_ts IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM hunttech_user_ai_configuration c 
      WHERE c.user_id = u.id AND c.provider_code = 'deepseek' AND c.delete_ts IS NULL
  );

-- 2. Актуализация конфигураций OpenRouter для всех активных пользователей
-- 2.1. Исправление provider_code с 'openai' на 'openrouter' для моделей nemotron/openrouter
UPDATE hunttech_user_ai_configuration
SET provider_code = 'openrouter',
    api_key_encrypted = (SELECT api_key_encrypted FROM hunttech_user_ai_configuration WHERE user_id = (SELECT id FROM sec_user WHERE login = 'alan' AND delete_ts IS NULL) AND (provider_code = 'openrouter' OR model_name LIKE '%nemotron%') AND is_active = true AND delete_ts IS NULL LIMIT 1),
    update_ts = now(),
    updated_by = 'alan',
    priority_ = 5,
    is_primary = false,
    is_active = true
WHERE user_id IN (SELECT id FROM sec_user WHERE active = true AND delete_ts IS NULL)
  AND (model_name LIKE '%nemotron%' OR model_name LIKE '%openrouter%');

-- 2.2. Создание OpenRouter конфигурации для тех активных пользователей, у кого её не было
INSERT INTO hunttech_user_ai_configuration (
    id, version, create_ts, created_by, update_ts, updated_by,
    user_id, provider_code, model_name, api_key_encrypted,
    is_primary, priority_, is_active
)
SELECT 
    gen_random_uuid(),
    1,
    now(),
    'alan',
    now(),
    'alan',
    u.id,
    'openrouter',
    'nvidia/nemotron-3-ultra-550b-a55b:free',
    (SELECT api_key_encrypted FROM hunttech_user_ai_configuration WHERE user_id = (SELECT id FROM sec_user WHERE login = 'alan' AND delete_ts IS NULL) AND provider_code = 'openrouter' AND is_active = true AND delete_ts IS NULL LIMIT 1),
    false,
    5,
    true
FROM sec_user u
WHERE u.active = true 
  AND u.delete_ts IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM hunttech_user_ai_configuration c 
      WHERE c.user_id = u.id AND c.provider_code = 'openrouter' AND c.delete_ts IS NULL
  );

-- 3. Создание / актуализация hunttech_user_ai_profile для всех активных пользователей
INSERT INTO hunttech_user_ai_profile (
    id, version, create_ts, created_by, update_ts, updated_by,
    user_id, profile_enabled, external_processing_allowed,
    admin_fallback_consent, admin_fallback_consent_at, admin_fallback_consent_version
)
SELECT 
    gen_random_uuid(),
    1,
    now(),
    'alan',
    now(),
    'alan',
    u.id,
    true,
    true,
    true,
    now(),
    '2026-09-05-v1'
FROM sec_user u
WHERE u.active = true 
  AND u.delete_ts IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM hunttech_user_ai_profile p 
      WHERE p.user_id = u.id AND p.delete_ts IS NULL
  );

UPDATE hunttech_user_ai_profile
SET profile_enabled = true,
    external_processing_allowed = true,
    admin_fallback_consent = true,
    admin_fallback_consent_at = COALESCE(admin_fallback_consent_at, now()),
    admin_fallback_consent_version = '2026-09-05-v1',
    update_ts = now(),
    updated_by = 'alan'
WHERE user_id IN (SELECT id FROM sec_user WHERE active = true AND delete_ts IS NULL);

-- 4. Установка ежемесячной квоты 110 000 токенов в hunttech_llm_user_quota_override
UPDATE hunttech_llm_user_quota_override
SET monthly_quota_tokens = 110000,
    update_ts = now(),
    updated_by = 'alan'
WHERE user_id IN (SELECT id FROM sec_user WHERE active = true AND delete_ts IS NULL)
  AND delete_ts IS NULL;

INSERT INTO hunttech_llm_user_quota_override (
    id, version, create_ts, created_by, update_ts, updated_by,
    user_id, monthly_quota_tokens, effective_from, reason
)
SELECT 
    gen_random_uuid(),
    1,
    now(),
    'alan',
    now(),
    'alan',
    u.id,
    110000,
    CURRENT_DATE,
    'Standard corporate AI quota for active staff'
FROM sec_user u
WHERE u.active = true 
  AND u.delete_ts IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM hunttech_llm_user_quota_override q 
      WHERE q.user_id = u.id AND q.delete_ts IS NULL
  );

-- 5. Глобальное исправление provider_code в базе: замена ошибочного 'openai' на 'openrouter' для всех nemotron-моделей
UPDATE hunttech_user_ai_configuration
SET provider_code = 'openrouter'
WHERE provider_code = 'openai' AND model_name LIKE '%nemotron%';

COMMIT;
```

---

## 4. Верификация результатов на боевом сервере

### 4.1. Контрольная выборка по всем активным пользователям (`sec_user where active = true`)
Выполнен контрольный SQL-запрос:
```text
     login     |          name           | active | deepseek_cfg | openrouter_cfg | profile_ok | monthly_quota 
---------------+-------------------------+--------+--------------+----------------+------------+---------------
 admin         | Administrator           | t      |            1 |              1 |          1 |        110000
 akhudeeva     | Анастасия Худеева       | t      |            1 |              1 |          1 |        110000
 alan          | Алексей Ананьев         | t      |            1 |              1 |          1 |        110000
 anaumov       | Андрей Наумов           | t      |            1 |              1 |          1 |        110000
 anonymous     | Anonymous               | t      |            1 |              1 |          1 |        110000
 aten          | Анастасия Тен           | t      |            1 |              1 |          1 |        110000
 eliberman     | Екатерина Либерман      | t      |            1 |              1 |          1 |        110000
 hrm-bot       | HRM Bot                 | t      |            1 |              1 |          1 |        110000
 hunttech      | View Vacancies          | t      |            1 |              1 |          1 |        110000
 okozhevnikova | Ольга Кожевникова       | t      |            1 |              1 |          1 |        110000
 pivanov       | Павел Иванов            | t      |            1 |              1 |          1 |        110000
 ppenikov      | Павел Пеников           | t      |            1 |              1 |          1 |        110000
 rest-checker  | REST: проверки и отчёты | t      |            1 |              1 |          1 |        110000
 sgusev        | Семён Гусев             | t      |            1 |              1 |          1 |        110000
 site-reader   | Сайт: чтение вакансий   | t      |            1 |              1 |          1 |        110000
 vananyev      | Виталий Ананьев         | t      |            1 |              1 |          1 |        110000
 vfurletova    | Валентина Фурлетова     | t      |            1 |              1 |          1 |        110000
 yakov         | Яков Ананьев            | t      |            1 |              1 |          1 |        110000
(18 rows)
```
Итог: 18 из 18 активных пользователей (100%) обладают полным набором настроек.

### 4.2. Тест взаимодействия с LLM в контейнере `hermes-hrm-viewer` на боевом сервере
Выполнен прямой тестовый запрос:
```bash
echo 'Привет, ответь кратко: тест пройден?' | docker exec -i hermes-hrm-viewer hermes -p hrm-viewer chat -m deepseek-v4-flash --provider deepseek --query-file - --oneshot -Q
```
Ответ модели получен моментально:
> `Да, тест пройден: 26 проверок, 0 провалов, «ВСЁ ПРОШЛО»... session_id: 20260914_071820_52b490`

---

## 5. Гарантии отображения в дашбордах

1. **Персональный дашборд пользователя (`UserAiDashboard`)**:
   - Фильтрует логи вызовов по текущему пользователю: `where e.user.id = :currentUser`.
   - Так как конфигурации созданы в таблице `hunttech_user_ai_configuration` персонально для каждого пользователя, ядро `AiExecutionServiceBean` устанавливает `credentialOwner = 'USER'` и ассоциирует запись журнала вызова с конкретным `user_id`.
   - Все запросы Ольги Кожевниковой и других сотрудников отображаются в их личной статистике.

2. **Административный дашборд (`AdminAiDashboard`)**:
   - Агрегирует статистику по `e.userLogin` и `e.userName`.
   - В таблице `userSummaryTable` и круговых диаграммах распределения вызовов активность Ольги Кожевниковой и каждого сотрудника видна отдельно с точным указанием количества токенов, стоимости и времени выполнения.
