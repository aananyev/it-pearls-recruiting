# Handoff: восстановление подбора вакансий через DeepSeek

Дата: 2026-09-21
Ветка: `agent/antigravity-dev`
Коммит: `a9bd61b9c fix(ai): восстановить подбор вакансий через DeepSeek`

## Выполнено

- Добавлена зарегистрированная миграция `260921-1-routeCandidateVacancyMatchToDeepSeek.sql` и rollback-скрипт.
- Функция `CANDIDATE_VACANCY_MATCH_ANALYZE` на локальной PostgreSQL направлена в конфигурацию `deepseek` с моделью `deepseek-v4-flash`.
- Добавлен тест `CandidateVacancyMatchProviderMigrationTest`, проверяющий контракт миграции и rollback.
- Версия приложения обновлена pre-commit hook до `0.685`.
- Изменения отправлены в `origin/agent/antigravity-dev`.

## Локальная проверка и деплой

- Миграция применена штатной задачей `:app-core:updateDb`.
- Выполнен локальный deploy и restart общего Tomcat.
- `http://127.0.0.1:8080/hrm/` и widgetset отвечают `200`.
- В `sys_db_changelog` зарегистрирован путь `70-hunttech_recruiting/update/postgres/26/260921-1-routeCandidateVacancyMatchToDeepSeek.sql`.
- В свежем журнале Tomcat нет `PSQLException`, `Unfetched Attribute` и `LazyInitialization`.

## Границы
