# Hermes: проверка отвязки бухгалтерского бота от встроенного HRM-бота

STATUS: WAITING_FOR_HERMES

PROJECT: HRM HuntTech  
Repository: `alekseyananyev/hunttech`  
Branch: `agent/accounting-documents-recognition-confirmation`  
Base: `master`  
Verified HEAD: точный полный SHA указан в PR после финального push.

## Назначение проверки

Проверить корректировку архитектуры бухгалтерского Telegram-бота после решения Алексея:

- бухгалтерский бот должен функционировать в Hermes;
- AI/OCR должен использовать AI API, подключенный в Hermes;
- встроенный Telegram-бот HRM HuntTech не должен быть связан с бухгалтерской первичкой;
- HRM HuntTech остается владельцем учетной модели, реестра и событий.

## Что изменено

1. Из `modules/core/src/com/company/hunttech/core/telegrambot/telegram/Bot.java` удален прием бухгалтерских `photo`/`document`.
2. Встроенный HRM-бот больше не вызывает `AccountingDocumentIngestService`.
3. Добавлена спецификация внешнего Hermes-бота:
   - `docs/bots/AccountingDocumentsHermesBot.md`.
4. Обновлены документы:
   - `docs/bots/AccountingDocumentsTelegramBot.md`;
   - `docs/bots/AccountingDocumentsTelegramBot_MVP_Plan.md`;
   - `docs/bots/README.md`;
   - `docs/services/AccountingDocumentIngestService.md`;
   - `docs/services/README.md`.

## Обязательные проверки

Перед проверками Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки равен `Verified HEAD`;
3. PR открыт из `agent/accounting-documents-recognition-confirmation` в `master`;
4. base PR = `master`;
5. HEAD PR равен `Verified HEAD`;
6. conflicts = NONE.

Команды:

```bash
git status --short
git rev-parse HEAD
git diff --check
grep -n "AccountingDocumentIngest\|AccountingDocumentIngestResult\|processAccountingDocumentUpload\|isAccountingDocumentUpload" modules/core/src/com/company/hunttech/core/telegrambot/telegram/Bot.java || true
./gradlew :app-core:compileJava --no-daemon --stacktrace
./gradlew :app-core:test --tests com.company.hunttech.core.AccountingDocumentIngestSupportTest --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидания:

- `Bot.java` не содержит бухгалтерского приема файлов и вызова `AccountingDocumentIngestService`;
- `AccountingDocumentIngestService` продолжает компилироваться и проходить профильный тест;
- документация явно фиксирует Hermes как владельца Telegram runtime и AI API;
- секреты Telegram, Yandex.Mail и AI API не добавлены в git;
- существующие деловые документы Yandex.Disk и локального диска не изменялись.

## Запреты Hermes

Hermes не меняет Java, docs, tests, Liquibase или SQL; не делает commit, push, rebase, merge; не разрешает конфликты; не изменяет production; не трогает существующие деловые документы на Yandex.Disk и локальном диске.

## Формат отчета

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE | FAILED_VERIFICATION
Repo:
Branch:
PR:
Base:
проверен HEAD:
HEAD match: PASS/FAIL
Conflicts: NONE/...
Bot decoupling: PASS/FAIL
Docs synchronized: PASS/FAIL
Secrets in git: NONE/...
Compile: PASS/FAIL
Profile test: PASS/FAIL
Clean assemble: PASS/FAIL
Tomcat errors: NONE/...
Code/docs changed by Hermes: NO
Production changed: NO
Existing Yandex/local business documents changed: NO
```
