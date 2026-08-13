# Project — проект

> Справочник/мастер-данные проектов клиентов и внутренних инициатив.
> Триггер оптимизации: «оптимизируй сущность Project».

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Сущность `Project` — проект/контракт клиента в HRM HuntTech: название, логотип, департамент компании, владелец (Person). Каждая вакансия `OpenPosition` обязательно привязана к проекту (`projectName`).

### Связи в интерфейсе и Навигация (UI Context & Navigation)

`hunttech_Project.browse`, `hunttech_Project.edit`; FK в `OpenPosition`, фильтры browse вакансий. UI Spec: [browse](../../screens/project/hunttech_Project.browse_Spec.md), [edit — канон](../../ui/ProjectEdit_Spec.md), [edit — legacy](../../screens/project/hunttech_Project.edit_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Browse без LOB descriptions в основном SELECT; nested `projectOwner` и `projectDepartment` в `openPosition-browse-view`; lazy exists для описаний проекта/компании в OpenPositionBrowse.

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.hunttech.entity.Project` |
| **Имя в CUBA** | `hunttech_Project` |
| **Таблица БД** | `HUNTTECH_PROJECT` |
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

### Индексы производительности

Для PostgreSQL добавлены частичные индексы под фактические запросы Project-экранов и связанных сервисов:
| Индекс | Выражение | Назначение |
|--------|-----------|------------|
| `IDX_HUNTTECH_PROJECT_ACTIVE_NAME` | `HUNTTECH_PROJECT (PROJECT_NAME, ID) WHERE DELETE_TS IS NULL AND PROJECT_IS_CLOSED = FALSE` | основной активный список `ProjectBrowse` с сортировкой по имени |
| `IDX_HUNTTECH_PROJECT_TREE_PICKER_ACTIVE_NAME` | `HUNTTECH_PROJECT (PROJECT_NAME, ID) WHERE DELETE_TS IS NULL AND NOT (PROJECT_IS_CLOSED = TRUE)` | lookup родительского проекта в `ProjectEdit` |
| `IDX_HUNTTECH_PROJECT_DEFAULT_ACTIVE` | `HUNTTECH_PROJECT (ID) WHERE DELETE_TS IS NULL AND DEFAULT_PROJECT = TRUE` | `ProjectServiceBean.createProjectDefault()` |
| `IDX_HUNTTECH_OPEN_POSITION_OPEN_PROJECT` | `HUNTTECH_OPEN_POSITION (PROJECT_NAME_ID) WHERE DELETE_TS IS NULL AND NOT (OPEN_CLOSE = TRUE)` | `ProjectBrowse` / `ProjectEdit`: открытые вакансии проекта |

HSQL получил обычные составные аналоги этих индексов для локальных и тестовых контуров.

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
| Browse (tree) | `hunttech_Project.browse` | `project-browse-view` |
| Edit | `hunttech_Project.edit` | `project-edit-view` |

### Оптимизации экранов

- **Browse:** `readOnly`, узкий `excludeProperties` (LOB, openPosition)
- **Browse Java:** batch-кэш счётчиков открытых вакансий и LOB-описаний (`ProjectBrowse`)
- **Browse Java:** начальные фильтры задаются в `InitEvent` до `@LoadDataBeforeShow`; вложенные изменения checkbox не вызывают повторные `projectsDl.load()`
- **Edit Java:** lazy load LOB и коллекции `openPosition` по вкладкам (`ProjectEdit`); вакансии — отдельный `CollectionLoader` с JPQL `where e.projectName = :project`, без привязки к `property="openPosition"` на instance (избегает unfetched при `@LoadDataBeforeShow`)
- **Edit Java:** новый несохранённый проект не выполняет запрос открытых вакансий, потому что связанных строк ещё не может быть
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
| Повторная загрузка на старте Browse | checkbox handlers + ручной `load()` | параметры до автозагрузки | −1/−2 load при открытии | `initDefaultProjectFilters` |
| Запрос открытых вакансий для нового ProjectEdit | выполнялся всегда | пропускается для new entity | −1 loadList | `getOpenedPosition()` |
| Полей в view (оценка) | ~15+ _local | ~12 scalar/FK | − | person-owner-view, companyDepartament-picker-view |
| Глубина FK projectDepartment | `_local` + company `_local` | `companyDepartament-picker-view` | −2 уровня | |

### Замеры 2026-07-04 — ProjectBrowse / ProjectEdit

```bash
./gradlew :app-web:test \
  --tests com.company.hunttech.web.screens.project.ProjectBrowsePerfTest \
  --tests com.company.hunttech.web.screens.project.ProjectEditPerfTest
```

| Тест | Результат |
|------|-----------|
| `ProjectBrowsePerfTest` | `BUILD SUCCESSFUL`; `projectLoadList=2`, `loadValues=0` на пустом тестовом наборе |
| `ProjectEditPerfTest` | `BUILD SUCCESSFUL`; `projectLoadList=1`, без запроса открытых вакансий для нового проекта |

Локальная PostgreSQL БД получила индексы `IDX_HUNTTECH_PROJECT_ACTIVE_NAME`, `IDX_HUNTTECH_PROJECT_TREE_PICKER_ACTIVE_NAME`, `IDX_HUNTTECH_PROJECT_DEFAULT_ACTIVE`, `IDX_HUNTTECH_OPEN_POSITION_OPEN_PROJECT`. На текущем небольшом объёме `HUNTTECH_PROJECT` планировщик может выбирать `Seq Scan`; индексы рассчитаны на рост объёма, lookup-запросы и фильтр открытых вакансий.

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

`ProjectBrowsePerfTest`, `ProjectEditPerfTest`, `ProjectPerfTestSupport` — web-level счётчики `DataService` по аналогии с Company performance suite.

```bash
./gradlew :app-core:test --tests "com.company.hunttech.core.ProjectServiceTest"
```

---

## 10. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-13 | Обработка логотипа проекта переведена на AI-first: функция `PROJECT_LOGO_IMAGE_GENERATE` (capability IMAGE_GENERATION, OpenAI `gpt-image-2`) удаляет фон, классический конвейер (ресайз, flood-fill, круг) — детерминированный финал и fallback; исправлена интеграция `hunttech_ProjectLogoImageProcessingService` web↔core (`WebRemoteProxyBeanCreator`), AI-слой получил `AiExecutionService.executeImage`; конфиг `hunttech.projectLogo.ai.enabled` |
| 2026-08-12 | ProjectEdit отрефакторена по контракту Edit-форм: sidebar 270px (визуал логотипа 96×96, identity, label-навигация «Разделы», spacer, hint), workspace (toolbar, tabSheet edit-tabs, карточки edit-card, footer), presentation-only Java-навигация; канонический UI Spec — `docs/ui/ProjectEdit_Spec.md`, контрактный тест `ProjectEditLayoutContractTest`, detached-тест `ProjectDetachedObjectTest` |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-23 | Fix ProjectEdit: `projectOpenPositionsDc` — standalone loader вместо `property="openPosition"`; lazy load по вкладке «Вакансии» |
| 2026-06-22 | Аудит Edit unfetched FK: `ProjectEdit` без каскадных обработчиков location; lazy LOB/collections через reload — OK |
| 2026-06-23 | Оптимизация: project-browse/edit/picker/tree-picker views, lazy LOB и `openPosition` по вкладкам, batch N+1 в `ProjectBrowse`, `ProjectServiceTest`, документация |
| 2026-07-04 | Оптимизация Project по сценарию Company: стартовые фильтры без повторных load, skip open-position query для нового ProjectEdit, PostgreSQL/HSQL индексы, Project perf-тесты |
| 2026-08-12 | Автоматическая обработка загружаемого логотипа (`projectLogo`): конвертация в PNG, ресайз до 300px, удаление белого фона, вписывание в круг — кастомный загрузчик `WebProjectLogoFileUploadField` + `ProjectLogoImageProcessingService`; структура сущности не менялась |
