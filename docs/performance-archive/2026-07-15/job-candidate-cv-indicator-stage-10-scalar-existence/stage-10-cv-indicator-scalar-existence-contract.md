# Stage 10 — скалярная проверка наличия резюме

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `e7b58149e01b960c6e14ab338dae0a796808c729`  
**Тип этапа:** performance and fetch-safety implementation

## 1. Основание

`JobCandidateCvInitialViewOptimizer` исключает `candidateCv` из initial view. Однако `JobCandidateEdit.onBeforeShow()` вызывает `hasCandidateCv()`, а текущая реализация читает:

```java
List<CandidateCV> cvs = getEditedEntity().getCandidateCv();
```

Это противоречит ленивому контракту коллекции и создаёт два риска:

1. обращение к unfetched-атрибуту detached/частично загруженной сущности;
2. материализация коллекции резюме только ради индикатора «Резюме: ДА/НЕТ».

Для индикатора требуется только булев признак существования записи `CandidateCV`; entity-графы и LOB-поля не нужны.

## 2. Цель

Заменить прямое чтение `candidateCv` в `hasCandidateCv()` на один скалярный запрос по UUID кандидата.

Бизнес-поведение не меняется:

- есть хотя бы одно неудалённое резюме → `Резюме: ДА`;
- резюме нет → `Резюме: НЕТ`;
- новый кандидат → запрос не выполняется, результат `false`;
- вкладка «Резюме» продолжает загружать коллекцию только через `ensureCandidateCvLoaded()`.

## 3. Разрешённые изменения

Разрешено изменить только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-cv-indicator-stage-10-scalar-existence/
.ai/active-work.yml
```

Запрещено изменять:

- `job-candidate-edit.xml`;
- `JobCandidateCvInitialViewOptimizer`;
- `ensureCandidateCvLoaded()` и порядок загрузки вкладки резюме;
- `CandidateCV` и `JobCandidate` entities;
- `views.xml`;
- сервисы;
- Liquibase, индексы и БД;
- component ID, captions и actions;
- SCSS;
- production-данные.

## 4. Точная реализация

### 4.1 Переписать `hasCandidateCv()`

Допустимая реализация:

```java
/**
 * Проверяет наличие резюме скалярным запросом, не загружая коллекцию CandidateCV.
 */
private boolean hasCandidateCv() {
    if (PersistenceHelper.isNew(getEditedEntity()) || getEditedEntity().getId() == null) {
        return false;
    }

    Long cvCount = dataManager.loadValue(
            "select count(e.id) from hunttech_CandidateCV e " +
                    "where e.candidate.id = :candidateId",
            Long.class)
            .parameter("candidateId", getEditedEntity().getId())
            .one();

    return cvCount != null && cvCount > 0;
}
```

Допустима эквивалентная реализация, если соблюдены условия:

- параметр передаётся как UUID, а не detached entity;
- запрос возвращает скалярное число;
- не загружаются `CandidateCV` entities;
- не читается `getEditedEntity().getCandidateCv()`;
- новый кандидат не выполняет SQL;
- CUBA soft deletion остаётся стандартной и не обходится нативным SQL.

### 4.2 Не менять lazy-load коллекции

`ensureCandidateCvLoaded()` остаётся единственной точкой полной загрузки резюме при открытии вкладки.

Запрещено:

- устанавливать `candidateCvLoaded=true` после скалярной проверки;
- записывать пустую коллекцию в entity;
- merge-ить `CandidateCV` во время initial open;
- загружать `textCV`, файлы, вакансии или другие associations для индикатора.

### 4.3 Комментарии

Нетривиальный метод получает содержательный комментарий на русском. Комментарий должен объяснять, что скалярная проверка защищает lazy-contract и не материализует коллекцию.

## 5. Документация

Обновить `docs/ui/JobCandidateEdit_Spec.md`:

- в Behavior Summary описать скалярную проверку индикатора резюме;
- в разделе Stage 2 зафиксировать фактический `COUNT` по UUID;
- указать, что полная коллекция загружается только при первом открытии `tabResume`;
- добавить первую строку истории изменений с датой `2026-07-15`.

## 6. Обязательные проверки

### 6.1 Статические

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
```

Поиском подтвердить:

- `hasCandidateCv()` не вызывает `getCandidateCv()`;
- `hasCandidateCv()` содержит только scalar `loadValue`;
- `ensureCandidateCvLoaded()` не изменён;
- XML и views не изменены.

### 6.2 Автотесты

Обязательно:

```bash
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

`ScreenViewIntegrityTest`: 8/8 PASS.

Узкий unit-тест допускается и должен проверять минимум:

1. новый кандидат возвращает `false` без loader-вызова;
2. count `0` возвращает `false`;
3. count `1+` возвращает `true`;
4. `null` обрабатывается как `false`, если используемый API допускает `null`.

### 6.3 SQL/runtime verification

С SQL-логированием подтвердить:

| Сценарий | Ожидаемый результат |
|---|---|
| Новый кандидат | 0 запросов CandidateCV для индикатора |
| Существующий кандидат без CV | 1 scalar COUNT, 0 CandidateCV entities |
| Существующий кандидат с CV | 1 scalar COUNT, 0 CandidateCV entities |
| Открытие вкладки «Резюме» | отдельная загрузка через `candidateCV-browse-view` |
| Повторное открытие вкладки | без повторной полной загрузки |

Проверить отсутствие выборки `textCV` и файлов в scalar-запросе.

### 6.4 Ручной smoke-test

Проверить:

- кандидат с резюме показывает `Резюме: ДА`;
- кандидат без резюме показывает `Резюме: НЕТ`;
- новый кандидат открывается без ошибки;
- вкладка «Резюме» отображает существующие записи;
- добавление нового резюме и повторное открытие карточки обновляет индикатор;
- сохранение кандидата без открытия вкладки не удаляет резюме;
- отсутствуют `Cannot get unfetched attribute [candidateCv]`, detached, NPE и OOM;
- `/hrm` отвечает HTTP 200.

## 7. Acceptance gate

Stage 10 считается завершённым только при одновременном выполнении:

- функционального Java-коммита;
- diff в разрешённом scope;
- обновления `JobCandidateEdit_Spec.md`;
- `ScreenViewIntegrityTest` 8/8;
- `clean assemble` — `BUILD SUCCESSFUL`;
- SQL-подтверждения scalar-only запроса;
- ручного smoke-test;
- итогового отчёта Hermes.

При отсутствии обязательного пункта выставить:

```text
STAGE_10_BLOCKED
```

## 8. Итоговый отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-cv-indicator-stage-10-scalar-existence/
stage-10-cv-indicator-hermes-report.md
```

Отчёт должен содержать:

- базовый и итоговый SHA;
- полный список изменённых файлов;
- точный JPQL scalar-запрос;
- подтверждение отсутствия `getCandidateCv()` в `hasCandidateCv()`;
- результаты compile, tests, assemble;
- SQL verification;
- результаты ручного smoke-test;
- HTTP 200;
- итоговый вердикт.

## 9. Сообщение коммита

```text
perf(job-candidate): проверять наличие резюме скалярным запросом

- исключить чтение candidateCv из initial open
- сохранить ленивую загрузку вкладки резюме
- обновить спецификацию JobCandidateEdit
```
