# {Название формы} (`{FormName}`)

> Шаблон UI-спецификации CUBA 7.3 для проекта **HRM HuntTech**.
> Скопируйте в `docs/ui/{FormName}_Spec.md` и заполните разделы.
> **При любом изменении UI** (экран, фрагмент, меню) — синхронизируйте в той же сессии ([living-ui-documentation.mdc](../../.cursor/rules/living-ui-documentation.mdc), [`.cursorrules`](../../.cursorrules)).
> Связанная сущность: [entities/{EntityName}.md](../entities/{EntityName}.md) · [architecture/{EntityName}_Spec.md](../architecture/{EntityName}_Spec.md) (если есть).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `{controllerId}` |
| **Java-класс** | `com.company.itpearls.web.{package}.{ClassName}` |
| **XML-дескриптор** | `{descriptor}.xml` |
| **Маршрут** | `@Route(path = "…")` / menu id |
| **Открытие** | menu / opener screen / lookup / dialog |
| **Права** | `{screenPermission}` |

### Назначение

Краткое описание роли формы (1–3 предложения).

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `{EntityName}` / нет (составной UI) |
| **View / fetch plan** | `{view-name}` |
| **Data containers** | `jobCandidatesDs`, `jobCandidateDc`, … |
| **Loaders** | JPQL / query key / parent |

### Привязки property

| Компонент | property | datasource |
|-----------|----------|------------|
| `{fieldId}` | `{entity.field}` | `{dc}` |

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

```mermaid
flowchart TD
    Parent[Родительский экран] --> Current[{FormName}]
    Current --> Child[Дочерний / lookup / fragment]
```

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `{ParentScreen}` | … |
| Дочерний | `{ChildScreen}` | `screenBuilders.editor()` / lookup |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие | Метод | Логика |
|---------|-------|--------|
| `InitEntity` | `onInitEntity` | … |
| `BeforeCommitChanges` | … | … |

### Валидация, lazy load, стили строк

- …

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `{actionId}` | `{buttonId}` | … | … |

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корень: `grid` / `scrollBox` / …
- Вкладки: `{tabId}` — содержимое
- Таблица browse: колонки `{property}`

### Стили и сообщения

| Элемент | stylename / CSS | message key |
|---------|-----------------|-------------|
| … | … | `{msg://…}` |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| YYYY-MM-DD | Создан шаблон / первая версия Spec |
