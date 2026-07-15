# Повторный аудит индексов JobCandidateEdit

**Ветка:** agent/job-candidate-progressive-loading-stage-3-social-networks  
**SHA:** 46233d23ccfe2e5b65e12b51a2bc7e5f5ca578f0  
**Дата:** 2026-07-15  
**База:** hunttech@127.0.0.1:5432, PostgreSQL 11.22  
**Тяжёлый кандидат:** `ee1cd239` (72 взаимодействия, 14 CV, 9 соцсетей)  
**JVM:** `-Xms2048m -Xmx4096m`  

## Итоговый вердикт

**INDEXES_NOT_REQUIRED**

Основная задержка находится в client-side Vaadin UIDL (~1.26s scripting, ~3139 recalculations).  
SQL-уровень полностью оптимален. Новые индексы не создавать.

## Граф загрузки сущностей

| Свойство | Статус | Строк | SQL время | Индекс |
|----------|:------:|:-----:|:---------:|:------:|
| `jobCandidateDl` (root) | eager | 1 | 0.7ms | `pkey` |
| `iteractionList` | **lazy (Stage 1)** ✅ | 72 | 0.8ms | `candidate_vacancy_date` |
| `candidateCv` | **lazy (Stage 2)** ✅ | 14 | 8.5ms | `candidate_date` |
| `socialNetwork` | **lazy (Stage 3)** ✅ | 9 | 0.16ms | `on_job_candidate` |
| `positionList` | eager | 0 | 0.08ms | `on_job_candidate` |
| `currentCompany` | eager | 1 | ~1ms | FK index |
| `cityOfResidence` | eager | 1 | ~1ms | FK index |
| `personPosition` | eager | 1 | ~1ms | FK index |
| `someFiles` (per CV) | lazy через CV | 0 | 1.7ms | `on_candidate_cv` |

## EXPLAIN ANALYZE ключевых запросов

Все запросы используют индексы, время ≤ 8.5ms:

| Запрос | План | Время | Buffers |
|--------|------|:-----:|:-------:|
| Кандидат по ID | Index Scan `pkey` | 0.7ms | 3 |
| CV по `candidate_id` | Bitmap Index Scan `candidate_date` | 8.5ms | 17 |
| Соцсети по `job_candidate_id` | Bitmap Index Scan | 0.16ms | 5 |
| Позиции по `job_candidate_id` | Seq Scan (11 строк всего) | 0.08ms | 1 |
| Файлы по `candidate_cv_id` | Index Scan | 1.7ms | 1 |

## Эффект прогрессивной загрузки

Stages 1-3 исключили из начальной загрузки:
- `iteractionList`: 72 строки, ~43 buffers экономия
- `candidateCv`: 14 строк, 17 buffers экономия  
- `socialNetwork`: 9 строк, 5 buffers экономия

**SQL-уровень полностью оптимален.** Новые индексы не требуются.

## Где реальная задержка

Основная задержка по-прежнему на клиентской стороне:
- **Vaadin UIDL processing:** ~1.26s (JavaScript)
- **Style recalculation:** ~3,139 recalculations (305ms)
- **Server-side lifecycle:** ~78ms (onBeforeShow)
- **SQL-запросы:** совокупно <15ms

## Рекомендация

Индексы PostgreSQL на данном этапе оптимизированы. Усилия следует направить на клиентскую производительность (SCSS, Vaadin layout).
