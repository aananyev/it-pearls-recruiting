# CompanyGroup — группа компаний

> Простой справочник для группировки компаний в tree-browse Company.
> Триггер оптимизации: «оптимизируй сущность CompanyGroup».

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.itpearls.entity.CompanyGroup` |
| **Имя в CUBA** | `itpearls_CompanyGroup` |
| **Таблица БД** | `ITPEARLS_COMPANY_GROUP` |
| **Тип данных** | справочник (мало записей) |
| **LOB** | нет |

Единственное бизнес-поле: `companyRuGroupName`. Composition `company` → `Company.companyGroup`.

---

## 4. Представления

| View | Назначение |
|------|------------|
| `companyGroup-browse-view` | browse (только имя) |
| `companyGroup-edit-view` | edit |
| `companyGroup-picker-view` | FK в Company edit / browse |
| `companyGroup-view` | legacy (`company` → `company-picker-view`) |

---

## 5. Экраны

| Экран | View |
|-------|------|
| Browse | `companyGroup-browse-view` |
| Edit | `companyGroup-edit-view` |

### Оптимизации

- `cacheable="true"` на browse loader
- Убрана загрузка всех `company` (_local) при открытии browse/edit группы

---

## 7. Производительность

**Точка отсчёта:** `ca6d3bb70c0c919308778b5e8e5201d746e06bae`

### Таблица до/после — CompanyGroupBrowse

| Метрика | До | После | Δ | Комментарий |
|---------|-----|-------|---|-------------|
| View | `_local` + company list | `companyGroup-browse-view` | — | 1 поле |
| SQL при открытии (оценка) | 1 + N companies | 1 | −N | |
| cacheable loader | нет | да | + | справочник |
| Полей в view | 1 + N×company _local | 1 | −N | |

### Backlog

| Проблема | Приоритет |
|----------|-----------|
| FTS CompanyGroup | низкий |
| Entity cache | низкий |

---

## 9. Тесты

`CompanyGroupServiceTest` + `TestEntityTracker`.

```bash
./gradlew :app-core:test --tests "com.company.itpearls.core.CompanyGroupServiceTest"
```

---

## 10. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-22 | Аудит Edit unfetched FK: `CompanyGroupEdit` — scalar-поля, вложенных FK в обработчиках нет — OK |
| 2026-06-23 | Оптимизация: companyGroup-browse/edit/picker views, cacheable loader, убрана загрузка списка `company`, `CompanyGroupServiceTest`, документация |
