# City Browse (`itpearls_City.browse`)

> Сущность: [City.md](../entities/City.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **City** HRM HuntTech: список / lookup сущности `City`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_City.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_City.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.city.CityBrowse ` |
| **XML-дескриптор** | `city-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.city` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `citiesTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `citiesTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_City.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **City** HRM HuntTech: список / lookup сущности `City`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `City` |
| **View** | `city-browse-view` |
| **Data containers** | `citiesDc` (collection) |
| **Loader** | `citiesDl` |

### JPQL (если задан)

```
select e from itpearls_City e
order by e.cityRuName
```

### Привязки property (form / table)

- см. XML

### Колонки таблицы (browse)

- `cityRuName`
- `cityPhoneCode`
- `cityRegion`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_City.edit` | create / edit action |
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

- Корневой layout: `expand` на основную таблицу / форму (`citiesTable`)
- Фильтр: `filter` → `citiesDl`
- Таблицы: `citiesTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.city` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
