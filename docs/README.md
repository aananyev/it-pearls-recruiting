# Документация HRM HuntTech

Проект рекрутинговой системы **HRM HuntTech** на **CUBA Platform 7.3**.

Документация предназначена для разработчиков: описание сущностей, экранов, схемы БД и практик оптимизации производительности.

---

## Структура каталога

```
docs/
├── README.md                 ← этот файл (индекс)
├── LOCAL_DATABASE.md         ← локальная PostgreSQL, миграции, запуск
├── architecture/             ← полные спецификации по триггеру (см. ниже)
│   └── README.md
├── ui/
│   └── login-screen.md       ← экран входа HRM HuntTech
├── templates/
│   └── entity-template.md    ← шаблон описания сущности CUBA
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
    ├── Position.md             ← должности
    ├── JobCandidate.md         ← кандидаты (подсистема: entity + browse + edit)
    └── RecrutiesTasks.md       ← подписки рекрутёров
```

---

## Быстрый старт

| Задача | Документ |
|--------|----------|
| Поднять локальную БД и приложение | [LOCAL_DATABASE.md](LOCAL_DATABASE.md) |
| Экран входа (login) | [ui/login-screen.md](ui/login-screen.md) |
| Описать новую сущность | [templates/entity-template.md](templates/entity-template.md) |
| Оптимизировать существующую сущность | [.cursor/rules/entity-performance-optimization.mdc](../.cursor/rules/entity-performance-optimization.mdc) |
| Living Documentation (код ↔ docs, DoD) | [.cursor/rules/living-documentation.mdc](../.cursor/rules/living-documentation.mdc) · [`.cursorrules`](../.cursorrules) |
| Полная спецификация сущности (триггер) | «Сделай документацию сущности {Имя}» → [architecture/{Имя}_Spec.md](architecture/README.md) |
| Зафиксировать дату в «История изменений» | [.cursor/rules/documentation-with-dates.mdc](../.cursor/rules/documentation-with-dates.mdc) |
| Понять устройство типов взаимодействий | [entities/Iteraction.md](entities/Iteraction.md) |
| Понять записи взаимодействий с кандидатами | [entities/IteractionList.md](entities/IteractionList.md) |
| Понять справочник персон (сотрудники) | [entities/Person.md](entities/Person.md) |
| Понять справочник стран | [entities/Country.md](entities/Country.md) |
| Понять справочник городов | [entities/City.md](entities/City.md) |
| Понять справочник регионов | [entities/Region.md](entities/Region.md) |
| Понять справочник проектов | [entities/Project.md](entities/Project.md) |
| Понять справочник компаний | [entities/Company.md](entities/Company.md) |
| Понять департаменты компаний | [entities/CompanyDepartament.md](entities/CompanyDepartament.md) |
| Понять группы компаний | [entities/CompanyGroup.md](entities/CompanyGroup.md) |
| Понять дерево компетенций | [entities/SkillTree.md](entities/SkillTree.md) |
| Понять подсистему кандидатов | [entities/JobCandidate.md](entities/JobCandidate.md) |
| Понять подсистему вакансий | [entities/OpenPosition.md](entities/OpenPosition.md) · [architecture/OpenPosition_Spec.md](architecture/OpenPosition_Spec.md) |

---

## Архитектурные спецификации (`docs/architecture/`)

**Триггерная фраза:** «Сделай документацию сущности {Имя}» (например: JobCandidate, OpenPosition)

| Сценарий | Путь | Разделы |
|----------|------|---------|
| Триггер (полная регенерация из кода) | `architecture/{EntityName}_Spec.md` | 6: Data Model, Views, Browse, Edit, Fragments, Deployment |
| Living sync (правка кода в сессии) | `entities/{EntityName}.md` | 5 блоков living-doc |

Оба каталога **не объединяются**: architecture — канон для one-pass спецификации по запросу; entities — инкрементальная синхронизация при изменении кода. При наличии обоих файлов — cross-links в шапке.

Подробности: [architecture/README.md](architecture/README.md)

---

## Шаблон документации сущности

Файл: **[templates/entity-template.md](templates/entity-template.md)**

Используйте при добавлении описания любой сущности. Шаблон покрывает:

1. **Обзор** — назначение, объём данных, критичность
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

- **Брендинг:** во всей документации (`docs/**/*.md`, шаблоны) — **HRM HuntTech**; не использовать «IT Pearls» / «IT-Pearls» в prose. Исключение — цитирование legacy-идентификаторов из кода (`itpearls`, `ITPEARLS_*`, имена файлов ресурсов, ключи `messages.properties`). Правило для агента: [living-documentation.mdc](../.cursor/rules/living-documentation.mdc), § «Брендинг в документации».
- Документация сущностей — **на русском языке**
- Имена файлов сущностей — как Java-класс: `Iteraction.md`, `IteractionList.md`
- При изменении views/экранов сущности — обновлять соответствующий `docs/entities/*.md`
- Не дублировать полные DDL в документах — ссылаться на `modules/core/db/`

### Политика Living Documentation

**Триггер (приоритет):** «Сделай документацию сущности {Имя}» → STOP остального → scan кода → `docs/architecture/{EntityName}_Spec.md` (6 разделов, one-pass) → финал: `Документация для сущности {Имя} успешно создана/актуализирована в файле {Путь}`

**Изменение кода** (Entity, экраны, сервисы, фрагменты, views, миграции) — синхронизировать в той же сессии:

- Living-doc сущности → `docs/entities/{EntityName}.md`
- UI без одной entity → `docs/ui/{name}.md`
- Новый документ → обновить этот `README.md`
- Правила агента: [living-documentation.mdc](../.cursor/rules/living-documentation.mdc), [`.cursorrules`](../.cursorrules)

Блоки living-doc (`entities/`): (1) Архитектура Сущности, (2) Интерфейсный Слой, (3) Бизнес-логика, (4) Взаимодействие компонентов, (5) Инструкция по развертыванию.

Завершение задачи с правкой кода — Diff-log: `Синхронизация документации [EntityName]: ...`

### История изменений (формат дат)

Добавлять запись в раздел **«История изменений»** соответствующего документа:

| Дата | Изменение |
|------|-----------|
| YYYY-MM-DD | Краткое описание изменения |

- Дата — формат **YYYY-MM-DD** (дата внесения изменения)
- Новые записи — **сверху** таблицы
- Правило для Cursor-агента: [.cursor/rules/documentation-with-dates.mdc](../.cursor/rules/documentation-with-dates.mdc)
