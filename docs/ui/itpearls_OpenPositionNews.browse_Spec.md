# OpenPositionNews Browse (`itpearls_OpenPositionNews.browse`)

> Сущность: [OpenPositionNews.md](../entities/OpenPositionNews.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **OpenPositionNews** HRM HuntTech: список / lookup сущности `OpenPositionNews`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_OpenPositionNews.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Новости по вакансиям. Клик по строке раскрывает полный текст; в форме для новой записи автоматически подставляются дата и автор.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_OpenPositionNews.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.openpositionnews.OpenPositionNewsBrowse ` |
| **XML-дескриптор** | `open-position-news-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.openpositionnews` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `openPositionNewsDataGrid` |
| **Меню** | `web-menu.xml` → `screen="itpearls_OpenPositionNews.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **OpenPositionNews** HRM HuntTech: список / lookup сущности `OpenPositionNews`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `OpenPositionNews` |
| **View** | `openPositionNews-browse-view` |
| **Data containers** | `openPositionNewsCollectionDc` (collection) |
| **Loader** | `openPositionNewsCollectionDl` |

### JPQL (если задан)

```
select e from itpearls_OpenPositionNews e
```

### Привязки property (form / table)

- `dateNews`
- `openPosition`
- `author`

### Колонки таблицы (browse)

- `dateNews`
- `openPosition`
- `comment`
- `author`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_OpenPositionNews.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse: клик по строке → details с полным комментарием; PostLoad кэширует LOB. Edit: после показа — dateNews и author = текущий пользователь.

### 4.2 Скрытые вычисления

Усечение комментария до 120 символов в колонке; scalar-кэш полного текста; иконка открыта/закрыта вакансия в picker.

### 4.3 Валидация и сохранение

Стандартный commit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Клик по строке | Раскрытие details с полным комментарием |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`openPositionNewsDataGrid`)
- Фильтр: `filter` → `openPositionNewsCollectionDl`
- Таблицы: `openPositionNewsDataGrid`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.openpositionnews` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
