# Stage 2.1 final report

## 1. Что проверено

Проверен блокер file storage после production-миграции базы `itpearls -> hunttech`: 633 записи `sys_file` без физических файлов.

## 2. Производственная база и файлы

Данные production-базы не изменялись. Файловое хранилище не изменялось. Приложение не запускалось.

## 3. Архитектура file storage

- Сервер: `hr.hunttech.ru`
- Tomcat: `inactive`
- File storage root: `/opt/app_home/fileStorage`
- Владелец: `tomcat:tomcat`
- Отдельный mount отсутствует
- Найденный архив fileStorage: только финальный архив этапа 2

## 4. Количественный итог

| Метрика | Значение |
|---|---:|
| Missing files | 633 |
| С любыми ссылками | 501 |
| С активными ссылками | 499 |
| Всего строк ссылок | 663 |
| Активных строк ссылок | 660 |
| Soft-deleted/archive ссылок | 3 |
| Найдено в активном storage по другому пути | 0 |
| Найдено в доступных архивах | 0 |

## 5. Классификация

| Класс | Количество | Статус |
|---|---:|---|
| `E_ACTIVE_LINKED_FILE_MISSING` | 499 | Блокер |
| `D_ONLY_SOFT_DELETED_OR_ARCHIVE_LINKS` | 2 | Блокер до решения |
| `C_ORPHAN_SYS_FILE_METADATA` | 132 | Блокер до решения |
| `A_RECOVERABLE_SOURCE_FOUND` | 0 | Нет источника |

## 6. Бизнес-области

Основные ссылки найдены в резюме кандидатов, оригиналах резюме, дополнительных файлах, импортных файлах и очереди полнотекстового поиска.

## 7. Созданные файлы

- `reports/missing-files-investigation-report.md`
- `reports/missing-files-register.md`
- `reports/missing-files-register.csv`
- `reports/missing-files-summary.md`
- `reports/missing-files-business-impact-report.md`
- `reports/missing-files-fts-impact-report.md`
- `reports/missing-files-recovery-report.md`
- `reports/missing-files-decisions-register.md`
- `config/missing-files-recovery-manifest.yaml`
- `config/missing-files-decision-register.yaml`
- `validation/find-missing-files.sh`
- `validation/validate-file-paths.sh`
- `validation/validate-file-checksums.sh`
- `validation/find-sys-file-references.sql`
- `validation/classify-missing-file-links.sql`
- `validation/validate-recovered-files.sh`
- `validation/validate-file-storage-completeness.sh`
- `recovery/stage-missing-files.sh`
- `recovery/restore-approved-files.sh`
- `recovery/rollback-restored-files.sh`
- `runbooks/missing-files-investigation-runbook.md`
- `runbooks/missing-files-recovery-runbook.md`
- `runbooks/missing-files-rollback-runbook.md`
- `runbooks/missing-files-fts-recovery.md`

## 8. Выполненные команды

Выполнялись только read-only проверки базы, чтение файловой системы и просмотр tar-архива без распаковки. Команды с секретами не использовались и в отчёт не помещались.

## 9. Блокирующие вопросы

1. Есть ли external backup fileStorage за 2019-2023 годы?
2. Существуют ли snapshots диска сервера до текущего состояния?
3. Можно ли получить архивы старого `/opt/app_home/fileStorage` у хостинга?
4. Какие из 499 активных файлов бизнес считает критичными?
5. Допустимо ли запускать приложение, если часть старых документов признана утраченной?

## 10. Вердикт

`ЭТАП 3 ЗАПРЕЩЁН — MISSING FILES НЕ ВОССТАНОВЛЕНЫ И НЕ СОГЛАСОВАНЫ`
