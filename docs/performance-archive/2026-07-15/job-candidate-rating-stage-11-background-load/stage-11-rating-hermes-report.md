# Stage 11 — фоновая загрузка рейтинга

**SHA:** $(git rev-parse HEAD)
**Date:** 2026-07-15

## Изменения

- `setRatingLabel()` удалён из `onBeforeShow()`
- Добавлен `startRatingBackgroundLoading()` с `BackgroundTask`
- `applyRatingLabel(double)` — UI-форматирование без SQL
- `ratingLoading`/`ratingLoaded` — защита от повторов
- Вызов в `onAfterShow()` после `setBlockUnblockButton()`

## Результаты

| Проверка | Результат |
|----------|:---------:|
| compileJava | ✅ BUILD SUCCESSFUL |
| HTTP 200 | ✅ |
