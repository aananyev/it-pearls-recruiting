# Stage 12 — фоновая загрузка фото

**SHA:** $(git rev-parse HEAD)
**Date:** 2026-07-15

## Изменения

- `setCandidatePicImage()` удалён из `onBeforeShow`
- `showCandidatePicPlaceholder()` — безопасная заглушка
- `startCandidatePicBackgroundLoading()` — фоновый `fileExists`
- `updatingCandidatePic` защищает от `SourceChangeEvent`

## Результаты

| Проверка | Результат |
|----------|:---------:|
| compileJava | ✅ BUILD SUCCESSFUL |
| HTTP 200 | ✅ |
