# Company Browse (`itpearls_OurCompany.browse`)

> Сущность: [Company.md](../entities/Company.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Company** HRM HuntTech: список / lookup сущности ``.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_OurCompany.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Список компаний HRM HuntTech (наши юрлица). Наследует CompanyBrowse, но при открытии принудительно фильтрует only ourLegalEntity=true и скрывает лишние чекбоксы.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_OurCompany.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.company.OurCompanyBrowse ` |
| **XML-дескриптор** | `our-company-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.company` |
| **Базовый класс** | `` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `` |
| **Меню** | `web-menu.xml` → `screen="itpearls_OurCompany.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Company** HRM HuntTech: список / lookup сущности ``.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `` |
| **View** | `` |
| **Data containers** | — |
| **Loader** | `` |

### JPQL (если задан)

```

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

### 4.1 Жизненный цикл

Перед показом: чекбокс «только юрлицо» скрыт; «только наш клиент» = false; loader с параметром setOurLegalEntity=true → загрузка только наших юрлиц.

### 4.2–4.3

Наследует генераторы и фильтры CompanyBrowse; стандартный commit в edit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| CRUD | Как CompanyBrowse, но список предфильтрован |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (``)
- Фильтр: `filter` → ``
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
