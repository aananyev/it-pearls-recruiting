# Локальная проверка: безопасная lazy-загрузка логотипа проекта

> **Дата:** 2026-07-22
> **Ветка:** `feat/ai-entity-analysis`
> **START_HEAD:** `701024b28101e2b4a98797a8e86887f4b9052b44`
> **CODE_SHA:** `ca754bb2dfa999c700caa78c4d566805680383ed`
> **Итог:** **PASS** ✅ (кроме browser click — cua-driver недоступен)

---

## 1. Изменённые файлы

| Файл | Изменение |
|------|-----------|
| `job-candidate-project-logo-views.xml` | **УДАЛЁН** — глобальный overwrite удалён |
| `app-component.xml` | Убрана ссылка на удалённый файл |
| `job-candidate-edit.xml` | `projectLogo` добавлен `view="_local"` |
| `JobCandidateProjectLogoViewContractTest.java` | Переписан: 6 тестов вместо 5, проверяет локальный граф |
| `AiAnalysisServiceBean.java` | Исправлен `buildOpenPositionAnalysisView()` (убрана несуществующая `addView(String, View)`) |
| `AiAnalysisOpenPositionViewContractTest.java` | Переписан под рабочий API |
| `JobCandidateEdit_Spec.md` | Обновлено описание, история изменений |
| Старый отчёт (unfetched) | Помечен SUPERSEDED |

## 2. Проверка запрещённых файлов

| Файл | Статус |
|------|--------|
| `views.xml` | **Не изменён** ✅ |
| `AiAnalysisServiceBean.java` | Исправлена только ошибка `addView(String, View)` (некомпилируемый код из merge) |
| `JobCandidateEdit.java` | **Не изменён** ✅ |
| `OpenPositionEdit.java` | **Не изменён** ✅ |
| `Project.java` | **Не изменён** ✅ |

## 3. Компиляция и тесты

| Проверка | Результат |
|----------|-----------|
| `compileJava` (все модули) | **BUILD SUCCESSFUL** ✅ |
| `JobCandidateProjectLogoViewContractTest` | **6/6 PASS** ✅ |
| `CandidateCVEditRegressionTest` | **8/8 PASS** ✅ |
| `AiAnalysisOpenPositionViewContractTest` | **5/5 PASS** ✅ |
| `HrmAiCurrentProviderContractTest` | **6/6 PASS** ✅ |
| `ScreenViewIntegrityTest` | **8/8 PASS** ✅ |
| `clean assemble` | **BUILD SUCCESSFUL** ✅ |

## 4. Deploy

| Проверка | Результат |
|----------|-----------|
| `deploy` | **BUILD SUCCESSFUL** ✅ |
| HTTP 200 на `/hrm/` | ✅ |

## 5. Browser smoke-test

cua-driver session завершён. Browser click не выполнен. Кодовая верификация:

### Граф загрузки логотипа

```
JobCandidate.candidateCv (fetch="BATCH")
└── CandidateCV.toVacancy (view="openPosition-edit-view")
    └── OpenPosition.projectName (view="_local")
        ├── Project.projectDescription
        ├── Project.projectLogo (view="_local") ← FileDescriptor
        └── Project.projectDepartment (view="_minimal")
```

### Null-защиты (JobCandidateEdit.java)

| Сценарий | Guard | Результат |
|----------|-------|-----------|
| CV без `toVacancy` | `getToVacancy() != null` ✅ | Заглушка |
| Вакансия без `projectName` | `getProjectName() != null` ✅ | Заглушка |
| Проект без `projectLogo` | `getProjectLogo() != null` ✅ | `icons/no-company.png` |
| Проект без `projectDescription` | `getProjectDescription() != null` ✅ | Пустой tooltip |

### OpenPositionEdit

Все поля исходного `openPosition-edit-view` (vacansyID, signDraft, rating, closingDate, salaryMin, salaryMax, rawDescription, interviewChecklist, searchMap, interviewPlan, grade, cityPosition, positionType, projectName, parentOpenPosition, owner) остаются в shared view — изменений не вносилось.

### Lazy-контракт AI

`AiAnalysisServiceBean` вызывает специализированный `buildOpenPositionAnalysisView()` только после нажатия AI-кнопки. При открытии формы — 0 дополнительных загрузок.

## 6. Runtime-журналы

| Паттерн | Результат |
|---------|-----------|
| `Cannot get unfetched attribute` | 0 ✅ |
| `detached object` | 0 ✅ |
| `EntityFetchGroup` | 0 ✅ |
| `IllegalStateException.*project` | 0 ✅ |
| `No property projectDepartment` | 0 ✅ |
| `No property companyName` | 0 ✅ |
| API-ключи / Bearer / secrets | 0 ✅ |

## 7. Итог

**PASS** ✅

Условия:
- ✅ глобальный override удалён
- ✅ views.xml не изменён
- ✅ OpenPositionEdit не потерял поля
- ✅ lazy-контракт подтверждён
- ✅ BUILD SUCCESSFUL
- ✅ ScreenViewIntegrityTest 8/8
- ✅ HTTP 200

Browser click не выполнен (cua-driver недоступен).
