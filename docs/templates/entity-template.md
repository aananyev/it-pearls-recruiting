# {Название сущности} (`{EntityName}`)

> Шаблон документации сущности CUBA 7.3 для проекта **IT Pearls Recruiting** (HuntTech).
> Скопируйте файл в `docs/entities/{EntityName}.md` и заполните разделы.
> Триггер оптимизации: «давай оптимизировать работу сущности {EntityName}» — см. [`.cursor/rules/entity-performance-optimization.mdc`](../../.cursor/rules/entity-performance-optimization.mdc).
> **При любом изменении кода сущности** — обновляйте документ и добавляйте запись в §9 «История изменений» (см. [`.cursor/rules/documentation-with-dates.mdc`](../../.cursor/rules/documentation-with-dates.mdc)).

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.itpearls.entity.{EntityName}` |
| **Имя в CUBA** | `itpearls_{EntityName}` |
| **Таблица БД** | `{TABLE_NAME}` |
| **Тип данных** | справочник / транзакционная / связующая |
| **Ожидаемый объём** | ~N записей |
| **Критичность** | низкая / средняя / высокая |
| **Ответственный модуль** | `global` / `core` / `web` |

### Назначение

Краткое описание бизнес-роли сущности (1–3 предложения).

### Отображаемое имя

- **NamePattern:** `{pattern}`
- **Lookup:** `{поле для lookup}`

---

## 2. Архитектура и связи

### 2.1 Диаграмма связей (опционально)

```mermaid
erDiagram
    {EntityName} ||--o{ {ChildEntity} : "связь"
    {EntityName} }o--|| {ParentEntity} : "FK"
