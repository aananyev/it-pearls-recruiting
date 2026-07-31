# AccountingDocumentIngestService

## История изменений

| Дата | Изменение |
| ---- | --------- |
| 2026-07-29 | Уточнено, что бухгалтерский Telegram runtime перенесен в Hermes, а сервис остается учетной точкой приема новых файлов. |
| 2026-07-29 | Добавлен сервис приема фото/PDF из Telegram для Этапа 2 бухгалтерского бота. |

## Назначение и бизнес-смысл (What & Why)

`AccountingDocumentIngestService` принимает файл, уже скачанный внешним Hermes-ботом, сохраняет его во входящую папку Yandex.Disk и создает запись реестра `AccountingDocument` в HRM HuntTech.

Сервис закрывает ручной шаг «скачать скан из Telegram и положить во входящие», но пока не раскладывает документы по клиентским папкам и не отправляет письма бухгалтеру.

## UI Context & Navigation

Пользовательского экрана нет. Вход в процесс — отдельный Telegram-бот Hermes. Встроенный HRM Telegram-бот не принимает бухгалтерские фото/PDF.

Технический вызов:

| Источник | Вызов |
| -------- | ----- |
| Hermes-бот бухгалтерских документов | получает Telegram `photo` или `document`, скачивает новый файл по `fileId`, вызывает учетный контракт HRM HuntTech на базе `AccountingDocumentIngestService.ingestTelegramFile(...)` |

## Behavior Summary

| Действие | Условие | Результат |
| -------- | ------- | --------- |
| Пользователь отправил фото | Telegram message содержит `photo` | Hermes-бот скачивает максимальный размер фото и передает в сервис. |
| Пользователь отправил PDF | Telegram message содержит `document` с PDF или поддержанным расширением | Файл сохраняется во входящую папку. |
| Файл принят | Hash еще не встречался | Создается `AccountingDocument` со статусом `NEW` и событие `RECEIVED`. |
| Файл уже есть | Найден такой же `fileHash` | Повторная обработка останавливается, новая запись не создается. |
| Файл не поддерживается | Не фото и не PDF | Пользователь получает отказ в Telegram. |

## Настройки

Сервис определяет входящую папку в таком порядке:

1. активная запись `AccountingAutomationSettings`;
2. CUBA app properties:
   - `hunttech.accountingBot.yandexDiskRootPath`;
   - `hunttech.accountingBot.incomingScansPath`;
3. переменные окружения:
   - `ACCOUNTING_BOT_YANDEX_DISK_ROOT`;
   - `ACCOUNTING_BOT_INCOMING_SCANS_PATH`;
4. локальный fallback:
   - `${user.home}/Yandex.Disk-alan@hunttech.ru.localized/ХантТек`;
   - `Сканы/Входящие`.

Разрешенный Telegram user id определяется в таком порядке:

1. `AccountingAutomationSettings.confirmationTelegramUserId`;
2. `hunttech.accountingBot.allowedTelegramUserId`;
3. `ACCOUNTING_BOT_ALLOWED_TELEGRAM_USER_ID`.

Если Telegram user id не настроен, файл принимается с warning в логах. Перед production это значение обязательно должно быть настроено.

## Что Создается

Для принятого файла:

- физический файл во входящей папке `Сканы/Входящие/YYYY-MM-DD/`;
- запись `AccountingDocument`;
- запись `AccountingDocumentEvent` с типом `RECEIVED`.

Файл получает техническое имя:

```text
yyyyMMdd-HHmmss-<telegramChatId>-<telegramMessageId>-<исходное имя>.<расширение>
```

## Определение Потока

Этап 2 использует простой классификатор по имени файла и подписи Telegram.

| Признаки | Поток |
| -------- | ----- |
| `чек`, `авансов`, `топливо`, `азс`, `бензин`, `дизель`, `такси`, `парковк` | `ADVANCE_REPORT` |
| остальные фото/PDF | `PRIMARY` |

Для `ADVANCE_REPORT` тип документа ставится `RECEIPT`. Для первички тип определяется по словам `договор`, `акт`, `упд`, `счет/счёт`, `задани`; иначе `OTHER`.

## Ограничения Этапа 2

Сервис не выполняет:

- OCR/AI-распознавание реквизитов;
- выбор контрагента;
- создание `AccountingCompanyAlias`;
- раскладку в `Договоры/Клиенты/...`;
- перемещение чеков в `К отправке` или `Отправлено`;
- отправку писем через Yandex.Mail.

OCR/AI-распознавание, Telegram-подтверждения и отправка писем выполняются внешним Hermes-ботом. Секреты Telegram и AI API Hermes не должны храниться в HRM HuntTech.

Строго запрещено трогать, удалять, переименовывать или перемещать уже существующие деловые документы на Yandex.Disk и локальном диске.
