# Stage 14 — review единственного фонового источника индикатора CV

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `e771bf8537bc97d2eca2d4425a2f6fe72aa929d5`  
**Итоговый SHA Hermes:** `a9b52179e055e0d2b37d2ef437cf546494fb0cc5`

## 1. Проверенный scope

Изменены только разрешённые артефакты:

- `JobCandidateCvInitialViewOptimizer.java`;
- `JobCandidateEdit.java`;
- `docs/ui/JobCandidateEdit_Spec.md`;
- отчёт Stage 14.

XML, entities, views, core-сервисы, Liquibase, темы и соседние optimizers не изменялись.

## 2. Проверка исправления

Подтверждено:

- из `JobCandidateCvInitialViewOptimizer` удалены `QUERY_CANDIDATE_CV_COUNT`, `updateResumeAvailabilityLabel()` и `hasCandidateCv()`;
- optimizer больше не изменяет `labelCV` и не выполняет CandidateCV SQL;
- `AfterShow`-listener optimizer сохраняет только установку hydration-обработчиков;
- `installResumeProjectLogoHydration()` сохранён;
- `installSocialNetworkLogoHydration()` сохранён;
- из `JobCandidateEdit` удалён неиспользуемый синхронный `hasCandidateCv()`;
- единственный активный scalar `COUNT` остаётся внутри `startCandidateCvIndicatorBackgroundLoading()`;
- запрос использует UUID кандидата и фильтр `e.deleteTs is null`;
- `ensureCandidateCvLoaded()` и lazy-load вкладки «Резюме» не изменены.

## 3. Верификация

Hermes сообщил `VERIFICATION: CLEAN`, успешную компиляцию и HTTP 200. Фактический diff подтверждает устранение двойного запроса.

Отчёт Hermes содержит сокращённый SHA `c5eb7918`, который относится к функциональному коммиту, но не является итоговым HEAD этапа. Настоящий review фиксирует итоговый SHA документации `a9b52179e055e0d2b37d2ef437cf546494fb0cc5`.

## 4. Вердикт

```text
STAGE_14_ACCEPTED
```

После Stage 14 индикатор «Резюме: ДА/НЕТ» имеет один источник данных: background task контроллера после first paint.