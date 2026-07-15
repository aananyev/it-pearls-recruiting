# Stage 11 — фоновая загрузка рейтинга кандидата

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `1d49e0e4e6598f162fe48cf4a0826e2431657271`  
**Тип этапа:** performance implementation

## 1. Основание

`JobCandidateEdit.onBeforeShow()` синхронно вызывает:

```java
setRatingLabel(getEditedEntity());
```

Текущий расчёт рейтинга выполняет скалярный middleware-запрос:

```jpql
select avg(e.rating + 1)
from hunttech_IteractionList e
where e.candidate.id = :candidateId
  and e.rating is not null
```

Рейтинг отображается в боковой карточке, но его получение не требуется для загрузки editable-полей, валидации или сохранения кандидата. Поэтому отдельный round-trip не должен блокировать first paint.

## 2. Цель

Удалить расчёт среднего рейтинга из `onBeforeShow()` и запустить его после отображения формы через `BackgroundTask`.

Бизнес-поведение должно сохраниться:

- новый кандидат получает прежнее представление нулевого рейтинга без SQL;
- существующий кандидат после фоновой загрузки видит то же количество звёзд и тот же текст, что до Stage 11;
- отсутствие оценок обрабатывается прежним fallback-значением;
- ошибка загрузки рейтинга не блокирует открытие и не закрывает форму;
- рейтинг не записывается в entity и не влияет на `DataContext`.

## 3. Разрешённые изменения

Разрешено изменить только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-rating-stage-11-background-load/
.ai/active-work.yml
```

Запрещено изменять:

- `job-candidate-edit.xml`;
- `StarsAndOtherService` и `StarsAndOtherServiceBean`;
- `InteractionService`;
- entities;
- `views.xml`;
- JPQL-смысл расчёта рейтинга;
- Liquibase, индексы и БД;
- component ID, captions и actions;
- SCSS;
- production-данные.

## 4. Точная реализация

### 4.1 Удалить запрос из `onBeforeShow`

Удалить вызов:

```java
setRatingLabel(getEditedEntity());
```

из `onBeforeShow()`.

После изменения initial open не должен выполнять SQL `avg(e.rating + 1)`.

### 4.2 Сохранить форматирование отдельно от загрузки

Перед изменением изучить текущую реализацию `setRatingLabel(...)` и разделить:

1. получение среднего значения;
2. применение результата к `candidateRatingLabel` через существующий `StarsAndOtherService`.

Формат звёзд, округление, fallback и caption менять запрещено.

Допустимая структура:

```java
private void applyRatingLabel(double averageRating) {
    // Здесь остаётся существующая логика отображения рейтинга без SQL.
}
```

Название может отличаться, но UI-метод не должен обращаться к `DataManager`.

### 4.3 Добавить идемпотентную фоновую загрузку

Добавить флаги:

```java
private boolean ratingLoading;
private boolean ratingLoaded;
```

Добавить метод с содержательным русским комментарием:

```java
private void startRatingBackgroundLoading() {
    if (ratingLoading || ratingLoaded) {
        return;
    }

    if (PersistenceHelper.isNew(getEditedEntity()) || getEditedEntity().getId() == null) {
        ratingLoaded = true;
        applyRatingLabel(0.0);
        return;
    }

    UUID candidateId = getEditedEntity().getId();
    ratingLoading = true;

    BackgroundTask<Void, Double> task =
            new BackgroundTask<Void, Double>(30, TimeUnit.SECONDS, this) {
                @Override
                public Double run(TaskLifeCycle<Void> taskLifeCycle) {
                    DataManager backgroundDataManager = AppBeans.get(DataManager.class);
                    Double average = backgroundDataManager.loadValue(
                            "select avg(e.rating + 1) " +
                                    "from hunttech_IteractionList e " +
                                    "where e.candidate.id = :candidateId " +
                                    "and e.rating is not null",
                            Double.class)
                            .parameter("candidateId", candidateId)
                            .optional()
                            .orElse(0.0);
                    return average != null ? average : 0.0;
                }

                @Override
                public void done(Double result) {
                    ratingLoading = false;
                    ratingLoaded = true;
                    applyRatingLabel(result != null ? result : 0.0);
                }

                @Override
                public boolean handleException(Exception exception) {
                    ratingLoading = false;
                    ratingLoaded = true;
                    log.error("Не удалось загрузить рейтинг кандидата, candidateId={}",
                            candidateId, exception);
                    applyRatingLabel(0.0);
                    return true;
                }
            };

    backgroundWorker.handle(task).execute();
}
```

Допустима эквивалентная реализация при выполнении условий:

- в background-поток передаётся только UUID и scalar-result;
- entity и UI-компоненты не передаются между потоками;
- `DataManager` получается через `AppBeans` внутри `run()`;
- UI обновляется только в `done()` или `handleException()`;
- повторный запуск в пределах экрана блокируется флагами;
- запрос и логика расчёта не меняются.

### 4.4 Точка запуска

Вызвать `startRatingBackgroundLoading()` из существующего `onAfterShow()`.

Не создавать второй `AfterShowEvent` subscriber. Использовать уже существующий метод, где запускается `startSkillsBackgroundLoading()`.

Рекомендуемый порядок:

```java
setPercentLabel();
setBlockUnblockButton(...);
startRatingBackgroundLoading();
startSkillsBackgroundLoading();
```

Порядок фоновых задач может быть эквивалентным. Они не должны ожидать друг друга.

### 4.5 Не менять обновление рейтинга после действий

Stage 11 оптимизирует только initial open. Не добавлять новую бизнес-логику пересчёта после создания/редактирования взаимодействия, если её не было ранее.

## 5. Документация

Обновить `docs/ui/JobCandidateEdit_Spec.md`:

- в Business & Context Intro указать, что рейтинг появляется после first paint;
- в Behavior Summary описать background scalar `AVG`;
- зафиксировать, что форматирование звёзд и бизнес-формула не изменились;
- добавить первую строку истории изменений с датой `2026-07-15`.

## 6. Обязательные проверки

### 6.1 Статические

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
```

