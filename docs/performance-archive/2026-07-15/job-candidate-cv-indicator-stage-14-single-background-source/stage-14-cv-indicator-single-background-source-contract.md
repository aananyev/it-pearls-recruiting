# Stage 14 — единый фоновый источник индикатора резюме

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `e2aad7ce144fd370e1ba22b817c813b50aac91bd`  
**Тип этапа:** correctness and performance repair

## 1. Основание

Stage 13 добавил фоновый scalar `COUNT` в `JobCandidateEdit`, но старый `JobCandidateCvInitialViewOptimizer` продолжает регистрировать собственный `AfterShow`-listener:

```java
screen.addAfterShowListener(event -> {
    updateResumeAvailabilityLabel(screen, screenData);
    installResumeProjectLogoHydration(screen, screenData, dataContext);
    installSocialNetworkLogoHydration(screen, screenData, dataContext);
});
```

`updateResumeAvailabilityLabel()` вызывает `hasCandidateCv(candidate)`, который выполняет второй synchronous `COUNT` через injected `DataManager`.

Stage 14 устраняет второй запрос и оставляет единственный источник `labelCV`: background flow контроллера.

## 2. Цель

После Stage 14:

- до first paint выполняется `0 CandidateCV COUNT`;
- после `onAfterShow()` выполняется ровно один background `COUNT`;
- `labelCV` обновляется только `JobCandidateEdit.applyCandidateCvIndicator()`;
- `JobCandidateCvInitialViewOptimizer` отвечает только за initial view и hydration логотипов;
- полная коллекция CV загружается только на вкладке «Резюме».

Бизнес-результат сохраняется:

- CV существует → `Резюме: ДА`;
- CV отсутствует → `Резюме: НЕТ`;
- новый кандидат → CandidateCV SQL отсутствует.

## 3. Разрешённый scope

Разрешено изменять только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateCvInitialViewOptimizer.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-cv-indicator-stage-14-single-background-source/
.ai/active-work.yml
```

Запрещено изменять:

- `job-candidate-edit.xml`;
- `JobCandidateInitialViewOptimizer`;
- `JobCandidateSocialNetworkInitialViewOptimizer`;
- entities;
- `views.xml`;
- core-сервисы;
- Liquibase, индексы и БД;
- component ID, actions, captions;
- SCSS и темы;
- production-данные.

## 4. Изменения в `JobCandidateCvInitialViewOptimizer`

### 4.1 Удалить старый indicator flow

Удалить из `inject()` вызов:

```java
updateResumeAvailabilityLabel(screen, screenData);
```

Сохранить без изменения:

```java
installResumeProjectLogoHydration(screen, screenData, dataContext);
installSocialNetworkLogoHydration(screen, screenData, dataContext);
```

Допустимо оставить один `addAfterShowListener`, если он используется только для установки hydration-listener’ов и не выполняет CV `COUNT`.

### 4.2 Удалить dead code

Из optimizer удалить:

- `updateResumeAvailabilityLabel(...)`;
- `hasCandidateCv(JobCandidate candidate)`;
- `QUERY_CANDIDATE_CV_COUNT`;
- `JOB_CANDIDATE_CONTAINER_ID`;
- `CV_LABEL_ID`;
- неиспользуемые imports `PersistenceHelper`, `Label`, `InstanceContainer`, если они больше нигде не применяются.

Не удалять injected `DataManager`: он используется для batch hydration проектов и типов социальных сетей.

### 4.3 Обновить JavaDoc

В class-level JavaDoc зафиксировать:

- optimizer исключает `candidateCv` из initial view;
- индикатор `ДА/НЕТ` теперь полностью принадлежит background flow `JobCandidateEdit`;
- optimizer больше не выполняет `CandidateCV COUNT`.

## 5. Изменения в `JobCandidateEdit`

Удалить неиспользуемый synchronous метод:

```java
private boolean hasCandidateCv()
```

Сохранить без функциональных изменений:

- `applyCandidateCvIndicator(boolean)`;
- `candidateCvIndicatorLoading`;
- `candidateCvIndicatorLoaded`;
- `startCandidateCvIndicatorBackgroundLoading()`;
- scalar JPQL с `e.deleteTs is null`;
- `ensureCandidateCvLoaded()`;
- порядок независимых background tasks в `onAfterShow()`.

Запрещено объединять CV indicator task с rating, photo или Skillsbar.

## 6. Документация

Обновить `docs/ui/JobCandidateEdit_Spec.md` не только историей:

### Behavior Summary

Добавить явный сценарий:

```text
Открытие существующего кандидата → до first paint отображается «Резюме: …» →
после AfterShow выполняется единственный background scalar COUNT → label получает ДА/НЕТ.
```

### Этап 2 — резюме

Указать:

- `candidateCv` исключён из initial view;
- optimizer не выполняет COUNT;
- единственный COUNT находится в `JobCandidateEdit` background task;
- полная коллекция загружается через `ensureCandidateCvLoaded()` только на `tabResume`;
- indicator flow не изменяет `candidateCvLoaded` и не записывает коллекцию в entity.

### История изменений

Добавить первой строкой:

```text
2026-07-15 — Stage 14: удалён дублирующий synchronous COUNT из JobCandidateCvInitialViewOptimizer; индикатор CV имеет один background-источник.
```

## 7. Обязательные проверки

### 7.1 Статический поиск

```bash
git diff --check
rg -n "QUERY_CANDIDATE_CV_COUNT|updateResumeAvailabilityLabel|hasCandidateCv" \
  modules/web/src/com/company/hunttech/web/screens/jobcandidate
