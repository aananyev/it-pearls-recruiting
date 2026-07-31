# Hermes: проверка Этапа 1 бухгалтерского Telegram-бота

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Контекст

Реализован Этап 1 MVP бухгалтерского Telegram-бота: модель БД, справочники, Liquibase changelog и документация процесса.

Изменения не должны затрагивать production и не должны выполнять операции с реальными деловыми документами на Yandex.Disk или локальном диске.

## Репозиторий

Repo: `aananyev/it-pearls-recruiting`
Branch: `agent/accounting-documents-bot`
Base: `master`
PR: `https://github.com/aananyev/it-pearls-recruiting/pull/104`

Проверяемый HEAD должен совпадать с HEAD ветки `agent/accounting-documents-bot` после последнего push.

## Что Проверить

1. Ветка существует, PR открыт из `agent/accounting-documents-bot` в `master`.
2. HEAD PR совпадает с проверяемым SHA; при несовпадении остановить проверку со статусом `HEAD_MISMATCH`.
3. Конфликтов с `master` нет.
4. Новые сущности зарегистрированы в:
   - `modules/global/src/com/company/hunttech/persistence.xml`;
   - `modules/global/src/com/company/hunttech/views.xml`.
5. Changelog `modules/core/db/changelog/260729-1-addAccountingBotEntities.xml`:
   - создает только новые таблицы `HUNTTECH_ACCOUNTING_*`;
   - не меняет существующие `Company`, `Currency`, `ExtUser`, `LaborAgreement`;
   - не содержит `DROP`, `DELETE`, `TRUNCATE`;
   - содержит preconditions на существующие таблицы;
   - предзаполняет только `RUB`, категории чеков и получателя бухгалтерии.
6. Документация синхронизирована:
   - `docs/bots/`;
   - `docs/entities/accounting-bot/AccountingBotEntities.md`;
   - `docs/database/migrations/accounting-bot-preseed-migration-2026-07-29.md`;
   - профильные `README`.

## Команды

```bash
./gradlew :app-global:compileJava --no-daemon --stacktrace
./gradlew :app-core:compileJava --no-daemon --stacktrace
./gradlew :app-core:test --tests com.company.hunttech.core.DatabaseSchemaReconciliationChangelogTest --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Если есть disposable-копия локальной БД, дополнительно проверить применимость Liquibase changelog и повторный запуск без дублей.

## Запреты

- Не делать merge.
- Не менять функциональный код.
- Не делать rebase.
- Не менять production.
- Не запускать production-миграции.
- Не трогать, не удалять, не переименовывать и не перемещать уже существующие деловые документы на Yandex.Disk и локальном диске.

## Ожидаемый отчет

При успехе:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/accounting-documents-bot
PR: https://github.com/aananyev/it-pearls-recruiting/pull/104
Base: master
проверен HEAD: <SHA>
HEAD match: PASS
conflicts: NONE
checks: PASS
production: NOT TOUCHED
business documents on disk: NOT TOUCHED
merge: NOT DONE
```

При ошибке:

```text
PROJECT: HRM HuntTech
STATUS: FAILED_VERIFICATION
FAILED STEP: <шаг>
ROOT CAUSE: <причина>
проверен HEAD: <SHA>
code changes by Hermes: NO
production touched: NO
```
