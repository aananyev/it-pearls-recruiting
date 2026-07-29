# Hermes: проверка Этапа 2 бухгалтерского Telegram-бота

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Контекст

Реализован Этап 2 MVP бухгалтерского Telegram-бота: прием фото/PDF из Telegram, сохранение нового файла во входящую папку Yandex.Disk и создание реестровой записи `AccountingDocument` с событием `RECEIVED`.

Этап 2 не выполняет OCR, подтверждение карточкой, выбор контрагента, раскладку по клиентским папкам и отправку Yandex.Mail.

## Репозиторий

Repo: `aananyev/it-pearls-recruiting`
Branch: `agent/accounting-documents-telegram-ingest`
Base: `master`

Проверяемый HEAD должен совпадать с HEAD ветки `agent/accounting-documents-telegram-ingest` после последнего push.

## Что Проверить

1. Ветка существует, PR открыт из `agent/accounting-documents-telegram-ingest` в `master`.
2. HEAD PR совпадает с проверяемым SHA; при несовпадении остановить проверку со статусом `HEAD_MISMATCH`.
3. Конфликтов с `master` нет.
4. Telegram-бот:
   - принимает `photo`;
   - принимает `document`;
   - скачивает файл по Telegram `fileId`;
   - передает файл в `AccountingDocumentIngestService`.
5. `AccountingDocumentIngestService`:
   - принимает только фото/PDF;
   - сохраняет новый файл в `Сканы/Входящие/YYYY-MM-DD/`;
   - создает `AccountingDocument` со статусом `NEW`;
   - создает `AccountingDocumentEvent` с типом `RECEIVED`;
   - считает `fileHash`;
   - блокирует повтор по `fileHash`.
6. Код не содержит секретов Telegram/Yandex.Mail и персональных Telegram ID.
7. Документация синхронизирована:
   - `docs/bots/AccountingDocumentsTelegramBot.md`;
   - `docs/bots/AccountingDocumentsTelegramBot_MVP_Plan.md`;
   - `docs/services/AccountingDocumentIngestService.md`;
   - `docs/services/README.md`.

## Команды

```bash
./gradlew :app-global:compileJava --no-daemon --stacktrace
./gradlew :app-core:compileJava --no-daemon --stacktrace
./gradlew :app-core:test --tests com.company.hunttech.core.AccountingDocumentIngestSupportTest --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Если есть локальный тестовый Telegram-бот и тестовая папка Yandex.Disk, дополнительно выполнить smoke:

1. Настроить `hunttech.accountingBot.allowedTelegramUserId` и пути через `local.app.properties` или переменные окружения.
2. Отправить боту тестовый PDF или фото.
3. Проверить, что создан новый файл только в `Сканы/Входящие/YYYY-MM-DD/`.
4. Проверить строки `AccountingDocument` и `AccountingDocumentEvent`.

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
Branch: agent/accounting-documents-telegram-ingest
PR: <номер PR>
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