```

Ожидается:

- `QUERY_CANDIDATE_CV_COUNT` — 0 совпадений;
- `updateResumeAvailabilityLabel` — 0 совпадений;
- `hasCandidateCv` — 0 совпадений;
- JPQL `select count(e) from hunttech_CandidateCV` — ровно одно активное совпадение в background task контроллера;
- `labelCV.setValue` для `ДА/НЕТ` — только в `applyCandidateCvIndicator`;
- optimizer не содержит `loadValue` для CandidateCV.

### 7.2 Compile и tests

```bash
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Требования:

- `ScreenViewIntegrityTest` — 8/8 PASS;
- `BUILD SUCCESSFUL`;
- existing optimizer tests зелёные;
- добавить/обновить узкий тест, подтверждающий отсутствие indicator listener в optimizer, если архитектура тестов позволяет.

### 7.3 Runtime/SQL verification

| Сценарий | Ожидаемый результат |
|---|---:|
| Новый кандидат | 0 CandidateCV COUNT |
| Существующий с CV до first paint | 0 COUNT |
| Существующий с CV после AfterShow | ровно 1 background COUNT |
| Существующий без CV после AfterShow | ровно 1 background COUNT |
| Повторный `AfterShow` | 0 дополнительных COUNT |
| Открытие `tabResume` | отдельный entity-load коллекции, без повторного indicator COUNT |
| Hydration логотипов CV | работает после загрузки вкладки |
| Hydration логотипов соцсетей | работает после загрузки вкладки |

Обязательно отдельно показать SQL/runtime evidence, что optimizer не выполняет запрос.

### 7.4 Ручной smoke-test

Проверить:

- `Резюме: …` видно до завершения background task;
- кандидат с CV получает `Резюме: ДА`;
- кандидат без CV получает `Резюме: НЕТ`;
- новый кандидат открывается без CandidateCV SQL;
- вкладка резюме отображает записи и логотипы проектов;
- вкладка социальных сетей отображает логотипы;
- сохранение без открытия вкладки не удаляет CV;
- быстрое закрытие формы не создаёт UI-thread exception;
- отсутствуют unfetched/detached/NPE/OOM;
- `/hrm` отвечает HTTP 200.

## 8. Acceptance gate

Stage 14 принимается только при одновременном выполнении:

- удалён старый optimizer COUNT;
- оставлен ровно один background COUNT;
- hydration-функции optimizer сохранены;
- diff ограничен разрешённым scope;
- `JobCandidateEdit_Spec.md` полностью синхронизирован;
- compile/test/assemble зелёные;
- `ScreenViewIntegrityTest` 8/8;
- runtime доказал таблицу запросов;
- ручной smoke-test выполнен;
- HTTP 200;
- итоговый отчёт содержит реальные SHA.

При отсутствии любого пункта:

```text
STAGE_14_BLOCKED
```

## 9. Итоговый отчёт Hermes

Создать:

```text
docs/performance-archive/2026-07-15/
job-candidate-cv-indicator-stage-14-single-background-source/
stage-14-cv-indicator-hermes-report.md
```

Отчёт должен содержать:

- базовый SHA `e2aad7ce144fd370e1ba22b817c813b50aac91bd`;
- реальный итоговый SHA;
- список изменённых файлов;
- grep/rg results;
- единственный активный JPQL;
- compile/test/assemble;
- `ScreenViewIntegrityTest` 8/8;
- runtime-таблицу запросов;
- smoke-test;
- HTTP 200;
- итоговый вердикт.

## 10. Сообщение коммита

```text
fix(job-candidate): удалить дублирующий CV COUNT

- оставить индикатор резюме только в background task
- сохранить hydration логотипов вкладок
- синхронизировать спецификацию JobCandidateEdit
```