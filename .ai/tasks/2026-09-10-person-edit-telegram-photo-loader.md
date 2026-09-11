# Задача: Загрузка фотографии профиля из Telegram в форме редактирования PersonEdit

## Дата: 2026-09-10
## Ветка: agent/antigravity-dev
## Роли: Аналитик, UI/UX-дизайнер, Java Backend-разработчик, Автоматизированный тестировщик

### Описание задачи
1. В форме редактирования `PersonEdit` (`person-edit.xml`) в блоке ввода Telegram (`telegramNameField`) реализовать справа от поля ввода кнопку «Загрузить фото» (`loadTelegramPhotoButton`).
2. При наличии введенного Telegram username (например, `@durov`, `durov`, `https://t.me/durov`) и нажатии на кнопку:
   - Выполнить получение фотографии профиля человека из Telegram.
   - Сохранить полученную фотографию в `FileStorage` как `FileDescriptor`.
   - Поместить фотографию в круглый компонент `OvaFallbackImage` (`personPic`) в левом sidebar карточки `PersonEdit`.
   - Привязать созданный `FileDescriptor` к свойству `fileImageFace` редактируемой сущности `Person`, чтобы при сохранении (кнопка «ОК» / Commit) фотография сохранялась в БД.
3. Разработать интерфейс и обработку сценариев:
   - Поле Telegram пустое -> предупреждение с просьбой ввести имя пользователя.
   - Фотография найдена и загружена -> успешное уведомление (Tray) и мгновенное обновление аватара в sidebar.
   - Профиль без фото или приватный -> информативное предупреждение.
   - Ошибка сети/сервиса -> сообщение об ошибке.
4. Если требуются дополнительные действия (например, для приватных профилей через Telegram Bot API / MTProto сервис):
   - Разработать ТЗ и промпт для субагента Hermes на создание специализированного Telegram-бота/микросервиса (`.ai/tasks/2026-09-10-hermes-telegram-photo-bot-task.md` и `.ai/prompts/2026-09-10-telegram-avatar-bot-prompt.md`).
   - Предусмотреть в Java-сервисе HuntTech возможность обращения к такому внешнему микросервису при наличии настройки URL.

### Архитектурное решение (Аналитик и UI/UX-дизайнер)
1. **Компоновка UI в `person-edit.xml`**:
   - Внутри `groupBox id="personContactsSection"` в форме `personContactsForm` поле `telegramNameField` оборачивается в горизонтальный контейнер `hbox id="telegramBox" caption="mainMsg://msgTelegram" width="100%" spacing="true" expand="telegramNameField"`.
   - Поле ввода `telegramNameField` сохраняет привязку к сущности (`dataContainer="personDc"` и `property="telegramName"`), стиль `edit-form-control` и ширину `100%`.
   - Кнопка `loadTelegramPhotoButton`:
     - `caption="msg://msgLoadTelegramPhoto"` («Загрузить фото»)
     - `icon="font-icon:CAMERA"`
     - `stylename="secondary"`
     - `description="msg://msgLoadTelegramPhotoDesc"` («Загрузить фотографию профиля из Telegram»)
   - Динамическое управление состоянием: если поле Telegram пустое, кнопка неактивна или при нажатии выводит аккуратное уведомление.
2. **Получение фото профиля (Java Backend-разработчик)**:
   - В сервисе `TelegramIntegrationService` / `TelegramIntegrationServiceBean`:
     - Нормализация Telegram username (удаление префиксов `https://t.me/`, `t.me/`, знака `@`, обрезка пробелов).
     - Метод `saveUserProfilePhotoToFileStorage(String telegramIdOrUsername, String customFileName)` расширяется:
       1. Если задан `hunttech.telegram.avatarServiceUrl` (микросервис Hermes), выполняется запрос к нему.
       2. Если username публичный, выполняется загрузка публичной страницы `https://t.me/<cleanUsername>` с парсингом тега `og:image` / `tgme_page_photo_image` (с отсевом дефолтных иконок Telegram).
       3. Загрузка байтов изображения (JPG, PNG, WebP) с проверкой MIME-типа.
       4. Сохранение файла через `FileLoader` и регистрация `FileDescriptor` в `DataManager`.
   - В контроллере `PersonEdit.java`:
     - Регистрация клика `onLoadTelegramPhotoButtonClick`.
     - Слияние полученного дескриптора в `dataContext.merge(fd)`.
     - Присвоение `getEditedEntity().setFileImageFace(mergedFd)`.
     - Отображение в компоненте `personPic.setSource(...)`.

### Статус выполнения
- [x] Анализ требований и исследование публичного Telegram API
- [x] Разработка ТЗ и промпта для субагента Hermes (бот для фото)
- [x] Расширение `TelegramIntegrationServiceBean` для скачивания аватара по username
- [x] Обновление разметки `person-edit.xml` (кнопка правее поля Telegram)
- [x] Обновление контроллера `PersonEdit.java` (логика загрузки и отображения в OvalFallbackImage)
- [x] Локализация сообщений (`messages_ru.properties` и `messages.properties`)
- [x] Unit-тесты и контрактные тесты (`PersonEditLayoutContractTest`, `TelegramIntegrationServiceBeanTest`)
- [x] Валидация сборки через Gradle (`agent-gradle.sh`)
- [x] Проверка через Alibaba OCR (`ocr review --audience agent`: 4 замечания учтены и исправлены)
- [x] Git commit и push
