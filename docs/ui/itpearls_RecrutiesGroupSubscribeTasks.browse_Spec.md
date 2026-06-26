# RecrutiesTasks Browse (`itpearls_RecrutiesGroupSubscribeTasks.browse`)

> Сущность: [RecrutiesTasks.md](../entities/RecrutiesTasks.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **RecrutiesTasks** HRM HuntTech: список / lookup сущности `RecrutiesTasks`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_RecrutiesGroupSubscribeTasks.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_RecrutiesGroupSubscribeTasks.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.recrutiestasks.RecrutiesTasksGroupSubscribeBrowse ` |
| **XML-дескриптор** | `recruties-tasks-group-subscribe-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.openposition` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `recrutiesTasksesTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `recrutiesTasksesTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_RecrutiesGroupSubscribeTasks.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **RecrutiesTasks** HRM HuntTech: список / lookup сущности `RecrutiesTasks`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `RecrutiesTasks` |
| **View** | `_local` |
| **Data containers** | `recrutiesTasksesDc` (instance) |
| **Loader** | `recrutiesTasksesDl` |

### JPQL (если задан)

```
select e from itpearls_RecruitiesTasks e
```

### Привязки property (form / table)

- см. XML

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_RecrutiesTasks.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `screen` | `onInit` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| create/edit/remove | standard | — | CRUD |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`recrutiesTasksesTable`)
- Фильтр: `filter` → `recrutiesTasksesDl`
- Таблицы: `recrutiesTasksesTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.openposition` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
