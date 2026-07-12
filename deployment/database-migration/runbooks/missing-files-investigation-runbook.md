# Runbook: расследование missing files

## Назначение

Повторить read-only проверку `sys_file` и file storage перед решением о переходе к этапу 3.

## Предусловия

- Tomcat остановлен.
- Пользователи не пишут в базу.
- Есть доступ к PostgreSQL без сохранения пароля в командах.
- Есть доступ на чтение `/opt/app_home/fileStorage`.

## Порядок

1. Проверить, что Tomcat неактивен.
2. Запустить `validation/find-missing-files.sh` с `DB_NAME=hunttech` и `STORAGE_ROOT=/opt/app_home/fileStorage`.
3. Запустить `validation/validate-file-paths.sh` по полученному CSV.
4. Выполнить read-only SQL `validation/find-sys-file-references.sql`.
5. Сформировать классификацию через `validation/classify-missing-file-links.sql`.
6. Обновить `reports/missing-files-register.csv`.
7. Не переходить к восстановлению без решения владельца данных.

## Запреты

- Не запускать `/hrm`.
- Не удалять строки `sys_file`.
- Не изменять ссылки в бизнес-таблицах.
- Не создавать файлы-заглушки.
