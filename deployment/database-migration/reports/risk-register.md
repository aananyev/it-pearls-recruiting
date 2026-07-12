# Risk register

Дата: 2026-07-10

| ID | Риск | Вероятность | Влияние | Статус | Митигация |
|---|---|---:|---:|---|---|
| R-001 | Production architecture не подтверждена SQL-аудитом | Низкая | Критичное | Closed | Read-only production audit выполнен; см. `audit/production-architecture-audit.md` |
| R-002 | `itpearls` и `hunttech` могут быть не tablespace, а database/table prefix | Подтверждено | Высокое | Closed | Production `itpearls` является database/table prefix, не tablespace; `hunttech` отсутствует |
| R-003 | Потеря CUBA security данных | Средняя | Критичное | Open | Полный перенос `sec_*`, FK validation, login smoke tests |
| R-004 | Потеря пользовательских SMTP/IMAP/POP3 настроек | Средняя | Высокое | Open | Переносить `sec_user` extension columns без логирования значений |
| R-005 | Потеря файлов и фото пользователей | Средняя | Высокое | Open | Сверить `sys_file`, file storage, `image_id`, `official_photo_id`, `user_avatar_id` |
| R-006 | Legacy link-таблицы содержат неперенесенные данные | Средняя | Высокое | Open | Проверить row counts и mapping для `*_link__u*` |
| R-007 | Роль приложения имеет лишние PostgreSQL privileges | Средняя | Высокое | Open | Снять grants, owner, default privileges; согласовать least privilege |
| R-008 | Backup не восстановим | Средняя | Критичное | Open | File/list/checksum verification пройдены; full test restore не пройден из-за нехватки места |
| R-009 | Post-cutover данные потеряются при rollback | Средняя | Критичное | Open | Maintenance mode до smoke tests, определить политику post-cutover writes |
| R-010 | Liquibase и CUBA scripts расходятся | Средняя | Высокое | Open | Сравнить `SYS_DB_CHANGELOG`, Liquibase history и фактическую схему |
| R-011 | Secrets попадут в Git | Средняя | Критичное | Open | `.gitignore`, manual review, masking |
| R-012 | Production migration займет больше окна | Неизвестно | Высокое | Open | Измерить backup/restore/test migration duration |
| R-013 | Разные имена DB: `hunttech`, `HuntTech`, `itpearls` | Средняя | Высокое | Open | Source confirmed as `itpearls`; target canonical name still requires approval |
| R-014 | Существующий FDW migration script может изменить production | Средняя | Критичное | Open | Не запускать без отдельного review и отдельного задания |
| R-015 | Локальная роль `cuba` superuser маскирует проблемы прав | Высокая | Среднее | Open | Проверять на роли, эквивалентной production app user |
| R-016 | Production `itpearls` оказался database, а не tablespace | Подтверждено | Высокое | Open | Проектировать миграцию как database/table-prefix migration, не как tablespace move |
| R-017 | `cuba.automaticDatabaseUpdate=true` на production | Высокая | Критичное | Open | Перед deployment/cutover явно отключить или контролировать update scripts |
| R-018 | Локальное test restore окружение не имеет места | Подтверждено | Высокое | Open | Повторить restore в окружении с минимум 20 GB свободно |
| R-019 | `sys_scheduled_execution` занимает почти весь объем базы | Высокая | Среднее | Open | Отдельно решить retention/cleanup после backup и только отдельным заданием |
| R-020 | Локальная база `hunttech` удалена перед повторной отработкой | Подтверждено read-only проверкой | Высокое | Open | Не пересоздавать target DB без отдельного approved preparation step; не опираться на устаревшие local `hunttech` diff |
| R-021 | Локальное свободное место после очистки все еще с небольшим запасом | Подтверждено | Среднее | Open | Перед restore убедиться, что 27 GiB достаточно, либо выбрать более просторное изолированное окружение |
| R-022 | Client utilities в shell `14.20`, local server и production `11.22` | Подтверждено | Среднее | Open | Использовать matching utilities `11.22` для restore или явно документировать версию client tools |
| R-023 | Production содержит validated FK с фактическими orphan rows | Подтверждено | Критичное | Open | До миграции утвердить правило исправления/маппинга 17 `itpearls_job_candidate.current_company_id` ссылок на отсутствующие компании |
| R-024 | На `hr.hunttech.ru` есть другие базы данных, которые можно случайно затронуть cluster-wide операциями | Подтверждено требованием владельца | Критичное | Open | Миграция должна быть строго ограничена `itpearls` -> `hunttech`; запрет wildcard-команд, globals restore, shared roles/grants/tablespaces без отдельного анализа; обязательные preflight-проверки hostname, port, `current_database()`, source/target DB |

## Главный текущий риск

Production-база проаудирована и backup создан. После очистки диска restore прошел дальше прежней точки, но full test restore снова не пройден: восстановление остановилось на FK `itpearls_job_candidate.current_company_id -> itpearls_company.id`, потому что в production подтверждено 17 orphan rows. Дополнительно локальная база `hunttech` сейчас отсутствует. Промышленную миграцию проводить нельзя до утвержденного правила обработки этих orphan rows и успешного full restore validation.
