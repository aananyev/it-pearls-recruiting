# Country Browse (`itpearls_Country.browse`)

> Сущность: [Country.md](../entities/Country.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Country** HRM HuntTech: список / lookup сущности `Country`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Country.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник стран. Стандартный browse/edit без кастомной Java-логики.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Country.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.country.CountryBrowse ` |
| **XML-дескриптор** | `country-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.country` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `countriesTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `countriesTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Country.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Country** HRM HuntTech: список / lookup сущности `Country`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Country` |
| **View** | `country-browse-view` |
| **Data containers** | `countriesDc` (collection) |
| **Loader** | `countriesDl` |

### JPQL (если задан)

```
select e from itpearls_Country e
order by e.countryRuName
```

### Привязки property (form / table)

- см. XML

### Колонки таблицы (browse)

- `countryRuName`
- `countryShortName`
- `phoneCode`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Country.edit` | create / edit action |
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

- Корневой layout: `expand` на основную таблицу / форму (`countriesTable`)
- Фильтр: `filter` → `countriesDl`
- Таблицы: `countriesTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.country` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
