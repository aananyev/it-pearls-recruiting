# Stage 13 — review фонового индикатора резюме

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Базовый SHA:** `7260e062a01137b6472bbf38df9adf6090ebfa81`  
**Итоговый HEAD Hermes:** `09b4ffe4fce4187cfdc4bce3c0ad9bd8d9c8cb8c`

## 1. Что реализовано корректно

В `JobCandidateEdit`:

- синхронный вызов `hasCandidateCv()` удалён из `onBeforeShow()`;
- до результата показывается нейтральная подпись `Резюме: …`;
- `startCandidateCvIndicatorBackgroundLoading()` запускается из `onAfterShow()`;
- в background передаётся UUID кандидата;
- `DataManager` получается через `AppBeans`;
- сохранён scalar `COUNT` и фильтр `e.deleteTs is null`;
- UI обновляется в `done()`;
- полная коллекция CV по-прежнему загружается через `ensureCandidateCvLoaded()`.

## 2. Блокирующее расхождение

`JobCandidateCvInitialViewOptimizer` сохранил старую реализацию индикатора:

1. В `inject()` регистрируется `screen.addAfterShowListener(...)`.
2. Listener вызывает `updateResumeAvailabilityLabel(...)`.
3. Метод синхронно вызывает `hasCandidateCv(candidate)`.
4. `hasCandidateCv(candidate)` выполняет отдельный `dataManager.loadValue(QUERY_CANDIDATE_CV_COUNT, Long.class)`.

Следовательно, после Stage 13 возможны два запроса:

- синхронный `COUNT` из `JobCandidateCvInitialViewOptimizer` на UI-потоке;
- фоновый `COUNT` из `JobCandidateEdit`.

Это нарушает основные acceptance-условия Stage 13:

- не гарантируется `0 CandidateCV SQL` до/на first paint;
- не гарантируется ровно один scalar `COUNT`;
- остаётся второй источник изменения `labelCV`;
- итоговое значение зависит от порядка завершения двух независимых обработчиков.

## 3. Дополнительные замечания

- В контроллере остался неиспользуемый метод `hasCandidateCv()` со старым синхронным запросом.
- `JobCandidateEdit_Spec.md` дополнен только строкой истории, но Behavior Summary и раздел Stage 2 не синхронизированы с фактическим background-flow.
- Отчёт Hermes содержит placeholder `$(git rev-parse HEAD)` и не содержит runtime-таблицу запросов, `ScreenViewIntegrityTest` 8/8 и `clean assemble`.

## 4. Вердикт

```text
STAGE_13_BLOCKED_BY_DUPLICATE_OPTIMIZER_QUERY
```

Фоновая реализация контроллера корректна, но Stage 13 нельзя считать завершённым до удаления старого синхронного индикатора из `JobCandidateCvInitialViewOptimizer` и подтверждения единственного background `COUNT`.

Исправление выделено в Stage 14, чтобы не смешивать его с дальнейшей оптимизацией справочников.