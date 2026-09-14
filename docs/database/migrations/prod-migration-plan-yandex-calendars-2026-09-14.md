# План миграции структуры (DDL) и системных настроек корпоративных Яндекс-календарей (DML) на Production (`hr.hunttech.ru`)

> **Дата составления**: 14 сентября 2026 г.  
> **Окружение**: Production `hr.hunttech.ru` (`92.63.101.170`), СУБД PostgreSQL 11.22 (`hunttech`), Apache Tomcat 9  
> **Статус плана**: **ГОТОВ К ПРИМЕНЕНИЮ НА PRODUCTION**  
> **Ответственный агент**: Разработчик (ветка `agent/antigravity-dev`) / CI/CD Hermes-1  
> **Область изменений**: Интеграция взаимодействий с кандидатами с Яндекс.Календарём (CalDAV) и централизованное управление корпоративными календарями (`hunttech_CorporateYandexCalendar`)

---

## ⚠ Базовое правило безопасности (МАНДАТОРНО)

> [!IMPORTANT]
> **Любые изменения структуры (DDL) и системных данных (DML) ВСЕГДА фиксируются в этом плане миграции и применяются при деплое на боевой сервер.**  
> **ПРЕЖДЕ чем применять любые миграции на прод, ОБЯЗАТЕЛЬНО выполняется полный логический бэкап базы данных (`pg_dump -Fc`) и артефактов (`wars`, `conf`), если пользователь явно не предложит иное.**

---

## 1. Состав миграции

### 1.1. Структура базы данных (DDL):
1. **Новая таблица `HUNTTECH_CORP_YANDEX_CAL`** (Справочник корпоративных Яндекс Календарей):
   - `ID` (UUID, PK)
   - `VERSION`, `CREATE_TS`, `CREATED_BY`, `UPDATE_TS`, `UPDATED_BY`, `DELETE_TS`, `DELETED_BY`
   - `NAME` (VARCHAR(255), NOT NULL) — отображаемое название календаря
   - `CALENDAR_PATH` (VARCHAR(512), NOT NULL) — путь / идентификатор CalDAV
   - `ACCOUNT_EMAIL` (VARCHAR(255)) — email сервисного аккаунта Яндекс
   - `CALENDAR_BASE_URL` (VARCHAR(512), DEFAULT 'https://caldav.yandex.ru') — точка входа CalDAV
   - `IS_DEFAULT` (BOOLEAN, DEFAULT FALSE) — признак корпоративного календаря по умолчанию
   - `ACTIVE` (BOOLEAN, DEFAULT TRUE) — статус доступности для выбора рекрутерами
   - `DESCRIPTION` (VARCHAR(1024)) — описание назначения календаря
   - `OAUTH_TOKEN_ENCRYPTED` (VARCHAR(4096)) — зашифрованный токен доступа к CalDAV

2. **Колонки в таблице `HUNTTECH_ITERACTION_LIST`** (История взаимодействий с кандидатами):
   - `CALENDAR_EVENT_ID` (VARCHAR(255)) — UID события в CalDAV iCalendar RFC 5545
   - `CALENDAR_ID` (VARCHAR(512)) — путь к целевому календарю события
   - `CALENDAR_SYNC_STATE` (VARCHAR(50)) — статус синхронизации (`SYNCED` / `FAILED`)
   - `ADD_TO_CALENDAR` (BOOLEAN, DEFAULT FALSE) — флаг включения события в календарь

### 1.2. Системные данные и настройки (DML pre-seed):
Автоматическое создание двух подключенных к системе корпоративных календарей с наследованием зашифрованного токена аккаунта `alan@hunttech.ru`:
1. **«Hunttech у заказчика»**:
   - `CALENDAR_PATH`: `/calendars/alan%40hunttech.ru/events-34179601/`
   - `ACCOUNT_EMAIL`: `alan@hunttech.ru`
   - `IS_DEFAULT`: `true`
   - `ACTIVE`: `true`
   - `DESCRIPTION`: `Основной корпоративный календарь для проведения собеседований с заказчиками`
   - `OAUTH_TOKEN_ENCRYPTED`: копируется из `HUNTTECH_USER_YANDEX_CONFIG` пользователя `alan`
