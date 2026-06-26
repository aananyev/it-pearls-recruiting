# Iteraction Edit (`itpearls_Iteraction.edit`)

> Сущность: [Iteraction.md](../entities/Iteraction.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Iteraction** HRM HuntTech: редактирование записи сущности `Iteraction`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Iteraction.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник типов взаимодействий. В форме — флаги уведомлений, lazy-load цепочки и email-шаблона; перед сохранением null-булевы флаги приводятся к false.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Iteraction.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.iteraction.IteractionEdit ` |
| **XML-дескриптор** | `iteraction-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.iteraction` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `iteractionDc` |
| **focusComponent** | `iterationNameField` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Iteraction.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Iteraction** HRM HuntTech: редактирование записи сущности `Iteraction`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Iteraction` |
| **View** | `iteraction-edit-view` |
| **Data containers** | `iteractionDc` (instance), `workStatusDc` (collection), `iteractionsTreeDc` (collection), `iteractionElementsDc` (collection) |
| **Loader** | `iteractionDl` |

### JPQL (если задан)

```
select e from itpearls_EmployeeWorkStatus e order by e.workStatusName
```

### Привязки property (form / table)

- `mandatoryIteraction`
- `number`
- `iteractionTree`
- `iterationName`
- `pic`
- `signEndProcessVacancyClosed`
- `signStartCase`
- `signEndCase`
- `statistics`
- `signComment`
- `signPriorityNews`
- `signViewOnlyManager`
- `signEmailSend`
- `signFeedback`
- `signOurInterviewAssigned`
- `signOurInterview`
- `signClientInterview`
- `signSendToClient`
- `signPersonalReserve`
- `signPersonalReserveDelete`
- `signPersonalReservePut`
- `signPersonalReserveRemove`
- `outstaffingSign`
- `staffInteractionStatus`
- `workStatus`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Iteraction.browse` | create / edit action |
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

- Корневой layout: `expand` на основную таблицу / форму (`iterationNameField`)
- Фильтр: `filter` → `iteractionDl`
- Таблицы: —

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
