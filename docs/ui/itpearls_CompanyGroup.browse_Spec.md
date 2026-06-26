# CompanyGroup Browse (`itpearls_CompanyGroup.browse`)

> Сущность: [CompanyGroup.md](../entities/CompanyGroup.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **CompanyGroup** HRM HuntTech: список / lookup сущности `CompanyGroup`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_CompanyGroup.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_CompanyGroup.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.companygroup.CompanyGroupBrowse ` |
| **XML-дескриптор** | `company-group-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.companygroup` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `companyGroupsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `companyGroupsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_CompanyGroup.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **CompanyGroup** HRM HuntTech: список / lookup сущности `CompanyGroup`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `CompanyGroup` |
| **View** | `companyGroup-browse-view` |
| **Data containers** | `companyGroupsDc` (collection) |
| **Loader** | `companyGroupsDl` |

### JPQL (если задан)

```
select e from itpearls_CompanyGroup e
```

### Привязки property (form / table)

- см. XML

### Колонки таблицы (browse)

- `companyRuGroupName`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_CompanyGroup.edit` | create / edit action |
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

- Корневой layout: `expand` на основную таблицу / форму (`companyGroupsTable`)
- Фильтр: `filter` → `companyGroupsDl`
- Таблицы: `companyGroupsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.companygroup` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
