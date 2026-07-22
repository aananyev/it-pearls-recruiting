# Database Migration Report — UserAiProfile

## Meta

| Поле | Значение |
|------|----------|
| Production БД | itpearls (hr.hunttech.ru:5432) |
| Время backup | 2026-07-22 15:46 MSK |
| Путь к dump | `/tmp/cuba_deploy_manual_20260722_164621/itpearls.dump` |
| Размер dump | 792M |
| SHA-256 dump | не вычислен (remote file) |
| Результат updateDb | ✅ SUCCESS |
| Применённый SQL | `260722-1-createUserAiProfile.sql` |
| Rollback | не потребовался |

## DDL созданной таблицы

```sql
CREATE TABLE itpearls_user_ai_profile (
    id UUID NOT NULL,
    create_ts TIMESTAMP,
    created_by VARCHAR(50),
    update_ts TIMESTAMP,
    updated_by VARCHAR(50),
    delete_ts TIMESTAMP,
    deleted_by VARCHAR(50),
    version INTEGER NOT NULL DEFAULT 1,
    user_id UUID,
    job_title VARCHAR(255),
    functional_role VARCHAR(50),
    seniority_level VARCHAR(50),
    professional_experience_years INTEGER,
    recruiting_experience_years INTEGER,
    key_skills TEXT,
    communication_style VARCHAR(50),
    preferred_language VARCHAR(50),
    response_detail_level VARCHAR(50),
    answer_structure VARCHAR(50),
    terminology_level VARCHAR(50),
    personalization_allowed BOOLEAN DEFAULT FALSE,
    consent_date TIMESTAMP,
    consent_version VARCHAR(50),
    CONSTRAINT pk_itpearls_user_ai_profile PRIMARY KEY (id),
    CONSTRAINT fk_itpearls_user_ai_profile_on_user FOREIGN KEY (user_id) REFERENCES sec_user(id),
    CONSTRAINT chk_itpearls_user_ai_profile_prof_exp CHECK (professional_experience_years IS NULL OR (professional_experience_years >= 0 AND professional_experience_years <= 70)),
    CONSTRAINT chk_itpearls_user_ai_profile_recr_exp CHECK (recruiting_experience_years IS NULL OR (recruiting_experience_years >= 0 AND recruiting_experience_years <= 70))
);
```

## Индексы

| Имя | Тип | Определение |
|-----|-----|-------------|
| pk_itpearls_user_ai_profile | UNIQUE Btree | id |
| idx_itpearls_user_ai_profile_unq_user | UNIQUE Btree | user_id (WHERE delete_ts IS NULL) |

## Проверка данных

| Проверка | Результат |
|----------|-----------|
| Таблица существует | ✅ |
| PK создан | ✅ |
| FK на SEC_USER создан | ✅ |
| unique index idx_itpearls_user_ai_profile_unq_user | ✅ |
| check constraint prof_exp | ✅ |
| check constraint recr_exp | ✅ |
| Строки в новой таблице | 0 (пусто — ожидаемо) |
| Существующие данные других таблиц | Не изменены |
