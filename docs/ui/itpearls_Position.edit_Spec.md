# Position Edit (`itpearls_Position.edit`)

> Сущность: [Position.md](../entities/Position.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Position** HRM HuntTech: редактирование записи сущности `Position`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Position.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

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

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `screen` | `onBeforeShow` | см. Java |
| `positionEnNameField` | `onPositionEnNameFieldValueChange` | см. Java |
| `positionRuNameField` | `onPositionRuNameFieldTextChange1` | см. Java |
| `positionEnNameField` | `onPositionEnNameFieldTextChange` | см. Java |
| `positionRuNameField` | `onPositionRuNameFieldTextChange` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| create/edit/remove | standard | — | CRUD |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

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
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
