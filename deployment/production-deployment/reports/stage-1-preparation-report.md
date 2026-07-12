# Stage 1 preparation report: HRM HuntTech production deployment

Дата: 2026-07-12

## 1. Цель этапа

Подготовить production-серверный deployment workflow для перехода со старого приложения `http://hr.hunttech.ru:8080/app` и базы `itpearls` на новое приложение `http://hr.hunttech.ru:8080/hrm` и базу `hunttech`.

Этап 1 не выполняет миграцию и не меняет production.

## 2. Подтверждение ограничений

В рамках этапа 1:

- `/app` не останавливался;
- Tomcat не перезапускался;
- production datasource не изменялся;
- база `itpearls` не изменялась;
- база `hunttech` на production не создавалась;
- WAR-файлы на production не заменялись;
- Liquibase/updateDb/automatic database update не запускались;
- старые артефакты не удалялись;
- `git push` не выполнялся.

## 3. Изученные материалы

Изучены материалы:

- `deployment/database-migration/`;
- `deployment/database-migration/runbooks/HRM-HuntTech-production-migration-human-runbook.md`;
- `deployment/database-migration/runbooks/HRM-HuntTech-production-migration-AI-algorithm.md`;
- `deployment/database-migration/config/migration-manifest.yaml`;
- `deployment/database-migration/config/table-migration-mapping.yaml`;
- `deployment/database-migration/reports/second-stage-summary.md`;
- `deployment/database-migration/reports/test-restore-report.md`;
- `deployment/database-migration/reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md`;
- `deployment/database-migration/reports/local-dashboard-widgets-fix-2026-07-12.md`;
- `deployment/database-migration/reports/migration-algorithm-documentation-summary.md`.

Изучены проектные файлы:

- `build.gradle`;
- `settings.gradle`;
- `modules/core/src/com/company/hunttech/app.properties`;
- `modules/core/src/app.properties`;
- `modules/web/src/com/company/hunttech/web-app.properties`;
- `modules/core/web/META-INF/war-context.xml`;
- `modules/web/web/META-INF/context.xml`;
- `modules/core/web/WEB-INF/web.xml`;
- `modules/web/web/WEB-INF/web.xml`;
- `deploy_shared.conf`;
- `deploy-prod.sh`;
- `etc/tomcat-setenv.sh`.

## 4. Фактическая архитектура приложения

- CUBA Platform: `7.3-SNAPSHOT`.
- Gradle: `5.6.4`, подтвержден через Gradle wrapper.
- Root project: `hunttech_recruiting`.
- Module prefix: `app`.
- Modules: `app-global`, `app-core`, `app-gui`, `app-web`, `app-web-toolkit`.
- New web context: `hrm`.
- New core context: `hrm-core`.
- `buildWar.singleWar=false`.
- Правильный deployment format: CUBA two-WAR deployment, `/hrm` + `/hrm-core`.
- Build task: `buildWar`.
- Local DB tasks существуют: `app-core:createDb`, `app-core:updateDb`.
- Local deployment tasks существуют: `app-core:deploy`, `app-web:deploy`, `restart`, `start`, `stop`.

## 5. Database configuration новой версии

Подтверждено по проекту:

- target DB в Gradle local tasks: `hunttech`;
- JNDI datasource в `war-context.xml`: `jdbc/CubaDS`;
- local JNDI URL в шаблоне проекта: `jdbc:postgresql://127.0.0.1:5432/hunttech`;
- `buildWar` устанавливает `cuba.automaticDatabaseUpdate=false`;
- web connection URL: `http://localhost:8080/hrm-core`;
- core web context: `hrm-core`;
- web context: `hrm`.

Предупреждение: локальные properties содержат включенные `cuba.schedulingActive=true`, email settings и Telegram placeholders. Для production pre-cutover подготовлены `.example` overrides с отключенными scheduled tasks, email, Telegram и external writes.

## 6. Production Tomcat audit

По ранее зафиксированному production-аудиту:

- host: `hr.hunttech.ru`;
- systemd service: `tomcat9`;
- service user: `tomcat`;
- webapps directory: `/var/lib/tomcat9/webapps`;
- old web app: `/var/lib/tomcat9/webapps/app`;
- old core app: `/var/lib/tomcat9/webapps/app-core`;
- application home: `/opt/app_home`.

Текущий read-only SSH-аудит не выполнен из-за отсутствия доступа:

```text
alekseyananyev@hr.hunttech.ru: Permission denied (publickey,password).
```

Это блокирует переход к production-действиям.

## 7. HTTP read-only checks

Read-only HTTP checks выполнены без изменения production:

```bash
curl -I --max-time 10 http://hr.hunttech.ru:8080/app/ || true
curl -I --max-time 10 http://hr.hunttech.ru:8080/hrm/ || true
```

Результат на момент проверки:

- `/app`: подключение к `hr.hunttech.ru:8080` не установлено;
- `/hrm`: подключение к `hr.hunttech.ru:8080` не установлено.

Это не доказывает остановку Tomcat изнутри сервера. Требуется SSH/systemd аудит оператором.

## 8. Остаточные ссылки и классификация

Выявленные классы ссылок:

- `modulePrefix = app` в `settings.gradle` и `build.gradle`: оставить, это Gradle module prefix и legacy module name, не context path.
- `webContextName = hrm`: корректно для новой версии.
- `coreContextName = hrm-core`: корректно для новой версии.
- `/app`, `app-core` в старых deployment/runbook контекстах: оставить для rollback и migration compatibility.
- `itpearls` в migration docs: относится к source database и истории миграции.
- `hunttech_*` entity names and table prefixes: корректно для новой модели.
- `deploy-prod.sh` содержит старую production-deploy логику с остановкой Tomcat/updateDb: не использовать для staged migration без ревизии.
- `modules/core/src/app.properties.backup` содержит секреты и не должен попадать в Git.

