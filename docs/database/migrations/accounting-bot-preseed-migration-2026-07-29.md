# Миграция реестра бухгалтерского Telegram-бота и предзаполнение справочников

## Назначение и бизнес-смысл (What & Why)

Документ фиксирует обязательный регламент будущей миграции HRM HuntTech для бухгалтерского Telegram-бота: прием первички и чеков авансового отчета через Telegram, раскладка файлов в Yandex.Disk, отправка через Yandex.Mail и хранение статусов в базе HRM HuntTech.

В Этапе 1 код проекта меняется только в части новых сущностей бухгалтерского контура, `views.xml`, `persistence.xml`, локализации и Liquibase. Документ нужен как обязательная спецификация для локальной и будущей production-миграции, чтобы не продублировать существующие справочники HRM HuntTech и не внести неконтролируемые данные в рабочую базу.

Фактический changelog Этапа 1: `modules/core/db/changelog/260729-1-addAccountingBotEntities.xml`.

Главный принцип: существующие справочники HRM HuntTech остаются источниками истины. Новые таблицы добавляются только для бухгалтерского процесса, статусов, событий, получателей, категорий чеков, алиасов компаний и настроек автоматизации.

Отдельное обязательное правило: миграция и бот не должны трогать, удалять, менять, переименовывать, перезаписывать или перемещать уже существующие деловые документы на Yandex.Disk и локальном диске: акты, договоры, УПД, счета, задания, чеки и другие файлы клиентского или бухгалтерского архива. Любая файловая операция разрешена только для новых файлов, загруженных через Telegram-бот во входящую папку.

## UI Context & Navigation

На первом этапе UI в HRM HuntTech не создается. Пользователь работает через Telegram-бота, а база HRM HuntTech используется как backend-реестр.

Будущие экраны могут быть добавлены позже: входящие документы, документы на подтверждении, готовые к отправке, отправленные, авансовые отчеты, ошибки распознавания и журнал событий.

Миграция не должна добавлять пункты меню, экраны, XML descriptors или web-контроллеры. Если UI появится в отдельной задаче, она должна иметь отдельную UI-документацию.

## Behavior Summary

| Действие | Условие | Результат |
| --- | --- | --- |
| Будущая миграция применяется к локальной БД | таблицы бухгалтерского контура отсутствуют | создаются новые таблицы и индексы |
| Будущая миграция применяется повторно | таблицы или строки уже существуют | повторный запуск не создает дубли |
| В `Currency` нет `RUB` | локальная или production-БД готовится к запуску бота | добавляется рубль как обязательная валюта |
| В `Company` есть клиенты и короткие названия | выполняется подготовка алиасов | создается dry-run отчет; автоматическая вставка алиасов в production запрещена без подтверждения |
| Контрагент из документа не найден | бот обрабатывает документ | новая компания не создается без подтверждения пользователя |
| Получатель бухгалтерии изменился | меняется `AccountingRecipient` | старые `AccountingEmailBatch` сохраняют фактические To/Cc/Bcc |
| Production-миграция планируется | нет отчета Hermes по точному HEAD и копии БД | миграция запрещена |

## 1. Границы миграции

### 1.1. Что разрешено

- добавить новые сущности и таблицы бухгалтерского контура;
- добавить enum-поля внутри новых сущностей;
- добавить FK из новых таблиц на существующие `Company`, `Currency`, `ExtUser`, опционально `LaborAgreement`;
- предзаполнить новые справочники бухгалтерского контура;
- идемпотентно добавить `RUB` в `Currency`, если отсутствует;
- подготовить dry-run отчет стартовых алиасов компаний из существующего справочника `Company`;
- создать локальные тестовые настройки автоматизации только для локальной БД и только без секретов в git.

### 1.2. Что запрещено

- менять существующие сущности HRM HuntTech без отдельной прямой задачи;
- добавлять поля в `Company`, `Currency`, `ExtUser`, `LaborAgreement`, `InternalEmailer`, `SomeFiles`, `FileType` в рамках этой миграции;
- создавать новый справочник компаний или контрагентов;
- создавать новый справочник валют;
- использовать `InternalEmailer` как журнал отправки бухгалтеру;
- складывать файлы Yandex.Disk в CUBA `FileDescriptor` без отдельного решения;
- массово создавать компании из OCR без подтверждения пользователя;
- менять уже существующие акты, договоры, УПД, счета, задания, чеки и другие деловые файлы на Yandex.Disk или локальном диске;
- удалять, переименовывать или чистить production-данные;
- выполнять production-миграцию без backup, проверки на копии production-БД и отчета Hermes.

