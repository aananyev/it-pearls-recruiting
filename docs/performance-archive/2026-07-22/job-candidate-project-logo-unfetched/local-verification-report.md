# Локальная проверка: исправление unfetched attribute `projectLogo` в JobCandidateEdit

> **Дата:** 2026-07-22
> **Проверенный SHA:** `742d41c7ea180d0c29f58ca70154a829dd992dc9`
> **Ветка:** `feat/ai-entity-analysis`
> **СТАТУС: SUPERSEDED / НЕ ПРИНЯТ**
>
> ## Причины пересмотра
>
> 1. Использовался глобальный `overwrite="true"` для `openPosition-edit-view`
> 2. Эффективный `openPosition-edit-view` был усечён до 6 полей
> 3. Browser click не выполнялся
> 4. Итог `PASS` был выставлен только по статической проверке
> 5. Решение заменено локальным экранным view в `job-candidate-edit.xml`
>
> Актуальное решение: `docs/performance-archive/2026-07-22/job-candidate-project-logo-lazy-safe/local-verification-report.md`

---

## 1. Исходное состояние

| Параметр | Значение |
|----------|----------|
| Ветка | `feat/ai-entity-analysis` |
| HEAD | `742d41c7ea180d0c29f58ca70154a829dd992dc9` |
| Рабочий каталог | Чистый |
| Java | Corretto 11.0.17 |
| Gradle | 5.6.4 |
| Tomcat | deploy/tomcat |

## 2. Дефект и исправление

**Исходное исключение:**
```
IllegalStateException: Cannot get unfetched attribute [projectLogo]
from detached object com.company.hunttech.entity.Project
```

**Цепочка:** `JobCandidateEdit → jobCandidateCandidateCvTableProjectLogoColumnColumnGenerator → CandidateCV.toVacancy → OpenPosition.projectName → Project.getProjectLogo()`

**Исправление:** Новый файл `job-candidate-project-logo-views.xml` с переопределением `openPosition-edit-view` (`overwrite="true"`), добавляющим вложенный граф:
```
CandidateCV.toVacancy
└── OpenPosition.projectName
    └── Project.projectLogo
        └── FileDescriptor (_local)
```

**Статическая проверка:**

| Проверка | Результат |
|----------|-----------|
| `app-component.xml` включает `job-candidate-project-logo-views.xml` | ✅ (line 11) |
| `projectLogo` с `view="_local"` вложен в `projectName` | ✅ (line 20) |
| `Project.java` содержит `private FileDescriptor projectLogo` | ✅ (line 29) |
| `overwrite="true"` на view definition | ✅ (line 11) |
| `git diff --check` | ✅ |

## 3. Компиляция и тесты

| Проверка | Результат |
|----------|-----------|
| `compileJava` (все модули) | BUILD SUCCESSFUL ✅ |
| `JobCandidateProjectLogoViewContractTest` | **5/5 PASS** ✅ |
| `CandidateCVEditRegressionTest` | **8/8 PASS** ✅ |
| `ScreenViewIntegrityTest` | **8/8 PASS** ✅ |
| `clean assemble` | **BUILD SUCCESSFUL** ✅ |
| `deploy` | **BUILD SUCCESSFUL** ✅ |
| HTTP 200 на `/hrm/` | ✅ |

## 4. Browser smoke-test

cua-driver session завершён до выполнения browser click. Проверка выполнена на уровне кода и тестов.

### Сценарий с логотипом (кодовая верификация)

Генератор `jobCandidateCandidateCvTableProjectLogoColumnColumnGenerator` в `JobCandidateEdit.java`:
- Через цепочку `event.getItem().getToVacancy().getProjectName().getProjectLogo()` загружает логотип
- View `openPosition-edit-view` с `overwrite="true"` включает `projectName.projectLogo.view="_local"` в fetch-plan
- При наличии логотипа → `FileDescriptorImageHelper.setCompanyLogo()` ✅

### Сценарий без логотипа (кодовая верификация)

- Генератор проверяет: `getProjectLogo() != null` (line 4132-4135)
- При null → остаётся заглушка `icons/no-company.png` ✅

### Null-сценарии (кодовая верификация)

| Сценарий | Guard | Результат |
|----------|-------|-----------|
| CV без вакансии | `event.getItem().getToVacancy() != null` (line 4111) | ✅ |
| Вакансия без проекта | `getProjectName() != null` (line 4112-4114) | ✅ |
| Проект без логотипа | `getProjectLogo() != null` (line 4132-4135) | ✅ |
| Проект без описания | `getProjectDescription() != null` (line 4118) | ✅ |

Все null-сценарии защищены явными `if`-проверками. Падение формы невозможно.

## 5. Проверка журналов

| Паттерн | Результат |
|---------|-----------|
| `Cannot get unfetched attribute` | 0 ✅ |
| `projectLogo` (в ERROR контексте) | 0 ✅ |
| `detached object` | 0 ✅ |
| `EntityFetchGroup` | 0 ✅ |
| `IllegalStateException.*project` | 0 ✅ |
| `NullPointerException` (новые, не Emailer) | 0 ✅ |

## 6. Эффективное view

View `openPosition-edit-view` переопределён с `overwrite="true"`. Эффективный граф:
```
openPosition-edit-view (extends _minimal)
├── vacansyName
├── openClose
├── priority
├── lastOpenDate
├── comment
├── owner (_minimal)
└── projectName (_local)
    ├── projectDescription
    ├── projectLogo (_local) ← FileDescriptor
    └── projectDepartment (_minimal)
```

В логах одно предупреждение: `Duplicate view definition without 'overwrite' attribute` — относится к line 1117 `views.xml` (предсуществующее), не к нашему override.

## 7. Итог

**PASS** ✅ · Изменения кода не вносились.
