# Stage 13 — фоновая проверка индикатора резюме

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `24d9e029ed95a0fc170de1046c71c9a71aea1aec`  
**Тип этапа:** performance and fetch-safety implementation

## 1. Основание

После Stage 10 метод `hasCandidateCv()` больше не материализует коллекцию `candidateCv`, но по-прежнему синхронно вызывается из `onBeforeShow()` и выполняет scalar `COUNT` до first paint:

```jpql
select count(e)
from hunttech_CandidateCV e
where e.candidate.id = :candidateId
  and e.deleteTs is null
```

Для начального редактирования кандидата этот результат нужен только для информационной надписи «Резюме: ДА/НЕТ». Он не участвует в валидации, сохранении или построении editable-полей.

## 2. Цель

Удалить scalar `COUNT` наличия CV из пути до first paint и выполнять его отдельной фоновой задачей после `AfterShow`.

Итоговое бизнес-поведение сохраняется:

- существует хотя бы одно неудалённое резюме → `Резюме: ДА`;
- резюме отсутствует → `Резюме: НЕТ`;
- новый кандидат → запрос не выполняется;
- полная коллекция `CandidateCV` загружается только при первом открытии вкладки «Резюме»;
- сохранение кандидата без открытия вкладки не изменяет существующие CV.

## 3. Разрешённые изменения

Разрешено изменять только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-cv-indicator-stage-13-background-load/
.ai/active-work.yml
```

Запрещено изменять:

- `job-candidate-edit.xml`;
- `JobCandidateCvInitialViewOptimizer`;
- `ensureCandidateCvLoaded()`;
- entities `JobCandidate` и `CandidateCV`;
- `views.xml`;
- core-сервисы;
- Liquibase, индексы и БД;
- component ID, actions и captions;
- SCSS и темы;
- production-данные.

## 4. Точная реализация

### 4.1 Удалить синхронную проверку из `onBeforeShow()`

Удалить вызов `hasCandidateCv()` и ветку, выполняющую `COUNT` до отображения формы.

Для существующего кандидата до завершения background task допустимо установить нейтральное состояние:

```java
labelCV.setValue("Резюме: …");
```

Для нового кандидата сохранить прежнее поведение: SQL не выполняется, коллекция не читается.

### 4.2 Разделить загрузку и UI

Добавить отдельный метод скалярной загрузки, который не обращается к экрану:

```java
private boolean loadCandidateCvExists(DataManager backgroundDataManager, UUID candidateId) {
    Long count = backgroundDataManager.loadValue(
            "select count(e) from hunttech_CandidateCV e " +
                    "where e.candidate.id = :candidateId and e.deleteTs is null",
            Long.class)
            .parameter("candidateId", candidateId)
            .one();
    return count != null && count > 0;
}
```

Допустима эквивалентная реализация при обязательном сохранении:

- scalar `COUNT`;
- параметра UUID вместо detached entity;
- явного условия `e.deleteTs is null`;
- отсутствия загрузки `CandidateCV` entities, `textCV`, файлов и associations.

Добавить UI-only метод:

```java
private void applyCandidateCvIndicator(boolean hasCv) {
    labelCV.setValue(hasCv ? "Резюме: ДА" : "Резюме: НЕТ");
}
```

### 4.3 Фоновая задача

Добавить независимые флаги:

```java
private boolean candidateCvIndicatorLoading;
private boolean candidateCvIndicatorLoaded;
```

Добавить идемпотентный `startCandidateCvIndicatorBackgroundLoading()`:

- при `loading || loaded` немедленно завершаться;
- для нового кандидата или `id == null` не выполнять SQL;
- передавать в background только UUID кандидата;
- внутри `run()` получать `DataManager` через `AppBeans.get(DataManager.class)`;
- не обращаться в `run()` к UI, `getEditedEntity()`, `DataContext` и entity-графу кандидата;
- выполнять ровно один scalar `COUNT`;
- в `done()` устанавливать `ДА/НЕТ` и завершать флаги;
- в `handleException()` логировать ошибку, оставлять форму открытой и не подменять ошибку ложным значением «НЕТ»;
- не объединять эту задачу с rating, Skillsbar, photo или positions background task.

Вызвать метод из существующего `onAfterShow()`.

### 4.4 Сохранить lazy-contract вкладки

Не изменять:

- `candidateCvLoaded`;
- `ensureCandidateCvLoaded()`;
- `cvTabInitialized`;
- запрос полной загрузки CV;
- merge коллекции в `DataContext`;
- обработчики вкладки «Резюме».

Scalar indicator не должен устанавливать `candidateCvLoaded=true` и не должен записывать пустую коллекцию в `JobCandidate`.

## 5. Комментарии

Добавить содержательные комментарии на русском:

- почему scalar-проверка запускается после first paint;
- почему используются UUID и отдельный `DataManager`;
- почему результат индикатора не меняет состояние коллекции CV.

## 6. Документация

Обновить `docs/ui/JobCandidateEdit_Spec.md`:

- Business & Context Intro / Behavior Summary: индикатор резюме определяется после first paint scalar-запросом;
- Stage 2: `COUNT` выполняется фоновой задачей, полная коллекция — только на `tabResume`;
- правила загрузки;
- первую строку истории изменений с датой `2026-07-15`.

## 7. Обязательные проверки

### 7.1 Статические

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
```

