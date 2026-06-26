# Iteraction Browse (`itpearls_IteractionRequirement.browse`)

> Сущность: [Iteraction.md](../entities/Iteraction.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Iteraction** HRM HuntTech: список / lookup сущности `Iteraction`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_IteractionRequirement.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Связь типов взаимодействий с требованиями цепочки (дерево). При выборе типа в дереве загружаются требования; если записей нет — автоматически создаются для всех типов с iteractionTree.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_IteractionRequirement.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.iteraction.IteractionRequirementBrowse ` |
| **XML-дескриптор** | `iteraction-requirement-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.iteraction` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `iteractionsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `iteractionsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_IteractionRequirement.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Iteraction** HRM HuntTech: список / lookup сущности `Iteraction`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Iteraction` |
| **View** | `iteraction-tree-browse-view` |
| **Data containers** | `iteractionsDc` (collection), `iteractionRequirementDc` (collection) |
| **Loader** | `iteractionsDl` |

### JPQL (если задан)

```
select e from itpearls_Iteraction e order by e.number
```

### Привязки property (form / table)

- `number`
- `iterationName`
- `iteractionRequirement`
- `requirement`
- `requirementAll`

### Колонки таблицы (browse)

- `number`
- `iterationName`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Iteraction.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Перед показом и при смене выбора в дереве → setRequirement: загрузка ItearctionRequirements для выбранного Iteraction; если пусто — commit новых записей requirement=false для всех типов с iteractionTree.

### 4.2–4.3

Стандартное редактирование грида требований.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Выбор строки в дереве | Перезагрузка таблицы требований |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`iteractionsTable`)
- Фильтр: `filter` → `iteractionsDl`
- Таблицы: `iteractionsTable`, `iteractionRequirementsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.iteraction` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
