# Stage 15 — запрет фонового разбора CV для скрытого Skillsbar

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `092633eef6b089b6afe70e90c88e7383daa336f0`  
**Тип этапа:** performance and memory-safety implementation

## 1. Основание

В XML экрана компонент `skillBox` имеет `visible="false"`. Несмотря на это, `onAfterShow()` безусловно вызывает `startSkillsBackgroundLoading()`.

Текущий background-flow:

1. выбирает последнее `CandidateCV.textCV`;
2. выполняет `Jsoup.parse(...).text()`;
3. вызывает `PdfParserService.parseSkillTree()`;
4. для каждого навыка вызывает `ParseCVService.countMachesSkill()`;
5. строит `SkillLabelData` и фрагмент `Skillsbar`;
6. добавляет результат в скрытый контейнер.

Для текущего UI вычисление не создаёт видимого результата, но потребляет SQL, heap, CPU и сервисные вызовы после каждого открытия существующего кандидата. Этот путь особенно опасен с учётом ранее зафиксированного `OutOfMemoryError` при работе с CV.

## 2. Цель

Когда `skillBox` скрыт:

- не выполнять запрос `CandidateCV.textCV`;
- не читать LOB резюме;
- не запускать `Jsoup`, `PdfParserService` и `ParseCVService`;
- не создавать `Skillsbar` и `SkillLabelData`;
- не менять видимое поведение формы.

Если `skillBox` станет видимым до `AfterShow`, существующий background-flow должен продолжить работать.

## 3. Разрешённый scope

Разрешено изменять только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-skills-stage-15-hidden-component-guard/
.ai/active-work.yml
```

Запрещено изменять:

- `job-candidate-edit.xml` и значение `skillBox.visible`;
- `Skillsbar` и `SkillLabelData`;
- `PdfParserService` и `ParseCVService`;
- entities и `views.xml`;
- core-сервисы;
- Liquibase, индексы и БД;
- component ID, actions, captions и layout;
- SCSS и темы;
- production-данные.

## 4. Требуемая реализация

### 4.1 Visibility guard

В начале `startSkillsBackgroundLoading()` после проверки `skillsLoading || skillsLoaded` добавить явную проверку:

```java
// Скрытый Skillsbar не должен инициировать чтение и разбор полного текста резюме.
if (skillBox == null || !skillBox.isVisible()) {
    return;
}
```

Требования:

- guard выполняется до чтения кандидата, UUID и установки `skillsLoading`;
- при скрытом компоненте флаги не переводятся в состояние загрузки;
- при видимом компоненте существующая логика выполняется один раз;
- guard не должен менять `visible` компонента.

### 4.2 Thread-safe DataManager flow

Сейчас `run()` сначала вызывает `loadLastCvText(candidateId)`, использующий injected `dataManager`, и только затем получает `DataManager` через `AppBeans`.

Исправить порядок:

```java
DataManager backgroundDataManager = AppBeans.get(DataManager.class);
String cvText = loadLastCvText(backgroundDataManager, candidateId);
```

Изменить helper так, чтобы он использовал переданный background `DataManager`:

```java
private String loadLastCvText(DataManager backgroundDataManager, UUID candidateId)
```

или выполнить эквивалентный scalar-запрос непосредственно внутри `run()`.

Запрещено обращаться из `run()` к:

- injected `dataManager` контроллера;
- `getEditedEntity()`;
- UI-компонентам;
- `DataContext`;
- экранным entity-графам.

### 4.3 Сохранить существующую логику для видимого компонента

Не изменять:

- JPQL выбора последнего `textCV`;
- сортировку `order by e.datePost desc`;
- `maxResults(1)`;
- преобразование HTML в plain text;
- поиск и приоритизацию навыков;
- дедупликацию;
- стили меток;
- создание фрагмента в `done()`;
- обработку исключения;
- таймаут background task.

Не объединять задачу Skillsbar с background-задачами фотографии, CV indicator, рейтинга или вкладки позиций.

## 5. Комментарии

Добавить содержательные комментарии на русском:

- почему скрытый компонент не должен инициировать LOB/CPU-нагрузку;
- почему `DataManager` получается через `AppBeans` внутри background thread;
- почему UI создаётся только в `done()`.

Существующие англоязычные комментарии в изменяемом блоке привести к русскому языку, чтобы не смешивать языки внутри одного метода.

## 6. Документация

Обновить `docs/ui/JobCandidateEdit_Spec.md`:

- Behavior Summary: скрытый Skillsbar не запускает background-разбор CV;
- модель загрузки: `textCV` читается только при видимом `skillBox`;
- раздел контроля качества;
- первая строка истории изменений `2026-07-15`.

## 7. Проверки

### 7.1 Статические

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
```

