# CompanyGroup Edit (`itpearls_CompanyGroup.edit`)

> Сущность: [CompanyGroup.md](../entities/CompanyGroup.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **CompanyGroup** HRM HuntTech: редактирование записи сущности `CompanyGroup`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_CompanyGroup.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник групп компаний. Стандартный browse/edit без кастомной Java-логики.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_CompanyGroup.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.companygroup.CompanyGroupEdit ` |
| **XML-дескриптор** | `company-group-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.companygroup` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `companyGroupDc` |
| **focusComponent** | `form` |
| **Меню** | `web-menu.xml` → `screen="itpearls_CompanyGroup.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **CompanyGroup** HRM HuntTech: редактирование записи сущности `CompanyGroup`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `CompanyGroup` |
| **View** | `companyGroup-edit-view` |
| **Data containers** | `companyGroupDc` (instance) |
| **Loader** | `` |

### JPQL (если задан)

```

```

### Привязки property (form / table)

- `companyRuGroupName`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_CompanyGroup.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Стандартный CUBA.

### 4.2–4.3

Нет скрытых вычислений; стандартный commit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| CRUD | Стандартный CUBA |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`form`)
- Фильтр: `filter` → ``
- Таблицы: —

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.companygroup` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
