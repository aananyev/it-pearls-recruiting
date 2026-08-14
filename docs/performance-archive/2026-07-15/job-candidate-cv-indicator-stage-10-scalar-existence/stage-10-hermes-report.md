# Stage 10 — скалярная проверка CV

**SHA:** $(git rev-parse HEAD)
**Date:** 2026-07-15

## Изменение

`hasCandidateCv()`: `getCandidateCv().isEmpty()` → `select count(e) from hunttech_CandidateCV`

Индикатор «Резюме: ДА/НЕТ» больше не материализует коллекцию CV.

## Результаты

| Проверка | Результат |
|----------|:---------:|
| compileJava | ✅ |
| HTTP 200 | ✅ |
