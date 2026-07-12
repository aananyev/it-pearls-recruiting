# Tomcat context path migration: `/app` to `/hrm`

Дата: 2026-07-12

## Цель

Перейти от старого context path `/app` к новому `/hrm`, сохраняя старый `/app` как rollback point.

## Фактическая CUBA architecture

Проект не является single-WAR production deployment:

- `buildWar.singleWar=false`;
- web context: `/hrm`;
- core context: `/hrm-core`;
- old web context: `/app`;
- old core context: `/app-core`.

Следовательно, production deployment должен учитывать два приложения Tomcat: web и middleware/core.

## Required context paths

| Component | Old context | New context | Status |
|---|---:|---:|---|
| Web UI | `/app` | `/hrm` | New target |
| Core middleware | `/app-core` | `/hrm-core` | New target |

## Cookie and session isolation

Проверить перед открытием доступа:

- session cookie path для `/hrm`;
- отсутствие конфликта с `/app`;
- logout/login работают отдельно;
- WebSocket endpoint не конфликтует;
- redirects не отправляют пользователя на `/app`.

## Database isolation

- `/app` must connect only to `itpearls`.
- `/hrm` must connect only to `hunttech`.
- No simultaneous writers after cutover.
- `cuba.automaticDatabaseUpdate=false` for `/hrm` first production start.

## Stage 1 checks

Stage 1 does not deploy or stop anything. It only prepares scripts and records blockers.

```bash
curl -I --max-time 10 http://hr.hunttech.ru:8080/app/ || true
curl -I --max-time 10 http://hr.hunttech.ru:8080/hrm/ || true
```

## Later deployment rule

Do not overwrite or delete `/app` or `/app-core`. New artifacts must be deployed as `/hrm` and `/hrm-core`.
