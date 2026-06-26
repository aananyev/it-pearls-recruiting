# CompanyDepartament — департамент компании

> **Примечание:** в коде опечатка `CompanyDepartament` (не Department).
> Триггер оптимизации: «оптимизируй сущность CompanyDepartament».

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

`CompanyDepartament` — подразделение/департамент внутри компании-клиента HRM HuntTech. Связывает проекты с организационной структурой заказчика.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

`itpearls_CompanyDepartament.browse`, `itpearls_CompanyDepartament.edit`; FK в `Project.projectDepartment`. UI Spec: [browse](../ui/itpearls_CompanyDepartament.browse_Spec.md), [edit](../ui/itpearls_CompanyDepartament.edit_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Browse с FK на Company; используется в фильтрах и колонках OpenPosition (логотип/название через project → department → company).

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.itpearls.entity.CompanyDepartament` |
| **Имя в CUBA** | `itpearls_CompanyDepartament` |
| **Таблица БД** | `ITPEARLS_COMPANY_DEPARTAMENT` |
| **Тип данных** | справочник |
| **Критичность** | высокая — FK в Project, Person, OpenPosition |

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

### Backlog

| Проблема | Приоритет |
|----------|-----------|
| FTS CompanyDepartament | низкий |
| Индекс на `DEPARTAMENT_RU_NAME` (есть в HSQL migrations) | низкий |
| Вернуть колонку «есть описание» через batch-иконку | низкий |

---

## 9. Тесты

`CompanyDepartamentServiceTest` + `TestEntityTracker`.

```bash
./gradlew :app-core:test --tests "com.company.itpearls.core.CompanyDepartamentServiceTest"
```

---

## 10. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-22 | Аудит Edit unfetched FK: `CompanyDepartamentEdit` без каскадных обработчиков; lazy LOB/projects через reload — OK |
| 2026-06-23 | Оптимизация: устранена рекурсия `companyName` в `companyDepartament-view`, browse/edit/picker views, lazy LOB, `CompanyDepartamentServiceTest`, документация |