2. **«Мои события (alan@hunttech.ru)»**:
   - `CALENDAR_PATH`: `/calendars/alan%40hunttech.ru/events-32367521/`
   - `ACCOUNT_EMAIL`: `alan@hunttech.ru`
   - `IS_DEFAULT`: `false`
   - `ACTIVE`: `true`
   - `DESCRIPTION`: `Корпоративный календарь общих встреч и событий аккаунта alan@hunttech.ru`
   - `OAUTH_TOKEN_ENCRYPTED`: копируется из `HUNTTECH_USER_YANDEX_CONFIG` пользователя `alan`

---

## 2. Пошаговый протокол выполнения миграции на Production

### Шаг 0. Полный серверный бэкап (Safety Gate)

Выполняется на сервере `hr.hunttech.ru` под учетной записью администратора с правами `sudo`:

```bash
# 1. Создание защищенного каталога бэкапа с временной меткой
BACKUP_DIR="/tmp/cuba_deploy_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR/wars"

# 2. Создание полного бинарного дампа PostgreSQL базы данных hunttech
su - postgres -c "pg_dump -Fc hunttech" > "$BACKUP_DIR/hunttech_pre_yandex_cal.dump"

# 3. Резервное копирование текущих боевых WAR-файлов
cp -a /var/lib/tomcat9/webapps/hrm.war "$BACKUP_DIR/wars/" 2>/dev/null || true
cp -a /var/lib/tomcat9/webapps/hrm-core.war "$BACKUP_DIR/wars/" 2>/dev/null || true

# 4. Проверка целостности созданного дампа
su - postgres -c "pg_restore -l $BACKUP_DIR/hunttech_pre_yandex_cal.dump | head -n 15"
ls -lh "$BACKUP_DIR/hunttech_pre_yandex_cal.dump"
```

---

### Шаг 1. Применение структурных DDL-миграций

Выполняется либо автоматически через Liquibase (`db.changelog-master.xml` при старте приложения), либо вручную через `psql`:

```bash
su - postgres -c "psql -d hunttech" << 'EOF'
BEGIN;

-- 1. Создание таблицы HUNTTECH_CORP_YANDEX_CAL (если не создана)
CREATE TABLE IF NOT EXISTS HUNTTECH_CORP_YANDEX_CAL (
    ID uuid NOT NULL,
    VERSION integer NOT NULL,
    CREATE_TS timestamp without time zone,
    CREATED_BY character varying(50),
    UPDATE_TS timestamp without time zone,
    UPDATED_BY character varying(50),
    DELETE_TS timestamp without time zone,
    DELETED_BY character varying(50),
    NAME character varying(255) NOT NULL,
    CALENDAR_PATH character varying(512) NOT NULL,
    ACCOUNT_EMAIL character varying(255),
    CALENDAR_BASE_URL character varying(512) DEFAULT 'https://caldav.yandex.ru',
    IS_DEFAULT boolean DEFAULT false,
    ACTIVE boolean DEFAULT true,
    DESCRIPTION character varying(1024),
    OAUTH_TOKEN_ENCRYPTED character varying(4096),
    CONSTRAINT PK_HUNTTECH_CORP_YANDEX_CAL PRIMARY KEY (ID)
);

-- 2. Добавление колонок в HUNTTECH_ITERACTION_LIST
ALTER TABLE HUNTTECH_ITERACTION_LIST ADD COLUMN IF NOT EXISTS CALENDAR_EVENT_ID character varying(255);
ALTER TABLE HUNTTECH_ITERACTION_LIST ADD COLUMN IF NOT EXISTS CALENDAR_ID character varying(512);
ALTER TABLE HUNTTECH_ITERACTION_LIST ADD COLUMN IF NOT EXISTS CALENDAR_SYNC_STATE character varying(50);
ALTER TABLE HUNTTECH_ITERACTION_LIST ADD COLUMN IF NOT EXISTS ADD_TO_CALENDAR boolean DEFAULT false;

COMMIT;
EOF
```

