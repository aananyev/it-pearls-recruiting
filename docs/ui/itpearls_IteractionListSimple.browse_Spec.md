# IteractionList Browse (`itpearls_IteractionListSimple.browse`)

> Сущность: [IteractionList.md](../entities/IteractionList.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **IteractionList** HRM HuntTech: список / lookup сущности `IteractionList`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_IteractionListSimple.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_IteractionListSimple.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse ` |
| **XML-дескриптор** | `iteraction-list-simple-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.iteractionlist` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `iteractionListsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `iteractionListsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_IteractionListSimple.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **IteractionList** HRM HuntTech: список / lookup сущности `IteractionList`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `IteractionList` |
| **View** | `iteractionList-simple-browse-view` |
| **Data containers** | `iteractionListsDc` (collection) |
| **Loader** | `iteractionListsDl` |

### JPQL (если задан)

```
select e from itpearls_IteractionList e order by e.numberIteraction desc
```

### Привязки property (form / table)

- `numberIteraction`
- `dateIteraction`
- `iteractionType`
- `vacancy`
- `recrutier`

### Колонки таблицы (browse)

- `icon`
- `rating`
- `currentOpenCloseColumn`
- `commentColumn`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_IteractionList.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `screen` | `onBeforeShow` | см. Java |
| `screen` | `onInit` | см. Java |


### @Install (generators / providers)

| Target | Subject | Назначение |
|--------|---------|------------|
| `iteractionListsTable.rating` | `columnGenerator` | см. Java |
| `iteractionListsTable.iteractionType` | `descriptionProvider` | см. Java |
| `iteractionListsTable.vacancy` | `descriptionProvider` | см. Java |
| `iteractionListsTable.commentColumn` | `columnGenerator` | см. Java |
| `iteractionListsTable.commentColumn` | `styleProvider` | см. Java |
| `iteractionListsTable.commentColumn` | `descriptionProvider` | см. Java |
| `iteractionListsTable.currentOpenCloseColumn` | `columnGenerator` | см. Java |
| `iteractionListsTable.currentOpenCloseColumn` | `styleProvider` | см. Java |
| `iteractionListsTable.currentOpenCloseColumn` | `descriptionProvider` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| create/edit/remove | standard | — | CRUD |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`iteractionListsTable`)
- Фильтр: `filter` → `iteractionListsDl`
- Таблицы: `iteractionListsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.iteractionlist` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
