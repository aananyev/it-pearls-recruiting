# Company Edit (`itpearls_Company.edit`)

> Сущность: [Company.md](../entities/Company.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Company** HRM HuntTech: редактирование записи сущности `Company`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Company.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Company.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.company.CompanyEdit ` |
| **XML-дескриптор** | `company-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.company` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `companyDc` |
| **focusComponent** | `companyOwnershipField` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Company.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Company** HRM HuntTech: редактирование записи сущности `Company`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Company` |
| **View** | `company-edit-view` |
| **Data containers** | `companyDc` (instance), `departmentOfCompanyDc` (collection), `companyOwnershipsDc` (collection), `companyDirectorsDc` (collection), `companyGroupDc` (collection), `cityOfCompaniesDc` (collection), `regionOfCompaniesDc` (collection), `countryOfCompaniesDc` (collection) |
| **Loader** | `companyOwnershipsLc` |

### JPQL (если задан)

```
select e from itpearls_Ownershup e
```

### Привязки property (form / table)

- `departmentOfCompany`
- `ourLegalEntity`
- `ourClient`
- `companyOwnership`
- `comanyName`
- `companyShortName`
- `companyGroup`
- `companyDirector`
- `cityOfCompany`
- `regionOfCompany`
- `countryOfCompany`
- `addressOfCompany`
- `fileCompanyLogo`
- `companyDescription`
- `workingConditions`
- `departamentRuName`
- `departamentDirector`
- `departamentHrDirector`

### Колонки таблицы (browse)

- `departamentRuName`
- `departamentDirector`
- `departamentHrDirector`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Company.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `mainTab` | `onMainTabSelectedTabChange` | см. Java |
| `screen` | `onAfterShow` | см. Java |
| `companyLogoFileUpload` | `onCompanyLogoFileUploadBeforeValueClear` | см. Java |
| `screen` | `onBeforeShow` | см. Java |
| `cityOfCompanyField` | `onCityOfCompanyFieldValueChange` | см. Java |
| `regionOfCompanyField` | `onRegionOfCompanyFieldValueChange` | см. Java |
| `companyLogoFileUpload` | `onCompanyLogoFileUploadFileUploadSucceed` | см. Java |
| `companyLogoFileImage` | `onCompanyLogoFileImageSourceChange` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `lookup` | standard CUBA action | — | CRUD / lookup |
| `create` | standard CUBA action | — | CRUD / lookup |
| `edit` | standard CUBA action | — | CRUD / lookup |
| `remove` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`companyOwnershipField`)
- Фильтр: `filter` → `companyOwnershipsLc`
- Таблицы: `departmentOfCompanyTable`

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
