# Stage 3 deployment report

Дата: 2026-07-12

## Цель

Продолжить production deployment HRM HuntTech после завершенной миграции базы `itpearls -> hunttech`.

## Business waiver

Перед стартом этапа 3 владелец процесса подтвердил:

> Данные прошлых периодов безвозвратно потеряны. Делаем этап 3 и продолжаем миграцию.

Это снимает stop gate этапа 2.1 только как бизнес-принятие потери доступности historical file storage объектов. База `hunttech` и метаданные `sys_file` не очищаются.

## Защитные условия

- Старую базу `itpearls` не изменять.
- Другие базы PostgreSQL на `hr.hunttech.ru` не трогать.
- Не запускать Liquibase/updateDb.
- Не включать `cuba.automaticDatabaseUpdate`.
- Не открывать доступ пользователям без отдельной фразы: `РАЗРЕШАЮ ОТКРЫТЬ HRM ПОЛЬЗОВАТЕЛЯМ`.

## Начальное состояние

- `tomcat9` был остановлен до этапа 3.
- База `hunttech` уже создана на этапе 2 и прошла DB validation.
- File storage имеет известный accepted-loss дефект: 633 missing files.

## Журнал выполнения

### 1. Waiver зафиксирован

Обновлены:

- `deployment/database-migration/reports/missing-files-decisions-register.md`;
- `deployment/database-migration/config/missing-files-decision-register.yaml`;
- `deployment/database-migration/config/missing-files-recovery-manifest.yaml`.

### 2. Production build

Команда сборки:

```bash
GRADLE_USER_HOME=/private/tmp/hunttech-gradle-home HRM_DEPLOY_APPROVED=yes deployment/production-deployment/scripts/build-production-artifact.sh --execute
```

Результат:

- Gradle build: successful;
- generated WAR:
  - `build/distributions/war/hrm.war`;
  - `build/distributions/war/hrm-core.war`.

SHA-256:

```text
1eab0dafdbf72a00a3e84eaa3f28e4b2dfcd7aaf3f3c99d7333616ec18021b17  hrm.war
10cd0016e07f4a00c8dd32d6f1abbd0cdd4bfb7303002c40352852e23e2c1b5f  hrm-core.war
```

Artifact checks:

- `hrm-core.war` contains `cuba.automaticDatabaseUpdate = false`;
- `hrm.war` contains `cuba.automaticDatabaseUpdate = false`;
- web connection URL points to `http://localhost:8080/hrm-core`;
- core JNDI resource points to database `hunttech`.

### 3. Deployment while Tomcat stopped

Server-side deployment directory:

`/var/backups/hunttech-hrm/20260712-233414-stage3-deployment`

Actions completed:

- confirmed host `hr.hunttech.ru`;
- confirmed `tomcat9` was `inactive`;
- confirmed databases `itpearls` and `hunttech` exist;
- saved current Tomcat contexts and webapps into the stage 3 backup directory;
- uploaded `hrm.war` and `hrm-core.war` to `/tmp`;
- verified remote SHA-256 of uploaded WAR files;
- installed:
  - `/var/lib/tomcat9/webapps/hrm.war`;
  - `/var/lib/tomcat9/webapps/hrm-core.war`;
- created runtime overrides:
  - `/opt/app_home/hrm/conf/local.app.properties`;
  - `/opt/app_home/hrm-core/conf/local.app.properties`;
- created Tomcat contexts:
  - `/etc/tomcat9/Catalina/localhost/hrm.xml`;
  - `/etc/tomcat9/Catalina/localhost/hrm-core.xml`.

### 4. Server validation after deploy

Tomcat remained stopped:

```text
tomcat9: inactive
```

Deployed WAR ownership and permissions:

```text
tomcat|tomcat|644|155319878|/var/lib/tomcat9/webapps/hrm.war
tomcat|tomcat|644|169667058|/var/lib/tomcat9/webapps/hrm-core.war
```

Tomcat context validation:

```text
/etc/tomcat9/Catalina/localhost/hrm.xml       root|tomcat|640
/etc/tomcat9/Catalina/localhost/hrm-core.xml  root|tomcat|640
hrm-core context contains hunttech: 1
hrm-core context contains itpearls: 0
```

Runtime override validation:

```text
cuba.automaticDatabaseUpdate=false
cuba.schedulingActive=false
cuba.email.smtpHost=
hunttech.telegram.botToken=
cuba.fileStorageDir=/opt/app_home/fileStorage
```

Database sizes after deploy:

```text
hunttech|6382 MB
itpearls|6346 MB
```

Other PostgreSQL databases listed for safety:

```text
alan
cuba
hunttech
hunttech_protocols
itpearls
postgres
wp_database
```

No commands were executed against databases other than the approved HRM source/target checks.

## Current stop point

The new `/hrm` and `/hrm-core` WARs are deployed on disk, but Tomcat is still stopped.

Tomcat was not started because starting the service exposes web contexts on public port `8080`. The original stage 1 runbook requires a separate human phrase before opening user access:

`РАЗРЕШАЮ ОТКРЫТЬ HRM ПОЛЬЗОВАТЕЛЯМ`

Before start/cutover, the operator must choose one of these controlled actions:

1. Disable old `/app` and `/app-core` artifacts on disk, then start Tomcat with only `/hrm`/`/hrm-core`.
2. Start Tomcat with both old and new contexts only if there is an external access restriction preventing users from writing to `/app`.

Recommended next action: disable old `/app` deployment artifacts without deleting them, then start Tomcat and validate `/hrm`.

## Current verdict

`STAGE 3 DEPLOYMENT PREPARED — TOMCAT START/CUTOVER REQUIRES EXPLICIT OPENING APPROVAL`
