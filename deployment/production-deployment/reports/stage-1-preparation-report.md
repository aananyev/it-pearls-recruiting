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

Read-only SSH-аудит выполнен через `root@hr.hunttech.ru` после уточнения SSH-пользователя. Остановка Tomcat была выполнена вручную оператором, не Codex.

Фактические параметры:

- host: `hr.hunttech.ru`;
- SSH user for audit: `root`;
- systemd service: `tomcat9`;
- service user: `tomcat`;
- service unit: `/usr/lib/systemd/system/tomcat9.service`;
- systemd override: `/etc/systemd/system/tomcat9.service.d/override.conf`;
- `CATALINA_HOME`: `/usr/share/tomcat9`;
- `CATALINA_BASE`: `/var/lib/tomcat9`;
- `CATALINA_TMPDIR`: `/tmp`;
- Java reported by service audit: OpenJDK `21.0.10`;
- Tomcat version script reports JVM: `11.0.30+7-post-Ubuntu-1ubuntu124.04`;
- Tomcat: Apache Tomcat `9.0.70` Ubuntu package;
- OS: Linux `6.8.0-111-generic`, amd64;
- webapps directory: `/var/lib/tomcat9/webapps`;
- old web app: `/var/lib/tomcat9/webapps/app`;
- old core app: `/var/lib/tomcat9/webapps/app-core`;
- old web WAR: `/var/lib/tomcat9/webapps/app.war`;
- old core WAR: `/var/lib/tomcat9/webapps/app-core.war`;
- old WAR backups present: `app.war.old`, `app-core.war.old`;
- application home: `/opt/app_home`;
- file storage path: `/opt/app_home/fileStorage`;
- logs directory observed: `/opt/app_home/logs`;
- nginx: inactive;
- Apache/httpd: inactive;
- firewall: active, allows `22/tcp`, `5432/tcp`, `8080`, `8082`, `7777`, `5201`.

Фактический статус:

- `tomcat9` is `inactive (dead)`;
- stopped at `2026-07-12 15:04:30 MSK`;
- stop was performed manually by the operator;
- no listener on port `8080`;
- PostgreSQL is listening on `5432`;
- `/app` and `/hrm` are not reachable from external HTTP check while Tomcat is stopped.

Context and datasource facts:

- `/etc/tomcat9/Catalina/localhost/app.xml` points to database `itpearls` through `jdbc/CubaDS`;
- `/etc/tomcat9/Catalina/localhost/app-core.xml` points to database `itpearls` through `jdbc/CubaDS`;
- `/var/lib/tomcat9/webapps/app-core/META-INF/context.xml` points to database `itpearls`;
- credentials were redacted and must not be stored in Git;
- `/var/lib/tomcat9/webapps/app-core/WEB-INF/local.app.properties` contains `cuba.automaticDatabaseUpdate = true`;
- `/var/lib/tomcat9/webapps/app/WEB-INF/local.app.properties` contains `cuba.automaticDatabaseUpdate = true`;
- old `/app` connection URL list points to `http://localhost:8080/app-core`.

Critical note: old deployed app has `automaticDatabaseUpdate=true`. It is currently safe only because Tomcat is stopped. The new `/hrm` first production start must keep `cuba.automaticDatabaseUpdate=false`, and this must be verified in logs.

## 7. HTTP read-only checks

Read-only HTTP checks выполнены без изменения production:

```bash
curl -I --max-time 10 http://hr.hunttech.ru:8080/app/ || true
curl -I --max-time 10 http://hr.hunttech.ru:8080/hrm/ || true
```

Результат на момент проверки:

- `/app`: подключение к `hr.hunttech.ru:8080` не установлено;
- `/hrm`: подключение к `hr.hunttech.ru:8080` не установлено.

SSH/systemd audit confirmed that this is because `tomcat9` is stopped.

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

- Tomcat был остановлен вручную оператором до обновления этого отчета; Codex остановку не выполнял.
- HTTP 8080 недоступен извне, потому что `tomcat9` сейчас `inactive`.
- Старое deployed-приложение содержит `cuba.automaticDatabaseUpdate=true`; перед запуском новой версии `/hrm` обязательно проверить, что у `/hrm` значение `false`.
- Старый `deploy-prod.sh` не соответствует staged workflow: он предусматривает остановку Tomcat и updateDb, поэтому не должен использоваться для этой migration без отдельной ревизии.
- В рабочем дереве есть посторонние изменения вне `deployment/production-deployment`; они не относятся к этому этапу.
- В проекте есть pre-existing backup/properties файлы с секретами вне нового production-deployment каталога; они не добавлялись в новые документы.

## 13. Блокирующие вопросы

Перед production-действиями обязательно закрыть:

1. Получить отдельное задание на ЭТАП 2 перед финальной миграцией.
2. Перед ЭТАПОМ 2 повторно проверить, что `tomcat9` остается остановленным и пользователи не пишут в `itpearls`.
3. Подтвердить production JNDI datasource для будущей базы `hunttech` без сохранения секретов в Git.
4. Подтвердить target file storage isolation для `/hrm`.
5. Утвердить pre-cutover config for disabled scheduling/email/Telegram/external writes.
6. Подтвердить exact WAR filenames после `buildWar` на ЭТАПЕ 3.
7. При старте `/hrm` проверить, что `cuba.automaticDatabaseUpdate=false` и приложение не меняет schema.

## 14. Выполненные команды без секретов

```bash
mkdir -p deployment/production-deployment/scripts deployment/production-deployment/config deployment/production-deployment/runbooks deployment/production-deployment/validation deployment/production-deployment/reports
curl -I --max-time 10 http://hr.hunttech.ru:8080/app/ || true
curl -I --max-time 10 http://hr.hunttech.ru:8080/hrm/ || true
ssh -o BatchMode=yes -o ConnectTimeout=8 hr.hunttech.ru '<read-only tomcat audit>'
ssh -o BatchMode=yes -o ConnectTimeout=10 root@hr.hunttech.ru '<read-only tomcat/systemd/webapps audit>'
ssh -o BatchMode=yes -o ConnectTimeout=10 root@hr.hunttech.ru '<read-only context/datasource audit with redaction>'
GRADLE_USER_HOME=/private/tmp/hunttech-gradle-home ./gradlew tasks --all --console=plain
deployment/production-deployment/validation/validate-deployment.sh
deployment/production-deployment/scripts/verify-environment.sh --dry-run
```

## 15. Production impact

Production impact from Codex: none; only read-only SSH/HTTP audit was performed.

Operator production impact: Tomcat was manually stopped by the operator before this report update.

Production database impact: none.

Other PostgreSQL databases on `hr.hunttech.ru`: not touched.

Tomcat production impact from Codex: no change performed. Observed state: `tomcat9 inactive`.

## 16. Вердикт

`ПОДГОТОВКА ЗАВЕРШЕНА — МОЖНО ПЕРЕХОДИТЬ К ФИНАЛЬНОЙ МИГРАЦИИ`

Условие: переход к ЭТАПУ 2 разрешен только по отдельному заданию. На момент отчета `tomcat9` остановлен оператором, `/app` не принимает HTTP-запросы, старые артефакты сохранены, production-база и другие базы не изменялись Codex.
