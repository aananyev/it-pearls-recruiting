# Person Edit (`itpearls_Person.edit`)

> Сущность: [Person.md](../entities/Person.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Person** HRM HuntTech: редактирование записи сущности `Person`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Person.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Person.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.person.PersonEdit ` |
| **XML-дескриптор** | `person-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.person` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `personDc` |
| **focusComponent** | `form` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Person.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Person** HRM HuntTech: редактирование записи сущности `Person`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Person` |
| **View** | `person-edit-view` |
| **Data containers** | `personDc` (instance), `positionCityDc` (collection), `positionCountriesDc` (collection), `personPositionsDc` (collection) |
| **Loader** | `positionCityLc` |

### JPQL (если задан)

```
select e from itpearls_City e order by e.cityRuName
```

### Привязки property (form / table)

- `firstName`
- `middleName`
- `secondName`
- `companyDepartment`
- `birdhDate`
- `email`
- `phone`
- `mobPhone`
- `skypeName`
- `telegramName`
- `wiberName`
- `watsupName`
- `cityOfResidence`
- `positionCountry`
- `personPosition`
- `fileImageFace`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Person.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `fileImageFaceUpload` | `onFileImageFaceUploadFileUploadSucceed` | см. Java |
| `screen` | `onBeforeShow` | см. Java |
| `defaultPeoplePic` | `onDefaultPeoplePicSourceChange` | см. Java |
| `fileImageFaceUpload` | `onFileImageFaceUploadBeforeValueClear` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `lookup` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`form`)
- Фильтр: `filter` → `positionCityLc`
- Таблицы: —

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.person` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
