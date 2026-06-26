# OpenPositionComment Browse (`itpearls_OpenPositionComment.browse`)

> Сущность: [OpenPositionComment.md](../entities/OpenPositionComment.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **OpenPositionComment** HRM HuntTech: список / lookup сущности `OpenPositionComment`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_OpenPositionComment.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Комментарии и рейтинги вакансий. Звёздный рейтинг в списке и форме; после сохранения — глобальное уведомление с названием вакансии.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_OpenPositionComment.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.openpositioncomment.OpenPositionCommentBrowse ` |
| **XML-дескриптор** | `open-position-comment-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.openpositioncomment` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `openPositionCommentsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `openPositionCommentsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_OpenPositionComment.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **OpenPositionComment** HRM HuntTech: список / lookup сущности `OpenPositionComment`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `OpenPositionComment` |
| **View** | `openPositionComment-browse-view` |
| **Data containers** | `openPositionCommentsDc` (collection) |
| **Loader** | `openPositionCommentsDl` |

### JPQL (если задан)

```
select e from itpearls_OpenPositionComment e
```

### Привязки property (form / table)

- `openPosition`
- `dateComment`
- `user`

### Колонки таблицы (browse)

- `openPosition`
- `rating`
- `dateComment`
- `user`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_OpenPositionComment.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse: генератор звёздного рейтинга. Edit: шкала рейтинга при init; для новой записи — текущая дата; после commit — UiNotificationEvent.

### 4.2 Скрытые вычисления

Звёзды через StarsAndOtherService; динамический цвет label рейтинга.

### 4.3 Валидация и сохранение

После сохранения: глобальное уведомление с названием вакансии.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| setOpenPositionField / setUserField | Программная установка FK при открытии из других экранов |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`openPositionCommentsTable`)
- Фильтр: `filter` → `openPositionCommentsDl`
- Таблицы: `openPositionCommentsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.openpositioncomment` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
