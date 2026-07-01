# IteractionList Edit (`itpearls_IteractionList.edit`)

> Сущность: [IteractionList.md](../entities/IteractionList.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **IteractionList** HRM HuntTech: редактирование записи сущности `IteractionList`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_IteractionList.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Записи взаимодействий кандидата с вакансией. Сложный browse с фильтрами по роли и дате; edit с проверкой подписки, цепочки взаимодействий, статуса кандидата и email после сохранения.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_IteractionList.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.iteractionlist.IteractionListEdit ` |
| **XML-дескриптор** | `iteraction-list-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.iteractionlist` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `iteractionListDc` |
| **focusComponent** | `` |
| **Меню** | `web-menu.xml` → `screen="itpearls_IteractionList.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **IteractionList** HRM HuntTech: редактирование записи сущности `IteractionList`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `IteractionList` |
| **View** | `iteractionList-edit-view` |
| **Data containers** | `iteractionListDc` (instance), `iteractionTypesDc` (collection), `openPositionDc` (collection), `usersDc` (collection) |
| **Loader** | `iteractionListDl` |

### JPQL (если задан)

```
select e from itpearls_Iteraction e
                    where e.iteractionTree is not null
                    order by e.iterationName
```

### Привязки property (form / table)

- `numberIteraction`
- `dateIteraction`
- `rating`
- `candidate`
- `vacancy`
- `iteractionType`
- `addString`
- `addDate`
- `addInteger`
- `communicationMethod`
- `recrutier`
- `comment`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_IteractionList.browse` | create / edit action |
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

- Корневой layout: `expand` на основную таблицу / форму (``)
- Фильтр: `filter` → `iteractionListDl`
- Таблицы: —

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.iteractionlist` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-30 | `setClosingDateLabel`: null-guard при сбросе `vacancyFiels` (диалог закрытой позиции) — устранён NPE |
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
