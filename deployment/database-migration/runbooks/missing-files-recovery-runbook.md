# Runbook: восстановление missing files

## Текущее состояние

На 2026-07-12 источники восстановления на сервере не найдены. Этот runbook применяется только если будет найден внешний verified backup.

## Безопасный порядок

1. Получить внешний архив fileStorage.
2. Сохранить архив вне Git и вне PostgreSQL data directory.
3. Рассчитать SHA-256 архива.
4. Распаковать архив только в staging-каталог.
5. Сопоставить файлы по UUID и расширению с `reports/missing-files-register.csv`.
6. Заполнить `config/missing-files-recovery-manifest.yaml`.
7. Запустить `recovery/stage-missing-files.sh` в dry-run.
8. После письменного разрешения запустить `recovery/restore-approved-files.sh`.
9. Проверить `validation/validate-recovered-files.sh`.
10. Повторить полную проверку `validation/validate-file-storage-completeness.sh`.

## Критерий успеха

Количество отсутствующих файлов должно стать 0 либо все оставшиеся отсутствующие файлы должны иметь письменное решение владельца данных.
