# Объединение исправлений и локальный deploy — 2026-09-21

## Состав релиза

- `0ad233c31` — восстановление дашбордов рекрутера: view `recruiter-dashboard-iteraction-list-view` добавлен в `views.xml`, Kanban и воронка читают признаки этапов без обращения к незагруженным атрибутам.
- `a9bd61b9c` — безопасная Liquibase-миграция функции `CANDIDATE_VACANCY_MATCH_ANALYZE` на корпоративный DeepSeek с rollback-скриптом и контрактной проверкой.
- `9bb9cdd3d` — обновлённый экран подбора открытых вакансий, прогресс выполнения, обогащение навыков, сервисный контракт и семь синхронных SCSS-тем.
- `cb73e7453` — merge обеих линий без потери файлов. Конфликтовал только `build.gradle`: сохранена более новая версия `0.687`.

## Проверки перед deploy

- Профильные тесты: `RecruiterDashboardsContractTest`, `ScreenViewIntegrityTest`, `CandidateVacancyMatchContractTest`, `CandidateSkillEnrichmentServiceTest`, `CandidateVacancyMatchProviderMigrationTest`, `CandidateVacancyMatchProgressTest` — `BUILD SUCCESSFUL`.
- `:app-web:buildScssThemes --no-daemon --stacktrace` — `BUILD SUCCESSFUL`.
- `clean assemble --no-daemon --stacktrace` — `BUILD SUCCESSFUL`.
- `git diff --check` для обеих исходных веток и объединённого результата — без ошибок.
- OCR не запускался: автоматический контроль среды запретил передачу внутреннего diff внешнему провайдеру без указания конкретного получателя и точного состава данных.

## Локальный deploy

- Локальная PostgreSQL доступна; `updateDb` выполнился успешно.
- Штатный `scripts/start-app.sh --branch <worktree>` применён для worktree Antigravity, который совпадает с опубликованным `master` по SHA.
- Общий Gradle cache был недоступен для `fileHashes.lock`; deploy выполнен с ранее прогретым локальным `GRADLE_USER_HOME=/private/tmp/hrm-gradle-home`.
- После запуска Tomcat: `GET /hrm/` — HTTP 200; widgetset `AppWidgetSet.nocache.js` — HTTP 200.
- `hrm` и `hrm-core` успешно развернуты, что подтверждено строками `Deployment ... has finished` в `catalina.out`.

## Наблюдения локальной среды

- В логах есть предупреждения `sysctlbyname` от `StatisticsAccumulator`, не влияющие на HTTP-доступность.
- Фоновый Telegram bot получает 409 из-за другого активного long-polling клиента.
- Локальная конфигурация DeepSeek содержит недействительный пользовательский ключ и нерасшифровываемый corporate credential. Это состояние локальных данных/секретов; код и миграция не изменялись для обхода этой ошибки.

## Git

- Локальный и удалённый `master` на момент deploy: `cb73e74533c6c149e01df3857ffabe2a86c21d91`.
- В master сохранены оба исправления и все документы, тесты, миграции, сервисные и UI-изменения исходных веток.
