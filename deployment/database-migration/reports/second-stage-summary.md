# Second-stage summary

Дата: 2026-07-10

## 1. Фактическое имя production-базы

`itpearls`

## 2. Фактическая версия PostgreSQL

`PostgreSQL 11.22 (Ubuntu 11.22-1.pgdg20.04+1)`

## 3. Фактические schemas

`public`

## 4. Фактические tablespaces

- `pg_default`
- `pg_global`

## 5. Существует ли tablespace `itpearls`

Нет.

## 6. Существует ли база `itpearls`

Да.

## 7. Существует ли tablespace `hunttech`

Нет.

## 8. Какие tablespaces используют production-объекты

`pg_default`

## 9. Фактический владелец production-базы

`cuba`

## 10. Technical user приложения

`cuba`

## 11. Production roles, ownership и grants

Подробный полный grants-аудит подготовлен в `audit/sql/production-security-audit.sql`. Установлено: application DB owner и technical user - `cuba`.

## 12. Количество таблиц

154

## 13. Critical security records

| Metric | Count |
|---|---:|
| `sec_user` | 89 |
| active users | 15 |
| inactive or blocked users | 71 |
| `sec_role` | 12 |
| `sec_user_role` | 178 |
| `sec_group` | 7 |
| `sec_permission` | 3980 |
| `sec_constraint` | 0 |
| `sec_user_setting` | 1471 |
| `sec_remember_me` | 88 |
| `sec_session_log` | 29537 |
| `sys_file` | 13458 |
| `sys_db_changelog` | 995 |

## 14. Размер базы

`6342 MB`

## 15. Самые крупные таблицы

1. `sys_scheduled_execution` - 19,662,724 estimated rows, 5972 MB.
2. `itpearls_candidate_cv` - 7,752 estimated rows, 89 MB.
3. `sys_fts_queue` - 198,822 estimated rows, 49 MB.
4. `itpearls_iteraction_list` - 66,964 estimated rows, 48 MB.
5. `ddcdi_import_exec_detail` - 4,004 estimated rows, 23 MB.

## 16. Отличия production от локальной `itpearls`

На audited column metadata level отличий нет.

## 17. Отличия production от локальной `hunttech`

- две пустые legacy link-таблицы отсутствуют в `hunttech`;
- default `temperature=0.7` есть в production, но отсутствует в local `hunttech`.

## 18. Статус legacy link-таблиц

Обе существуют и обе пустые:

- `itpearls_job_candidate_position_link__u59616`: 0 rows
- `itpearls_open_position_city_link__u70664`: 0 rows

## 19. Статус `vacancy_prompt_template.temperature`

Production default: `0.7`.

## 20. Статус AI changeset

AI objects exist on production:

- `itpearls_user_ai_configuration`
- `itpearls_vacancy_prompt_template`
- `itpearls_open_position.raw_description`

## 21. Путь к backup

`/var/backups/hunttech-hrm/20260710-100131/itpearls_20260710-100131.dump`

## 22. Размер backup

829701541 bytes / 792 MB.

## 23. SHA-256 backup

`43e78190d9e9a176f38d6d44384ff0a697d6742073afa355dd7c1a3f9ea1aa1f`

## 24. Результат `pg_restore --list`

Успешно, exit code 0, 1093 entries.

## 25. Результат тестового восстановления

Не пройдено. Restore остановился из-за недостатка свободного места в локальном окружении.

## 26. Ошибки и предупреждения restore

Primary error:

`No space left on device`

Secondary errors:

- не создалась часть indexes/constraints;
- последующие FK ошибки являются следствием неполного restore.

## 27. Результаты проверки данных

Полная проверка данных не выполнена из-за неполного restore.

## 28. Результаты проверки связей

Полная проверка связей не выполнена из-за неполного restore.

## 29. Результаты проверки security HRM

Production aggregate security counts собраны. Restore security validation не завершен.

## 30. Результаты проверки sequences

Production sequences count: 2. Restore sequence validation не завершен.

## 31. Список созданных файлов

См. новые файлы в:

- `audit/production-*.md`
- `audit/sql/production-*.sql`
- `backup/*.sh`
- `validation/*.sql`
- `runbooks/*`
- `reports/*`

## 32. Список выполненных команд без секретов

- `pg_isready -h hr.hunttech.ru -p 5432`
- SSH read-only checks for systemd/Tomcat config with secret redaction
- `psql` read-only production audit inside `BEGIN READ ONLY`
- `pg_dump --format=custom --verbose --no-password`
- `pg_dumpall --globals-only --no-password`
- `pg_restore --list`
- `sha256sum`
- local `pg_restore --no-owner --no-privileges` into isolated test DB

## 33. Production не изменялась

Production database structure and data were not changed. No DDL/DML was executed inside production database.

Files were created only in backup directory:

`/var/backups/hunttech-hrm/20260710-100131`

## 34. Оставшиеся блокирующие вопросы

1. Где выполнить полный test restore с достаточным диском?
2. Удалять ли частичный локальный restore DB после review?
3. Сохранять ли `temperature=0.7` default в target `hunttech`?
4. Исключать ли пустые legacy link tables из миграции?
5. Что делать с `cuba.automaticDatabaseUpdate=true` перед будущим cutover?

## 35. Возможность перехода к migration scripts

Переходить к финальным migration scripts пока рано. Нужно сначала выполнить успешный full test restore и validation.

## Verdict

`BACKUP СОЗДАН, НО TEST RESTORE НЕ ПРОЙДЕН`
