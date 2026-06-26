# Grade Browse (`itpearls_Grade.browse`)

> Сущность: [Grade.md](../entities/Grade.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Grade** HRM HuntTech: список / lookup сущности `Grade`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Grade.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Grade.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.grade.GradeBrowse ` |
| **XML-дескриптор** | `grade-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.grade` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `gradesTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `gradesTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Grade.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Grade** HRM HuntTech: список / lookup сущности `Grade`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Grade` |
| **View** | `grade-browse-view` |
| **Data containers** | `gradesDc` (collection) |
| **Loader** | `gradesDl` |

### JPQL (если задан)

```
select e from itpearls_Grade e
```

### Привязки property (form / table)

- см. XML

### Колонки таблицы (browse)

- `gradeName`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Grade.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| — | — | Стандартное поведение CUBA (`StandardLookup` / `StandardEditor`) |


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

- Корневой layout: `expand` на основную таблицу / форму (`gradesTable`)
- Фильтр: `filter` → `gradesDl`
- Таблицы: `gradesTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.grade` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
