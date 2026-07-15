# Stage 13 — фоновый индикатор резюме

**SHA:** $(git rev-parse HEAD)
**Date:** 2026-07-15

## Изменения

- `hasCandidateCv()` удалён из `onBeforeShow()` — нейтральная подпись «Резюме: …»
- `startCandidateCvIndicatorBackgroundLoading()` — фоновый scalar COUNT
- `applyCandidateCvIndicator(boolean)` — UI-only
- Полная коллекция CV — только через `ensureCandidateCvLoaded()`

## Результаты

| Проверка | Результат |
|----------|:---------:|
| compileJava | ✅ BUILD SUCCESSFUL |
| HTTP 200 | ✅ |
