# Документация HRM HuntTech

Проект рекрутинговой системы **HRM HuntTech** на **CUBA Platform 7.3**.

Документация предназначена для разработчиков: описание сущностей, экранов, схемы БД и практик оптимизации производительности.

---

## Структура каталога

```
docs/
├── README.md                 ← этот файл (индекс)
├── LOCAL_DATABASE.md         ← локальная PostgreSQL, миграции, запуск
├── 02_business_logic/        ← TDD бизнес-логики (сквозные процессы)
│   ├── db-schema-diff-report.md   ← отчёт: расхождения PostgreSQL ↔ модель приложения
│   └── user-settings-photo-sync.md
├── architecture/             ← полные спецификации по триггеру (см. ниже)
│   └── README.md
├── ui/
│   ├── README.md               ← каталог UI Spec
│   ├── archive/                ← удалённые UI Spec
│   ├── login-screen.md         ← legacy (kebab); канон при миграции: loginBranded_Spec.md
│   └── {FormName}_Spec.md      ← living UI (GLOBAL UI TRIGGER)
├── templates/
│   ├── entity-template.md    ← шаблон описания сущности CUBA
│   └── ui-template.md        ← шаблон UI Spec (6 разделов)
├── services/
│   └── ImageProcessingService.md  ← сжатие/масштабирование фото профиля
├── components/
│   └── FallbackImage.md           ← UI-компонент placeholder для image
├── ui-components/
│   ├── OvalImage.md               ← круглый аватар (extends Image)
│   └── OvaFallbackImage.md        ← круг + fallback (OvalImage + FallbackImage)
└── entities/
    ├── IteractionList.md      ← взаимодействие с кандидатом
    ├── Iteraction.md         ← тип взаимодействия с кандидатом (образец)
    ├── Person.md             ← сотрудник / контактное лицо
    ├── Country.md              ← справочник стран
    ├── Region.md               ← справочник регионов
    ├── City.md                 ← справочник городов
    ├── Project.md              ← проекты
    ├── Company.md              ← компании
    ├── CompanyDepartament.md   ← департаменты компаний
    ├── CompanyGroup.md         ← группы компаний
    ├── SkillTree.md            ← дерево компетенций
    ├── OpenPosition.md         ← вакансии
    ├── OpenPositionComment.md  ← комментарии к вакансии
    ├── OpenPositionNews.md     ← новости вакансии
    ├── Grade.md                ← грейды
    ├── VacancyPromptTemplate.md ← шаблоны AI-промптов вакансий
    ├── UserAiConfiguration.md  ← персональные AI-ключи пользователей
    ├── ExtUser.md              ← расширенный пользователь (фото, почта, AI)
    ├── Position.md             ← должности
    ├── JobCandidate.md         ← кандидаты (подсистема: entity + browse + edit)
    └── RecrutiesTasks.md       ← подписки рекрутёров
```

---

## Быстрый старт

