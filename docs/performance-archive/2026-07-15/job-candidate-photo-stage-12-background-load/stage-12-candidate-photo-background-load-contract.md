# Stage 12 — фоновая проверка и отображение фотографии кандидата

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `714326b8140da2e7df8e82982e9a2befcd08127e`  
**Тип этапа:** performance and file-storage safety implementation

## 1. Основание

`JobCandidateEdit.onBeforeShow()` синхронно вызывает `setCandidatePicImage()`.

Для кандидата с фотографией текущий путь выполняет:

1. `FileDescriptorImageHelper.fileExists(fileLoader, faceImage)`;
2. затем `FileDescriptorImageHelper.setCandidateFace(...)`;
3. внутри `setCandidateFace()` метод `setImageSource()` повторно вызывает `fileExists(...)`.

Проверка файлового хранилища может обращаться к middleware или удалённому storage. Она не должна блокировать first paint карточки кандидата.

## 2. Цель

Убрать проверку физического файла фотографии из `onBeforeShow()` и выполнять её после отображения формы через отдельный `BackgroundTask`.

Бизнес-поведение сохраняется:

- фотография существует → отображается фотография кандидата;
- `FileDescriptor` отсутствует → отображается стандартная заглушка;
- метаданные есть, физический файл отсутствует → отображается стандартная заглушка без падения формы;
- новый кандидат → заглушка, без SQL и без обращения к файловому хранилищу;
- загрузка и очистка фотографии пользователем продолжают работать по прежним actions и events.

## 3. Разрешённый scope

Разрешено изменить только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-photo-stage-12-background-load/
.ai/active-work.yml
```

Запрещено изменять:

- `job-candidate-edit.xml`;
- `FileDescriptorImageHelper.java`;
- `JobCandidate` и `FileDescriptor` entities;
- `views.xml`;
- сервисы;
- Liquibase, индексы и БД;
- component ID, captions и actions;
- SCSS и темы;
- production-данные.

## 4. Точная реализация

### 4.1 Удалить storage I/O из `onBeforeShow()`

Из `onBeforeShow()` удалить вызов:

```java
setCandidatePicImage();
```

Вместо него синхронно установить только безопасное начальное состояние UI без `FileLoader`:

- `candidateDefaultPic` видим;
- `candidatePic` скрыт;
- не вызывать `fileExists()`;
- не создавать `FileDescriptorResource` до результата фоновой проверки.

Допустим отдельный метод `showCandidatePicPlaceholder()` с содержательным русским комментарием.

### 4.2 Добавить флаги задачи

Добавить отдельные флаги:

```java
private boolean candidatePicLoading;
private boolean candidatePicLoaded;
```

Флаг `updatingCandidatePic` сохранить для защиты от рекурсивного `SourceChangeEvent`.

### 4.3 Добавить `startCandidatePicBackgroundLoading()`

Метод вызывается из существующего `onAfterShow()` и должен быть идемпотентным.

Обязательная логика:

1. Если `candidatePicLoading || candidatePicLoaded` — выйти.
2. Получить `FileDescriptor faceImage = getEditedEntity().getFileImageFace()` на UI-потоке.
3. Если кандидат новый, ID кандидата отсутствует или `faceImage == null`:
   - выставить `candidatePicLoaded = true`;
   - оставить заглушку;
   - не выполнять SQL и `fileExists()`.
4. Передать в background только UUID `FileDescriptor`.
5. В `BackgroundTask.run()`:
   - получить `DataManager` и `FileLoader` через `AppBeans`;
   - точечно загрузить `FileDescriptor` по UUID;
   - выполнить ровно один `FileDescriptorImageHelper.fileExists(...)`;
   - вернуть только скалярный результат наличия файла.
6. В `done()` на UI-потоке:
   - сбросить `candidatePicLoading`;
   - установить `candidatePicLoaded = true`;
   - если файл существует, напрямую установить `FileDescriptorResource` для исходного `faceImage`;
   - не вызывать `setCandidateFace()`, потому что он повторно проверит storage;
   - переключить видимость `candidateDefaultPic/candidatePic`.
7. В `handleException()`:
   - сбросить `candidatePicLoading`;
   - установить `candidatePicLoaded = true`;
   - оставить заглушку;
   - записать ошибку в лог;
   - не закрывать форму и не показывать блокирующий диалог.

### 4.4 UI применяется только на UI-потоке

Запрещено в `run()`:

- обращаться к `candidatePic` или `candidateDefaultPic`;
- менять visibility;
- создавать Vaadin/CUBA UI resources;
- обращаться к `getEditedEntity()`;
- передавать `JobCandidate` в background.

### 4.5 Защита `SourceChangeEvent`

При установке `FileDescriptorResource` в `done()` обернуть UI-обновление в `updatingCandidatePic = true/false`, чтобы `onCandidatePicSourceChange()` не запускал повторную синхронную проверку.

Сценарии загрузки и очистки фотографии пользователем не переносить в background и не менять функционально.

### 4.6 Не объединять фоновые задачи

Задача фотографии остаётся отдельной от:

- `startRatingBackgroundLoading()`;
- `startSkillsBackgroundLoading()`;
- загрузки вкладки «Позиции и вакансии».

Не создавать общий executor/result object для разных бизнес-блоков.

## 5. Документация

Обновить `docs/ui/JobCandidateEdit_Spec.md`:

- в Business & Context Intro указать, что фотография не проверяется в storage до first paint;
- в Behavior Summary описать: заглушка → AfterShow → одна фоновая проверка → фотография или сохранение заглушки;
- добавить технический раздел Stage 12;
- добавить первой строкой историю изменений `2026-07-15`.

## 6. Обязательные проверки

### 6.1 Статические

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
```

