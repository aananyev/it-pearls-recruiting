# JobCandidateEdit Optimization Test Results

Дата: 2026-07-13.

## Выполнено

| Проверка | Результат |
| -------- | --------- |
| `:app-web:compileJava` | passed |
| `:app-web:compileTestJava` | passed |
| `:app-web:test --tests com.company.hunttech.web.screens.jobcandidate.JobCandidateEditPerfTest` | passed |

## Web performance test result

```text
PERF_RESULT JobCandidateEdit openMicros=81824 editedCandidate=216f42c5-10c9-704c-ef7a-38805afa31d5 load=0 loadList=12 getCount=0 loadValues=3 jobCandidateLoad=0 jobCandidateLoadList=1 jobCandidateGetCount=0
```

## Regression coverage

Добавлена проверка, что переключение на `tabCandidate` не увеличивает число `Company` `loadList`, то есть полный options container компаний не загружается при открытии вкладки.

## Не выполнено в этом проходе

Полный набор из 5 UI/Tomcat прогонов, ручная проверка всех вкладок и локальный browser smoke test не выполнялись.

