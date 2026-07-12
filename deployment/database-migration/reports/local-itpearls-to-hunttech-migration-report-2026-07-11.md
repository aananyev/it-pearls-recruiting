# Отчет о локальной миграции `itpearls` -> `hunttech`

Дата: 2026-07-11
Контур: локальный PostgreSQL и локальный Tomcat проекта `/Users/alekseyananyev/StudioProjects/hunttech_recruiting`
Production-сервер `hr.hunttech.ru`: не изменялся

## 1. Цель

Отработать реальную локальную миграцию базы данных `itpearls` в новую локальную базу `hunttech` с учетом текущей логики проекта HuntTech HRM:

- сохранить все данные;
- создать новую целевую базу `hunttech`;
- переименовать legacy-объекты `itpearls_*` в `hunttech_*`;
- создать/сохранить новые таблицы, индексы, constraints и зависимости;
- проверить целостность данных, security-модель и запуск приложения.

## 2. Исходное состояние

Перед миграцией локальная база `hunttech` отсутствовала. Источником была локальная база `itpearls`.

Контрольные показатели источника:

| Метрика | Значение |
|---|---:|
| Размер `itpearls` | 6342 MB |
| Таблицы | 154 |
| Constraints | 409 |
| Indexes | 514 |
| `sec_user` | 89 |
| `sys_file` | 13458 |
| `sys_db_changelog` | 995 |
| Legacy link table `itpearls_job_candidate_position_link__u59616` | 0 строк |
| Legacy link table `itpearls_open_position_city_link__u70664` | 0 строк |
| Orphan-ссылки `itpearls_job_candidate.current_company_id` | 17 |

## 3. Выполненная миграция

Миграция выполнена локально скриптом:

`deployment/database-migration/migration/40-run-test-prefix-migration.sh`

Основной трансформационный сценарий:

`deployment/database-migration/migration/20-transform-restored-copy-to-hunttech.sql`

Выполненные действия:

1. Остановлен локальный Tomcat.
2. Проверено отсутствие целевой базы `hunttech`.
3. Создана локальная база `hunttech` на основе локальной `itpearls`.
4. Создана таблица журнала миграции `hunttech_migration_run_log`.
5. Создана таблица карантина `hunttech_migration_quarantine`.
6. Для 17 orphan-ссылок `current_company_id` созданы placeholder-записи в целевой таблице компаний.
7. Все 17 случаев зафиксированы в `hunttech_migration_quarantine`.
8. Пустые legacy link-таблицы удалены в целевой базе.
9. Таблицы, sequences, views/materialized views, индексы и constraints с namespace `itpearls` переименованы в `hunttech`.
10. `hunttech_vacancy_prompt_template.temperature` получил default `0.7`.
11. Security/UI-ссылки в `sec_permission`, `sec_user_setting`, `sec_filter`, `sec_presentation`, `sys_config` переведены с `itpearls` на `hunttech`.
12. `sec_user.dtype` переведен с `itpearls_ExtUser` на `hunttech_ExtUser`, чтобы CUBA ORM видел пользователей текущей модели.
13. Созданы дополнительные post-migration индексы текущей версии проекта.

## 4. Правило сохранения orphan-ссылок

В источнике обнаружено 17 кандидатов, у которых `current_company_id` ссылался на отсутствующие компании.

Чтобы не потерять данные и не изменять смысловые ссылки:

- значения `current_company_id` сохранены;
- в целевой таблице компаний созданы placeholder-записи с теми же UUID;
- каждый случай записан в `hunttech_migration_quarantine`;
- после миграции orphan-ссылок по `current_company_id` нет.

Это технически сохраняет ссылочную целостность, но перед production-миграцией требуется бизнес-решение: заменить placeholder-компании реальными компаниями, подтвердить допустимость placeholder-подхода или задать другое правило.

## 5. Проверки целостности

Результаты `deployment/database-migration/validation/validate-migration-target.sql`:

| Метрика | Значение |
|---|---:|
| Target database | `hunttech` |
| Legacy `itpearls_*` tables remaining | 0 |
| `hunttech_*` tables | 59 |
| `sec_user` | 89 |
| `sec_role` | 12 |
| `sec_user_role` | 178 |
| `sec_permission` | 3980 |
| `sys_file` | 13458 |
| `sys_db_changelog` | 995 |
| Invalid constraints | 0 |
| `hunttech_vacancy_prompt_template.temperature` default | 0.7 |
| Quarantine rows | 17 |
| Orphan `current_company_id` после миграции | 0 |
| Security references to `itpearls` | 0 |

Дополнительные структурные проверки:

| Метрика | Значение |
|---|---:|
| Таблицы всего | 154 |
| Constraints после миграции | 405 |
| Foreign keys | 245 |
| Invalid constraints | 0 |
| Indexes после миграции | 538 |
| Sequences | 4 |
| Index names containing `itpearls` | 0 |
| Constraint names containing `itpearls` | 0 |
| Table names containing `itpearls` | 0 |

Точное сравнение количества строк:

| Результат | Количество |
|---|---:|
| Совпавшие таблицы | 152 |
| Исключенные пустые legacy link-таблицы | 2 |
| Несовпадения | 0 |

