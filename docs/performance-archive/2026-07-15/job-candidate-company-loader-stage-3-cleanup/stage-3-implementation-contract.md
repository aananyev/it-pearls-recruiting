# Stage 3 — удаление остаточного контейнера компаний

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Рабочая ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Принятый базовый SHA:** `8c792f82e806589a4e0f8d502b77fddd1664f392`  
**Автор технического решения:** ChatGPT  
**Исполнитель точечного patch:** Hermes  

## 1. Назначение этапа

Stage 2 доказал, что полный loader `currentCompaniesLc` можно безопасно исключить из initial open. Stage 3 удаляет временный compatibility-слой:

- `currentCompaniesDc`;
- `currentCompaniesLc`;
- `JobCandidateCompanyLoaderOptimizer`;
- unit-тест временного optimizer.

После сохранения новой компании экран должен точечно загрузить только созданную `Company` через `DataManager` с `company-picker-view`, merge-ить её в экранный `DataContext` и передать в `currentCompanyField`.

## 2. Строгие границы изменений

Разрешено менять только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java
modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateCompanyLoaderOptimizer.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidateCompanyLoaderOptimizerTest.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidateCreatedCompanyResolverTest.java
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-company-loader-stage-3-cleanup/
.ai/active-work.yml
```

Запрещено менять:

- entities;
- `views.xml`;
- Liquibase и БД;
- `currentCompanyField` ID, binding и actions;
- suggestion-поиск `%строка%`, лимит 50 и порог 2 символа;
- экраны `CompanyBrowse` и `CompanyEdit`;
- остальные loaders и progressive-loading Stages 1–3;
- SCSS;
- production.

Не выполнять массовое форматирование `JobCandidateEdit.java` и XML.

## 3. Точный Java patch

### 3.1 Импорт

Добавить:

```java
import java.util.function.Function;
```

### 3.2 Удалить injections

Удалить только:

```java
@Inject
private CollectionLoader<Company> currentCompaniesLc;
@Inject
private CollectionContainer<Company> currentCompaniesDc;
```

### 3.3 Заменить `mergeCreatedCompany`

Текущую реализацию с `currentCompaniesDc.containsItem/add/replaceItem` заменить на:

```java
/**
 * После сохранения CompanyEdit повторно загружает только созданную компанию
 * узким picker-view и merge-ит её в DataContext текущего редактора кандидата.
 */
private Company mergeCreatedCompany(Company company) {
    return resolveCreatedCompany(
            company,
            companyId -> dataManager.load(Company.class)
                    .query("select e from hunttech_Company e where e.id = :companyId")
                    .parameter("companyId", companyId)
                    .view("company-picker-view")
                    .one(),
            dataContext::merge);
}

/**
 * Сохраняет create-company flow тестируемым без полного справочника компаний.
 * Для несохранённой или отменённой сущности не выполняет SQL и merge.
 */
static Company resolveCreatedCompany(Company company,
                                     Function<UUID, Company> companyLoader,
                                     Function<Company, Company> companyMerger) {
    if (company == null || company.getId() == null) {
        return company;
    }

    Company persistedCompany = companyLoader.apply(company.getId());
    return companyMerger.apply(persistedCompany);
}
```

Не менять:

```java
.withTransformation(this::mergeCreatedCompany)
.withField(currentCompanyField)
```

Это сохраняет стандартный возврат выбранного значения из `CompanyEdit` в `SuggestionPickerField`.

## 4. Точный XML patch

Из `job-candidate-edit.xml` полностью удалить только блок:

```xml
<collection id="currentCompaniesDc"
            class="com.company.hunttech.entity.Company">
    <view extends="company-picker-view"/>
    <loader id="currentCompaniesLc" cacheable="true">
        <query><![CDATA[select e from hunttech_Company e order by e.comanyName]]></query>
    </loader>
</collection>
```

`currentCompanyField` не менять. Он должен сохранить:

- `id="currentCompanyField"`;
- `dataContainer="jobCandidateDc"`;
- `property="currentCompany"`;
- `minSearchStringLength="2"`;
- `suggestionsLimit="50"`;
- `asyncSearchDelayMs="300"`;
- actions `lookup` и `open`;
- текущий query `%$searchString%`.

Добавить перед `currentCompanyField` содержательный XML-комментарий на русском, если рядом отсутствует комментарий, объясняющий, что поле выполняет ограниченный серверный поиск и не использует options container.

## 5. Удаление временного Stage 2 слоя

После удаления loader удалить файлы:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateCompanyLoaderOptimizer.java
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidateCompanyLoaderOptimizerTest.java
```

Перед удалением выполнить поиск и подтвердить отсутствие других ссылок:

```bash
grep -R "JobCandidateCompanyLoaderOptimizer" modules/ docs/ --exclude-dir=build
grep -R "currentCompaniesLc\|currentCompaniesDc" modules/ --exclude-dir=build
```