Подтвердить поиском:

- `onBeforeShow()` не вызывает `hasCandidateCv()` и не содержит `CandidateCV COUNT`;
- scalar `COUNT` расположен только в background-load методе;
- `getCandidateCv()` отсутствует в indicator flow;
- `ensureCandidateCvLoaded()` не изменён;
- XML, views, entities, core и themes не изменены.

### 7.2 Автотесты

```bash
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Требование: `ScreenViewIntegrityTest` — 8/8 PASS, итоговый build — `BUILD SUCCESSFUL`.

Узкий unit-тест допускается и должен проверять минимум:

1. новый кандидат не запускает loader;
2. count `0` → `Резюме: НЕТ`;
3. count `1+` → `Резюме: ДА`;
4. повторный запуск не создаёт второй запрос;
5. ошибка не закрывает форму и не выставляет ложное «НЕТ».

### 7.3 Runtime/SQL verification

| Сценарий | До first paint | После `AfterShow` |
|---|---:|---:|
| Новый кандидат | 0 CandidateCV SQL | 0 |
| Существующий без CV | 0 | ровно 1 scalar COUNT |
| Существующий с CV | 0 | ровно 1 scalar COUNT |
| Повторное событие `AfterShow` | 0 | 0 дополнительных COUNT |
| Открытие вкладки «Резюме» | 0 indicator entity-load | отдельная загрузка через `candidateCV-browse-view` |

Подтвердить отсутствие выборки `textCV`, файлов и `CandidateCV` entities в indicator flow.

### 7.4 Ручной smoke-test

Проверить:

- кандидат с CV после загрузки показывает `Резюме: ДА`;
- кандидат без CV показывает `Резюме: НЕТ`;
- новый кандидат открывается без SQL и ошибок;
- вкладка «Резюме» отображает существующие записи;
- сохранение без открытия вкладки не удаляет CV;
- добавление CV и повторное открытие карточки обновляет индикатор;
- быстрое закрытие формы не создаёт UI-thread exception;
- параллельные background-задачи рейтинга, фото и Skillsbar не конфликтуют;
- отсутствуют `unfetched`, detached, NPE и OOM;
- `/hrm` отвечает HTTP 200.

## 8. Acceptance gate

Stage 13 принимается только при одновременном выполнении:

- функционального Java-коммита;
- diff только в разрешённом scope;
- обновления полной спецификации, а не только истории;
- compile и tests;
- `ScreenViewIntegrityTest` 8/8;
- `clean assemble` — `BUILD SUCCESSFUL`;
- runtime-таблицы вызовов;
- ручного smoke-test;
- HTTP 200;
- итогового отчёта с фактическими SHA.

При отсутствии любого обязательного пункта:

```text
STAGE_13_BLOCKED
```

## 9. Итоговый отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-cv-indicator-stage-13-background-load/
stage-13-cv-indicator-hermes-report.md
```

Отчёт должен содержать:

- реальный базовый и итоговый SHA без shell-placeholder;
- полный список изменённых файлов;
- точный JPQL;
- результаты compile/test/assemble;
- `ScreenViewIntegrityTest` 8/8;
- runtime-таблицу COUNT-вызовов;
- ручной smoke-test;
- HTTP 200;
- итоговый вердикт.

## 10. Сообщение коммита

```text
perf(job-candidate): загружать индикатор резюме после first paint

- убрать scalar COUNT из onBeforeShow
- сохранить ленивую загрузку полной коллекции CV
- обновить спецификацию JobCandidateEdit
```