# Implementation Report — SettingWindow AI Profile Deployment

## Meta

| Поле | Значение |
|------|----------|
| Дата развертывания | 2026-07-22 |
| Сервер | hr.hunttech.ru |
| Окружение | Production |
| Ветка | `agent/working-setting-window-about-me-ai-profile` |
| DEPLOY_SHA | `1a7acd88e4db261db094370f73a510e0c0fe7d7f` |
| Deployment-скрипт | `deploy-prod.sh` (с ручной корректировкой) |
| Время начала | ~16:30 MSK |
| Время завершения | ~15:55 MSK |
| Rollback | не потребовался |

## Последовательность действий

1. **Попытка 1** — ошибка `app-web:compileJava` (setCollapsed не существует в CUBA 7.3)
2. **Диагностика** — установлено: `previewGroup.setCollapsed(false)` → `previewGroup.setExpanded(true)`
3. **Вторая попытка** — setCollapsed заменён на setExpanded
4. **Диагностика SCSS** — локальный ignored-каталог `hunttech-modern-light` без `styles.scss`; удалён
5. **Билд** — `clean assemble` BUILD SUCCESSFUL
6. **WAR** — `clean buildWar`, SHA зафиксированы
7. **Backup** — PostgreSQL + WAR на сервере
8. **Deploy** — rsync новых WAR на production
9. **Migration** — CUBA updateDb через SSH-туннель
10. **Tomcat** — запущен, HTTP 200

## WAR

| Файл | Размер | SHA-256 |
|------|--------|---------|
| app-core.war → hrm-core.war | 177M | 6bbb04e578e1a65bcd8e3795fd0724cecce08a372f3333d6382eb6c1b3e82b4b |
| app.war → hrm.war | 160M | 559b7d4748903f49866992e472698506c0d2edabbf349f31f229697ff50c323b |

## Production URL

- HTTP: `http://hr.hunttech.ru:8080/hrm/` → **200**
- HTTPS: `https://hr.hunttech.ru/hrm/` — таймаут (нет nginx reverse proxy)

## Production конфигурация

| Параметр | Значение |
|----------|----------|
| Сервер | hr.hunttech.ru |
| Tomcat | tomcat9 (systemd), /var/lib/tomcat9/webapps |
| БД | itpearls, PostgreSQL, пользователь cuba |
| Путь backup | /tmp/cuba_deploy_manual_20260722_164621/ |

## Решение

Этап принят. Дефектов не обнаружено. Rollback не потребовался.
