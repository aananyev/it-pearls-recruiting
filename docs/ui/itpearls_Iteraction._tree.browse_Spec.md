# Iteraction Browse (`itpearls_Iteraction._tree.browse`)

> Сущность: [Iteraction.md](../entities/Iteraction.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Iteraction** HRM HuntTech: список / lookup сущности `Iteraction`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Iteraction._tree.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Iteraction._tree.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.iteraction.IteractionTreeBrowse ` |
| **XML-дескриптор** | `iteraction-tree-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.iteraction` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `iteractionsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `iteractionTreeTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Iteraction._tree.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Iteraction** HRM HuntTech: список / lookup сущности `Iteraction`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Iteraction` |
| **View** | `iteraction-tree-browse-view` |
| **Data containers** | `iteractionsTreeDc` (collection) |
| **Loader** | `iteractionsDl` |

### JPQL (если задан)

```
select e from itpearls_Iteraction e order by e.number
```

### Привязки property (form / table)

- `number`
- `mandatoryIteraction`
- `iterationName`

### Колонки таблицы (browse)

- `number`
- `mandatoryIteraction`
- `iterationName`
- `notification`
- `needSendEmail`
- `needSendMemo`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Iteraction.edit` | create / edit action |
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
| `iteractionTreeTable.needSendEmail` | `columnGenerator` | см. Java |
| `iteractionTreeTable.needSendMemo` | `columnGenerator` | см. Java |
| `iteractionTreeTable.notification` | `columnGenerator` | см. Java |
| `iteractionTreeTable.notification` | `styleProvider` | см. Java |
| `iteractionTreeTable.needSendEmail` | `styleProvider` | см. Java |
| `iteractionTreeTable.needSendMemo` | `styleProvider` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `add` | standard CUBA action | — | CRUD / lookup |
| `edit` | standard CUBA action | — | CRUD / lookup |
| `remove` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`iteractionTreeTable`)
- Фильтр: `filter` → `iteractionsDl`
- Таблицы: `iteractionTreeTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.iteraction` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
