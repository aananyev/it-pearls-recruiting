# Company — компания

> Справочник компаний-клиентов и юридических лиц.
> Триггер оптимизации: «оптимизируй сущность Company».

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Справочник `Company` в HRM HuntTech хранит компании-клиенты и юридические лица: название, группа компаний, география (страна, регион, город), логотип и LOB-описания (адрес, описание, условия работы). Используется как FK текущего работодателя кандидата (`JobCandidate.currentCompany`), заказчика в проектах и в структуре департаментов.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экраны: `itpearls_Company.browse` (дерево), `itpearls_Company.edit`, специализированные `itpearls_OurCompany.browse` и `itpearls_ClientsCompany.browse`. Lookup через `company-picker-view` в карточках кандидата, вакансии, проекта. UI Spec: [browse](../ui/itpearls_Company.browse_Spec.md), [edit](../ui/itpearls_Company.edit_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Browse без LOB в view (`company-browse-view`); edit с lazy reload LOB на форме (`company-edit-view`); каскадный picker города с регионом/страной. Потребители: `JobCandidate`, `OpenPosition` (через project/department), `CompanyDepartament`.

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.itpearls.entity.Company` |
| **Имя в CUBA** | `itpearls_Company` |
| **Таблица БД** | `ITPEARLS_COMPANY` |
| **Тип данных** | справочник (тысячи записей; локально на 2026-07-02 ~5 590 активных записей) |
| **Критичность** | высокая — FK в CompanyDepartament, OpenPosition, JobCandidate |

### LOB-поля

| Поле | Колонка |
|------|---------|
| `addressOfCompany` | `ADDRESS_OF_COMPANY` |
| `companyDescription` | `COMPANY_DESCRIPTION` |
| `workingConditions` | `WORKING_CONDITIONS` |

### Индексы FK (дочерние)

`IDX_ITPEARLS_COMPANY_ON_COMPANY_GROUP`, `ON_COUNTRY_OF_COMPANY`, `ON_REGION_OF_COMPANY`, `ON_CITY_OF_COMPANY` — ✅ в init schema.

---

## 4. Представления

| View | Назначение |
|------|------------|
| `company-browse-view` | tree-browse без LOB и без `departmentOfCompany`; содержит FK, показываемые в таблице и используемые генераторами |
| `company-edit-view` | форма без LOB; `cityOfCompany` → `city-location-view` для каскада регион/страна; **обязательно включает `departmentOfCompany`**, потому что `company-edit.xml` содержит nested collection container `departmentOfCompanyDc` |
| `company-picker-view` | lookup / FK в других формах |
| `company-view` | legacy (узкие FK) |
| `company-view-search` | поиск (без LOB) |

---

## 5. Экраны

| Экран | View |
|-------|------|
| Browse / OurCompany / ClientsCompany | `company-browse-view` |
| Edit | `company-edit-view` |

### Оптимизации

- **CompanyBrowse:** batch-кэш `companyDescription` для tooltip логотипа (LOB не в browse view)
- **CompanyBrowse:** убрана повторная автозагрузка `companiesDl` в `BeforeShow`; данные грузит `@LoadDataBeforeShow`, фильтры перезагружают loader только при изменении checkbox.
- **CompanyEdit:** lazy load LOB по вкладкам; каскад город→регион→страна через `dataManager.reload` с `city-location-view` / `region-browse-view` (lookup возвращает узкий picker-view)
- **CompanyEdit / departments:** `departmentOfCompany` нельзя убирать из `company-edit-view`, пока в XML есть `<collection id="departmentOfCompanyDc" property="departmentOfCompany"/>`; иначе при открытии edit из browse возникает `IllegalStateException: Cannot get unfetched attribute [departmentOfCompany] from detached object`.
- **companyGroup** picker → `companyGroup-picker-view` + cacheable loader
- Cross-form: `company-picker-view` в OpenPosition, JobCandidate, LaborAgreement, ApplicationRecruitmentList, group-subscribe

---

## 7. Производительность

**Точка отсчёта:** `ca6d3bb70c0c919308778b5e8e5201d746e06bae`

### Таблица до/после — CompanyBrowse

| Метрика | До | После | Δ | Комментарий |
|---------|-----|-------|---|-------------|
| View | `company-view` (_local) | `company-browse-view` | — | без LOB, без departments |
| LOB в SELECT | 3 поля TOAST | 0 | −3 | address, description, conditions |
| companyGroup expand | `_local` + company list | `companyGroup-picker-view` | −N компаний | |
| Описание в tooltip логотипа | из entity (LOB в view) | 1 batch SQL | −LOB из main view | `CompanyBrowse` cache |
| excludeProperties фильтра | system only | + LOB + departments | — | |

### Замеры 2026-07-02 — CompanyBrowse / CompanyEdit

Замеры сняты web perf-тестами:

```bash
./gradlew app-web:test \
  --tests com.company.itpearls.web.screens.company.CompanyBrowsePerfTest \
  --tests com.company.itpearls.web.screens.company.CompanyEditPerfTest
```

| Метрика | Было | Стало | Комментарий |
|---------|------|-------|-------------|
| `CompanyBrowse` `companyLoadList` | 2 | 1 | удалена повторная загрузка в `CompanyBrowse.onBeforeShow()` |
| `CompanyBrowse` `loadList` всего | 3 | 2 | минус один запрос списка компаний |
| `CompanyBrowse` open time | ~76 759 µs | ~94 078 µs в последнем прогоне | время UI-теста шумное; ключевой стабильный показатель — количество загрузок |
| `CompanyEdit` open time | ~1 660 274 µs | ~1 629 750 µs после отката unsafe view-оптимизации | `departmentOfCompany` возвращён в view из-за nested container |

> Важно: попытка убрать `departmentOfCompany` из `company-edit-view` дала регрессию `Cannot get unfetched attribute [departmentOfCompany]`.
> Поэтому текущая безопасная production-оптимизация — только устранение лишней загрузки Browse.

### Backlog

| Проблема | Приоритет |
|----------|-----------|
| FTS Company в `fts.xml` | низкий |
| Legacy `company-view` в views.xml (JobHistory и др.) | средний |
| cacheable на companiesDl (динамические фильтры) | низкий |

---

## 9. Тесты

`CompanyServiceTest` + `TestEntityTracker`.

```bash
./gradlew :app-core:test --tests "com.company.itpearls.core.CompanyServiceTest"
```

### Regression / view-contract tests

После оптимизаций 2026-07-02 добавлены проверки против unfetched attribute:

- `testBrowseLoadCompany()` вызывает `PersistenceHelper.checkLoadedWithView(loaded, "company-browse-view")`.
- `testEditViewLoadsDepartmentOfCompanyForNestedContainer()` проверяет `company-edit-view`, доступность `departmentOfCompany` и сценарий nested container `departmentOfCompanyDc`.

Эти тесты защищают от повторения ошибки:

```text
IllegalStateException: Cannot get unfetched attribute [departmentOfCompany] from detached object ...
```

Общее правило для дальнейших оптимизаций сущностей: если форма содержит XML-binding, nested collection container, колонку таблицы, lookup или код контроллера, который читает связанное поле, соответствующий view-contract тест должен подтверждать, что это поле загружено.

### Web performance tests

Добавлены web perf-тесты:

- `CompanyBrowsePerfTest` — измеряет время открытия, число загруженных компаний и количество вызовов `DataService`.
- `CompanyEditPerfTest` — измеряет открытие edit screen и количество вызовов `DataService`.
- `CompanyPerfTestSupport` — общий счётчик `load`, `loadList`, `getCount`, `loadValues`, включая отдельные метрики по `itpearls_Company`.

Они нужны для сравнения производительности в формате «было — стало».

---

## 10. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-02 | Актуализация после оптимизации: удалена повторная загрузка `CompanyBrowse`; добавлены perf-тесты и view-contract regression-тесты; зафиксировано, что `departmentOfCompany` обязателен в `company-edit-view` из-за `departmentOfCompanyDc` |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-22 | Исправление unfetched FK на Edit: `company-edit-view` — `cityOfCompany` → `city-location-view`; `CompanyEdit` — reload `cityRegion`/`regionCountry` в обработчиках picker |
| 2026-06-23 | Оптимизация: company-browse/edit/picker views, lazy LOB по вкладкам, batch tooltip `companyDescription`, `CompanyServiceTest`, документация |
