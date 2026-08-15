# Задание: умная обработка фотографии кандидата в JobCandidateEdit

Дата: 2026-08-15
Автор: Hermes-2 (ветка agent/hermes2-dev)
Адресат: Hermes-1 (проверка PR, merge, deploy)

## Что сделано

Добавлено действие обработки фотографий в форму `JobCandidateEdit` по аналогии
с логотипами ProjectEdit и CompanyEdit (коммиты 410ce5fd, 38dd5947, 2b415646).

Форма уже использовала кастомный компонент `<upload>` (регистрируется в
`cuba-ui-component.xml` как замена `WebFileUploadField`), поле
`fileImageFaceUpload` привязано к `property="fileImageFace"`. Обработка не
срабатывала, потому что `WebProjectLogoFileUploadField.isLogoField()` знал только
свойства `projectLogo` и `fileCompanyLogo`.

Изменения:

1. `modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java`
   — добавлена константа `CANDIDATE_PHOTO_PROPERTY = "fileImageFace"`, включена
   в `isLogoField()`; javadoc и debug-лог обобщены (изображение, а не только логотип).
2. `modules/web/src/com/hunttech/hrm/web/cuba-ui-component.xml` — комментарий
   регистрации обновлён (projectLogo / fileCompanyLogo / fileImageFace).
3. `modules/global/src/com/company/hunttech/app/ProjectLogoImageProcessingService.java`
   — javadoc интерфейса обобщён на фотографию кандидата.
4. `docs/ui/JobCandidateEdit_Spec.md` — синхронизирована: раздел «Левая панель»,
   таблица «Actions и неизменяемые контракты» (`fileImageFaceUpload`), запись в
   «Истории изменений» (2026-08-15).

Конвейер тот же, что для логотипов: PNG, ресайз до 300×300 (конфиг
`hunttech.projectLogo.maxSize`), удаление белого фона (rembg → AI → классика),
вписывание в круг. Управляется общим конфигом `hunttech.projectLogo.*`
(включая `enabled=false` — полное отключение).

XML формы и контроллер `JobCandidateEdit.java` НЕ менялись.

## Как проверено

- `:app-web:compileJava` — BUILD SUCCESSFUL.
- `:app-core:test` — `ScreenViewIntegrityTest` 8/8, `JobCandidateEditLayoutContractTest`
  13/13, `ProjectLogoImageProcessingServiceBeanTest` 7/7 — 0 failures (через
  `scripts/agent-gradle.sh`, сериализация соблюдена).

## Что ожидается от Hermes-1

- Проверить PR (база master, ветка agent/hermes2-dev, метка WAITING_FOR_HERMES).
- После merge + deploy + restart: smoke-проверка в браузере — загрузка фото
  кандидата в JobCandidateEdit проходит конвейер (превью — обработанный PNG),
  повторная загрузка в том же экране обрабатывается заново; загрузки файлов в
  других экранах не затронуты.