## 2. Переиспользуемые сущности HRM HuntTech

| Данные бухгалтерского процесса | Существующая сущность | Правило |
| --- | --- | --- |
| Клиент / контрагент | `Company` | ссылка из `AccountingDocument.company`; новая компания только после подтверждения |
| Наше юрлицо | `Company` | выбирать записи с `ourLegalEntity = true` |
| Валюта | `Currency` | обязательное наличие `RUB` |
| Отправитель Yandex.Mail | `ExtUser` | использовать SMTP-настройки пользователя-отправителя |
| Договор | `LaborAgreement` | опциональная связь для документов типа договор |

Существующие сущности не изменяются. Новые таблицы ссылаются на них через FK.

## 3. Новые таблицы будущей миграции

Имена приведены как целевой контракт будущей реализации.

| Таблица | Назначение | Предзаполнение |
| --- | --- | --- |
| `HUNTTECH_ACCOUNTING_DOCUMENT` | реестр документов, статусов, путей Yandex.Disk и Telegram-метаданных | нет |
| `HUNTTECH_ACCOUNTING_EMAIL_BATCH` | пакет отправки бухгалтеру, один пакет = одно письмо | нет |
| `HUNTTECH_ACCOUNTING_DOCUMENT_EVENT` | история событий документа | нет |
| `HUNTTECH_ACCOUNTING_RECIPIENT` | внешние получатели бухгалтерских писем | да |
| `HUNTTECH_ACCOUNTING_EXPENSE_CATEGORY` | категории чеков авансового отчета | да |
| `HUNTTECH_ACCOUNTING_COMPANY_ALIAS` | алиасы OCR-названий к существующей `Company` | нет в обязательном changelog; только dry-run или ручные подтверждения |
| `HUNTTECH_ACCOUNTING_AUTOMATION_SETTINGS` | настройки путей, расписания, Telegram и отправителя | таблица создается; секреты и Telegram ID не предзаполняются |

## 4. Enum-контракты без предзаполнения

Эти значения не являются справочниками и не должны храниться отдельными таблицами.

| Enum | Значения |
| --- | --- |
| `AccountingFlowType` | `PRIMARY`, `ADVANCE_REPORT`, `ALL` |
| `AccountingDocumentType` | `CONTRACT`, `ACT`, `UPD`, `INVOICE`, `TASK`, `RECEIPT`, `OTHER` |
| `AccountingDocumentStatus` | `NEW`, `WAITING_CONFIRMATION`, `WAITING_COMPANY_MATCH`, `CONFIRMED`, `READY_TO_SEND`, `SENT`, `REJECTED`, `BAD_SCAN`, `ERROR` |
| `AccountingEmailBatchStatus` | `DRAFT`, `READY_TO_SEND`, `SENT`, `ERROR`, `CANCELLED` |
| `AccountingDocumentEventType` | `RECEIVED`, `OCR_PROCESSED`, `CONFIRMATION_REQUESTED`, `CONFIRMED`, `RENAMED`, `MOVED_TO_FINAL_FOLDER`, `ADDED_TO_EMAIL_BATCH`, `SENT`, `MOVED_TO_SENT_FOLDER`, `REJECTED`, `ERROR` |
| `AccountingRecipientRole` | `PRIMARY_ACCOUNTANT`, `BACKUP_ACCOUNTANT`, `OWNER_COPY`, `OTHER` |

## 5. Предзаполнение справочников

### 5.1. `Currency`

Обязательное значение:

| `currencyShortName` | `currencyLongName` |
| --- | --- |
| `RUB` | `Российский рубль` |

Правило миграции: вставить только если `RUB` отсутствует. Если `RUB` уже есть, не менять название и UUID существующей записи.

### 5.2. `AccountingExpenseCategory`

Минимальный набор:

| Код | Название | Ключевые слова |
| --- | --- | --- |
| `FUEL` | `Топливо` | `топливо`, `бензин`, `дизель`, `азс` |
| `SERVICES` | `Услуги` | `услуга`, `услуги`, `сервис` |
| `TRANSPORT` | `Транспорт` | `такси`, `парковка`, `проезд`, `транспорт` |
| `COMMUNICATION` | `Связь и интернет` | `связь`, `интернет`, `телефон` |
| `MATERIALS` | `Материалы и товары` | `товар`, `материалы`, `канцтовары` |
| `OTHER` | `Другое` | `прочее`, `другое` |

