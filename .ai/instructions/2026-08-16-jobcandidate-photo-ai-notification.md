# Задание: нотификация об AI-обработке фотографии в JobCandidateEdit

Дата: 2026-08-16
Автор: Hermes-2 (ветка agent/hermes2-dev)
Адресат: Hermes-1 (проверка PR, merge, deploy)

## Что сделано

В форме `JobCandidateEdit` после применения алгоритма удаления фона (нейросеть
rembg/u2net) пользователю показывается исчезающая TRAY-нотификация стандартными
средствами CUBA: «Фотография обработана с помощью AI» / «Фон удалён автоматически
нейросетью» (правый нижний угол, исчезает сама).

Реализация — в общем компоненте `WebProjectLogoFileUploadField` (без изменения
XML и контроллера формы), с честной семантикой: нотификация показывается ТОЛЬКО
когда фон действительно удалён нейросетью, а не при простой конвертации/ресайзе.

Изменения:

1. `modules/global/src/com/company/hunttech/app/ProcessedImage.java` — добавлен
   флаг `aiProcessed` (геттер + 5-арг конструктор; 4-арг делегирует с `false`).
2. `modules/core/src/com/company/hunttech/app/ProjectLogoImageProcessingServiceBean.java`
   — `aiProcessed=true` только когда результат rembg (или AI-функции для логотипов)
   реально применён; классический flood-fill и простые конвертация/ресайз флаг НЕ ставят.
3. `modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java`
   — после успешной обработки в режиме `CANDIDATE_PHOTO` (`fileImageFace`) при
   `processedByAi` показывается нотификация через `AppUI.getCurrent().getNotifications()`
   (`Notifications.NotificationType.TRAY`, стандартный механизм CUBA). Для логотипов
   (`projectLogo`, `fileCompanyLogo`) нотификация не показывается — там фон может быть
   удалён классикой, и утверждение «обработано AI» было бы некорректным.
4. `modules/core/test/.../ProjectLogoRembgServiceBeanTest.java` — 3 существующих
   теста дополнены ассертами на `isAiProcessed()` + 2 новых теста: фото кандидата
   с rembg → `aiProcessed=true`; фото кандидата без rembg (только PNG+ресайз) →
   `aiProcessed=false`.
5. `docs/ui/JobCandidateEdit_Spec.md` — раздел «Левая панель», контракт
   `fileImageFaceUpload`, «История изменений» (2026-08-16); заодно исправлено
   описание конвейера фото (для людей — только rembg, без AI-функции/классики/круга).

Поведение: нотификация срабатывает во всех формах загрузки фото кандидата
(JobCandidateEdit, CandidateCVEdit, PersonEdit — общее свойство `fileImageFace`).
Если rembg недоступен — фото только конвертируется/ресайзится, нотификация
НЕ показывается. Загрузка никогда не ломается (прежний принцип бесшовного отката).

## Как проверено

- `:app-web:compileJava` — BUILD SUCCESSFUL.
- `:app-core:test` — `ProjectLogoRembgServiceBeanTest` 6/6 (включая 2 новых),
  `ProjectLogoImageProcessingServiceBeanTest` 7/7, `ImageProcessingServiceBeanTest`,
  `ScreenViewIntegrityTest`, `JobCandidateEditLayoutContractTest` — 0 failures
  (через `scripts/agent-gradle.sh`, сериализация соблюдена).

## Что ожидается от Hermes-1

- Проверить PR (база master, ветка agent/hermes2-dev, метка WAITING_FOR_HERMES).
- После merge + deploy + restart: smoke-проверка в браузере — загрузка фото
  кандидата в JobCandidateEdit с работающим rembg показывает исчезающую нотификацию
  «Фотография обработана с помощью AI»; без rembg (сервис остановлен) нотификации
  нет, фото сохраняется; загрузки логотипов в ProjectEdit/CompanyEdit и других
  файлов не затронуты.
