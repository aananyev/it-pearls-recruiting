# 2026-08-12 — Защита Emailer от «писем-сирот» (потерянный файл контента)

## Проблема

Планировщик `cuba_Emailer.processQueuedEmails` (period=10с) выбирает письма в статусе
SENDING и пытается отправить. Если письмо сохранено с контентом в FileStorage
(`content_text_file_id`), а сам файл потерян/удалён, происходило:

1. `loadBodyAndAttachments()` молча логгировал `FileStorageException` и оставлял
   `contentText = null`;
2. `sendSendingMessage()` падал с `NullPointerException` на
   `requireNonNull(contentText)` **вне** try/catch;
3. письмо навсегда оставалось в SENDING, `attemptsMade` не рос, и планировщик
   пытался отправить его каждые 10 секунд — лог засорялся ошибками, очередь
   не разгребалась.

Симптом в логе:

```
ERROR Emailer$EmailSendTask — Exception while sending email:
java.lang.NullPointerException: sendingMessage.caption is null  (Emailer.sendSendingMessage:201)
java.lang.NullPointerException: sendingMessage.contentText is null (Emailer.sendSendingMessage:202)
```

## Решение

Класс `HunttechEmailer extends Emailer` (core, `com.company.hunttech.app`) переопределяет
`sendSendingMessage()`: если после загрузки письма `address/caption/contentText/from`
отсутствуют, письмо помечается `SendingStatus.NOTSENT` с warn-логом и **не отправляется**.
Цикл NPE прекращается, битое письмо уходит из очереди.

Регистрация: `modules/core/src/com/company/hunttech/spring.xml` — бин
`id="cuba_Emailer"` с классом `HunttechEmailer` (перекрывает платформенный бин,
`allowBeanDefinitionOverriding=true` по умолчанию в Spring).

## Изменённые файлы

- `modules/core/src/com/company/hunttech/app/HunttechEmailer.java` — **создан**
- `modules/core/src/com/company/hunttech/spring.xml` — добавлен бин `cuba_Emailer`
- `modules/core/test/com/company/hunttech/hunttech/core/HunttechEmailerTest.java` — **создан** (3 теста)

## Тесты

- `springXmlRegistersHunttechEmailerAsCubaEmailer` — регистрация бина в spring.xml
- `hunttechEmailerExtendsEmailerAndOverridesSendSendingMessage` — наследование и сигнатура
- `emailerBeanExistsInContainer` — бин присутствует (в тест-контейнере — TestEmailer, ожидаемая подмена)

## Ручная проверка после деплоя

1. В очереди нет писем в статусе SENDING, застрявших с потерянным файлом.
2. В логе нет `NullPointerException: sendingMessage.* is null` от `Emailer$EmailSendTask`.
3. Письма с реально потерянным контентом помечены `NOTSENT` (status=300) с warn-логом.
