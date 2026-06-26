# Iteraction Edit (`itpearls_Iteraction_tree.edit`)

> Сущность: [Iteraction.md](../entities/Iteraction.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Iteraction** HRM HuntTech: редактирование записи сущности `Iteraction`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Iteraction_tree.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Редактирование узла дерева типов взаимодействий. Стандартный editor Iteraction с полем iteractionTree без кастомных @Subscribe в Java.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Iteraction_tree.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.iteraction.IteractionTreeEdit ` |
| **XML-дескриптор** | `iteraction-tree-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.iteraction` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `iteractionDc` |
| **focusComponent** | `form` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Iteraction_tree.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Iteraction** HRM HuntTech: редактирование записи сущности `Iteraction`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Iteraction` |
| **View** | `iteraction-view` |
| **Data containers** | `iteractionDc` (instance), `iteractionTreesDc` (collection) |
| **Loader** | `iteractionTreesLc` |

### JPQL (если задан)

```
select e from itpearls_Iteraction e
order by e.iterationName
```

### Привязки property (form / table)

- `number`
- `iteractionTree`
- `iterationName`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Iteraction.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1–4.3

Стандартный CUBA editor; иерархия задаётся полем iteractionTree в XML/entity.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Commit | Стандартный editor |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`form`)
- Фильтр: `filter` → `iteractionTreesLc`
- Таблицы: —

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
