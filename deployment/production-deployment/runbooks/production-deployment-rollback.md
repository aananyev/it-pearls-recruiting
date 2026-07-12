# Production deployment rollback runbook

Дата: 2026-07-12

## Цель

Сохранить возможность возврата пользователей на старое приложение `/app` и базу `itpearls`, не удаляя `/hrm`, `hunttech`, старые WAR, старые каталоги или старую базу.

## Принцип

Rollback отличается до и после появления пользовательских записей в `/hrm`.

## Rollback before user writes

Допустим только если доказано, что после cutover пользователи не записывали данные в `hunttech`.

Порядок:

1. Остановить доступ к `/hrm`.
2. Проверить отсутствие пользовательских write-событий после cutover timestamp.
3. Запустить или открыть `/app`.
4. Проверить, что `/app` подключается к `itpearls`.
5. Выполнить smoke test старого приложения.
6. Сохранить `/hrm` и `hunttech` для анализа.

Подготовленный guard:

```bash
CONFIRMED_NO_HRM_USER_WRITES=yes HRM_DEPLOY_APPROVED=yes \
  deployment/production-deployment/scripts/rollback-to-app.sh --execute
```

## Rollback after user writes

Автоматический rollback запрещен.

Порядок:

1. Немедленно остановить запись в `/hrm`.
2. Зафиксировать timestamp.
3. Сохранить обе базы.
4. Определить delta новых/измененных данных в `hunttech`.
5. Подготовить отчет для человеческого решения.
6. Не удалять ни одну базу.
7. Выполнить только утвержденный recovery plan.

## Forbidden rollback actions

- `DROP DATABASE itpearls`.
- `DROP DATABASE hunttech` без отдельного письменного решения.
- Удаление `/var/lib/tomcat9/webapps/app`.
- Удаление `/var/lib/tomcat9/webapps/app.war`.
- Массовое удаление через wildcard.
- Переключение пользователей на `/app` после writes в `/hrm` без delta analysis.

## Required validation after rollback

- `/app` reachable.
- `/hrm` not accepting user writes.
- Database connections from old app point only to `itpearls`.
- Security login works.
- Files are readable.
- Scheduled jobs are not duplicated.
- No two active writers exist.
