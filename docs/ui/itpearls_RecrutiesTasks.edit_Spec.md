# RecrutiesTasks Edit (`itpearls_RecrutiesTasks.edit`)

> Сущность: [RecrutiesTasks.md](../entities/RecrutiesTasks.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **RecrutiesTasks** HRM HuntTech: редактирование записи сущности `OpenPosition`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_RecrutiesTasks.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_RecrutiesTasks.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.recrutiestasks.RecrutiesTasksEdit ` |
| **XML-дескриптор** | `recruties-tasks-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.recrutiestasks` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `recrutiesTasksDc` |
| **focusComponent** | `` |
| **Меню** | `web-menu.xml` → `screen="itpearls_RecrutiesTasks.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **RecrutiesTasks** HRM HuntTech: редактирование записи сущности `OpenPosition`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `OpenPosition` |
| **View** | `openPosition-rtasks-picker-view` |
| **Data containers** | `openPositionsDc` (collection), `recrutiesTasksDc` (instance), `usersDc` (collection) |
| **Loader** | `openPositionsDl` |

### JPQL (если задан)

```
select e from itpearls_OpenPosition e
                        where e.openClose = false
                        order by e.vacansyName
```

### Привязки property (form / table)

- `openPosition`
- `planForPeriod`
- `startDate`
- `endDate`
- `reacrutier`
- `subscribe`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_RecrutiesTasks.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `screen` | `onBeforeShow` | см. Java |
| `screen` | `onBeforeCommitChanges` | см. Java |
| `windowCommitAndCloseButton` | `onWindowCommitAndCloseButtonClick` | см. Java |
| `screen` | `onAfterCommitChanges` | см. Java |


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

- Корневой layout: `expand` на основную таблицу / форму (``)
- Фильтр: `filter` → `openPositionsDl`
- Таблицы: —

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.recrutiestasks` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
