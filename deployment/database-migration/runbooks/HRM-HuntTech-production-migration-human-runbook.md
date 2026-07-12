# Регламент безопасной миграции базы данных HRM HuntTech с itpearls на hunttech

Дата: 2026-07-12
Статус: production-миграция не разрешена; документ фиксирует проверенный локальный алгоритм и production-условия.

Все сведения, не подтвержденные отчетами проекта, помечаются как `ТРЕБУЕТ ПОДТВЕРЖДЕНИЯ ПЕРЕД PRODUCTION`.

## 1. Назначение документа

Цель миграции: перенести HRM HuntTech с legacy database `itpearls` на новую database `hunttech` без потери данных, связей, security-модели, файловых метаданных и истории.

Подтвержденная production-архитектура:

- сервер приложения и PostgreSQL: `hr.hunttech.ru`;
- production database: `itpearls`;
- PostgreSQL: `11.22`;
- schema: `public`;
- owner и technical user: `cuba`;
- JDBC production: `jdbc:postgresql://localhost/itpearls`;
- tablespaces: `pg_default`, `pg_global`;
- tablespace `itpearls` и tablespace `hunttech` отсутствуют.

Target-архитектура: новая отдельная database `hunttech`, schema `public`, tablespace `pg_default`.

Это миграция database-to-database, а не перенос между PostgreSQL tablespaces. Старая database `itpearls` не преобразуется на месте, потому что она должна остаться неизменной точкой возврата. Миграция in-place запрещена.

## 2. Участники и ответственность

| Роль | Ответственность |
|---|---|
| Руководитель миграции | Утверждает окно, принимает go/no-go и rollback-решения, подписывает итоговый отчет. |
| DBA | Выполняет backup, restore, создание target DB, SQL-валидацию, контроль constraints/sequences/grants. |
| Системный администратор | Останавливает/запускает сервисы, контролирует диск, systemd/Tomcat, file storage, backup directory. |
| Разработчик HRM | Контролирует CUBA/Jmix-логику, schema scripts, `automaticDatabaseUpdate`, smoke tests. |
| Ответственный за проверку данных | Сверяет counts, checksums, security агрегаты, business records, карантин миграции. |
| Представитель пользователей | Проверяет пользовательские сценарии рекрутера и принимает бизнес-результат. |
| Лицо rollback-решения | Разрешает rollback, особенно после открытия записи пользователям. |

## 3. Предварительные условия

Перед стартом production-миграции каждый пункт должен иметь статус, время, исполнителя, результат, ссылку на лог и комментарий.

- Окно работ утверждено.
- Пользователи предупреждены.
- Production credentials известны оператору, но не записаны в Git.
- Доступ к `hr.hunttech.ru` проверен.
- Свободное место рассчитано и подтверждено.
- Backup directory существует, доступен и находится вне PostgreSQL data directory.
- Restore-окружение с достаточным местом готово.
- Git commit утвержден; все migration-скрипты закоммичены.
- Working tree чистый или список отличий утвержден.
- Backup и restore протестированы.
- Rollback simulation выполнен.
- File storage path найден и проверен.
- Outbound email, Telegram, integrations и scheduled jobs можно отключить.
- `cuba.automaticDatabaseUpdate` отключен или контролируется.
- Решение по двум legacy link-таблицам утверждено.
- Решение по `temperature=0.7` утверждено.
- Решение по `sys_scheduled_execution` утверждено.
- Решение по 17 orphan `current_company_id` утверждено.
- Другие базы на `hr.hunttech.ru` защищены: запрещены cluster-wide операции без отдельного решения.

## 4. Требования к дисковому пространству

Расчет должен учитывать:

- размер source database `itpearls`: подтверждено около 6342 MB;
- размер custom dump: подтверждено около 792 MB для backup от 2026-07-10;
- размер target database после локальной миграции: около 6378 MB;
- временное место для restore, indexes, constraints;
- WAL во время загрузки и создания indexes;
- PostgreSQL logs и migration logs;
- физический file storage;
- резерв не менее 30-50%.

Формула допуска: `source_db + target_db + dump + WAL/index_temp + logs + file_storage_copy + 30-50% reserve`.

Миграцию запрещено начинать, если диск, WAL filesystem, backup filesystem или file storage filesystem не имеют подтвержденного запаса.

## 5. Подготовка production

1. Проверить hostname: ожидается `hr.hunttech.ru`.
2. Проверить PostgreSQL version: ожидается `11.22`.
3. Проверить production DB: `itpearls`, schema `public`, owner `cuba`.
4. Проверить active connections.
5. Проверить долгие транзакции и prepared transactions.
6. Проверить свободное место, inodes, WAL и backup filesystem.
7. Проверить состояние приложения и datasource.
8. Найти и проверить file storage.
9. Зафиксировать production configuration: systemd/Tomcat/JNDI/local.app.properties без секретов.
10. Зафиксировать Git commit.
11. Проверить SHA-256 migration scripts.

## 6. Остановка записи

Финальный migration snapshot запрещено создавать при активной пользовательской записи.

Порядок:

