# Project Browse (`itpearls_Project.browse`)

> Сущность: [Project.md](../entities/Project.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Project** HRM HuntTech: список / lookup сущности `Project`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Project.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Проекты заказчиков. В списке — фильтры открытых проектов и проектов с вакансиями, иерархическая таблица, бейдж «новый» для свежих записей. В форме закрытие проекта блокирует поля и предлагает закрыть все открытые вакансии.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Project.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.project.ProjectBrowse ` |
| **XML-дескриптор** | `project-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.project` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `projectsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `projectsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Project.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Project** HRM HuntTech: список / lookup сущности `Project`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Project` |
| **View** | `project-browse-view` |
| **Data containers** | `projectsDc` (collection) |
| **Loader** | `projectsDl` |

### JPQL (если задан)

```
select e from itpearls_Project e
                            order by e.projectIsClosed desc, e.projectName
```

### Привязки property (form / table)

- `projectIsClosed`
- `projectName`
- `startProjectDate`
- `endProjectDate`
- `projectDepartment`

### Колонки таблицы (browse)

- `projectLogoColumn`
- `projectIsClosed`
- `projectName`
- `startProjectDate`
- `endProjectDate`
- `projectDepartment`
- `iconProjectDesc`
- `openPositionsCountColumn`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Project.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse: при инициализации — иерархическая колонка; перед показом — фильтры onlyOpenProject / withOpenPosition. Edit: дата старта, загрузка открытых вакансий, ссылки чатов; вкладки lazy-load LOB и вакансий.

### 4.2 Скрытые вычисления

Кэш счётчиков открытых вакансий и описаний; генераторы логотипа, имени (бейдж <14 дней, фото владельца), иконки описания; preview логотипа при upload.

### 4.3 Валидация и сохранение

Перед сохранением: нормализация флага «проект закрыт»; при открытии/закрытии — глобальное уведомление в системе.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Цепочка |
|---------|---------|
| Фильтры onlyOpen / withOpenPosition | Вкл. → перезагрузка списка |
| «Проект закрыт» | Вкл. → блокировка ключевых полей, установка endDate → диалог закрытия всех открытых вакансий |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`projectsTable`)
- Фильтр: `filter` → `projectsDl`
- Таблицы: `projectsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.project` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