---

### Шаг 2. Применение DML Pre-Seed миграции корпоративных календарей

Выполняется наполнение записей и привязка зашифрованного токена пользователя `alan`:

```bash
su - postgres -c "psql -d hunttech" << 'EOF'
BEGIN;

-- 1. Основной корпоративный календарь собеседований с заказчиками «Hunttech у заказчика»
INSERT INTO HUNTTECH_CORP_YANDEX_CAL (
    ID, VERSION, CREATE_TS, CREATED_BY, UPDATE_TS, UPDATED_BY, DELETE_TS, DELETED_BY,
    NAME, CALENDAR_PATH, ACCOUNT_EMAIL, CALENDAR_BASE_URL, IS_DEFAULT, ACTIVE, DESCRIPTION, OAUTH_TOKEN_ENCRYPTED
)
SELECT 
    '26091400-0000-4000-a000-000000000001'::uuid,
    1,
    now(),
    'admin',
    now(),
    'admin',
    null,
    null,
    'Hunttech у заказчика',
    '/calendars/alan%40hunttech.ru/events-34179601/',
    'alan@hunttech.ru',
    'https://caldav.yandex.ru',
    true,
    true,
    'Основной корпоративный календарь для проведения собеседований с заказчиками',
    (SELECT c.OAUTH_TOKEN_ENCRYPTED FROM HUNTTECH_USER_YANDEX_CONFIG c JOIN SEC_USER u ON u.ID = c.USER_ID WHERE lower(u.LOGIN) = 'alan' AND c.DELETE_TS IS NULL AND u.DELETE_TS IS NULL LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_CORP_YANDEX_CAL WHERE CALENDAR_PATH = '/calendars/alan%40hunttech.ru/events-34179601/'
);

-- 2. Корпоративный календарь общих встреч и событий аккаунта alan@hunttech.ru
INSERT INTO HUNTTECH_CORP_YANDEX_CAL (
    ID, VERSION, CREATE_TS, CREATED_BY, UPDATE_TS, UPDATED_BY, DELETE_TS, DELETED_BY,
    NAME, CALENDAR_PATH, ACCOUNT_EMAIL, CALENDAR_BASE_URL, IS_DEFAULT, ACTIVE, DESCRIPTION, OAUTH_TOKEN_ENCRYPTED
)
SELECT 
    '26091400-0000-4000-a000-000000000002'::uuid,
    1,
    now(),
    'admin',
    now(),
    'admin',
    null,
    null,
    'Мои события (alan@hunttech.ru)',
    '/calendars/alan%40hunttech.ru/events-32367521/',
    'alan@hunttech.ru',
    'https://caldav.yandex.ru',
    false,
    true,
    'Корпоративный календарь общих встреч и событий аккаунта alan@hunttech.ru',
    (SELECT c.OAUTH_TOKEN_ENCRYPTED FROM HUNTTECH_USER_YANDEX_CONFIG c JOIN SEC_USER u ON u.ID = c.USER_ID WHERE lower(u.LOGIN) = 'alan' AND c.DELETE_TS IS NULL AND u.DELETE_TS IS NULL LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_CORP_YANDEX_CAL WHERE CALENDAR_PATH = '/calendars/alan%40hunttech.ru/events-32367521/'
);

-- 3. Синхронизация токена, если календарь был предварительно заведен без токена
UPDATE HUNTTECH_CORP_YANDEX_CAL
SET OAUTH_TOKEN_ENCRYPTED = (SELECT c.OAUTH_TOKEN_ENCRYPTED FROM HUNTTECH_USER_YANDEX_CONFIG c JOIN SEC_USER u ON u.ID = c.USER_ID WHERE lower(u.LOGIN) = 'alan' AND c.DELETE_TS IS NULL AND u.DELETE_TS IS NULL LIMIT 1)
WHERE (OAUTH_TOKEN_ENCRYPTED IS NULL OR OAUTH_TOKEN_ENCRYPTED = '')
  AND ACCOUNT_EMAIL = 'alan@hunttech.ru'
  AND EXISTS (SELECT 1 FROM HUNTTECH_USER_YANDEX_CONFIG c JOIN SEC_USER u ON u.ID = c.USER_ID WHERE lower(u.LOGIN) = 'alan' AND c.DELETE_TS IS NULL AND u.DELETE_TS IS NULL AND c.OAUTH_TOKEN_ENCRYPTED IS NOT NULL);

COMMIT;
EOF
```