1. Уведомить пользователей.
2. Включить maintenance mode или остановить HRM service.
3. Остановить scheduled jobs и integrations.
4. Проверить отсутствие активных пользовательских транзакций.
5. Проверить, что приложение больше не пишет в `itpearls`.
6. Зафиксировать время последней записи.
7. Повторить финальные baseline counts.

## 7. Финальный backup

Обязательные backup:

- database dump: `pg_dump -Fc`;
- globals: `pg_dumpall --globals-only` только как backup, не для автоматического restore;
- конфигурация приложения;
- systemd/Tomcat конфигурация;
- physical file storage.

Требования:

- SHA-256 для каждого файла;
- `pg_restore --list` для custom dump;
- backup manifest;
- права файлов не шире `0600` для чувствительных артефактов;
- backup вне Git и вне PostgreSQL data directory;
- команда без паролей в history/log;
- остановка процедуры при любом ненулевом exit code.

## 8. Создание target database

Target:

- database: `hunttech`;
- owner: `cuba`;
- schema: `public`;
- tablespace: `pg_default`;
- encoding/collation/ctype: должны совпадать с source или быть письменно утверждены;
- connection limit: `ТРЕБУЕТ ПОДТВЕРЖДЕНИЯ ПЕРЕД PRODUCTION`.

Запрещено использовать существующую production database как target. Перед созданием надо проверить, что `hunttech` отсутствует или является явно утвержденной disposable target. Если база существует, остановиться.

## 9. Развертывание target schema

Разрешен только контролируемый способ:

- использовать утвержденные schema scripts из Git;
- не полагаться на неконтролируемый `cuba.automaticDatabaseUpdate`;
- фиксировать примененные CUBA SQL update scripts и Liquibase changesets;
- сравнить target schema с локальной проверенной `hunttech`;
- остановиться при расхождении.

Production `cuba.automaticDatabaseUpdate=true` является открытым риском и должен быть отключен или контролируемо обработан до запуска приложения на target.

## 10. Миграция данных

Порядок групп:

1. системные справочники;
2. независимые бизнес-сущности;
3. родительские сущности;
4. дочерние сущности;
5. link-таблицы;
6. security;
7. пользовательские настройки;
8. история и аудит;
9. `sys_file`;
10. `sys_scheduled_execution`;
11. остальные технические таблицы;
12. sequences.

Для каждой группы:

- копировать с сохранением UUID/PK;
- сохранять все значения, `NULL`, timestamps, soft-delete и version columns;
- constraints отключать только если это описано в script/mapping;
- после загрузки включить и validate constraints;
- журналировать counts, PK, checksums, errors;
- при ошибке остановиться, не продолжать следующую группу.

## 11. Карта преобразования

Machine-readable mapping:

`deployment/database-migration/config/table-migration-mapping.yaml`

Файл содержит 154 source-таблицы, target mapping, strategy, primary keys, dependencies, column mapping, transformations, count policy, checksum policy, exceptions и human approval flags.

Production counts в mapping не фиксируются заранее: они должны быть пересчитаны после `STOP_WRITES`.

## 12. Legacy-таблицы

Подтвержденные пустые legacy link-таблицы:

- `itpearls_job_candidate_position_link__u59616`;
- `itpearls_open_position_city_link__u70664`.

Обе имели 0 строк в production/local source и отсутствуют в target. Перед production решение должно быть повторно подтверждено после остановки записи. Это решение нельзя автоматически переносить на другие legacy-таблицы.

## 13. Поле temperature

Для `vacancy_prompt_template.temperature`:

- существующие значения переносятся без изменения;
- `NULL` не заменяется;
- database default и application default рассматриваются отдельно;
- локально проверено setting target default `0.7`;
- изменение default не должно обновлять существующие строки.

Production default `0.7` подтвержден, но schema decision должен быть утвержден перед production.

## 14. Security HRM

Обязательная проверка:

- `sec_user`, `sec_role`, `sec_user_role`, `sec_group`;
- `sec_permission`, `sec_constraint`, `sec_user_setting`, `sec_remember_me`;
- `ExtUser` через `sec_user.dtype = hunttech_ExtUser`;
- active/blocked users;
- password hashes сохраняются без пересоздания и не выводятся;
- SMTP/IMAP/POP3/API-key поля копируются, но не печатаются;
- ownership/grants/default privileges проверяются отдельно.

Контрольные production агрегаты: `sec_user=89`, active users `15`, inactive/blocked `71`, `sec_role=12`, `sec_user_role=178`, `sec_group=7`, `sec_permission=3980`, `sys_file=13458`, `sys_db_changelog=995`. Перед production они пересчитываются заново после остановки записи.

## 15. File storage

Нужно:

- найти фактический production path (`ТРЕБУЕТ ПОДТВЕРЖДЕНИЯ ПЕРЕД PRODUCTION`);
- остановить запись приложения;
- сделать backup file storage;
- рассчитать checksum/file manifest;
- скопировать в target storage;
- проверить количество файлов и объем;
- выявить missing files;
- сверить связи с `sys_file`;
- исключить запись тестовой системы в production storage.

Локальный тест выявил, что отсутствие файлов в локальном file storage вызывает FTS/file errors, хотя database migration может быть успешной.