Поиском подтвердить:

- `onBeforeShow()` не вызывает `setRatingLabel`, `loadAverageRating` или новый background-start;
- запрос `avg(e.rating + 1)` находится только в background `run()`;
- UI обновляется только на UI-потоке;
- форматирование через `StarsAndOtherService` сохранено;
- XML, views, entities, services и БД не изменены.

### 6.2 Автотесты

Обязательно:

```bash
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

`ScreenViewIntegrityTest`: 8/8 PASS.

Узкий unit-тест должен проверить минимум:

1. новый кандидат не запускает loader;
2. первый запуск существующего кандидата выполняет один scalar load;
3. повторный запуск не выполняет второй load;
4. `null` преобразуется в нулевой fallback;
5. ошибка фоновой загрузки не пробрасывается в UI lifecycle.

Если прямое тестирование `BackgroundTask` затруднено, вынести чистое нормализующее/форматирующее поведение в package-private helper без изменения UI-контракта.

### 6.3 SQL/runtime verification

| Сценарий | Ожидаемый результат |
|---|---|
| Открытие нового кандидата | 0 SQL рейтинга |
| Initial open существующего кандидата до first paint | 0 SQL рейтинга |
| После `AfterShow` | ровно 1 scalar `AVG` |
| Повторные UI-события в том же экране | 0 дополнительных `AVG` |
| Кандидат без оценок | fallback без ошибки |
| Ошибка middleware | форма остаётся открытой, ошибка записана в log |

Подтвердить, что запрос не загружает `IteractionList` entities и связанные views.

### 6.4 Ручной smoke-test

Проверить:

- форма открывается до появления рейтинга;
- рейтинг появляется после отображения формы;
- кандидат с оценками показывает прежнее число звёзд;
- кандидат без оценок показывает прежний fallback;
- новый кандидат открывается без SQL рейтинга;
- быстрое закрытие формы во время фоновой задачи не вызывает UI exception;
- другие фоновые задачи Skillsbar и Positions продолжают работать;
- отсутствуют detached, unfetched, NPE и ошибки доступа к UI из background thread;
- `/hrm` отвечает HTTP 200.

## 7. Acceptance gate

Stage 11 считается завершённым только при одновременном выполнении:

- функционального Java-коммита;
- diff только в разрешённом scope;
- обновления `JobCandidateEdit_Spec.md`;
- `ScreenViewIntegrityTest` 8/8;
- `clean assemble` — `BUILD SUCCESSFUL`;
- SQL-подтверждения отсутствия rating-query до first paint;
- ручного smoke-test;
- итогового отчёта Hermes с реальными SHA.

При отсутствии обязательного пункта выставить:

```text
STAGE_11_BLOCKED
```

## 8. Итоговый отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-rating-stage-11-background-load/
stage-11-rating-hermes-report.md
```

Отчёт должен содержать:

- реальный базовый и итоговый SHA;
- полный список изменённых файлов;
- старую и новую точки вызова;
- точный scalar JPQL;
- подтверждение неизменности формулы и форматирования;
- результаты compile, tests и assemble;
- SQL/runtime verification;
- ручной smoke-test;
- HTTP 200;
- итоговый вердикт.

## 9. Сообщение коммита

```text
perf(job-candidate): загружать рейтинг после открытия формы

- убрать scalar AVG из onBeforeShow
- обновлять звёзды после фоновой загрузки
- сохранить формулу и формат рейтинга
- обновить спецификацию JobCandidateEdit
```
