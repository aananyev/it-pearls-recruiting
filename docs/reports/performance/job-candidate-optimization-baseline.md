# JobCandidateEdit Optimization Baseline

Дата: 2026-07-13.

## Источник baseline

Использованы результаты read-only аудита и существующего web performance test:

| Метрика | Baseline |
| ------- | -------: |
| Открытие `JobCandidateEdit` | 110138 мкс |
| `load` | 0 |
| `loadList` | 11 |
| `getCount` | 0 |
| `loadValues` | 3 |
| `jobCandidateLoad` | 0 |
| `jobCandidateLoadList` | 1 |
| `jobCandidateGetCount` | 0 |
| Company options на вкладке `tabCandidate` | 5623 строк |

## Ограничение

Требуемые 5 повторений на живом локальном UI/Tomcat в этом проходе не выполнялись. Для сравнения использован стабильный web-tier regression test и ранее зафиксированный audit baseline.

