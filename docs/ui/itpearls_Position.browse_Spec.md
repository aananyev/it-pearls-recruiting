# Position Browse (`itpearls_Position.browse`)

> Сущность: [Position.md](../entities/Position.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Position** HRM HuntTech: список / lookup сущности `Position`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Position.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Position.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.position.PositionBrowse ` |
| **XML-дескриптор** | `position-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.position` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `positionsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `positionsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Position.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Position** HRM HuntTech: список / lookup сущности `Position`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Position` |
| **View** | `position-browse-view` |
| **Data containers** | `positionsDc` (collection) |
| **Loader** | `positionsDl` |

### JPQL (если задан)

```
select e from itpearls_Position e order by e.positionRuName
```

### Привязки property (form / table)

- `positionRuName`
- `positionEnName`

### Колонки таблицы (browse)

- `positionRuName`
- `positionEnName`
- `standartDescriptionIcon`
- `whoThisGuyIcon`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Position.edit` | create / edit action |
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
| `positionsTable.standartDescriptionIcon` | `columnGenerator` | см. Java |
| `positionsTable.standartDescriptionIcon` | `styleProvider` | см. Java |
| `positionsTable.standartDescriptionIcon` | `descriptionProvider` | см. Java |
| `positionsTable.whoThisGuyIcon` | `columnGenerator` | см. Java |
| `positionsTable.whoThisGuyIcon` | `styleProvider` | см. Java |
| `positionsTable.whoThisGuyIcon` | `descriptionProvider` | см. Java |


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

- Корневой layout: `expand` на основную таблицу / форму (`positionsTable`)
- Фильтр: `filter` → `positionsDl`
- Таблицы: `positionsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.position` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
