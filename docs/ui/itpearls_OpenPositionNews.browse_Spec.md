# OpenPositionNews Browse (`itpearls_OpenPositionNews.browse`)

> Сущность: [OpenPositionNews.md](../entities/OpenPositionNews.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **OpenPositionNews** HRM HuntTech: список / lookup сущности `OpenPositionNews`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_OpenPositionNews.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_OpenPositionNews.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.openpositionnews.OpenPositionNewsBrowse ` |
| **XML-дескриптор** | `open-position-news-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.openpositionnews` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `openPositionNewsDataGrid` |
| **Меню** | `web-menu.xml` → `screen="itpearls_OpenPositionNews.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **OpenPositionNews** HRM HuntTech: список / lookup сущности `OpenPositionNews`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `OpenPositionNews` |
| **View** | `openPositionNews-browse-view` |
| **Data containers** | `openPositionNewsCollectionDc` (collection) |
| **Loader** | `openPositionNewsCollectionDl` |

### JPQL (если задан)

```
select e from itpearls_OpenPositionNews e
```

### Привязки property (form / table)

- `dateNews`
- `openPosition`
- `author`

### Колонки таблицы (browse)

- `dateNews`
- `openPosition`
- `comment`
- `author`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_OpenPositionNews.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `screen` | `onInit` | см. Java |


### @Install (generators / providers)

| Target | Subject | Назначение |
|--------|---------|------------|
| `openPositionNewsDataGrid` | `detailsGenerator` | см. Java |
| `openPositionNewsDataGrid.openPosition` | `descriptionProvider` | см. Java |
| `openPositionNewsDataGrid.comment` | `columnGenerator` | см. Java |
| `openPositionNewsDataGrid.comment` | `descriptionProvider` | см. Java |


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

- Корневой layout: `expand` на основную таблицу / форму (`openPositionNewsDataGrid`)
- Фильтр: `filter` → `openPositionNewsCollectionDl`
- Таблицы: `openPositionNewsDataGrid`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.openpositionnews` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
