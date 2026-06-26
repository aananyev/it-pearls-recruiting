# City Edit (`itpearls_City.edit`)

> Сущность: [City.md](../entities/City.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **City** HRM HuntTech: редактирование записи сущности `City`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_City.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник городов: стандартный список и форма без кастомной логики в Java. При открытии загружается полный список; создание и редактирование — через стандартные действия CUBA.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_City.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.city.CityEdit ` |
| **XML-дескриптор** | `city-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.city` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `cityDc` |
| **focusComponent** | `form` |
| **Меню** | `web-menu.xml` → `screen="itpearls_City.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **City** HRM HuntTech: редактирование записи сущности `City`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `City` |
| **View** | `city-edit-view` |
| **Data containers** | `cityDc` (instance), `cityRegionsDc` (collection) |
| **Loader** | `cityRegionsLc` |

### JPQL (если задан)

```
select e from itpearls_Region e
```

### Привязки property (form / table)

- `cityRuName`
- `cityPhoneCode`
- `cityRegion`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_City.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл формы (Lifecycle)

| Этап | Что происходит |
|------|----------------|
| Открытие browse | Загрузка всех городов; generic-фильтр CUBA |
| Открытие edit | Стандартный editor, commit через framework |

### 4.2 Скрытые вычисления

Нет — колонки привязаны к полям сущности.

### 4.3 Валидация и сохранение

Стандартная валидация полей entity и commit editor'а.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Цепочка |
|---------|----------|
| Создать / Изменить / Удалить | Стандартный CUBA CRUD |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`form`)
- Фильтр: `filter` → `cityRegionsLc`
- Таблицы: —

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.city` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
