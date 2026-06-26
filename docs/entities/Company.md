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
| **Тип данных** | справочник (сотни записей) |
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
| `company-browse-view` | tree-browse без LOB и departmentOfCompany |
| `company-edit-view` | форма без LOB; `cityOfCompany` → `city-location-view` для каскада регион/страна |
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
- **CompanyEdit:** lazy load LOB по вкладкам; каскад город→регион→страна через `dataManager.reload` с `city-location-view` / `region-browse-view` (lookup возвращает узкий picker-view)
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

---

## 10. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-22 | Исправление unfetched FK на Edit: `company-edit-view` — `cityOfCompany` → `city-location-view`; `CompanyEdit` — reload `cityRegion`/`regionCountry` в обработчиках picker |
| 2026-06-23 | Оптимизация: company-browse/edit/picker views, lazy LOB по вкладкам, batch tooltip `companyDescription`, `CompanyServiceTest`, документация |
