# IteractionList Browse (`itpearls_IteractionList.browse`)

> Сущность: [IteractionList.md](../entities/IteractionList.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **IteractionList** HRM HuntTech: список / lookup сущности `IteractionList`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_IteractionList.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Записи взаимодействий кандидата с вакансией. Сложный browse с фильтрами по роли и дате; edit с проверкой подписки, цепочки взаимодействий, статуса кандидата и email после сохранения.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_IteractionList.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.iteractionlist.IteractionListBrowse ` |
| **XML-дескриптор** | `iteraction-list-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.iteractionlist` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `iteractionListsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `iteractionListsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_IteractionList.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **IteractionList** HRM HuntTech: список / lookup сущности `IteractionList`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `IteractionList` |
| **View** | `iteractionList-browse-view` |
| **Data containers** | `iteractionListsDc` (collection) |
| **Loader** | `iteractionListsDl` |

### JPQL (если задан)

```
select e
                        from itpearls_IteractionList e
                        order by e.numberIteraction desc
```

### Привязки property (form / table)

- `numberIteraction`
- `iteractionType`
- `vacancy`
- `recrutier`
- `dateIteraction`

### Колонки таблицы (browse)

- `icon`
- `numberIteraction`
- `rating`
- `iteractionType`
- `vacancy`
- `currentOpenCloseColumn`
- `recrutier`
- `dateIteraction`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_IteractionList.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse: фильтры по роли, 90 дней, outstaffing; icon-колонка. Edit: подписки, popular types, vacancy picker; BeforeCommit — цепочка, Employee; AfterCommit — news + email.

### 4.2 Скрытые вычисления

Кэш recruiter tasks; rowStyleProvider; звёзды рейтинга; проверка «свой кандидат»; автокомментарий из addDate/addString/addInteger.

### 4.3 Валидация и сохранение

Перед сохранением: chainInteraction, currentPriority/OpenClose, Employee hire/fire. Перед закрытием: статус кандидата, sendMessages/email; диалоги закрытой вакансии и неподписки Researcher.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Копировать | Копия записи с новым номером |
| Карточка кандидата | Открытие JobCandidate.edit |
| Подписка / отписка | RecrutiesTasks |
| Фильтр «только мои» | JPQL по текущему рекрутеру |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`iteractionListsTable`)
- Фильтр: `filter` → `iteractionListsDl`
- Таблицы: `iteractionListsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.iteractionlist` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
