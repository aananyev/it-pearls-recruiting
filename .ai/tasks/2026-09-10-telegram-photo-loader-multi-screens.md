# Задача: Кнопка загрузки фотографии из Telegram в экранах JobCandidateEdit и ExtUserEdit

## Дата: 2026-09-10
## Ветка: agent/antigravity-dev
## Роли: Аналитик, UI/UX-дизайнер, Java Backend-разработчик, Автоматизированный тестировщик

### 1. Описание задачи
1. В экранной форме редактирования кандидата `JobCandidateEdit` (`job-candidate-edit.xml`, `JobCandidateEdit.java`) в карточке контактных данных рядом с полем ввода Telegram username (`telegramNameField`) разместить кнопку «Загрузить фото» (`loadTelegramPhotoButton`).
2. При нажатии на кнопку:
   - Получить фото профиля из Telegram через `TelegramIntegrationService.saveUserProfilePhotoToFileStorage(telegramName, fileName)`.
   - Сохранить полученный `FileDescriptor` в `jobCandidate.fileImageFace` через `dataContext.merge(photoFd)`.
   - Отобразить фото в овальном аватаре `candidatePic` (`OvaFallbackImage`) в левом sidebar.
   - Если привязан загрузчик `fileImageFaceUpload`, обновить его значение.
   - Вывести уведомление об успешной загрузке, отсутствии фото или ошибке.
3. В экранной форме редактирования пользователя `ExtUserEdit` (`ext-user-edit.xml`, `ExtUserEditor.java`):
   - Привести кнопку в строке Telegram к аналогичному стандарту (`loadTelegramPhotoButton`, caption «Загрузить фото», icon `font-icon:CAMERA`, style `secondary`).
   - Убрать устаревшую блокирующую проверку `!telegramIntegrationService.isConfigured()`, которая запрещала скачивать публичные фото по username без настроенного токена бота.
   - Связать успешную загрузку с мгновенным обновлением овального аватара `userPic` (`OvaFallbackImage`, 176×176) в левом sidebar карточки пользователя.
   - Синхронизировать `officialPhoto` и `userAvatar` в `ExtUser`.
4. Обеспечить Data View Integrity (все атрибуты объявлены во view `_local` / XML).
5. Создать контрактные тесты и выполнить валидацию через Alibaba OCR (`ocr review --audience agent`).
