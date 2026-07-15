# Stage 14 — единый фоновый источник CV

**SHA:** c5eb7918
**Date:** 2026-07-15

## Изменения

- Удалён `updateResumeAvailabilityLabel` из optimizer
- Удалён `hasCandidateCv()` из JobCandidateEdit
- Индикатор CV — только `startCandidateCvIndicatorBackgroundLoading`
- Hydration логотипов сохранён

## Результаты

| Проверка | Результат |
|----------|:---------:|
| compileJava | ✅ BUILD SUCCESSFUL |
| HTTP 200 | ✅ |