Подтвердить:

- visibility-guard расположен до UUID и `skillsLoading = true`;
- background `run()` не использует injected `dataManager`;
- XML не изменён;
- сервисы, entities, views и темы не изменены.

### 7.2 Автотесты

```bash
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Требования:

- `ScreenViewIntegrityTest` — 8/8 PASS;
- итоговый build — `BUILD SUCCESSFUL`.

Узкий unit-тест должен подтвердить минимум:

1. `skillBox.visible=false` → background task не создаётся;
2. скрытый компонент → CV loader не вызывается;
3. `skillBox.visible=true` → задача создаётся один раз;
4. повторный вызов при `skillsLoading` или `skillsLoaded` не создаёт вторую задачу;
5. новый кандидат не выполняет CV SQL.

### 7.3 Runtime verification

| Сценарий | `textCV` SQL | Parse service | UI fragment |
|---|---:|---:|---:|
| Текущий XML, существующий кандидат | 0 | 0 | 0 |
| Текущий XML, новый кандидат | 0 | 0 | 0 |
| Искусственно видимый `skillBox` в тесте | 1 | 1 flow | 1 максимум |
| Повторный запуск после success | 0 дополнительных | 0 | 0 |

Подтвердить, что остальные background-задачи после `AfterShow` продолжают выполняться независимо.

### 7.4 Ручной smoke-test

Проверить:

- существующий кандидат открывается без изменения UI;
- фотография, рейтинг и индикатор CV загружаются как ранее;
- вкладки «Резюме», «Взаимодействия», «Контакты» и «Позиции» работают;
- отсутствие `textCV` не вызывает ошибок;
- быстрое закрытие формы не создаёт UI-thread exception;
- отсутствуют OOM, detached, unfetched и NPE;
- `/hrm` отвечает HTTP 200.

## 8. Acceptance gate

Stage 15 принимается только при наличии:

- функционального Java-коммита;
- diff только в разрешённом scope;
- обновлённой спецификации и истории;
- compile и test results;
- `ScreenViewIntegrityTest` 8/8;
- `clean assemble` — `BUILD SUCCESSFUL`;
- runtime-подтверждения нулевого `textCV` SQL при скрытом `skillBox`;
- ручного smoke-test;
- HTTP 200;
- отчёта с реальными базовым и итоговым SHA.

Иначе:

```text
STAGE_15_BLOCKED
```

## 9. Итоговый отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-skills-stage-15-hidden-component-guard/
stage-15-hidden-skills-hermes-report.md
```

Отчёт должен включать:

- полный base SHA `092633eef6b089b6afe70e90c88e7383daa336f0`;
- фактический итоговый SHA;
- список изменённых файлов;
- расположение visibility-guard;
- подтверждение background `DataManager`;
- compile/test/assemble;
- `ScreenViewIntegrityTest` 8/8;
- runtime-таблицу SQL/service вызовов;
- smoke-test;
- HTTP 200.

## 10. Сообщение коммита

```text
perf(job-candidate): не разбирать CV для скрытого Skillsbar

- добавить visibility-guard до запуска background task
- использовать отдельный DataManager в фоновом потоке
- обновить спецификацию JobCandidateEdit
```