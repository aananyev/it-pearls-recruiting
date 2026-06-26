# IteractionList Fragment (`itpearls_IteractionListBrowse`)

> Сущность: [IteractionList.md](../entities/IteractionList.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Фрагмент таблицы взаимодействий (`IteractionList`) для встраивания в экраны подсистемы кандидатов. Отображает дату, кандидата, тип, вакансию, проект и рекрутёра. Java-контроллер без подписок — логика на стороне родителя.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_IteractionListBrowse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_IteractionListBrowse` |
| **Java-класс** | `com.company.itpearls.web.screens.jobcandidate.IteractionListBrowse` |
| **XML-дескриптор** | `jobcandidate/iteraction-list-browse.xml` (fragment) |
| **Базовый класс** | `ScreenFragment` |
| **Открытие** | embed во внешний экран (данные через parent `dataContainer`) |
| **Меню** | нет отдельного пункта |

### Назначение

Фрагмент таблицы взаимодействий (`IteractionList`) для встраивания в экраны подсистемы кандидатов. Отображает дату, кандидата, тип, вакансию, проект и рекрутёра. Java-контроллер без подписок — логика на стороне родителя.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `IteractionList` (колонки по property) |
| **Data containers** | не объявлены во fragment — binding через parent |
| **Колонки** | `dateIteraction`, `candidate`, `iteractionType`, `vacancy`, `project`, `recrutier`, `recrutierName` |

### Форматирование

- `dateIteraction` — `DateFormatter` `dd.MM.yyyy`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | экраны `jobcandidate` | `<fragment>` / programmatic embed |
| Полный browse | `itpearls_IteractionList.browse` | menu |
| Редактирование | `itpearls_IteractionList.edit` | из родителя / browse |

---

## 4. Модель поведения и интерактивность (Behavior Model)

Стандартный `ScreenFragment` без `@Subscribe` и `@Install`. Интерактивность определяется родительским экраном и привязанным `dataContainer`.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | id | Эффект |
|---------|-----|--------|
| `buttonsPanel` | `buttonsPanel` | пустая панель (`alwaysVisible="true"`) |
| actions таблицы | — | не заданы |

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- Корень: `fragment` → `layout` → `dataGrid` `iteractionListsTable` width 100%
- 7 колонок property без caption override (кроме formatter на дате)
- Кнопочная панель без кнопок

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec |
