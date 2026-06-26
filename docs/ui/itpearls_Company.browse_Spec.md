# Company Browse (`itpearls_Company.browse`)

> Сущность: [Company.md](../entities/Company.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Company** HRM HuntTech: список / lookup сущности `Company`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Company.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Company.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.company.CompanyBrowse ` |
| **XML-дескриптор** | `company-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.company` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `companiesTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `companiesTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Company.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Company** HRM HuntTech: список / lookup сущности `Company`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Company` |
| **View** | `company-browse-view` |
| **Data containers** | `companiesDc` (collection) |
| **Loader** | `companiesDl` |

### JPQL (если задан)

```
select e from itpearls_Company e
order by e.comanyName
```

### Привязки property (form / table)

- `companyGroup`
- `comanyName`
- `companyShortName`
- `countryOfCompany`
- `regionOfCompany`
- `cityOfCompany`

### Колонки таблицы (browse)

- `companyGroup`
- `companyLogoColumn`
- `ourCompanyIconColumn`
- `ourClientIconColumn`
- `comanyName`
- `companyShortName`
- `countryOfCompany`
- `regionOfCompany`
- `cityOfCompany`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Company.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `screen` | `onBeforeShow` | см. Java |
| `checkBoxOnlyOurClient` | `onCheckBoxOnlyOurClientValueChange` | см. Java |
| `checkBoxOnlyLegalEntity` | `onCheckBoxOnlyLegalEntityValueChange` | см. Java |


### @Install (generators / providers)

| Target | Subject | Назначение |
|--------|---------|------------|
| `companiesTable.companyLogoColumn` | `columnGenerator` | см. Java |
| `companiesTable.ourCompanyIconColumn` | `columnGenerator` | см. Java |
| `companiesTable.ourClientIconColumn` | `columnGenerator` | см. Java |
| `companiesTable.ourCompanyIconColumn` | `styleProvider` | см. Java |
| `companiesTable.ourClientIconColumn` | `styleProvider` | см. Java |


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

- Корневой layout: `expand` на основную таблицу / форму (`companiesTable`)
- Фильтр: `filter` → `companiesDl`
- Таблицы: `companiesTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.company` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
