# Person Browse (`itpearls_Person.browse`)

> Сущность: [Person.md](../entities/Person.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Person** HRM HuntTech: список / lookup сущности `Person`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Person.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Person.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.person.PersonBrowse ` |
| **XML-дескриптор** | `person-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.person` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `personsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `personsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Person.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Person** HRM HuntTech: список / lookup сущности `Person`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Person` |
| **View** | `person-browse-view` |
| **Data containers** | `personsDc` (collection) |
| **Loader** | `personsDl` |

### JPQL (если задан)

```
select e from itpearls_Person e order by e.secondName, e.firstName
```

### Привязки property (form / table)

- `secondName`
- `firstName`
- `middleName`
- `personPosition`
- `positionCountry`
- `cityOfResidence`

### Колонки таблицы (browse)

- `personPicColumn`
- `secondName`
- `firstName`
- `middleName`
- `personPosition`
- `positionCountry`
- `cityOfResidence`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Person.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| — | — | Стандартное поведение CUBA (`StandardLookup` / `StandardEditor`) |


### @Install (generators / providers)

| Target | Subject | Назначение |
|--------|---------|------------|
| `personsTable.personPicColumn` | `columnGenerator` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `create` | standard CUBA action | — | CRUD / lookup |
| `edit` | standard CUBA action | — | CRUD / lookup |
| `remove` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`personsTable`)
- Фильтр: `filter` → `personsDl`
- Таблицы: `personsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.person` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
