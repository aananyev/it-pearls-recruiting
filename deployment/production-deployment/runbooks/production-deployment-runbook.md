# Production deployment runbook: HRM HuntTech `/hrm`

Дата: 2026-07-12

## Назначение

Runbook описывает подготовку и последующий безопасный deploy новой версии HRM HuntTech по context path `/hrm` рядом со старым `/app`.

Этап 1 является только подготовительным: запрещены остановка `/app`, создание базы `hunttech`, изменение datasource, замена WAR, перезапуск Tomcat и запуск новой версии на production.

## Проверенные факты проекта

- Project root: `/Users/alekseyananyev/StudioProjects/hunttech_recruiting`.
- CUBA version в `build.gradle`: `7.3-SNAPSHOT`.
- Gradle module prefix: `app`.
- New web context: `hrm`.
- New core context: `hrm-core`.
- `buildWar.singleWar=false`.
- Production artifact model: CUBA two-WAR deployment, web `/hrm` plus core `/hrm-core`.
- `buildWar` задает `cuba.automaticDatabaseUpdate=false`.
- Local target database в Gradle tasks: `hunttech`.
- Source production database: `itpearls`.
- Target production database: `hunttech`.

## Production Tomcat facts from prior audit

- Production host: `hr.hunttech.ru`.
- Previously audited systemd service: `tomcat9`.
- Previously audited service user: `tomcat`.
- Previously audited WAR directory: `/var/lib/tomcat9/webapps`.
- Previously audited old web app: `/var/lib/tomcat9/webapps/app`.
- Previously audited old core app: `/var/lib/tomcat9/webapps/app-core`.

Текущий SSH-аудит Tomcat не завершен: доступ из Codex окружения отклонен сервером. До production-действий оператор должен выполнить `scripts/verify-tomcat-deployment.sh` с рабочими правами.

## Stage 1 allowed actions

- Read project configuration.
- Read existing migration documentation.
- Prepare scripts, manifests, runbooks and reports.
- Perform read-only HTTP checks.
- Perform read-only SSH audit only if credentials are available.

## Stage 1 forbidden actions

- Stop `/app`.
- Restart Tomcat.
- Change production datasource.
- Create production database `hunttech`.
- Change database `itpearls`.
- Replace deployed WAR files.
- Run Liquibase/updateDb/automatic database update.
- Delete old artifacts or files.

## Deployment stages after stage 1

1. Finish production Tomcat audit.
2. Final migration stage creates verified `hunttech` database from frozen `itpearls`.
3. Build fresh two-WAR artifacts with `buildWar`.
4. Backup current Tomcat deployment.
5. Deploy `/hrm` and `/hrm-core` with pre-cutover config.
6. Start restricted instance only after migration and backup validation.
7. Validate `/hrm` before user access.
8. Open user access only after explicit human phrase: `РАЗРЕШАЮ ОТКРЫТЬ HRM ПОЛЬЗОВАТЕЛЯМ`.

## Pre-cutover configuration requirements

- `cuba.automaticDatabaseUpdate=false`.
- `cuba.schedulingActive=false`.
- Outbound email disabled.
- Telegram disabled.
- External callbacks disabled.
- User access disabled.
- New application connects only to `hunttech`.
- Old `/app` continues to use only `itpearls` until stopped for final migration.

## Commands for stage 1 verification

```bash
deployment/production-deployment/scripts/verify-environment.sh --dry-run
deployment/production-deployment/validation/validate-deployment.sh
deployment/production-deployment/validation/validate-context-path.sh
```

## Commands that require later approval

These commands are prepared but must not be run in stage 1:

```bash
HRM_DEPLOY_APPROVED=yes deployment/production-deployment/scripts/build-production-artifact.sh --execute
HRM_DEPLOY_APPROVED=yes deployment/production-deployment/scripts/backup-current-deployment.sh --execute
HRM_DEPLOY_APPROVED=yes deployment/production-deployment/scripts/deploy-hrm-disabled.sh --execute <hrm.war> <hrm-core.war>
HRM_DEPLOY_APPROVED=yes deployment/production-deployment/scripts/configure-hrm.sh --execute
HRM_DEPLOY_APPROVED=yes deployment/production-deployment/scripts/start-hrm-restricted.sh --execute
```

## Stop conditions

Stop immediately if:

- `/hrm` artifact tries to connect to `itpearls`;
- automatic DB update is detected;
- Tomcat audit differs from manifest;
- old `/app` artifacts are missing;
- target context path is not `/hrm`;
- target database is not `hunttech`;
- any command would modify `itpearls` during stage 1;
- any script requires secrets in Git.

## Stage 1 verdict

Because current SSH Tomcat audit is blocked, stage 1 cannot be accepted as complete until an operator provides read-only Tomcat facts or runs the audit script successfully.
