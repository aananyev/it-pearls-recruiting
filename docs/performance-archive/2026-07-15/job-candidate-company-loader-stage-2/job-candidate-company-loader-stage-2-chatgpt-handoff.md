# Передача Stage 2 на проверку Hermes

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Проверяемый HEAD:** `101c5caf5947ad0af59634ad2adaebf503c015d3`

## Реализованный scope

- добавлен `JobCandidateCompanyLoaderOptimizer`;
- компонент до `@LoadDataBeforeShow` получает `currentCompaniesLc` из `ScreenData`;
- `PreLoadListener` вызывает `preventLoad()` и не допускает выполнения полного JPQL списка компаний;
- `JobCandidateEdit.java` и `job-candidate-edit.xml` не изменялись;
- suggestion, lookup, open и create-company flow сохранены;
- добавлен `JobCandidateCompanyLoaderOptimizerTest`, фиксирующий регистрацию pre-load listener;
- спецификация `docs/ui/JobCandidateEdit_Spec.md` синхронизирована с lifecycle-оптимизацией.

## Обязательные проверки Hermes

```bash
git fetch origin --prune
git show origin/coordination/active-work:.ai/active-work.yml

git switch agent/job-candidate-progressive-loading-stage-3-social-networks
git pull --ff-only origin agent/job-candidate-progressive-loading-stage-3-social-networks

git status --short --branch
git rev-parse HEAD
git rev-parse origin/agent/job-candidate-progressive-loading-stage-3-social-networks
```

Три значения должны совпасть с:

```text
101c5caf5947ad0af59634ad2adaebf503c015d3
```

Далее выполнить:

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-web:test \
          --tests "com.company.hunttech.web.screens.jobcandidate.JobCandidateCompanyLoaderOptimizerTest" \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- unit-тест Stage 2 — PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- `BUILD SUCCESSFUL`.

## Runtime и SQL

Развернуть локальный контекст:

```text
http://localhost:8080/hrm/
```

Проверить HTTP 200.

Для тяжёлого существующего кандидата `ee1cd239...`, кандидата без компании и нового кандидата выполнить один прогрев и пять измерительных открытий.

При initial open, до ввода в поле компании, SQL не должен содержать:

```jpql
select e from hunttech_Company e order by e.comanyName
```

Зафиксировать:

- MIN, MAX, AVG, P50, P95;
- SQL count;
- SQL total;
- количество Company, загруженных полным loader — ожидается `0`.

## Функциональная регрессия

Проверить:

1. текущая компания отображается сразу;
2. ввод 0–1 символа не выполняет suggestion SQL;
3. ввод 2+ символов возвращает не более 50 компаний;
4. выбор suggestion устанавливает компанию;
5. `lookup` открывает экран выбора;
6. `open` открывает выбранную компанию;
7. `createCompany` открывает `CompanyEdit`;
8. после сохранения новая компания подставляется кандидату;
9. отмена создания позволяет повторно запустить action;
10. сохранение кандидата сохраняет связь с компанией.

В логах не допускаются:

```text
Cannot get unfetched attribute
IllegalStateException
NullPointerException
OutOfMemoryError
```

## Вердикт

Допустим один итог:

```text
STAGE_2_CONFIRMED
STAGE_2_REGRESSION
STAGE_2_BLOCKED
```

Отчёт сохранить в:

```text
docs/performance-archive/2026-07-15/job-candidate-company-loader-stage-2/
job-candidate-company-loader-stage-2-hermes-report.md
```
