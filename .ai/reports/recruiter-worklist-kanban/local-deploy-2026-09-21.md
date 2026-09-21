# Локальный deploy: рабочий список рекрутера и Kanban

**Проект:** HRM HuntTech
**Дата:** 2026-09-21
**Ветка:** `agent/recruiter-worklist-kanban-label`
**Основа:** `origin/master` (`9d89522112010a41845e941c3da0b864fc5ab3ad`)
**Проверенный кодовый SHA:** `68e8d9abbf992d970c2a74d6a79f9d1be218c820`
**Статус:** `WAITING_FOR_HERMES`

## Причина пустого списка

`hunttech_RotatingCandidate.browse` открывал период «За 7 дней» и фильтр «Только мои». Read-only проверка локальной БД для login `eliberman` показала 0 записей за 7 дней и 54 записи за 30 дней; те же 54 строки удовлетворяют фильтру автора списка. Поэтому экран оставался пустым из-за слишком узкого периода по умолчанию.

Исправление устанавливает существующую опцию «За месяц» стартовым значением (`daysBetween = 30`); запрос и остальные фильтры не менялись. Отдельно название Dashboard Add-on и виджета сокращено до «Kanban».

## Проверки и deploy

Первый запуск остановился до сборки ветки: sandbox запретил запись в lock-файл стандартного `~/.gradle`; deploy был повторен с уже прогретым временным Gradle-кэшем в `/private/tmp`. Повторный запуск прошёл полностью.

- `ScreenViewIntegrityTest`: 8/8 PASS.
- `RecruiterDashboardsContractTest`: 5/5 PASS.
- `clean assemble --no-daemon --stacktrace`: `BUILD SUCCESSFUL`.
- Штатный `scripts/start-app.sh --branch <worktree>`: ветка и отсутствие новых Liquibase-миграций подтверждены; PostgreSQL готов; `updateDb` и чистый deploy успешны.
- Tomcat запущен; `http://localhost:8080/hrm/` ответил HTTP 200.
- SHA256 собранного и развернутого `app-web-0.689-SNAPSHOT.jar` совпал: `05fcc5fd84b086cb5e4f082f785ce34eefd7eae5a6e71a62272291613b1dc649`.
- Ресурс dashboard в deployed JAR содержит подписи `Kanban`; старое имя отсутствует.
- В startup log остаётся повторяющееся сообщение Log4j2 `could not find a logging implementation` и системные предупреждения `sysctlbyname failed`; приложение при этом запустилось и отдало HTTP 200. Проверить состояние логов отдельно при верификации Hermes.
- Визуальный smoke через браузер не выполнен: доступ к локальному браузеру блокирует административная политика. Нужна проверка экрана через разрешённый Hermes CDP smoke.
- Production не затрагивался.

В `origin/master`, от которого создана ветка, уже присутствуют ранее объединённые исправления дашборда (`0ad233c31`) и подбора вакансии (`9bb9cdd3d`); этот PR создан поверх них и их не отменяет.