| Задача | Документ |
|--------|----------|
| Поднять локальную БД и приложение | [LOCAL_DATABASE.md](LOCAL_DATABASE.md) |
| Расхождения схемы БД и приложения | [02_business_logic/db-schema-diff-report.md](02_business_logic/db-schema-diff-report.md) |
| Архитектура подсистемы AI (LLM) | [../AI_INTEGRATION.md](../AI_INTEGRATION.md) |
| Экран входа (login) | [ui/login-screen.md](ui/login-screen.md) (legacy) · [ui/README.md](ui/README.md) |
| Описать UI-форму (экран, фрагмент) | [templates/ui-template.md](templates/ui-template.md) · [living-ui-documentation.mdc](../.cursor/rules/living-ui-documentation.mdc) |
| Описать новую сущность | [templates/entity-template.md](templates/entity-template.md) |
| Оптимизировать существующую сущность | [.cursor/rules/entity-performance-optimization.mdc](../.cursor/rules/entity-performance-optimization.mdc) |
| Ограничение структуры БД при оптимизации | [`.cursorrules`](../.cursorrules) § «ОПТИМИЗАЦИЯ: ОГРАНИЧЕНИЕ СТРУКТУРЫ БД» · [entity-performance-optimization.mdc](../.cursor/rules/entity-performance-optimization.mdc) |
| View ↔ Java integrity (UNFETCHED ATTRIBUTE ACCESS) | [.cursor/rules/data-view-integrity.mdc](../.cursor/rules/data-view-integrity.mdc) |
| Living Documentation (код ↔ docs, DoD) | [living-documentation.mdc](../.cursor/rules/living-documentation.mdc) · [living-ui-documentation.mdc](../.cursor/rules/living-ui-documentation.mdc) · [`.cursorrules`](../.cursorrules) |
| Полная спецификация сущности (триггер) | «Сделай документацию сущности {Имя}» → [architecture/{Имя}_Spec.md](architecture/README.md) |
| Зафиксировать дату в «История изменений» | [.cursor/rules/documentation-with-dates.mdc](../.cursor/rules/documentation-with-dates.mdc) |
| Понять устройство типов взаимодействий | [entities/Iteraction.md](entities/Iteraction.md) · UI: [каталог](ui/README.md#iteraction) |
| Понять записи взаимодействий с кандидатами | [entities/IteractionList.md](entities/IteractionList.md) · UI: [каталог](ui/README.md#iteractionlist) |
| Понять справочник персон (сотрудники) | [entities/Person.md](entities/Person.md) |
| Понять справочник стран | [entities/Country.md](entities/Country.md) |
| Понять справочник городов | [entities/City.md](entities/City.md) |
| Понять справочник регионов | [entities/Region.md](entities/Region.md) |
| Понять справочник проектов | [entities/Project.md](entities/Project.md) · UI: [browse](ui/itpearls_Project.browse_Spec.md), [edit](ui/itpearls_Project.edit_Spec.md) |
| Понять справочник компаний | [entities/Company.md](entities/Company.md) · UI: [каталог](ui/README.md#company) |
| Понять департаменты компаний | [entities/CompanyDepartament.md](entities/CompanyDepartament.md) · UI: [browse](ui/itpearls_CompanyDepartament.browse_Spec.md), [edit](ui/itpearls_CompanyDepartament.edit_Spec.md) |
| Понять группы компаний | [entities/CompanyGroup.md](entities/CompanyGroup.md) · UI: [browse](ui/itpearls_CompanyGroup.browse_Spec.md), [edit](ui/itpearls_CompanyGroup.edit_Spec.md) |
| Понять дерево компетенций | [entities/SkillTree.md](entities/SkillTree.md) · UI: [browse](ui/itpearls_SkillTree.browse_Spec.md), [edit](ui/itpearls_SkillTree.edit_Spec.md) |
| Понять подсистему кандидатов | [entities/JobCandidate.md](entities/JobCandidate.md) · UI: [каталог](ui/README.md#jobcandidate) |
| Понять подсистему вакансий | [entities/OpenPosition.md](entities/OpenPosition.md) · [architecture/OpenPosition_Spec.md](architecture/OpenPosition_Spec.md) · UI: [каталог](ui/README.md#openposition) |
| Обработка фото профиля (сжатие, лимиты) | [services/ImageProcessingService.md](services/ImageProcessingService.md) · [entities/ExtUser.md](entities/ExtUser.md) |
| Модуль пользователя и синхронизация фото (TDD) | [02_business_logic/user-settings-photo-sync.md](02_business_logic/user-settings-photo-sync.md) |
| UI-компонент FallbackImage (placeholder для image) | [components/FallbackImage.md](components/FallbackImage.md) · [ui/FallbackImage_Component.md](ui/FallbackImage_Component.md) |
| UI-компонент OvalImage (круглый аватар) | [ui-components/OvalImage.md](ui-components/OvalImage.md) |
| UI-компонент OvaFallbackImage (круг + placeholder) | [ui-components/OvaFallbackImage.md](ui-components/OvaFallbackImage.md) |
| Каталог всех UI Spec | [ui/README.md](ui/README.md) (47 форм) |

---

## Архитектурные спецификации (`docs/architecture/`)

**Триггерная фраза:** «Сделай документацию сущности {Имя}» (например: JobCandidate, OpenPosition)

| Сценарий | Путь | Разделы |
|----------|------|---------|
| Триггер (полная регенерация из кода) | `architecture/{EntityName}_Spec.md` | Intro + 6: Data Model, Views, Browse, Edit, Fragments, Deployment |
| Living sync (правка кода в сессии) | `entities/{EntityName}.md` | Intro + 5 блоков living-doc |

Оба каталога **не объединяются**: architecture — канон для one-pass спецификации по запросу; entities — инкрементальная синхронизация при изменении кода. При наличии обоих файлов — cross-links в шапке.

Подробности: [architecture/README.md](architecture/README.md)

---

## UI-спецификации (`docs/ui/`)

**GLOBAL UI TRIGGER:** create / modify / fix / delete экрана, окна, фрагмента, меню → синхронизировать `docs/ui/{FormName}_Spec.md` в той же сессии.

| Сценарий | Путь | Разделы |
|----------|------|---------|
| Living sync UI | `ui/{FormName}_Spec.md` | Intro + 6: Invocation, Data Binding, Hierarchy, Behavior, Actions, Layout |
| Legacy | `ui/{kebab-name}.md` | напр. [login-screen.md](ui/login-screen.md) |
| Архив удалённого UI | `ui/archive/{FormName}_Spec.md` | после delete из кода |
| Шаблон | [templates/ui-template.md](templates/ui-template.md) | ручное создание |

Каталог и соглашения: [ui/README.md](ui/README.md). Правила агента: [living-ui-documentation.mdc](../.cursor/rules/living-ui-documentation.mdc).

### Описанные UI-формы

Полный каталог **47 UI Spec** по 16 documented entities: [ui/README.md](ui/README.md).

Завершение задачи с правкой UI — Diff-log: `Синхронизация UI-документации [Имя_Формы]: ...`

---

## Шаблон документации сущности

Файл: **[templates/entity-template.md](templates/entity-template.md)**

Используйте при добавлении описания любой сущности. **Сначала** — блок «Бизнес-контекст» (3 подраздела), **затем** технические разделы. Шаблон покрывает:

0. **Бизнес-контекст** — назначение, навигация в UI, краткое поведение (обязательный ввод)
1. **Обзор** — метаданные, объём данных, критичность
2. **Архитектура** — FK, входящие/исходящие связи, сервисы
3. **Поля** — бизнес-поля, enum, LOB
4. **Views** — специализированные представления в `views.xml`
5. **Экраны** — Browse / Edit / Tree, JPQL, lazy loaders
6. **База данных** — таблица, индексы, миграции
7. **Производительность** — чеклист оптимизаций, backlog
8. **Развёртывание** — cache, FTS, app.properties

### Как добавить документ сущности

```bash
cp docs/templates/entity-template.md docs/entities/MyEntity.md
# заполнить разделы, добавить ссылку в таблицу ниже
```

---

## Описанные сущности

| Сущность | Таблица БД | Тип | Документ | Статус |
|----------|------------|-----|----------|--------|
| **Iteraction** | `ITPEARLS_ITERACTION` | справочник (дерево) | [entities/Iteraction.md](entities/Iteraction.md) | ✅ заполнен |
| **IteractionList** | `ITPEARLS_ITERACTION_LIST` | транзакционная | [entities/IteractionList.md](entities/IteractionList.md) | ✅ заполнен |
| **Person** | `ITPEARLS_PERSON` | справочник | [entities/Person.md](entities/Person.md) | ✅ заполнен |
| **Country** | `ITPEARLS_COUNTRY` | справочник | [entities/Country.md](entities/Country.md) | ✅ заполнен |
| **Region** | `ITPEARLS_REGION` | справочник | [entities/Region.md](entities/Region.md) | ✅ заполнен |
| **City** | `ITPEARLS_CITY` | справочник | [entities/City.md](entities/City.md) | ✅ заполнен |
| **Project** | `ITPEARLS_PROJECT` | мастер-данные | [entities/Project.md](entities/Project.md) | ✅ заполнен |
| **Company** | `ITPEARLS_COMPANY` | справочник | [entities/Company.md](entities/Company.md) | ✅ заполнен |
| **CompanyDepartament** | `ITPEARLS_COMPANY_DEPARTAMENT` | справочник | [entities/CompanyDepartament.md](entities/CompanyDepartament.md) | ✅ заполнен |
| **CompanyGroup** | `ITPEARLS_COMPANY_GROUP` | справочник | [entities/CompanyGroup.md](entities/CompanyGroup.md) | ✅ заполнен |
| **SkillTree** | `ITPEARLS_SKILL_TREE` | справочник (дерево) | [entities/SkillTree.md](entities/SkillTree.md) | ✅ заполнен |
| **JobCandidate** | `ITPEARLS_JOB_CANDIDATE` | транзакционная | [entities/JobCandidate.md](entities/JobCandidate.md) | ✅ заполнен |
| **OpenPosition** | `ITPEARLS_OPEN_POSITION` | транзакционная | [entities/OpenPosition.md](entities/OpenPosition.md) · [Spec](architecture/OpenPosition_Spec.md) | ✅ заполнен |
| **Grade** | `ITPEARLS_GRADE` | справочник | [entities/Grade.md](entities/Grade.md) | ✅ заполнен |
| **Position** | `ITPEARLS_POSITION` | справочник | [entities/Position.md](entities/Position.md) | ✅ заполнен |
| **OpenPositionNews** | `ITPEARLS_OPEN_POSITION_NEWS` | транзакционная | [entities/OpenPositionNews.md](entities/OpenPositionNews.md) | ✅ заполнен |
| **OpenPositionComment** | `ITPEARLS_OPEN_POSITION_COMMENT` | транзакционная | [entities/OpenPositionComment.md](entities/OpenPositionComment.md) | ✅ заполнен |
| **RecrutiesTasks** | `ITPEARLS_RECRUTIES_TASKS` | транзакционная | [entities/RecrutiesTasks.md](entities/RecrutiesTasks.md) | ✅ заполнен |

---

## Архитектура проекта (кратко)

| Модуль | Путь | Содержимое |
|--------|------|------------|
| **global** | `modules/global/` | Entity, enums, `views.xml`, messages |
| **core** | `modules/core/` | Сервисы, миграции БД, `app.properties` |
| **web** | `modules/web/` | Экраны, виджеты, тема hover |
| **gui** | `modules/gui/` | Desktop-клиент (если используется) |

Ключевые файлы:

| Область | Путь |
|---------|------|
| Views | `modules/global/src/com/company/itpearls/views.xml` |
| Entity | `modules/global/src/com/company/itpearls/entity/` |
| Экраны | `modules/web/src/com/company/itpearls/web/screens/` |
| Миграции PostgreSQL | `modules/core/db/update/postgres/` |
| Схема БД (init) | `modules/core/db/init/postgres/` |
| Кэш сущностей | `modules/core/src/com/company/itpearls/app.properties` |
| FTS | `modules/core/src/com/company/itpearls/fts.xml` |
| Стили UI | `modules/web/themes/hover/com.company.itpearls/hover-ext.scss` |

---

## Оптимизация производительности

**Триггерная фраза:** «давай оптимизировать работу сущности {Имя}»

Методология и чеклист: [.cursor/rules/entity-performance-optimization.mdc](../.cursor/rules/entity-performance-optimization.mdc)

**Образец реализации:** [entities/Iteraction.md](entities/Iteraction.md) — раздел «Производительность»

Типовой план:

1. Анализ entity, views, экранов, потребителей (`rg`)
2. Специализированные views: `{entity}-browse-view`, `-edit-view`, `-picker-view`
3. Исключение LOB из browse/edit views, lazy load на вкладках
4. `cacheable="true"` на справочных loaders, `readOnly="true"` на browse
5. Узкий `excludeProperties` в фильтрах
6. Устранение N+1 в `rowStyleProvider` / `columnGenerator`
7. Сборка: `./gradlew compileJava deploy -x test`

**Структура БД:** индексы, новые колонки, поля entity и миграции — **только с явного согласия**; без согласия оптимизируй через views, JPQL и Java ([`.cursorrules`](../.cursorrules), [data-view-integrity.mdc](../.cursor/rules/data-view-integrity.mdc)).

---

## Развёртывание

Подробности локальной среды: [LOCAL_DATABASE.md](LOCAL_DATABASE.md)

```bash
./gradlew setupTomcat deploy start
# http://localhost:8080/app
```

Версия CUBA: `7.3-SNAPSHOT` (`build.gradle`).

---

## Соглашения

- **Business & Context Intro:** любой `docs/entities/*.md`, `docs/architecture/*_Spec.md`, `docs/ui/*_Spec.md` **обязан** начинать содержательную часть с трёх подразделов ввода (назначение, навигация в UI, краткое поведение) **до** технических таблиц/XML/БД. Документ без ввода — недействителен. Шаблоны: [entity-template.md](templates/entity-template.md), [ui-template.md](templates/ui-template.md). Правило агента: [`.cursorrules`](../.cursorrules) § Business & Context Intro. Существующие UI Spec (~47) дополняются при следующем living sync или отдельным batch-проходом.
- **Брендинг:** во всей документации (`docs/**/*.md`, шаблоны) — **HRM HuntTech**; не использовать «IT Pearls» / «IT-Pearls» в prose. Исключение — цитирование legacy-идентификаторов из кода (`itpearls`, `ITPEARLS_*`, имена файлов ресурсов, ключи `messages.properties`). Правило для агента: [living-documentation.mdc](../.cursor/rules/living-documentation.mdc), § «Брендинг в документации».
- Документация сущностей — **на русском языке**
- Имена файлов сущностей — как Java-класс: `Iteraction.md`, `IteractionList.md`
- При изменении views/экранов сущности — обновлять `docs/entities/*.md` и `docs/ui/{FormName}_Spec.md`
- **Оптимизация без изменения БД:** сужение view, batch/JPQL, loaders — без запроса; миграции, индексы, новые поля entity — только после явного согласия ([`.cursorrules`](../.cursorrules) § «ОПТИМИЗАЦИЯ: ОГРАНИЧЕНИЕ СТРУКТУРЫ БД»)
- Не дублировать полные DDL в документах — ссылаться на `modules/core/db/`

### Политика Living Documentation

**Триггер сущности (приоритет):** «Сделай документацию сущности {Имя}» → STOP остального → scan кода → `docs/architecture/{EntityName}_Spec.md` (6 разделов, one-pass) → финал: `Документация для сущности {Имя} успешно создана/актуализирована в файле {Путь}`

**Изменение кода сущности** (Entity, views, сервисы, миграции) — синхронизировать в той же сессии:

- Living-doc сущности → `docs/entities/{EntityName}.md`
- Diff-log: `Синхронизация документации [EntityName]: ...`

**Изменение UI** (экран, фрагмент, меню, data bindings) — синхронизировать в той же сессии:

- UI Spec → `docs/ui/{FormName}_Spec.md` (Business & Context Intro + 6 разделов)
- При привязке к сущности — также `docs/entities/{EntityName}.md` §2
- Новый Spec → [ui/README.md](ui/README.md)
- Diff-log: `Синхронизация UI-документации [Имя_Формы]: ...`

**DELETE UI:** archive в `docs/ui/archive/`, обновить индексы и parent docs, отчёт по menu.xml.

Правила агента: [living-documentation.mdc](../.cursor/rules/living-documentation.mdc), [living-ui-documentation.mdc](../.cursor/rules/living-ui-documentation.mdc), [`.cursorrules`](../.cursorrules)

### История изменений (формат дат)

Добавлять запись в раздел **«История изменений»** соответствующего документа:

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Глобальный аудит Living Documentation: Business & Context Intro во всех entity/UI/architecture spec (см. `docs/ui/README.md`) |
| YYYY-MM-DD | Краткое описание изменения |

- Дата — формат **YYYY-MM-DD** (дата внесения изменения)
- Новые записи — **сверху** таблицы
- Правило для Cursor-агента: [.cursor/rules/documentation-with-dates.mdc](../.cursor/rules/documentation-with-dates.mdc)
