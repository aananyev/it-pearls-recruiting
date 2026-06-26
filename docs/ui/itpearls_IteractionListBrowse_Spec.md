# IteractionList Fragment (`itpearls_IteractionListBrowse`)

> Сущность: [IteractionList.md](../entities/IteractionList.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Фрагмент таблицы взаимодействий (`IteractionList`) для встраивания в экраны подсистемы кандидатов. Отображает дату, кандидата, тип, вакансию, проект и рекрутёра. Java-контроллер без подписок — логика на стороне родителя.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_IteractionListBrowse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Расширенный browse взаимодействий: фильтр по дате (от), «только мои», стили строк, иконки типов. Используется как отдельный экран списка.


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

### 4.1 Жизненный цикл

Перед показом — инициализация колонок и фильтров; dateFrom и checkBoxShowOnlyMy меняют параметры loader; PostLoad — стили и иконки.

### 4.2 Скрытые вычисления

rowStyleProvider по типу и дате; rating stars; open/close иконки вакансии.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| dateFromField | Перезагрузка с даты |
| checkBoxShowOnlyMy | Фильтр по текущему рекрутеру |
| CRUD | Стандартный + copy actions из контроллера |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- Корень: `fragment` → `layout` → `dataGrid` `iteractionListsTable` width 100%
- 7 колонок property без caption override (кроме formatter на дате)
- Кнопочная панель без кнопок

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec |
