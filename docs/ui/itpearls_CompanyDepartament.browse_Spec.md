# CompanyDepartament Browse (`itpearls_CompanyDepartament.browse`)

> Сущность: [CompanyDepartament.md](../entities/CompanyDepartament.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **CompanyDepartament** HRM HuntTech: список / lookup сущности `CompanyDepartament`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_CompanyDepartament.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_CompanyDepartament.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.companydepartament.CompanyDepartamentBrowse ` |
| **XML-дескриптор** | `company-departament-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.companydepartament` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `companyDepartamentsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `companyDepartamentsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_CompanyDepartament.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **CompanyDepartament** HRM HuntTech: список / lookup сущности `CompanyDepartament`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `CompanyDepartament` |
| **View** | `companyDepartament-browse-view` |
| **Data containers** | `companyDepartamentsDc` (collection) |
| **Loader** | `companyDepartamentsDl` |

### JPQL (если задан)

```
select e from itpearls_CompanyDepartament e
order by e.departamentRuName
```

### Привязки property (form / table)

- см. XML

### Колонки таблицы (browse)

- `departamentRuName`
- `companyName`
- `departamentHrDirector`
- `departamentDirector`
- `departamentNumberOfProgrammers`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_CompanyDepartament.edit` | create / edit action |
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

- Корневой layout: `expand` на основную таблицу / форму (`companyDepartamentsTable`)
- Фильтр: `filter` → `companyDepartamentsDl`
- Таблицы: `companyDepartamentsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.companydepartament` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