Правило миграции: уникальность по `code`; повторный запуск не меняет существующие строки, кроме явно согласованного исправления опечаток в отдельной задаче.

### 5.3. `AccountingRecipient`

Справочник получателей бухгалтерских писем заполняется только утвержденными адресами.

Минимальное требование:

- минимум один активный получатель для `PRIMARY`;
- минимум один активный получатель для `ADVANCE_REPORT`;
- если один бухгалтер получает оба потока, использовать `flowType = ALL`;
- фактические адреса отправки всегда копируются в `AccountingEmailBatch.toEmails`, `ccEmails`, `bccEmails`.

Production-миграция не должна угадывать email бухгалтера из переписки или настроек пользователя. Адреса передаются в миграционном задании явно.

### 5.4. `AccountingCompanyAlias`

Стартовые алиасы создаются только для существующих записей `Company`, но не вставляются обязательным changelog Этапа 1. Перед production нужна отдельная dry-run выгрузка и ручное подтверждение спорных совпадений.

Источники алиасов:

- `Company.comanyName`;
- `Company.companyShortName`;
- названия папок Yandex.Disk из `ХантТек/Договоры/Клиенты/<год>/<контрагент>`;
- ручные подтверждения пользователя в Telegram после запуска бота.

Правила:

- алиас не создает новую компанию;
- один алиас должен ссылаться только на одну `Company`;
- конфликт алиаса между несколькими компаниями не разрешается автоматически;
- алиасы из Yandex.Disk должны проходить dry-run отчет перед вставкой в production.

### 5.5. `AccountingAutomationSettings`

Настройки автоматизации хранят значения процесса, но не заменяют системные настройки пользователей. В Этапе 1 создается только таблица; обязательный changelog не вставляет строку настроек, потому что в ней могут оказаться локальные пути и закрытые идентификаторы.

Локальные стартовые значения:

| Поле | Значение |
| --- | --- |
| `yandexDiskRootPath` | `/Users/alekseyananyev/Yandex.Disk-alan@hunttech.ru.localized/ХантТек` |
| `incomingScansPath` | `Сканы` |
| `primaryDocumentsPath` | `Договоры/Клиенты` |
| `advanceReportsPath` | `Бухгалтерия/Авансовые отчеты` |
| `primarySendSchedule` | один раз в день |
| `advanceReportSendSchedule` | один раз в месяц в последних числах месяца |
| `senderUser` | утвержденный пользователь HRM HuntTech с Yandex.Mail SMTP |
| `confirmationTelegramUserId` | утвержденный Telegram user id владельца процесса |

Production-значения должны быть подтверждены перед миграцией. Секреты, пароли и персональные Telegram ID не должны попадать в changelog, документацию или git.

## 6. Обязательный порядок локальной подготовки

1. Добавить сущности и changelog в отдельной будущей задаче.
2. Применить миграцию только к локальной БД или disposable-копии.
3. Проверить, что существующие таблицы HRM HuntTech не изменились.
4. Проверить наличие `RUB`.
5. Проверить полный набор `AccountingExpenseCategory`.
6. Проверить список `AccountingRecipient`.
7. Сформировать dry-run отчет по `AccountingCompanyAlias`, но не вставлять спорные алиасы без подтверждения.
8. Подтвердить настройки путей Yandex.Disk и расписаний вне git.
9. Запустить повторное применение миграции и убедиться, что дубли не создаются.
10. Передать Hermes точный HEAD и команды проверки.

## 7. Обязательный порядок production-миграции

Production-миграция допустима только после отдельной прямой команды Алексея на production-работы.

1. Зафиксировать ветку, PR и полный HEAD SHA.
2. Сделать backup production-БД.
3. Восстановить backup в отдельную disposable-БД.
4. Выполнить read-only preflight:
   - наличие `HUNTTECH_COMPANY`;
   - наличие `HUNTTECH_CURRENCY`;
   - наличие `SEC_USER`;
   - наличие `HUNTTECH_LABOR_AGREEMENT`;
   - количество компаний с `OUR_CLIENT = TRUE`;
   - количество юрлиц с `OUR_LEGAL_ENTITY = TRUE`;
   - наличие `RUB`.
