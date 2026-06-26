# OpenPositionNews Edit (`itpearls_OpenPositionNews.edit`)

> Сущность: [OpenPositionNews.md](../entities/OpenPositionNews.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **OpenPositionNews** HRM HuntTech: редактирование записи сущности `OpenPositionNews`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_OpenPositionNews.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_OpenPositionNews.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.openpositionnews.OpenPositionNewsEdit ` |
| **XML-дескриптор** | `open-position-news-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.openpositionnews` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `openPositionNewsDc` |
| **focusComponent** | `newsVBox` |
| **Меню** | `web-menu.xml` → `screen="itpearls_OpenPositionNews.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **OpenPositionNews** HRM HuntTech: редактирование записи сущности `OpenPositionNews`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `OpenPositionNews` |
| **View** | `openPosition-picker-view` |
| **Data containers** | `openPositionNewsDc` (instance), `openPositionsDc` (collection), `authorDc` (collection) |
| **Loader** | `openPositionsDl` |

### JPQL (если задан)

```
select e from itpearls_OpenPosition e order by e.openClose, e.vacansyName
```

### Привязки property (form / table)

- `subject`
- `openPosition`
- `dateNews`
- `comment`
- `author`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_OpenPositionNews.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `screen` | `onAfterShow` | см. Java |


### @Install (generators / providers)

| Target | Subject | Назначение |
|--------|---------|------------|
| `openPositionField` | `optionIconProvider` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `lookup` | standard CUBA action | — | CRUD / lookup |
| `open` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`newsVBox`)
- Фильтр: `filter` → `openPositionsDl`
- Таблицы: —

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
