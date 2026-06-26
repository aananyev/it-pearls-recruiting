# Position Edit (`itpearls_Position.edit`)

> Сущность: [Position.md](../entities/Position.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Position** HRM HuntTech: редактирование записи сущности `Position`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Position.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник должностей. В списке — иконки наличия стандартного описания и подсказки; в форме — сводная подпись «EN — RU» при изменении имён.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Position.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.position.PositionEdit ` |
| **XML-дескриптор** | `position-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.position` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `positionDc` |
| **focusComponent** | `positionRuNameField` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Position.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Position** HRM HuntTech: редактирование записи сущности `Position`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Position` |
| **View** | `position-edit-view` |
| **Data containers** | `positionDc` (instance) |
| **Loader** | `` |

### JPQL (если задан)

```

```

### Привязки property (form / table)

- `positionRuName`
- `positionEnName`
- `standartDescription`
- `whoIsThisGuy`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Position.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse: после загрузки кэшируются LOB-поля для колонок. Edit: перед показом для существующей записи подгружаются `standartDescription` и `whoIsThisGuy`, обновляется сводный label.

### 4.2 Скрытые вычисления

| Колонка | Правило |
|---------|---------|
| Иконка описания | FILE / FILE_TEXT, цвет и tooltip (plain text из HTML) |

### 4.3 Валидация и сохранение

Стандартный commit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| CRUD | Стандартный CUBA |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`positionRuNameField`)
- Фильтр: `filter` → ``
- Таблицы: —

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.position` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
