# Project — проект

> Справочник/мастер-данные проектов клиентов и внутренних инициатив.
> Триггер оптимизации: «оптимизируй сущность Project».

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Сущность `Project` — проект/контракт клиента в HRM HuntTech: название, логотип, департамент компании, владелец (Person). Каждая вакансия `OpenPosition` обязательно привязана к проекту (`projectName`).

### Связи в интерфейсе и Навигация (UI Context & Navigation)

`itpearls_Project.browse`, `itpearls_Project.edit`; FK в `OpenPosition`, фильтры browse вакансий. UI Spec: [browse](../ui/itpearls_Project.browse_Spec.md), [edit](../ui/itpearls_Project.edit_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Browse без LOB descriptions в основном SELECT; nested `projectOwner` и `projectDepartment` в `openPosition-browse-view`; lazy exists для описаний проекта/компании в OpenPositionBrowse.

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.itpearls.entity.Project` |
| **Имя в CUBA** | `itpearls_Project` |
| **Таблица БД** | `ITPEARLS_PROJECT` |
| **Тип данных** | мастер-данные (десятки–сотни записей, дерево) |
| **Критичность** | высокая — FK в OpenPosition, ApplicationRecruitmentList |
| **Ответственный модуль** | `global`, `web`, `core` |

### Назначение

Проект объединяет вакансии (`OpenPosition`), привязан к департаменту компании и владельцу (`Person`). Поддерживает иерархию через `projectTree`, логотип, чаты, LOB-описание и шаблон письма.

### LOB-поля

| Поле | Колонка |
|------|---------|
| `projectDescription` | `PROJECT_DESCRIPTION` |
| `templateLetter` | `TEMPLATE_LETTER` |

---

## 4. Представления (views.xml)

| View | Назначение | Где используется |
|------|------------|------------------|
| `project-browse-view` | колонки tree-browse **без LOB** | `project-browse.xml` |
| `project-edit-view` | поля формы **без LOB и openPosition** | `project-edit.xml`, CRUD-тесты |
| `project-picker-view` | lookup / FK | OpenPosition, ApplicationRecruitmentList, group-subscribe |
| `project-tree-picker-view` | родитель в дереве | project-edit, project-browse (projectTree) |
| `project-department-child-view` | проекты департамента на вкладке | CompanyDepartament edit |
| `openPosition-project-tab-view` | вакансии на вкладке Edit Project | lazy load |
| `project-view` | legacy (_local, узкие FK) | совместимость |

---

## 5. Экраны

| Экран | Controller | View |
|-------|------------|------|
| Browse (tree) | `itpearls_Project.browse` | `project-browse-view` |
| Edit | `itpearls_Project.edit` | `project-edit-view` |

### Оптимизации экранов

- **Browse:** `readOnly`, узкий `excludeProperties` (LOB, openPosition)
- **Browse Java:** batch-кэш счётчиков открытых вакансий и LOB-описаний (`ProjectBrowse`)
- **Edit Java:** lazy load LOB и коллекции `openPosition` по вкладкам (`ProjectEdit`); вакансии — отдельный `CollectionLoader` с JPQL `where e.projectName = :project`, без привязки к `property="openPosition"` на instance (избегает unfetched при `@LoadDataBeforeShow`)
- **Loaders:** `companyDepartament-picker-view` + `cacheable`, `person-picker-view` + `cacheable`

---

## 7. Производительность

**Точка отсчёта:** `ca6d3bb70c0c919308778b5e8e5201d746e06bae`

### Таблица до/после — ProjectBrowse

| Метрика | До | После | Δ | Комментарий |
|---------|-----|-------|---|-------------|
| View | `project-view` + inline `_local` | `project-browse-view` | — | убраны LOB |
| LOB в основном SELECT | да (`projectDescription`, `templateLetter`) | нет | −2 LOB | TOAST не тянется при пагинации |
| SQL на строку (счётчик вакансий) | 1 запрос/строка | 1 batch после load | −(N−1) | `refreshOpenPositionCountCache` |
| SQL на LOB для иконки описания | в основном view | 1 batch после load | − | `refreshProjectDescriptionCache` |
| Полей в view (оценка) | ~15+ _local | ~12 scalar/FK | − | person-owner-view, companyDepartament-picker-view |
| Глубина FK projectDepartment | `_local` + company `_local` | `companyDepartament-picker-view` | −2 уровня | |

### Backlog

| Проблема | Приоритет |
|----------|-----------|
| FTS на Project в `fts.xml` | низкий |
| Legacy `project-view` | низкий |
| `ProjectServiceBean.createProjectDefault` пишет LOB при создании | низкий |
| Entity cache для дерева проектов | низкий |

---

## 9. Тесты

`ProjectServiceTest` — create, edit, browse load (без LOB), soft delete. `TestEntityTracker` для очистки.

```bash
./gradlew :app-core:test --tests "com.company.itpearls.core.ProjectServiceTest"
```

---

## 10. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-23 | Fix ProjectEdit: `projectOpenPositionsDc` — standalone loader вместо `property="openPosition"`; lazy load по вкладке «Вакансии» |
| 2026-06-22 | Аудит Edit unfetched FK: `ProjectEdit` без каскадных обработчиков location; lazy LOB/collections через reload — OK |
| 2026-06-23 | Оптимизация: project-browse/edit/picker/tree-picker views, lazy LOB и `openPosition` по вкладкам, batch N+1 в `ProjectBrowse`, `ProjectServiceTest`, документация |