---

### Шаг 3. Верификация целостности данных (Post-Migration Verification)

Запустить контрольный SQL-запрос для проверки структуры и данных:

```bash
su - postgres -c "psql -d hunttech" << 'EOF'
SELECT 
    NAME, 
    ACCOUNT_EMAIL, 
    CALENDAR_PATH, 
    IS_DEFAULT, 
    ACTIVE, 
    (OAUTH_TOKEN_ENCRYPTED IS NOT NULL AND length(OAUTH_TOKEN_ENCRYPTED) > 10) AS HAS_TOKEN
FROM HUNTTECH_CORP_YANDEX_CAL
ORDER BY IS_DEFAULT DESC, NAME ASC;
EOF
```

**Ожидаемый результат:**
```text
             name              |  account_email   |                 calendar_path                 | is_default | active | has_token 
-------------------------------+------------------+-----------------------------------------------+------------+--------+-----------
 Hunttech у заказчика          | alan@hunttech.ru | /calendars/alan%40hunttech.ru/events-34179601/ | t          | t      | t
 Мои события (alan@hunttech.ru)| alan@hunttech.ru | /calendars/alan%40hunttech.ru/events-32367521/ | f          | t      | t
(2 rows)
```

---

## 3. План отката (Rollback Plan)

В случае необходимости отката миграции до первоначального состояния:

```bash
# 1. Откат DML записей корпоративных календарей
su - postgres -c "psql -d hunttech" << 'EOF'
BEGIN;
DELETE FROM HUNTTECH_CORP_YANDEX_CAL WHERE ID IN (
    '26091400-0000-4000-a000-000000000001'::uuid,
    '26091400-0000-4000-a000-000000000002'::uuid
);
COMMIT;
EOF

# 2. При критических сбоях — полное восстановление из логического дампа
# su - postgres -c "pg_restore --clean --if-exists -d hunttech $BACKUP_DIR/hunttech_pre_yandex_cal.dump"
```

---

## 4. Контрольный чек-лист деплоя

| № | Проверка | Статус |
|---|---|---|
| 1 | Полный бэкап PostgreSQL (`pg_dump -Fc`) выполнен и верифицирован | [ ] |
| 2 | Таблица `HUNTTECH_CORP_YANDEX_CAL` создана со всеми полями | [ ] |
| 3 | Колонки `CALENDAR_*` и `ADD_TO_CALENDAR` добавлены в `HUNTTECH_ITERACTION_LIST` | [ ] |
| 4 | Записи «Hunttech у заказчика» и «Мои события» созданы с токенами | [ ] |
| 5 | Ровно один корпоративный календарь имеет `is_default = true` | [ ] |
| 6 | Пункт меню «Корпоративные Яндекс Календари» доступен в веб-интерфейсе | [ ] |
| 7 | Создание встречи кандидату с добавлением в календарь успешно отрабатывает под рекрутером без ошибок CalDAV | [ ] |
