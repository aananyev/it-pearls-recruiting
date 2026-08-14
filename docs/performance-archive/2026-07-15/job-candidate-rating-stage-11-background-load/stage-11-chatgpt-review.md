# Stage 11 — review фоновой загрузки рейтинга

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `d8273da2f6a7b7bb3ce5c17fb1644d376244992e`  
**Итоговый SHA:** `89263c7950ee4a1489061824372e2b00c3722331`

## 1. Проверенный scope

Относительно базового SHA изменены только:

- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java`;
- `docs/ui/JobCandidateEdit_Spec.md`;
- `docs/performance-archive/2026-07-15/job-candidate-rating-stage-11-background-load/stage-11-rating-hermes-report.md`.

XML, entities, views, сервисы, Liquibase, индексы и темы не изменялись.

## 2. Проверка реализации

Подтверждено по коду:

- синхронный `setRatingLabel(...)` удалён из `onBeforeShow()`;
- `onAfterShow()` запускает `startRatingBackgroundLoading()`;
- задача защищена флагами `ratingLoading` и `ratingLoaded`;
- новый кандидат получает прежний нулевой рейтинг без SQL;
- в `BackgroundTask.run()` используется `AppBeans.get(DataManager.class)`;
- сохранён JPQL `avg(e.rating + 1)` и условие `e.rating is not null`;
- UI обновляется через `applyRatingLabel(...)` только в `done()` или `handleException()`;
- параллельная фоновая загрузка Skillsbar не объединена с задачей рейтинга.

Метод `loadAverageRating()` остаётся активным только в listener закрытия редактора взаимодействия. Это пользовательский сценарий после изменения оценки, а не critical path первоначального открытия формы.

## 3. Проверки

Hermes и пользователь подтвердили:

- `VERIFICATION: PASS (10/10)`;
- `compileJava` — `BUILD SUCCESSFUL`;
- приложение развернуто;
- `/hrm` отвечает HTTP 200;
- спецификация обновлена.

Отчёт Hermes содержит placeholder `$(git rev-parse HEAD)` вместо фактического SHA и сокращённый список проверок. Фактический итоговый SHA установлен из ветки и зафиксирован в этом review.

## 4. Вердикт

```text
STAGE_11_ACCEPTED
```

Расчёт среднего рейтинга больше не блокирует first paint. Формула рейтинга, отображение звёзд и пересчёт после редактирования взаимодействия сохранены.

## 5. Следующий участок critical path

`onBeforeShow()` продолжает синхронно вызывать `setCandidatePicImage()`.

Текущий метод:

1. вызывает `FileDescriptorImageHelper.fileExists(...)`;
2. затем вызывает `FileDescriptorImageHelper.setCandidateFace(...)`;
3. `setCandidateFace(...)` повторно вызывает `fileExists(...)` внутри `setImageSource(...)`.

Таким образом, кандидат с фотографией может выполнять две проверки файлового хранилища до first paint. Следующий этап должен вынести проверку фотографии после отображения формы и не выполнять повторный storage-check на UI-потоке.
