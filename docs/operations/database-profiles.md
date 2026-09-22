# Профили подключения HRM HuntTech

Дата: 2026-09-22

## Назначение

Приложение использует JNDI-ресурс `jdbc/CubaDS`. Профиль выбирается явно через
`HUNTTECH_DB_PROFILE`; пароль никогда не хранится в Git и не печатается
диагностикой.

| Профиль | PostgreSQL host | Остальные параметры |
|---|---|---|
| `LOCAL` | `192.168.1.135` | port/database/user берутся из текущего контракта (`5432`/`hunttech`/`cuba`) и могут быть уточнены профильными переменными; пароль — `HUNTTECH_LOCAL_DB_PASSWORD` |
| `TEST` | только явно заданный отдельный host | обязательны `HUNTTECH_TEST_DB_HOST`, `HUNTTECH_TEST_DB_PORT`, `HUNTTECH_TEST_DB_NAME`, `HUNTTECH_TEST_DB_USER`, `HUNTTECH_TEST_DB_PASSWORD`; fallback на LOCAL/PRODUCTION запрещён |
| `PRODUCTION` | только `127.0.0.1` | текущий production target `5432`/`hunttech`/`cuba`; пароль должен быть установлен на production вне Git |

`localhost` не считается допустимым production host: guard требует именно
`127.0.0.1`, чтобы исключить неявный fallback.

## Локальный запуск

Перед запуском задайте отдельный рабочий пароль вне репозитория:

```bash
export HUNTTECH_DB_PROFILE=LOCAL
export HUNTTECH_LOCAL_DB_PASSWORD='***'
bash scripts/start-app.sh
```

Скрипт проверяет доступность `192.168.1.135`, рендерит JNDI context в
`deploy/tomcat` с правами `0600` и запускает Tomcat. Локальный PostgreSQL не
запускается автоматически. Production credentials использовать запрещено.

## TEST

TEST нельзя запускать без подтверждённых реквизитов. Пример контракта без
значений:

```bash
export HUNTTECH_DB_PROFILE=TEST
export HUNTTECH_TEST_DB_HOST='<approved-test-host>'
export HUNTTECH_TEST_DB_PORT='<approved-test-port>'
export HUNTTECH_TEST_DB_NAME='<approved-test-database>'
export HUNTTECH_TEST_DB_USER='<approved-test-user>'
export HUNTTECH_TEST_DB_PASSWORD='***'
```

Заполнение этих переменных значениями из LOCAL или PRODUCTION не является
настройкой TEST и будет отклонено.

## Production guard

До SSH, backup, остановки Tomcat или передачи WAR production-скрипты выполняют:

```bash
HUNTTECH_DB_PROFILE=PRODUCTION \
  bash scripts/validate-db-profile.sh --profile PRODUCTION
```

Команда выводит только профиль, host, port, database и user. Она завершается
ошибкой при неизвестном профиле, host `192.168.1.135`, `localhost`, generic
`HUNTTECH_DB_HOST` или hardcoded datasource password в tracked-конфигурации.

Production JNDI context должен быть установлен оператором из secret store на
`hr.hunttech.ru`; репозиторий предоставляет только безопасный шаблон с
loopback host и placeholder для пароля. В рамках BL-2026-034 production не
изменялся и deployment не выполнялся.

## Проверки

```bash
bash scripts/test-db-profiles.sh
bash deployment/production-deployment/validation/validate-deployment.sh
```

Тесты проверяют LOCAL=`192.168.1.135`, PRODUCTION=`127.0.0.1`, отказ от
внешнего production host, отказ неизвестного профиля, независимость TEST и
отсутствие прежнего tracked datasource password.

## Rollback

Сначала остановить новую `/hrm` запись и зафиксировать timestamp. Вернуть
предыдущий проверенный WAR и предыдущий внешний JNDI context; базы данных не
удалять и адреса между окружениями не переносить. До пользовательских записей
разрешён только подтверждённый rollback на `/app`; после записей нужен отдельный
delta/recovery plan. Секреты при rollback берутся из прежнего secret store, а не
из Git или shell history.