Отдельное правило для `hunttech_company`: целевое количество строк ожидаемо больше на 17 из-за placeholder-компаний для сохранения FK.

## 6. Проверки AI-объектов и новой логики

| Проверка | Результат |
|---|---|
| `hunttech_user_ai_configuration` существует | да |
| `hunttech_vacancy_prompt_template` существует | да |
| `hunttech_open_position.raw_description` существует | да |
| `hunttech_vacancy_prompt_template.temperature` default | `0.7` |
| `idx_user_ai_configuration_user` | создан |
| `idx_vacancy_prompt_template_code` | создан |

## 7. Проверки security-модели

Критичные таблицы CUBA security сохранены:

| Таблица | Строк |
|---|---:|
| `sec_user` | 89 |
| `sec_role` | 12 |
| `sec_user_role` | 178 |
| `sec_permission` | 3980 |
| `sec_filter` | 159 |
| `sec_user_setting` | 1471 |

Важное исправление по совместимости с текущей моделью:

- до исправления все строки `sec_user.dtype` имели значение `itpearls_ExtUser`;
- CUBA не видел системного пользователя `anonymous` как актуальную сущность;
- после исправления `anonymous` и `admin` имеют `hunttech_ExtUser`;
- legacy `dtype` со значением `itpearls_ExtUser` не осталось.

## 8. Проверка запуска приложения

Локальный Tomcat запущен на новой базе `hunttech`.

Результаты:

| Проверка | Результат |
|---|---|
| `/hrm/` | HTTP 200 |
| `/hrm-core/remoting` прямой HEAD-запрос | HTTP 404, без прежнего падения `BeanFactory not initialized` |
| Web block `hrm` | `AppContext started` |
| Core block `hrm-core` | `AppContext started` |
| Anonymous session | успешно создана |
| Admin session при старте | успешно создана |

Обнаруженные неблокирующие проблемы локального запуска:

1. `cuba.tempDir` указывает на `${app.home}/fileStorage/temp`, а локальный `fileStorage` является ссылкой на `/opt/app_home/fileStorage`; для локального окружения это требует отдельной настройки прав/пути.
2. FTS-задача пытается индексировать файлы из `sys_file`, но часть файлов отсутствует в локальном файловом хранилище.
3. FTS падает на несовместимости `tika-parsers`/`pdfbox`: отсутствует ожидаемый метод `PDDocument.load(...)`.
4. Emailer в фоне встречает сообщения с `caption is null`.
5. CUBA сообщает о наличии unapplied update scripts; это не запускалось автоматически и требует отдельной сверки с `sys_db_changelog` перед production.

Эти проблемы не являются потерей данных при миграции, но должны быть разобраны до боевого переключения.

## 9. Размеры и диск

| Объект | Размер |
|---|---:|
| `itpearls` | 6342 MB |
| `hunttech` | 6378 MB |
| Свободно на диске после миграции | около 16 GiB |

Размер `hunttech` ожидаемо немного больше из-за дополнительных объектов, индексов, карантинной таблицы и 17 placeholder-компаний.

## 10. Измененные файлы

Обновлены:

- `deployment/database-migration/migration/20-transform-restored-copy-to-hunttech.sql`
- `deployment/database-migration/validation/validate-migration-target.sql`

Создан отчет:

- `deployment/database-migration/reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md`

## 11. Выполненные команды без секретов

Ключевые операции:

```bash
deploy/tomcat/bin/shutdown.sh
./deployment/database-migration/migration/40-run-test-prefix-migration.sh
psql -h 127.0.0.1 -p 5432 -U postgres -d hunttech -f deployment/database-migration/validation/validate-migration-target.sql
deploy/tomcat/bin/catalina.sh run
curl -I http://localhost:8080/hrm/
curl -I http://localhost:8080/hrm-core/remoting
```

Параметры с паролями не использовались и в отчет не записывались.

## 12. Production safety

Production-сервер `hr.hunttech.ru` и production-база не изменялись.

Не выполнялись на production:

- DDL;
- DML;
- миграция;
- переключение приложения;
- создание базы `hunttech`;
- изменение ролей, grants или ownership.

Вся работа выполнена локально.

## 13. Остаточные риски перед production

1. Требуется бизнес-решение по 17 placeholder-компаниям.
2. Нужно отдельно проверить production-файловое хранилище `sys_file`, чтобы FTS и вложения не ссылались на отсутствующие файлы.
3. Нужно согласовать, отключать ли фоновые задачи FTS/email на время миграции и первого запуска.
4. Нужно сверить CUBA update scripts и `sys_db_changelog`, чтобы приложение не пыталось применять старые/лишние update scripts.
5. Для production нужен отдельный полный backup, test restore и rollback window.
6. Свободное место на локальном диске после теста ограничено; для production нужен расчет пространства с запасом.

## 14. Вердикт

Локальная миграция `itpearls` -> `hunttech` выполнена успешно.

Данные сохранены, целостность FK восстановлена без удаления строк, security-модель сохранена и адаптирована к текущей модели `hunttech_ExtUser`, приложение локально стартует на базе `hunttech`.

Production-миграцию можно проектировать дальше, но запускать ее нельзя без решения по 17 карантинным company-ссылкам, проверки файлового хранилища, backup/test restore production и финального runbook переключения.
