# JobCandidateEdit Optimization Summary

Дата: 2026-07-13.

## Итог

Причина задержки: initial fetch graph загружал тяжёлые коллекции кандидата, а вкладка `tabCandidate` строила полный options container компаний.

| Метрика | До | После | Изменение |
| ------- | --: | ----: | --------: |
| Открытие формы, web test | 110138 мкс | 81824 мкс | -25.7% |
| `loadList` | 11 | 12 | +1 |
| `loadValues` | 3 | 3 | 0 |
| Company options при `tabCandidate` | 5623 | 0 full preload | -5623 |

## Изменения

- Initial view `jobCandidateDc` больше не загружает `candidateCv`, `iteractionList`, `socialNetwork`.
- `candidateCv` загружается при первом открытии `tabResume`.
- `iteractionList` загружается при первом открытии `tabIteraction`.
- `socialNetwork` загружается при первом открытии `tabContactInfo`.
- `currentCompanyField` использует server-side suggestion search вместо полной предварительной загрузки компаний.
- Сценарий создания новой `Company` сохранён через create action и merge в текущий `DataContext`.

## Проверки

Passed:

- `./gradlew :app-web:compileJava :app-web:compileTestJava --stacktrace`
- `./gradlew :app-web:test --tests com.company.hunttech.web.screens.jobcandidate.JobCandidateEditPerfTest --stacktrace`

## Ограничения

Полный ручной UI smoke test, 5 повторений performance baseline/result и локальный Tomcat restart в этом проходе не выполнены. Production и production database не изменялись. Схема БД не менялась. Индексы не создавались.

## Изменённые файлы

- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml`
- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java`
- `modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidateEditPerfTest.java`
- `modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidatePerfTestSupport.java`
- `docs/reports/performance/job-candidate-optimization-baseline.md`
- `docs/reports/performance/job-candidate-component-data-dependencies.md`
- `docs/reports/performance/job-candidate-optimization-implementation.md`
- `docs/reports/performance/job-candidate-optimization-test-results.md`
- `docs/reports/performance/job-candidate-optimization-summary.md`

## Verdict

`ОПТИМИЗАЦИЯ РЕАЛИЗОВАНА, НО НЕ ПРОШЛИ ВСЕ REGRESSION-ТЕСТЫ`

