# IteractionList Edit (`itpearls_IteractionList.edit`)

> Сущность: [IteractionList.md](../entities/IteractionList.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **IteractionList** HRM HuntTech: редактирование записи сущности `IteractionList`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_IteractionList.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

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

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `candidateField` | `onCandidateFieldValueChange` | см. Java |
| `screen` | `onAfterShow2` | см. Java |
| `screen` | `onBeforeClose1` | см. Java |
| `screen` | `onBeforeCommitChanges` | см. Java |
| `screen` | `onAfterCommitChanges1` | см. Java |
| `screen` | `onBeforeClose` | см. Java |
| `screen` | `onBeforeShow` | см. Java |
| `screen` | `onAfterShow` | см. Java |
| `iteractionTypeField` | `onIteractionTypeFieldValueChange` | см. Java |
| `screen` | `onInit` | см. Java |
| `vacancyFiels` | `onVacancyFielsValueChange` | см. Java |
| `vacancyFiels` | `onVacancyFielsValueChange2` | см. Java |
| `addDate` | `onAddDateValueChange` | см. Java |
| `addString` | `onAddStringValueChange` | см. Java |
| `addInteger` | `onAddIntegerValueChange` | см. Java |


### @Install (generators / providers)

| Target | Subject | Назначение |
|--------|---------|------------|
| `candidateField` | `optionImageProvider` | см. Java |
| `ratingField` | `optionStyleProvider` | см. Java |
| `vacancyFiels` | `optionImageProvider` | см. Java |
| `vacancyFiels` | `optionIconProvider` | см. Java |
| `vacancyFiels` | `optionStyleProvider` | см. Java |
| `recrutierField` | `optionIconProvider` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `lookup` | standard CUBA action | — | CRUD / lookup |
| `open` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

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
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