Массовая замена не выполнялась.

## 9. Подготовленные файлы

Создан каталог `deployment/production-deployment/`.

Config:

- `config/deployment-manifest.yaml`;
- `config/hrm-production.properties.example`;
- `config/hrm-pre-cutover.properties.example`.

Scripts:

- `scripts/verify-environment.sh`;
- `scripts/build-production-artifact.sh`;
- `scripts/verify-artifact.sh`;
- `scripts/backup-current-deployment.sh`;
- `scripts/deploy-hrm-disabled.sh`;
- `scripts/configure-hrm.sh`;
- `scripts/start-hrm-restricted.sh`;
- `scripts/stop-hrm.sh`;
- `scripts/start-old-app.sh`;
- `scripts/stop-old-app.sh`;
- `scripts/switch-to-hrm.sh`;
- `scripts/rollback-to-app.sh`;
- `scripts/verify-tomcat-deployment.sh`;
- `scripts/collect-logs.sh`;
- `scripts/cleanup-failed-deployment.sh`;
- `scripts/lib.sh`.

Runbooks:

- `runbooks/production-deployment-runbook.md`;
- `runbooks/production-deployment-rollback.md`;
- `runbooks/tomcat-context-path-migration.md`.

Validation:

- `validation/validate-deployment.sh`;
- `validation/validate-context-path.sh`;
- `validation/validate-application-health.sh`;
- `validation/validate-integrations-disabled.sh`.

Reports:

- `reports/stage-1-preparation-report.md`;
- `reports/.gitignore`.

## 10. Guard rails in scripts

Скрипты:

- используют `set -Eeuo pipefail`;
- имеют error trap;
- поддерживают `--dry-run` там, где применимо;
- требуют `HRM_DEPLOY_APPROVED=yes` для mutating actions;
- запрещают wildcard/root path cleanup;
- не содержат production credentials;
- не удаляют `/app`;
- не удаляют old WAR;
- не изменяют old database;
- не выполняют `git push`;
- разделяют `/app` and `/hrm`.

## 11. Проверки

Выполнено:

```bash
deployment/production-deployment/validation/validate-deployment.sh
deployment/production-deployment/scripts/verify-environment.sh --dry-run
GRADLE_USER_HOME=/private/tmp/hunttech-gradle-home ./gradlew tasks --all --console=plain
```

Результаты:

- bash syntax validation: успешно;
- deployment artifact set validation: успешно;
- Gradle tasks: `BUILD SUCCESSFUL`;
- `buildWar` task подтвержден;
- `app-core:createDb` and `app-core:updateDb` подтверждены;
- local Tomcat tasks подтверждены.

## 12. Предупреждения

- SSH read-only Tomcat audit заблокирован отсутствием доступа.
- HTTP 8080 недоступен извне на момент проверки; причина не установлена.
- Старый `deploy-prod.sh` не соответствует staged workflow: он предусматривает остановку Tomcat и updateDb, поэтому не должен использоваться для этой migration без отдельной ревизии.
- В рабочем дереве есть посторонние изменения вне `deployment/production-deployment`; они не относятся к этому этапу.
- В проекте есть pre-existing backup/properties файлы с секретами вне нового production-deployment каталога; они не добавлялись в новые документы.

## 13. Блокирующие вопросы

Перед production-действиями обязательно закрыть:

1. Предоставить оператору рабочий SSH-доступ для read-only Tomcat audit.
2. Подтвердить фактические `CATALINA_HOME`, `CATALINA_BASE`, Java version, Tomcat version, JVM options и deployed apps.
3. Подтвердить причину недоступности `hr.hunttech.ru:8080` извне.
4. Подтвердить exact WAR filenames после `buildWar`.
5. Подтвердить production JNDI datasource для `hunttech` без сохранения секретов в Git.
6. Подтвердить production file storage path and isolation.
7. Утвердить pre-cutover config for disabled scheduling/email/Telegram/external writes.
8. Подтвердить human approval gate для остановки `/app` перед stage 2.

## 14. Выполненные команды без секретов

```bash
mkdir -p deployment/production-deployment/scripts deployment/production-deployment/config deployment/production-deployment/runbooks deployment/production-deployment/validation deployment/production-deployment/reports
curl -I --max-time 10 http://hr.hunttech.ru:8080/app/ || true
curl -I --max-time 10 http://hr.hunttech.ru:8080/hrm/ || true
ssh -o BatchMode=yes -o ConnectTimeout=8 hr.hunttech.ru '<read-only tomcat audit>'
GRADLE_USER_HOME=/private/tmp/hunttech-gradle-home ./gradlew tasks --all --console=plain
deployment/production-deployment/validation/validate-deployment.sh
deployment/production-deployment/scripts/verify-environment.sh --dry-run
```

## 15. Production impact

Production impact: none from this Codex run.

Production database impact: none.

Other PostgreSQL databases on `hr.hunttech.ru`: not touched.

Tomcat production impact: no change performed.

## 16. Вердикт

`ПОДГОТОВКА НЕ ЗАВЕРШЕНА — PRODUCTION-РАБОТЫ ЗАПРЕЩЕНЫ`

Причина: не завершен обязательный read-only production Tomcat audit по SSH, а порт `8080` сейчас не подтвержден как доступный извне. Переход к финальной миграции, остановке `/app`, созданию `hunttech` и deployment `/hrm` запрещен до закрытия блокирующих вопросов.
