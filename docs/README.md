# Документация IT Pearls Recruiting

Проект рекрутинговой системы на **CUBA Platform 7.3** (HuntTech / IT Pearls).

Документация предназначена для разработчиков: описание сущностей, экранов, схемы БД и практик оптимизации производительности.

---

## Структура каталога

```
docs/
├── README.md                 ← этот файл (индекс)
├── LOCAL_DATABASE.md         ← локальная PostgreSQL, миграции, запуск
├── templates/
│   └── entity-template.md    ← шаблон описания сущности CUBA
└── entities/
    ├── IteractionList.md      ← взаимодействие с кандидатом
    ├── Iteraction.md         ← тип взаимодействия с кандидатом (образец)
    ├── Person.md             ← сотрудник / контактное лицо
    ├── Country.md              ← справочник стран
    ├── Region.md               ← справочник регионов
    └── City.md                 ← справочник городов
```

---

## Быстрый старт

| Задача | Документ |
|--------|----------|
| Поднять локальную БД и приложение | [LOCAL_DATABASE.md](LOCAL_DATABASE.md) |
| Описать новую сущность | [templates/entity-template.md](templates/entity-template.md) |
| Оптимизировать существующую сущность | [.cursor/rules/entity-performance-optimization.mdc](../.cursor/rules/entity-performance-optimization.mdc) |
| Понять устройство типов взаимодействий | [entities/Iteraction.md](entities/Iteraction.md) |
| Понять записи взаимодействий с кандидатами | [entities/IteractionList.md](entities/IteractionList.md) |
| Понять справочник персон (сотрудники) | [entities/Person.md](entities/Person.md) |
| Понять справочник стран | [entities/Country.md](entities/Country.md) |
| Понять справочник городов | [entities/City.md](entities/City.md) |
| Понять справочник регионов | [entities/Region.md](entities/Region.md) |

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
| JobCandidate | `ITPEARLS_JOB_CANDIDATE` | транзакционная | — | планируется |
| OpenPosition | `ITPEARLS_OPEN_POSITION` | транзакционная | — | планируется |

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

- Документация сущностей — **на русском языке**
- Имена файлов сущностей — как Java-класс: `Iteraction.md`, `IteractionList.md`
- При изменении views/экранов сущности — обновлять соответствующий `docs/entities/*.md`
- Не дублировать полные DDL в документах — ссылаться на `modules/core/db/`