Поиском подтвердить:

- `onBeforeShow()` не вызывает `setCandidatePicImage()` и `fileExists()`;
- `startCandidatePicBackgroundLoading()` вызывается из `onAfterShow()`;
- в `run()` отсутствуют UI-компоненты;
- `done()` не вызывает `setCandidateFace()`;
- `FileDescriptorImageHelper.java` не изменён;
- XML и views не изменены.

### 6.2 Автотесты

```bash
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

`ScreenViewIntegrityTest`: 8/8 PASS.

Узкий unit-тест должен проверить минимум:

1. новый кандидат не запускает storage-check;
2. кандидат без `FileDescriptor` не запускает storage-check;
3. существующий файл приводит к показу фотографии;
4. отсутствующий файл оставляет заглушку;
5. повторный вызов не запускает вторую задачу;
6. exception оставляет заглушку и закрывает задачу корректно.

### 6.3 Runtime/storage verification

С логированием или счётчиком вызовов подтвердить:

| Сценарий | До first paint | После `AfterShow` |
|---|---:|---:|
| Новый кандидат | 0 `fileExists` | 0 `fileExists` |
| Кандидат без фотографии | 0 | 0 |
| Кандидат с существующим файлом | 0 | ровно 1 |
| Метаданные есть, файла нет | 0 | ровно 1 |
| Повторный `AfterShow`/event | 0 | 0 дополнительных |

Отдельно подтвердить отсутствие второго `fileExists()` после успешной фоновой проверки.

### 6.4 Ручной smoke-test

Проверить:

- карточка сначала открывается с заглушкой и остаётся интерактивной;
- существующая фотография появляется после открытия;
- кандидат без фото остаётся с заглушкой;
- запись с отсутствующим физическим файлом не вызывает ошибку UI;
- новый кандидат открывается с заглушкой;
- загрузка нового фото работает;
- очистка фото возвращает заглушку;
- быстрое закрытие формы не вызывает UI-thread exception;
- параллельные background-задачи рейтинга и Skillsbar не конфликтуют;
- нет `detached`, `unfetched`, NPE и ошибок FileStorage в UI;
- `/hrm` отвечает HTTP 200.

## 7. Acceptance gate

Stage 12 считается завершённым только при одновременном выполнении:

- функционального Java-коммита;
- diff только в разрешённом scope;
- обновления `JobCandidateEdit_Spec.md`;
- `ScreenViewIntegrityTest` 8/8;
- `clean assemble` — `BUILD SUCCESSFUL`;
- runtime-подтверждения нуля storage-вызовов до first paint;
- подтверждения ровно одного `fileExists()` после `AfterShow` для записи с фото;
- ручного smoke-test;
- HTTP 200;
- итогового отчёта с фактическими SHA.

При отсутствии обязательного пункта установить:

```text
STAGE_12_BLOCKED
```

## 8. Итоговый отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-photo-stage-12-background-load/
stage-12-candidate-photo-hermes-report.md
```

Отчёт должен содержать:

- фактические базовый и итоговый SHA без shell-placeholder;
- полный список изменённых файлов;
- статический call graph фотографии;
- таблицу количества `fileExists()` по сценариям;
- результаты compile, ScreenViewIntegrityTest и assemble;
- результаты ручного smoke-test;
- HTTP 200;
- итоговый вердикт.

## 9. Сообщение коммита

```text
perf(job-candidate): загружать фотографию после first paint

- убрать проверку file storage из onBeforeShow
- выполнить одну фоновую проверку наличия фотографии
- сохранить заглушку и пользовательские сценарии загрузки фото
- обновить спецификацию JobCandidateEdit
```
