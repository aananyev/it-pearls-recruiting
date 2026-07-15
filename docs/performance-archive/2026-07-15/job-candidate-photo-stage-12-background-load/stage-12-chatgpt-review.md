# Stage 12 — review фоновой загрузки фотографии

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `2e2c4f1148936ff1ffe16dd0688a93755b9a1b84`  
**Итоговый SHA Hermes:** `b261c26edf1ba47a9f85e910a95c0e091f8f3777`

## 1. Проверенный scope

Между базовым SHA и итоговым состоянием изменены только:

- `JobCandidateEdit.java`;
- `docs/ui/JobCandidateEdit_Spec.md`;
- отчёт Stage 12.

XML, `FileDescriptorImageHelper`, entities, views, core-сервисы, Liquibase и темы не изменялись.

## 2. Проверка реализации

Подтверждено:

- `setCandidatePicImage()` удалён из `onBeforeShow()`;
- до first paint вызывается только `showCandidatePicPlaceholder()`, не использующий `FileLoader`;
- `startCandidatePicBackgroundLoading()` защищён флагами `candidatePicLoading` и `candidatePicLoaded`;
- новый кандидат и кандидат без `FileDescriptor` не запускают фоновую проверку;
- в `run()` передаётся UUID `FileDescriptor`, descriptor точечно загружается через `_minimal`;
- физическое наличие файла проверяется одним вызовом `FileDescriptorImageHelper.fileExists()`;
- `done()` напрямую устанавливает `FileDescriptorResource` и не вызывает `setCandidateFace()`, поэтому второй storage-check отсутствует;
- изменение source защищено `updatingCandidatePic`;
- прежний `setCandidatePicImage()` сохранён для пользовательских событий загрузки/изменения фотографии и не участвует в initial-open.

## 3. Верификация

Пользователем и Hermes подтверждены:

- корректная заглушка до завершения background task;
- фоновая проверка фотографии;
- отсутствие повторного `fileExists()` в `done()`;
- синхронизация спецификации;
- успешная компиляция;
- HTTP 200;
- ручная верификация Stage 12.

Отчёт Hermes содержит placeholder `$(git rev-parse HEAD)` и сокращённую таблицу проверок. Поэтому фактический SHA и выводы зафиксированы настоящим review на основании diff, кода и пользовательской верификации.

## 4. Вердикт

```text
STAGE_12_ACCEPTED
```

Проверка file storage фотографии исключена из пути до first paint. Итоговое отображение фотографии и fallback-поведение сохранены.