## 16. Полная валидация

Сравнение только `COUNT(*)` недостаточно.

Проверить:

- количество таблиц и строк каждой таблицы;
- PK и distinct PK;
- duplicate PK/unique;
- FK и orphan records;
- check constraints и not-null;
- indexes, triggers, functions, views;
- sequences и next values;
- ownership и grants;
- security aggregates;
- `sys_file` links;
- deterministic checksums по утвержденным колонкам;
- mapping exceptions.

## 17. Тест приложения

Тестировать отдельный application instance:

- target DB;
- отдельный port/app home;
- outbound email disabled;
- Telegram disabled;
- integrations disabled;
- scheduled jobs disabled;
- automatic update disabled.

Smoke test:

- вход администратора;
- вход рекрутера;
- вакансии и карточка вакансии;
- кандидаты и карточка кандидата;
- поиск;
- файлы;
- permissions;
- AI prompt templates и user AI configuration без раскрытия ключей;
- создать, изменить и удалить только запись с префиксом `MIGRATION_TEST_`;
- проверить логи.

## 18. Cutover

Допуск:

- все validation зеленые;
- backup проверен;
- rollback готов;
- target app готов;
- file storage готов;
- критичных warnings нет;
- пользователи еще не допущены;
- решение человека получено.

Переключение:

1. сохранить старую datasource-конфигурацию;
2. заменить datasource на `hunttech`;
3. запустить приложение;
4. не открывать доступ пользователям до post-switch validation.

## 19. Post-cutover проверки

Проверить:

- login admin/recruiter;
- бизнес smoke test;
- application logs и database logs;
- active connections;
- scheduled tasks;
- integrations;
- чтение файлов;
- права пользователей;
- создание и сохранение новой тестовой записи;
- отсутствие записи в старую базу.

## 20. Rollback

### До открытия записи пользователям

1. Остановить новую систему.
2. Вернуть старый datasource.
3. Запустить старую систему.
4. Выполнить smoke test.
5. Сохранить target DB и логи для анализа.

### После открытия записи пользователям

Простой rollback запрещен без учета новых данных.

Нужно:

- остановить запись;
- определить новые и измененные строки;
- сформировать delta;
- решить, переносить delta обратно или сохранять новую систему;
- получить решение руководителя миграции;
- не уничтожать ни одну из баз.

## 21. Критерии успеха

- Source `itpearls` не изменена.
- Target `hunttech` создана отдельно.
- Все 154 source-таблицы учтены mapping-ом.
- Все counts/checksums/PK/FK/security/file checks пройдены.
- No invalid constraints.
- App smoke test успешен.
- Пользователи допущены только после approval.
- Backup, manifest и rollback artifacts сохранены.

## 22. Критерии немедленной остановки

- checksum backup не совпал;
- backup не читается;
- restore имеет error;
- row counts расходятся без утвержденного exception;
- FK невалидны;
- security отличается;
- отсутствуют критичные файлы;
- приложение запускает неожиданный update;
- недостаточно места;
- запись в старую базу после snapshot;
- одновременная запись в две базы;
- команда указывает не ту database;
- обнаружено влияние на другие базы кластера.

## 23. Итоговый отчёт

Итоговый отчет должен содержать:

- версии PostgreSQL и утилит;
- Git commit;
- backup paths без секретов;
- checksums;
- durations;
- row counts/checksums;
- FK/security/file validation;
- smoke test result;
- cutover timestamp;
- rollback readiness;
- список warnings;
- решение go/no-go;
- подписи руководителя миграции, DBA, ответственного за данные и представителя пользователей.

## 24. Команды

Команды должны быть параметризованы и не содержать секретов.

Примеры:

```bash
pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$SOURCE_DB" \
  --format=custom --verbose --no-password \
  --file="$BACKUP_DIR/${SOURCE_DB}_${TS}.dump"

pg_restore --list "$BACKUP_DIR/${SOURCE_DB}_${TS}.dump" \
  > "$BACKUP_DIR/${SOURCE_DB}_${TS}.restore-list.txt"

./deployment/database-migration/backup/backup-production.sh
./deployment/database-migration/backup/verify-backup.sh
./deployment/database-migration/migration/00-preflight.sh
./deployment/database-migration/migration/40-run-test-prefix-migration.sh
./deployment/database-migration/validation/compare-production-and-restore.sh
```

Destructive commands требуют exact object name, dry-run/plan output и human approval.

## 25. Чек-листы

Для каждого пункта использовать поля:

| Пункт | Статус | Время | Исполнитель | Результат | Лог | Комментарий |
|---|---|---|---|---|---|---|
| Pre-migration | TODO | | | | | |
| Backup | TODO | | | | | |
| Migration | TODO | | | | | |
| Validation | TODO | | | | | |
| Cutover | TODO | | | | | |
| Rollback readiness | TODO | | | | | |
| Post-migration | TODO | | | | | |

Базовые чек-листы находятся в:

- `deployment/database-migration/validation/pre-migration-checklist.md`;
- `deployment/database-migration/validation/post-migration-checklist.md`;
- `deployment/database-migration/validation/application-smoke-test.md`.
