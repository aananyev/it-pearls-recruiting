# Company Browse (`itpearls_ClientsCompany.browse`)

> Сущность: [Company.md](../entities/Company.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Company** HRM HuntTech: список / lookup сущности `Company`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_ClientsCompany.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Список клиентских компаний. Наследует CompanyBrowse без дополнительной Java-логики — поведение как у Company с XML-настройками дескриптора clients-company-browse.xml.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_ClientsCompany.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.company.ClientsCompanyBrowse ` |
| **XML-дескриптор** | `clients-company-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.company` |
| **Базовый класс** | `` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `` |
| **Меню** | `web-menu.xml` → `screen="itpearls_ClientsCompany.browse"` (если есть пункт) |
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
                                      where e.ourClient = true
                                      order by e.comanyName
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
| Парный экран | `itpearls_Company.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1–4.3

Наследует CompanyBrowse целиком; кастомных @Subscribe в ClientsCompanyBrowse нет.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| CRUD | Как CompanyBrowse |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (``)
- Фильтр: `filter` → `companiesDl`
- Таблицы: —

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.company` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
