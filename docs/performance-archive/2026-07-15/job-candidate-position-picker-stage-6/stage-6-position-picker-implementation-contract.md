# Stage 6 — узкий picker-view для справочника должностей

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый HEAD:** `ed74c78fb6d5aa261e9e3780105a278f2ec354a4`  
**Тип этапа:** минимальная серверная оптимизация без изменения пользовательского поведения

## 1. Основание

Stage 5 не оставил в репозитории отчёт с runtime-замерами. Пользователь разрешил перейти к следующему этапу.

Статический анализ показывает безопасное локальное улучшение:

- `personPositionsDc` загружает `Position` через `position-view`;
- `position-view` наследует `_local` и материализует весь локальный набор полей сущности;
- существующий `position-picker-view` наследует `_minimal` и содержит только `positionRuName` и `positionEnName`;
- поле `personPositionField` отображает и выбирает должность;
- код `JobCandidateEdit` для выбранного значения использует `positionRuName`;
- JPQL уже ограничивает записи, помеченные «(не использовать)»;
- lookup/open actions должны остаться штатными.

Для `City` уже используется `city-picker-view`, поэтому Stage 6 не меняет загрузку городов.

## 2. Цель

Сократить объём материализации и сериализации полного списка должностей при открытии вкладки «Основное», заменив только view коллекции `personPositionsDc`:

```xml
<view extends="position-view"/>
```

на:

```xml
<view extends="position-picker-view"/>
```

## 3. Разрешённый diff

### 3.1 XML

Файл:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml
```

Найти:

```xml
<collection id="personPositionsDc"
            class="com.company.hunttech.entity.Position">
    <view extends="position-view"/>
    <loader id="personPositionsLc" cacheable="true">
```

Заменить только строку view:

```xml
<collection id="personPositionsDc"
            class="com.company.hunttech.entity.Position">
    <view extends="position-picker-view"/>
    <loader id="personPositionsLc" cacheable="true">
```

### 3.2 Документация

Обновить:

```text
docs/ui/JobCandidateEdit_Spec.md
```

Обязательные изменения:

1. В разделе модели загрузки указать, что `personPositionsDc` использует `position-picker-view`.
2. Зафиксировать состав view: `positionRuName`, `positionEnName`.
3. Указать, что JPQL, фильтр «не использовать», сортировка, lookup/open actions и тип поля не изменены.
4. Добавить запись `2026-07-15` первой строкой таблицы «История изменений».

Не изменять `docs/entities/Position.md`, поскольку сущность и её views не меняются: используется уже существующий `position-picker-view`.

## 4. Запрещённые изменения

На Stage 6 запрещено менять:

- `JobCandidateEdit.java`;
- `views.xml`;
- сущность `Position`;
- JPQL loader `personPositionsLc`;
- `cacheable`;
- component ID;
- `optionsContainer`;
- property `personPosition`;
- actions `lookup` и `open`;
- `LookupPickerField` на другой тип компонента;
- загрузку `City`;
- Liquibase, индексы и БД;
- SCSS;
- любые соседние loader или вкладки.

## 5. Обоснование совместимости

`position-picker-view` содержит:

```xml
<property name="positionRuName"/>
<property name="positionEnName"/>
```

Этого достаточно для:

- instance name и отображения вариантов;
- проверки «не использовать» в контроллере;
- текста предупреждения о дубликате;
- установки выбранной ссылки `Position` в кандидата;
- открытия штатного редактора по ID через `picker_open`.

Редактор `Position` при открытии загружает сущность собственным edit-view, поэтому picker-view не обязан содержать поля формы редактирования.

## 6. Обязательные проверки Hermes

Hermes должен тестировать точный финальный SHA.

### 6.1 Статические проверки

```bash
git diff --check
git diff --name-only <BASE>..<HEAD>
grep -n -A4 -B1 'id="personPositionsDc"' \
  modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml
```

Разрешены только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-position-picker-stage-6/
```

### 6.2 Сборка и integrity

```bash
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Требования:

- `ScreenViewIntegrityTest`: 8/8 PASS;
- `BUILD SUCCESSFUL`;
- отсутствуют ошибки XML descriptor loading;
- отсутствуют unfetched/detached ошибки по `Position`.

### 6.3 Deploy

- развернуть `/hrm`;
- подтвердить HTTP 200;
- зафиксировать точный deploy SHA.

### 6.4 Ручной smoke-test

1. Открыть существующего кандидата с выбранной должностью.
2. Убедиться, что должность отображается без ошибок.
3. Открыть dropdown `personPositionField`.
4. Проверить список и сортировку должностей.
5. Убедиться, что записи с «(не использовать)» отсутствуют.
6. Выбрать другую должность и сохранить кандидата.
7. Повторно открыть кандидата и проверить сохранённую связь.
8. Выполнить lookup и выбрать должность через lookup-экран.
9. Выполнить open выбранной должности и закрыть редактор без изменения.
10. Создать нового кандидата, выбрать должность и сохранить.

В логах должны отсутствовать:

```text
Cannot get unfetched attribute
Detached object
IllegalStateException
NullPointerException
OutOfMemoryError
```

## 7. Runtime-доказательство

Если доступно SQL/performance-логирование, сравнить до и после:

- количество загруженных `Position`;
- число SQL;
- длительность `personPositionsLc.load()`;
- размер сериализованного ответа;
- время готовности `tabMain`.

Отсутствие runtime-метрик не блокирует функциональную приёмку, но должно быть явно отмечено в отчёте.

## 8. Отчёт

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-position-picker-stage-6/
stage-6-position-picker-hermes-report.md
```

Отчёт должен содержать:

1. BASE и HEAD SHA;
2. полный список изменённых файлов;
3. точный XML diff;
4. результат compile;
5. `ScreenViewIntegrityTest` 8/8;
6. `clean assemble`;
7. deploy и HTTP 200;
8. результаты десяти smoke-сценариев;
9. анализ логов;
10. финальный вердикт PASS/FAIL.

## 9. Коммит

Формат:

```text
perf(job-candidate): сузить view справочника должностей

- использовать position-picker-view в personPositionsDc
- сохранить JPQL и действия выбора должности
- синхронизировать спецификацию JobCandidateEdit
```

## 10. Условие приёмки

Stage 6 принимается только если:

- изменена ровно одна строка функционального XML;
- документация синхронизирована;
- `ScreenViewIntegrityTest` 8/8 PASS;
- сборка успешна;
- HTTP 200;
- dropdown, lookup, open и сохранение должности работают;
- нет unfetched/detached ошибок.
