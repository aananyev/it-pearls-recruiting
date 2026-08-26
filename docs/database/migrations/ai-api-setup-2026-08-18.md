# Настройка AI API для вызовов AI-функций HRM HuntTech (2026-08-18)

Что необходимо, чтобы вызовы AI-функций (`AiExecutionService.executeText/executeImage`) реально работали:

1. **Миграция БД** `260818-1-addAdminAiPromptSeed` — таблицы AI Control Plane + 7 промптов (применена; идемпотентна, повторный прогон безопасен).
2. **Мастер-ключ шифрования** `hunttech.ai.encryptionKey` — в `deploy/app_home/local.app.properties` (и в app_home того Tomcat, который реально запущен: `deploy/tomcat/app_home/local.app.properties`). Без него AiSecretCipher падает: «Не настроен hunttech.ai.encryptionKey: требуется секрет не короче 32 символов» (см. catalina.out).
3. **Активный корпоративный ключ** `hunttech_admin_ai_configuration` (provider, зашифрованный `api_key_encrypted`, `is_active=true`).
4. **Привязка** `hunttech_ai_function_configuration.admin_configuration_id` на активный admin-конфиг (для policy `USER_OVERRIDE_ALLOWED`).
5. **Для policy `USER_REQUIRED`** (`STANDARDIZE_VACANCY`) — пользовательский ключ `hunttech_user_ai_configuration` + override `hunttech_user_ai_function_override`.

## Текущее состояние локальной dev-БД (hunttech@127.0.0.1:5432)

- Таблицы Control Plane: есть (4).
- AI-функции: 7, все `is_active=true`, `delete_ts IS NULL`; admin_configuration_id привязан.
- Admin-конфиг: DeepSeek (active, priority 1).
- Override: alan (a9c2a715-...) → STANDARDIZE_VACANCY (deepseek-v4-flash).

## Шифрование ключа (AiSecretCipher)

Формат `v1:<base64 iv>:<base64 ct>` — AES/GCM/NoPadding, **ключ = ВЕСЬ SHA-256(masterKey)** (32 байта, AES-256; НЕ `[0:16]` — скилл hunttech-ai-control-plane в этом месте устарел).

Инструмент: `/tmp/AiSecretTool.java` (временный). Запуск:
```bash
javac AiSecretTool.java && java AiSecretTool "<masterKey>" "<apiKey>"
```
Значение класть в `api_key_encrypted` (регистр префикса не важен: `V1:`/`v1:`).

## SQL-скрипт настройки (идемпотентный, без секретов в тексте)

Ключ и id передаются переменными psql (`admin_id`, `enc`, `override_id`):

```sql
\set ON_ERROR_STOP 1

INSERT INTO hunttech_admin_ai_configuration
    (id, version, create_ts, created_by, name, provider_code, api_key_encrypted,
     default_model_name, base_api_url, is_active, priority_, last_test_status, last_test_at, last_error)
SELECT :'admin_id'::uuid, 1, now(), 'hermes', 'DeepSeek (локальный dev)', 'deepseek',
       :'enc', 'deepseek-v4-flash', NULL, true, 1, NULL, NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM hunttech_admin_ai_configuration
    WHERE provider_code = 'deepseek' AND delete_ts IS NULL AND is_active
);

UPDATE hunttech_ai_function_configuration f
   SET admin_configuration_id = (
       SELECT id FROM hunttech_admin_ai_configuration
       WHERE is_active AND delete_ts IS NULL
       ORDER BY priority_, create_ts LIMIT 1
   )
 WHERE f.delete_ts IS NULL;

INSERT INTO hunttech_user_ai_function_override
    (id, version, create_ts, user_id, ai_function_id, user_ai_configuration_id, model_name, enabled)
SELECT :'override_id'::uuid, 1, now(),
       '<user_id>', f.id, '<user_ai_configuration_id>', 'deepseek-v4-flash', true
  FROM hunttech_ai_function_configuration f
 WHERE f.code = 'STANDARDIZE_VACANCY' AND f.delete_ts IS NULL
   AND NOT EXISTS (SELECT 1 FROM hunttech_user_ai_function_override o
                    WHERE o.user_id = '<user_id>' AND o.ai_function_id = f.id);
```

Пример вызова (локально):
```bash
export PGPASSWORD=cuba
ADMIN_ID=$(python3 -c "import uuid; print(uuid.uuid4())")
OVERRIDE_ID=$(python3 -c "import uuid; print(uuid.uuid4())")
ENC=$(java AiSecretTool "$(grep '^hunttech.ai.encryptionKey=' ../hunttech_recruiting/deploy/app_home/local.app.properties | cut -d= -f2-)" "$DEEPSEEK_API_KEY")
psql -h 127.0.0.1 -U cuba -d hunttech -v ON_ERROR_STOP=1 \
     -v admin_id="$ADMIN_ID" -v enc="$ENC" -v override_id="$OVERRIDE_ID" \
     -f setup_ai_admin.sql
```

Проверка:
```sql
SELECT f.code, f.execution_policy, f.admin_configuration_id IS NOT NULL AS has_admin,
       (SELECT count(*) FROM hunttech_user_ai_function_override o
         WHERE o.ai_function_id = f.id AND o.enabled) AS overrides
  FROM hunttech_ai_function_configuration f
 WHERE f.delete_ts IS NULL ORDER BY f.code;
```

## Важно

- После добавления `hunttech.ai.encryptionKey` в app_home текущего Tomcat — **рестарт Tomcat** (ключ читается при старте контекста; `start-app.sh --force --branch <worktree>`).
- На прод: миграция 260818-1 + настройка ключей — зона Hermes-1 (владелец согласует API-ключ; миграция самодостаточна, пофайловые 260812-2..260816-5 не применять).
- `gen_random_uuid()`/`uuid_generate_v4()` на локальной PG11 отсутствуют (pgcrypto не установлен) — id генерировать клиентом.