```

### 2.2 Исходящие связи (FK из этой таблицы)

| Поле Java | Колонка БД | Связанная сущность | Fetch | Обязательность |
|-----------|------------|-------------------|-------|----------------|
| `{field}` | `{COLUMN}` | `{Entity}` | LAZY/EAGER | да/нет |

### 2.3 Входящие связи (кто ссылается на эту сущность)

| Сущность | Поле | Колонка БД | Назначение |
|----------|------|------------|------------|
| `{Entity}` | `{field}` | `{COLUMN}` | {описание} |

### 2.4 Сервисы и бизнес-логика

| Сервис | Метод | Описание |
|--------|-------|----------|
| `{ServiceBean}` | `{method}` | {что делает} |

---

## 3. Поля сущности

### 3.1 Системные (StandardEntity)

| Поле | Колонка | Тип | Примечание |
|------|---------|-----|------------|
| id | ID | UUID | PK |
| version | VERSION | integer | оптимистичная блокировка |
| createTs / createdBy | CREATE_TS / CREATED_BY | | аудит |
| updateTs / updatedBy | UPDATE_TS / UPDATED_BY | | аудит |
| deleteTs / deletedBy | DELETE_TS / DELETED_BY | | soft delete |

### 3.2 Бизнес-поля

| Поле Java | Колонка БД | Тип | Ограничения | Описание |
|-----------|------------|-----|-------------|----------|
| `{field}` | `{COLUMN}` | {type} | unique / not null / length | {бизнес-смысл} |

### 3.3 Перечисления и флаги

| Поле | Тип | Значения | Назначение |
|------|-----|----------|------------|
| `{enumField}` | enum / Integer | {значения} | {описание} |

### 3.4 LOB и тяжёлые поля

| Поле | Колонка | Тип | Где используется | Стратегия загрузки |
|------|---------|-----|------------------|-------------------|
| `{lobField}` | `{COLUMN}` | text / bytea | Edit, вкладка X | lazy reload на вкладке |

---

## 4. Представления (views.xml)

Файл: `modules/global/src/com/company/itpearls/views.xml`

| View | Extends | Назначение | Где используется |
|------|---------|------------|------------------|
| `{entity}-browse-view` | `_minimal` | Browse-таблица, без LOB | `{entity}-browse.xml` |
| `{entity}-tree-browse-view` | `_minimal` | Tree browse (если есть) | `{entity}-tree-browse.xml` |
| `{entity}-edit-view` | `_minimal` | Edit-форма, без LOB | `{entity}-edit.xml` |
| `{entity}-picker-view` | `_minimal` | Lookup / picker | FK-поля, lookup-экраны |
| `{entity}-view` | `_base` / `_local` | Legacy / runtime | {перечислить потребителей} |

### Содержимое view (детализация)

#### `{entity}-browse-view`

```
{список property}
```

#### `{entity}-edit-view`

```
{список property, явно указать исключённые LOB}
```

---

## 5. Экраны (screens)

Каталог: `modules/web/src/com/company/itpearls/web/screens/{entity}/`

| Экран | Controller ID | Дескриптор | View | Тип | Меню |
|-------|---------------|------------|------|-----|------|
| Browse | `itpearls_{Entity}.browse` | `{entity}-browse.xml` | `{entity}-browse-view` | StandardLookup | {пункт меню} |
| Edit | `itpearls_{Entity}.edit` | `{entity}-edit.xml` | `{entity}-edit-view` | StandardEditor | — |
| Tree Browse | `itpearls_{Entity}._tree.browse` | `{entity}-tree-browse.xml` | `{entity}-tree-browse-view` | StandardLookup | {пункт меню} |

### 5.1 Browse

- **JPQL:** `{query}`
- **readOnly:** да/нет
- **cacheable loader:** да/нет
- **Колонки таблицы:** {список}
- **Фильтр excludeProperties:** {список}

### 5.2 Edit

- **Вкладки (TabSheet):** {список вкладок}
- **Lazy loaders:** {какие collection/LOB грузятся при открытии вкладки}
- **Особенности UI:** {валидация, зависимости полей}

### 5.3 Cross-form (FK в других экранах)

| Экран | Поле FK | View для FK | Необходимые поля |
|-------|---------|-------------|------------------|
| `{OtherScreen}` | `{fkField}` | `{view}` | {поля} |

---

## 6. База данных

### 6.1 Таблица `{TABLE_NAME}`

DDL: `modules/core/db/init/postgres/10.create-db.sql`  
Ограничения и индексы: `modules/core/db/init/postgres/20.create-db.sql`  
Миграции: `modules/core/db/update/postgres/`

### 6.2 Индексы

| Индекс | Колонки | Тип | Назначение |
|--------|---------|-----|------------|
| `{INDEX_NAME}` | `{columns}` | btree / unique partial | {зачем} |

### 6.3 Рекомендации по индексам

- Индексировать: FK в дочерних транзакционных таблицах, поля ORDER BY, уникальные бизнес-ключи.
- **Не** индексировать: boolean-флаги с низкой селективностью, редко используемые поля.

### 6.4 TOAST / LOB

| Колонка | Влияние на SELECT * | Рекомендация |
|---------|---------------------|--------------|
| `{COLUMN}` | уходит в TOAST | исключить из browse/tree views |

---

## 7. Производительность

> Заполняется при оптимизации или сразу, если известны проблемы.
> Методология: `.cursor/rules/entity-performance-optimization.mdc`

### 7.1 Текущее состояние

| Область | Статус | Комментарий |
|---------|--------|-------------|
| Специализированные views | ✅ / ⚠️ / ❌ | |
| LOB lazy load | ✅ / ⚠️ / ❌ | |
| cacheable loaders | ✅ / ⚠️ / ❌ | |
| readOnly browse | ✅ / ⚠️ / ❌ | |
| N+1 в providers | ✅ / ⚠️ / ❌ | |
| Entity cache (EclipseLink) | ✅ / ⚠️ / ❌ | `app.properties` |

### 7.2 Выполненные оптимизации

- [ ] `{entity}-browse-view` — только колонки таблицы
- [ ] `{entity}-edit-view` — без LOB
- [ ] `{entity}-picker-view` — для FK
- [ ] Lazy load LOB на вкладке `{tabName}`
- [ ] Lazy load collection на вкладке `{tabName}`
- [ ] `cacheable="true"` на справочных loaders
- [ ] `readOnly="true"` на browse data
- [ ] Узкий `excludeProperties` в фильтре

### 7.3 Известные проблемы и backlog

| Проблема | Приоритет | Предлагаемое решение |
|----------|-----------|---------------------|
| {описание} | высокий/средний/низкий | {решение} |

### 7.4 Потребители (rg-чеклист)

```bash
rg "itpearls_{Entity}|{EntityName}\." modules/ --glob '*.{java,xml}'
rg "view=\".*{entity}" modules/ --glob '*.xml'
```

Зафиксированные потребители:

- {экран / сервис / виджет}

---

## 8. Развёртывание и конфигурация

| Параметр | Файл | Значение |
|----------|------|----------|
| DBMS | `app.properties` | postgres |
| Entity cache | `app.properties` | `eclipselink.cache.shared.itpearls_{Entity}=...` |
| FTS | `fts.xml` | включён / выключен |

Локальная БД: см. [LOCAL_DATABASE.md](../LOCAL_DATABASE.md).

---

## 9. История изменений

> **Обязательный раздел.** При каждом изменении entity, views, экранов, сервисов, БД или бизнес-логики добавляйте новую строку **в начало таблицы** с датой **YYYY-MM-DD** и кратким описанием.
> Правило для агента: [`.cursor/rules/documentation-with-dates.mdc`](../../.cursor/rules/documentation-with-dates.mdc).

| Дата | Изменение |
|------|-----------|
| YYYY-MM-DD | Создание документа |
| 2026-06-22 | Оптимизация browse-view, добавлен {entity}-browse-view |

---

## 10. Связанные документы

- [Индекс документации](../README.md)
- [Шаблон сущности](../templates/entity-template.md)
- [LOCAL_DATABASE.md](../LOCAL_DATABASE.md)
- [Оптимизация сущностей](../../.cursor/rules/entity-performance-optimization.mdc)
- [Документирование изменений с датой](../../.cursor/rules/documentation-with-dates.mdc)
