# CompanyDepartament — департамент компании

> **Примечание:** в коде опечатка `CompanyDepartament` (не Department).
> Триггер оптимизации: «оптимизируй сущность CompanyDepartament».

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

`CompanyDepartament` — подразделение/департамент внутри компании-клиента HRM HuntTech. Связывает проекты с организационной структурой заказчика.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

`hunttech_CompanyDepartament.browse`, `hunttech_CompanyDepartament.edit`; FK в `Project.projectDepartment`. UI Spec: [browse](../ui/hunttech_CompanyDepartament.browse_Spec.md), [edit](../ui/hunttech_CompanyDepartament.edit_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Browse с FK на Company; используется в фильтрах и колонках OpenPosition (логотип/название через project → department → company).

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.hunttech.entity.CompanyDepartament` |
| **Имя в CUBA** | `hunttech_CompanyDepartament` |
| **Таблица БД** | `HUNTTECH_COMPANY_DEPARTAMENT` |
| **Тип данных** | справочник |
| **Критичность** | высокая — FK в Project, Person, OpenPosition |

### Индексы производительности (локальная БД, 2026-07-02)

Для ускорения загрузки департаментов конкретной компании на вкладке `CompanyEdit` добавлен частичный индекс:

| Индекс | Таблица / поля | Назначение |
|--------|----------------|------------|
| `IDX_HUNTTECH_COMPANY_DEPT_ACTIVE_COMPANY_NAME` | `HUNTTECH_COMPANY_DEPARTAMENT (COMPANY_NAME_ID, DEPARTAMENT_RU_NAME) WHERE DELETE_TS IS NULL` | загрузка активных департаментов компании с сортировкой по названию |

DDL, применённый на локальной БД:

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS IDX_HUNTTECH_COMPANY_DEPT_ACTIVE_COMPANY_NAME
ON HUNTTECH_COMPANY_DEPARTAMENT (COMPANY_NAME_ID, DEPARTAMENT_RU_NAME)
WHERE DELETE_TS IS NULL;
```

На текущем локальном объёме (`~112` активных департаментов) PostgreSQL может продолжать выбирать `Seq Scan`, потому что таблица очень маленькая. Индекс добавлен как задел на рост данных и на быстрый доступ к департаментам из `CompanyEdit`.

### LOB-поля

| Поле | Колонка |
|------|---------|
| `departamentDescription` | `DEPARTAMENT_DESCRIPTION` |
| `templateLetter` | `TEMPLATE_LETTER` |

---

## 4. Представления

| View | Назначение |
|------|------------|
| `companyDepartament-browse-view` | browse **без LOB** |
| `companyDepartament-edit-view` | edit без LOB, projectOfDepartment — child view |
| `companyDepartament-picker-view` | lookup (Project edit, OpenPosition и др.) |
| `companyDepartament-department-child-view` | таблица департаментов на Company edit |
| `companyDepartament-view` | legacy (исправлена рекурсия companyName) |

**Было:** `companyDepartament-view` с циклическим expand `companyName → departmentOfCompany → companyName`.

---

## 5. Экраны

| Экран | View |
|-------|------|
| Browse | `companyDepartament-browse-view` |
| Edit | `companyDepartament-edit-view` |

### Оптимизации

- Убрана колонка `departamentDescription` из browse (LOB)
- **CompanyDepartamentEdit:** lazy LOB и `projectOfDepartment` по вкладкам
- `companyNamesDc` → `company-picker-view` + cacheable

---

## 7. Производительность

**Точка отсчёта:** `ca6d3bb70c0c919308778b5e8e5201d746e06bae`

### Таблица до/после — CompanyDepartamentBrowse

| Метрика | До | После | Δ | Комментарий |
|---------|-----|-------|---|-------------|
| View | `companyDepartament-view` | `companyDepartament-browse-view` | — | |
| Рекурсивный expand company | 3+ уровня _local | `company-picker-view` | −рекурсия | |
| LOB в browse | departamentDescription колонка + view | убрано | −TOAST | |
| projectOfDepartment в browse | да (_local) | нет | −N проектов | |
| Полей в view (оценка) | ~20+ | 6 | −14 | |

### Замеры индекса локальной БД — 2026-07-02

Текущий объём `HUNTTECH_COMPANY_DEPARTAMENT` мал: около `116` строк всего и `112` активных.
Поэтому запрос вкладки департаментов конкретной компании выполняется менее чем за `1 ms`, а планер может предпочитать последовательное сканирование.

Индекс `IDX_HUNTTECH_COMPANY_DEPT_ACTIVE_COMPANY_NAME` важен как защита при росте числа департаментов и компаний:

```sql
SELECT id, departament_ru_name, departament_director_id, departament_hr_director_id
FROM hunttech_company_departament
WHERE delete_ts IS NULL
  AND company_name_id = :companyId
ORDER BY departament_ru_name;
```

### Backlog

| Проблема | Приоритет |
|----------|-----------|
| FTS CompanyDepartament | низкий |
| Индекс на `DEPARTAMENT_RU_NAME` для общего Browse/search | низкий; для вкладки CompanyEdit уже есть составной локальный индекс по `(COMPANY_NAME_ID, DEPARTAMENT_RU_NAME)` |
| Вернуть колонку «есть описание» через batch-иконку | низкий |

---

## 9. Тесты

`CompanyDepartamentServiceTest` + `TestEntityTracker`.

```bash
./gradlew :app-core:test --tests "com.company.hunttech.core.CompanyDepartamentServiceTest"
```

---

## 10. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-02 | Локальная БД: добавлен частичный индекс `IDX_HUNTTECH_COMPANY_DEPT_ACTIVE_COMPANY_NAME` для активных департаментов компании |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-22 | Аудит Edit unfetched FK: `CompanyDepartamentEdit` без каскадных обработчиков; lazy LOB/projects через reload — OK |
| 2026-06-23 | Оптимизация: устранена рекурсия `companyName` в `companyDepartament-view`, browse/edit/picker views, lazy LOB, `CompanyDepartamentServiceTest`, документация |
