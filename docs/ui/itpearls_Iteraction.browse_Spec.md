# Iteraction Browse (`itpearls_Iteraction.browse`)

> Сущность: [Iteraction.md](../entities/Iteraction.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Iteraction** HRM HuntTech: список / lookup сущности `Iteraction`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Iteraction.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник типов взаимодействий. В форме — флаги уведомлений, lazy-load цепочки и email-шаблона; перед сохранением null-булевы флаги приводятся к false.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Iteraction.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.iteraction.IteractionBrowse ` |
| **XML-дескриптор** | `iteraction-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.iteraction` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `iteractionsTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `iteractionsTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Iteraction.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Iteraction** HRM HuntTech: список / lookup сущности `Iteraction`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Iteraction` |
| **View** | `iteraction-browse-view` |
| **Data containers** | `iteractionsDc` (collection) |
| **Loader** | `iteractionsDl` |

### JPQL (если задан)

```
select e from itpearls_Iteraction e order by e.number, e.iterationName
```

### Привязки property (form / table)

- см. XML

### Колонки таблицы (browse)

- `iteractionTree`
- `number`
- `mandatoryIteraction`
- `iterationName`
- `callButtonText`
- `callClass`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Iteraction.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse стандартный. Edit: radio/maps уведомлений; вкладки lazy-load элементов цепочки, email-шаблона, workStatus.

### 4.2 Скрытые вычисления

Превью иконки; заполнение ключей email из EmailGenerationService; lazy load textEmailToSend.

### 4.3 Валидация и сохранение

Перед сохранением: sign-флаги (endCase, ourInterview, clientInterview, sentToClient, statistics, priorityNews, viewOnlyManagers) null → false.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Чекбоксы calendar/callDialog/flag/mandatory | Вкл./выкл. связанных полей |
| notificationNeedSend | Блок настроек уведомлений |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`iteractionsTable`)
- Фильтр: `filter` → `iteractionsDl`
- Таблицы: `iteractionsTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.iteraction` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