5. Выполнить `liquibase validate`.
6. Сгенерировать `update-sql` и подтвердить отсутствие `DROP`, `DELETE`, `TRUNCATE`.
7. Применить миграцию к disposable-БД.
8. Проверить idempotency: повторный запуск не создает новые строки справочников.
9. Проверить FK, индексы и счетчики строк.
10. Проверить, что production-приложение на копии стартует без ошибок.
11. Hermes готовит отчет по точному HEAD.
12. Только после успешного отчета и отдельного разрешения применять к production.

## 8. Контрольные SQL-запросы

Запросы приведены как шаблон для будущей миграции. Фактические имена колонок должны быть синхронизированы с реализованными entity и changelog.

### 8.1. Проверка существующих справочников

```sql
select count(*) as clients_count
from HUNTTECH_COMPANY
where DELETE_TS is null
  and OUR_CLIENT = true;

select count(*) as legal_entities_count
from HUNTTECH_COMPANY
where DELETE_TS is null
  and OUR_LEGAL_ENTITY = true;

select count(*) as rub_count
from HUNTTECH_CURRENCY
where DELETE_TS is null
  and upper(CURRENCY_SHORT_NAME) = 'RUB';
```

### 8.2. Проверка предзаполнения новых справочников

```sql
select CODE, NAME_RU, ACTIVE
from HUNTTECH_ACCOUNTING_EXPENSE_CATEGORY
where DELETE_TS is null
order by SORT_ORDER, CODE;

select FLOW_TYPE, RECIPIENT_ROLE, EMAIL, ACTIVE
from HUNTTECH_ACCOUNTING_RECIPIENT
where DELETE_TS is null
order by FLOW_TYPE, RECIPIENT_ROLE, EMAIL;

select count(*) as company_alias_count
from HUNTTECH_ACCOUNTING_COMPANY_ALIAS
where DELETE_TS is null;
```

### 8.3. Проверка отсутствия дублей

```sql
select CODE, count(*) as rows_count
from HUNTTECH_ACCOUNTING_EXPENSE_CATEGORY
where DELETE_TS is null
group by CODE
having count(*) > 1;

select lower(EMAIL) as normalized_email, FLOW_TYPE, count(*) as rows_count
from HUNTTECH_ACCOUNTING_RECIPIENT
where DELETE_TS is null
group by lower(EMAIL), FLOW_TYPE
having count(*) > 1;

select lower(ALIAS) as normalized_alias, count(distinct COMPANY_ID) as company_count
from HUNTTECH_ACCOUNTING_COMPANY_ALIAS
where DELETE_TS is null
group by lower(ALIAS)
having count(distinct COMPANY_ID) > 1;
```

## 9. Блокеры production-миграции

Production-миграция запрещена, если:

- нет свежего backup production-БД;
- changelog не проверен на disposable-копии;
- `update-sql` содержит destructive DDL/DML;
- не утвержден список `AccountingRecipient`;
- не утвержден пользователь-отправитель Yandex.Mail;
- не утверждены production-пути Yandex.Disk;
- сценарий требует изменения уже существующих деловых файлов Yandex.Disk или локального диска;
- есть конфликтующие алиасы компаний;
- `RUB` отсутствует и не может быть добавлен идемпотентно;
- Hermes не подтвердил точный HEAD SHA.

## 10. Связанные документы

- [AccountingDocumentsTelegramBot.md](../../bots/AccountingDocumentsTelegramBot.md) — функциональная спецификация бухгалтерского Telegram-бота.
- [database README](../README.md) — индекс документации БД.
- [production-schema-reconciliation-2026-07-27.md](production-schema-reconciliation-2026-07-27.md) — пример безопасной additive-migration с preconditions.
- [production-migration-runbook.md](../../../deployment/database-migration/runbooks/production-migration-runbook.md) — общий production runbook миграций.

## 11. История изменений

| Дата | Изменение |
| --- | --- |
| 2026-07-29 | Уточнено, что запрет относится к существующим деловым документам: актам, договорам, УПД, счетам, заданиям и чекам |
| 2026-07-29 | Добавлено обязательное правило неприкосновенности существующих документов на Yandex.Disk и локальном диске |
| 2026-07-29 | Создан обязательный регламент будущей миграции бухгалтерского Telegram-бота и предзаполнения справочников |
