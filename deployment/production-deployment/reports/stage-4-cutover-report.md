# Stage 4 cutover report

Дата: 2026-07-12

## Разрешение

Cutover выполнен после точной фразы:

`РАЗРЕШАЮ ОТКРЫТЬ HRM ПОЛЬЗОВАТЕЛЯМ`

## Цель

Отключить старый контекст `/app`, запустить Tomcat и открыть новый HRM HuntTech по адресу:

`http://hr.hunttech.ru:8080/hrm/`

## Preflight

- Host: `hr.hunttech.ru`.
- Tomcat before cutover: `inactive`.
- `hrm.war` and `hrm-core.war` already deployed.
- `hrm-core.xml` pointed to `hunttech`.
- `hrm-core.xml` did not contain `itpearls`.
- Databases present:
  - `itpearls`: `6346 MB`;
  - `hunttech`: `6382 MB`.

## Cutover backup

Server backup directory:

`/var/backups/hunttech-hrm/20260712-224608-stage4-cutover`

Saved before start:

- Tomcat contexts;
- old `/app` and `/app-core` artifacts;
- new `/hrm` and `/hrm-core` artifacts;
- database list.

## Old application disable

Old application was not deleted. It was disabled by moving artifacts out of active Tomcat deployment paths.

Initially, old artifacts were renamed inside `/var/lib/tomcat9/webapps`, but Tomcat auto-deployed renamed directories such as `app-core.disabled-*`. This was corrected immediately:

- Tomcat was stopped.
- All `app*` artifacts were moved out of `/var/lib/tomcat9/webapps`.
- Preserved location:

`/var/backups/hunttech-hrm/20260712-224608-stage4-cutover/disabled-old-artifacts-removed-from-webapps`

Final active HRM webapps:

```text
ROOT
host-manager
hrm
hrm-core
hrm-core.war
hrm.war
manager
```

`/app` is no longer deployed.

## Runtime safety fix

After first start, CUBA still loaded DB-stored app properties and attempted scheduled FTS/email processing. Tomcat was stopped and runtime safety overrides were applied:

- appended non-secret overrides to:
  - `/var/lib/tomcat9/webapps/hrm-core/WEB-INF/local.app.properties`;
  - `/var/lib/tomcat9/webapps/hrm/WEB-INF/local.app.properties`;
  - `/opt/app_home/local.app.properties`;
- updated only target database `hunttech.sys_config`:
  - `cuba.schedulingActive=false`;
  - `cuba.email.smtpHost=`;
  - `cuba.email.smtpUser=`;
  - `cuba.email.smtpPassword=`;
  - `hunttech.telegram.botName=`;
  - `hunttech.telegram.botToken=`.

Secret values were not printed into reports. Source database `itpearls` was not modified.

## Final validation

Tomcat:

```text
tomcat9: active
```

HTTP:

```text
GET/HEAD http://hr.hunttech.ru:8080/hrm/ -> HTTP 200
GET/HEAD http://hr.hunttech.ru:8080/app/ -> HTTP 404
```

Database connections:

```text
hunttech|1
```

No active connection to `itpearls` was observed in the final check.

## Known residual risks

- 633 historical physical files are missing and were accepted as irrecoverable by business decision.
- 499 of those missing files have active business references.
- Vaadin runs in debug mode according to startup warning.
- The application startup log lists pending DB update scripts and prints the standard message to set `cuba.automaticDatabaseUpdate=true`; this is a warning/instruction, not evidence that updateDb ran.

## Rollback notes

Rollback material is preserved:

- old database `itpearls` still exists and was not altered;
- old `/app` and `/app-core` artifacts are preserved in stage 4 backup;
- stage 3 deployment backup also exists:
  - `/var/backups/hunttech-hrm/20260712-233414-stage3-deployment`.

Rollback before meaningful user writes remains technically possible by stopping Tomcat and restoring old app artifacts/context files.

## Verdict

`HRM OPENED ON /hrm — OLD /app DISABLED — TARGET DATABASE hunttech ACTIVE`
