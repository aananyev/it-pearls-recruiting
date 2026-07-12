# Post-cutover validation report

Дата локальной проверки: 2026-07-13
Серверное время проверки: 2026-07-12T23:04:21+03:00
Сервер: `hr.hunttech.ru`

## Цель

Проверить состояние production после открытия HRM HuntTech на `/hrm` и отключения старого `/app`.

## Выполненные проверки

Проверки выполнялись read-only. Production данные и структура базы не изменялись.

## HTTP

```text
http://hr.hunttech.ru:8080/hrm/ -> HTTP 200
http://hr.hunttech.ru:8080/app/ -> HTTP 404
```

`/hrm` отдаёт HTML CUBA/Vaadin приложения. Старый `/app` не доступен как активный пользовательский контекст.

## Tomcat

```text
tomcat9: active
```

Активные webapps:

```text
ROOT
host-manager
hrm
hrm-core
hrm-core.war
hrm.war
manager
```

Старые `app*` артефакты отсутствуют в активном `/var/lib/tomcat9/webapps`; они сохранены в backup каталоге stage 4.

## PostgreSQL

Активные подключения к HRM-базам:

```text
hunttech|1
```

Активных подключений к `itpearls` на момент проверки не обнаружено.

Размеры баз:

```text
hunttech|6382 MB
itpearls|6346 MB
```

## Runtime safety flags

Проверено без вывода секретов:

```text
cuba.schedulingActive=false
cuba.email.smtpHost=<empty>
cuba.email.smtpUser=<empty>
cuba.email.smtpPassword=<empty>
hunttech.telegram.botName=<empty>
hunttech.telegram.botToken=<empty>
```

## Security aggregate counts

```text
sec_user=89
sec_role=12
sec_user_role=178
```

## Fresh logs after final fix

Период проверки server journal:

`since 2026-07-12 22:52:00`

Итог:

```text
ERROR=0
Exception=0
SEVERE=0
ScheduledRunnerThread=0
EmailSendTask=0
Telegram=0
automaticDatabaseUpdate_mentions=0
Liquibase_or_updateDb=0
```

## Residual risks

- Исторические 633 physical files из `sys_file` остаются утраченными по принятому бизнес-решению.
- Vaadin debug mode warning был отмечен на этапе cutover и требует отдельного технического исправления.
- Полная бизнес-валидация требует ручного входа рекрутера: логин, главная страница, вакансии, кандидаты, роли, создание безопасной тестовой записи.

## Verdict

`POST-CUTOVER TECHNICAL VALIDATION PASSED`

Следующий шаг: ручная бизнес-проверка пользователем HRM и наблюдение логов в течение рабочего окна.
