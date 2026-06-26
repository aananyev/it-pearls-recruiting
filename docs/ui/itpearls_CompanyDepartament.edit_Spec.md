# CompanyDepartament Edit (`itpearls_CompanyDepartament.edit`)

> Сущность: [CompanyDepartament.md](../entities/CompanyDepartament.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **CompanyDepartament** HRM HuntTech: редактирование записи сущности `CompanyDepartament`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_CompanyDepartament.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Департаменты компаний. В форме редактирования тяжёлые поля (описание, шаблон письма, проекты) загружаются при первом открытии соответствующей вкладки.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_CompanyDepartament.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.companydepartament.CompanyDepartamentEdit ` |
| **XML-дескриптор** | `company-departament-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.companydepartament` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `companyDepartamentDc` |
| **focusComponent** | `form` |
| **Меню** | `web-menu.xml` → `screen="itpearls_CompanyDepartament.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **CompanyDepartament** HRM HuntTech: редактирование записи сущности `CompanyDepartament`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `CompanyDepartament` |
| **View** | `companyDepartament-edit-view` |
| **Data containers** | `companyDepartamentDc` (instance), `companyDepartamentProjectOfDepartmentsDc` (collection), `companyNamesDc` (collection), `departamentHrDirectorsDc` (collection), `departamentDirectorsDc` (collection) |
| **Loader** | `companyNamesLc` |

### JPQL (если задан)

```
select e from itpearls_Company e
                where e.comanyName not like '%(не использовать)'
                order by e.comanyName
```

### Привязки property (form / table)

- `projectOfDepartment`
- `departamentRuName`
- `companyName`
- `departamentHrDirector`
- `departamentDirector`
- `departamentNumberOfProgrammers`
- `departamentDescription`
- `templateLetter`

### Колонки таблицы (browse)

- `projectName`
- `startProjectDate`
- `endProjectDate`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_CompanyDepartament.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse — стандартный. Edit: после показа для существующей записи подгружается описание департамента; смена вкладки → ленивая загрузка LOB и коллекции проектов.

### 4.2 Скрытые вычисления

Ленивый reload текстовых полей и `projectOfDepartment` только при первом заходе на вкладку.

### 4.3 Валидация и сохранение

Стандартный commit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| CRUD | Стандартный CUBA |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`form`)
- Фильтр: `filter` → `companyNamesLc`
- Таблицы: `companyDepartamentTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.companydepartament` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
