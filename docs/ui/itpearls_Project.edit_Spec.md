# Project Edit (`itpearls_Project.edit`)

> Сущность: [Project.md](../entities/Project.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Project** HRM HuntTech: редактирование записи сущности `Project`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Project.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Project.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.project.ProjectEdit ` |
| **XML-дескриптор** | `project-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.project` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `projectDc` |
| **focusComponent** | `projectNameField` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Project.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Project** HRM HuntTech: редактирование записи сущности `Project`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Project` |
| **View** | `project-edit-view` |
| **Data containers** | `projectDc` (instance), `projectOpenPositionsDc` (collection), `projectTreeDc` (collection), `projectDepartmentsDc` (collection), `projectOwnersDc` (collection) |
| **Loader** | `projectOpenPositionsDl` |

### JPQL (если задан)

```
select e from itpearls_OpenPosition e
                        where e.projectName = :project
                        order by e.createTs desc
```

### Привязки property (form / table)

- `projectIsClosed`
- `defaultProject`
- `projectTree`
- `projectName`
- `startProjectDate`
- `endProjectDate`
- `projectDepartment`
- `projectOwner`
- `generalChat`
- `chatForCV`
- `projectLogo`
- `projectDescription`
- `openClose`
- `vacansyName`
- `numberPosition`
- `positionType`
- `createTs`
- `templateLetter`

### Колонки таблицы (browse)

- `openClose`
- `vacansyName`
- `numberPosition`
- `positionType`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Project.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `projectLogoFileUpload` | `onProjectLogoFileUploadBeforeValueClear` | см. Java |
| `screen` | `onBeforeShow1` | см. Java |
| `projectLogoFileUpload` | `onProjectLogoFileUploadFileUploadSucceed` | см. Java |
| `projectTab` | `onProjectTabSelectedTabChange` | см. Java |
| `checkBoxProjectIsClosed` | `onCheckBoxProjectIsClosedValueChange1` | см. Java |
| `screen` | `onBeforeShow` | см. Java |
| `generalChatTextField` | `onGeneralChatTextFieldValueChange` | см. Java |
| `chatForCVTextField` | `onChatForCVTextFieldValueChange` | см. Java |
| `checkBoxProjectIsClosed` | `onCheckBoxProjectIsClosedValueChange` | см. Java |
| `screen` | `onBeforeCommitChanges` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `lookup` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`projectNameField`)
- Фильтр: `filter` → `projectOpenPositionsDl`
- Таблицы: `projectOpenPositionTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.project` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