После patch второй поиск должен вернуть ноль совпадений в `modules/`.

## 6. Новый unit-тест

Создать:

```text
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidateCreatedCompanyResolverTest.java
```

Тесты:

```java
package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.Company;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JobCandidateCreatedCompanyResolverTest {

    @Test
    public void nullCompanyDoesNotTriggerLoadOrMerge() {
        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        AtomicBoolean mergerCalled = new AtomicBoolean(false);

        Company result = JobCandidateEdit.resolveCreatedCompany(
                null,
                id -> {
                    loaderCalled.set(true);
                    return null;
                },
                company -> {
                    mergerCalled.set(true);
                    return company;
                });

        assertNull(result);
        assertTrue(!loaderCalled.get());
        assertTrue(!mergerCalled.get());
    }

    @Test
    public void companyWithoutIdDoesNotTriggerLoadOrMerge() {
        Company transientCompany = new Company();
        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        AtomicBoolean mergerCalled = new AtomicBoolean(false);

        Company result = JobCandidateEdit.resolveCreatedCompany(
                transientCompany,
                id -> {
                    loaderCalled.set(true);
                    return null;
                },
                company -> {
                    mergerCalled.set(true);
                    return company;
                });

        assertSame(transientCompany, result);
        assertTrue(!loaderCalled.get());
        assertTrue(!mergerCalled.get());
    }

    @Test
    public void persistedCompanyIsLoadedByIdAndMergedIntoScreenContext() {
        UUID companyId = UUID.randomUUID();
        Company editorResult = new Company();
        editorResult.setId(companyId);
        Company loadedCompany = new Company();
        loadedCompany.setId(companyId);
        Company mergedCompany = new Company();
        mergedCompany.setId(companyId);
        AtomicReference<UUID> loadedId = new AtomicReference<>();
        AtomicReference<Company> mergedValue = new AtomicReference<>();

        Company result = JobCandidateEdit.resolveCreatedCompany(
                editorResult,
                id -> {
                    loadedId.set(id);
                    return loadedCompany;
                },
                company -> {
                    mergedValue.set(company);
                    return mergedCompany;
                });

        assertSame(companyId, loadedId.get());
        assertSame(loadedCompany, mergedValue.get());
        assertSame(mergedCompany, result);
    }
}
```

Комментарии в тесте добавить только для бизнес-инварианта, без комментирования очевидных assertions.

## 7. Документация

Обновить `docs/ui/JobCandidateEdit_Spec.md`:

- в Behavior Summary заменить временную блокировку loader на окончательную архитектуру без полного контейнера;
- удалить описание временного `JobCandidateCompanyLoaderOptimizer`;
- указать точечную загрузку созданной Company через `DataManager` и `company-picker-view`;
- в модели данных явно зафиксировать отсутствие `currentCompaniesDc/currentCompaniesLc`;
- сохранить Business & Context Intro;
- добавить первой строкой истории изменений запись `2026-07-15` о Stage 3.

Создать итоговый отчёт Hermes:

```text
docs/performance-archive/2026-07-15/job-candidate-company-loader-stage-3-cleanup/stage-3-hermes-report.md
```

## 8. Обязательные проверки

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-web:test \
          --tests "com.company.hunttech.web.screens.jobcandidate.JobCandidateCreatedCompanyResolverTest" \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- новый unit-тест PASS;
- `ScreenViewIntegrityTest`: 8/8 PASS;
- `BUILD SUCCESSFUL`;
- deploy локального `/hrm`;
- HTTP 200.

## 9. Runtime smoke-test

Проверить:

1. существующий кандидат с компанией открывается, компания видна сразу;
2. кандидат без компании открывается без ошибки;
3. новый кандидат открывается без ошибки;
4. 0–1 символ не запускают suggestion SQL;
5. 2+ символа возвращают не более 50 компаний;
6. suggestion устанавливает Company;
7. `lookup` выбирает Company;
8. `open` открывает текущую Company;
9. `createCompany` открывает `CompanyEdit`;
10. сохранённая новая Company точечно загружается по ID и устанавливается в поле;
11. отмена создания не выполняет SQL по ID;
12. кандидат сохраняется и повторно открывается с выбранной компанией.

SQL-доказательство:

- отсутствует `select ... from hunttech_Company order by e.comanyName` без фильтра;
- после create выполняется один запрос `where e.id = :companyId`;
- suggestion остаётся отдельным ограниченным запросом;
- `currentCompaniesDc/currentCompaniesLc` отсутствуют в runtime screen model.

Логи не должны содержать:

```text
Cannot get unfetched attribute
IllegalStateException
detached object
NullPointerException
OutOfMemoryError
NoSuchElementException currentCompaniesLc
Injection error currentCompaniesDc
```

## 10. Вердикт

Допустимы только:

```text
STAGE_3_CONFIRMED
STAGE_3_REGRESSION
STAGE_3_BLOCKED
```

До отчёта Hermes Stage 3 не считается принятым.
