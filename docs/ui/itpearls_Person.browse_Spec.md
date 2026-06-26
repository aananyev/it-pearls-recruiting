# Person Browse (`itpearls_Person.browse`)

> Сущность: [Person.md](../entities/Person.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Person** HRM HuntTech: список / lookup сущности `Person`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Person.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник персон (контакты). В списке — миниатюра фото с подсказкой ФИО; в форме — переключение placeholder и загруженного фото.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Person.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.person.PersonBrowse ` |
| **XML-дескриптор** | `person-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.person` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `personsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `personsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Person.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Person** HRM HuntTech: список / lookup сущности `Person`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Person` |
| **View** | `person-browse-view` |
| **Data containers** | `personsDc` (collection) |
| **Loader** | `personsDl` |

### JPQL (если задан)

```
select e from itpearls_Person e order by e.secondName, e.firstName
```

### Привязки property (form / table)

- `secondName`
- `firstName`
- `middleName`
- `personPosition`
- `positionCountry`
- `cityOfResidence`

### Колонки таблицы (browse)

- `personPicColumn`
- `secondName`
- `firstName`
- `middleName`
- `personPosition`
- `positionCountry`
- `cityOfResidence`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Person.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse: только генератор колонки аватара. Edit: перед показом — default или загруженное фото; upload/clear обновляет preview.

### 4.2 Скрытые вычисления

Миниатюра 20px, fallback `no-programmer.jpeg`, HTML-tooltip с ФИО.

### 4.3 Валидация и сохранение

Стандартный commit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| CRUD | Стандартный CUBA |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`personsTable`)
- Фильтр: `filter` → `personsDl`
- Таблицы: `personsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.person` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
