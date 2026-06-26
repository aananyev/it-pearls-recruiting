# OpenPositionComment Edit (`itpearls_OpenPositionComment.edit`)

> Сущность: [OpenPositionComment.md](../entities/OpenPositionComment.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **OpenPositionComment** HRM HuntTech: редактирование записи сущности `OpenPositionComment`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_OpenPositionComment.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Комментарии и рейтинги вакансий. Звёздный рейтинг в списке и форме; после сохранения — глобальное уведомление с названием вакансии.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_OpenPositionComment.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.openpositioncomment.OpenPositionCommentEdit ` |
| **XML-дескриптор** | `open-position-comment-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.openpositioncomment` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `openPositionCommentDc` |
| **focusComponent** | `form` |
| **Меню** | `web-menu.xml` → `screen="itpearls_OpenPositionComment.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **OpenPositionComment** HRM HuntTech: редактирование записи сущности `OpenPositionComment`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `OpenPositionComment` |
| **View** | `openPosition-picker-view` |
| **Data containers** | `openPositionCommentDc` (instance), `openPositionsDc` (collection) |
| **Loader** | `openPositionsDl` |

### JPQL (если задан)

```
select e from itpearls_OpenPosition e
```

### Привязки property (form / table)

- `openPosition`
- `rating`
- `comment`
- `user`
- `dateComment`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_OpenPositionComment.browse` | create / edit action |
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

- Корневой layout: `expand` на основную таблицу / форму (`form`)
- Фильтр: `filter` → `openPositionsDl`
- Таблицы: —

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
