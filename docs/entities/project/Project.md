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
| `shortDescription` | `SHORT_DESCRIPTION` |
| `templateLetter` | `TEMPLATE_LETTER` |

`shortDescription` («Коротко о проекте») — краткое описание сути проекта (два предложения — генерация в 2 раза больше изначальной редакции), генерируется AI по кнопке «Кратко» во вкладке «Описание проекта» ProjectEdit и выводится в sidebar-разделе «Коротко». В отличие от `projectDescription`/`templateLetter` грузится сразу в `project-edit-view` (раздел sidebar виден при открытии формы, без lazy load по вкладкам).

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
| `project-edit-view` | поля формы **без LOB-описаний и openPosition** (исключение: `shortDescription` — нужен sidebar-разделу «Коротко» при открытии) | `project-edit.xml`, CRUD-тесты |
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
- **Edit AI «Кратко»:** кнопка «Кратко» во вкладке «Описание проекта» генерирует `shortDescription` (sidebar-раздел «Коротко») через `ProjectAiService.generateShortDescription` → `PROJECT_SHORT_DESCRIPTION_GENERATE`; генерация сокращена в 4 раза (одно предложение, `MAX_TOKENS` 125, миграция 260814-3), затем увеличена в 2 раза (два предложения, `MAX_TOKENS` 250, миграция 260814-4); кнопка disabled без текста описания, раздел sidebar скрыт при пустом `shortDescription`; sidebar-порядок: идентификация (только название, подпись «Проект» удалена) → «Разделы» → «Коротко», заголовок «Коротко» — полоса как у «Разделы», при переполнении sidebar скроллится тонким скроллбаром
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
| 2026-08-14 | ProjectEdit: исправлена строка дат — shared `.v-slot-edit-form-control { width: 100% !important }` перебивал expandRatio (50/50), «Дата окончания проекта» выталкивалась за границу; слоты растягиваются Vaadin-инлайном (7 тем) |
| 2026-08-14 | ProjectEdit: AI-генерация «Кратко» увеличена в 2 раза (два предложения вместо одного, `MAX_TOKENS` 125→250, миграция 260814-4); из sidebar убрана подпись типа записи «Проект»; sidebar скроллится при переполнении (тонкий скроллбар, SCSS 7 тем) |
| 2026-08-14 | ProjectEdit sidebar: блок «Коротко» перенесён после навигации «Разделы»; заголовок «Коротко» — полоса-заголовок как у «Разделы» (`label-nav-title project-editor-short-description-title`); AI-генерация «Кратко» сокращена в 4 раза (одно предложение, `MAX_TOKENS` 500→125; seed 260814-2 + миграция 260814-3); убрана подсказка «PDF, DOCX или TXT до 10 МБ…», кнопки «Загрузить описание»/«Кратко» выровнены вправо |
| 2026-08-14 | Обработка логотипа: локальный rembg-этап (бесплатная нейросеть u2net, systemd rembg.service на сервере приложения, `POST 127.0.0.1:7000/api/remove`) — первый шаг AI-конвейера перед платной функцией `PROJECT_LOGO_IMAGE_GENERATE`; недоступность rembg → AI → классический flood-fill; конфиг `hunttech.projectLogo.rembg.{enabled,url,timeoutMs}` |
| 2026-08-14 | «Кратко о проекте»: новое поле `shortDescription` (`SHORT_DESCRIPTION`, CLOB) — краткое описание сути проекта (до 5 предложений); AI-функция `PROJECT_SHORT_DESCRIPTION_GENERATE` (capability TEXT_GENERATION), кнопка «Кратко» во вкладке «Описание проекта» ProjectEdit, sidebar-раздел «Коротко» (виден при непустом значении); поле добавлено в `project-edit-view` |
| 2026-08-14 | Обработка логотипа: классический конвейер дополнительно удаляет серый фон-градиенты (логотип SSP) — пиксели с насыщенностью ≤ 30 и яркостью ≥ 40, соединённые с краем, полностью прозрачны (`graySaturationThreshold`/`grayMinChannel`); белый фон — без изменений |
| 2026-08-14 | ProjectEdit: строка дат — обе даты на одной строке одинакового размера (box.expandRatio 50/50) |
| 2026-08-14 | ProjectEdit: компоновка рабочей области — поля вкладки «Проект» на всю ширину (50%→100%), RichTextArea описания и dataGrid вакансий ограничены по ширине (SCSS min-width: 0 / max-width: 100%) |
| 2026-08-13 | Обработка логотипа проекта переведена на AI-first: функция `PROJECT_LOGO_IMAGE_GENERATE` (capability IMAGE_GENERATION, OpenAI `gpt-image-2`) удаляет фон, классический конвейер (ресайз, flood-fill, круг) — детерминированный финал и fallback; исправлена интеграция `hunttech_ProjectLogoImageProcessingService` web↔core (`WebRemoteProxyBeanCreator`), AI-слой получил `AiExecutionService.executeImage`; конфиг `hunttech.projectLogo.ai.enabled` |
| 2026-08-12 | ProjectEdit отрефакторена по контракту Edit-форм: sidebar 270px (визуал логотипа 96×96, identity, label-навигация «Разделы», spacer, hint), workspace (toolbar, tabSheet edit-tabs, карточки edit-card, footer), presentation-only Java-навигация; канонический UI Spec — `docs/ui/ProjectEdit_Spec.md`, контрактный тест `ProjectEditLayoutContractTest`, detached-тест `ProjectDetachedObjectTest` |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-23 | Fix ProjectEdit: `projectOpenPositionsDc` — standalone loader вместо `property="openPosition"`; lazy load по вкладке «Вакансии» |
| 2026-06-22 | Аудит Edit unfetched FK: `ProjectEdit` без каскадных обработчиков location; lazy LOB/collections через reload — OK |
| 2026-06-23 | Оптимизация: project-browse/edit/picker/tree-picker views, lazy LOB и `openPosition` по вкладкам, batch N+1 в `ProjectBrowse`, `ProjectServiceTest`, документация |
| 2026-07-04 | Оптимизация Project по сценарию Company: стартовые фильтры без повторных load, skip open-position query для нового ProjectEdit, PostgreSQL/HSQL индексы, Project perf-тесты |
| 2026-08-12 | Автоматическая обработка загружаемого логотипа (`projectLogo`): конвертация в PNG, ресайз до 300px, удаление белого фона, вписывание в круг — кастомный загрузчик `WebProjectLogoFileUploadField` + `ProjectLogoImageProcessingService`; структура сущности не менялась |
